package com.greathiit.bwc.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.common.ExcelUtil;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.system.entity.SysRole;
import com.greathiit.bwc.module.system.entity.SysRoleMenu;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import com.greathiit.bwc.module.system.mapper.SysRoleMapper;
import com.greathiit.bwc.module.system.mapper.SysRoleMenuMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantUserMapper;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.PermissionService;
import com.greathiit.bwc.security.RequirePermission;
import com.greathiit.bwc.security.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户 / 角色 / 被访人：租户内隔离的账号管理、角色分配、批量导入导出。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysTenantUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysTenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;
    private final CryptoService cryptoService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final com.greathiit.bwc.module.system.service.TenantService tenantService;

    @GetMapping
    @RequirePermission("system:read")
    public R<IPage<SysTenantUser>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String roleCode,
                                         @RequestParam(required = false) String status) {
        LambdaQueryWrapper<SysTenantUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysTenantUser::getUsername, keyword)
                    .or().like(SysTenantUser::getRealName, keyword)
                    .or().like(SysTenantUser::getPhoneMask, keyword));
        }
        if (roleCode != null && !roleCode.isBlank()) {
            wrapper.eq(SysTenantUser::getRoleCode, roleCode);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysTenantUser::getStatus, status);
        }
        wrapper.orderByAsc(SysTenantUser::getId);
        IPage<SysTenantUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::sanitize);
        return R.ok(result);
    }

    @GetMapping("/{id}")
    @RequirePermission("system:read")
    public R<SysTenantUser> get(@PathVariable Long id) {
        SysTenantUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        sanitize(user);
        return R.ok(user);
    }

    @PostMapping
    @RequirePermission("system:write")
    public R<SysTenantUser> create(@RequestBody Map<String, Object> body) {
        SysTenantUser user = new SysTenantUser();
        user.setUsername(string(body.get("username")));
        user.setRealName(string(body.get("realName")));
        user.setRoleCode(string(body.get("roleCode")));
        user.setUserType(string(body.getOrDefault("userType", "STAFF")));
        user.setDept(string(body.get("dept")));
        user.setStatus(string(body.getOrDefault("status", "NORMAL")));
        return R.ok(doCreate(user, string(body.get("password")),
                string(body.get("phone")), string(body.get("idCard"))));
    }

    private SysTenantUser doCreate(SysTenantUser user, String rawPassword, String phone, String idCard) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BizException(400, "用户名不能为空");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId == 0) {
            throw new BizException(400, "请选择租户后再创建用户");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysTenantUser>()
                .eq(SysTenantUser::getUsername, user.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(400, "用户名已存在");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant != null && tenant.getUserQuota() != null) {
            Long count = userMapper.selectCount(null);
            if (count != null && count >= tenant.getUserQuota()) {
                throw new BizException(400, "已超出该租户用户配额：" + tenant.getUserQuota());
            }
        }
        user.setId(null);
        user.setTenantId(tenantId);
        user.setPassword(passwordEncoder.encode(rawPassword == null || rawPassword.isBlank()
                ? "Admin@123" : rawPassword));
        SysRole role = requireValidRole(user.getRoleCode());
        user.setRoleCode(role.getRoleCode());
        user.setRoleId(role.getId());
        if (phone != null && !phone.isBlank()) {
            user.setPhoneEnc(cryptoService.encrypt(phone));
            user.setPhoneMask(cryptoService.maskPhone(phone));
        }
        if (idCard != null && !idCard.isBlank()) {
            user.setIdCardEnc(cryptoService.encrypt(idCard));
            user.setIdCardMask(cryptoService.maskIdCard(idCard));
        }
        userMapper.insert(user);
        auditService.record("user", "CREATE", user.getUsername(), "新增用户：" + user.getRealName());
        sanitize(user);
        return user;
    }

    @PutMapping("/{id}")
    @RequirePermission("system:write")
    public R<SysTenantUser> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SysTenantUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (body.get("realName") != null) {
            user.setRealName(string(body.get("realName")));
        }
        if (body.get("roleCode") != null) {
            SysRole role = requireValidRole(string(body.get("roleCode")));
            user.setRoleCode(role.getRoleCode());
            user.setRoleId(role.getId());
        }
        if (body.get("userType") != null) {
            user.setUserType(string(body.get("userType")));
        }
        if (body.get("dept") != null) {
            user.setDept(string(body.get("dept")));
        }
        if (body.get("status") != null) {
            user.setStatus(string(body.get("status")));
        }
        if (body.get("phone") != null) {
            String phone = string(body.get("phone"));
            user.setPhoneEnc(cryptoService.encrypt(phone));
            user.setPhoneMask(cryptoService.maskPhone(phone));
        }
        if (body.get("idCard") != null) {
            String idCard = string(body.get("idCard"));
            user.setIdCardEnc(cryptoService.encrypt(idCard));
            user.setIdCardMask(cryptoService.maskIdCard(idCard));
        }
        if (body.get("password") != null && !string(body.get("password")).isBlank()) {
            user.setPassword(passwordEncoder.encode(string(body.get("password"))));
        }
        userMapper.updateById(user);
        permissionService.evictAll();
        auditService.record("user", "UPDATE", user.getUsername(), "修改用户");
        sanitize(user);
        return R.ok(user);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:write")
    public R<Void> delete(@PathVariable Long id) {
        SysTenantUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (user.getId().equals(AuthContext.userId())) {
            throw new BizException(400, "不能删除当前登录账号");
        }
        userMapper.deleteById(id);
        auditService.record("user", "DELETE", user.getUsername(), "删除用户");
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    @RequirePermission("system:write")
    public R<Map<String, Object>> resetPassword(@PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, String> body) {
        SysTenantUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        String password = body == null || body.get("password") == null || body.get("password").isBlank()
                ? "Admin@123" : body.get("password");
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
        auditService.record("user", "RESET_PASSWORD", user.getUsername(), "重置密码");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", user.getUsername());
        result.put("password", password);
        return R.ok(result);
    }

    /** 对象级校验：角色必须存在且属于当前租户（平台超管忽略租户过滤） */
    private void requireRole(Long id) {
        if (roleMapper.selectById(id) == null) {
            throw BizException.notFound("角色不存在或不属于当前租户");
        }
    }

    /** 角色编码必须真实存在且不能是平台角色，防止通过用户管理伪造高权限角色 */
    private SysRole requireValidRole(String roleCode) {
        String code = roleCode == null || roleCode.isBlank() ? "STAFF" : roleCode.trim();
        if ("SYSTEM".equals(code) || "PLATFORM_OPERATOR".equals(code)) {
            throw BizException.forbidden("不允许分配平台角色");
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, code).last("limit 1"));
        if (role == null) {
            throw new BizException(400, "角色不存在：" + code);
        }
        return role;
    }

    @GetMapping("/roles")
    @RequirePermission("system:read")
    public R<List<SysRole>> roles() {
        return R.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getId)));
    }

    @GetMapping("/roles/menu-options")
    @RequirePermission("system:read")
    public R<List<Map<String, Object>>> roleMenuOptions() {
        return R.ok(tenantService.roleMenuTree(TenantContext.getTenantId()));
    }

    @PostMapping("/roles")
    @RequirePermission("system:write")
    public R<SysRole> createRole(@RequestBody SysRole role) {
        if (role.getRoleCode() == null || role.getRoleCode().isBlank()) {
            throw new BizException(400, "角色编码不能为空");
        }
        if ("SYSTEM".equals(role.getRoleCode()) || "PLATFORM_OPERATOR".equals(role.getRoleCode())) {
            throw BizException.forbidden("不允许创建平台角色");
        }
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, role.getRoleCode()));
        if (exists != null && exists > 0) {
            throw new BizException(400, "角色编码已存在：" + role.getRoleCode());
        }
        role.setId(null);
        role.setTenantId(TenantContext.getTenantId());
        if (role.getDataScope() == null) {
            role.setDataScope("TENANT");
        }
        roleMapper.insert(role);
        auditService.record("role", "CREATE", role.getRoleCode(), "新增角色：" + role.getRoleName());
        return R.ok(role);
    }

    @PutMapping("/roles/{id}")
    @RequirePermission("system:write")
    public R<SysRole> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        requireRole(id);
        role.setId(id);
        role.setTenantId(null);
        roleMapper.updateById(role);
        permissionService.evictAll();
        return R.ok(roleMapper.selectById(id));
    }

    @DeleteMapping("/roles/{id}")
    @RequirePermission("system:write")
    public R<Void> deleteRole(@PathVariable Long id) {
        requireRole(id);
        Long used = userMapper.selectCount(new LambdaQueryWrapper<SysTenantUser>()
                .eq(SysTenantUser::getRoleId, id));
        if (used != null && used > 0) {
            throw new BizException(400, "该角色下仍有用户，不能删除");
        }
        SysRole role = roleMapper.selectById(id);
        if (role != null && List.of("TENANT_ADMIN", "CAPTAIN", "GUARD", "STAFF", "STUDENT",
                "LEADER", "VISITEE").contains(role.getRoleCode())) {
            throw new BizException(400, "内置角色不允许删除");
        }
        roleMapper.deleteById(id);
        permissionService.evictAll();
        return R.ok();
    }

    @GetMapping("/roles/{id}/menus")
    @RequirePermission("system:read")
    public R<List<Long>> roleMenus(@PathVariable Long id) {
        requireRole(id);
        List<SysRoleMenu> list = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, id));
        return R.ok(list.stream().map(SysRoleMenu::getMenuId).toList());
    }

    @PutMapping("/roles/{id}/menus")
    @RequirePermission("system:write")
    public R<Void> saveRoleMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        requireRole(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setTenantId(TenantContext.getTenantId());
                roleMenu.setRoleId(id);
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }
        auditService.record("role", "MENUS", String.valueOf(id), "角色菜单授权");
        permissionService.evictAll();
        return R.ok();
    }

    @GetMapping("/visitees")
    @RequirePermission("system:read")
    public R<List<SysTenantUser>> visitees() {
        List<SysTenantUser> users = userMapper.selectList(new LambdaQueryWrapper<SysTenantUser>()
                .in(SysTenantUser::getRoleCode, List.of("STAFF", "VISITEE", "TENANT_ADMIN", "CAPTAIN"))
                .eq(SysTenantUser::getStatus, "NORMAL")
                .orderByAsc(SysTenantUser::getId));
        users.forEach(this::sanitize);
        return R.ok(users);
    }

    @GetMapping("/export")
    @RequirePermission("system:read")
    public void export(HttpServletResponse response) throws IOException {
        List<SysTenantUser> users = userMapper.selectList(new LambdaQueryWrapper<SysTenantUser>()
                .orderByAsc(SysTenantUser::getId));
        List<Map<String, ?>> rows = new ArrayList<>();
        for (SysTenantUser user : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", user.getUsername());
            row.put("realName", user.getRealName());
            row.put("roleCode", user.getRoleCode());
            row.put("userType", user.getUserType());
            row.put("dept", user.getDept());
            row.put("phone", user.getPhoneMask());
            row.put("status", user.getStatus());
            rows.add(row);
        }
        ExcelUtil.write(response, "用户列表", List.of("用户名", "姓名", "角色", "类型", "部门", "手机号", "状态"),
                List.of("username", "realName", "roleCode", "userType", "dept", "phone", "status"), rows);
    }

    /**
     * 批量导入：列顺序 用户名/姓名/手机号/角色编码/部门。
     */
    @PostMapping("/import")
    @RequirePermission("system:write")
    public R<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择 Excel 文件");
        }
        int success = 0;
        List<String> failures = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String username = cell(row, 0);
                if (username == null || username.isBlank()) {
                    continue;
                }
                try {
                    SysTenantUser user = new SysTenantUser();
                    user.setUsername(username);
                    user.setRealName(cell(row, 1));
                    user.setRoleCode(cell(row, 3) == null ? "STAFF" : cell(row, 3));
                    user.setUserType("STAFF");
                    user.setDept(cell(row, 4));
                    user.setStatus("NORMAL");
                    doCreate(user, "Admin@123", cell(row, 2), null);
                    success++;
                } catch (Exception e) {
                    failures.add(username + "：" + e.getMessage());
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", failures.size());
        result.put("failures", failures);
        result.put("defaultPassword", "Admin@123");
        auditService.record("user", "IMPORT", "excel", "批量导入用户：" + success);
        return R.ok(result);
    }

    private String cell(Row row, int index) {
        if (row.getCell(index) == null) {
            return null;
        }
        row.getCell(index).setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
        return row.getCell(index).getStringCellValue();
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void sanitize(SysTenantUser user) {
        if (user == null) {
            return;
        }
        user.setPassword(null);
        user.setPhoneEnc(null);
        user.setIdCardEnc(null);
        user.setOpenid(null);
        user.setUnionid(null);
    }
}
