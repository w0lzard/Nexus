# Nexus

A backend API for a social media application built with **Kotlin**, **Spring Boot 3**, and **PostgreSQL**.

## Tech Stack

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Language         | Kotlin 2.1                          |
| Framework        | Spring Boot 3.4.3                   |
| Database         | PostgreSQL 16                       |
| Migrations       | Flyway                              |
| Auth             | Spring Security + JWT (JJWT 0.12)   |
| Cache            | Redis 7                             |
| Search           | Elasticsearch 8.11                  |
| Storage          | AWS S3 (SDK v2)                     |
| Real-time        | WebSockets                          |
| Email            | Spring Mail                         |
| API Docs         | SpringDoc OpenAPI (Swagger UI)      |
| Rate Limiting    | Bucket4j                            |
| Monitoring       | Spring Actuator                     |
| Build            | Gradle (Kotlin DSL)                 |

## Project Structure

```
src/main/kotlin/com/ryuken/Nexus/
├── NexusApplication.kt          # Application entry point
├── controllers/                  # REST controllers
│   └── AuthController.kt        # POST /api/auth/register, /login, /refresh
├── database/
│   └── repository/
│       └── UserRepository.kt    # JPA repository for User entity
├── dto/
│   └── AuthDtos.kt              # RegisterRequest, LoginRequest, AuthResponse, etc.
├── exception/
│   └── GlobalExceptionHandler.kt # Centralized error handling
├── model/
│   └── User.kt                  # User JPA entity + Role enum
├── security/
│   ├── CustomUserDetailsService.kt  # Loads users for Spring Security
│   ├── JwtAuthenticationFilter.kt   # Validates JWT on every request
│   └── SecurityConfig.kt           # Security filter chain, BCrypt, CORS
├── service/
│   └── AuthService.kt           # Register, login, refresh token logic
└── util/
    └── JwtUtil.kt               # JWT generation, validation, parsing
```

## Prerequisites

- **Java 17+**
- **Docker & Docker Compose** (for infrastructure services)

## Getting Started

### 1. Start Infrastructure Services

```bash
docker compose up -d
```

This starts:
- **PostgreSQL** on port `5432` (database: `socialapp`, user: `postgres`, password: `password`)
- **Redis** on port `6379`
- **Elasticsearch** on port `9200`

### 2. Run the Application

```bash
./gradlew bootRun
```

The server starts on **http://localhost:8080**.

> On Windows, use `.\gradlew.bat bootRun` instead.

### 3. Verify

```bash
curl http://localhost:8080/actuator
```

## API Endpoints

### Auth

| Method | Endpoint              | Description                  | Auth Required |
|--------|-----------------------|------------------------------|---------------|
| POST   | `/api/auth/register`  | Register a new user          | No            |
| POST   | `/api/auth/login`     | Login with username or email | No            |
| POST   | `/api/auth/refresh`   | Refresh access token         | No            |

### Register

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

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "johndoe",
    "password": "securepass123"
  }'
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<your-refresh-token>"
  }'
```

### Response Format

Successful auth responses return:

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

Error responses return:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Username 'johndoe' is already taken",
  "timestamp": "2026-02-25T00:00:00Z"
}
```

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

## Configuration

Key settings in `src/main/resources/application.yml`:

| Property                       | Default     | Description                    |
|--------------------------------|-------------|--------------------------------|
| `server.port`                  | `8080`      | Server port                    |
| `spring.datasource.url`       | `jdbc:postgresql://localhost:5432/socialapp` | Database URL |
| `app.jwt.secret`              | *(set in yml)* | JWT signing secret (min 256 bits) |
| `app.jwt.expiration-ms`       | `900000`    | Access token TTL (15 min)      |
| `app.jwt.refresh-expiration-ms`| `604800000` | Refresh token TTL (7 days)     |

> ⚠️ Change the JWT secret before deploying to production.

## Running Tests

```bash
./gradlew test
```

## Building

```bash
./gradlew bootJar
```

The JAR is output to `build/libs/Nexus-0.0.1-SNAPSHOT.jar`. Run it with:

```bash
java -jar build/libs/Nexus-0.0.1-SNAPSHOT.jar
```

## License

This project is private and unlicensed.

