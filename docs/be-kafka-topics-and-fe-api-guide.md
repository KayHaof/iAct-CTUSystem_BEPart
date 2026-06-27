# Kafka va Notification FE Integration Guide

Ngay cap nhat: 2026-06-28

Tai lieu nay chi tong hop nhung phan lien quan den Kafka, notification-service va cac API/luong ma FE can biet de xay dung UI xu ly thong bao. Cac API CRUD chung cua user/activity/master-data khong duoc liet ke lai o day.

## 1. Muc tieu cho FE

FE can xay cac phan sau:

- Notification bell va unread badge.
- Notification center: danh sach, loc chua doc/da doc, danh dau da doc, danh dau tat ca da doc, xoa thong bao.
- Realtime notification qua WebSocket.
- Form gui thong bao khan cap cho ADMIN/DEPARTMENT.
- Cac UI nghiep vu co tac dong Kafka notification:
  - Duyet/tu choi/huy activity.
  - Dang ky/huy dang ky activity.
  - Check-in.
  - Nop/duyet/tu choi minh chung.
  - Cap nhat notification preference cua sinh vien.

## 2. Ket luan kiem tra hien trang Kafka

Trang thai hien tai:

- Luong business event sang notification-service da duoc noi cho activity, registration, attendance, proof, point va user preference.
- Notification-service nhan event, tao notification, luu vao `notification_db.notifications`, kiem tra `notification_preferences`, dispatch WebSocket va phat lifecycle event.
- User khong bi xoa hoan toan. He thong dung `user.deactivated` cho khoa/ban/deactivate.
- Khong co nghiep vu doi role user, nen `user.role-changed` chi la constant/topic du phong.
- `user.deleted` chi la du phong, runtime khong phat vi he thong khong xoa user vinh vien.
- `system.dead-letter` da co topic nhung chua co error-handler DLQ that. FE chua can xu ly.

Verify gan nhat:

```powershell
mvn.cmd -pl common,user-service,activity-service,notification-service -am -DskipTests compile
```

Ket qua: `BUILD SUCCESS`.

## 3. WebSocket notification cho FE

Notification-service cau hinh STOMP:

| Hang muc | Gia tri |
| --- | --- |
| WebSocket endpoint | `/ws` |
| Handshake query | `userId={currentUserId}` |
| Broker prefixes | `/topic`, `/queue` |
| App destination prefix | `/app` |
| User destination prefix | `/user` |

FE ket noi:

```text
/ws?userId={currentUserId}
```

FE subscribe:

| Channel | Muc dich |
| --- | --- |
| `/topic/user/{userId}` | Notification ca nhan cua user dang dang nhap |
| `/topic/notifications` | Notification public/broadcast/toan he thong |

Payload FE nhan duoc:

```json
{
  "id": 1,
  "userId": 10,
  "title": "Dang ky thanh cong",
  "message": "Ban da dang ky thanh cong hoat dong...",
  "type": 1,
  "activityId": 100,
  "isRead": false,
  "readAt": null,
  "createdAt": "2026-06-28T00:00:00",
  "sourceEventId": "uuid",
  "sourceTopic": "iact.activity.registration.created"
}
```

Goi y UI theo `type`:

| type | Y nghia de hien thi |
| --- | --- |
| `1` | Thanh cong / approved / positive |
| `2` | Thong tin / cap nhat / can xem |
| `3` | Can chu y / rejected / cancelled / urgent |
| `99` | System/admin account event neu co |

## 4. API notification-service FE can dung

Base path: `/api/v1/notifications`

| Method | Path | Role | Query/body | Muc dich FE |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/notifications` | Authenticated | `page`, `size`, `isRead` | Lay danh sach notification cua user hien tai |
| `GET` | `/api/v1/notifications/count-unread` | Authenticated | none | Lay so unread de hien badge |
| `GET` | `/api/v1/notifications/{id}` | Authenticated | path `id` | Xem chi tiet notification |
| `PUT` | `/api/v1/notifications/{id}/read` | Authenticated | path `id` | Danh dau mot notification da doc |
| `PUT` | `/api/v1/notifications/read-all` | Authenticated | none | Danh dau tat ca notification cua user da doc |
| `DELETE` | `/api/v1/notifications/{id}` | Authenticated | path `id` | Xoa notification cua user |
| `POST` | `/api/v1/notifications/urgent` | ADMIN, DEPARTMENT | `UrgentNotificationRequest` | Gui thong bao khan cap |

Response chinh:

```json
{
  "code": 200,
  "message": "...",
  "result": {
    "pageNumber": 1,
    "totalPage": 3,
    "totalRows": 52,
    "data": [
      {
        "id": 1,
        "userId": 10,
        "title": "Thong bao",
        "message": "Noi dung",
        "type": 2,
        "activityId": 100,
        "isRead": false,
        "readAt": null,
        "createdAt": "2026-06-28T00:00:00",
        "sourceEventId": "uuid",
        "sourceTopic": "iact.activity.activity.submitted"
      }
    ]
  }
}
```

`UrgentNotificationRequest`:

```json
{
  "title": "Thong bao khan",
  "message": "Noi dung",
  "priority": 3,
  "targetType": "ACTIVITY",
  "targetId": 100,
  "activityId": 100,
  "userIds": ["1", "2", "3"]
}
```

Luu y:

- Neu `userIds` rong/null, BE tao public notification va dispatch qua `/topic/notifications`.
- Neu co `userIds`, BE tao notification ca nhan cho tung user va dispatch qua `/topic/user/{userId}`.
- `priority` duoc map truc tiep ve `type`; mac dinh la `3` neu khong truyen.
- DTO hien co `targetType` goi y gom `ALL_DEPARTMENT`, `ACTIVITY`, `CLASS` va `targetId`, nhung service hien tai chua tu resolve recipient tu hai field nay. FE nen truyen `userIds` khi can gui dung danh sach user cu the; neu khong co `userIds` thi he thong xem nhu broadcast public.

## 5. API preference lien quan notification

Base path: `/api/v1/student-preferences`

| Method | Path | Role | Muc dich Kafka/notification |
| --- | --- | --- | --- |
| `GET` | `/api/v1/student-preferences` | STUDENT | FE lay setting hien tai de hien form cau hinh |
| `PUT` | `/api/v1/student-preferences` | STUDENT | Cap nhat preference, user-service phat `preference.updated`, notification-service sync vao `notification_preferences` |
| `POST` | `/api/v1/student-preferences/reset` | STUDENT | Reset preference, user-service phat `preference.updated` |

Phan `notificationSettings` FE can quan tam:

```json
{
  "notificationSettings": {
    "newActivityAlert": true,
    "reminderAlert": true,
    "reminderDaysBefore": 1
  }
}
```

Mapping trong notification-service:

| Setting | Notification type bi anh huong |
| --- | --- |
| `newActivityAlert` | type `1`, type `2` |
| `reminderAlert` | type `3` |
| `reminderDaysBefore` | Hien luu trong preference, chua co scheduler reminder rieng |

## 6. API nghiep vu kich hoat Kafka notification

Day khong phai danh sach CRUD day du. Chi gom API ma khi FE thao tac se tao event Kafka va co the sinh notification.

### 6.1 Activity lifecycle

Base path: `/api/v1/activities`

| Method | Path | Role | Kafka topic sinh ra | Notification effect |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/activities` | ADMIN, DEPARTMENT, OTHER | `activity.created`, neu khong phai draft them `activity.submitted` | Submitted tao notification public/admin ve hoat dong cho duyet |
| `PUT` | `/api/v1/activities/{id}` | Owner/admin/department permission | `activity.updated` | Thong bao cap nhat hoat dong neu co owner target |
| `DELETE` | `/api/v1/activities/{id}` | Owner/admin/department permission | `activity.deleted`, legacy `iact.activity.deleted` | Cleanup notification theo activity |
| `PUT` | `/api/v1/activities/{id}/approve` | ADMIN | `activity.approved` | Thong bao owner hoat dong da duyet |
| `PUT` | `/api/v1/activities/{id}/reject` | ADMIN | `activity.rejected` | Thong bao owner bi tu choi, kem reason |
| `PUT` | `/api/v1/activities/{id}/cancel` | ADMIN | `activity.cancelled` | Thong bao hoat dong bi huy |

Body reason:

```json
{
  "reason": "Ly do tu choi/huy"
}
```

Activity status FE nen map:

| status | Label goi y |
| --- | --- |
| `0` | Cho duyet |
| `1` | Da duyet |
| `2` | Bi tu choi |
| `3` | Ban nhap |
| `4` | Da huy |

### 6.2 Registration lifecycle

Base path: `/api/v1/registrations`

| Method | Path | Role | Kafka topic sinh ra | Notification effect |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/registrations/join` | STUDENT | `registration.created` | Thong bao dang ky thanh cong cho student |
| `PATCH` | `/api/v1/registrations/cancel-by-activity/{activityId}` | STUDENT | `registration.cancelled` | Thong bao huy dang ky |
| `DELETE` | `/api/v1/registrations/{id}` | STUDENT | `registration.cancelled` | Thong bao huy dang ky |
| `PUT` | `/api/v1/registrations/{id}/status` | ADMIN/DEPARTMENT permission | Hien chua phat topic rieng | FE khong nen ky vong notification tu API nay |

Registration status:

| status | Label goi y |
| --- | --- |
| `0` | Da dang ky |
| `1` | Da tham gia/check-in |
| `2` | Da huy |

### 6.3 Attendance/check-in

Base path: `/api/v1/attendances`

| Method | Path | Role | Kafka topic sinh ra | Notification effect |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/attendances/check-in` | STUDENT | `attendance.checked-in` | Thong bao check-in thanh cong |
| `POST` | `/api/v1/attendances/verify-qr` | ADMIN, DEPARTMENT | `attendance.checked-in` | Thong bao check-in thanh cong cho student |

### 6.4 Proof va point

Base path: `/api/v1/proofs`

| Method | Path | Role | Kafka topic sinh ra | Notification effect |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/proofs/submit` | STUDENT | `proof.submitted` | Thong bao cho owner/admin/department can duyet |
| `PUT` | `/api/v1/proofs/{id}/approve` | ADMIN, DEPARTMENT | `proof.approved`, `point.awarded` | Thong bao minh chung duoc duyet va diem duoc ghi nhan |
| `PUT` | `/api/v1/proofs/{id}/reject?reason=...` | ADMIN, DEPARTMENT | `proof.rejected`, `point.revoked` | Thong bao minh chung bi tu choi va diem bi thu hoi |

Proof status:

| status | Label goi y |
| --- | --- |
| `0` | Cho duyet |
| `1` | Da duyet |
| `2` | Bi tu choi |

## 7. Topic dac biet lien quan thong bao

### 7.1 Business topics notification-service dang nghe

| Topic | Producer | Consumer | FE nhan qua |
| --- | --- | --- | --- |
| `iact.activity.activity.created` | activity-service | notification-service | Khong tao notification, consumer bo qua co chu y |
| `iact.activity.activity.submitted` | activity-service | notification-service | `/topic/notifications` |
| `iact.activity.activity.updated` | activity-service | notification-service | `/topic/user/{userId}` neu co target |
| `iact.activity.activity.deleted` | activity-service | notification-service | Khong dispatch, cleanup notification theo `activityId` |
| `iact.activity.activity.approved` | activity-service | notification-service | `/topic/user/{ownerUserId}` |
| `iact.activity.activity.rejected` | activity-service | notification-service | `/topic/user/{ownerUserId}` |
| `iact.activity.activity.cancelled` | activity-service | notification-service | `/topic/user/{ownerUserId}` hoac public tuy payload |
| `iact.activity.activity.draft-expired` | activity-service | notification-service | Khong dispatch, cleanup notification theo `activityId` |
| `iact.activity.registration.created` | activity-service | notification-service | `/topic/user/{studentId}` |
| `iact.activity.registration.cancelled` | activity-service | notification-service | `/topic/user/{studentId}` |
| `iact.activity.attendance.checked-in` | activity-service | notification-service | `/topic/user/{studentId}` |
| `iact.activity.proof.submitted` | activity-service | notification-service | `/topic/user/{ownerUserId}` hoac public neu khong co owner |
| `iact.activity.proof.approved` | activity-service | notification-service | `/topic/user/{studentId}` |
| `iact.activity.proof.rejected` | activity-service | notification-service | `/topic/user/{studentId}` |
| `iact.activity.point.awarded` | activity-service | notification-service | `/topic/user/{studentId}` |
| `iact.activity.point.recalculated` | activity-service | notification-service | `/topic/user/{studentId}` neu co producer goi |
| `iact.activity.point.revoked` | activity-service | notification-service | `/topic/user/{studentId}` |

### 7.2 User preference topics

| Topic | Producer | Consumer | Tac dong |
| --- | --- | --- | --- |
| `iact.user.preference.created` | user-service | notification-service | Tao/update cau hinh nhan thong bao |
| `iact.user.preference.updated` | user-service | notification-service | Dong bo cau hinh nhan thong bao |
| `iact.user.preference.deleted` | user-service du phong | notification-service | Xoa preference notification cua user |

### 7.3 Notification command topics

| Topic | Producer | Consumer | Tac dong |
| --- | --- | --- | --- |
| `iact.notification.notification.created` | Any service | notification-service | Tao notification theo command payload |
| `iact.notification.notification.cleanup-requested` | Any service/admin tooling | notification-service | Cleanup notification theo `activityId` |
| `iact.notification.notification.broadcast-requested` | Any service/admin tooling | notification-service | Tao broadcast/public hoac theo list user |
| `iact.notification.notification.urgent-requested` | Any service/admin tooling | notification-service | Tao urgent notification |

### 7.4 Notification lifecycle/audit topics

| Topic | Producer | Consumer | FE co can xu ly? |
| --- | --- | --- | --- |
| `iact.notification.notification.dispatched` | notification-service | notification-service audit consumer | Khong |
| `iact.notification.notification.read` | notification-service | notification-service audit consumer | Khong |
| `iact.notification.notification.deleted` | notification-service | notification-service audit consumer | Khong |
| `iact.notification.delivery.failed` | notification-service | notification-service audit consumer | Khong, tru khi lam admin monitoring |

### 7.5 Legacy/du phong

| Topic | Trang thai |
| --- | --- |
| `iact.notification.created` | Legacy command, giu compatibility |
| `iact.activity.deleted` | Legacy cleanup, giu compatibility |
| `iact.identity.user.created` | Legacy identity inbound, giu compatibility |
| `iact.identity.user.snapshot` | Legacy user projection, van dang publish song song |
| `iact.user.user.deleted` | Du phong, khong dung vi khong xoa user vinh vien |
| `iact.user.user.role-changed` | Du phong, khong dung vi khong co doi role |
| `iact.system.dead-letter` | Du phong van hanh, chua co DLQ handler that |

## 8. Man hinh FE de xay rieng cho notification

### 8.1 Notification bell

Du lieu:

- On init: `GET /api/v1/notifications/count-unread`.
- Subscribe `/topic/user/{userId}` va `/topic/notifications`.
- Khi nhan message moi:
  - prepend vao dropdown/list neu dang mo.
  - tang unread count neu `isRead=false`.

### 8.2 Notification center page

Tinh nang:

- Tabs: Tat ca, Chua doc, Da doc.
- Search local theo title/message neu can.
- Pagination server-side bang `page`, `size`.
- Action:
  - Mark read: `PUT /api/v1/notifications/{id}/read`.
  - Read all: `PUT /api/v1/notifications/read-all`.
  - Delete: `DELETE /api/v1/notifications/{id}`.
- Click notification:
  - Neu co `activityId`, dieu huong sang activity detail.
  - Sau click co the mark read.

### 8.3 Urgent notification form

Role: ADMIN, DEPARTMENT.

Form fields goi y:

- `title`
- `message`
- `priority`: 1/2/3
- `targetType`: ALL_DEPARTMENT, ACTIVITY, CLASS
- `targetId`: departmentId, activityId hoac classId neu FE muon luu ngu canh
- `activityId` neu notification can dieu huong ve activity
- `userIds` neu gui theo danh sach user cu the

Submit: `POST /api/v1/notifications/urgent`.

### 8.4 Student notification settings

Role: STUDENT.

Data source:

- Load: `GET /api/v1/student-preferences`.
- Save: `PUT /api/v1/student-preferences`.
- Reset: `POST /api/v1/student-preferences/reset`.

Controls:

- Toggle `newActivityAlert`.
- Toggle `reminderAlert`.
- Input/stepper `reminderDaysBefore`.

## 9. Luu y quan trong cho FE

- Khong dung tu "xoa vinh vien user" tren UI. Dung "Vo hieu hoa", "Khoa tai khoan", "Ban".
- Khong xay UI doi role user neu BE chua co nghiep vu nay.
- REST va WebSocket nen di cung nhau:
  - REST load lich su va count.
  - WebSocket nhan realtime.
- Source of truth cho notification list van la REST `/api/v1/notifications`; WebSocket chi de cap nhat realtime.
- Neu FE bi mat ket noi WebSocket, khi reconnect nen refetch unread count va trang dau danh sach notification.
