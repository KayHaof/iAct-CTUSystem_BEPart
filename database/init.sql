-- mysql> GRANT ALL PRIVILEGES ON *.* TO 'database'@'%' WITH GRANT OPTION;
-- FLUSH PRIVILEGES;

-- docker exec -it mysql_database mysql -u root -p

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ======================
-- 1. ORGANIZATION STRUCTURE
-- ======================
CREATE TABLE departments (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             name VARCHAR(255) NOT NULL,
                             description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE major (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       program_type VARCHAR(100),
                       department_id BIGINT,
                       CONSTRAINT fk_major_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE classes (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         class_code VARCHAR(100) UNIQUE,
                         name VARCHAR(255),
                         major_id BIGINT,
                         academic_year INT,
                         CONSTRAINT fk_class_major FOREIGN KEY (major_id) REFERENCES major(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ======================
-- 2. USERS (Đã bổ sung department_id trực tiếp)
-- ======================
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       keycloak_id VARCHAR(255) NOT NULL UNIQUE,
                       username VARCHAR(100) UNIQUE,
                       email VARCHAR(255) UNIQUE,
                       role_type TINYINT NOT NULL COMMENT '1=student,2=department,3=admin,4=other',
                       status TINYINT DEFAULT 1 COMMENT '1=active,0=inactive,2=locked',

                       student_code VARCHAR(50) UNIQUE,
                       class_id BIGINT,
                       department_id BIGINT, -- Mối quan hệ bổ sung trực tiếp
                       full_name VARCHAR(255),
                       birthday DATE,
                       gender TINYINT,
                       phone VARCHAR(20),
                       address TEXT,

                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT fk_user_class FOREIGN KEY (class_id) REFERENCES classes(id),
                       CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ======================
-- 3. PROFILES (ORGANIZERS)
-- ======================
CREATE TABLE organizers (
                            user_id BIGINT PRIMARY KEY,
                            name VARCHAR(255),
                            department_id BIGINT NULL,
                            representative_user BIGINT,
                            CONSTRAINT fk_organizer_user FOREIGN KEY (user_id) REFERENCES users(id),
                            CONSTRAINT fk_organizer_department FOREIGN KEY (department_id) REFERENCES departments(id),
                            CONSTRAINT fk_organizer_representative FOREIGN KEY (representative_user) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ======================
-- 4. ACADEMIC & CATEGORIES
-- ======================
CREATE TABLE semesters (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           semester_name VARCHAR(100),
                           academic_year VARCHAR(20),
                           start_date DATE,
                           end_date DATE,
                           is_active BOOLEAN DEFAULT FALSE,
                           is_locked BOOLEAN DEFAULT FALSE,
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            parent_id BIGINT,
                            max_point INT,
                            code VARCHAR(50) UNIQUE,
                            name VARCHAR(255),
                            CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ======================
-- 5. ACTIVITIES & BENEFITS
-- ======================
CREATE TABLE activities (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            title VARCHAR(255),
                            description TEXT,
                            content MEDIUMTEXT,
                            registration_start DATETIME,
                            registration_end DATETIME,
                            start_date DATETIME,
                            end_date DATETIME,
                            location VARCHAR(255),
                            semester_id BIGINT,
                            max_participants INT,
                            cover_image VARCHAR(2048),
                            thumbnail VARCHAR(2048),
                            source_link VARCHAR(2048),
                            organizer_id BIGINT,
                            is_external BOOLEAN DEFAULT FALSE,
                            qr_code_token VARCHAR(255) UNIQUE,
                            status TINYINT,
                            created_by BIGINT,
                            CONSTRAINT fk_activity_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
                            CONSTRAINT fk_activity_organizer FOREIGN KEY (organizer_id) REFERENCES organizers(user_id),
                            CONSTRAINT fk_activity_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE benefits (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          activity_id BIGINT,
                          type TINYINT,
                          category_id BIGINT,
                          point INT,
                          CONSTRAINT fk_benefit_activity FOREIGN KEY (activity_id) REFERENCES activities(id),
                          CONSTRAINT fk_benefit_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ======================
-- 6. INTERACTION
-- ======================
CREATE TABLE registrations (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               student_id BIGINT,
                               activity_id BIGINT,
                               registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               status TINYINT,
                               cancel_reason TEXT,
                               CONSTRAINT fk_registration_student FOREIGN KEY (student_id) REFERENCES users(id),
                               CONSTRAINT fk_registration_activity FOREIGN KEY (activity_id) REFERENCES activities(id),
                               UNIQUE KEY uk_student_activity (student_id, activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE attendances (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             registration_id BIGINT,
                             checkin_time DATETIME,
                             checkout_time DATETIME,
                             latitude DECIMAL(10, 8),
                             longitude DECIMAL(11, 8),
                             method TINYINT,
                             CONSTRAINT fk_attendance_registration FOREIGN KEY (registration_id) REFERENCES registrations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE proofs (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        student_id BIGINT,
                        activity_id BIGINT,
                        image_url VARCHAR(2048),
                        description TEXT,
                        status TINYINT,
                        rejection_reason TEXT,
                        verified_user BIGINT,
                        verified_time DATETIME,
                        CONSTRAINT fk_proof_student FOREIGN KEY (student_id) REFERENCES users(id),
                        CONSTRAINT fk_proof_activity FOREIGN KEY (activity_id) REFERENCES activities(id),
                        CONSTRAINT fk_proof_verified_user FOREIGN KEY (verified_user) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ======================
-- 7. COMPLAINTS & AWARDS
-- ======================
CREATE TABLE complaints (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            student_id BIGINT,
                            activity_id BIGINT,
                            semester_id BIGINT,
                            reason VARCHAR(255),
                            detail MEDIUMTEXT,
                            evidence_url VARCHAR(2048),
                            status TINYINT,
                            detail_response MEDIUMTEXT,
                            resolved_at DATETIME,
                            CONSTRAINT fk_complaint_student FOREIGN KEY (student_id) REFERENCES users(id),
                            CONSTRAINT fk_complaint_activity FOREIGN KEY (activity_id) REFERENCES activities(id),
                            CONSTRAINT fk_complaint_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE awards (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255),
                        type TINYINT,
                        description TEXT,
                        requirements MEDIUMTEXT,
                        is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE student_awards (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                student_id BIGINT NOT NULL,
                                award_id BIGINT NOT NULL,
                                semester_id BIGINT NOT NULL,
                                status TINYINT DEFAULT 0,
                                decision_number VARCHAR(100),
                                achieved_at DATE,
                                evidence_url VARCHAR(2048),
                                CONSTRAINT fk_student_award_student FOREIGN KEY (student_id) REFERENCES users(id),
                                CONSTRAINT fk_student_award_award FOREIGN KEY (award_id) REFERENCES awards(id),
                                CONSTRAINT fk_student_award_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE award_criteria (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                award_id BIGINT,
                                category_id BIGINT,
                                min_point_required INT,
                                CONSTRAINT fk_criteria_award FOREIGN KEY (award_id) REFERENCES awards(id),
                                CONSTRAINT fk_criteria_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ======================
-- 8. SYSTEM NOTIFICATIONS
-- ======================
CREATE TABLE notifications (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT,
                               title VARCHAR(255),
                               message TEXT,
                               type TINYINT,
                               activity_id BIGINT NULL,
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
                               CONSTRAINT fk_notification_activity FOREIGN KEY (activity_id) REFERENCES activities(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;