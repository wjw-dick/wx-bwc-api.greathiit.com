-- 端到端测试发现：/api/visitor/appointments/{id}/approve 注解要求 visitor:approve，
-- 但 sys_menu 中从未定义该权限，导致默认拒绝策略下所有角色审批均 403。
-- 本迁移可重复执行：新增按钮型权限节点（visible=0，不进菜单树），并授予可审批角色。
SET NAMES utf8mb4;

INSERT INTO sys_menu (tenant_id, platform, parent_id, name, name_en, path, icon, sort, menu_type, permission, visible, create_time, deleted)
SELECT 0, 'admin', 40, '访客审批', 'Visitor Approve', NULL, NULL, 99, 'BUTTON', 'visitor:approve', 0, NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE permission = 'visitor:approve' AND deleted = 0
);

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id, create_time, deleted)
SELECT r.tenant_id, r.id, m.id, NOW(), 0
FROM sys_role r
JOIN sys_menu m ON m.permission = 'visitor:approve' AND m.tenant_id = 0 AND m.deleted = 0
WHERE UPPER(r.role_code) IN ('TENANT_ADMIN', 'CAPTAIN', 'GUARD', 'STAFF', 'VISITEE')
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = 0
  );
