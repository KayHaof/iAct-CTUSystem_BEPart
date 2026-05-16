-- ============================================================
-- Normalize user_db major relationship
-- Goal:
--   1. Preserve data from legacy `major` table if it exists.
--   2. Ensure canonical table is `majors`.
--   3. Rewire `classes.major_id` to reference `majors(id)`.
--   4. Remove legacy `major` table after backup and FK normalization.
-- ============================================================

USE `user_db`;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `majors` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `code` VARCHAR(50),
    `program_type` VARCHAR(50),
    `department_id` BIGINT,
    `is_active` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `fk_major_department` (`department_id`),
    CONSTRAINT `fk_major_department` FOREIGN KEY (`department_id`)
        REFERENCES `departments`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `classes_backup_20260515` AS SELECT * FROM `classes`;
CREATE TABLE IF NOT EXISTS `majors_backup_20260515` AS SELECT * FROM `majors`;

ALTER TABLE `classes`
    MODIFY COLUMN `class_code` VARCHAR(50),
    MODIFY COLUMN `academic_year` VARCHAR(10),
    MODIFY COLUMN `is_active` TINYINT DEFAULT 1;

ALTER TABLE `departments`
    MODIFY COLUMN `is_active` TINYINT DEFAULT 1;

ALTER TABLE `majors`
    MODIFY COLUMN `is_active` TINYINT DEFAULT 1;

SET @legacy_major_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'major'
);

SET @sql := IF(
    @legacy_major_exists = 1,
    'CREATE TABLE IF NOT EXISTS `major_backup_20260515` AS SELECT * FROM `major`',
    'SELECT ''Legacy table major does not exist'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @legacy_major_exists = 1,
    'INSERT INTO `majors` (`id`, `name`, `program_type`, `department_id`, `is_active`, `created_at`, `updated_at`)
     SELECT legacy.`id`, legacy.`name`, legacy.`program_type`, legacy.`department_id`, 1, NOW(), NOW()
     FROM `major` legacy
     LEFT JOIN `majors` target ON target.`id` = legacy.`id`
     WHERE target.`id` IS NULL',
    'SELECT ''No legacy major rows to migrate'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `classes` c
LEFT JOIN `majors` m ON m.`id` = c.`major_id`
SET c.`major_id` = NULL
WHERE c.`major_id` IS NOT NULL
  AND m.`id` IS NULL;

SET @legacy_fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'major_id'
      AND REFERENCED_TABLE_NAME = 'major'
    LIMIT 1
);

SET @sql := IF(
    @legacy_fk_name IS NOT NULL,
    CONCAT('ALTER TABLE `classes` DROP FOREIGN KEY `', @legacy_fk_name, '`'),
    'SELECT ''No classes.major_id FK references legacy major table'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @wrong_fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'major_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
      AND REFERENCED_TABLE_NAME <> 'majors'
    LIMIT 1
);

SET @sql := IF(
    @wrong_fk_name IS NOT NULL,
    CONCAT('ALTER TABLE `classes` DROP FOREIGN KEY `', @wrong_fk_name, '`'),
    'SELECT ''No wrong classes.major_id FK remains'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @correct_fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'major_id'
      AND REFERENCED_TABLE_NAME = 'majors'
);

SET @fk_class_major_name_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classes'
      AND CONSTRAINT_NAME = 'fk_class_major'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql := IF(
    @correct_fk_exists = 0 AND @fk_class_major_name_exists = 0,
    'ALTER TABLE `classes`
       ADD CONSTRAINT `fk_class_major`
       FOREIGN KEY (`major_id`) REFERENCES `majors`(`id`) ON DELETE SET NULL',
    'SELECT ''classes.major_id already references majors or fk_class_major exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @remaining_legacy_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND REFERENCED_TABLE_NAME = 'major'
);

SET @sql := IF(
    @legacy_major_exists = 1 AND @remaining_legacy_fk = 0,
    'DROP TABLE IF EXISTS `major`',
    'SELECT ''Legacy table major was not dropped because referenced FKs still exist or table is absent'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;

SELECT c.`id`, c.`major_id`
FROM `classes` c
LEFT JOIN `majors` m ON c.`major_id` = m.`id`
WHERE c.`major_id` IS NOT NULL
  AND m.`id` IS NULL;
