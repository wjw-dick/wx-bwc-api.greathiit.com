-- 报表中心子菜单权限修正（幂等）
-- 1) 全部学校报表仅平台端可见；2) 月报季报需要后勤/报表读权限
UPDATE sys_menu SET permission = 'platform:read', update_time = NOW()
WHERE path = '/report-center/platform' AND permission <> 'platform:read';

UPDATE sys_menu SET permission = 'logistics:read', update_time = NOW()
WHERE path = '/report-center/monthly' AND permission <> 'logistics:read';
