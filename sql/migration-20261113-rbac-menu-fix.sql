-- 2026-11-13 权限整改：菜单权限口径修正
-- 1) 备份当前角色-菜单关系，便于回滚
-- 2) 收回误授权给租户角色的平台专属菜单
-- 3) 收回 C 端角色的月报菜单（logistics:read）
-- 4) 列表页权限由 write 改为 read，写操作由后端接口和按钮权限控制
SET NAMES utf8mb4;
USE `bwc`;

CREATE TABLE IF NOT EXISTS `sys_role_menu_bak_20261113` AS
SELECT * FROM `sys_role_menu`;

-- 平台专属菜单：全部学校报表，不得授权给任何租户角色
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.id = rm.role_id
JOIN `sys_menu` m ON m.id = rm.menu_id
WHERE r.tenant_id > 0
  AND m.path = '/report-center/platform';

-- C 端角色不展示月报季报（该菜单权限为 logistics:read）
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.id = rm.role_id
JOIN `sys_menu` m ON m.id = rm.menu_id
WHERE r.tenant_id > 0
  AND r.role_code IN ('STAFF', 'STUDENT', 'VISITEE')
  AND m.path = '/report-center/monthly';

-- 列表页应以 read 权限展示；写按钮由前端 hasPerm 与后端接口共同控制
UPDATE `sys_menu` SET permission = 'visitor:read', update_time = NOW()
WHERE path = '/visitor/appointments' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'vehicle:read', update_time = NOW()
WHERE path IN ('/vehicle/vehicles', '/vehicle/appointments') AND deleted = 0;
UPDATE `sys_menu` SET permission = 'event:read', update_time = NOW()
WHERE path = '/event/events' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'fire:read', update_time = NOW()
WHERE path = '/fire/facilities' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'patrol:read', update_time = NOW()
WHERE path = '/patrol/routes' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'lost:read', update_time = NOW()
WHERE path = '/lost/items' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'education:read', update_time = NOW()
WHERE path = '/education/courses' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'report:read', update_time = NOW()
WHERE path = '/report/applies' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'device:read', update_time = NOW()
WHERE path = '/device/devices' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'emergency:read', update_time = NOW()
WHERE path = '/emergency/plans' AND deleted = 0;
UPDATE `sys_menu` SET permission = 'logistics:read', update_time = NOW()
WHERE path = '/logistics/schedules' AND deleted = 0;

-- C 端可通过小程序提交投诉/建议，需要反馈菜单对应的 logistics:write
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT r.tenant_id, r.id, 144, NOW(), NOW(), 0
FROM `sys_role` r
WHERE r.tenant_id > 0
  AND r.role_code IN ('STAFF', 'STUDENT', 'VISITEE')
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
      WHERE rm.role_id = r.id AND rm.menu_id = 144 AND rm.deleted = 0
  );
