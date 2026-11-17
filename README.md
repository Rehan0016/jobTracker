<<<<<<< HEAD
# JobTracker — Spring Boot Application Tracker API

A production-grade, secure RESTful API built with **Spring Boot** and **Spring Security** to manage and track job applications with strict user ownership, JWT authentication, advanced search/filtering, pagination, and dashboard metrics.

---

## Features

- **User Authentication**: Secure user registration and login with BCrypt password hashing and signed JWT (JSON Web Tokens).
- **Strict User Ownership**: Multi-tenant data isolation enforced at the database query level. Users can never view, mutate, or delete another user's records (returns HTTP 404 to avoid disclosing existence).
- **Job Application Management**: Comprehensive CRUD operations for tracking job applications.
- **Search & Filtering**: Multi-field search across company name and job title, and filtering by `ApplicationStatus` (`APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`, `SELECTED`).
- **Pagination & Sorting**: Efficient pagination and sorting backed by Spring Data JPA `Pageable` and SQL `LIMIT`/`OFFSET` queries.
- **Dashboard Analytics**: Real-time aggregated statistics categorized by application status, strictly isolated per user.
- **Robust Exception Handling**: Centralized `@RestControllerAdvice` delivering consistent, structured JSON error responses with field-level validation feedback.
- **Profile-Based Configuration**: Separate configurations for local development (`dev`) and production (`prod`).

---

## Tech Stack

- **Java**: 17
- **Framework**: Spring Boot 4
- **Security**: Spring Security 7, JJWT (io.jsonwebtoken 0.12.6), BCrypt
- **Persistence**: Spring Data JPA, Hibernate 6, HikariCP
- **Databases**: MySQL (runtime), H2 (in-memory testing)
- **Build Tool**: Maven (`mvnw`)

---

## Environment Configuration

The application uses profile-based configuration and reads sensitive credentials from environment variables. An example template is provided in [`.env.example`](.env.example).

### Key Environment Variables

| Variable | Description | Development Default | Production Default |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | Must be set to `prod` |
| `SERVER_PORT` / `PORT` | HTTP port | `8080` | `8080` |
| `DB_HOST` | MySQL hostname | `localhost` | *(Required)* |
| `DB_PORT` | MySQL container/local port | `3306` | `3306` |
| `DB_HOST_PORT` | Docker host-mapped MySQL port | `3307` | `3307` |
| `DB_NAME` | MySQL database name | `jobtracker` | *(Required)* |
| `DB_USERNAME` | MySQL database username | `root` | *(Required)* |
| `DB_PASSWORD` | MySQL database password | *(None)* | *(Required)* |
| `JWT_SECRET` | 256-bit Hex JWT Secret Key | Provided via `.env` | *(Required, minimum 64 hex characters)* |
| `JWT_EXPIRATION` | JWT token lifetime (ms) | `86400000` (24h) | `86400000` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | `http://localhost:3000,http://localhost:5173` | *(Configured per deployment)* |

---

## Running the Application

### 1. Development Profile (`dev`)

The `dev` profile enables automatic schema updates (`ddl-auto=update`), debug SQL logs, and local development endpoints.

1. Ensure MySQL is running on your machine with database `jobtracker`:
   ```sql
   CREATE DATABASE IF NOT EXISTS jobtracker;
   ```
2. Set your local environment variables (PowerShell example):
   ```powershell
   $env:SPRING_PROFILES_ACTIVE="dev"
   $env:DB_USERNAME="root"
   $env:DB_PASSWORD="your_password"
   $env:JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
   ```
3. Run with Maven:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   Or explicitly pass the profile:
   ```powershell
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### 2. Production Profile (`prod`)

The `prod` profile enforces strict production safeguards:
- **`ddl-auto=validate`**: Hibernate will never alter or drop production tables.
- **HikariCP Connection Pool**: Configured pool limits (`DB_POOL_MAX`, `DB_POOL_MIN`, timeouts).
- **Secure Logging**: SQL and verbose security logging are suppressed; logs are formatted for production ingestion.
- **SSL**: Enforces `useSSL=true` on MySQL connections.
- **No Hardcoded Secrets**: Fails fast if `DB_PASSWORD`, `DB_HOST`, or `JWT_SECRET` are missing.

To package and run in production:

1. Build the production executable JAR:
   ```powershell
   .\mvnw.cmd clean package -DskipTests
   ```
2. Execute the JAR with `prod` profile:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export DB_HOST=your-mysql-host
   export DB_PORT=3306
   export DB_NAME=jobtracker_prod
   export DB_USERNAME=app_user
   export DB_PASSWORD=your_secure_password
   export JWT_SECRET=your_production_64_hex_character_key
   export CORS_ALLOWED_ORIGINS=https://jobtracker.yourdomain.com

   java -jar target/jobtracker-0.0.1-SNAPSHOT.jar
   ```

---

## Docker Containerization

The backend includes a production-ready, multi-stage [`Dockerfile`](Dockerfile) designed for minimal image size and maximum container security:

- **Stage 1 (Builder)**: `maven:3.9-eclipse-temurin-17-alpine` builds and packages the application JAR, executing the full test suite during image build.
- **Stage 2 (Runtime)**: `eclipse-temurin:17-jre-alpine` provides a minimal runtime image (~180MB).
- **Security**: The application runs under an unprivileged system user (`appuser:appgroup`), avoiding container root execution.
- **Zero Hardcoded Secrets**: Secrets and database credentials must be provided as environment variables at container launch.
- **Built-in Health Check**: Configured with `wget` checking `/api/health` every 30 seconds.

### 1. Build the Docker Image

Run the following command from the project root:

```bash
docker build -t jobtracker-backend:latest .
```

*Tip: To build without re-running tests inside Docker (e.g. if tests already ran in CI/CD):*
```bash
docker build --build-arg SKIP_TESTS=true -t jobtracker-backend:latest .
```

### 2. Run the Container

Launch the container passing runtime configuration via environment variables:

```bash
docker run -d \
  --name jobtracker-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=jobtracker \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_secure_password \
  -e JWT_SECRET=your_64_character_hex_jwt_secret_key_here \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173 \
  jobtracker-backend:latest
```

Alternatively, supply the environment file:

```bash
docker run -d \
  --name jobtracker-api \
  -p 8080:8080 \
  --env-file .env \
  jobtracker-backend:latest
```

### 3. Check Container Health & Logs

```bash
# View live application logs
docker logs -f jobtracker-api

# Inspect container health status
docker inspect --format='{{json .State.Health.Status}}' jobtracker-api
```

---

## Running the Test Suite

The test suite runs entirely isolated using an in-memory H2 database, requiring no running MySQL instance.

```powershell
.\mvnw.cmd test
```

All 91 unit and integration tests (including security filter chains, authentication flows, and multi-user ownership isolation) are executed automatically.

---

## API Endpoints Overview

### Authentication (`/api/auth`)
| Method | Endpoint | Description | Public |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register a new user | Yes |
| `POST` | `/api/auth/login` | Login and obtain JWT token | Yes |

### Job Applications (`/api/applications`) — *Requires Bearer Token*
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/applications` | Create job application (auto-bound to user) |
| `GET` | `/api/applications` | List current user's applications |
| `GET` | `/api/applications/{id}` | Get application by ID (404 if not owner) |
| `PUT` | `/api/applications/{id}` | Update application (404 if not owner) |
| `DELETE` | `/api/applications/{id}` | Delete application (404 if not owner) |
| `GET` | `/api/applications/search?query=...` | Search user's applications by company or title |
| `GET` | `/api/applications/paged?page=0&size=10` | Paginated and sorted list of user's applications |
| `GET` | `/api/applications/stats` | Dashboard metrics for current user |

### Health Check (`/api/health`)
| Method | Endpoint | Description | Public |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Service health status | Yes |

---

## Docker Compose Full-Stack Infrastructure

The project includes an enterprise-grade [`docker-compose.yml`](docker-compose.yml) orchestrating the complete full-stack environment:
1. **MySQL 8.0**: Database with persistent named volume (`mysql_data`), custom health check (`mysqladmin ping`), and UTF-8 charset.
2. **Spring Boot Backend**: Production profile, connecting via the internal bridge network (`jobtracker-network`), waiting for MySQL to be healthy before starting.
3. **React + Nginx Frontend**: Production multi-stage build, serving static assets with SPA routing and proxying `/api/` traffic directly to the backend.

### 1. Quick Start with Docker Compose

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```
2. Adjust credentials if needed in `.env` (do not commit `.env` to version control).
3. Build and launch all services in detached mode:
   ```bash
   docker compose up --build -d
   ```
4. Verify all services are healthy and running:
   ```bash
   docker compose ps
   ```
5. Access the application:
   - **Frontend Application**: [http://localhost:3000](http://localhost:3000)
   - **Backend REST API**: [http://localhost:8081](http://localhost:8081)
   - **Backend Health Check**: [http://localhost:8081/api/health](http://localhost:8081/api/health)

### 2. Viewing Logs & Managing Containers

```bash
# View aggregated live logs
docker compose logs -f

# View backend logs specifically
docker compose logs -f backend

# Stop all containers (preserving database volume)
docker compose down

# Stop and wipe database volume (clean reset)
docker compose down -v
```

---

## Frontend Application (React + Vite + TypeScript)

The frontend is a modern, responsive Single Page Application (SPA) located in [`frontend/`](frontend).

### Features & Design
- **Tech Stack**: React, TypeScript, Vite, Tailwind CSS, Lucide icons, Axios, React Router.
- **Modern SaaS Aesthetics**: Soft gradients, slate surfaces, crisp KPI cards, status badges, and refined typography.
- **Authentication**: JWT token management, automatic Bearer request injection, session handling, 401 response interceptor, protected routes (`/dashboard`, `/applications`), and public routes (`/login`, `/register`).
- **Interactive Dashboard**: Real-time KPI stat cards (`Total Applications`, `Applied`, `Interviews`, `Offers`, `Selected`, `Rejected`), visual pipeline distribution progress bar, and recent applications quick table.
- **Full CRUD Applications Table**: Search by company and job title, status filter dropdown, column sorting (`companyName`, `jobTitle`, `applicationDate`), server-side pagination, and modal dialogs for create, edit, and deletion confirmation.
- **Multi-Tenant Data Isolation**: Strictly scopes and renders data belonging exclusively to the authenticated user.

### Running Frontend Locally

```bash
cd frontend

# Install dependencies
npm install

# Start development server (with Vite API proxy to backend on port 8080)
npm run dev

# Run Vitest test suite
npm test -- --run

# Build production bundle
npm run build
```

---

## Testing Verification

### Backend Tests
Execute the full Spring Boot test suite (91 automated tests):
```powershell
.\mvnw.cmd test
```
- Total tests: **91**
- Failures: **0**, Errors: **0**

### Frontend Tests
Execute the complete frontend test suite (11 automated tests covering auth flows, CRUD modals, dashboard metrics, and user isolation):
```powershell
cd frontend
npm test -- --run
```
- Test files: **4 passed**
- Total tests: **11 passed**

=======
# jobTracker
>>>>>>> origin/main
