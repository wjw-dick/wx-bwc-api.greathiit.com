-- ============================================================================
-- 初始化数据：租户 / 平台账号 / 角色 / 菜单 / 字典 / 参数 / 示例业务数据
-- 默认密码：Admin@123（BCrypt，$2y$ 与 Spring BCryptPasswordEncoder 兼容）
-- ============================================================================
USE `bwc`;
SET NAMES utf8mb4;

-- ------------------------- 租户 -------------------------
INSERT INTO `sys_tenant` (`id`,`tenant_id`,`name`,`code`,`type`,`status`,`open_time`,`expire_time`,`user_quota`,`contact`,`phone`,`remark`,`create_time`,`update_time`) VALUES
(1,0,'示范中学','DEMO','中小学','NORMAL','2025-09-01 00:00:00','2027-12-31 23:59:59',300,'张主任','13800000001','首批试点学校','2026-01-01 09:00:00','2026-10-01 09:00:00'),
(2,0,'示范大学','UNIV','高校','NORMAL','2025-09-01 00:00:00','2027-12-31 23:59:59',2000,'李处长','13800000002','高校试点','2026-01-01 09:00:00','2026-10-01 09:00:00'),
(3,0,'实验学校','EXP','中小学','TRIAL','2026-09-01 00:00:00','2026-11-15 23:59:59',100,'王老师','13800000003','试用租户','2026-09-01 09:00:00','2026-10-01 09:00:00');

-- ------------------------- 平台账号 -------------------------
INSERT INTO `sys_platform_user` (`id`,`tenant_id`,`username`,`password`,`real_name`,`role_code`,`status`,`create_time`,`update_time`) VALUES
(1,0,'system','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','系统超级管理员','SYSTEM','NORMAL','2026-01-01 09:00:00','2026-01-01 09:00:00'),
(2,0,'operator','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','平台运营','PLATFORM_OPERATOR','NORMAL','2026-01-01 09:00:00','2026-01-01 09:00:00');

-- ------------------------- 全局字典 -------------------------
INSERT INTO `sys_global_dict` (`tenant_id`,`dict_type`,`dict_label`,`dict_value`,`sort`,`status`,`remark`,`create_time`,`update_time`) VALUES
(0,'event_type','失窃','THEFT',1,'NORMAL',NULL,NOW(),NOW()),
(0,'event_type','纠纷','DISPUTE',2,'NORMAL',NULL,NOW(),NOW()),
(0,'event_type','求助','HELP',3,'NORMAL',NULL,NOW(),NOW()),
(0,'event_type','失物','LOST',4,'NORMAL',NULL,NOW(),NOW()),
(0,'event_type','治安','SECURITY',5,'NORMAL',NULL,NOW(),NOW()),
(0,'hazard_level','一般','GENERAL',1,'NORMAL',NULL,NOW(),NOW()),
(0,'hazard_level','较大','MAJOR',2,'NORMAL',NULL,NOW(),NOW()),
(0,'hazard_level','重大','CRITICAL',3,'NORMAL',NULL,NOW(),NOW()),
(0,'fire_facility_type','灭火器','EXTINGUISHER',1,'NORMAL',NULL,NOW(),NOW()),
(0,'fire_facility_type','烟感','SMOKE',2,'NORMAL',NULL,NOW(),NOW()),
(0,'fire_facility_type','消火栓','HYDRANT',3,'NORMAL',NULL,NOW(),NOW()),
(0,'device_type','监控','CAMERA',1,'NORMAL',NULL,NOW(),NOW()),
(0,'device_type','道闸','GATE',2,'NORMAL',NULL,NOW(),NOW()),
(0,'device_type','人脸机','FACE',3,'NORMAL',NULL,NOW(),NOW()),
(0,'device_type','报警主机','ALARM',4,'NORMAL',NULL,NOW(),NOW()),
(0,'lost_category','证件','CARD',1,'NORMAL',NULL,NOW(),NOW()),
(0,'lost_category','电子产品','ELECTRONIC',2,'NORMAL',NULL,NOW(),NOW()),
(0,'lost_category','衣物','CLOTHING',3,'NORMAL',NULL,NOW(),NOW()),
(0,'lost_category','书籍','BOOK',4,'NORMAL',NULL,NOW(),NOW()),
(0,'lost_category','其他','OTHER',5,'NORMAL',NULL,NOW(),NOW()),
(0,'report_type','大型活动','ACTIVITY',1,'NORMAL',NULL,NOW(),NOW()),
(0,'report_type','施工/动火','CONSTRUCTION',2,'NORMAL',NULL,NOW(),NOW()),
(0,'report_type','学生外出','OUTING',3,'NORMAL',NULL,NOW(),NOW()),
(0,'report_type','危化品','CHEMICAL',4,'NORMAL',NULL,NOW(),NOW()),
(0,'report_type','宣传点位','BANNER',5,'NORMAL',NULL,NOW(),NOW()),
(0,'plate_type','固定车牌','FIXED',1,'NORMAL',NULL,NOW(),NOW()),
(0,'plate_type','临时车牌','TEMP',2,'NORMAL',NULL,NOW(),NOW()),
(0,'urgency','普通','NORMAL',1,'NORMAL',NULL,NOW(),NOW()),
(0,'urgency','紧急','URGENT',2,'NORMAL',NULL,NOW(),NOW()),
(0,'urgency','特急','CRITICAL',3,'NORMAL',NULL,NOW(),NOW());

-- ------------------------- 平台参数 -------------------------
INSERT INTO `sys_platform_param` (`tenant_id`,`param_key`,`param_name`,`param_value`,`remark`,`create_time`,`update_time`) VALUES
(0,'pass_code_valid_hours','通行码有效期（小时）','24','访客通行码默认有效时长',NOW(),NOW()),
(0,'visitor_advance_hours','访客预约提前时限（小时）','2','需提前预约的最小时长',NOW(),NOW()),
(0,'hazard_default_days','隐患整改默认限期（天）','7','自动分派时生成整改期限',NOW(),NOW()),
(0,'patrol_late_minutes','巡逻迟到容忍（分钟）','15','超过时间窗判定迟到',NOW(),NOW()),
(0,'tenant_expire_policy','租户到期策略','disable','disable=到期自动停用',NOW(),NOW()),
(0,'plate_command_timeout','车牌命令超时（秒）','8','相机离线判定',NOW(),NOW()),
(0,'parking_fee_enabled','停车收费开关','false','可选功能',NOW(),NOW()),
(0,'upload_max_mb','上传大小上限（MB）','20','证件照/现场照片',NOW(),NOW());

-- ------------------------- 语言 -------------------------
INSERT INTO `sys_language` (`tenant_id`,`lang_code`,`lang_name`,`enabled`,`sort`,`create_time`,`update_time`) VALUES
(0,'zh-CN','简体中文',1,1,NOW(),NOW()),
(0,'en-US','English',1,2,NOW(),NOW());

-- ------------------------- 菜单（tenant_id=0 为全局模板） -------------------------
INSERT INTO `sys_menu` (`id`,`tenant_id`,`platform`,`parent_id`,`name`,`name_en`,`path`,`icon`,`sort`,`menu_type`,`permission`,`visible`,`create_time`,`update_time`) VALUES
(1,0,'admin',0,'工作台','Dashboard','/dashboard','HomeFilled',1,'MENU','dashboard:read',1,NOW(),NOW()),
(10,0,'admin',0,'平台管理','Platform','/platform','OfficeBuilding',10,'MENU','platform:read',1,NOW(),NOW()),
(11,0,'admin',10,'租户管理','Tenants','/platform/tenants','School',1,'MENU','tenant:write',1,NOW(),NOW()),
(12,0,'admin',10,'全部学校看板','All Schools Dashboard','/platform/dashboard','DataAnalysis',2,'MENU','platform:read',1,NOW(),NOW()),
(13,0,'admin',10,'平台审计','Platform Audit','/platform/audit','Document',3,'MENU','platform:read',1,NOW(),NOW()),
(20,0,'admin',0,'系统管理','System','/system','Setting',20,'MENU','system:read',1,NOW(),NOW()),
(21,0,'admin',20,'用户管理','Users','/system/users','User',1,'MENU','user:write',1,NOW(),NOW()),
(22,0,'admin',20,'角色权限','Roles','/system/roles','UserFilled',2,'MENU','role:write',1,NOW(),NOW()),
(23,0,'admin',20,'数据字典','Dictionaries','/system/dicts','Collection',3,'MENU','dict:write',1,NOW(),NOW()),
(24,0,'admin',20,'平台参数','Platform Params','/system/params','Tickets',4,'MENU','param:write',1,NOW(),NOW()),
(25,0,'admin',20,'菜单配置','Menus','/system/menus','Menu',5,'MENU','menu:write',1,NOW(),NOW()),
(26,0,'admin',20,'多语言','i18n','/system/i18n','ChatDotSquare',6,'MENU','i18n:write',1,NOW(),NOW()),
(27,0,'admin',20,'对象存储','Storage','/system/storage','Coin',7,'MENU','storage:write',1,NOW(),NOW()),
(28,0,'admin',20,'微信配置','WeChat','/system/wechat','ChatLineSquare',8,'MENU','wechat:write',1,NOW(),NOW()),
(29,0,'admin',20,'消息模板','Message Templates','/system/message-templates','Message',9,'MENU','wechat:write',1,NOW(),NOW()),
(30,0,'admin',20,'审计日志','Audit Logs','/system/audit','Notebook',10,'MENU','audit:read',1,NOW(),NOW()),
(40,0,'admin',0,'门禁访客','Visitors','/visitor','Postcard',40,'MENU','visitor:read',1,NOW(),NOW()),
(48,0,'admin',40,'门岗核验','Gate Check','/visitor/gate','Camera',0,'MENU','visitor:write',1,NOW(),NOW()),
(41,0,'admin',40,'访客预约','Appointments','/visitor/appointments','Calendar',1,'MENU','visitor:write',1,NOW(),NOW()),
(42,0,'admin',40,'通行记录','Visit Records','/visitor/records','Switch',2,'MENU','visitor:read',1,NOW(),NOW()),
(43,0,'admin',40,'临时入校','Temp Entry','/visitor/temp','Position',3,'MENU','visitor:write',1,NOW(),NOW()),
(44,0,'admin',40,'黑名单','Blacklist','/visitor/blacklist','CircleClose',4,'MENU','visitor:write',1,NOW(),NOW()),
(45,0,'admin',40,'重点人员','Key Persons','/visitor/key-persons','Star',5,'MENU','visitor:write',1,NOW(),NOW()),
(46,0,'admin',40,'晚归登记','Late Return','/visitor/late-return','Moon',6,'MENU','visitor:write',1,NOW(),NOW()),
(47,0,'admin',40,'访客统计','Visitor Stats','/visitor/stats','TrendCharts',7,'MENU','visitor:read',1,NOW(),NOW()),
(50,0,'admin',0,'车辆管理','Vehicles','/vehicle','Van',50,'MENU','vehicle:read',1,NOW(),NOW()),
(51,0,'admin',50,'车辆备案','Vehicles','/vehicle/vehicles','Van',1,'MENU','vehicle:write',1,NOW(),NOW()),
(52,0,'admin',50,'车辆预约','Appointments','/vehicle/appointments','Calendar',2,'MENU','vehicle:write',1,NOW(),NOW()),
(53,0,'admin',50,'违停上报','Violations','/vehicle/violations','Warning',3,'MENU','vehicle:write',1,NOW(),NOW()),
(54,0,'admin',50,'进出记录','Vehicle Logs','/vehicle/logs','Switch',4,'MENU','vehicle:read',1,NOW(),NOW()),
(55,0,'admin',50,'停车收费','Parking Fees','/vehicle/fees','Money',5,'MENU','vehicle:write',1,NOW(),NOW()),
(56,0,'admin',50,'车牌识别配置','Plate Auth Config','/vehicle/plate-config','Key',6,'MENU','plate:write',1,NOW(),NOW()),
(57,0,'admin',50,'相机管理','Plate Cameras','/vehicle/plate-cameras','VideoCamera',7,'MENU','plate:write',1,NOW(),NOW()),
(58,0,'admin',50,'授权台账','Auth Records','/vehicle/plate-records','List',8,'MENU','plate:read',1,NOW(),NOW()),
(60,0,'admin',0,'治安事件','Security Events','/event','Bell',60,'MENU','event:read',1,NOW(),NOW()),
(61,0,'admin',60,'事件工单','Events','/event/events','AlarmClock',1,'MENU','event:write',1,NOW(),NOW()),
(62,0,'admin',60,'纠纷调解','Disputes','/event/disputes','ChatDotRound',2,'MENU','event:write',1,NOW(),NOW()),
(63,0,'admin',60,'报警联动','Alarm Links','/event/alarms','Link',3,'MENU','event:write',1,NOW(),NOW()),
(70,0,'admin',0,'消防管理','Fire','/fire','FlameFilled',70,'MENU','fire:read',1,NOW(),NOW()),
(71,0,'admin',70,'设施台账','Facilities','/fire/facilities','Grid',1,'MENU','fire:write',1,NOW(),NOW()),
(72,0,'admin',70,'扫码巡检','Inspections','/fire/inspections','Camera',2,'MENU','fire:write',1,NOW(),NOW()),
(73,0,'admin',70,'隐患整改','Hazards','/fire/hazards','WarningFilled',3,'MENU','hazard:write',1,NOW(),NOW()),
(74,0,'admin',70,'消防演练','Drills','/fire/drills','Flag',4,'MENU','fire:write',1,NOW(),NOW()),
(75,0,'admin',70,'消防培训','Trainings','/fire/trainings','Reading',5,'MENU','fire:write',1,NOW(),NOW()),
(76,0,'admin',70,'违规用电','Power Checks','/fire/power-checks','TurnOff',6,'MENU','fire:write',1,NOW(),NOW()),
(77,0,'admin',70,'消防报警','Fire Alarms','/fire/alarms','BellFilled',7,'MENU','fire:write',1,NOW(),NOW()),
(80,0,'admin',0,'巡逻管理','Patrol','/patrol','Compass',80,'MENU','patrol:read',1,NOW(),NOW()),
(81,0,'admin',80,'巡逻路线','Routes','/patrol/routes','Guide',1,'MENU','patrol:write',1,NOW(),NOW()),
(82,0,'admin',80,'巡逻点位','Points','/patrol/points','Location',2,'MENU','patrol:write',1,NOW(),NOW()),
(83,0,'admin',80,'巡逻记录','Records','/patrol/records','Timer',3,'MENU','patrol:read',1,NOW(),NOW()),
(84,0,'admin',80,'查寝巡查','Dorm Checks','/patrol/dorm-checks','House',4,'MENU','patrol:write',1,NOW(),NOW()),
(85,0,'admin',80,'巡逻统计','Patrol Stats','/patrol/stats','DataLine',5,'MENU','patrol:read',1,NOW(),NOW()),
(90,0,'admin',0,'失物招领','Lost & Found','/lost','Suitcase',90,'MENU','lost:read',1,NOW(),NOW()),
(91,0,'admin',90,'失物登记','Lost Items','/lost/items','Suitcase',1,'MENU','lost:write',1,NOW(),NOW()),
(92,0,'admin',90,'失物公告','Lost Notices','/lost/notices','ChatLineSquare',2,'MENU','lost:write',1,NOW(),NOW()),
(93,0,'admin',90,'认领登记','Claims','/lost/claims','Checked',3,'MENU','lost:write',1,NOW(),NOW()),
(94,0,'admin',90,'处置审批','Disposals','/lost/disposals','Delete',4,'MENU','lost:write',1,NOW(),NOW()),
(100,0,'admin',0,'安全宣传','Education','/education','Reading',100,'MENU','education:read',1,NOW(),NOW()),
(101,0,'admin',100,'安全课程','Courses','/education/courses','Notebook',1,'MENU','education:write',1,NOW(),NOW()),
(102,0,'admin',100,'安全考试','Exams','/education/exams','EditPen',2,'MENU','education:write',1,NOW(),NOW()),
(103,0,'admin',100,'温馨提示','Warnings','/education/warnings','Warning',3,'MENU','education:write',1,NOW(),NOW()),
(104,0,'admin',100,'通知公告','Notices','/education/notices','Bell',4,'MENU','education:write',1,NOW(),NOW()),
(105,0,'admin',100,'宣传素材','Materials','/education/materials','Picture',5,'MENU','education:write',1,NOW(),NOW()),
(110,0,'admin',0,'报备审批','Approvals','/report','DocumentChecked',110,'MENU','report:read',1,NOW(),NOW()),
(111,0,'admin',110,'安全报备','Reports','/report/applies','Document',1,'MENU','report:write',1,NOW(),NOW()),
(112,0,'admin',110,'调监控申请','Monitor Access','/report/monitor-access','VideoPlay',2,'MENU','report:write',1,NOW(),NOW()),
(120,0,'admin',0,'设备运维','Devices','/device','Monitor',120,'MENU','device:read',1,NOW(),NOW()),
(121,0,'admin',120,'设备台账','Devices','/device/devices','Monitor',1,'MENU','device:write',1,NOW(),NOW()),
(122,0,'admin',120,'报修工单','Repairs','/device/repairs','Tools',2,'MENU','device:write',1,NOW(),NOW()),
(123,0,'admin',120,'设备巡检','Inspections','/device/inspections','Aim',3,'MENU','device:write',1,NOW(),NOW()),
(130,0,'admin',0,'应急管理','Emergency','/emergency','FirstAidKit',130,'MENU','emergency:read',1,NOW(),NOW()),
(131,0,'admin',130,'应急预案','Plans','/emergency/plans','Files',1,'MENU','emergency:write',1,NOW(),NOW()),
(132,0,'admin',130,'应急物资','Supplies','/emergency/supplies','Box',2,'MENU','emergency:write',1,NOW(),NOW()),
(133,0,'admin',130,'通讯录','Contacts','/emergency/contacts','Phone',3,'MENU','emergency:write',1,NOW(),NOW()),
(134,0,'admin',130,'突发事件','Events','/emergency/events','Lightning',4,'MENU','emergency:write',1,NOW(),NOW()),
(140,0,'admin',0,'后勤管理','Logistics','/logistics','Management',140,'MENU','logistics:read',1,NOW(),NOW()),
(141,0,'admin',140,'排班管理','Schedules','/logistics/schedules','Calendar',1,'MENU','logistics:write',1,NOW(),NOW()),
(142,0,'admin',140,'考勤记录','Attendance','/logistics/attendance','Clock',2,'MENU','logistics:read',1,NOW(),NOW()),
(143,0,'admin',140,'绩效管理','Performance','/logistics/performance','Medal',3,'MENU','logistics:write',1,NOW(),NOW()),
(144,0,'admin',140,'投诉反馈','Feedback','/logistics/feedback','ChatSquare',4,'MENU','logistics:write',1,NOW(),NOW()),
(150,0,'admin',0,'报表中心','Reports','/report-center','PieChart',150,'MENU','report:read',1,NOW(),NOW()),
(151,0,'admin',150,'租户报表','Tenant Reports','/report-center/tenant','TrendCharts',1,'MENU','report:read',1,NOW(),NOW()),
(152,0,'admin',150,'全部学校报表','All Schools','/report-center/platform','Histogram',2,'MENU','report:read',1,NOW(),NOW()),
(153,0,'admin',150,'月报季报','Monthly Reports','/report-center/monthly','Document',3,'MENU','report:read',1,NOW(),NOW());

-- 小程序 tabBar 可视化配置
INSERT INTO `sys_menu` (`tenant_id`,`platform`,`parent_id`,`name`,`name_en`,`path`,`icon`,`sort`,`menu_type`,`visible`,`config_json`,`create_time`,`update_time`) VALUES
(0,'miniprogram',0,'首页','Home','/pages/index/index','home',1,'MENU',1,'{"tab":true} ',NOW(),NOW()),
(0,'miniprogram',0,'访客','Visitor','/pages/visitor/list','visitor',2,'MENU',1,'{"tab":true}',NOW(),NOW()),
(0,'miniprogram',0,'失物','Lost','/pages/lost/index','lost',3,'MENU',1,'{"tab":true}',NOW(),NOW()),
(0,'miniprogram',0,'消息','Messages','/pages/message/index','message',4,'MENU',1,'{"tab":true}',NOW(),NOW()),
(0,'miniprogram',0,'我的','Profile','/pages/profile/index','profile',5,'MENU',1,'{"tab":true}',NOW(),NOW());

-- H5 门户导航
INSERT INTO `sys_menu` (`tenant_id`,`platform`,`parent_id`,`name`,`name_en`,`path`,`icon`,`sort`,`menu_type`,`visible`,`create_time`,`update_time`) VALUES
(0,'h5',0,'首页','Home','/','',1,'MENU',1,NOW(),NOW()),
(0,'h5',0,'访客预约','Visitor Booking','/visitor','',2,'MENU',1,NOW(),NOW()),
(0,'h5',0,'车辆预约','Vehicle Booking','/vehicle','',3,'MENU',1,NOW(),NOW()),
(0,'h5',0,'安全上报','Safety Report','/safety','',4,'MENU',1,NOW(),NOW()),
(0,'h5',0,'失物招领','Lost & Found','/lost','',5,'MENU',1,NOW(),NOW()),
(0,'h5',0,'安全资讯','News','/news','',6,'MENU',1,NOW(),NOW()),
(0,'h5',0,'学校门户','School','/school','',7,'MENU',1,NOW(),NOW());

-- App 导航
INSERT INTO `sys_menu` (`tenant_id`,`platform`,`parent_id`,`name`,`name_en`,`path`,`icon`,`sort`,`menu_type`,`visible`,`create_time`,`update_time`) VALUES
(0,'app',0,'首页','Home','/home','home',1,'MENU',1,NOW(),NOW()),
(0,'app',0,'巡逻','Patrol','/patrol','qr',2,'MENU',1,NOW(),NOW()),
(0,'app',0,'事件','Events','/events','alert',3,'MENU',1,NOW(),NOW()),
(0,'app',0,'消息','Messages','/messages','message',4,'MENU',1,NOW(),NOW()),
(0,'app',0,'我的','Profile','/profile','user',5,'MENU',1,NOW(),NOW());

-- ------------------------- 角色 -------------------------
INSERT INTO `sys_role` (`id`,`tenant_id`,`role_code`,`role_name`,`data_scope`,`remark`,`create_time`,`update_time`) VALUES
(1,1,'TENANT_ADMIN','租户管理员','TENANT','保卫处负责人',NOW(),NOW()),
(2,1,'CAPTAIN','保安队长','TENANT','保安队伍管理',NOW(),NOW()),
(3,1,'GUARD','保安','TENANT','一线执勤',NOW(),NOW()),
(4,1,'STAFF','教职工','SELF','教职工/被访人',NOW(),NOW()),
(5,1,'STUDENT','学生','SELF','学生',NOW(),NOW()),
(6,1,'LEADER','校领导','TENANT_READ','只读看板',NOW(),NOW()),
(7,1,'VISITEE','被访人','SELF','访客审批',NOW(),NOW()),
(11,2,'TENANT_ADMIN','租户管理员','TENANT','保卫处负责人',NOW(),NOW()),
(12,2,'CAPTAIN','保安队长','TENANT','保安队伍管理',NOW(),NOW()),
(13,2,'GUARD','保安','TENANT','一线执勤',NOW(),NOW()),
(14,2,'STAFF','教职工','SELF','教职工/被访人',NOW(),NOW()),
(15,2,'STUDENT','学生','SELF','学生',NOW(),NOW()),
(16,2,'LEADER','校领导','TENANT_READ','只读看板',NOW(),NOW()),
(17,2,'VISITEE','被访人','SELF','访客审批',NOW(),NOW());

-- ------------------------- 租户用户 -------------------------
INSERT INTO `sys_tenant_user` (`id`,`tenant_id`,`username`,`password`,`real_name`,`phone_mask`,`role_id`,`role_code`,`user_type`,`dept`,`status`,`create_time`,`update_time`) VALUES
(1,1,'demo_admin','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','示范中学管理员','138****0001',1,'TENANT_ADMIN','ADMIN','保卫处','NORMAL',NOW(),NOW()),
(2,1,'demo_captain','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','保安队长','138****0002',2,'CAPTAIN','CAPTAIN','保卫处','NORMAL',NOW(),NOW()),
(3,1,'demo_guard','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','保安员小王','138****0003',3,'GUARD','GUARD','保卫处','NORMAL',NOW(),NOW()),
(4,1,'demo_teacher','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','张老师','138****0004',4,'STAFF','STAFF','教务处','NORMAL',NOW(),NOW()),
(5,1,'demo_student','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','学生小李','138****0005',5,'STUDENT','STUDENT','高一(1)班','NORMAL',NOW(),NOW()),
(6,1,'demo_leader','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','校领导','138****0006',6,'LEADER','LEADER','校办','NORMAL',NOW(),NOW()),
(7,2,'univ_admin','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','示范大学管理员','139****0001',11,'TENANT_ADMIN','ADMIN','保卫处','NORMAL',NOW(),NOW()),
(8,2,'univ_guard','$2y$10$kR3GhaaZgP40YaniiaqgHOOIp0d6IMesBexbNRryEIlbksekfKrY6','大学保安','139****0002',13,'GUARD','GUARD','保卫处','NORMAL',NOW(),NOW());

-- ------------------------- 租户配置 -------------------------
INSERT INTO `sys_tenant_config` (`tenant_id`,`config_key`,`config_value`,`encrypted`,`remark`,`create_time`,`update_time`) VALUES
(1,'school_name','示范中学',0,'学校名称',NOW(),NOW()),
(1,'duty_phone','0571-88880001',0,'值班电话',NOW(),NOW()),
(1,'logo','https://wx-bwc-api.greathiit.com/static/logo-demo.png',0,'Logo',NOW(),NOW()),
(1,'official_account','demo-school',0,'公众号',NOW(),NOW()),
(1,'miniprogram','gh_demo_school',0,'小程序原始ID',NOW(),NOW()),
(1,'notice_enabled','true',0,'通知开关',NOW(),NOW()),
(1,'parking_fee_enabled','false',0,'停车收费开关',NOW(),NOW()),
(1,'portal_intro','示范中学坚持“平安校园”建设，构建门禁访客、消防隐患、巡逻防控一体化安防体系。',0,'门户简介',NOW(),NOW()),
(2,'school_name','示范大学',0,'学校名称',NOW(),NOW()),
(2,'duty_phone','0571-88880002',0,'值班电话',NOW(),NOW()),
(2,'notice_enabled','true',0,'通知开关',NOW(),NOW()),
(2,'portal_intro','示范大学保卫处为全校师生提供安全、便捷的校园环境。',0,'门户简介',NOW(),NOW());

INSERT INTO `sys_storage_config` (`tenant_id`,`driver`,`config_json`,`enabled`,`create_time`,`update_time`) VALUES
(1,'local','{"basePath":"./uploads","publicUrl":"/uploads"}',1,NOW(),NOW());

INSERT INTO `sys_wechat_config` (`tenant_id`,`app_type`,`app_id`,`app_secret_mask`,`token`,`server_url`,`auth_domain`,`js_domain`,`remark`,`create_time`,`update_time`) VALUES
(1,'MP','wx_mp_demo','****','bwc-token','https://wx-bwc-api.greathiit.com/api/wechat/callback','wx-bwc-api.greathiit.com','wx-bwc-api.greathiit.com','示范中学服务号',NOW(),NOW()),
(1,'MINI','wx_mini_demo','****','bwc-mini-token','https://wx-bwc-api.greathiit.com/api/wechat/mini/callback','wx-bwc-api.greathiit.com','wx-bwc-api.greathiit.com','示范中学校园小程序',NOW(),NOW()),
(1,'OPEN','wx_open_demo','****',NULL,NULL,NULL,NULL,'微信开放平台 UnionID 绑定',NOW(),NOW());

INSERT INTO `sys_message_template` (`tenant_id`,`biz_type`,`channel`,`template_id`,`title`,`fields_json`,`enabled`,`create_time`,`update_time`) VALUES
(1,'VISITOR_APPROVE','MP','tpl_visitor_approve_demo','访客审批结果','{"first":"您的访客预约已审批","keyword1":"访客姓名","keyword2":"来访时间","keyword3":"审批结果","remark":"请凭通行码入校"}',1,NOW(),NOW()),
(1,'TODO','MINI','tpl_todo_demo','待办审批提醒','{"thing1":"待办事项","time2":"截止时间"}',1,NOW(),NOW()),
(1,'HAZARD','MP','tpl_hazard_demo','隐患整改督办','{"thing1":"隐患位置","phrase2":"隐患等级","time3":"整改期限"}',1,NOW(),NOW()),
(1,'EVENT','MINI','tpl_event_demo','事件分派通知','{"thing1":"事件类型","thing2":"事发地点","thing3":"处置要求"}',1,NOW(),NOW()),
(1,'LOST','MP','tpl_lost_demo','失物认领通知','{"thing1":"物品名称","thing2":"认领地点"}',1,NOW(),NOW()),
(1,'WARNING','MP','tpl_warning_demo','安全预警','{"thing1":"预警类型","thing2":"预警内容"}',1,NOW(),NOW()),
(1,'EXPIRE','MP','tpl_expire_demo','到期提醒','{"thing1":"提醒对象","date2":"到期日期"}',1,NOW(),NOW());

-- ------------------------- 示例业务数据 -------------------------
INSERT INTO `bwc_visitor_appointment` (`id`,`tenant_id`,`visitor_name`,`id_card_mask`,`phone_mask`,`reason`,`visit_time`,`leave_time`,`accompany_count`,`plate_no`,`visit_user_id`,`visit_user_name`,`status`,`approve_time`,`pass_code`,`pass_expire_time`,`source`,`create_time`,`update_time`) VALUES
(1,1,'赵访客','330***********1234','138****1234','家长会','2026-10-30 09:00:00','2026-10-30 11:00:00',1,'浙A12345',4,'张老师','APPROVED','2026-10-29 10:00:00','PASS202610300001','2026-10-30 23:59:59','H5',NOW(),NOW()),
(2,1,'钱访客','330***********5678','139****5678','业务洽谈','2026-10-29 14:00:00','2026-10-29 16:00:00',0,NULL,1,'示范中学管理员','PENDING',NULL,NULL,NULL,'MINI',NOW(),NOW()),
(3,1,'孙访客','330***********9012','137****9012','维修设备','2026-10-28 10:00:00','2026-10-28 12:00:00',0,NULL,1,'示范中学管理员','REJECTED','2026-10-28 09:00:00',NULL,NULL,'PC',NOW(),NOW());

INSERT INTO `bwc_visit_record` (`tenant_id`,`appointment_id`,`visitor_name`,`pass_code`,`gate`,`in_time`,`out_time`,`operator`,`method`,`status`,`create_time`,`update_time`) VALUES
(1,1,'赵访客','PASS202610300001','南门','2026-10-30 09:05:00',NULL,'保安员小王','QR','IN',NOW(),NOW()),
(1,3,'孙访客',NULL,'东门','2026-10-28 10:10:00','2026-10-28 11:30:00','保安员小王','MANUAL','OUT',NOW(),NOW());

INSERT INTO `bwc_blacklist` (`tenant_id`,`name`,`id_card_mask`,`reason`,`level`,`operator`,`status`,`create_time`,`update_time`) VALUES
(1,'周某某','330***********3333','多次违规入校','SERIOUS','demo_admin','ACTIVE',NOW(),NOW());

INSERT INTO `bwc_key_person` (`tenant_id`,`name`,`id_card_mask`,`phone_mask`,`category`,`remark`,`status`,`create_time`,`update_time`) VALUES
(1,'吴某某','330***********4444','136****4444','校外重点关注','需核实来访目的','ACTIVE',NOW(),NOW());

INSERT INTO `bwc_temp_entry` (`tenant_id`,`visitor_name`,`id_card_mask`,`phone_mask`,`reason`,`visit_user`,`in_time`,`operator`,`status`,`create_time`,`update_time`) VALUES
(1,'郑访客','330***********5555','135****5555','临时送材料','张老师','2026-10-29 08:30:00','保安员小王','IN',NOW(),NOW());

INSERT INTO `bwc_late_return` (`tenant_id`,`student_id`,`student_name`,`dorm`,`type`,`record_time`,`reason`,`confirm_status`,`create_time`,`update_time`) VALUES
(1,5,'学生小李','1号楼302','LATE','2026-10-28 22:40:00','参加社团活动','PENDING',NOW(),NOW());

INSERT INTO `bwc_vehicle` (`tenant_id`,`plate_no`,`owner_name`,`owner_id`,`owner_phone_mask`,`vehicle_type`,`valid_start`,`valid_end`,`status`,`auth_status`,`remark`,`create_time`,`update_time`) VALUES
(1,'浙A88888','张老师',4,'138****0004','FIXED','2026-01-01 00:00:00','2027-12-31 23:59:59','NORMAL','ACTIVE','教职工车辆',NOW(),NOW()),
(1,'浙A66666','校办',NULL,'138****0006','FIXED','2026-01-01 00:00:00','2027-12-31 23:59:59','NORMAL','NONE','公务车辆',NOW(),NOW());

INSERT INTO `bwc_vehicle_appointment` (`tenant_id`,`plate_no`,`visitor_name`,`phone_mask`,`reason`,`visit_time`,`leave_time`,`status`,`auth_status`,`create_time`,`update_time`) VALUES
(1,'浙B12345','钱访客','139****5678','商务来访','2026-10-30 09:00:00','2026-10-30 12:00:00','PENDING','NONE',NOW(),NOW());

INSERT INTO `bwc_violation` (`tenant_id`,`plate_no`,`location`,`violate_time`,`description`,`handle_status`,`operator`,`create_time`,`update_time`) VALUES
(1,'浙B99999','教学楼消防通道',NOW(),'占用消防通道','PENDING','保安员小王',NOW(),NOW());

INSERT INTO `bwc_vehicle_log` (`tenant_id`,`plate_no`,`camera_id`,`camera_name`,`in_out`,`trigger_time`,`is_whitelist`,`create_time`,`update_time`) VALUES
(1,'浙A88888','CAM-SOUTH-01','南门相机','in','2026-10-29 08:00:00',1,NOW(),NOW()),
(1,'浙B12345','CAM-SOUTH-01','南门相机','out','2026-10-29 18:00:00',0,NOW(),NOW());

INSERT INTO `bwc_plate_config` (`tenant_id`,`base_url`,`api_key_mask`,`status`,`mqtt_connected`,`create_time`,`update_time`) VALUES
(1,'http://172.16.28.201:8000','****','DISABLED',0,NOW(),NOW());

INSERT INTO `bwc_plate_camera` (`tenant_id`,`cam_id`,`name`,`campus`,`cam_type`,`remark`,`enabled`,`sync_status`,`create_time`,`update_time`) VALUES
(1,'CAM-SOUTH-01','南门相机','主校区','ENTRANCE','南门入口',1,'SYNCED',NOW(),NOW()),
(1,'CAM-NORTH-01','北门相机','主校区','EXIT','北门出口',1,'SYNCED',NOW(),NOW());

INSERT INTO `bwc_security_event` (`id`,`tenant_id`,`event_no`,`event_type`,`location`,`longitude`,`latitude`,`description`,`urgency`,`reporter`,`status`,`assign_to_name`,`assign_time`,`source`,`create_time`,`update_time`) VALUES
(1,1,'DEMO-SE-20261029-0001','纠纷','食堂门口',120.123456,30.123456,'学生之间发生口角','URGENT','保安员小王','ASSIGNED','保安员小王','2026-10-29 11:00:00','REPORT',NOW(),NOW()),
(2,1,'DEMO-SE-20261028-0002','求助','教学楼三楼',120.123500,30.123500,'学生突发身体不适','URGENT','张老师','CLOSED','保安员小王','2026-10-28 15:00:00','REPORT',NOW(),NOW());

INSERT INTO `bwc_work_order` (`tenant_id`,`order_no`,`event_id`,`title`,`assign_to_name`,`status`,`deadline`,`create_time`,`update_time`) VALUES
(1,'DEMO-WO-20261029-0001',1,'食堂门口纠纷处置','保安员小王','HANDLING','2026-10-29 12:00:00',NOW(),NOW());

INSERT INTO `bwc_dispute` (`tenant_id`,`title`,`parties`,`location`,`occur_time`,`description`,`mediator`,`status`,`create_time`,`update_time`) VALUES
(1,'食堂排队纠纷','高一(1)班 小李 / 高一(2)班 小王','食堂','2026-10-29 11:20:00','排队碰撞引发口角','demo_captain','MEDIATING',NOW(),NOW());

INSERT INTO `bwc_fire_facility` (`id`,`tenant_id`,`facility_no`,`type`,`location`,`qr_code`,`production_date`,`expire_date`,`last_check_date`,`next_check_date`,`status`,`responsible`,`create_time`,`update_time`) VALUES
(1,1,'FF-2026-0001','灭火器','教学楼一楼东侧','QR-FF-2026-0001','2025-01-01','2027-01-01','2026-09-01','2026-11-01','NORMAL','张老师',NOW(),NOW()),
(2,1,'FF-2026-0002','消火栓','宿舍楼二楼','QR-FF-2026-0002','2024-06-01','2026-11-10','2026-08-01','2026-10-30','NORMAL','保安员小王',NOW(),NOW());

INSERT INTO `bwc_fire_inspection` (`tenant_id`,`facility_id`,`facility_no`,`inspector`,`inspect_time`,`result`,`abnormal_flag`,`remark`,`create_time`,`update_time`) VALUES
(1,1,'FF-2026-0001','保安员小王','2026-10-28 09:00:00','NORMAL',0,'压力正常',NOW(),NOW());

INSERT INTO `bwc_hazard` (`tenant_id`,`hazard_no`,`source`,`hazard_level`,`location`,`description`,`reporter`,`report_time`,`assign_to_name`,`assign_time`,`deadline`,`status`,`create_time`,`update_time`) VALUES
(1,'DEMO-HZ-20261027-0001','INSPECTION','MAJOR','宿舍楼二楼','消火栓压力不足，箱门损坏','保安员小王','2026-10-27 10:00:00','保安员小王','2026-10-27 10:30:00','2026-11-03 23:59:59','RECTIFYING',NOW(),NOW()),
(1,'DEMO-HZ-20261025-0002','REPORT','GENERAL','教学楼走廊','应急灯不亮','张老师','2026-10-25 14:00:00','保安员小王','2026-10-25 15:00:00','2026-11-01 23:59:59','CLOSED',NOW(),NOW());

INSERT INTO `bwc_fire_drill` (`tenant_id`,`title`,`drill_time`,`location`,`participants`,`sign_count`,`summary`,`status`,`create_time`,`update_time`) VALUES
(1,'秋季消防疏散演练','2026-10-20 15:00:00','教学楼','300',286,'演练顺利完成','FINISHED',NOW(),NOW());

INSERT INTO `bwc_fire_training` (`tenant_id`,`title`,`trainer`,`training_time`,`location`,`participants`,`content`,`status`,`create_time`,`update_time`) VALUES
(1,'消防器材使用培训','消防大队','2026-10-18 14:00:00','报告厅',120,'灭火器操作与疏散要领','FINISHED',NOW(),NOW());

INSERT INTO `bwc_power_check` (`tenant_id`,`location`,`check_time`,`checker`,`result`,`violation_desc`,`handle_result`,`create_time`,`update_time`) VALUES
(1,'男生宿舍302','2026-10-26 21:00:00','保安员小王','ABNORMAL','使用违规大功率电器','已收缴并教育',NOW(),NOW());

INSERT INTO `bwc_fire_alarm` (`tenant_id`,`alarm_type`,`location`,`alarm_time`,`device_id`,`content`,`status`,`create_time`,`update_time`) VALUES
(1,'SMOKE','教学楼三楼','2026-10-29 12:10:00','SM-0301','烟感报警',  'PENDING',NOW(),NOW());

INSERT INTO `bwc_patrol_route` (`id`,`tenant_id`,`name`,`description`,`status`,`create_time`,`update_time`) VALUES
(1,1,'校园夜间巡逻路线','覆盖教学楼、宿舍、操场','NORMAL',NOW(),NOW());

INSERT INTO `bwc_patrol_point` (`id`,`tenant_id`,`route_id`,`name`,`location`,`qr_code`,`longitude`,`latitude`,`time_window_start`,`time_window_end`,`sort`,`status`,`create_time`,`update_time`) VALUES
(1,1,1,'教学楼点位','教学楼一楼','QR-PT-001',120.1234,30.1234,'22:00','22:30',1,'NORMAL',NOW(),NOW()),
(2,1,1,'宿舍楼点位','宿舍楼门口','QR-PT-002',120.1235,30.1235,'22:30','23:00',2,'NORMAL',NOW(),NOW());

INSERT INTO `bwc_patrol_record` (`tenant_id`,`route_id`,`point_id`,`guard_id`,`guard_name`,`check_time`,`method`,`status`,`create_time`,`update_time`) VALUES
(1,1,1,3,'保安员小王','2026-10-28 22:10:00','QR','NORMAL',NOW(),NOW()),
(1,1,2,3,'保安员小王','2026-10-28 22:45:00','QR','ABNORMAL',NOW(),NOW());

INSERT INTO `bwc_dorm_check` (`tenant_id`,`dorm`,`check_date`,`checker`,`should_count`,`actual_count`,`absent_names`,`status`,`create_time`,`update_time`) VALUES
(1,'1号楼302','2026-10-28','保安员小王',6,5,'学生小李','ABNORMAL',NOW(),NOW());

INSERT INTO `bwc_lost_item` (`id`,`tenant_id`,`item_no`,`name`,`category`,`location`,`found_time`,`description`,`finder`,`status`,`storage_location`,`create_time`,`update_time`) VALUES
(1,1,'LF-2026-0001','校园卡','CARD','图书馆','2026-10-28 10:00:00','姓名模糊，卡号尾号1234','图书馆管理员','PUBLISHED','保卫处失物柜',NOW(),NOW()),
(2,1,'LF-2026-0002','黑色雨伞','OTHER','食堂','2026-10-27 18:00:00','长柄黑伞','保安员小王','CLAIMED','保卫处失物柜',NOW(),NOW());

INSERT INTO `bwc_lost_notice` (`tenant_id`,`item_id`,`title`,`content`,`publish_time`,`publisher`,`status`,`views`,`create_time`,`update_time`) VALUES
(1,1,'失物招领：校园卡一张','请失主携带有效证件到保卫处认领。','2026-10-28 11:00:00','demo_admin','PUBLISHED',56,NOW(),NOW());

INSERT INTO `bwc_lost_claim` (`tenant_id`,`item_id`,`claimant`,`phone_mask`,`id_card_mask`,`claim_time`,`status`,`create_time`,`update_time`) VALUES
(1,2,'李同学','138****0005','330***********8888','2026-10-28 19:00:00','APPROVED',NOW(),NOW());

INSERT INTO `bwc_safety_course` (`id`,`tenant_id`,`title`,`category`,`content`,`status`,`publish_time`,`create_time`,`update_time`) VALUES
(1,1,'秋季开学安全教育第一课','安全教育','<h3>防火防盗防诈骗</h3><p>请同学们认真学习...</p>','PUBLISHED','2026-09-01 09:00:00',NOW(),NOW());

INSERT INTO `bwc_course_study` (`tenant_id`,`course_id`,`user_id`,`user_name`,`progress`,`study_time`,`finish_flag`,`create_time`,`update_time`) VALUES
(1,1,5,'学生小李',100,'2026-09-02 10:00:00',1,NOW(),NOW());

INSERT INTO `bwc_exam` (`id`,`tenant_id`,`title`,`course_id`,`total_score`,`pass_score`,`questions_json`,`status`,`start_time`,`end_time`,`create_time`,`update_time`) VALUES
(1,1,'新生安全知识考试',1,100,60,'[{"id":1,"q":"发生火灾应拨打?","options":["110","119","120"],"answer":"119","score":50},{"id":2,"q":"灭火器使用口诀是?","options":["提拔握压","摇一摇","直接喷"],"answer":"提拔握压","score":50}]','PUBLISHED','2026-09-01 00:00:00','2026-12-31 23:59:59',NOW(),NOW());

INSERT INTO `bwc_exam_record` (`tenant_id`,`exam_id`,`user_id`,`user_name`,`score`,`pass_flag`,`submit_time`,`create_time`,`update_time`) VALUES
(1,1,5,'学生小李',100,1,'2026-09-03 10:00:00',NOW(),NOW());

INSERT INTO `bwc_warning` (`tenant_id`,`type`,`title`,`content`,`warning_level`,`publish_time`,`publisher`,`status`,`create_time`,`update_time`) VALUES
(1,'诈骗','警惕冒充老师收取资料费诈骗','近期发现冒充班主任在家长群收费的诈骗行为，请家长通过官方渠道核实。','HIGH','2026-10-28 09:00:00','demo_admin','PUBLISHED',NOW(),NOW());

INSERT INTO `bwc_notice` (`tenant_id`,`title`,`content`,`category`,`publish_time`,`publisher`,`status`,`views`,`create_time`,`update_time`) VALUES
(1,'关于开展校园消防安全检查的通知','<p>定于10月30日开展全校消防安全检查...</p>','通知公告','2026-10-28 08:00:00','demo_admin','PUBLISHED',123,NOW(),NOW());

INSERT INTO `bwc_material` (`tenant_id`,`title`,`category`,`file_url`,`description`,`status`,`create_time`,`update_time`) VALUES
(1,'消防安全宣传海报','海报','https://wx-bwc-api.greathiit.com/static/material-fire.png','校园消防宣传','NORMAL',NOW(),NOW());

INSERT INTO `bwc_report_apply` (`id`,`tenant_id`,`report_no`,`type`,`title`,`applicant`,`content`,`start_time`,`end_time`,`location`,`safety_plan`,`status`,`create_time`,`update_time`) VALUES
(1,1,'DEMO-RA-20261028-0001','ACTIVITY','秋季运动会','张老师','运动会安全方案','2026-11-01 08:00:00','2026-11-02 18:00:00','操场','设置医疗点、安保巡逻','PENDING',NOW(),NOW());

INSERT INTO `bwc_monitor_access` (`id`,`tenant_id`,`apply_no`,`applicant`,`reason`,`area`,`time_range`,`approve_level`,`status`,`create_time`,`update_time`) VALUES
(1,1,'DEMO-MA-20261028-0001','张老师','查找失物线索','教学楼一楼','2026-10-28 08:00 ~ 12:00','DEPT','PENDING',NOW(),NOW());

INSERT INTO `bwc_device` (`id`,`tenant_id`,`device_no`,`type`,`name`,`location`,`vendor`,`model`,`install_date`,`status`,`create_time`,`update_time`) VALUES
(1,1,'DEV-CAM-001','CAMERA','南门监控','南门','海康','DS-2CD','2025-08-01','NORMAL',NOW(),NOW()),
(2,1,'DEV-GATE-001','GATE','南门道闸','南门','捷顺','JS-200','2025-08-01','FAULT',NOW(),NOW());

INSERT INTO `bwc_device_repair` (`tenant_id`,`repair_no`,`device_id`,`device_name`,`fault_desc`,`reporter`,`report_time`,`status`,`assign_to`,`create_time`,`update_time`) VALUES
(1,'DEMO-DR-20261029-0001',2,'南门道闸','道闸无法抬杆','保安员小王','2026-10-29 08:00:00','ASSIGNED','维保单位',NOW(),NOW());

INSERT INTO `bwc_device_inspection` (`tenant_id`,`device_id`,`inspector`,`inspect_time`,`result`,`remark`,`create_time`,`update_time`) VALUES
(1,1,'保安员小王','2026-10-28 09:00:00','NORMAL','运行正常',NOW(),NOW());

INSERT INTO `bwc_emergency_plan` (`tenant_id`,`title`,`type`,`content`,`version`,`status`,`publish_time`,`create_time`,`update_time`) VALUES
(1,'校园火灾应急预案','火灾','<p>一、报警 二、疏散 三、扑救</p>','V1.0','PUBLISHED','2026-01-10 09:00:00',NOW(),NOW());

INSERT INTO `bwc_emergency_supply` (`id`,`tenant_id`,`name`,`category`,`spec`,`quantity`,`unit`,`location`,`status`,`create_time`,`update_time`) VALUES
(1,1,'防暴盾牌','防暴','标准',10,'面','保卫处器材室','NORMAL',NOW(),NOW());

INSERT INTO `bwc_supply_record` (`tenant_id`,`supply_id`,`type`,`quantity`,`user_name`,`record_time`,`operator`,`remark`,`create_time`,`update_time`) VALUES
(1,1,'BORROW',2,'保安员小王','2026-10-28 09:00:00','demo_captain','巡逻使用',NOW(),NOW());

INSERT INTO `bwc_emergency_contact` (`tenant_id`,`name`,`dept`,`duty`,`phone`,`sort`,`create_time`,`update_time`) VALUES
(1,'张主任','保卫处','值班负责人','0571-88880001',1,NOW(),NOW());

INSERT INTO `bwc_emergency_event` (`tenant_id`,`title`,`type`,`location`,`occur_time`,`description`,`response`,`status`,`reporter`,`create_time`,`update_time`) VALUES
(1,'停电应急事件','极端天气','教学楼','2026-10-20 19:00:00','雷雨导致停电','启动应急照明并疏散','CLOSED','demo_admin',NOW(),NOW());

INSERT INTO `bwc_schedule` (`tenant_id`,`guard_id`,`guard_name`,`shift_date`,`shift_type`,`start_time`,`end_time`,`remark`,`create_time`,`update_time`) VALUES
(1,3,'保安员小王','2026-10-30','NIGHT','18:00','06:00','夜班巡逻',NOW(),NOW());

INSERT INTO `bwc_attendance` (`tenant_id`,`guard_id`,`guard_name`,`check_time`,`type`,`method`,`status`,`create_time`,`update_time`) VALUES
(1,3,'保安员小王','2026-10-29 18:00:00','ON','APP','NORMAL',NOW(),NOW());

INSERT INTO `bwc_performance` (`tenant_id`,`guard_id`,`guard_name`,`month`,`score`,`items_json`,`remark`,`create_time`,`update_time`) VALUES
(1,3,'保安员小王','2026-10',92,'{"出勤":95,"巡逻":90,"事件处置":92}','表现良好',NOW(),NOW());

INSERT INTO `bwc_feedback` (`tenant_id`,`type`,`title`,`content`,`contact`,`submit_time`,`status`,`create_time`,`update_time`) VALUES
(1,'SUGGESTION','建议增加夜间照明','操场东侧夜间较暗','138****0004','2026-10-28 20:00:00','PENDING',NOW(),NOW());

INSERT INTO `bwc_monthly_report` (`tenant_id`,`month`,`content_json`,`create_time`,`update_time`) VALUES
(1,'2026-09','{"visitorCount":120,"eventCount":8,"hazardClosedRate":0.9,"patrolRate":0.95,"fireIntactRate":0.98,"lostClaimRate":0.8,"alarmCount":2}',NOW(),NOW());
