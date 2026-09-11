# CounselX Auth Service

Production-oriented authentication service for CounselX.

## Stack

- Java 21
- Spring Boot 4.1.1
- Spring Security + JWT
- Spring Data JPA / Hibernate
- MySQL 8.4 (AWS RDS)
- Spring Data Redis / Lettuce
- AWS ElastiCache Serverless Valkey
- Gmail SMTP for email OTP
- Actuator

## Features

- Student registration
- BCrypt password hashing
- Automatic `STUDENT` role assignment
- JWT access token (15 minutes)
- 7-day rotating refresh tokens
- SHA-256 hashed refresh tokens in MySQL
- JWT authentication filter
- `/api/auth/me`
- Logout/revocation of active refresh tokens
- CORS
- Actuator health
- Email OTP verification
- OTP expiry and attempt limit
- Redis-backed OTP cooldown/rate limiting
- AWS RDS MySQL through environment variables
- AWS ElastiCache Serverless Valkey over TLS

## AWS architecture

```text
Frontend
   |
   v
API Gateway / Load Balancer
   |
   v
CounselX Auth Service
   |----------------------|
   v                      v
AWS RDS MySQL       AWS ElastiCache
                   Serverless Valkey
                         |
                    OTP rate limit
                    cooldown counters
```

The application uses Redis only for short-lived rate-limit state. OTP values and refresh tokens are not stored in Redis.

## Environment variables

```text
DB_HOST=127.0.0.1
DB_PORT=3307
DB_NAME=auth_db
DB_USERNAME=admin
DB_PASSWORD=<RDS password>

JWT_SECRET=<at least 32 random characters>

MAIL_USERNAME=<Gmail address>
MAIL_PASSWORD=<Gmail App Password>

REDIS_HOST=counselx-redis-1bfkdu.serverless.aps1.cache.amazonaws.com
REDIS_PORT=6379

# Leave blank when the Valkey cache uses the default no-password user.
# Set these when your AWS user group requires password authentication.
REDIS_USERNAME=
REDIS_PASSWORD=

CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

Do not commit these values to GitHub.

## AWS ElastiCache Serverless Valkey

The current CounselX cache endpoint is configured as the default `REDIS_HOST` above. It can be overridden through the environment variable.

TLS is enabled:

```properties
spring.data.redis.ssl.enabled=true
```

AWS ElastiCache Serverless Valkey uses TLS, and AWS recommends allowing the required cache ports from the application EC2 security group. For the serverless cache, allow TCP `6379` and `6380` from the EC2 security group as needed.

If the cache is configured with RBAC/password authentication, set:

```text
REDIS_USERNAME=<Valkey username>
REDIS_PASSWORD=<Valkey password>
```

For a no-password default user, keep both variables empty.

## Redis OTP rate limiting

`POST /api/auth/register` and `POST /api/auth/send-email-otp` use Redis before sending an OTP.

Default rules:

- 60-second cooldown per email
- Maximum 5 OTP send requests per email per hour
- Redis keys contain a SHA-256 hash of the normalized email, not the email itself
- Rate limiting is performed atomically with a Redis Lua script
- OTP itself remains hashed in MySQL
- Existing OTP verification limit: 5 incorrect attempts
- OTP expiry: 10 minutes

A rate-limit rejection returns HTTP `429 Too Many Requests`.

## Windows development with AWS RDS

Keep the SSH tunnel open:

```powershell
ssh -i "C:\path\to\Devops Key (1).pem" -L 3307:auth-db.cpkucas26poz.ap-south-1.rds.amazonaws.com:3306 ubuntu@<EC2_PUBLIC_IP>
```

Then test RDS from a second Windows PowerShell:

```powershell
mysql -h 127.0.0.1 -P 3307 -u admin -p
```

Do not run that `3307` test from inside EC2; `3307` is the Windows-side SSH tunnel port.

The Auth Service running on Windows uses:

```text
DB_HOST=127.0.0.1
DB_PORT=3307
```

## AWS Redis network requirement

The Auth Service must be able to reach the Valkey endpoint from its runtime environment.

Recommended security group rule:

```text
Protocol: TCP
Port: 6379
Source: Auth Service / EC2 security group
```

For ElastiCache Serverless, also allow `6380` from the same application security group if the client/service requires the read-optimized endpoint.

Do not expose Redis/Valkey to the public internet.

## Endpoints

### Public

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/send-email-otp`
- `POST /api/auth/verify-email`

### JWT protected

- `GET /api/auth/me`
- `POST /api/auth/logout`

### Health

- `GET /actuator/health`

With Redis configured, Actuator can also report the Redis health status.

## Email OTP example

Send/resend:

```http
POST /api/auth/send-email-otp
Content-Type: application/json

{
  "email": "student@gmail.com"
}
```

Verify:

```http
POST /api/auth/verify-email
Content-Type: application/json

{
  "email": "student@gmail.com",
  "otp": "123456"
}
```

## Important security notes

- Never commit `.env`, passwords, JWT secrets, Gmail App Passwords, AWS credentials, or private keys.
- Use AWS Secrets Manager/Parameter Store before production deployment.
- Rotate any credential that has been exposed.
- Keep RDS and ElastiCache private; do not make them publicly accessible.


## Local Windows SSH tunnel for AWS Valkey (TLS)

For local IntelliJ development, do **not** disable Redis TLS certificate verification.

AWS Valkey presents a certificate for its AWS endpoint hostname. Therefore, connecting
to `127.0.0.1:6379` through an SSH tunnel causes Java hostname verification to fail if
the TLS client verifies the tunnel address.

Use the tunnel for transport:

```powershell
ssh -i "C:\path\to\Devops Key (1).pem" -L 6379:counselx-redis-1bfkdu.serverless.aps1.cache.amazonaws.com:6379 ubuntu@65.0.105.244
```

Keep this terminal open.

For the application, use:

```text
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_USERNAME=counselx-auth
REDIS_PASSWORD=<AWS Valkey password>
REDIS_TLS_HOST=counselx-redis-1bfkdu.serverless.aps1.cache.amazonaws.com
```

**Important:** the current Spring Data Redis/Lettuce connection must preserve the AWS
hostname for TLS SNI/hostname verification. If your local setup still reports:

`No subject alternative names matching IP address 127.0.0.1 found`

then the application is verifying the tunnel IP instead of the AWS hostname. In that case,
the safest production-oriented solution is to run the service inside AWS (EC2/EKS) and
connect directly to the Valkey endpoint, rather than disabling certificate verification.



## Local Windows AWS Valkey SSH Tunnel — Correct TLS Setup

The AWS Valkey TLS certificate is issued for the AWS Valkey endpoint hostname, not
`127.0.0.1`. Therefore, do **not** set the Spring Redis host to `127.0.0.1` when TLS
hostname verification is enabled.

### 1. Create the SSH tunnel

Keep this PowerShell window open:

```powershell
ssh -i "C:\Users\vijay\Downloads\Devops Key (1).pem" -L 6379:counselx-redis-1bfkdu.serverless.aps1.cache.amazonaws.com:6379 ubuntu@65.0.105.244
```

### 2. Map the AWS hostname to localhost

Open **Notepad as Administrator** and edit:

```text
C:\Windows\System32\drivers\etc\hosts
```

Add:

```text
127.0.0.1 counselx-redis-1bfkdu.serverless.aps1.cache.amazonaws.com
```

Save the file.

Now Java connects to the AWS hostname, so TLS hostname verification sees the correct
AWS hostname, while Windows sends the TCP connection to `127.0.0.1:6379`, which is the
SSH tunnel.

### 3. IntelliJ environment variables

Use:

```text
REDIS_HOST=counselx-redis-1bfkdu.serverless.aps1.cache.amazonaws.com
REDIS_PORT=6379
REDIS_USERNAME=counselx-auth
REDIS_PASSWORD=<AWS Valkey password>
REDIS_TLS_HOST=counselx-redis-1bfkdu.serverless.aps1.cache.amazonaws.com
```

Do not disable TLS certificate/hostname verification.

### 4. Verify the tunnel

```powershell
Test-NetConnection 127.0.0.1 -Port 6379
```

It should show:

```text
TcpTestSucceeded : True
```

Then start Auth Service and check:

```text
http://localhost:8081/actuator/health
```

Redis should report `UP`.
