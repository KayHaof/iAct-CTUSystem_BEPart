USE `user_db`;

CREATE TABLE IF NOT EXISTS `class_representatives` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `representative_type` VARCHAR(50) NOT NULL,
    `is_active` TINYINT DEFAULT 1,
    `start_date` DATE,
    `end_date` DATE,
    `assigned_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_class_representative_class` (`class_id`),
    KEY `idx_class_representative_student` (`student_id`),
    KEY `idx_class_representative_active` (`student_id`, `is_active`, `start_date`, `end_date`),
    KEY `fk_class_representative_assigned_by` (`assigned_by`),
    CONSTRAINT `fk_class_representative_class` FOREIGN KEY (`class_id`) REFERENCES `classes`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_class_representative_student` FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_class_representative_assigned_by` FOREIGN KEY (`assigned_by`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
