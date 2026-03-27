package com.example.feature.activities.specification;

import com.example.common.entity.Users;
import com.example.feature.activities.model.Activities;
import com.example.feature.registration.model.Registrations;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

                if (studentDeptId != null) {
                    Join<Object, Object> organizerJoin = root.join("organizer", JoinType.INNER);
                    Join<Object, Object> departmentJoin = organizerJoin.join("department", JoinType.INNER);
                    Predicate isMyFaculty = cb.equal(departmentJoin.get("id"), studentDeptId);
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
            if ("PENDING".equalsIgnoreCase(status)) {
                return cb.equal(root.get("status"), 0); // 0 = Chờ duyệt
            }
            if ("APPROVED".equalsIgnoreCase(status)) {
                return cb.equal(root.get("status"), 1); // 1 = Đã duyệt
            }
            if ("REJECTED".equalsIgnoreCase(status)) {
                return cb.equal(root.get("status"), 2); // 2 = Từ chối
            }
            if ("ALL".equalsIgnoreCase(status)) {
                return null;
            }
            if ("3".equals(status)) {
                return cb.equal(root.get("status"), 3);
            }
            if ("EXCLUDE_DRAFT".equals(status)) {
                return cb.notEqual(root.get("status"), 3);
            }

            LocalDateTime now = LocalDateTime.now();

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

                return cb.and(timeValid, notFull);

            } else if ("UPCOMING".equals(status)) {
                return cb.greaterThan(root.get("registrationStart"), now);

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

            // 1. Lấy ID từ Object organizer
            Predicate conditionOrganizer = criteriaBuilder.equal(root.get("organizer").get("id"), userId);

            // 2. Lấy ID từ Object createdBy
            Predicate conditionCreator = criteriaBuilder.equal(root.get("createdBy").get("id"), userId);

            // 3. Nối bằng OR (Là Người tổ chức HOẶC Người tạo thì đều được xem)
            return criteriaBuilder.or(conditionOrganizer, conditionCreator);
        };
    }

    public static Specification<Activities> hasDepartmentId(Long departmentId) {
        return (root, query, cb) -> {
            if (departmentId == null) return null;
            return cb.equal(root.get("departmentId"), departmentId);
        };
    }
}