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
| Docker + Docker Compose | Local containerised development |
| Jenkins | CI/CD pipeline |
| GitHub Actions | Automated CI on push/PR |
| AWS EC2 | Production hosting |
| Nginx | Reverse proxy + static serving |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        AWS EC2 Instance                     │
│  ┌─────────────┐    ┌──────────────────────────────────┐   │
│  │    Nginx    │───▶│   React (Vite Build / Static)    │   │
│  │  :80 / :443 │    └──────────────────────────────────┘   │
│  │             │    ┌──────────────────────────────────┐   │
│  │  /api proxy │───▶│   Spring Boot (Java 21)  :8080   │   │
│  └─────────────┘    └──────────────────┬─────────────┘    │
│                                         │                    │
│                      ┌──────────────────▼──────────────┐   │
│                      │        MySQL 8.0  :3306          │   │
│                      └─────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
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

## Installation

### Prerequisites

- Java 21+
- Node.js 20+
- npm 10+
- Maven 3.9+
- Docker + Docker Compose
- MySQL 8.0 (or use Docker Compose)

### Quick Start (Docker)

```bash
# Clone the repository
git clone https://github.com/your-org/employee-management-portal.git
cd employee-management-portal

# Copy environment template
cp .env.example .env

# Edit .env with your values (DB credentials, JWT secret, etc.)

# Start all services
docker-compose up -d

# Access the portal
# Frontend:    http://localhost:3000
# Backend API: http://localhost:8080/api
# Swagger UI:  http://localhost:8080/api/swagger-ui.html
# API docs:    http://localhost:8080/api/v3/api-docs
```

> **API context path**: All backend routes are served under `/api` (configured via
> `server.servlet.context-path=/api`). Swagger UI is therefore reachable at
> `http://localhost:8080/api/swagger-ui.html`.
>
> **Quick auth flow in Swagger UI**: call `POST /api/auth/login`, copy the
> `accessToken` value from the response, click **Authorize** 🔒 at the top of
> the page, and paste the token. All protected endpoints will then include the
> `Authorization: Bearer <token>` header automatically.

---

## Development Setup

### Backend

```bash
cd backend

# Build (skip tests for speed)
mvn clean install -DskipTests

# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# The API will be available at http://localhost:8080
```

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start the development server
npm run dev

# The app will be available at http://localhost:5173
# Vite proxies /api/* to http://localhost:8080/api
```

---

## Docker Setup

```bash
# Build and start all containers
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop all containers
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

### Services

| Service | Port | Description |
|---|---|---|
| `frontend` | 3000 | React Vite build served by Nginx |
| `backend` | 8080 | Spring Boot API |
| `mysql` | 3306 | MySQL 8.0 database |

---

## Environment Variables

Copy `.env.example` to `.env` and fill in your values:

```dotenv
# Database
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=employee_db
MYSQL_USER=emp_user
MYSQL_PASSWORD=emp_password

# Spring Boot
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/employee_db
SPRING_DATASOURCE_USERNAME=emp_user
SPRING_DATASOURCE_PASSWORD=emp_password

# JWT
JWT_SECRET=your-256-bit-secret-minimum-32-chars
JWT_EXPIRATION_MS=86400000

# Frontend (Vite)
VITE_API_BASE_URL=/api
```

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
mvn test
```

- **JUnit 5** + **Mockito** for unit tests
- **Testcontainers** (MySQL 8) for integration tests
- **106 test cases** across 7 test classes:
  - `JwtServiceTest` — JWT token lifecycle (7 cases)
  - `AuthServiceTest` — login/register service logic (6 cases)
  - `EmployeeServiceTest` — CRUD + ownership checks (9 cases)
  - `EmployeeControllerTest` — HTTP layer (11 cases)
  - `GlobalExceptionHandlerTest` — error response format (7 cases)
  - `RbacSecurityTest` — RBAC enforcement (31 cases)
  - `AuditingIntegrationTest` — JPA auditing (@DataJpaTest, 20 cases)
  - `ApiDocumentationTest` — OpenAPI availability + 401/403/404/400 (15 cases)
  - `EmployeeManagementIntegrationTest` — full stack (Testcontainers)

### Frontend Tests

```bash
cd frontend
npm run test        # Single run
npm run test:watch  # Watch mode
npm run test:coverage
```

- **Vitest** + **React Testing Library** + **jsdom**
- Test files live in `src/tests/`
- **~200 test cases** across:
  - Auth context, login/register pages
  - Dashboard hooks + page states
  - Employee hooks, table, form, page
  - Department hooks, table, form, page
  - Leave hooks, table, form, page, calculations
  - Formatter utilities for all modules

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

| Phase | Module | Target |
|---|---|---|
| 3G | Attendance Management | Next |
| 3H | Performance Reviews | Upcoming |
| 3I | Profile + Settings Pages | Upcoming |
| 4A | Backend Dashboard API | Upcoming |
| 4B | Notifications (WebSocket) | Future |
| 4C | Reports & Analytics | Future |
| 4D | Bulk Import (CSV/Excel) | Future |
| 5A | Mobile App (React Native) | Future |

---

## Changelog

### Development Loop 4 — API Documentation & Consistency *(current)*
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
