-- ============================================================================
-- 校园保卫安防管理系统（SaaS）数据库结构
-- MySQL 8.0 / utf8mb4 / utf8mb4_unicode_ci
-- 说明：所有业务表均含 tenant_id 行级隔离字段；平台级表由租户插件白名单排除。
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `bwc` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `bwc`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 平台级表（不走租户隔离）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '固定0',
  `name` VARCHAR(128) NOT NULL COMMENT '学校名称',
  `code` VARCHAR(32) NOT NULL COMMENT '租户编码',
  `type` VARCHAR(20) NOT NULL DEFAULT 'SCHOOL' COMMENT '中小学/高校/教育局',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'TRIAL/NORMAL/DISABLED',
  `open_time` DATETIME NULL,
  `expire_time` DATETIME NULL,
  `user_quota` INT NOT NULL DEFAULT 100,
  `contact` VARCHAR(64) NULL,
  `phone` VARCHAR(32) NULL,
  `remark` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`code`),
  KEY `idx_tenant_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户（学校）';

DROP TABLE IF EXISTS `sys_platform_user`;
CREATE TABLE `sys_platform_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt',
  `real_name` VARCHAR(64) NULL,
  `role_code` VARCHAR(32) NOT NULL DEFAULT 'PLATFORM_OPERATOR' COMMENT 'SYSTEM/PLATFORM_OPERATOR',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `last_login_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户';

DROP TABLE IF EXISTS `sys_global_dict`;
CREATE TABLE `sys_global_dict` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `dict_type` VARCHAR(64) NOT NULL,
  `dict_label` VARCHAR(128) NOT NULL,
  `dict_value` VARCHAR(64) NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局标准字典';

DROP TABLE IF EXISTS `sys_platform_param`;
CREATE TABLE `sys_platform_param` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `param_key` VARCHAR(64) NOT NULL,
  `param_name` VARCHAR(128) NOT NULL,
  `param_value` VARCHAR(500) NULL,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_param_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台参数';

DROP TABLE IF EXISTS `sys_language`;
CREATE TABLE `sys_language` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `lang_code` VARCHAR(16) NOT NULL,
  `lang_name` VARCHAR(64) NOT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `sort` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lang_code` (`lang_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语言';

-- 菜单：tenant_id=0 为平台默认模板，租户可覆盖
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `platform` VARCHAR(20) NOT NULL DEFAULT 'admin' COMMENT 'admin/miniprogram/h5/app',
  `parent_id` BIGINT NOT NULL DEFAULT 0,
  `name` VARCHAR(64) NOT NULL,
  `name_en` VARCHAR(64) NULL,
  `path` VARCHAR(255) NULL,
  `icon` VARCHAR(64) NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `menu_type` VARCHAR(20) NOT NULL DEFAULT 'MENU' COMMENT 'MENU/BUTTON',
  `permission` VARCHAR(64) NULL,
  `visible` TINYINT NOT NULL DEFAULT 1,
  `config_json` TEXT NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_menu_platform` (`platform`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单可视化配置';

-- ---------------------------------------------------------------------------
-- 租户级系统表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `role_code` VARCHAR(32) NOT NULL,
  `role_name` VARCHAR(64) NOT NULL,
  `data_scope` VARCHAR(20) NOT NULL DEFAULT 'TENANT' COMMENT 'TENANT/SELF/ALL',
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_role_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_rm_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单';

DROP TABLE IF EXISTS `sys_tenant_user`;
CREATE TABLE `sys_tenant_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `real_name` VARCHAR(64) NULL,
  `phone_enc` VARCHAR(255) NULL,
  `phone_mask` VARCHAR(32) NULL,
  `id_card_enc` VARCHAR(255) NULL,
  `id_card_mask` VARCHAR(32) NULL,
  `role_id` BIGINT NULL,
  `role_code` VARCHAR(32) NULL,
  `user_type` VARCHAR(20) NOT NULL DEFAULT 'STAFF' COMMENT 'ADMIN/CAPTAIN/GUARD/STAFF/STUDENT/LEADER/VISITEE',
  `dept` VARCHAR(128) NULL,
  `openid` VARCHAR(64) NULL,
  `unionid` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `last_login_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tu_tenant` (`tenant_id`),
  KEY `idx_tu_username` (`username`),
  KEY `idx_tu_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户用户';

DROP TABLE IF EXISTS `sys_tenant_dict`;
CREATE TABLE `sys_tenant_dict` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `dict_type` VARCHAR(64) NOT NULL,
  `dict_label` VARCHAR(128) NOT NULL,
  `dict_value` VARCHAR(64) NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_td_tenant_type` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户自定义字典';

DROP TABLE IF EXISTS `sys_tenant_config`;
CREATE TABLE `sys_tenant_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `config_key` VARCHAR(64) NOT NULL,
  `config_value` TEXT NULL,
  `encrypted` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tc_key` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户配置';

DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `user_id` BIGINT NULL,
  `username` VARCHAR(64) NULL,
  `action` VARCHAR(32) NOT NULL,
  `module` VARCHAR(64) NOT NULL,
  `target` VARCHAR(255) NULL,
  `detail` TEXT NULL,
  `ip` VARCHAR(64) NULL,
  `user_agent` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_audit_tenant` (`tenant_id`,`create_time`),
  KEY `idx_audit_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

DROP TABLE IF EXISTS `sys_attachment`;
CREATE TABLE `sys_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `biz_type` VARCHAR(64) NULL,
  `biz_id` BIGINT NULL,
  `file_name` VARCHAR(255) NULL,
  `url` VARCHAR(500) NOT NULL,
  `size` BIGINT NULL,
  `content_type` VARCHAR(128) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_att_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件';

DROP TABLE IF EXISTS `sys_storage_config`;
CREATE TABLE `sys_storage_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `driver` VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT 'local/s3/oss/cos/minio',
  `config_json` TEXT NULL COMMENT 'AES加密后的配置',
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `last_test_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_storage_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象存储配置';

DROP TABLE IF EXISTS `sys_wechat_config`;
CREATE TABLE `sys_wechat_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `app_type` VARCHAR(20) NOT NULL COMMENT 'MP/MINI/OPEN',
  `app_id` VARCHAR(128) NULL,
  `app_secret_enc` VARCHAR(500) NULL,
  `app_secret_mask` VARCHAR(64) NULL,
  `token` VARCHAR(128) NULL,
  `aes_key_enc` VARCHAR(500) NULL,
  `server_url` VARCHAR(255) NULL,
  `auth_domain` VARCHAR(255) NULL,
  `js_domain` VARCHAR(255) NULL,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wx_tenant_type` (`tenant_id`,`app_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信账号配置';

DROP TABLE IF EXISTS `sys_message_template`;
CREATE TABLE `sys_message_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `biz_type` VARCHAR(64) NOT NULL COMMENT 'VISITOR_APPROVE/TODO/HAZARD/EVENT/LOST/WARNING/EXPIRE',
  `channel` VARCHAR(20) NOT NULL DEFAULT 'MP' COMMENT 'MP/MINI/SMS/STATION',
  `template_id` VARCHAR(128) NULL,
  `title` VARCHAR(128) NULL,
  `fields_json` TEXT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mt_tenant` (`tenant_id`,`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板';

DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `user_id` BIGINT NULL,
  `title` VARCHAR(128) NOT NULL,
  `content` TEXT NULL,
  `biz_type` VARCHAR(64) NULL,
  `biz_id` BIGINT NULL,
  `read_flag` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_msg_user` (`tenant_id`,`user_id`,`read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息';

DROP TABLE IF EXISTS `sys_message_log`;
CREATE TABLE `sys_message_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `channel` VARCHAR(20) NOT NULL,
  `biz_type` VARCHAR(64) NULL,
  `template_id` VARCHAR(128) NULL,
  `to_user` VARCHAR(128) NULL,
  `content` TEXT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'SENT',
  `error` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mlog_tenant` (`tenant_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送日志';

-- ---------------------------------------------------------------------------
-- 门禁与访客
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_visitor_appointment`;
CREATE TABLE `bwc_visitor_appointment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `visitor_name` VARCHAR(64) NOT NULL,
  `id_card_enc` VARCHAR(255) NULL,
  `id_card_mask` VARCHAR(32) NULL,
  `phone_enc` VARCHAR(255) NULL,
  `phone_mask` VARCHAR(32) NULL,
  `reason` VARCHAR(255) NULL,
  `visit_time` DATETIME NULL,
  `leave_time` DATETIME NULL,
  `accompany_count` INT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NULL,
  `visit_user_id` BIGINT NULL,
  `visit_user_name` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED/EXPIRED',
  `approve_by` BIGINT NULL,
  `approve_time` DATETIME NULL,
  `approve_remark` VARCHAR(255) NULL,
  `pass_code` VARCHAR(64) NULL,
  `pass_expire_time` DATETIME NULL,
  `source` VARCHAR(20) NOT NULL DEFAULT 'MINI' COMMENT 'MINI/MP/H5/PC',
  `remark` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_va_tenant` (`tenant_id`,`status`),
  KEY `idx_va_pass` (`pass_code`),
  KEY `idx_va_phone` (`phone_mask`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客预约单';

DROP TABLE IF EXISTS `bwc_visit_record`;
CREATE TABLE `bwc_visit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `appointment_id` BIGINT NULL,
  `visitor_name` VARCHAR(64) NULL,
  `pass_code` VARCHAR(64) NULL,
  `gate` VARCHAR(64) NULL,
  `in_time` DATETIME NULL,
  `out_time` DATETIME NULL,
  `operator` VARCHAR(64) NULL,
  `method` VARCHAR(20) NOT NULL DEFAULT 'QR' COMMENT 'QR/MANUAL/FACE',
  `status` VARCHAR(20) NOT NULL DEFAULT 'IN' COMMENT 'IN/OUT/DENIED',
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_vr_tenant_time` (`tenant_id`,`in_time`),
  KEY `idx_vr_appointment` (`appointment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通行记录';

DROP TABLE IF EXISTS `bwc_blacklist`;
CREATE TABLE `bwc_blacklist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `name` VARCHAR(64) NOT NULL,
  `id_card_mask` VARCHAR(32) NULL,
  `phone_mask` VARCHAR(32) NULL,
  `reason` VARCHAR(255) NULL,
  `level` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/SERIOUS',
  `operator` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bl_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单';

DROP TABLE IF EXISTS `bwc_key_person`;
CREATE TABLE `bwc_key_person` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `name` VARCHAR(64) NOT NULL,
  `id_card_mask` VARCHAR(32) NULL,
  `phone_mask` VARCHAR(32) NULL,
  `category` VARCHAR(64) NULL COMMENT '报备人员类别',
  `remark` VARCHAR(500) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_kp_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重点人员台账';

DROP TABLE IF EXISTS `bwc_temp_entry`;
CREATE TABLE `bwc_temp_entry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `visitor_name` VARCHAR(64) NOT NULL,
  `id_card_enc` VARCHAR(255) NULL,
  `id_card_mask` VARCHAR(32) NULL,
  `phone_enc` VARCHAR(255) NULL,
  `phone_mask` VARCHAR(32) NULL,
  `reason` VARCHAR(255) NULL,
  `visit_user` VARCHAR(64) NULL,
  `in_time` DATETIME NULL,
  `out_time` DATETIME NULL,
  `operator` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'IN',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_te_tenant` (`tenant_id`,`in_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='临时入校登记';

DROP TABLE IF EXISTS `bwc_late_return`;
CREATE TABLE `bwc_late_return` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `student_id` BIGINT NULL,
  `student_name` VARCHAR(64) NOT NULL,
  `dorm` VARCHAR(64) NULL,
  `type` VARCHAR(20) NOT NULL DEFAULT 'LATE' COMMENT 'LATE/NOT_RETURN',
  `record_time` DATETIME NULL,
  `reason` VARCHAR(500) NULL,
  `confirm_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED',
  `confirm_by` VARCHAR(64) NULL,
  `confirm_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_lr_tenant` (`tenant_id`,`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='晚归/不归登记';

-- ---------------------------------------------------------------------------
-- 车辆与车牌识别
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_vehicle`;
CREATE TABLE `bwc_vehicle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NOT NULL,
  `owner_name` VARCHAR(64) NULL,
  `owner_id` BIGINT NULL,
  `owner_phone_mask` VARCHAR(32) NULL,
  `vehicle_type` VARCHAR(20) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED/TEMP',
  `valid_start` DATETIME NULL,
  `valid_end` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DISABLED/EXPIRED',
  `auth_status` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PENDING/ACTIVE/FAILED/EXPIRED/REMOVED',
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_veh_tenant` (`tenant_id`,`plate_no`),
  KEY `idx_veh_status` (`auth_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校内车辆备案';

DROP TABLE IF EXISTS `bwc_vehicle_appointment`;
CREATE TABLE `bwc_vehicle_appointment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NOT NULL,
  `visitor_name` VARCHAR(64) NULL,
  `phone_mask` VARCHAR(32) NULL,
  `reason` VARCHAR(255) NULL,
  `visit_time` DATETIME NULL,
  `leave_time` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
  `approve_by` BIGINT NULL,
  `approve_time` DATETIME NULL,
  `auth_status` VARCHAR(20) NOT NULL DEFAULT 'NONE',
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_vapp_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外来车辆预约';

DROP TABLE IF EXISTS `bwc_vehicle_pass`;
CREATE TABLE `bwc_vehicle_pass` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NOT NULL,
  `type` VARCHAR(20) NOT NULL DEFAULT 'TEMP',
  `start_time` DATETIME NULL,
  `end_time` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `operator` VARCHAR(64) NULL,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_vp_tenant` (`tenant_id`,`plate_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='临时放行';

DROP TABLE IF EXISTS `bwc_violation`;
CREATE TABLE `bwc_violation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NOT NULL,
  `location` VARCHAR(128) NULL,
  `violate_time` DATETIME NULL,
  `description` VARCHAR(500) NULL,
  `photos` TEXT NULL,
  `handle_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REMINDED/HANDLED',
  `handle_result` VARCHAR(500) NULL,
  `operator` VARCHAR(64) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_vio_tenant` (`tenant_id`,`violate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违停上报';

DROP TABLE IF EXISTS `bwc_vehicle_log`;
CREATE TABLE `bwc_vehicle_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NOT NULL,
  `camera_id` VARCHAR(64) NULL,
  `camera_name` VARCHAR(128) NULL,
  `in_out` VARCHAR(10) NOT NULL DEFAULT 'in' COMMENT 'in/out',
  `trigger_time` DATETIME NULL,
  `is_whitelist` TINYINT NOT NULL DEFAULT 0,
  `image_url` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_vlog_tenant_time` (`tenant_id`,`trigger_time`),
  KEY `idx_vlog_plate` (`plate_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆进出记录';

DROP TABLE IF EXISTS `bwc_parking_fee`;
CREATE TABLE `bwc_parking_fee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NOT NULL,
  `enter_time` DATETIME NULL,
  `exit_time` DATETIME NULL,
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  `pay_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_fee_tenant` (`tenant_id`,`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='停车收费（可选）';

DROP TABLE IF EXISTS `bwc_plate_config`;
CREATE TABLE `bwc_plate_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `base_url` VARCHAR(255) NULL,
  `api_key_enc` VARCHAR(500) NULL,
  `api_key_mask` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DISABLED' COMMENT 'ENABLED/DISABLED',
  `last_test_time` DATETIME NULL,
  `mqtt_connected` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plate_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车牌识别接口配置（租户级）';

DROP TABLE IF EXISTS `bwc_plate_camera`;
CREATE TABLE `bwc_plate_camera` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `cam_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `campus` VARCHAR(128) NULL,
  `cam_type` VARCHAR(64) NULL,
  `remark` VARCHAR(255) NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `sync_status` VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/SYNCED/FAILED',
  `sync_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_cam_tenant` (`tenant_id`,`cam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车牌识别相机';

DROP TABLE IF EXISTS `bwc_plate_auth_record`;
CREATE TABLE `bwc_plate_auth_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NOT NULL,
  `plate_type` VARCHAR(20) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED/TEMP',
  `camera_id` VARCHAR(64) NULL,
  `camera_name` VARCHAR(128) NULL,
  `action` VARCHAR(20) NOT NULL COMMENT 'ADD/ADD_ALL/TEMP/DEL/DEL_ALL',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'active/expired/removed/failed/pending',
  `message` VARCHAR(500) NULL,
  `start_time` DATETIME NULL,
  `end_time` DATETIME NULL,
  `batch_no` VARCHAR(64) NULL,
  `operator` VARCHAR(64) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_par_tenant` (`tenant_id`,`plate_no`),
  KEY `idx_par_batch` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车牌下发/授权台账';

DROP TABLE IF EXISTS `bwc_plate_log`;
CREATE TABLE `bwc_plate_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `plate_no` VARCHAR(32) NOT NULL,
  `camera_id` VARCHAR(64) NULL,
  `camera_name` VARCHAR(128) NULL,
  `in_out` VARCHAR(10) NOT NULL DEFAULT 'in',
  `trigger_time` DATETIME NULL,
  `is_whitelist` TINYINT NOT NULL DEFAULT 0,
  `image_url` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_plog_tenant` (`tenant_id`,`trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相机进出日志同步';

-- ---------------------------------------------------------------------------
-- 治安事件与工单
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_security_event`;
CREATE TABLE `bwc_security_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `event_no` VARCHAR(64) NOT NULL,
  `event_type` VARCHAR(64) NOT NULL COMMENT '失窃/纠纷/求助/失物/治安',
  `location` VARCHAR(128) NULL,
  `longitude` DECIMAL(10,6) NULL,
  `latitude` DECIMAL(10,6) NULL,
  `description` TEXT NULL,
  `urgency` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/URGENT/CRITICAL',
  `photos` TEXT NULL,
  `reporter` VARCHAR(64) NULL,
  `reporter_phone_mask` VARCHAR(32) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'REPORTED' COMMENT 'REPORTED/ASSIGNED/HANDLING/CLOSED',
  `assign_to` BIGINT NULL,
  `assign_to_name` VARCHAR(64) NULL,
  `assign_time` DATETIME NULL,
  `handle_result` TEXT NULL,
  `handle_photos` TEXT NULL,
  `close_time` DATETIME NULL,
  `source` VARCHAR(20) NOT NULL DEFAULT 'REPORT' COMMENT 'REPORT/ALARM',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_se_tenant` (`tenant_id`,`status`),
  KEY `idx_se_no` (`event_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治安事件';

DROP TABLE IF EXISTS `bwc_work_order`;
CREATE TABLE `bwc_work_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `order_no` VARCHAR(64) NOT NULL,
  `event_id` BIGINT NULL,
  `title` VARCHAR(128) NOT NULL,
  `assign_to` BIGINT NULL,
  `assign_to_name` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDLING/DONE/CLOSED',
  `deadline` DATETIME NULL,
  `finish_time` DATETIME NULL,
  `result` TEXT NULL,
  `photos` TEXT NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_wo_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件工单';

DROP TABLE IF EXISTS `bwc_dispute`;
CREATE TABLE `bwc_dispute` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `parties` VARCHAR(255) NULL,
  `location` VARCHAR(128) NULL,
  `occur_time` DATETIME NULL,
  `description` TEXT NULL,
  `mediator` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/MEDIATING/MEDIATED/CLOSED',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dis_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='矛盾纠纷';

DROP TABLE IF EXISTS `bwc_mediation`;
CREATE TABLE `bwc_mediation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `dispute_id` BIGINT NOT NULL,
  `content` TEXT NULL,
  `result` VARCHAR(255) NULL,
  `mediator` VARCHAR(64) NULL,
  `mediation_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_med_dispute` (`dispute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调解记录';

DROP TABLE IF EXISTS `bwc_alarm_link`;
CREATE TABLE `bwc_alarm_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `alarm_type` VARCHAR(32) NOT NULL COMMENT 'FIRE/ACCESS/DEVICE',
  `source` VARCHAR(64) NULL,
  `event_id` BIGINT NULL,
  `content` VARCHAR(500) NULL,
  `link_time` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_al_tenant` (`tenant_id`,`link_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警联动记录';

-- ---------------------------------------------------------------------------
-- 消防
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_fire_facility`;
CREATE TABLE `bwc_fire_facility` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `facility_no` VARCHAR(64) NOT NULL,
  `type` VARCHAR(64) NOT NULL COMMENT '灭火器/烟感/消火栓',
  `location` VARCHAR(128) NULL,
  `qr_code` VARCHAR(128) NULL,
  `production_date` DATE NULL,
  `expire_date` DATE NULL,
  `last_check_date` DATE NULL,
  `next_check_date` DATE NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/ABNORMAL/EXPIRED',
  `responsible` VARCHAR(64) NULL,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ff_no` (`tenant_id`,`facility_no`),
  KEY `idx_ff_qr` (`qr_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消防设施台账';

DROP TABLE IF EXISTS `bwc_fire_inspection`;
CREATE TABLE `bwc_fire_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `facility_id` BIGINT NULL,
  `facility_no` VARCHAR(64) NULL,
  `inspector` VARCHAR(64) NULL,
  `inspect_time` DATETIME NULL,
  `result` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/ABNORMAL',
  `photos` TEXT NULL,
  `abnormal_flag` TINYINT NOT NULL DEFAULT 0,
  `hazard_id` BIGINT NULL,
  `remark` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_fi_tenant` (`tenant_id`,`inspect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消防巡检记录';

DROP TABLE IF EXISTS `bwc_hazard`;
CREATE TABLE `bwc_hazard` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `hazard_no` VARCHAR(64) NOT NULL,
  `source` VARCHAR(20) NOT NULL DEFAULT 'REPORT' COMMENT 'INSPECTION/REPORT',
  `hazard_level` VARCHAR(20) NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL/MAJOR/CRITICAL',
  `location` VARCHAR(128) NULL,
  `description` TEXT NULL,
  `photos` TEXT NULL,
  `reporter` VARCHAR(64) NULL,
  `report_time` DATETIME NULL,
  `assign_to` BIGINT NULL,
  `assign_to_name` VARCHAR(64) NULL,
  `assign_time` DATETIME NULL,
  `deadline` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'REPORTED' COMMENT 'REPORTED/ASSIGNED/RECTIFYING/PENDING_ACCEPT/CLOSED',
  `rectify_plan` TEXT NULL,
  `rectify_photos` TEXT NULL,
  `rectify_time` DATETIME NULL,
  `accept_by` VARCHAR(64) NULL,
  `accept_time` DATETIME NULL,
  `accept_remark` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_hz_tenant` (`tenant_id`,`status`),
  KEY `idx_hz_no` (`hazard_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐患';

DROP TABLE IF EXISTS `bwc_fire_drill`;
CREATE TABLE `bwc_fire_drill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `drill_time` DATETIME NULL,
  `location` VARCHAR(128) NULL,
  `participants` INT NOT NULL DEFAULT 0,
  `sign_count` INT NOT NULL DEFAULT 0,
  `summary` TEXT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/FINISHED',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_fd_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消防演练';

DROP TABLE IF EXISTS `bwc_fire_training`;
CREATE TABLE `bwc_fire_training` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `trainer` VARCHAR(64) NULL,
  `training_time` DATETIME NULL,
  `location` VARCHAR(128) NULL,
  `participants` INT NOT NULL DEFAULT 0,
  `content` TEXT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ft_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消防培训';

DROP TABLE IF EXISTS `bwc_power_check`;
CREATE TABLE `bwc_power_check` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `location` VARCHAR(128) NOT NULL,
  `check_time` DATETIME NULL,
  `checker` VARCHAR(64) NULL,
  `result` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `violation_desc` VARCHAR(500) NULL,
  `photos` TEXT NULL,
  `handle_result` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_pc_tenant` (`tenant_id`,`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规用电核查';

DROP TABLE IF EXISTS `bwc_fire_alarm`;
CREATE TABLE `bwc_fire_alarm` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `alarm_type` VARCHAR(64) NOT NULL,
  `location` VARCHAR(128) NULL,
  `alarm_time` DATETIME NULL,
  `device_id` VARCHAR(64) NULL,
  `content` VARCHAR(500) NULL,
  `link_event_id` BIGINT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_fa_tenant` (`tenant_id`,`alarm_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消防报警';

-- ---------------------------------------------------------------------------
-- 巡逻
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_patrol_route`;
CREATE TABLE `bwc_patrol_route` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(500) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_pr_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡逻路线';

DROP TABLE IF EXISTS `bwc_patrol_point`;
CREATE TABLE `bwc_patrol_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `route_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `location` VARCHAR(128) NULL,
  `qr_code` VARCHAR(128) NULL,
  `longitude` DECIMAL(10,6) NULL,
  `latitude` DECIMAL(10,6) NULL,
  `time_window_start` VARCHAR(8) NULL,
  `time_window_end` VARCHAR(8) NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_pp_route` (`route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡逻点位';

DROP TABLE IF EXISTS `bwc_patrol_record`;
CREATE TABLE `bwc_patrol_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `route_id` BIGINT NULL,
  `point_id` BIGINT NULL,
  `guard_id` BIGINT NULL,
  `guard_name` VARCHAR(64) NULL,
  `check_time` DATETIME NULL,
  `method` VARCHAR(20) NOT NULL DEFAULT 'QR' COMMENT 'QR/GPS',
  `longitude` DECIMAL(10,6) NULL,
  `latitude` DECIMAL(10,6) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/LATE/MISSED/ABNORMAL',
  `abnormal_desc` VARCHAR(500) NULL,
  `photos` TEXT NULL,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_prec_tenant` (`tenant_id`,`check_time`),
  KEY `idx_prec_point` (`point_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡逻记录';

DROP TABLE IF EXISTS `bwc_patrol_task`;
CREATE TABLE `bwc_patrol_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `route_id` BIGINT NULL,
  `guard_id` BIGINT NULL,
  `guard_name` VARCHAR(64) NULL,
  `plan_date` DATE NULL,
  `start_time` DATETIME NULL,
  `end_time` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/DONE/MISSED',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_pt_tenant` (`tenant_id`,`plan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡逻任务';

DROP TABLE IF EXISTS `bwc_dorm_check`;
CREATE TABLE `bwc_dorm_check` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `dorm` VARCHAR(64) NOT NULL,
  `check_date` DATE NULL,
  `checker` VARCHAR(64) NULL,
  `should_count` INT NOT NULL DEFAULT 0,
  `actual_count` INT NOT NULL DEFAULT 0,
  `absent_names` VARCHAR(500) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dc_tenant` (`tenant_id`,`check_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查寝/夜间巡查';

-- ---------------------------------------------------------------------------
-- 失物招领
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_lost_item`;
CREATE TABLE `bwc_lost_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `item_no` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `category` VARCHAR(64) NULL,
  `location` VARCHAR(128) NULL,
  `found_time` DATETIME NULL,
  `photos` TEXT NULL,
  `description` VARCHAR(500) NULL,
  `finder` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/CLAIMED/DISPOSED/ARCHIVED',
  `storage_location` VARCHAR(128) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_li_tenant` (`tenant_id`,`status`),
  KEY `idx_li_no` (`item_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物登记';

DROP TABLE IF EXISTS `bwc_lost_notice`;
CREATE TABLE `bwc_lost_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `item_id` BIGINT NULL,
  `title` VARCHAR(128) NOT NULL,
  `content` TEXT NULL,
  `publish_time` DATETIME NULL,
  `publisher` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  `views` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ln_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物公告';

DROP TABLE IF EXISTS `bwc_lost_claim`;
CREATE TABLE `bwc_lost_claim` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `item_id` BIGINT NOT NULL,
  `claimant_id` BIGINT NULL,
  `claimant` VARCHAR(64) NOT NULL,
  `phone_mask` VARCHAR(32) NULL,
  `id_card_mask` VARCHAR(32) NULL,
  `claim_time` DATETIME NULL,
  `verify_by` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `remark` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_lc_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认领登记';

DROP TABLE IF EXISTS `bwc_lost_disposal`;
CREATE TABLE `bwc_lost_disposal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `item_id` BIGINT NOT NULL,
  `disposal_type` VARCHAR(20) NOT NULL COMMENT 'ARCHIVE/DESTROY',
  `reason` VARCHAR(500) NULL,
  `apply_by` VARCHAR(64) NULL,
  `apply_time` DATETIME NULL,
  `approve_by` VARCHAR(64) NULL,
  `approve_time` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ld_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物处置审批';

-- ---------------------------------------------------------------------------
-- 安全宣传与教育
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_safety_course`;
CREATE TABLE `bwc_safety_course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `category` VARCHAR(64) NULL,
  `cover` VARCHAR(500) NULL,
  `content` LONGTEXT NULL,
  `video_url` VARCHAR(500) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
  `publish_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sc_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全课程';

DROP TABLE IF EXISTS `bwc_course_study`;
CREATE TABLE `bwc_course_study` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `course_id` BIGINT NOT NULL,
  `user_id` BIGINT NULL,
  `user_name` VARCHAR(64) NULL,
  `progress` INT NOT NULL DEFAULT 0,
  `study_time` DATETIME NULL,
  `finish_flag` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_cs_course_user` (`course_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录';

DROP TABLE IF EXISTS `bwc_exam`;
CREATE TABLE `bwc_exam` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `course_id` BIGINT NULL,
  `total_score` INT NOT NULL DEFAULT 100,
  `pass_score` INT NOT NULL DEFAULT 60,
  `questions_json` LONGTEXT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  `start_time` DATETIME NULL,
  `end_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ex_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全考试';

DROP TABLE IF EXISTS `bwc_exam_record`;
CREATE TABLE `bwc_exam_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `exam_id` BIGINT NOT NULL,
  `user_id` BIGINT NULL,
  `user_name` VARCHAR(64) NULL,
  `score` INT NOT NULL DEFAULT 0,
  `pass_flag` TINYINT NOT NULL DEFAULT 0,
  `answers_json` LONGTEXT NULL,
  `submit_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_er_exam_user` (`exam_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试成绩';

DROP TABLE IF EXISTS `bwc_warning`;
CREATE TABLE `bwc_warning` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `type` VARCHAR(64) NOT NULL COMMENT '诈骗/暴雨/风险',
  `title` VARCHAR(128) NOT NULL,
  `content` TEXT NULL,
  `warning_level` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `publish_time` DATETIME NULL,
  `publisher` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_wa_tenant` (`tenant_id`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警推送';

DROP TABLE IF EXISTS `bwc_notice`;
CREATE TABLE `bwc_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `content` LONGTEXT NULL,
  `category` VARCHAR(64) NULL,
  `publish_time` DATETIME NULL,
  `publisher` VARCHAR(64) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  `views` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_no_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告';

DROP TABLE IF EXISTS `bwc_material`;
CREATE TABLE `bwc_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `category` VARCHAR(64) NULL,
  `file_url` VARCHAR(500) NULL,
  `cover` VARCHAR(500) NULL,
  `description` VARCHAR(500) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ma_tenant` (`tenant_id`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宣传素材';

-- ---------------------------------------------------------------------------
-- 报备审批 / 调监控
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_report_apply`;
CREATE TABLE `bwc_report_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `report_no` VARCHAR(64) NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT 'ACTIVITY/CONSTRUCTION/HOTWORK/OUTING/CHEMICAL/BANNER',
  `title` VARCHAR(128) NOT NULL,
  `applicant` VARCHAR(64) NULL,
  `applicant_phone` VARCHAR(32) NULL,
  `content` TEXT NULL,
  `start_time` DATETIME NULL,
  `end_time` DATETIME NULL,
  `location` VARCHAR(128) NULL,
  `safety_plan` TEXT NULL,
  `attachment` VARCHAR(500) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `approver` VARCHAR(64) NULL,
  `approve_time` DATETIME NULL,
  `approve_remark` VARCHAR(500) NULL,
  `receipt_no` VARCHAR(64) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ra_tenant` (`tenant_id`,`type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报备审批';

DROP TABLE IF EXISTS `bwc_monitor_access`;
CREATE TABLE `bwc_monitor_access` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `apply_no` VARCHAR(64) NOT NULL,
  `applicant` VARCHAR(64) NULL,
  `applicant_id` BIGINT NULL,
  `reason` VARCHAR(500) NULL,
  `area` VARCHAR(128) NULL,
  `time_range` VARCHAR(128) NULL,
  `approve_level` VARCHAR(20) NOT NULL DEFAULT 'DEPT' COMMENT 'DEPT/LEADER',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DEPT_APPROVED/APPROVED/REJECTED',
  `approver1` VARCHAR(64) NULL,
  `approve1_time` DATETIME NULL,
  `approver2` VARCHAR(64) NULL,
  `approve2_time` DATETIME NULL,
  `access_record` VARCHAR(500) NULL,
  `result` VARCHAR(500) NULL,
  `feedback` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_moa_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调取监控申请';

-- ---------------------------------------------------------------------------
-- 设备运维
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_device`;
CREATE TABLE `bwc_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `device_no` VARCHAR(64) NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT 'CAMERA/GATE/FACE/ALARM',
  `name` VARCHAR(128) NOT NULL,
  `location` VARCHAR(128) NULL,
  `vendor` VARCHAR(64) NULL,
  `model` VARCHAR(64) NULL,
  `install_date` DATE NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/FAULT/OFFLINE/REPAIRING',
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dev_tenant` (`tenant_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安防设备台账';

DROP TABLE IF EXISTS `bwc_device_repair`;
CREATE TABLE `bwc_device_repair` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `repair_no` VARCHAR(64) NOT NULL,
  `device_id` BIGINT NULL,
  `device_name` VARCHAR(128) NULL,
  `fault_desc` VARCHAR(500) NULL,
  `photos` TEXT NULL,
  `reporter` VARCHAR(64) NULL,
  `report_time` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ASSIGNED/REPAIRING/DONE/CLOSED',
  `assign_to` VARCHAR(64) NULL,
  `finish_time` DATETIME NULL,
  `result` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dr_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备报修工单';

DROP TABLE IF EXISTS `bwc_device_follow`;
CREATE TABLE `bwc_device_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `repair_id` BIGINT NOT NULL,
  `content` VARCHAR(500) NULL,
  `operator` VARCHAR(64) NULL,
  `follow_time` DATETIME NULL,
  `photos` TEXT NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_df_repair` (`repair_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修跟进';

DROP TABLE IF EXISTS `bwc_device_inspection`;
CREATE TABLE `bwc_device_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `device_id` BIGINT NULL,
  `inspector` VARCHAR(64) NULL,
  `inspect_time` DATETIME NULL,
  `result` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `remark` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_di_tenant` (`tenant_id`,`inspect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备巡检';

-- ---------------------------------------------------------------------------
-- 应急管理
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_emergency_plan`;
CREATE TABLE `bwc_emergency_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `type` VARCHAR(64) NULL COMMENT '反恐/踩踏/火灾/极端天气',
  `content` LONGTEXT NULL,
  `file_url` VARCHAR(500) NULL,
  `version` VARCHAR(32) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  `publish_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ep_tenant` (`tenant_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急预案';

DROP TABLE IF EXISTS `bwc_emergency_supply`;
CREATE TABLE `bwc_emergency_supply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `name` VARCHAR(128) NOT NULL,
  `category` VARCHAR(64) NULL,
  `spec` VARCHAR(64) NULL,
  `quantity` INT NOT NULL DEFAULT 0,
  `unit` VARCHAR(16) NULL,
  `location` VARCHAR(128) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_es_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急物资';

DROP TABLE IF EXISTS `bwc_supply_record`;
CREATE TABLE `bwc_supply_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `supply_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL COMMENT 'BORROW/RETURN',
  `quantity` INT NOT NULL DEFAULT 0,
  `user_name` VARCHAR(64) NULL,
  `record_time` DATETIME NULL,
  `operator` VARCHAR(64) NULL,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sr_supply` (`supply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物资领用归还';

DROP TABLE IF EXISTS `bwc_emergency_contact`;
CREATE TABLE `bwc_emergency_contact` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `name` VARCHAR(64) NOT NULL,
  `dept` VARCHAR(64) NULL,
  `duty` VARCHAR(64) NULL,
  `phone` VARCHAR(32) NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ec_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急通讯录';

DROP TABLE IF EXISTS `bwc_emergency_event`;
CREATE TABLE `bwc_emergency_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(128) NOT NULL,
  `type` VARCHAR(64) NULL,
  `location` VARCHAR(128) NULL,
  `occur_time` DATETIME NULL,
  `description` TEXT NULL,
  `response` TEXT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'REPORTED',
  `reporter` VARCHAR(64) NULL,
  `photos` TEXT NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ee_tenant` (`tenant_id`,`occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='突发事件';

-- ---------------------------------------------------------------------------
-- 后勤：排班 / 考勤 / 绩效 / 反馈 / 月报
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `bwc_schedule`;
CREATE TABLE `bwc_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `guard_id` BIGINT NULL,
  `guard_name` VARCHAR(64) NULL,
  `shift_date` DATE NULL,
  `shift_type` VARCHAR(20) NOT NULL DEFAULT 'DAY' COMMENT 'DAY/NIGHT',
  `start_time` VARCHAR(8) NULL,
  `end_time` VARCHAR(8) NULL,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sch_tenant` (`tenant_id`,`shift_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保安排班';

DROP TABLE IF EXISTS `bwc_attendance`;
CREATE TABLE `bwc_attendance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `guard_id` BIGINT NULL,
  `guard_name` VARCHAR(64) NULL,
  `check_time` DATETIME NULL,
  `type` VARCHAR(20) NOT NULL DEFAULT 'ON' COMMENT 'ON/OFF',
  `method` VARCHAR(20) NOT NULL DEFAULT 'APP',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_att_tenant` (`tenant_id`,`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤打卡';

DROP TABLE IF EXISTS `bwc_performance`;
CREATE TABLE `bwc_performance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `guard_id` BIGINT NULL,
  `guard_name` VARCHAR(64) NULL,
  `month` VARCHAR(16) NULL,
  `score` INT NOT NULL DEFAULT 0,
  `items_json` TEXT NULL,
  `remark` VARCHAR(255) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_perf_tenant` (`tenant_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保安绩效';

DROP TABLE IF EXISTS `bwc_feedback`;
CREATE TABLE `bwc_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `type` VARCHAR(20) NOT NULL DEFAULT 'SUGGESTION' COMMENT 'COMPLAINT/SUGGESTION',
  `title` VARCHAR(128) NOT NULL,
  `content` TEXT NULL,
  `contact` VARCHAR(64) NULL,
  `submit_time` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDLING/DONE',
  `handler` VARCHAR(64) NULL,
  `handle_time` DATETIME NULL,
  `result` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_fb_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉反馈';

DROP TABLE IF EXISTS `bwc_monthly_report`;
CREATE TABLE `bwc_monthly_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `month` VARCHAR(16) NOT NULL,
  `content_json` LONGTEXT NULL,
  `file_url` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mr_tenant_month` (`tenant_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月报季报';

SET FOREIGN_KEY_CHECKS = 1;
