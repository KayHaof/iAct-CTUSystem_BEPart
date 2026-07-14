USE activity_db;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'activity_schedules'
      AND column_name = 'location_id'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `activity_schedules` ADD COLUMN `location_id` BIGINT NULL AFTER `location`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'activity_schedules'
      AND index_name = 'fk_schedule_location'
);
SET @sql := IF(
    @index_exists = 0,
    'ALTER TABLE `activity_schedules` ADD KEY `fk_schedule_location` (`location_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'activity_schedules'
      AND constraint_name = 'fk_schedule_location'
);
SET @sql := IF(
    @fk_exists = 0,
    'ALTER TABLE `activity_schedules` ADD CONSTRAINT `fk_schedule_location` FOREIGN KEY (`location_id`) REFERENCES `locations`(`id`) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'activity_location_bookings'
      AND column_name = 'schedule_id'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `activity_location_bookings` ADD COLUMN `schedule_id` BIGINT NULL AFTER `location_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'activity_location_bookings'
      AND index_name = 'fk_location_booking_schedule'
);
SET @sql := IF(
    @index_exists = 0,
    'ALTER TABLE `activity_location_bookings` ADD KEY `fk_location_booking_schedule` (`schedule_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'activity_location_bookings'
      AND constraint_name = 'fk_location_booking_schedule'
);
SET @sql := IF(
    @fk_exists = 0,
    'ALTER TABLE `activity_location_bookings` ADD CONSTRAINT `fk_location_booking_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `activity_schedules`(`id`) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
