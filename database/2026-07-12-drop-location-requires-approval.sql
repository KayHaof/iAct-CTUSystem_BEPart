USE activity_db;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'activity_db'
      AND table_name = 'locations'
      AND column_name = 'requires_approval'
);

SET @drop_requires_approval_sql = IF(
    @column_exists > 0,
    'ALTER TABLE `locations` DROP COLUMN `requires_approval`',
    'SELECT 1'
);

PREPARE drop_requires_approval_stmt FROM @drop_requires_approval_sql;
EXECUTE drop_requires_approval_stmt;
DEALLOCATE PREPARE drop_requires_approval_stmt;
