-- 门岗核验独立页面菜单（幂等）
-- 用途：门卫在 PC 后台直接扫码/输入通行码核验，不再依赖后台列表操作。
SET NAMES utf8mb4;
USE `bwc`;

INSERT INTO sys_menu (tenant_id, platform, parent_id, name, name_en, path, icon, sort, menu_type, permission, visible, create_time, update_time, deleted)
SELECT 0, 'admin', 40, '门岗核验', 'Gate Check', '/visitor/gate', 'Camera', 0, 'MENU', 'visitor:write', 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE platform = 'admin' AND path = '/visitor/gate' AND deleted = 0
);

-- 已拥有「访客预约」菜单的角色自动获得门岗核验
INSERT INTO sys_role_menu (tenant_id, role_id, menu_id, create_time, deleted)
SELECT rm.tenant_id, rm.role_id, target.id, NOW(), 0
FROM sys_role_menu rm
JOIN sys_menu source ON source.id = rm.menu_id AND source.path = '/visitor/appointments' AND source.deleted = 0
JOIN sys_menu target ON target.path = '/visitor/gate' AND target.tenant_id = 0 AND target.deleted = 0
WHERE rm.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu exist
      WHERE exist.role_id = rm.role_id AND exist.menu_id = target.id AND exist.deleted = 0
  );

-- 兜底：管理员/队长/保安角色直接授权
INSERT INTO sys_role_menu (tenant_id, role_id, menu_id, create_time, deleted)
SELECT r.tenant_id, r.id, m.id, NOW(), 0
FROM sys_role r
JOIN sys_menu m ON m.path = '/visitor/gate' AND m.tenant_id = 0 AND m.deleted = 0
WHERE UPPER(r.role_code) IN ('TENANT_ADMIN', 'CAPTAIN', 'GUARD')
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu exist
      WHERE exist.role_id = r.id AND exist.menu_id = m.id AND exist.deleted = 0
  );
