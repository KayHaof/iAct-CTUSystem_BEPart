package com.example.feature.activities.specification;

import com.example.feature.activities.model.Activities;
import com.example.feature.registration.model.Registrations;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDateTime;

public class ActivitySpecification {

    public static Specification<Activities> isApproved() {
        return (root, query, cb) -> cb.equal(root.get("status"), 1);
    }

    public static Specification<Activities> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            return cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
        };
    }

    public static Specification<Activities> hasLevel(String level, Long studentDeptId) {
        return (root, query, cb) -> {
            // 1. CẤP TRƯỜNG: isExternal = false VÀ (isFaculty = false HOẶC isFaculty = null)
            if ("UNIVERSITY".equals(level)) {
                Predicate isNotExternal = cb.isFalse(root.get("isExternal"));

                Predicate isFacultyFalse = cb.isFalse(root.get("isFaculty"));
                Predicate isFacultyNull = cb.isNull(root.get("isFaculty"));
                Predicate isUniversity = cb.or(isFacultyFalse, isFacultyNull);

                return cb.and(isNotExternal, isUniversity);
            }

            // 2. CẤP KHOA: isExternal = false VÀ isFaculty = true
            if ("FACULTY".equals(level)) {
                Predicate isNotExternal = cb.isFalse(root.get("isExternal"));
                Predicate isFaculty = cb.isTrue(root.get("isFaculty"));

                // Chọc thẳng vào biến departmentId (Long)
                if (studentDeptId != null) {
                    Predicate isMyFaculty = cb.equal(root.get("departmentId"), studentDeptId);
                    return cb.and(isNotExternal, isFaculty, isMyFaculty);
                }

                return cb.and(isNotExternal, isFaculty);
            }

            // 3. NGOÀI TRƯỜNG: isExternal = true
            if ("EXTERNAL".equals(level)) {
                return cb.isTrue(root.get("isExternal"));
            }

            // 4. TẤT CẢ (ALL)
            return null;
        };
    }

    public static Specification<Activities> hasStatus(String status, String keyword, boolean isOrganizer) {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();

            // --- XỬ LÝ STATUS "ALL" ---
            if ("ALL".equalsIgnoreCase(status)) {
                if (isOrganizer) {
                    return null; // Admin/Organizer xem tất cả
                } else {
                    // Student: Chỉ xem "Đã duyệt" VÀ "Chưa hết hạn đăng ký"
                    return cb.and(
                            cb.equal(root.get("status"), 1),
                            cb.greaterThanOrEqualTo(root.get("registrationEnd"), now)
                    );
                }
            }

            // --- CÁC TRẠNG THÁI CỐ ĐỊNH CỦA HOẠT ĐỘNG ---
            if ("PENDING".equalsIgnoreCase(status)) {
                return cb.equal(root.get("status"), 0); // 0 = Chờ duyệt
            }
            if ("APPROVED".equalsIgnoreCase(status)) {
                return cb.equal(root.get("status"), 1); // 1 = Đã duyệt
            }
            if ("REJECTED".equalsIgnoreCase(status)) {
                return cb.equal(root.get("status"), 2); // 2 = Từ chối
            }
            if ("3".equals(status)) {
                return cb.equal(root.get("status"), 3); // 3 = Draft
            }
            if ("EXCLUDE_DRAFT".equals(status)) {
                return cb.notEqual(root.get("status"), 3);
            }

            // --- XỬ LÝ TRẠNG THÁI THEO THỜI GIAN (OPEN / UPCOMING) ---
            if ("OPEN".equals(status)) {
                Predicate timeValid = cb.and(
                        cb.lessThanOrEqualTo(root.get("registrationStart"), now),
                        cb.greaterThanOrEqualTo(root.get("registrationEnd"), now)
                );

                Subquery<Long> countQuery = query.subquery(Long.class);
                Root<Registrations> regRoot = countQuery.from(Registrations.class);

                countQuery.select(cb.count(regRoot))
                        .where(cb.and(
                                cb.equal(regRoot.get("activity"), root),
                                cb.notEqual(regRoot.get("status"), 2) // status 2 = rejected registration
                        ));

                Predicate notFull = cb.greaterThan(root.get("maxParticipants").as(Long.class), countQuery);
                Predicate openCondition = cb.and(timeValid, notFull);

                if (!isOrganizer) {
                    return cb.and(cb.equal(root.get("status"), 1), openCondition);
                }
                return openCondition;

            } else if ("UPCOMING".equals(status)) {
                Predicate upcomingCondition = cb.greaterThan(root.get("registrationStart"), now);

                if (!isOrganizer) {
                    return cb.and(cb.equal(root.get("status"), 1), upcomingCondition);
                }
                return upcomingCondition;

            } else {
                if (isOrganizer) {
                    return null;
                }
                if (!StringUtils.hasText(keyword)) {
                    return cb.greaterThanOrEqualTo(root.get("registrationEnd"), now);
                }
                return null;
            }
        };
    }

    public static Specification<Activities> isOwnedByOrOrganizedBy(Long userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) return criteriaBuilder.conjunction();

            // [FIX LỖI MỚI NHẤT]:
            // organizer là Object Entity -> Phải get("organizer").get("id")
            Predicate conditionOrganizer = criteriaBuilder.equal(root.get("organizer").get("id"), userId);

            // createdBy là Long nguyên thủy -> get thẳng tên biến
            Predicate conditionCreator = criteriaBuilder.equal(root.get("createdBy"), userId);

            return criteriaBuilder.or(conditionOrganizer, conditionCreator);
        };
    }

    public static Specification<Activities> hasDepartmentId(Long departmentId) {
        return (root, query, cb) -> {
            if (departmentId == null) return null;
            // Chọc thẳng vào biến departmentId (Long)
            return cb.equal(root.get("departmentId"), departmentId);
        };
    }
}