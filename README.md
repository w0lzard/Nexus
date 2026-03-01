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
- Visibility controls
- Hashtag extraction & indexing

</td>
<td>

**🔔 Notifications**
- In-app notification feed
- Event-driven via Spring Events
- Real-time push over WebSocket (STOMP)
- Unread count badge support

</td>
</tr>
<tr>
<td>

**💬 Engagement**
- Likes with toggle
- Nested comments & replies
- Like counts cached in Redis
- `isLiked` flag per post per user

</td>
<td>

**🛡️ Safety & Scale**
- Rate limiting with Bucket4j
- Content reporting system
- Block & mute users
- Structured logging throughout

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
| ⚡ Cache | Redis 7 | Feed caching, session data |
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
├── 📄 NexusApplication.kt              # Application entry point
│
├── 📁 controllers/                     # REST layer — HTTP in, HTTP out
│   ├── AuthController.kt               # POST /api/auth/register, /login, /refresh
│   └── UserController.kt              # GET /me, GET /{username}, PUT /me
│
├── 📁 database/
│   └── repository/
│       └── UserRepository.kt          # JPA repository for User entity
│
├── 📁 dto/                             # Data Transfer Objects
│   └── AuthDtos.kt                    # RegisterRequest, LoginRequest, AuthResponse …
│
├── 📁 exception/                       # Error handling
│   └── GlobalExceptionHandler.kt      # Unified error responses
│
├── 📁 model/                           # JPA entities
│   └── User.kt                        # User entity + Role enum
│
├── 📁 security/                        # Spring Security layer
│   ├── CustomUserDetailsService.kt    # Loads user from DB for Spring Security
│   ├── JwtAuthenticationFilter.kt     # Validates Bearer token on every request
│   └── SecurityConfig.kt             # Filter chain, BCrypt, CORS, session policy
│
├── 📁 service/                         # Business logic
│   └── AuthService.kt                 # Register, login, refresh token
│
└── 📁 util/
    └── JwtUtil.kt                     # JWT generate / validate / parse
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

### 3️⃣ Run the Application

```bash
# Linux / macOS
./gradlew bootRun

# Windows
.\gradlew.bat bootRun
```

The server starts on **http://localhost:8080** 🎉

### 4️⃣ Verify

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
    "createdAt": "2026-02-25T00:00:00Z"
  }
}
```

**Error Response**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Username 'johndoe' is already taken",
  "timestamp": "2026-02-25T00:00:00Z"
}
```

### 📖 Interactive Docs

Full Swagger UI available at:

```
http://localhost:8080/swagger-ui/index.html
```

---

## ⚙️ Configuration

Key properties in `src/main/resources/application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/socialapp` | Database URL |
| `app.jwt.secret` | *(set in yml)* | JWT signing secret — **min 256 bits** |
| `app.jwt.expiration-ms` | `900000` | Access token TTL (15 minutes) |
| `app.jwt.refresh-expiration-ms` | `604800000` | Refresh token TTL (7 days) |

> ⚠️ **Never commit real secrets.** Use environment variables or a secrets manager in production.

---

## 🧪 Testing

```bash
./gradlew test
```

Test reports are generated at `build/reports/tests/test/index.html`.

---

## 📦 Building & Deployment

```bash
# Build the executable JAR
./gradlew bootJar

# Run the JAR directly
java -jar build/libs/Nexus-0.0.1-SNAPSHOT.jar
```

---

## 🗺️ Roadmap

- [x] Authentication (register / login / refresh)
- [x] JWT security filter chain
- [x] Global exception handler
- [ ] User profiles & avatar upload
- [ ] Posts (text + media)
- [ ] Follow system (public / private accounts)
- [ ] Home feed with Redis caching
- [ ] Likes & comments
- [ ] Reposts & quote posts
- [ ] In-app notifications (DB)
- [ ] Real-time push via WebSocket
- [ ] User search & hashtags
- [ ] Explore / trending feed
- [ ] Password reset via email
- [ ] Email verification
- [ ] Block & mute
- [ ] Rate limiting
- [ ] Content reporting
- [ ] Integration test suite

---

<div align="center">

Made with ❤️ using **Kotlin** + **Spring Boot**

</div>
