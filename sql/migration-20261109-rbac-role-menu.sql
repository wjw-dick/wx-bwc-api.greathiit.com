-- RBAC 增量迁移：为内置租户角色写入角色-菜单授权（幂等，可重复执行）
-- 目标库：bwc（单独连接执行），只新增 sys_role_menu 记录，不删除表、不清空数据
-- 执行：mysql -h127.0.0.1 -P3307 -uroot -p --default-character-set=utf8mb4 bwc < sql/migration-20261109-rbac-role-menu.sql

-- 1. 租户管理员：除平台级模块外的全部管理权限
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT r.`tenant_id`, r.`id`, m.`id`, NOW(), NOW(), 0
FROM `sys_role` r
JOIN `sys_menu` m ON m.`platform` = 'admin' AND m.`deleted` = 0
LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`id` AND rm.`menu_id` = m.`id` AND rm.`deleted` = 0
WHERE r.`role_code` = 'TENANT_ADMIN' AND r.`deleted` = 0
  AND m.`permission` IS NOT NULL AND m.`permission` <> ''
  AND m.`permission` NOT LIKE 'platform:%'
  AND m.`permission` NOT LIKE 'tenant:%'
  AND m.`permission` NOT LIKE 'menu:%'
  AND m.`permission` NOT LIKE 'param:%'
  AND m.`permission` NOT LIKE 'i18n:%'
  AND m.`permission` NOT LIKE 'storage:%'
  AND m.`permission` NOT LIKE 'wechat:%'
  AND rm.`id` IS NULL;

-- 2. 保卫处科长 / 保安队长（CAPTAIN）：业务模块读写
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT r.`tenant_id`, r.`id`, m.`id`, NOW(), NOW(), 0
FROM `sys_role` r
JOIN `sys_menu` m ON m.`platform` = 'admin' AND m.`deleted` = 0
LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`id` AND rm.`menu_id` = m.`id` AND rm.`deleted` = 0
WHERE r.`role_code` = 'CAPTAIN' AND r.`deleted` = 0
  AND (
    m.`permission` = 'dashboard:read'
    OR m.`permission` LIKE 'visitor:%' OR m.`permission` LIKE 'vehicle:%'
    OR m.`permission` LIKE 'plate:%' OR m.`permission` LIKE 'fire:%'
    OR m.`permission` LIKE 'hazard:%' OR m.`permission` LIKE 'event:%'
    OR m.`permission` LIKE 'patrol:%' OR m.`permission` LIKE 'lost:%'
    OR m.`permission` LIKE 'device:%' OR m.`permission` LIKE 'emergency:%'
    OR m.`permission` LIKE 'logistics:%' OR m.`permission` LIKE 'feedback:%'
    OR m.`permission` LIKE 'report:%'
  )
  AND rm.`id` IS NULL;

-- 3. 保安（GUARD）：与队长相同的业务读写
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT r.`tenant_id`, r.`id`, m.`id`, NOW(), NOW(), 0
FROM `sys_role` r
JOIN `sys_menu` m ON m.`platform` = 'admin' AND m.`deleted` = 0
LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`id` AND rm.`menu_id` = m.`id` AND rm.`deleted` = 0
WHERE r.`role_code` = 'GUARD' AND r.`deleted` = 0
  AND (
    m.`permission` = 'dashboard:read'
    OR m.`permission` LIKE 'visitor:%' OR m.`permission` LIKE 'vehicle:%'
    OR m.`permission` LIKE 'plate:%' OR m.`permission` LIKE 'fire:%'
    OR m.`permission` LIKE 'hazard:%' OR m.`permission` LIKE 'event:%'
    OR m.`permission` LIKE 'patrol:%' OR m.`permission` LIKE 'lost:%'
    OR m.`permission` LIKE 'device:%' OR m.`permission` LIKE 'emergency:%'
    OR m.`permission` LIKE 'logistics:%' OR m.`permission` LIKE 'feedback:%'
    OR m.`permission` LIKE 'report:%'
  )
  AND rm.`id` IS NULL;

-- 4. 校领导（LEADER）：业务模块只读
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT r.`tenant_id`, r.`id`, m.`id`, NOW(), NOW(), 0
FROM `sys_role` r
JOIN `sys_menu` m ON m.`platform` = 'admin' AND m.`deleted` = 0
LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`id` AND rm.`menu_id` = m.`id` AND rm.`deleted` = 0
WHERE r.`role_code` = 'LEADER' AND r.`deleted` = 0
  AND m.`permission` LIKE '%:read'
  AND (
    m.`permission` LIKE 'dashboard:%'
    OR m.`permission` LIKE 'visitor:%' OR m.`permission` LIKE 'vehicle:%'
    OR m.`permission` LIKE 'plate:%' OR m.`permission` LIKE 'fire:%'
    OR m.`permission` LIKE 'hazard:%' OR m.`permission` LIKE 'event:%'
    OR m.`permission` LIKE 'patrol:%' OR m.`permission` LIKE 'lost:%'
    OR m.`permission` LIKE 'device:%' OR m.`permission` LIKE 'emergency:%'
    OR m.`permission` LIKE 'logistics:%' OR m.`permission` LIKE 'feedback:%'
    OR m.`permission` LIKE 'report:%'
  )
  AND rm.`id` IS NULL;

-- 5. 教职工（STAFF）/ 被访人（VISITEE）：C 端业务读写 + 访客审批
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT r.`tenant_id`, r.`id`, m.`id`, NOW(), NOW(), 0
FROM `sys_role` r
JOIN `sys_menu` m ON m.`platform` = 'admin' AND m.`deleted` = 0
LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`id` AND rm.`menu_id` = m.`id` AND rm.`deleted` = 0
WHERE r.`role_code` IN ('STAFF', 'VISITEE') AND r.`deleted` = 0
  AND (
    m.`permission` = 'dashboard:read'
    OR m.`permission` LIKE 'visitor:%' OR m.`permission` LIKE 'vehicle:%'
    OR m.`permission` LIKE 'lost:%' OR m.`permission` LIKE 'education:%'
    OR m.`permission` LIKE 'report:%' OR m.`permission` LIKE 'device:%'
    OR m.`permission` LIKE 'feedback:%' OR m.`permission` LIKE 'emergency:%'
    OR m.`permission` = 'visitor:approve'
  )
  AND rm.`id` IS NULL;

-- 6. 学生（STUDENT）：C 端业务读写，无审批权限
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT r.`tenant_id`, r.`id`, m.`id`, NOW(), NOW(), 0
FROM `sys_role` r
JOIN `sys_menu` m ON m.`platform` = 'admin' AND m.`deleted` = 0
LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`id` AND rm.`menu_id` = m.`id` AND rm.`deleted` = 0
WHERE r.`role_code` = 'STUDENT' AND r.`deleted` = 0
  AND (
    m.`permission` = 'dashboard:read'
    OR m.`permission` LIKE 'visitor:%' OR m.`permission` LIKE 'vehicle:%'
    OR m.`permission` LIKE 'lost:%' OR m.`permission` LIKE 'education:%'
    OR m.`permission` LIKE 'report:%' OR m.`permission` LIKE 'device:%'
    OR m.`permission` LIKE 'feedback:%' OR m.`permission` LIKE 'emergency:%'
  )
  AND m.`permission` <> 'visitor:approve'
  AND rm.`id` IS NULL;
