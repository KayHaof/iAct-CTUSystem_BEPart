-- ============================================================
-- MULTIPLE DATABASE INITIALIZATION
-- Runs on first MySQL container start (when volume is empty)
-- ============================================================

-- Create user first
CREATE USER IF NOT EXISTS 'database'@'%' IDENTIFIED BY 'password1';
CREATE USER IF NOT EXISTS 'database'@'localhost' IDENTIFIED BY 'password1';

-- Create all databases
CREATE DATABASE IF NOT EXISTS `user_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `activity_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `notification_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `keycloak`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Grant privileges
GRANT ALL PRIVILEGES ON `user_db`.* TO 'database'@'%';
GRANT ALL PRIVILEGES ON `activity_db`.* TO 'database'@'%';
GRANT ALL PRIVILEGES ON `notification_db`.* TO 'database'@'%';
GRANT ALL PRIVILEGES ON `keycloak`.* TO 'database'@'%';

GRANT ALL PRIVILEGES ON `user_db`.* TO 'database'@'localhost';
GRANT ALL PRIVILEGES ON `activity_db`.* TO 'database'@'localhost';
GRANT ALL PRIVILEGES ON `notification_db`.* TO 'database'@'localhost';
GRANT ALL PRIVILEGES ON `keycloak`.* TO 'database'@'localhost';

FLUSH PRIVILEGES;

-- Use user_db and create tables
USE `user_db`;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `keycloak_id` VARCHAR(255) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `role_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1=STUDENT, 2=DEPARTMENT, 3=ADMIN, 4=OTHER',
    `status` TINYINT DEFAULT 1 COMMENT '1=ACTIVE, 0=INACTIVE',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keycloak_id` (`keycloak_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `departments`;
CREATE TABLE `departments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `code` VARCHAR(50),
    `description` TEXT,
    `is_active` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `majors`;
CREATE TABLE `majors` (
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
    CONSTRAINT `fk_major_department` FOREIGN KEY (`department_id`) REFERENCES `departments`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `class_code` VARCHAR(50),
    `major_id` BIGINT,
    `academic_year` VARCHAR(10),
    `is_active` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_code` (`class_code`),
    KEY `fk_class_major` (`major_id`),
    CONSTRAINT `fk_class_major` FOREIGN KEY (`major_id`) REFERENCES `majors`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `student_profiles`;
CREATE TABLE `student_profiles` (
    `user_id` BIGINT NOT NULL,
    `student_code` VARCHAR(50),
    `full_name` VARCHAR(255),
    `birthday` DATE,
    `gender` TINYINT,
    `phone` VARCHAR(20),
    `address` VARCHAR(500),
    `avatar_url` VARCHAR(500),
    `class_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_student_code` (`student_code`),
    KEY `fk_student_class` (`class_id`),
    CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_student_class` FOREIGN KEY (`class_id`) REFERENCES `classes`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `department_profiles`;
CREATE TABLE `department_profiles` (
    `user_id` BIGINT NOT NULL,
    `department_id` BIGINT,
    `full_name` VARCHAR(255),
    `phone` VARCHAR(20),
    `avatar_url` VARCHAR(500),
    `address` VARCHAR(100),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    KEY `fk_dept_profile_dept` (`department_id`),
    CONSTRAINT `fk_dept_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dept_profile_dept` FOREIGN KEY (`department_id`) REFERENCES `departments`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Use activity_db and create tables
USE `activity_db`;

DROP TABLE IF EXISTS `semesters`;
CREATE TABLE `semesters` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `academic_year` VARCHAR(20) NOT NULL,
    `start_date` DATE,
    `end_date` DATE,
    `is_active` TINYINT DEFAULT 0,
    `is_locked` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `code` VARCHAR(50),
    `max_point` INT DEFAULT 0,
    `parent_id` BIGINT,
    `is_active` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `fk_category_parent` (`parent_id`),
    CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `awards`;
CREATE TABLE `awards` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `type` TINYINT,
    `description` TEXT,
    `requirements` TEXT,
    `is_active` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `award_criteria`;
CREATE TABLE `award_criteria` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `award_id` BIGINT NOT NULL,
    `category_id` BIGINT,
    `min_point_required` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_criteria_award` (`award_id`),
    KEY `fk_criteria_category` (`category_id`),
    CONSTRAINT `fk_criteria_award` FOREIGN KEY (`award_id`) REFERENCES `awards`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_criteria_category` FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `organizers`;
CREATE TABLE `organizers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(255),
    `department_id` BIGINT,
    `is_active` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_organizer_user` (`user_id`),
    KEY `idx_organizer_user` (`user_id`),
    KEY `idx_organizer_dept` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `activities`;
CREATE TABLE `activities` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `content` LONGTEXT,
    `cover_image` VARCHAR(500),
    `thumbnail` VARCHAR(500),
    `source_link` VARCHAR(500),
    `location` VARCHAR(255),
    `semester_id` BIGINT,
    `category_id` BIGINT,
    `organizer_id` BIGINT,
    `department_id` BIGINT,
    `max_participants` INT,
    `current_participants` INT DEFAULT 0,
    `is_external` TINYINT DEFAULT 0,
    `is_faculty` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 0,
    `qr_code_token` VARCHAR(255),
    `registration_start` DATETIME,
    `registration_end` DATETIME,
    `start_date` DATETIME,
    `end_date` DATETIME,
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `handled_by` BIGINT,
    `handled_at` DATETIME(6),
    `reject_reason` TEXT,
    PRIMARY KEY (`id`),
    KEY `fk_activity_semester` (`semester_id`),
    KEY `fk_activity_category` (`category_id`),
    KEY `fk_activity_organizer` (`organizer_id`),
    KEY `fk_activity_created_by` (`created_by`),
    KEY `fk_activity_handled_by` (`handled_by`),
    KEY `idx_activities_status` (`status`),
    KEY `idx_activities_date` (`start_date`, `end_date`),
    CONSTRAINT `fk_activity_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_activity_category` FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_activity_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `organizers`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `activity_schedules`;
CREATE TABLE `activity_schedules` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `activity_id` BIGINT NOT NULL,
    `title` VARCHAR(255),
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `location` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_schedule_activity` (`activity_id`),
    CONSTRAINT `fk_schedule_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `registrations`;
CREATE TABLE `registrations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `activity_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `registered_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` TINYINT DEFAULT 0,
    `cancel_reason` TEXT,
    `cancel_at` DATETIME,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_activity` (`student_id`, `activity_id`),
    KEY `fk_registration_activity` (`activity_id`),
    KEY `idx_registrations_student` (`student_id`),
    KEY `idx_registrations_status` (`status`),
    CONSTRAINT `fk_registration_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `registration_schedules`;
CREATE TABLE `registration_schedules` (
    `registration_id` BIGINT NOT NULL,
    `schedule_id` BIGINT NOT NULL,
    PRIMARY KEY (`registration_id`, `schedule_id`),
    KEY `fk_reg_schedule_schedule` (`schedule_id`),
    CONSTRAINT `fk_reg_schedule_registration` FOREIGN KEY (`registration_id`) REFERENCES `registrations`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_reg_schedule_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `activity_schedules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `benefits`;
CREATE TABLE `benefits` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `activity_id` BIGINT NOT NULL,
    `category_id` BIGINT,
    `type` TINYINT,
    `point` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_benefit_activity` (`activity_id`),
    KEY `fk_benefit_category` (`category_id`),
    CONSTRAINT `fk_benefit_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_benefit_category` FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `attendances`;
CREATE TABLE `attendances` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `registration_id` BIGINT NOT NULL,
    `schedule_id` BIGINT,
    `checkin_time` DATETIME,
    `checkout_time` DATETIME,
    `latitude` DECIMAL(10, 8),
    `longitude` DECIMAL(11, 8),
    `method` TINYINT,
    `status` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_attendance_registration` (`registration_id`),
    KEY `fk_attendance_schedule` (`schedule_id`),
    KEY `idx_attendances_date` (`checkin_time`),
    CONSTRAINT `fk_attendance_registration` FOREIGN KEY (`registration_id`) REFERENCES `registrations`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_attendance_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `activity_schedules`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `proofs`;
CREATE TABLE `proofs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `registration_id` BIGINT NOT NULL,
    `image_url` VARCHAR(500),
    `description` TEXT,
    `status` TINYINT DEFAULT 0,
    `rejection_reason` TEXT,
    `verified_by` BIGINT,
    `verified_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_proof_registration` (`registration_id`),
    KEY `idx_proof_verified_by` (`verified_by`),
    CONSTRAINT `fk_proof_registration` FOREIGN KEY (`registration_id`) REFERENCES `registrations`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `complaints`;
CREATE TABLE `complaints` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `registration_id` BIGINT NOT NULL,
    `detail` TEXT NOT NULL,
    `evidence_url` VARCHAR(500),
    `response` TEXT,
    `status` TINYINT DEFAULT 0,
    `resolved_at` DATETIME(6),
    `resolved_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_complaint_registration` (`registration_id`),
    KEY `idx_complaint_resolved_by` (`resolved_by`),
    CONSTRAINT `fk_complaint_registration` FOREIGN KEY (`registration_id`) REFERENCES `registrations`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `student_awards`;
CREATE TABLE `student_awards` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `student_id` BIGINT NOT NULL,
    `award_id` BIGINT NOT NULL,
    `semester_id` BIGINT,
    `status` TINYINT DEFAULT 0,
    `decision_number` VARCHAR(100),
    `achieved_at` DATE,
    `evidence_url` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_student_award_student` (`student_id`),
    KEY `fk_student_award_award` (`award_id`),
    KEY `fk_student_award_semester` (`semester_id`),
    CONSTRAINT `fk_student_award_award` FOREIGN KEY (`award_id`) REFERENCES `awards`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_student_award_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Use notification_db and create tables
USE `notification_db`;

DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT,
    `type` TINYINT NOT NULL,
    `reference_id` BIGINT,
    `reference_type` VARCHAR(50),
    `is_read` TINYINT DEFAULT 0,
    `read_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notification_user` (`user_id`),
    KEY `idx_notification_type` (`type`),
    KEY `idx_notification_unread` (`user_id`, `is_read`),
    KEY `idx_notifications_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `notification_preferences`;
CREATE TABLE `notification_preferences` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` TINYINT NOT NULL,
    `is_enabled` TINYINT DEFAULT 1,
    `email_enabled` TINYINT DEFAULT 0,
    `push_enabled` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type` (`user_id`, `type`),
    KEY `idx_pref_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
