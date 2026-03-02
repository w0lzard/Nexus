<div align="center">

# ⚡ Nexus

**A high-performance social media backend API**

*Built with Kotlin · Spring Boot 3 · PostgreSQL · Redis · JWT*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![JWT](https://img.shields.io/badge/JWT-JJWT_0.12-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://github.com/jwtk/jjwt)
[![License](https://img.shields.io/badge/License-Private-red?style=for-the-badge)](.)

---

*Nexus is a feature-rich, production-ready REST API powering a full social media platform — from authentication and user profiles to posts, follows, likes, comments, real-time notifications, and beyond.*

</div>

---

## 📋 Table of Contents

- [✨ Features](#-features)
- [🛠️ Tech Stack](#️-tech-stack)
- [🏗️ Project Structure](#️-project-structure)
- [🚀 Getting Started](#-getting-started)
- [📡 API Reference](#-api-reference)
- [⚙️ Configuration](#️-configuration)
- [🧪 Testing](#-testing)
- [📦 Building & Deployment](#-building--deployment)
- [🗺️ Roadmap](#️-roadmap)

---

## ✨ Features

<table>
<tr>
<td>

**🔐 Authentication**
- JWT access + refresh tokens
- BCrypt password hashing
- Stateless Spring Security
- Token rotation on refresh

</td>
<td>

**👤 User Profiles**
- Public & private accounts
- Avatar uploads via Cloudinary
- Bio, display name, role system
- Follow / unfollow with pending state

</td>
</tr>
<tr>
<td>

**📝 Posts & Media**
- Text posts and image attachments
- Reposts and quote posts
- Visibility controls (PUBLIC / PRIVATE)
- Hashtag extraction & indexing

</td>
<td>

**🔔 Notifications**
- In-app notification feed
- Event-driven via Spring Events (`@Async`)
- Real-time push over WebSocket (STOMP)
- Unread count badge support

</td>
</tr>
<tr>
<td>

**💬 Engagement**
- Likes with toggle
- Nested comments & replies
- `isLiked` flag per post per user
- Comment & like counts on posts

</td>
<td>

**🛡️ Safety & Scale**
- Rate limiting with Bucket4j (per IP & per user)
- Content reporting system (admin actioning)
- Block & mute users
- Password reset via email token

</td>
</tr>
<tr>
<td>

**🔍 Discovery**
- User search by username / display name
- Hashtag pages
- Explore / trending feed
- Redis-cached trending scores (refreshed every 15 min)

</td>
<td>

**📧 Email**
- Verification email on registration
- Password reset flow with expiring tokens
- Configurable SMTP (Mailtrap / Gmail)

</td>
</tr>
</table>

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| 🟣 Language | Kotlin 2.1 | Primary language |
| 🍃 Framework | Spring Boot 3.4.3 | Application framework |
| 🐘 Database | PostgreSQL 16 | Primary data store |
| 🔄 Migrations | Flyway | Schema versioning |
| 🔑 Auth | Spring Security + JJWT 0.12 | JWT-based authentication |
| ⚡ Cache | Redis 7 | Feed & trending caching |
| 🔍 Search | Elasticsearch 8.11 | Full-text user/post search |
| ☁️ Storage | Cloudinary | Image uploads |
| 🔌 Real-time | WebSockets (STOMP) | Push notifications |
| 📧 Email | Spring Mail | Verification & password reset |
| 📖 API Docs | SpringDoc OpenAPI | Swagger UI |
| 🪣 Rate Limiting | Bucket4j | Abuse prevention |
| 📊 Monitoring | Spring Actuator | Health & metrics |
| 🏗️ Build | Gradle (Kotlin DSL) | Build tooling |
| 🐳 Infrastructure | Docker Compose | Local dev environment |

---

## 🏗️ Project Structure

```
src/main/kotlin/com/ryuken/Nexus/
│
├── 📄 NexusApplication.kt
│
├── 📁 config/
│   └── CacheConfig.kt                      # Redis cache configuration
│
├── 📁 controllers/                          # REST layer — HTTP in, HTTP out
│   ├── AuthController.kt                   # /api/auth/**
│   ├── UserController.kt                   # /api/users/**
│   ├── PostController.kt                   # /api/posts/**
│   ├── FollowController.kt                 # /api/users/{id}/follow/**
│   ├── NotificationController.kt           # /api/notifications/**
│   ├── PasswordController.kt               # /api/users/me/password, /api/auth/forgot-password
│   ├── BlockMuteController.kt              # /api/users/{id}/block, /mute
│   └── ReportController.kt                 # /api/reports/**
│
├── 📁 database/
│   └── repository/
│       ├── UserRepository.kt
│       ├── PostRepository.kt
│       ├── FollowRepository.kt
│       ├── LikeRepository.kt
│       ├── CommentRepository.kt
│       ├── NotificationRepository.kt
│       ├── HashtagRepository.kt
│       ├── BlockRepository.kt
│       ├── MuteRepository.kt
│       ├── PasswordResetTokenRepository.kt
│       └── ReportRepository.kt
│
├── 📁 dto/                                  # Data Transfer Objects
│   ├── AuthDtos.kt                         # RegisterRequest, LoginRequest, AuthResponse
│   ├── UserDtos.kt                         # UserResponse, UpdateProfileRequest
│   ├── PostDtos.kt                         # CreatePostRequest, PostResponse
│   ├── CommentDtos.kt                      # CommentRequest, CommentResponse
│   ├── NotificationDtos.kt                 # NotificationResponse
│   └── MiscDtos.kt                         # ReportRequest, shared DTOs
│
├── 📁 event/
│   └── NotificationEvent.kt                # Spring application event for async notifications
│
├── 📁 exception/
│   └── GlobalExceptionHandler.kt           # Unified error responses (@RestControllerAdvice)
│
├── 📁 model/                                # JPA entities
│   ├── User.kt
│   ├── Post.kt
│   ├── Follow.kt
│   ├── Like.kt
│   ├── Comment.kt
│   ├── Notification.kt
│   ├── Hashtag.kt
│   ├── Block.kt
│   ├── Mute.kt
│   ├── PasswordResetToken.kt
│   └── Report.kt
│
├── 📁 ratelimit/
│   └── RateLimitingFilter.kt               # Bucket4j per-IP and per-user rate limiting
│
├── 📁 scheduler/
│   └── TrendingRefreshScheduler.kt         # @Scheduled trending cache refresh (every 15 min)
│
├── 📁 security/
│   ├── CustomUserDetailsService.kt         # Loads user from DB for Spring Security
│   ├── JwtAuthenticationFilter.kt          # Validates Bearer token on every request
│   └── SecurityConfig.kt                  # Filter chain, BCrypt, session policy
│
├── 📁 service/
│   ├── AuthService.kt                      # Register, login, refresh token
│   ├── UserService.kt                      # Profile CRUD, avatar upload, search
│   ├── PostService.kt                      # Create, delete, feed, trending, hashtags
│   ├── FollowService.kt                    # Follow, unfollow, accept/reject requests
│   ├── LikeService.kt                      # Toggle like, publish NotificationEvent
│   ├── CommentService.kt                   # Add/delete comments, publish events
│   ├── NotificationService.kt              # Save, list, mark read, WS push
│   ├── EmailService.kt                     # Send verification & reset emails
│   ├── PasswordService.kt                  # Change password, forgot/reset flow
│   ├── CloudinaryService.kt                # Image upload/delete via Cloudinary SDK
│   ├── FileStorageService.kt               # FileStorageService interface
│   ├── BlockService.kt                     # Block / unblock users
│   ├── MuteService.kt                      # Mute / unmute users
│   └── ReportService.kt                    # Submit and action content reports
│
├── 📁 util/
│   ├── JwtUtil.kt                          # JWT generate / validate / parse
│   └── UserExtensions.kt                   # User entity → UserResponse mapper
│
└── 📁 websocket/
    ├── WebSocketConfig.kt                  # STOMP endpoint config (/ws)
    └── NotificationWebSocketService.kt     # Push to /user/queue/notifications

src/main/resources/
├── application.properties                  # Main configuration
└── db/migration/
    ├── V1__create_users_table.sql
    └── V2__create_social_tables.sql
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Docker & Docker Compose**

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/your-username/nexus.git
cd nexus
```

### 2️⃣ Start Infrastructure

Spin up PostgreSQL, Redis, and Elasticsearch with a single command:

```bash
docker compose up -d
```

| Service | Port | Credentials |
|---------|------|-------------|
| PostgreSQL | `5432` | db: `socialapp` · user: `postgres` · pass: `password` |
| Redis | `6379` | — |
| Elasticsearch | `9200` | — |

### 3️⃣ Configure

Edit `src/main/resources/application.properties`. At minimum, set your real credentials for:

```properties
# Cloudinary
app.cloudinary.cloud-name=your-cloud-name
app.cloudinary.api-key=your-api-key
app.cloudinary.api-secret=your-api-secret

# SMTP (Mailtrap or Gmail)
spring.mail.username=your-smtp-username
spring.mail.password=your-smtp-password
```

> ⚠️ **Never commit real secrets.** Use environment variables or a secrets manager in production.

### 4️⃣ Run the Application

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

The server starts on **http://localhost:8080** 🎉

### 5️⃣ Verify

```bash
curl http://localhost:8080/actuator/health
```

```json
{ "status": "UP" }
```

---

## 📡 API Reference

### 🔐 Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/auth/register` | Create a new account | ❌ |
| `POST` | `/api/auth/login` | Login, receive tokens | ❌ |
| `POST` | `/api/auth/refresh` | Swap refresh → new access token | ❌ |
| `POST` | `/api/auth/forgot-password` | Send password reset email | ❌ |
| `POST` | `/api/auth/reset-password` | Reset password with token | ❌ |

<details>
<summary><b>POST</b> <code>/api/auth/register</code></summary>

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "securepass123",
    "displayName": "John Doe"
  }'
```
</details>

<details>
<summary><b>POST</b> <code>/api/auth/login</code></summary>

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "johndoe",
    "password": "securepass123"
  }'
```
</details>

<details>
<summary><b>POST</b> <code>/api/auth/refresh</code></summary>

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<your-refresh-token>"
  }'
```
</details>

---

### 👤 Users

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/users/me` | Get current user profile | ✅ |
| `PUT` | `/api/users/me` | Update profile | ✅ |
| `POST` | `/api/users/me/avatar` | Upload avatar image | ✅ |
| `PUT` | `/api/users/me/password` | Change password | ✅ |
| `GET` | `/api/users/{username}` | Get public profile by username | ✅ |
| `GET` | `/api/users/search?q=` | Search users by name | ❌ |

---

### 📝 Posts

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/posts` | Create a post | ✅ |
| `GET` | `/api/posts/{id}` | Get post by ID | ✅ |
| `DELETE` | `/api/posts/{id}` | Delete own post | ✅ |
| `GET` | `/api/posts/feed` | Home feed (followed users) | ✅ |
| `GET` | `/api/posts/public` | Public posts paginated | ❌ |
| `GET` | `/api/posts/explore` | Trending / explore feed | ❌ |
| `GET` | `/api/posts/user/{username}` | Posts by a specific user | ✅ |
| `GET` | `/api/posts/hashtag/{tag}` | Posts by hashtag | ❌ |
| `POST` | `/api/posts/{id}/like` | Toggle like on a post | ✅ |
| `POST` | `/api/posts/{id}/comments` | Add a comment | ✅ |
| `GET` | `/api/posts/{id}/comments` | List comments on a post | ✅ |
| `DELETE` | `/api/posts/{id}/comments/{commentId}` | Delete own comment | ✅ |

---

### 🤝 Follows

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/users/{id}/follow` | Follow a user | ✅ |
| `DELETE` | `/api/users/{id}/follow` | Unfollow a user | ✅ |
| `GET` | `/api/users/{id}/follow/followers` | List followers | ✅ |
| `GET` | `/api/users/{id}/follow/following` | List following | ✅ |

---

### 🔔 Notifications

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/notifications` | Get notifications (paginated) | ✅ |
| `GET` | `/api/notifications/unread-count` | Get unread count | ✅ |
| `POST` | `/api/notifications/mark-all-read` | Mark all as read | ✅ |

**WebSocket (STOMP)**
- Connect to: `ws://localhost:8080/ws`
- Subscribe to: `/user/queue/notifications`
- Receives real-time notification payloads on like, comment, and follow events

---

### 🛡️ Block, Mute & Reports

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/users/{id}/block` | Block a user | ✅ |
| `DELETE` | `/api/users/{id}/block` | Unblock a user | ✅ |
| `POST` | `/api/users/{id}/mute` | Mute a user | ✅ |
| `DELETE` | `/api/users/{id}/mute` | Unmute a user | ✅ |
| `POST` | `/api/reports` | Submit a content report | ✅ |
| `GET` | `/api/reports` | List reports (ADMIN only) | ✅ |

---

### 📦 Response Shapes

**Success — Auth Response**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "johndoe",
    "email": "john@example.com",
    "displayName": "John Doe",
    "avatarUrl": null,
    "createdAt": "2026-03-01T00:00:00Z"
  }
}
```

**Error Response**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Username 'johndoe' is already taken",
  "timestamp": "2026-03-01T00:00:00Z"
}
```

### 📖 Interactive Docs

Full Swagger UI available at:

```
http://localhost:8080/swagger-ui/index.html
```

---

## ⚙️ Configuration

All configuration lives in `src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/socialapp` | Database URL |
| `spring.datasource.username` | `postgres` | DB username |
| `spring.datasource.password` | `password` | DB password |
| `app.jwt.secret` | *(set in properties)* | JWT signing secret — **min 256 bits** |
| `app.jwt.expiration-ms` | `900000` | Access token TTL (15 min) |
| `app.jwt.refresh-expiration-ms` | `604800000` | Refresh token TTL (7 days) |
| `app.cloudinary.cloud-name` | *(set yours)* | Cloudinary cloud name |
| `app.cloudinary.api-key` | *(set yours)* | Cloudinary API key |
| `app.cloudinary.api-secret` | *(set yours)* | Cloudinary API secret |
| `spring.mail.host` | `smtp.mailtrap.io` | SMTP host |
| `spring.mail.username` | *(set yours)* | SMTP username |
| `spring.mail.password` | *(set yours)* | SMTP password |
| `management.health.mail.enabled` | `false` | Mail health check (disabled until SMTP is configured) |

> ⚠️ **Never commit real secrets.** Use environment variables or a secrets manager in production.

---

## 🧪 Testing

```bash
# Windows
.\gradlew.bat test

# Linux / macOS
./gradlew test
```

Test reports are generated at `build/reports/tests/test/index.html`.

The test profile uses an **H2 in-memory database** with Flyway disabled, configured in `src/test/resources/application-test.properties`.

---

## 📦 Building & Deployment

```bash
# Build the executable JAR
.\gradlew.bat bootJar

# Run the JAR directly
java -jar build/libs/Nexus-0.0.1-SNAPSHOT.jar
```

To run with overridden properties in production:

```bash
java -jar Nexus-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:postgresql://prod-host:5432/socialapp \
  --app.jwt.secret=YourProductionSecret
```

---

## 🗺️ Roadmap

- [x] Authentication (register / login / refresh)
- [x] JWT security filter chain
- [x] Global exception handler
- [x] User profiles (get, update, avatar upload)
- [x] Cloudinary file storage
- [x] Posts (text + media, visibility, reposts, quote posts)
- [x] Follow system (public / private accounts with PENDING state)
- [x] Home feed with Redis caching
- [x] Likes (toggle, `isLiked` on PostResponse)
- [x] Nested comments & replies
- [x] Hashtag extraction & hashtag pages
- [x] Explore / trending feed (Redis-cached, refreshed every 15 min)
- [x] In-app notifications (DB-backed)
- [x] Event-driven notification triggers (`@Async` Spring Events)
- [x] Real-time push via WebSocket (STOMP)
- [x] User search
- [x] Password change endpoint
- [x] Password reset via email token
- [x] Email service (verification + reset emails)
- [x] Block & mute users
- [x] Rate limiting (Bucket4j — per IP on auth, per user on posts)
- [x] Content reporting (submit + admin review)
- [ ] Email verification flow (token → mark account verified)
- [ ] Integration test suite
- [ ] Docker production image
- [ ] CI/CD pipeline

---

<div align="center">

Made with ❤️ using **Kotlin** + **Spring Boot**

</div>
