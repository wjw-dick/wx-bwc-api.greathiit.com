-- 失物招领：捡拾登记增加拾获人联系电话（脱敏存储，幂等）
SET NAMES utf8mb4;
USE `bwc`;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'bwc_lost_item' AND column_name = 'finder_phone_mask'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE bwc_lost_item ADD COLUMN finder_phone_mask VARCHAR(32) NULL AFTER finder',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
