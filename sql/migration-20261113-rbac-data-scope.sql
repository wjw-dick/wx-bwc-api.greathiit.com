-- 2026-11-13 权限整改：补充业务单据归属字段，支持 C 端“只能看本人数据”
-- 仅新增可空列，不修改/删除历史数据；可重复执行。
SET NAMES utf8mb4;
USE `bwc`;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE bwc_vehicle_appointment ADD COLUMN owner_id BIGINT NULL AFTER phone_mask',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'bwc_vehicle_appointment' AND column_name = 'owner_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE bwc_report_apply ADD COLUMN owner_id BIGINT NULL AFTER applicant_phone',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'bwc_report_apply' AND column_name = 'owner_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE bwc_device_repair ADD COLUMN reporter_id BIGINT NULL AFTER reporter',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'bwc_device_repair' AND column_name = 'reporter_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE bwc_feedback ADD COLUMN user_id BIGINT NULL AFTER contact',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'bwc_feedback' AND column_name = 'user_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE bwc_emergency_event ADD COLUMN reporter_id BIGINT NULL AFTER reporter',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'bwc_emergency_event' AND column_name = 'reporter_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
