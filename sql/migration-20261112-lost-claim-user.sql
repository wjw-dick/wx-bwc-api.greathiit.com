-- 失物认领：记录认领人账号，支持小程序“我的认领”只查看本人申请（幂等）
SET NAMES utf8mb4;
USE `bwc`;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'bwc_lost_claim' AND column_name = 'claimant_id'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE bwc_lost_claim ADD COLUMN claimant_id BIGINT NULL AFTER item_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
