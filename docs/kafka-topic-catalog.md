# Kafka Topic Catalog cho hệ thống iAct

## 1. Mục đích

Tài liệu này tổng hợp các Kafka topics cần có để hệ thống iAct vận hành đầy đủ theo hướng event-driven, bao gồm topic đang tồn tại trong source code hiện tại và topic đề xuất cho CRUD, notification, projection, audit, analytics, cleanup và replay.

Phạm vi áp dụng:

- `user-service`: auth, users, profiles, departments, majors, classes, preferences, Keycloak provisioning/import.
- `activity-service`: activities, schedules, registrations, attendances, proofs, benefits, categories, semesters, points, dashboard, local user projection.
- `notification-service`: notification persistence, WebSocket dispatch, read/unread state, broadcast/urgent notification.
- `apigateway`, `discovery-service`: infrastructure services, không trực tiếp xử lý Kafka business events trong hiện trạng.
- `common`: shared event/DTO contracts.
- `keycloak`: external identity provider, được xem là external source cho user/role lifecycle.

Task tài liệu này không yêu cầu DB migration, không tạo API mới và không sửa code runtime.

## 2. Quy ước naming

Chuẩn topic đề xuất:

```text
iact.[service_name].[entity].[event]
```

Ví dụ:

- `iact.user.user.created`
- `iact.activity.activity.approved`
- `iact.activity.registration.cancelled`
- `iact.notification.notification.created`

Lưu ý compatibility:

- Source hiện tại đang có một số topic legacy/ngắn hơn như `iact.notification.created`, `iact.activity.deleted`, `iact.identity.user.created`, `iact.identity.user.snapshot`.
- Không nên đổi breaking ngay. Nếu triển khai chuẩn hóa, cần hỗ trợ song song legacy topic và topic mới trong một giai đoạn migration.
- Các consumer phải idempotent để xử lý được retry, duplicate message và replay.

## 3. Payload contract chuẩn

Mọi Kafka message mới nên dùng JSON với envelope thống nhất:

```json
{
  "eventId": "uuid",
  "eventVersion": 1,
  "eventType": "ACTIVITY_APPROVED",
  "aggregateType": "activity",
  "aggregateId": "123",
  "occurredAt": "2026-06-27T10:00:00Z",
  "producer": "activity-service",
  "actor": {
    "userId": 1,
    "username": "admin",
    "role": "ROLE_ADMIN"
  },
  "payload": {},
  "metadata": {
    "correlationId": "uuid",
    "source": "api"
  }
}
```

Yêu cầu contract:

- `eventId`: idempotency key mặc định.
- `eventVersion`: hỗ trợ versioning khi payload thay đổi.
- `eventType`: enum nghiệp vụ rõ nghĩa, không phụ thuộc vào số magic number.
- `aggregateType` và `aggregateId`: xác định business object chính.
- `occurredAt`: thời điểm event xảy ra, dùng ISO-8601.
- `producer`: service phát event.
- `actor`: user thực hiện hành động nếu có.
- `payload`: dữ liệu tối thiểu để consumer xử lý mà không cần gọi sync sang service khác.
- `metadata.correlationId`: nối trace giữa API request, Kafka event và log.

## 4. Topic hiện có trong source code

| Topic | Status | Producer | Consumer | Business trigger | Payload chính | Idempotency key | Consumer group | Priority | Ghi chú migration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `iact.notification.created` | existing/legacy | `activity-service` | `notification-service` | Activity lifecycle, registration, cancellation, check-in | `userId`, `activityId`, `title`, `content`, `message`, `type` | Chưa có `eventId`; tạm dùng `userId + activityId + type + occurredAt` nếu bổ sung | `notification-create-group-v1` | P0 | Nên giữ compatibility, sau đó map dần sang `iact.notification.notification.created` |
| `iact.activity.deleted` | existing/legacy | `activity-service` | `notification-service` | Delete activity, cleanup draft quá hạn | `activityId` | `activityId + eventType` | `notification-group` | P0 | Nên map dần sang `iact.activity.activity.deleted` hoặc `iact.notification.notification.cleanup-requested` tùy mục tiêu |
| `iact.identity.user.snapshot` | existing/legacy | `user-service` | `activity-service` | Publish user snapshot sau register/profile update/replay | `userId`, `username`, `email`, `fullName`, `studentCode`, `avatarUrl`, `departmentId` | `eventId` hoặc `userId` | `activity-group-v1` | P0 | Nên chuẩn hóa thành `iact.user.user.snapshot` |
| `iact.identity.user.created` | legacy/inbound | Chưa thấy producer nội bộ trực tiếp trong source hiện tại | `user-service`, `activity-service` | User created từ identity flow cũ hoặc external identity source | JSON user data, tối thiểu `userId`, `fullName`, `username` | `userId` | `profile-group-v1`, `activity-group-v1` | P0 | Cần xác nhận producer thật trước khi thay đổi |

## 5. Topic catalog đề xuất theo service/domain

### 5.1 User/Identity topics

| Topic | Status | Producer | Consumer | Business trigger | Payload chính | Idempotency key | Consumer group đề xuất | Priority | Ghi chú migration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `iact.user.user.created` | recommended | `user-service` | `activity-service`, `notification-service`, `audit/analytics` | Register user, admin create user, import user | User identity + profile summary + role | `eventId`, fallback `userId` | `activity-user-projection-v1`, `notification-user-v1` | P1 | Thay thế dần `iact.identity.user.created` |
| `iact.user.user.updated` | recommended | `user-service` | `activity-service`, `notification-service`, `audit/analytics` | Admin update user hoặc user sync từ Keycloak | User changed fields + profile summary | `eventId`, fallback `userId + updatedAt` | `activity-user-projection-v1` | P1 | Dùng để giữ local user projection mới |
| `iact.user.user.deactivated` | recommended | `user-service` | `activity-service`, `notification-service`, `audit/analytics` | Disable user/account status change | `userId`, `keycloakId`, `status`, `reason` | `eventId`, fallback `userId + status` | `activity-user-projection-v1` | P1 | Giúp các service chặn nghiệp vụ với user bị khóa |
| `iact.user.user.deleted` | recommended | `user-service` | `activity-service`, `notification-service`, `audit/analytics` | Soft delete hoặc delete user | `userId`, `keycloakId`, `deletedAt` | `eventId`, fallback `userId` | `activity-user-projection-v1` | P1 | Nên ưu tiên soft delete cho dữ liệu quan trọng |
| `iact.user.user.role-changed` | recommended | `user-service` | `activity-service`, `notification-service`, `audit/analytics` | Role assignment/change | `userId`, `oldRole`, `newRole`, `departmentId` | `eventId`, fallback `userId + newRole` | `activity-user-projection-v1` | P1 | Quan trọng cho RBAC projection |
| `iact.user.profile.created` | recommended | `user-service` | `activity-service`, `notification-service` | Tạo student/department profile | `userId`, `fullName`, `studentCode`, `departmentId`, `majorId`, `classId` | `eventId`, fallback `userId` | `activity-user-projection-v1` | P1 | Có thể gộp vào `user.created` nếu payload đủ |
| `iact.user.profile.updated` | recommended | `user-service` | `activity-service`, `notification-service` | Update profile/avatar/student code | Changed profile fields | `eventId`, fallback `userId + updatedAt` | `activity-user-projection-v1` | P1 | Cần cho organizer/student display data |
| `iact.user.department.created` | recommended | `user-service` | `activity-service`, `notification-service`, `analytics` | Admin tạo department | Department summary | `eventId`, fallback `departmentId` | `activity-master-data-v1` | P1 | Giúp activity filter/report không gọi sync |
| `iact.user.department.updated` | recommended | `user-service` | `activity-service`, `notification-service`, `analytics` | Admin cập nhật department | Changed department fields | `eventId`, fallback `departmentId + updatedAt` | `activity-master-data-v1` | P1 | Dùng cho cache invalidation |
| `iact.user.department.deleted` | recommended | `user-service` | `activity-service`, `notification-service`, `analytics` | Admin xóa/disable department | `departmentId`, `deletedAt` | `eventId`, fallback `departmentId` | `activity-master-data-v1` | P1 | Cần kiểm tra dữ liệu phụ thuộc trước khi triển khai |
| `iact.user.major.created` | recommended | `user-service` | `activity-service`, `analytics` | Admin tạo major | Major summary | `eventId`, fallback `majorId` | `activity-master-data-v1` | P2 | Hữu ích cho reporting/filter |
| `iact.user.major.updated` | recommended | `user-service` | `activity-service`, `analytics` | Admin cập nhật major | Changed major fields | `eventId`, fallback `majorId + updatedAt` | `activity-master-data-v1` | P2 | Hữu ích cho cache invalidation |
| `iact.user.major.deleted` | recommended | `user-service` | `activity-service`, `analytics` | Admin xóa/disable major | `majorId`, `deletedAt` | `eventId`, fallback `majorId` | `activity-master-data-v1` | P2 | Nên soft delete |
| `iact.user.class.created` | recommended | `user-service` | `activity-service`, `analytics` | Admin tạo class | Class summary | `eventId`, fallback `classId` | `activity-master-data-v1` | P2 | Hữu ích cho cohort analytics |
| `iact.user.class.updated` | recommended | `user-service` | `activity-service`, `analytics` | Admin cập nhật class | Changed class fields | `eventId`, fallback `classId + updatedAt` | `activity-master-data-v1` | P2 | Hữu ích cho cohort analytics |
| `iact.user.class.deleted` | recommended | `user-service` | `activity-service`, `analytics` | Admin xóa/disable class | `classId`, `deletedAt` | `eventId`, fallback `classId` | `activity-master-data-v1` | P2 | Nên soft delete |
| `iact.user.preference.created` | recommended | `user-service` | `activity-service`, `analytics` | Student tạo preference | `userId`, preference categories/tags | `eventId`, fallback `userId` | `activity-recommendation-v1` | P1 | Kích hoạt recommendation refresh |
| `iact.user.preference.updated` | recommended | `user-service` | `activity-service`, `analytics` | Student cập nhật preference | Changed preference fields | `eventId`, fallback `userId + updatedAt` | `activity-recommendation-v1` | P1 | Kích hoạt recommendation refresh |
| `iact.user.preference.deleted` | recommended | `user-service` | `activity-service`, `analytics` | Student xóa preference | `userId`, deleted preferences | `eventId`, fallback `userId` | `activity-recommendation-v1` | P2 | Kích hoạt recommendation refresh |
| `iact.user.user.snapshot` | recommended | `user-service` | `activity-service` | Projection replay hoặc after commit publish | Full user projection snapshot | `eventId`, fallback `userId` | `activity-user-projection-v1` | P0 | Topic chuẩn thay cho `iact.identity.user.snapshot` |
| `iact.user.user.replay-requested` | future | `admin/tooling` | `user-service` | Admin yêu cầu replay user projection | Filter/replay scope | `eventId`, fallback `requestedAt + scope` | `user-projection-replay-v1` | P2 | Dùng khi cần rebuild projection |

### 5.2 Activity topics

| Topic | Status | Producer | Consumer | Business trigger | Payload chính | Idempotency key | Consumer group đề xuất | Priority | Ghi chú migration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `iact.activity.activity.created` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Department/Admin tạo activity | Activity summary, organizer, department, schedule summary | `eventId`, fallback `activityId` | `notification-activity-v1` | P1 | Có thể chỉ notify khi activity submitted/approved |
| `iact.activity.activity.updated` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Update activity | Changed fields + activity summary | `eventId`, fallback `activityId + updatedAt` | `notification-activity-v1` | P1 | Hiện đang đi qua `iact.notification.created` |
| `iact.activity.activity.deleted` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Delete activity | `activityId`, `deletedAt`, `reason` | `eventId`, fallback `activityId` | `notification-cleanup-v1` | P0 | Topic chuẩn thay cho `iact.activity.deleted` |
| `iact.activity.activity.draft-saved` | future | `activity-service` | `analytics`, `audit` | Save draft | Draft summary | `eventId`, fallback `activityId + updatedAt` | `analytics-activity-v1` | P2 | Không nhất thiết notify user |
| `iact.activity.activity.draft-expired` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Scheduled cleanup draft quá hạn | `activityId`, `expiredAt` | `eventId`, fallback `activityId` | `notification-cleanup-v1` | P1 | Hiện cleanup phát `iact.activity.deleted` |
| `iact.activity.activity.submitted` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Submit activity for approval | Activity summary, submitter | `eventId`, fallback `activityId + submittedAt` | `notification-activity-v1` | P1 | Gửi notification cho Admin |
| `iact.activity.activity.approved` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Admin approve activity | Activity summary, approver | `eventId`, fallback `activityId + approvedAt` | `notification-activity-v1` | P0 | Hiện đang tạo notification trực tiếp qua legacy topic |
| `iact.activity.activity.rejected` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Admin reject activity | Activity summary, reason, approver | `eventId`, fallback `activityId + rejectedAt` | `notification-activity-v1` | P0 | Hiện đang tạo notification trực tiếp qua legacy topic |
| `iact.activity.activity.cancelled` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Admin cancel activity | Activity summary, reason, affected users | `eventId`, fallback `activityId + cancelledAt` | `notification-activity-v1` | P0 | Cần notify participant/owner |
| `iact.activity.schedule.created` | future | `activity-service` | `notification-service`, `analytics` | Add schedule/session | Schedule summary | `eventId`, fallback `scheduleId` | `notification-schedule-v1` | P2 | Cần nếu schedule update sau khi student đã đăng ký |
| `iact.activity.schedule.updated` | future | `activity-service` | `notification-service`, `analytics` | Change time/location | Changed schedule fields | `eventId`, fallback `scheduleId + updatedAt` | `notification-schedule-v1` | P2 | Nên notify registered students |
| `iact.activity.schedule.deleted` | future | `activity-service` | `notification-service`, `analytics` | Delete session | `scheduleId`, `activityId` | `eventId`, fallback `scheduleId` | `notification-schedule-v1` | P2 | Nên notify registered students |
| `iact.activity.benefit.created` | recommended | `activity-service` | `analytics`, `audit` | Add benefit | Benefit summary | `eventId`, fallback `benefitId` | `analytics-benefit-v1` | P1 | Phục vụ point calculation/report |
| `iact.activity.benefit.updated` | recommended | `activity-service` | `analytics`, `audit` | Update benefit | Changed benefit fields | `eventId`, fallback `benefitId + updatedAt` | `analytics-benefit-v1` | P1 | Có thể trigger point recalculation |
| `iact.activity.benefit.deleted` | recommended | `activity-service` | `analytics`, `audit` | Delete benefit | `benefitId`, `activityId` | `eventId`, fallback `benefitId` | `analytics-benefit-v1` | P1 | Có thể trigger point recalculation |
| `iact.activity.category.created` | recommended | `activity-service` | `analytics`, `audit` | Admin create category | Category summary | `eventId`, fallback `categoryId` | `analytics-master-data-v1` | P2 | Master data event |
| `iact.activity.category.updated` | recommended | `activity-service` | `analytics`, `audit` | Admin update category | Changed category fields | `eventId`, fallback `categoryId + updatedAt` | `analytics-master-data-v1` | P2 | Master data cache invalidation |
| `iact.activity.category.deleted` | recommended | `activity-service` | `analytics`, `audit` | Admin delete category | `categoryId` | `eventId`, fallback `categoryId` | `analytics-master-data-v1` | P2 | Cần kiểm tra dependency |
| `iact.activity.semester.created` | recommended | `activity-service` | `analytics`, `audit` | Admin create semester | Semester summary | `eventId`, fallback `semesterId` | `analytics-master-data-v1` | P2 | Master data event |
| `iact.activity.semester.updated` | recommended | `activity-service` | `analytics`, `audit` | Admin update semester | Changed semester fields | `eventId`, fallback `semesterId + updatedAt` | `analytics-master-data-v1` | P2 | Có thể invalidate dashboard |
| `iact.activity.semester.deleted` | recommended | `activity-service` | `analytics`, `audit` | Admin delete semester | `semesterId` | `eventId`, fallback `semesterId` | `analytics-master-data-v1` | P2 | Cần kiểm tra dependency |

### 5.3 Registration, attendance, proof và point topics

| Topic | Status | Producer | Consumer | Business trigger | Payload chính | Idempotency key | Consumer group đề xuất | Priority | Ghi chú migration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `iact.activity.registration.created` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Student đăng ký activity | `registrationId`, `userId`, `activityId`, `activityTitle` | `eventId`, fallback `registrationId` | `notification-registration-v1` | P0 | Hiện notification đi qua `iact.notification.created` |
| `iact.activity.registration.cancelled` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Student hủy đăng ký | `registrationId`, `userId`, `activityId`, `reason` | `eventId`, fallback `registrationId + cancelledAt` | `notification-registration-v1` | P0 | Hiện notification đi qua `iact.notification.created` |
| `iact.activity.registration.approved` | future | `activity-service` | `notification-service`, `analytics`, `audit` | Admin/Department approve registration nếu nghiệp vụ cần | Registration summary | `eventId`, fallback `registrationId` | `notification-registration-v1` | P2 | Chưa thấy flow rõ trong source hiện tại |
| `iact.activity.registration.rejected` | future | `activity-service` | `notification-service`, `analytics`, `audit` | Admin/Department reject registration nếu nghiệp vụ cần | Registration summary + reason | `eventId`, fallback `registrationId` | `notification-registration-v1` | P2 | Chưa thấy flow rõ trong source hiện tại |
| `iact.activity.attendance.checked-in` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Student QR check-in hoặc organizer check-in | `attendanceId`, `userId`, `activityId`, `scheduleId`, `checkedInAt` | `eventId`, fallback `attendanceId` | `notification-attendance-v1` | P0 | Hiện check-in notification đi qua `iact.notification.created` |
| `iact.activity.attendance.updated` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Manual correction attendance | Changed attendance fields | `eventId`, fallback `attendanceId + updatedAt` | `analytics-attendance-v1` | P1 | Có thể trigger point recalculation |
| `iact.activity.attendance.deleted` | future | `activity-service` | `notification-service`, `analytics`, `audit` | Delete/revoke attendance | `attendanceId`, `reason` | `eventId`, fallback `attendanceId` | `analytics-attendance-v1` | P2 | Nên soft delete nếu có điểm liên quan |
| `iact.activity.proof.submitted` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Student upload proof | `proofId`, `userId`, `activityId`, file metadata | `eventId`, fallback `proofId` | `notification-proof-v1` | P1 | Notify Department/Admin review |
| `iact.activity.proof.approved` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Admin/Department approve proof | `proofId`, `userId`, `activityId`, reviewer | `eventId`, fallback `proofId + approvedAt` | `notification-proof-v1` | P1 | Có thể trigger point awarded |
| `iact.activity.proof.rejected` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Admin/Department reject proof | `proofId`, `reason`, reviewer | `eventId`, fallback `proofId + rejectedAt` | `notification-proof-v1` | P1 | Notify student |
| `iact.activity.point.awarded` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Award training point after attendance/proof | `pointId`, `userId`, `activityId`, category, point | `eventId`, fallback `pointId` | `notification-point-v1` | P1 | Hữu ích cho student notification |
| `iact.activity.point.recalculated` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Recalculate points sau benefit/attendance/proof change | `userId`, `activityId`, old/new point summary | `eventId`, fallback `userId + activityId + recalculatedAt` | `analytics-point-v1` | P1 | Cần cho báo cáo điểm rèn luyện |
| `iact.activity.point.revoked` | recommended | `activity-service` | `notification-service`, `analytics`, `audit` | Revoke point do cancel/invalid attendance/proof | `pointId`, `reason` | `eventId`, fallback `pointId` | `notification-point-v1` | P1 | Notify student nếu ảnh hưởng điểm |

### 5.4 Notification topics

| Topic | Status | Producer | Consumer | Business trigger | Payload chính | Idempotency key | Consumer group đề xuất | Priority | Ghi chú migration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `iact.notification.notification.created` | recommended | Any business service | `notification-service` | Create notification request | `targetType`, `userId`, `role`, `activityId`, `title`, `content`, `type`, `severity` | `eventId`, fallback business key | `notification-create-v1` | P0 | Topic chuẩn thay cho `iact.notification.created` |
| `iact.notification.notification.dispatched` | recommended | `notification-service` | `analytics`, `audit` | Notification saved and sent via WebSocket | `notificationId`, target, dispatch status | `eventId`, fallback `notificationId` | `analytics-notification-v1` | P1 | Ghi nhận delivery lifecycle |
| `iact.notification.notification.read` | recommended | `notification-service` | `analytics`, `audit` | User mark notification read | `notificationId`, `userId`, `readAt` | `eventId`, fallback `notificationId + userId` | `analytics-notification-v1` | P1 | Dùng cho engagement metrics |
| `iact.notification.notification.unread` | future | `notification-service` | `analytics`, `audit` | User mark unread | `notificationId`, `userId` | `eventId`, fallback `notificationId + userId` | `analytics-notification-v1` | P2 | Tùy UI có hỗ trợ hay không |
| `iact.notification.notification.deleted` | recommended | `notification-service` | `analytics`, `audit` | User/admin delete notification | `notificationId`, `userId`, `deletedAt` | `eventId`, fallback `notificationId` | `analytics-notification-v1` | P1 | Dùng cho audit |
| `iact.notification.notification.cleanup-requested` | recommended | `activity-service`, `system scheduler` | `notification-service` | Cleanup notification by activity/user/time range | `activityId`, `userId`, `before`, `reason` | `eventId`, fallback cleanup scope | `notification-cleanup-v1` | P1 | Có thể thay một phần `iact.activity.deleted` |
| `iact.notification.notification.broadcast-requested` | recommended | `notification-service`, `admin tool` | `notification-service` | Admin/Department broadcast | `targetType`, `role`, `departmentId`, `title`, `content` | `eventId` | `notification-broadcast-v1` | P1 | RBAC: `ROLE_ADMIN`, có thể `ROLE_DEPARTMENT` |
| `iact.notification.notification.urgent-requested` | recommended | `notification-service`, `admin tool` | `notification-service` | Urgent notification | Target + urgent metadata | `eventId` | `notification-urgent-v1` | P1 | Cần rate limit và audit |
| `iact.notification.delivery.failed` | future | `notification-service` | `notification-service`, `audit` | WebSocket/save delivery failure | Failure reason + notification request | `eventId`, fallback `notificationId + failedAt` | `notification-retry-v1` | P2 | Dùng retry hoặc DLQ |
| `iact.notification.delivery.retried` | future | `notification-service` | `analytics`, `audit` | Retry delivery | Retry count + status | `eventId` | `analytics-notification-v1` | P2 | Dùng observability |

### 5.5 Analytics, audit và system topics

| Topic | Status | Producer | Consumer | Business trigger | Payload chính | Idempotency key | Consumer group đề xuất | Priority | Ghi chú migration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `iact.audit.event.created` | future | All services | Audit storage/reporting | CRUD quan trọng hoặc security-sensitive actions | Actor, action, target, before/after summary | `eventId` | `audit-store-v1` | P2 | Cần DB/table audit riêng nếu triển khai |
| `iact.analytics.dashboard.refresh-requested` | recommended | `user-service`, `activity-service`, `notification-service` | `activity-service` hoặc future analytics service | Activity/user/registration/attendance changes | Refresh scope + affected IDs | `eventId`, fallback scope key | `dashboard-refresh-v1` | P1 | Thay polling/scheduled-only cache refresh về sau |
| `iact.analytics.stats.recalculation-requested` | future | `activity-service`, admin tool | Analytics/stat service | Recalculate stats manually/scheduled | Scope, semesterId, departmentId | `eventId` | `stats-recalc-v1` | P2 | Hữu ích khi dữ liệu bị lệch |
| `iact.analytics.recommendation.refresh-requested` | recommended | `user-service`, `activity-service` | `activity-service` hoặc future recommendation service | Preference/activity/category/point changes | `userId`, `semesterId`, changed factors | `eventId`, fallback `userId + scope` | `recommendation-refresh-v1` | P1 | Hỗ trợ recommendation async |
| `iact.system.projection.replay-requested` | future | Admin tool | Owning service | Replay projection toàn phần hoặc theo scope | Service, entity, range/filter | `eventId` | `projection-replay-v1` | P2 | Dùng cho rebuild projection sau sự cố |
| `iact.system.cache.invalidated` | future | Any service | Services owning cache | Master data or lifecycle event invalidates cache | Cache name, key, reason | `eventId`, fallback cache key | `cache-invalidation-v1` | P2 | Có thể dùng cho dashboard/master data |
| `iact.system.integration.failed` | future | Any service | Ops/audit/notification | External integration failure: Keycloak, Cloudinary, Kafka | Integration name, operation, reason | `eventId` | `ops-integration-v1` | P2 | Có thể notify admin |
| `iact.system.dead-letter` | future | Kafka error handlers | Ops/replay tooling | Message cannot be processed after retries | Original topic, payload, exception summary | Original `eventId` | `dlq-monitor-v1` | P1 | Nên có khi Kafka usage mở rộng |

## 6. Data flow nghiệp vụ

### 6.1 User registration/import

1. `user-service` tạo user trong Keycloak và lưu `user_db`.
2. Sau transaction commit, `user-service` publish user event/snapshot.
3. `activity-service` consume user event để upsert local user projection trong `activity_db.users`.
4. `notification-service` có thể consume `iact.user.user.created` hoặc `iact.notification.notification.created` để gửi welcome/admin notification nếu nghiệp vụ cần.
5. `analytics/audit` consume để ghi nhận số lượng user mới, import result và security audit.

Topic P0/P1 liên quan:

- Hiện tại: `iact.identity.user.snapshot`, `iact.identity.user.created`.
- Đề xuất: `iact.user.user.created`, `iact.user.user.snapshot`, `iact.user.profile.created`, `iact.audit.event.created`.

### 6.2 Activity lifecycle

1. Department/Admin tạo hoặc cập nhật activity.
2. Khi submit/approve/reject/cancel/delete, `activity-service` publish activity lifecycle event.
3. `notification-service` tạo notification theo target: owner, registered students, role, department hoặc public.
4. `analytics/dashboard` invalidate hoặc refresh cache async.
5. `audit` lưu lại actor, action, reason và affected aggregate.

Topic P0/P1 liên quan:

- Hiện tại: `iact.notification.created`, `iact.activity.deleted`.
- Đề xuất: `iact.activity.activity.submitted`, `iact.activity.activity.approved`, `iact.activity.activity.rejected`, `iact.activity.activity.cancelled`, `iact.activity.activity.deleted`, `iact.notification.notification.created`.

### 6.3 Registration lifecycle

1. Student đăng ký hoặc hủy đăng ký activity.
2. `activity-service` lưu registration và publish registration event.
3. `notification-service` gửi notification cá nhân tới `/topic/user/{userId}`.
4. `analytics/dashboard` cập nhật số lượng đăng ký, sức chứa, tỷ lệ tham gia.
5. `audit` ghi nhận thao tác nếu cần.

Topic P0/P1 liên quan:

- Hiện tại: `iact.notification.created`.
- Đề xuất: `iact.activity.registration.created`, `iact.activity.registration.cancelled`, `iact.notification.notification.created`, `iact.analytics.dashboard.refresh-requested`.

### 6.4 Attendance/check-in

1. Student hoặc Admin/Department thực hiện check-in QR/manual.
2. `activity-service` lưu attendance và publish `iact.activity.attendance.checked-in`.
3. `notification-service` gửi thông báo check-in thành công nếu cần.
4. Point logic trong `activity-service` hoặc future point service consume để award/recalculate points.
5. `analytics/dashboard` cập nhật attendance rate.

Topic P0/P1 liên quan:

- Hiện tại: `iact.notification.created`.
- Đề xuất: `iact.activity.attendance.checked-in`, `iact.activity.point.awarded`, `iact.analytics.dashboard.refresh-requested`.

### 6.5 Proof và point lifecycle

1. Student upload proof.
2. Admin/Department approve/reject proof.
3. Nếu proof hợp lệ, point được award/recalculate.
4. Student nhận notification kết quả.
5. Analytics cập nhật point distribution và completion progress.

Topic P1 liên quan:

- `iact.activity.proof.submitted`
- `iact.activity.proof.approved`
- `iact.activity.proof.rejected`
- `iact.activity.point.awarded`
- `iact.activity.point.recalculated`
- `iact.activity.point.revoked`

### 6.6 Cleanup/delete/replay

1. Activity bị xóa hoặc draft quá hạn bị cleanup.
2. `activity-service` publish delete/cleanup event.
3. `notification-service` xóa notification theo `activityId`.
4. Analytics/audit ghi nhận.
5. Khi projection lệch, Admin/tooling publish replay request để owning service phát lại snapshot.

Topic P0/P1/P2 liên quan:

- Hiện tại: `iact.activity.deleted`.
- Đề xuất: `iact.activity.activity.deleted`, `iact.activity.activity.draft-expired`, `iact.notification.notification.cleanup-requested`, `iact.system.projection.replay-requested`.

## 7. Notification system hiện tại và hướng chuẩn hóa

Hiện tại `activity-service` publish trực tiếp `NotificationRequest` vào `iact.notification.created`. `notification-service` consume, lưu DB rồi dispatch qua WebSocket:

- Có `userId`: `/topic/user/{userId}`.
- Không có `userId`: `/topic/notifications`.

Payload hiện tại:

```json
{
  "userId": 123,
  "activityId": 456,
  "title": "Dang ky thanh cong",
  "content": "Ban da dang ky thanh cong hoat dong...",
  "message": "Dang ky thanh cong hoat dong...",
  "type": 1
}
```

Khuyến nghị chuẩn hóa:

- Thêm `eventId`, `eventVersion`, `eventType`, `targetType`, `severity`.
- Thay `type` số bằng enum hoặc ít nhất document rõ mapping.
- Tách rõ notification command topic `iact.notification.notification.created` với business event topic như `iact.activity.registration.created`.
- Trong migration phase, `notification-service` có thể consume cả `iact.notification.created` và `iact.notification.notification.created`.

Mapping `type` tạm thời:

| Type | Ý nghĩa đang dùng | Vấn đề |
| --- | --- | --- |
| `1` | Success/approval: đăng ký thành công, hoạt động được duyệt | Chấp nhận được |
| `2` | Info/update: hủy đăng ký, cập nhật hoạt động | Cần document rõ |
| `3` | Check-in success hoặc reject/cancel warning | Đang lẫn nghĩa, nên thay bằng enum |

Enum đề xuất:

- `REGISTRATION_SUCCESS`
- `REGISTRATION_CANCELLED`
- `CHECKIN_SUCCESS`
- `ACTIVITY_UPDATED`
- `ACTIVITY_APPROVED`
- `ACTIVITY_REJECTED`
- `ACTIVITY_CANCELLED`
- `PROOF_SUBMITTED`
- `PROOF_APPROVED`
- `PROOF_REJECTED`
- `POINT_AWARDED`
- `URGENT_BROADCAST`

## 8. Consumer groups đề xuất

| Service | Consumer group | Topic chính | Mục đích |
| --- | --- | --- | --- |
| `activity-service` | `activity-user-projection-v1` | User/profile/snapshot topics | Upsert local user projection |
| `activity-service` | `activity-master-data-v1` | Department/major/class topics | Sync master data nếu cần |
| `activity-service` | `activity-recommendation-v1` | Preference/activity/point topics | Refresh recommendation |
| `notification-service` | `notification-create-v1` | Notification command topic | Save and dispatch notification |
| `notification-service` | `notification-activity-v1` | Activity lifecycle topics | Create notifications from activity events |
| `notification-service` | `notification-registration-v1` | Registration topics | Create student notifications |
| `notification-service` | `notification-attendance-v1` | Attendance topics | Create check-in notifications |
| `notification-service` | `notification-cleanup-v1` | Delete/cleanup topics | Delete related notifications |
| Future analytics | `analytics-dashboard-v1` | User/activity/registration/attendance/point topics | Update dashboard/stat projections |
| Future audit | `audit-store-v1` | Audit and business topics | Persist audit trail |
| Ops/tooling | `dlq-monitor-v1` | Dead-letter topic | Monitor and replay failed messages |

## 9. Idempotency và error handling

Quy tắc bắt buộc khi triển khai:

- Mỗi event phải có `eventId`.
- Consumer phải lưu hoặc kiểm tra `eventId`/business key trước khi mutate DB.
- Projection upsert phải dùng business key ổn định như `userId`, `activityId`, `registrationId`.
- Delete/cleanup event phải chấp nhận việc dữ liệu đã bị xóa trước đó.
- Notification creation nên chống duplicate bằng `eventId` hoặc `sourceEventId`.
- Retry phải không tạo duplicate notification/point/audit.
- Khi mở rộng Kafka usage, nên có DLQ như `iact.system.dead-letter`.

Khuyến nghị DB khi triển khai thật:

- Mỗi consumer service nên có bảng `processed_kafka_events`.
- Nếu cần audit đầy đủ, thêm bảng `audit_events`.
- Mọi DB schema change phải có plan riêng và duyệt riêng.

## 10. API/RBAC liên quan đến event trigger

Task này không tạo API mới. Khi triển khai event publishing, cần giữ RBAC hiện tại:

| Nghiệp vụ | API/domain | Role |
| --- | --- | --- |
| User/admin CRUD | `/api/v1/users`, departments, majors, classes | `ROLE_ADMIN` |
| Auth/register | `/auth` | Public hoặc authenticated tùy endpoint |
| Profile/preference | `/api/v1/user-profiles`, `/api/v1/student-preferences` | Owner hoặc `ROLE_STUDENT` |
| Activity management | `/api/v1/activities` | `ROLE_ADMIN`, `ROLE_DEPARTMENT`, một số endpoint có `OTHER` theo source hiện tại |
| Activity approval/reject/cancel | `/api/v1/activities` action endpoints | `ROLE_ADMIN` |
| Registration | `/api/v1/registrations` | `ROLE_STUDENT`, `ROLE_ADMIN`, `ROLE_DEPARTMENT` theo permission |
| Attendance | `/api/v1/attendances` | `ROLE_STUDENT`, `ROLE_ADMIN`, `ROLE_DEPARTMENT` |
| Proof | `/api/v1/proofs` | `ROLE_STUDENT` và reviewer nếu bổ sung |
| Notification read/unread | `/api/v1/notifications` | Authenticated user |
| Broadcast/urgent notification | `/api/v1/notifications` | `ROLE_ADMIN`, có thể `ROLE_DEPARTMENT` theo nghiệp vụ |
| Internal notification dispatch | `/internal/notifications` | Internal-only, không thay thế Kafka flow |

## 11. Roadmap triển khai đề xuất

### P0: Giữ hệ thống hiện tại chạy ổn

- Document và không phá các legacy topics hiện có.
- Bổ sung `eventId`/`eventType` cho notification payload nếu có task code riêng.
- Đảm bảo `iact.identity.user.snapshot` tiếp tục đồng bộ `activity_db.users`.
- Đảm bảo `iact.activity.deleted` tiếp tục cleanup notification liên quan.

### P1: Chuẩn hóa event-driven cho nghiệp vụ chính

- Thêm topic chuẩn song song với legacy topic:
  - `iact.user.user.snapshot`
  - `iact.notification.notification.created`
  - `iact.activity.activity.approved`
  - `iact.activity.activity.rejected`
  - `iact.activity.activity.cancelled`
  - `iact.activity.registration.created`
  - `iact.activity.registration.cancelled`
  - `iact.activity.attendance.checked-in`
- Thêm idempotency store cho consumer.
- Chuẩn hóa notification event type enum.

### P2: Nâng cao observability, analytics và replay

- Thêm audit event store.
- Thêm dashboard/recommendation refresh topics.
- Thêm DLQ và replay tooling.
- Tách analytics service nếu dashboard/stat workload lớn.

## 12. Kết luận

Hệ thống hiện tại đã có Kafka cho hai trục chính:

- Notification realtime: `activity-service` -> `notification-service`.
- User projection: `user-service` -> `activity-service`.

Để bao phủ đầy đủ CRUD và nghiệp vụ nâng cao, hệ thống nên chuyển dần từ topic legacy dạng command chung sang event catalog rõ domain hơn. Cách triển khai an toàn là hỗ trợ song song legacy topics và standardized topics, thêm idempotency trước khi mở rộng consumer, sau đó mới migration producer theo từng business flow.

