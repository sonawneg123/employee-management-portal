# Employee Management Portal

A production-ready, enterprise-grade Employee Management Portal built with **Spring Boot 3** (backend) and **React 19** (frontend), deployed via **Docker**, **Jenkins CI/CD**, and **AWS EC2 + Nginx**.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture](#architecture)
4. [Completed Modules](#completed-modules)
5. [Project Structure](#project-structure)
6. [Frontend Features](#frontend-features)
7. [Backend Features](#backend-features)
8. [Database Overview](#database-overview)
9. [Authentication](#authentication)
10. [API Overview](#api-overview)
11. [Installation](#installation)
12. [Development Setup](#development-setup)
13. [Docker Setup](#docker-setup)
14. [Environment Variables](#environment-variables)
15. [NPM Scripts](#npm-scripts)
16. [Maven Commands](#maven-commands)
17. [Testing](#testing)
18. [Screenshots](#screenshots)
19. [Roadmap](#roadmap)
20. [Changelog](#changelog)
21. [License](#license)
22. [Author](#author)

---

## Project Overview

The **Employee Management Portal** is a full-stack enterprise application that enables organisations to manage:

- **Employees** — complete lifecycle CRUD with role-based access
- **Departments** — organisational structure with employee assignment
- **Leave Requests** — full approval/rejection workflow
- **Attendance** — daily tracking and reporting *(Phase 3G)*
- **Performance Reviews** — review cycles and scoring *(Phase 3H)*

The system enforces **role-based access control (RBAC)** with four roles: `ADMIN`, `HR`, `MANAGER`, `EMPLOYEE`.

---

## Technology Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| Spring Boot | 3.5.3 | Application framework |
| Spring Security | 6.x | Auth + RBAC |
| Spring Data JPA | 3.x | ORM layer |
| Hibernate | 6.x | JPA implementation |
| JWT (JJWT) | 0.12.x | Stateless auth tokens |
| MapStruct | 1.6.3 | DTO/entity mapping |
| Lombok | 1.18.x | Boilerplate reduction |
| Flyway | 10.x | Database migrations |
| MySQL | 8.0 | Relational database |
| Maven | 3.9+ | Build tool |
| Testcontainers | 1.19.x | Integration tests |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 19.x | UI library |
| Vite | 6.x | Build tool + dev server |
| Material UI (MUI) | v7 | Component library |
| React Router | v7 | Client-side routing |
| TanStack React Query | v5 | Server-state management |
| React Hook Form | v7 | Form state management |
| Zod | v3 | Schema validation |
| Axios | v1 | HTTP client |
| Day.js | v1 | Date utilities |
| Recharts | v2 | Data visualisation |
| React Helmet Async | v2 | Document head management |
| Vitest | v2 | Unit/component testing |
| React Testing Library | v16 | Component testing utilities |

### DevOps
| Technology | Purpose |
|---|---|
| Docker + Docker Compose | Local containerised development + production deployment |
| GitHub Actions | CI/CD pipeline (test → build → publish → deploy) |
| Amazon ECR | Private container registry |
| Amazon EC2 | Production application host (Amazon Linux 2023) |
| Amazon RDS MySQL 8 | Production managed database |
| AWS IAM + OIDC | Keyless authentication between GitHub Actions and AWS |
| Nginx | Reverse proxy + static serving |

---

## Architecture

```
  GitHub Actions
       │  OIDC (no static keys)
       ▼
  AWS IAM (GitHubActions-ECRPush-Role)
       │
       ▼
  Amazon ECR
   ├── employee-management-backend:<sha>
   └── employee-management-frontend:<sha>
       │  docker pull (EC2 instance role)
       ▼
  ┌─────────────────────────────────────────────────────────────┐
  │                     AWS EC2 Instance                        │
  │  ┌─────────────────────────────────────────────────────┐   │
  │  │  Docker Compose (docker-compose.prod.yml)            │   │
  │  │                                                      │   │
  │  │  frontend container (Nginx :80)                      │   │
  │  │    ├── /*        → React SPA (static)                │   │
  │  │    └── /api/*    → backend:8080 (proxy)              │   │
  │  │                                                      │   │
  │  │  backend container (Spring Boot :8080)               │   │
  │  │    └── SPRING_PROFILES_ACTIVE=prod                   │   │
  │  └───────────────────────────┬──────────────────────────┘   │
  └──────────────────────────────│──────────────────────────────┘
                                 │ SSL/TLS
                                 ▼
                       Amazon RDS MySQL 8
                       (private subnet, not publicly accessible)
```

**Key design decisions:**
- Backend returns **RFC 7807 ProblemDetail** for all errors
- All PKs are **UUID** (no sequential integer IDs exposed)
- DTOs only — JPA entities are never serialised to the API
- JWT is stateless — no server-side session storage
- Frontend uses **TanStack Query** for all server state (no Redux / Zustand)
- React Query cache is invalidated optimistically on mutations

---

## Completed Modules

| Phase | Module | Status |
|---|---|---|
| Phase 1 | Project Scaffold | ✅ Complete |
| Phase 2 | Backend (Spring Boot) | ✅ Complete |
| Phase 2V | Backend Tests + Postman | ✅ Complete |
| Phase 3A | Frontend Foundation | ✅ Complete |
| Phase 3B | Authentication Module | ✅ Complete |
| Phase 3C | Dashboard Module | ✅ Complete |
| Phase 3D | Employee Management Module | ✅ Complete |
| Phase 3E | Department Management Module | ✅ Complete |
| Phase 3F | Leave Management Module | ✅ Complete |
| Loop 2 | RBAC Hardening | ✅ Complete |
| Loop 3 | JPA Auditing | ✅ Complete |
| Loop 4 | API Documentation & Consistency | ✅ Complete |
| DB Phase | Database & Persistence | ✅ Complete |
| DevOps 1 | Docker & Docker Compose | ✅ Complete |
| DevOps 2 | GitHub Actions CI | ✅ Complete |
| DevOps 3 | AWS ECR + OIDC + EC2 Deployment | ✅ Complete |
| Phase 3G | Attendance Module | 🔜 Planned |
| Phase 3H | Performance Reviews Module | 🔜 Planned |

---

## Project Structure

```
employee-management-portal/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/company/employeemanagement/
│       │   │   ├── EmployeeManagementApplication.java
│       │   │   ├── config/          # Security, Auditing, JWT, OpenAPI
│       │   │   ├── controller/      # REST controllers (Auth, Employee)
│       │   │   ├── dto/
│       │   │   │   ├── request/     # RegisterRequest, LoginRequest, Create/UpdateEmployee
│       │   │   │   └── response/    # AuthResponse, EmployeeResponse, DepartmentResponse, PageResponse
│       │   │   ├── entity/          # JPA entities + enums
│       │   │   ├── exception/       # GlobalExceptionHandler, custom exceptions
│       │   │   ├── mapper/          # MapStruct mappers
│       │   │   ├── repository/      # Spring Data JPA repositories
│       │   │   ├── security/        # JWT service, filter, UserDetailsService
│       │   │   └── service/         # Business logic interfaces + implementations
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-prod.properties
│       │       └── db/migration/    # Flyway V1__init_schema.sql
│       └── test/                    # JUnit 5 + Testcontainers tests
│
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── api/                     # axiosInstance, queryClient
│       ├── components/
│       │   ├── auth/                # AuthLayout, LoginForm, RegisterForm, etc.
│       │   ├── common/              # LoadingScreen, PageLoader, ErrorBoundary
│       │   ├── dashboard/           # 15 dashboard widget components
│       │   ├── departments/         # 17 department management components
│       │   ├── employees/           # 17 employee management components
│       │   ├── layouts/             # AppLayout, Sidebar, Topbar
│       │   └── leaves/              # 21 leave management components
│       ├── constants/               # api, roles, routes, dashboard, employee, department, leave
│       ├── contexts/                # AuthContext
│       ├── hooks/                   # useEmployees, useDepartmentHooks, useLeaveHooks, etc.
│       ├── pages/
│       │   ├── auth/                # LoginPage, RegisterPage
│       │   ├── dashboard/           # DashboardPage
│       │   ├── departments/         # DepartmentsPage, DepartmentDetailsPage
│       │   ├── employees/           # EmployeesPage, EmployeeDetailsPage
│       │   ├── leaves/              # LeavesPage, LeaveDetailsPage, MyLeavesPage
│       │   ├── attendance/          # AttendancePage (placeholder)
│       │   ├── reviews/             # ReviewsPage (placeholder)
│       │   ├── profile/             # ProfilePage (placeholder)
│       │   └── settings/            # SettingsPage (placeholder)
│       ├── routes/                  # AppRoutes, ProtectedRoute, PublicRoute
│       ├── services/                # API service wrappers (per entity)
│       ├── tests/                   # Vitest unit + component tests
│       ├── theme/                   # MUI theme: palette, typography, components
│       └── utils/                   # Date, validation, formatters, columns, calculations
│
├── docker/                          # Nginx config
├── docker-compose.yml
├── .env.example
├── .github/workflows/ci.yml
├── jenkins/Jenkinsfile
└── postman/                         # Postman collection (14 requests)
```

---

## Frontend Features

### ✅ Authentication
- JWT login with remember-me
- Registration with password strength meter
- Auto-logout on token expiry
- Post-login redirect to original destination

### ✅ Dashboard
- Role-based layout (Admin / HR / Manager / Employee views)
- KPI summary cards with trend indicators
- Department distribution PieChart (Recharts)
- Employee status BarChart (Recharts)
- Recent activity feed
- Upcoming leaves widget
- Attendance summary widget
- Auto-refresh every 5 minutes

### ✅ Employee Management
- Server-side paginated, sortable table
- Search, department filter, status filter
- Create / Edit (modal dialog, RHF + Zod)
- Delete with confirmation dialog
- Full employee detail page
- Optimistic updates on edit/delete
- CSV export (current page)
- Responsive: table on desktop, cards on mobile

### ✅ Department Management
- Paginated sortable table
- Search + sort by name/code/created
- Create / Edit / Delete with CRUD dialogs
- Department details page with statistics card
- Embedded employee list per department
- Optimistic updates + dual cache invalidation
- CSV export

### ✅ Leave Management
- Full request lifecycle (PENDING → APPROVED / REJECTED / CANCELLED)
- Approval and rejection workflows (HR / Manager)
- Calendar view of leave requests
- Timeline view per employee
- Leave balance card
- Leave statistics (by type/status)
- My Leaves page (employee self-service)
- CSV export + snackbar notifications

---

## Backend Features

- **Spring Boot 3.5.3 / Java 21**
- **8 database tables**: `roles`, `users`, `departments`, `employees`, `leave_requests`, `attendance`, `performance_reviews`, `employee_role`
- **UUID primary keys** throughout
- **Flyway** migrations (V1__init_schema.sql)
- **JWT authentication** (HS256, configurable expiry)
- **Spring Security** method-level + URL-level protection
- **RFC 7807 ProblemDetail** error responses
- **MapStruct** DTO mapping with null-safety guards
- **Spring Data Auditing** (`createdAt` / `updatedAt` auto-populated)
- **Testcontainers** integration tests (MySQL 8)
- **OpenAPI 3 / Swagger UI** at `/swagger-ui.html`

---

## Database Overview

```
roles ──────────────────────────────────── (id, name)
        │
users ──┤ (id, email, password, firstName, lastName, role_id)
        │
departments ─────────────────────────────── (id, name, code, createdAt, updatedAt)
        │
employees ──┤ (id, employeeCode, departmentId, userId,
        │      jobTitle, phone, address,
        │      dateOfJoining, salary, status,
        │      createdAt, updatedAt)
        │
leave_requests (id, employeeId, leaveType, startDate, endDate,
                reason, status, reviewedBy, reviewedAt,
                createdAt, updatedAt)
        │
attendance (id, employeeId, date, checkIn, checkOut,
            status, notes, createdAt, updatedAt)
        │
performance_reviews (id, employeeId, reviewerId, reviewPeriod,
                     score, comments, status, createdAt, updatedAt)
```

---

## Authentication

1. **Register** → `POST /api/auth/register` → returns JWT + user info
2. **Login** → `POST /api/auth/login` → returns JWT + user info
3. JWT stored in `localStorage` (key: `emp_portal_token`)
4. Axios interceptor attaches `Authorization: Bearer <token>` to every request
5. 401 response → auto-clear storage + redirect to `/login?redirect=<original-path>`
6. Token expiry checked on every `AuthContext` render cycle

---

## API Overview

All endpoints are prefixed with `/api`. JWT required except auth endpoints.

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| POST | `/auth/register` | Register new user (returns JWT) | Public |
| POST | `/auth/login` | Authenticate, obtain JWT | Public |
| GET | `/employees` | Paginated employee list | All authenticated |
| POST | `/employees` | Create employee | ADMIN, HR |
| GET | `/employees/{id}` | Employee detail (own record only for EMPLOYEE) | All authenticated |
| PUT | `/employees/{id}` | Update employee | ADMIN, HR |
| DELETE | `/employees/{id}` | Delete employee | **ADMIN only** |
| GET | `/departments` | Paginated department list | All authenticated |
| GET | `/departments/all` | All departments (flat list, for dropdowns) | All authenticated |
| GET | `/departments/{id}` | Department detail | All authenticated |
| POST | `/departments` | Create department | ADMIN, HR |
| PUT | `/departments/{id}` | Update department | ADMIN, HR |
| DELETE | `/departments/{id}` | Delete department | **ADMIN only** |
| GET | `/leaves` | Paginated leave requests (own only for EMPLOYEE) | All authenticated |
| POST | `/leaves` | Submit leave request (own employee only for EMPLOYEE) | All authenticated |
| GET | `/leaves/{id}` | Leave detail (own only for EMPLOYEE) | All authenticated |
| PUT | `/leaves/{id}` | Update PENDING leave (own only for EMPLOYEE) | All authenticated |
| DELETE | `/leaves/{id}` | Cancel PENDING leave (own only for EMPLOYEE) | All authenticated |
| POST | `/leaves/{id}/approve` | Approve PENDING leave | ADMIN, HR, MANAGER |
| POST | `/leaves/{id}/reject` | Reject PENDING leave | ADMIN, HR, MANAGER |

---

## Docker Setup

### Prerequisites

| Tool | Minimum version |
|---|---|
| Docker Desktop | 24+ |
| Docker Compose v2 | built into Docker Desktop |

### Architecture

```
Browser
  │
  ▼  :80
Nginx  (frontend container)
  ├── /          →  React SPA (static files)
  └── /api/*     →  backend:8080  (Spring Boot)
                            │
                            ▼
                      mysql:3306  (MySQL 8)
```

All services run on an isolated bridge network (`emp_network`).
MySQL data is stored in the named volume `mysql_data` and survives `docker compose down`.

### Quick start

```bash
# 1. Copy the environment template and fill in secrets
cp .env.example .env
#    Edit .env — change DB_PASSWORD, MYSQL_ROOT_PASSWORD, and JWT_SECRET

# 2. Build images and start all services
docker compose up -d --build

# 3. Watch Flyway migrations on first boot
docker compose logs -f backend

# 4. Verify all services are healthy
docker compose ps
```

### Application URLs

| URL | Description |
|---|---|
| `http://localhost` | React SPA |
| `http://localhost/api` | Spring Boot REST API |
| `http://localhost/api/swagger-ui.html` | Swagger UI |
| `http://localhost/api/v3/api-docs` | OpenAPI JSON |
| `http://localhost/api/actuator/health` | Backend health |
| `http://localhost:8080/api` | Backend direct (127.0.0.1 only) |

### Startup health chain

```
mysql starts → healthcheck passes
  └── backend starts → Flyway V1 + V2 migrations → Spring Boot UP
        └── frontend starts (nginx)
```

### Managing the stack

```bash
# Stop containers (data is preserved)
docker compose down

# Stop AND delete ALL data
docker compose down -v          # ⚠ DELETES mysql_data volume

# Rebuild after code changes
docker compose up -d --build

# Stream logs
docker compose logs -f backend
docker compose logs -f mysql

# Check status and health
docker compose ps

# Open a shell in the backend container
docker compose exec backend sh
```

### Data persistence

```bash
docker compose down           # containers stop, mysql_data volume intact
docker compose up -d          # data still there ✅
docker compose down -v        # ⚠ DELETES all MySQL data
```

---

## AWS Deployment (Production)

### Architecture overview

```
GitHub Actions → OIDC → IAM Role → ECR push
                                   ↓
                              ECR repositories
                                   ↓
                         EC2 (IAM instance role → ECR pull)
                                   ↓
                          docker compose pull
                          docker compose up -d
                                   ↓
                              Health check
```

### Required AWS resources

| Resource | Name | Type | Bootstrap |
|---|---|---|---|
| ECR repository | `employee-management-backend` | Private, IMMUTABLE | Manual |
| ECR repository | `employee-management-frontend` | Private, IMMUTABLE | Manual |
| IAM OIDC provider | `token.actions.githubusercontent.com` | OIDC IdP | Manual |
| IAM role | `GitHubActions-ECRPush-Role` | GitHub Actions push | Manual |
| IAM policy | `GitHubActions-ECRPush-Policy` | ECR push (least-priv) | Manual |
| IAM role | `EC2-EmpPortal-InstanceRole` | EC2 ECR pull | Manual |
| IAM policy | `EC2-EmpPortal-ECRPull-Policy` | ECR pull (least-priv) | Manual |
| IAM instance profile | `EC2-EmpPortal-InstanceProfile` | Attach to EC2 | Manual |
| EC2 instance | `emp-portal-prod` | Amazon Linux 2023 | Manual |
| RDS instance | `emp-portal-db` | MySQL 8.0 | Manual |
| Security group | `emp-portal-app-sg` | EC2 — ports 22, 80 | Manual |
| Security group | `emp-portal-db-sg` | RDS — port 3306 from app SG | Manual |

All of the above are **manual bootstrap** resources.
See `aws/` directory for setup commands and IAM policy JSON files.
Future: migrate to Terraform.

### Bootstrap sequence

```bash
# 1. Create ECR repositories (once per account)
export AWS_REGION=us-east-1
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
bash aws/ecr/bootstrap-ecr.sh

# 2. Create OIDC provider + IAM roles (see aws/iam/iam-setup.md)

# 3. Launch EC2 (Amazon Linux 2023, t3.small or larger)
#    Attach: IAM instance profile EC2-EmpPortal-InstanceProfile
#    User data: aws/ec2/bootstrap-ec2.sh

# 4. Create RDS MySQL 8 (private subnet, not publicly accessible)
#    Create database user and grant privileges

# 5. Configure GitHub repository
#    Variables:  AWS_ROLE_ARN, AWS_REGION, ECR_REGISTRY, EC2_USER
#    Secrets:    EC2_HOST, EC2_SSH_KEY
#    Environment: production (with required reviewers if desired)

# 6. On EC2 — create /opt/emp-portal/.env.production
cp .env.production.example /opt/emp-portal/.env.production
# Fill in real RDS endpoint, credentials, JWT_SECRET
chmod 600 /opt/emp-portal/.env.production

# 7. Push to main — CI/CD pipeline runs automatically
```

### GitHub Actions secrets and variables

Set in: **Repository → Settings → Secrets and variables → Actions**

| Type | Name | Value |
|---|---|---|
| Variable | `AWS_ROLE_ARN` | `arn:aws:iam::ACCOUNT:role/GitHubActions-ECRPush-Role` |
| Variable | `AWS_REGION` | e.g. `us-east-1` |
| Variable | `ECR_REGISTRY` | `ACCOUNT.dkr.ecr.REGION.amazonaws.com` |
| Variable | `EC2_USER` | `ec2-user` |
| Secret | `EC2_HOST` | EC2 public IP or DNS |
| Secret | `EC2_SSH_KEY` | Private SSH key (PEM format) |

> No `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` are used anywhere.

### Deployment flow (main branch push)

```
1. backend-ci    — mvn clean verify (155 tests)
2. frontend-ci   — npm test (326 tests) + lint + build
3. docker-build  — validate Dockerfiles build cleanly
4. compose-validate — validate docker-compose.yml syntax
5. publish       — OIDC → ECR push, tags: <sha> + main-<sha>
6. deploy        — SCP compose+scripts → EC2 SSH
                   → ECR login (instance role)
                   → docker compose pull
                   → docker compose up -d
                   → health-check.sh (actuator + nginx)
```

### Rollback

```bash
# On EC2 — rollback to any previous SHA
export ECR_REGISTRY=<account>.dkr.ecr.<region>.amazonaws.com
export AWS_REGION=us-east-1
bash /opt/emp-portal/scripts/rollback.sh <previous-sha>

# Find available SHAs
aws ecr describe-images \
  --repository-name employee-management-backend \
  --query 'sort_by(imageDetails, &imagePushedAt)[-10:].imageTags' \
  --output table
```

### Production environment variables

Copy `.env.production.example` → `/opt/emp-portal/.env.production` on EC2.
Never commit `.env.production` — it is gitignored.

| Variable | Required | Description |
|---|---|---|
| `DB_HOST` | Yes | RDS endpoint |
| `DB_PORT` | No | Default `3306` |
| `DB_NAME` | Yes | Database name |
| `DB_USER` | Yes | Database application user |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | HS256 key (≥ 32 chars, `openssl rand -base64 48`) |
| `CORS_ORIGINS` | Yes | Production frontend URL |
| `ECR_REGISTRY` | Yes | Injected by deploy step |
| `IMAGE_TAG` | Yes | Git SHA, injected by deploy step |

---

## Development Setup (without Docker)

### Backend

```bash
cd backend

# Requires a running MySQL instance on localhost:3306
mvn clean install -DskipTests
mvn spring-boot:run

# API:      http://localhost:8080/api
# Swagger:  http://localhost:8080/api/swagger-ui.html
```

### Frontend

```bash
cd frontend

npm install
npm run dev

# App:  http://localhost:5173
# Vite proxies /api/* → http://localhost:8080 (vite.config.js)
```

---

## Environment Variables

Copy `.env.example` to `.env` (gitignored — never commit it).

```dotenv
# MySQL
MYSQL_ROOT_PASSWORD=RootChangeMe123!
DB_NAME=emp_portal
DB_USER=emp_user
DB_PASSWORD=AppChangeMe456!

# JWT signing key (minimum 32 characters)
# Generate: openssl rand -base64 48
JWT_SECRET=ReplaceWithAtLeast32CharRandomString!!

# CORS allowed origins
CORS_ORIGINS=http://localhost
```

| Variable | Required | Default | Description |
|---|---|---|---|
| `MYSQL_ROOT_PASSWORD` | Yes | — | MySQL root password (init only) |
| `DB_NAME` | No | `emp_portal` | Database name |
| `DB_USER` | No | `emp_user` | App database user |
| `DB_PASSWORD` | Yes | — | App database password |
| `JWT_SECRET` | Yes | — | HS256 signing key (≥ 32 chars) |
| `CORS_ORIGINS` | No | `http://localhost` | Allowed CORS origins |

---

## NPM Scripts

```bash
npm run dev          # Start Vite dev server (http://localhost:5173)
npm run build        # Production build → dist/
npm run preview      # Preview production build locally
npm run lint         # ESLint (0 warnings policy)
npm run lint:fix     # ESLint with auto-fix
npm run format       # Prettier format all src files
npm run test         # Run all tests once (Vitest)
npm run test:watch   # Run tests in watch mode
npm run test:coverage # Generate coverage report (v8)
```

---

## Maven Commands

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package JAR
mvn clean package

# Package skipping tests
mvn clean package -DskipTests

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Generate test coverage report (JaCoCo)
mvn test jacoco:report
```

---

## Testing

### Backend Tests

```bash
cd backend
mvn test          # unit + integration tests
mvn clean verify  # full build + test (CI command)
```

- **JUnit 5** + **Mockito** for unit tests
- **Testcontainers** (MySQL 8) for integration/repository tests
- **155 test cases** across 10 test classes:
  - `JwtServiceTest` — JWT token lifecycle (12 cases)
  - `AuthServiceTest` — login/register service logic (4 cases)
  - `EmployeeServiceTest` — CRUD + ownership checks (11 cases)
  - `EmployeeControllerTest` — HTTP layer (8 cases)
  - `GlobalExceptionHandlerTest` — error response format (12 cases)
  - `RbacSecurityTest` — RBAC enforcement (38 cases)
  - `AuditingIntegrationTest` — JPA auditing (`@DataJpaTest`, 19 cases)
  - `ApiDocumentationTest` — OpenAPI + 401/403/404/400 responses (21 cases)
  - `PersistenceRepositoryTest` — JPA repository layer (`@DataJpaTest`, 21 cases)
  - `EmployeeManagementIntegrationTest` — full stack (Testcontainers, 9 cases)

### Frontend Tests

```bash
cd frontend
npm test            # Single run (vitest run)
npm run test:watch  # Watch mode
npm run test:coverage
```

- **Vitest** + **React Testing Library** + **jsdom**
- Test files live in `src/tests/`
- **326 test cases** across 22 test files:
  - `AuthContext` — auth context state and token management (9 cases)
  - `LoginPage` / `RegisterPage` — form validation and submission (35 cases)
  - `useDashboard` / `DashboardPage` — hooks and page states (20 cases)
  - `dashboardFormatters` — dashboard formatting utilities (27 cases)
  - `useEmployees` / `EmployeesPage` / `EmployeeTable` / `EmployeeForm` — employee module (32 cases)
  - `employeeFormatters` — employee formatting utilities (23 cases)
  - `useDepartmentHooks` / `DepartmentsPage` / `DepartmentTable` / `DepartmentForm` — department module (30 cases)
  - `departmentFormatters` — department formatting utilities (18 cases)
  - `useLeaveHooks` / `LeavesPage` / `LeaveTable` / `LeaveForm` — leave module (57 cases)
  - `leaveCalculations` — leave day calculation logic (40 cases)
  - `leaveFormatters` — leave formatting utilities (36 cases)

---

## Screenshots

> *Screenshots will be added after UI stabilisation.*

| Screen | Description |
|---|---|
| Login | `docs/screenshots/login.png` |
| Dashboard (Admin) | `docs/screenshots/dashboard-admin.png` |
| Employee List | `docs/screenshots/employees.png` |
| Employee Details | `docs/screenshots/employee-detail.png` |
| Departments | `docs/screenshots/departments.png` |
| Leave Management | `docs/screenshots/leaves.png` |
| My Leaves | `docs/screenshots/my-leaves.png` |
| Leave Calendar | `docs/screenshots/leave-calendar.png` |

---

## Roadmap

| Phase | Module | Status |
|---|---|---|
| DevOps 1 | Docker & Docker Compose | ✅ Done |
| DevOps 2 | GitHub Actions CI | ✅ Done |
| DevOps 3 | Registry push + deployment pipeline | Next |
| 3G | Attendance Management | Upcoming |
| 3H | Performance Reviews | Upcoming |
| 3I | Profile + Settings Pages | Upcoming |
| 4A | Backend Dashboard API | Upcoming |
| 4B | Notifications (WebSocket) | Future |
| 4C | Reports & Analytics | Future |
| 4D | Bulk Import (CSV/Excel) | Future |
| 5A | Mobile App (React Native) | Future |

---

## Changelog

### DevOps Phase 4 — Real AWS Deployment Verification *(current)*
- Fixed `health-check.sh`: replaced `(( N++ ))` with `N=$((N+1))` (bash strict-mode arithmetic trap at 0)
- Fixed `health-check.sh`: replaced `compose ps --format` with `docker inspect` (stable across Compose v2 versions)
- Fixed `health-check.sh`: fallback DB check when `show-details` omits components
- Fixed `deploy.sh`: health-wait loop rewritten using `docker inspect` — eliminates false-healthy-on-empty-output bug
- Fixed `deploy.sh`: added `AWS_REGION` validation guard; increased timeout to 180s for cold RDS starts
- Fixed `application-prod.properties`: `show-details=never` → `show-details=always` (port 8080 is not published)
- Fixed `ci.yml`: `compose-validate` job now also validates `docker-compose.prod.yml` with CI placeholder vars
- Fixed `ci.yml`: removed invalid `strip_components` from `appleboy/scp-action` step
- Added `scripts/smoke-test.sh`: 11 real API checks (auth, CRUD, RBAC, leave workflow, cleanup)
- Added smoke-test step to `deploy` job in `ci.yml`
- Added `aws/ec2/verify-ec2.sh`: on-host verification (Docker, Compose, IAM role, ECR auth, files, permissions)
- Added `aws/ec2/ec2-rds-setup.md`: step-by-step security group, EC2, RDS, deploy-user SSH key setup
- Updated `.env.production.example`: clearer variable split (on-EC2 vs CI-injected), stronger placeholder names

### DevOps Phase 3 — AWS ECR + OIDC + EC2 Deployment
- Added `publish` job: OIDC → `aws-actions/configure-aws-credentials@v4` → ECR push with immutable SHA tags
- Added `deploy` job: SCP compose + scripts to EC2, SSH deploy via `appleboy/ssh-action`, health verification
- No static AWS credentials — GitHub Actions uses `id-token: write` OIDC; EC2 uses IAM instance role
- Created `docker-compose.prod.yml`: ECR images, RDS MySQL (no local MySQL container)
- Created `.env.production.example`: production template (never committed)
- Created `scripts/deploy.sh`: ECR login → pull → up -d → health wait loop
- Created `scripts/health-check.sh`: Nginx /healthz + Spring Boot /api/actuator/health + DB check
- Created `scripts/rollback.sh`: one-line rollback to any previous SHA
- Created `aws/ecr/bootstrap-ecr.sh`: one-time ECR repository creation + lifecycle policies
- Created `aws/ec2/bootstrap-ec2.sh`: Amazon Linux 2023 user-data (Docker, Compose v2, deploy user)
- Created `aws/iam/` — IAM trust policy JSON, ECR push policy JSON, ECR pull policy JSON, setup guide
- Existing 4 CI jobs (backend-ci, frontend-ci, docker-build, compose-validate) preserved unchanged

### DevOps Phase 2 — CI/CD (GitHub Actions)
- Rewrote `.github/workflows/ci.yml`: 4-job pipeline (backend-ci, frontend-ci, docker-build, compose-validate)
- Java 17 → **Java 21** throughout CI (matches `pom.xml` `<java.version>21</java.version>`)
- Added `npm test` (Vitest) step — frontend tests now run in CI (previously only lint + build)
- `docker-build` job: BuildKit GHA cache, `push: false` validation only, runs after both CI jobs pass
- `compose-validate` job: generates minimal `.env`, runs `docker compose config --quiet` on every push/PR
- Added `permissions: contents: read` (minimum required), `concurrency` cancel-in-progress
- PR trigger now covers both `main` and `develop` branches
- Removed stale registry env vars (`REGISTRY`, `IMAGE_NAME`) — no push in this loop

### DevOps Phase 1 — Docker & Docker Compose
- `backend/Dockerfile`: Java 17 → 21, added tini, non-root `appuser`, 3-stage build
- `frontend/Dockerfile`: 3-stage build with dedicated npm dep-cache layer
- `frontend/nginx.conf`: security headers (CSP, HSTS, X-Frame-Options), gzip, proxy timeouts, `/healthz` endpoint
- `docker-compose.yml`: complete rewrite — health checks, `127.0.0.1` port bindings, `service_healthy` dependency chain, correct env vars, SSL override
- `.env.example`: corrected variable names, added `MYSQL_ROOT_PASSWORD`
- Added `backend/.dockerignore`, `frontend/.dockerignore`, `.dockerignore` (root)

### Database & Persistence Phase
- Created `V2__add_roles_audit_columns_and_indexes.sql`: adds `created_at`/`updated_at`/`created_by`/`updated_by` to `roles` table + 7 performance indexes
- Fixed N+1 in `DepartmentMapper`: new `countEmployeesByDepartmentId()` COUNT query
- Fixed N+1 in employee list: two-step `findAllIds` + `findAllWithAssociationsByIds` JOIN FETCH strategy
- Fixed N+1 in leave request list: same two-step ID + JOIN FETCH pattern
- Added `PersistenceRepositoryTest.java` — 21 `@DataJpaTest` cases covering all repository operations

### Development Loop 4 — API Documentation & Consistency
- Expanded `OpenApiConfig.java` description: role table, auth instructions, error format reference
- Added `springdoc.swagger-ui.try-it-out-enabled=true` and `tags-sorter=alpha` to `application.properties`
- Fixed README API table: `DELETE /employees/{id}` and `DELETE /departments/{id}` now correctly list **ADMIN only**
- Added missing `GET /departments/all` entry to API table
- Corrected Swagger UI URL from `/swagger-ui.html` to `/api/swagger-ui.html` (context-path aware)
- Added Swagger UI auth flow instructions to README
- Created `ApiDocumentationTest.java` — 15 WebMvcTest cases covering OpenAPI descriptor availability, structured 401/403/400/404 responses

### Development Loop 3 — JPA Auditing
- Confirmed `BaseEntity` `createdAt`/`updatedAt` auditing already existed
- Added `createdBy`/`updatedBy` fields to `EmployeeResponse`, `LeaveRequestResponse`, `DepartmentResponse`
- Added H2 test dependency to `pom.xml`
- Created `AuditingIntegrationTest.java` — 20 `@DataJpaTest` cases verifying audit field population

### Development Loop 2 — Role-Based Access Control (RBAC)
- Created `SecurityUtils` component (`security/SecurityUtils.java`)
- Added `EmployeeRepository.findByUserId(UUID)` for employee-ownership lookups
- Updated `SecurityConfig`: JSON `AuthenticationEntryPoint` for 401, MANAGER added to leave approve/reject
- `LeaveRequestServiceImpl`: full ownership enforcement (findAll scoped, findById/create/update/cancel check ownership)
- `EmployeeServiceImpl`: ownership check on `findById` for EMPLOYEE role
- `LeaveController`: `@PreAuthorize` updated to include MANAGER on approve/reject
- Created `RbacSecurityTest.java` — 31 `@WebMvcTest` cases covering all 4 roles, unauthenticated access, resource-ownership violations
- Frontend `axiosInstance.js`: 403 interceptor redirecting to `/403`

### Phase 3F — Leave Management Module
- Added `LeavesPage`, `LeaveDetailsPage`, `MyLeavesPage` with full CRUD
- Full approval/rejection workflow (HR + Manager roles)
- Calendar view (monthly grid) and timeline view per employee
- Leave balance card and leave statistics component
- 21 reusable leave components
- 7 React Query hooks with optimistic updates
- Leave day calculation utilities (excluding weekends + holidays)
- 6 test files (~70 test cases)
- Added `README.md` (this file)
- Added `.gitignore`

### Phase 3E — Department Management Module
- `DepartmentsPage` + `DepartmentDetailsPage`
- 17 reusable department components
- 5 department React Query hooks with optimistic updates + dual cache invalidation
- Department employee list embedded in detail page

### Phase 3D — Employee Management Module
- `EmployeesPage` + `EmployeeDetailsPage`
- 17 reusable employee components
- 5 employee React Query hooks with optimistic updates
- Server-side sort, search, filter, pagination
- CSV export

### Phase 3C — Dashboard Module
- Role-based dashboard (Admin/HR/Manager/Employee layouts)
- Recharts PieChart + BarChart integration
- 15 dashboard components, 4 React Query hooks

### Phase 3B — Authentication Module
- Login + Register pages with RHF + Zod validation
- Password strength meter, remember-me, post-login redirect

### Phase 3A — Frontend Foundation
- Vite + React 19 + MUI v7 scaffold
- TanStack Query, React Router v7, Axios interceptors
- Theme (light/dark), routing, layouts, error boundary

### Phase 2V — Backend Tests + Postman
- 53 unit + integration test cases
- Postman collection with 14 automated requests

### Phase 2 — Backend
- Spring Boot 3.5.3 / Java 21 full backend
- 47 Java source files
- JWT auth, RBAC, Flyway migrations, MapStruct, Testcontainers

### Phase 1 — Project Scaffold
- Folder structure, pom.xml, package.json, Docker Compose, CI/CD

---

## License

*This project is licensed under the MIT License. See `LICENSE` for details.*

---

## Author

**Enterprise Engineering Team**

Built with ☕ Java and ⚛️ React.
