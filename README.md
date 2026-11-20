# JobTracker — Spring Boot Application Tracker API

A production-grade, secure RESTful API built with **Spring Boot** and **Spring Security** to manage and track job applications with strict user ownership, JWT authentication, advanced search/filtering, pagination, and dashboard metrics.

---

## Features

- **User Authentication**: Secure user registration and login with BCrypt password hashing and signed JWT (JSON Web Tokens).
- **Strict User Ownership**: Multi-tenant data isolation enforced at the database query level. Users can never view, mutate, or delete another user's records (returns HTTP 404 to avoid disclosing existence).
- **Job Application Management**: Comprehensive CRUD operations for tracking job applications.
- **Search & Filtering**: Multi-field search across company name and job title, and filtering by `ApplicationStatus` (`APPLIED`, `ASSESSMENT`, `INTERVIEW`, `OFFER`, `REJECTED`, `SELECTED`, `WITHDRAWN`).
- **Pagination & Sorting**: Efficient pagination and sorting backed by Spring Data JPA `Pageable` and SQL queries.
- **Dashboard Analytics**: Real-time aggregated statistics categorized by application status, strictly isolated per user.
- **Robust Exception Handling**: Centralized `@RestControllerAdvice` delivering consistent, structured JSON error responses with field-level validation feedback.
- **Profile-Based Configuration**: Separate configurations for local development (`dev`) and production (`prod`).
- **PostgreSQL / Supabase Integration**: Production-ready connection pooling optimized for Supabase's transaction pooler with SSL.

---

## Tech Stack

- **Java**: 17
- **Framework**: Spring Boot 3.4.2
- **Security**: Spring Security 6, JJWT (io.jsonwebtoken 0.12.6), BCrypt
- **Persistence**: Spring Data JPA, Hibernate 6, HikariCP
- **Databases**: PostgreSQL (Supabase runtime), H2 (in-memory testing)
- **Build Tool**: Maven (`mvnw`)

---

## Environment Configuration

The application uses profile-based configuration and reads sensitive credentials from environment variables. An example template is provided in [`.env.example`](.env.example).

### Key Environment Variables

| Variable | Description | Development Default | Production Default |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | `prod` *(Required on Render)* |
| `SERVER_PORT` / `PORT` | HTTP port | `8080` | Render assigns `$PORT` automatically |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/jobtracker` | `jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL database username | `postgres` | `postgres.hmeufaqxjfjgjjcipsee` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL database password | *(None)* | *(Required, set in Render)* |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema action | `update` | `update` |
| `DB_POOL_MAX` | HikariCP max pool size | *(default)* | `5` (optimized for Supabase pooler) |
| `DB_POOL_MIN` | HikariCP min idle connections | *(default)* | `1` |
| `JWT_SECRET` | 256-bit Hex JWT Secret Key | Development fallback | *(Required, minimum 64 hex characters)* |
| `JWT_EXPIRATION` | JWT token lifetime (ms) | `86400000` (24h) | `86400000` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | `http://localhost:3000,http://localhost:5173` | Configured per frontend deployment URL |

---

## Running the Application

### 1. Development Profile (`dev`)

The `dev` profile enables automatic schema updates (`ddl-auto=update`), debug SQL logs, and local development endpoints.

1. Ensure PostgreSQL is running locally or specify connection variables in `.env`:
   ```powershell
   $env:SPRING_PROFILES_ACTIVE="dev"
   $env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/jobtracker"
   $env:SPRING_DATASOURCE_USERNAME="postgres"
   $env:SPRING_DATASOURCE_PASSWORD="your_postgres_password"
   $env:JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
   ```
2. Run with Maven:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

### 2. Production Profile (`prod`)

The `prod` profile connects to Supabase PostgreSQL with SSL:
- **`ddl-auto=update`**: Automatically creates and updates tables (`users`, `job_applications`), foreign keys, and indexes on fresh Supabase databases.
- **HikariCP Connection Pool**: Connection pool sized to match Supabase pooler limits (`DB_POOL_MAX=5`).
- **Secure Logging**: SQL and verbose security logging suppressed.
- **SSL**: Enforces `sslmode=require` for secure cloud transport.
- **No Hardcoded Secrets**: Reads database credentials and JWT secrets strictly from environment variables.

To package and run in production:

1. Build the production executable JAR:
   ```powershell
   .\mvnw.cmd clean package -DskipTests
   ```
2. Execute the JAR with `prod` profile:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export SPRING_DATASOURCE_URL="jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require"
   export SPRING_DATASOURCE_USERNAME="postgres.hmeufaqxjfjgjjcipsee"
   export SPRING_DATASOURCE_PASSWORD="your_supabase_password"
   export JWT_SECRET="your_production_64_hex_character_key"
   export CORS_ALLOWED_ORIGINS="https://jobtracker.yourdomain.com"

   java -jar target/jobtracker-0.0.1-SNAPSHOT.jar
   ```

---

## Deploying to Render

### Step 1: Create a Web Service on Render
1. Link your GitHub repository in the [Render Dashboard](https://dashboard.render.com).
2. Choose **Web Service**.
3. Select **Docker** as the Runtime (or **Java** with build command `./mvnw clean package -DskipTests` and start command `java -jar target/jobtracker-0.0.1-SNAPSHOT.jar`).

### Step 2: Configure Environment Variables in Render
Under the **Environment** tab on Render, add these variables:

| Key | Value |
| :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `postgres.hmeufaqxjfjgjjcipsee` |
| `SPRING_DATASOURCE_PASSWORD` | `<your-actual-supabase-password>` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |
| `JWT_SECRET` | `<your-secure-64-character-hex-secret>` |
| `JWT_EXPIRATION` | `86400000` |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend-service.onrender.com` |

> Render automatically sets the `PORT` environment variable (e.g., 10000), which `server.port=${PORT:${SERVER_PORT:8080}}` binds to dynamically.

---

## Docker Containerization

The backend includes a production-ready, multi-stage [`Dockerfile`](Dockerfile) designed for minimal image size and maximum container security:

- **Stage 1 (Builder)**: `maven:3.9-eclipse-temurin-17-alpine` builds and packages the application JAR, executing the full test suite during image build.
- **Stage 2 (Runtime)**: `eclipse-temurin:17-jre-alpine` provides a minimal runtime image (~180MB).
- **Security**: The application runs under an unprivileged system user (`appuser:appgroup`), avoiding container root execution.
- **Zero Hardcoded Secrets**: Secrets and database credentials must be provided as environment variables at container launch.
- **Built-in Health Check**: Configured with `wget` checking `/api/health` every 30 seconds.

---

## Database Schema (PostgreSQL / Supabase)

The database schema is defined in [`src/main/resources/schema-postgres.sql`](src/main/resources/schema-postgres.sql).

Hibernate automatically handles table creation and schema migration on startup when `SPRING_JPA_HIBERNATE_DDL_AUTO=update`. Alternatively, you can run the SQL directly in the Supabase SQL Editor:

```sql
-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP(6) NOT NULL
);

-- Job Applications Table
CREATE TABLE IF NOT EXISTS job_applications (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    location VARCHAR(255) DEFAULT NULL,
    job_url VARCHAR(255) DEFAULT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('APPLIED', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED', 'SELECTED', 'WITHDRAWN')),
    application_date DATE DEFAULT NULL,
    notes VARCHAR(2000) DEFAULT NULL,
    job_description VARCHAR(4000) DEFAULT NULL,
    deadline DATE DEFAULT NULL,
    user_id BIGINT DEFAULT NULL,
    CONSTRAINT fk_job_applications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_job_applications_status ON job_applications (status);
CREATE INDEX IF NOT EXISTS idx_job_applications_user_id ON job_applications (user_id);
```

---

## REST API Specification

### Authentication Endpoints (`/api/auth`)
| Method | Endpoint | Description | Public |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register a new user account | Yes |
| `POST` | `/api/auth/login` | Authenticate and obtain JWT token | Yes |

### Job Application Endpoints (`/api/applications`)
All endpoints require `Authorization: Bearer <token>`:
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/applications` | Create a new job application |
| `GET` | `/api/applications` | List all applications for current user |
| `GET` | `/api/applications/{id}` | Get application by ID (404 if not owner) |
| `PUT` | `/api/applications/{id}` | Update application (404 if not owner) |
| `DELETE` | `/api/applications/{id}` | Delete application (404 if not owner) |
| `GET` | `/api/applications/search?query=...` | Search user's applications by company or title |
| `GET` | `/api/applications/paged?page=0&size=10` | Paginated and sorted list of user's applications |
| `GET` | `/api/applications/stats` | Dashboard metrics for current user |

### Health Check Endpoints
| Method | Endpoint | Description | Public |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Service health status | Yes |
| `GET` | `/api/db-health` | Database connection status | Yes |

---

## Testing Verification

### Backend Tests
Execute the full Spring Boot test suite (112 automated tests):
```powershell
.\mvnw.cmd test
```
- Total tests: **112**
- Failures: **0**, Errors: **0**
