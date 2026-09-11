# CounselX Auth Service - Profile & OTP Fixes

## Added
- `PUT /api/auth/me` for authenticated profile updates (first name, last name, mobile).
- `PUT /api/auth/profile-photo` multipart upload, max 5 MB, image content type only.
- `GET /api/auth/profile-photo` authenticated profile photo retrieval.
- `DELETE /api/auth/profile-photo` authenticated profile photo removal.
- `profilePhoto` and `profilePhotoContentType` columns in `users`.
- `profilePhotoAvailable` in `/api/auth/me`.

## OTP / Redis
- Redis cluster-safe hash-tagged keys are used by the OTP Lua script.
- Registration and resend both return HTTP 429 when the OTP limiter blocks a request.
- Cooldown remains 60 seconds and hourly limit remains 5.

## Deployment
The database schema uses `spring.jpa.hibernate.ddl-auto=update`, so the new profile-photo columns will be added automatically when the service starts against the existing `auth_db`.

No existing JWT, refresh-token, registration, verification, or login flow was intentionally removed.
