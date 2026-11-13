-- H5 门户补齐车辆预约、安全上报入口（幂等）
-- 背景：H5 原先只有访客/失物/资讯入口，车辆预约与治安事件/消防隐患/设备报修缺失。
SET NAMES utf8mb4;
USE `bwc`;

INSERT INTO sys_menu (tenant_id, platform, parent_id, name, name_en, path, icon, sort, menu_type, visible, create_time, update_time, deleted)
SELECT 0, 'h5', 0, '车辆预约', 'Vehicle Booking', '/vehicle', '', 3, 'MENU', 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE platform = 'h5' AND path = '/vehicle' AND deleted = 0
);

INSERT INTO sys_menu (tenant_id, platform, parent_id, name, name_en, path, icon, sort, menu_type, visible, create_time, update_time, deleted)
SELECT 0, 'h5', 0, '安全上报', 'Safety Report', '/safety', '', 4, 'MENU', 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE platform = 'h5' AND path = '/safety' AND deleted = 0
);

-- 保证已有门户菜单顺序稳定（幂等）
UPDATE sys_menu SET sort = 1 WHERE tenant_id = 0 AND platform = 'h5' AND path = '/' AND deleted = 0;
UPDATE sys_menu SET sort = 2 WHERE tenant_id = 0 AND platform = 'h5' AND path = '/visitor' AND deleted = 0;
UPDATE sys_menu SET sort = 3 WHERE tenant_id = 0 AND platform = 'h5' AND path = '/vehicle' AND deleted = 0;
UPDATE sys_menu SET sort = 4 WHERE tenant_id = 0 AND platform = 'h5' AND path = '/safety' AND deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE tenant_id = 0 AND platform = 'h5' AND path = '/lost' AND deleted = 0;
UPDATE sys_menu SET sort = 6 WHERE tenant_id = 0 AND platform = 'h5' AND path = '/news' AND deleted = 0;
UPDATE sys_menu SET sort = 7 WHERE tenant_id = 0 AND platform = 'h5' AND path = '/school' AND deleted = 0;
