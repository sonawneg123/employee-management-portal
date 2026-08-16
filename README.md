# Employee Management Portal

A production-ready, full-stack **Employee Management Portal** built with **Spring Boot 3 / Java 21** (backend) and **React 19** (frontend). The portal covers the complete HR lifecycle: employee & department management, attendance, leave, task management with submissions and reviews, real-time notifications, an AI HR Assistant with RAG/vector search, and a rich task analytics dashboard.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture](#architecture)
4. [Completed Modules & Roadmap](#completed-modules--roadmap)
5. [Project Structure](#project-structure)
6. [Authentication & Authorization](#authentication--authorization)
7. [Core Modules](#core-modules)
   - [Employee Management](#employee-management)
   - [Department Management](#department-management)
   - [Attendance Management](#attendance-management)
   - [Leave Management](#leave-management)
8. [Task Management — Phase 6A](#task-management--phase-6a)
   - [Attendance-Aware Task Assignment](#attendance-aware-task-assignment)
   - [Task Status & Activity Timeline](#task-status--activity-timeline)
   - [Task Comments](#task-comments)
   - [Task Categories & Priority](#task-categories--priority)
   - [Task Attachments](#task-attachments)
9. [Task Submission System — Phase 6B / 6B.1](#task-submission-system--phase-6b--6b1)
10. [Notifications — Phase 6A.1](#notifications--phase-6a1)
11. [Deadline Reminders](#deadline-reminders)
12. [Employee Workload Protection](#employee-workload-protection)
13. [Task Dashboard — Phase 6C–6E](#task-dashboard--phase-6c6e)
14. [Manager & HR Module](#manager--hr-module)
15. [AI Assistant / RAG Infrastructure](#ai-assistant--rag-infrastructure)
16. [File Storage Architecture](#file-storage-architecture)
17. [Database & Flyway Migrations](#database--flyway-migrations)
18. [API Documentation](#api-documentation)
19. [Security](#security)
20. [Testing](#testing)
21. [Build & Run Instructions](#build--run-instructions)
22. [Environment Variables](#environment-variables)
23. [Docker Setup](#docker-setup)
24. [NPM Scripts](#npm-scripts)
25. [Maven Commands](#maven-commands)
26. [Phase 7 — Upcoming: AI Task Analysis](#phase-7--upcoming-ai-task-analysis)
27. [Screenshots](#screenshots)
28. [License](#license)
29. [Author](#author)

---

## Project Overview

The **Employee Management Portal** is a full-stack enterprise application for organisations to manage their entire employee lifecycle:

| Domain | Capability |
|---|---|
| **Employee Management** | Full CRUD, profile, department assignment, role assignment |
| **Department Management** | Organisational hierarchy with embedded employee lists |
| **Leave Management** | Request, approval/rejection workflow, calendar view |
| **Attendance** | Daily check-in/out tracking, role-aware visibility |
| **Task Management** | Manager/HR task creation, assignment, monitoring, reassignment |
| **Task Submissions** | Employee work submission with file attachments, manager review cycle |
| **Task Comments** | Threaded discussion between employees and managers per task |
| **Task Activity Timeline** | Immutable audit log of all task lifecycle events |
| **Task Attachments** | Manager-uploaded task reference files, employee download |
| **Notifications** | In-app bell, sound alerts, unread count, mark-as-read |
| **Deadline Reminders** | Scheduled 24h, 2h, and overdue reminders via notifications |
| **AI HR Assistant** | Chat with a Groq/Llama-backed HR assistant using RAG/vector retrieval |
| **Performance Reviews** | Review records, ratings, role-aware CRUD |
| **Profile & Settings** | Self-service profile view and password change |
| **Task Dashboard** | Manager/HR task analytics, workload summary, overdue/urgent statistics |

The system enforces **role-based access control (RBAC)** with four roles: `ADMIN`, `HR`, `MANAGER`, `EMPLOYEE`.

---

## Technology Stack

### Backend

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| Spring Boot | 3.5.x | Application framework |
| Spring Security | 6.x | Authentication + RBAC |
| Spring Data JPA / Hibernate | 3.x / 6.x | ORM layer |
| JWT (JJWT) | 0.12.x | Stateless auth tokens (HS256) |
| MapStruct | 1.6.3 | DTO/entity mapping |
| Lombok | 1.18.x | Boilerplate reduction |
| Flyway | 10.x | Database migrations (V1–V26) |
| MySQL | 8.0 | Relational database |
| Maven | 3.9+ | Build tool |
| OpenAPI 3 / SpringDoc | latest | Swagger UI + API specification |

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
| Vitest | v2 | Unit / component testing |
| React Testing Library | v16 | Component testing utilities |

### AI / RAG Infrastructure

| Component | Technology | Notes |
|---|---|---|
| Chat completions | Groq API (Llama) | `llama-3.1-8b-instant` (default) |
| Embeddings | Hugging Face Inference API | `nomic-ai/nomic-embed-text-v1.5` (768-dim) |
| Vector store | MySQL (persisted embeddings) | `knowledge_chunks` table |
| Retrieval strategy | Vector (cosine similarity) or keyword | Configurable via `RAG_RETRIEVAL_STRATEGY` |
| Similarity threshold | Cosine similarity ≥ 0.70 (default) | Configurable |
| Knowledge ingestion | REST API (ADMIN/HR only) | Document chunking + embedding |

> **Note:** The AI infrastructure currently powers the **HR Assistant chat** feature. Task-submission AI analysis (Phase 7) is **not yet implemented**.

### DevOps / Infrastructure

| Tool | Purpose |
|---|---|
| Docker + Docker Compose | Local containerised development |
| `docker-compose.prod.yml` | Production deployment (ECR images + RDS) |
| GitHub Actions | CI/CD pipeline (test → build → publish → deploy) |
| Amazon ECR | Private container registry |
| Amazon EC2 | Production application host (Amazon Linux 2023) |
| Amazon RDS MySQL 8 | Production managed database |
| AWS IAM + OIDC | Keyless authentication between GitHub Actions and AWS |
| Nginx | Reverse proxy + static serving |
| Jenkins | Alternative CI pipeline (`jenkins/Jenkinsfile`) |

---

## Architecture

```
Browser
  │
  ▼  :80
Nginx  (frontend container)
  ├── /          →  React SPA (static files)
  └── /api/*     →  backend:8080  (Spring Boot)
                             │
                 ┌───────────┴───────────────────────────────┐
                 │         Spring Boot Application            │
                 │                                            │
                 │  SecurityFilterChain (JWT Bearer)          │
                 │  Controllers (REST API)                    │
                 │  Services (Business Logic)                 │
                 │  Repositories (Spring Data JPA)            │
                 │  FileStorageService → LocalFileStorage     │
                 │  TaskDeadlineReminderService (@Scheduled)  │
                 │  AiChatService → Groq API                  │
                 │  HuggingFaceEmbeddingService → HF API      │
                 └───────────────────────────────────────────┘
                             │
                       MySQL 8 / RDS
                  (Flyway V1–V26 migrations)
```

**Key design decisions:**

- Backend returns **RFC 7807 ProblemDetail** for all errors (`application/problem+json`)
- All PKs are **UUID** (no sequential integer IDs exposed in the API)
- DTOs only — JPA entities are never serialised to the API
- JWT is stateless — no server-side session storage
- Frontend uses **TanStack Query** for all server state (no Redux / Zustand)
- React Query cache is invalidated optimistically on mutations
- File storage uses an abstraction (`FileStorageService`) so that switching from local filesystem to S3 only requires a new implementation bean

---

## Completed Modules & Roadmap

### ✅ Completed

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
| Phase 3G | Attendance Module | ✅ Complete |
| Phase 3H | Performance Reviews Module | ✅ Complete |
| Phase 3I | Profile & Settings Module | ✅ Complete |
| Phase 4 | AI HR Assistant — Frontend Integration | ✅ Complete |
| Phase 5 | AI/Security/RAG Audit | ✅ Complete |
| Phase 6A | Core Task Management | ✅ Complete |
| Phase 6A.1 | Notifications, Activity Tracking & Attendance-Aware Task Workflow | ✅ Complete |
| Phase 6B | Task Submission & Review | ✅ Complete |
| Phase 6B.1 | Submission File Attachments | ✅ Complete |
| Phase 6C–6E | Advanced Task Management Expansion (Dashboard, Filters, Workload) | ✅ Complete |

### 🔜 Next

| Phase | Module | Status |
|---|---|---|
| Phase 7 | AI Task Analysis & Review | **Not implemented yet** |

---

## Project Structure

```
employee-management-portal/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/company/employeemanagement/
│       │   │   ├── EmployeeManagementApplication.java
│       │   │   ├── ai/                      # AI Assistant + RAG infrastructure
│       │   │   │   ├── client/              # GroqClient, GroqApiTypes
│       │   │   │   ├── config/              # GroqConfig, GroqProperties
│       │   │   │   ├── controller/          # AiChatController
│       │   │   │   ├── dto/                 # AiChatRequest, AiChatResponse
│       │   │   │   ├── rag/                 # RAG pipeline
│       │   │   │   │   ├── config/          # RagConfig, RagProperties
│       │   │   │   │   ├── controller/      # KnowledgeController
│       │   │   │   │   ├── embedding/       # HuggingFaceEmbeddingService, VectorSimilarity
│       │   │   │   │   ├── entity/          # KnowledgeDocument, KnowledgeChunk
│       │   │   │   │   ├── repository/      # KnowledgeChunkRepository, KnowledgeDocumentRepository
│       │   │   │   │   └── service/         # DocumentChunkingService, KnowledgeIngestionService,
│       │   │   │   │                        #   VectorKnowledgeRetrievalService, RagPromptContextBuilder
│       │   │   │   └── service/             # AiChatService, AiSystemPrompt
│       │   │   ├── config/                  # SecurityConfig, AuditingConfig, FileStorageProperties, OpenApiConfig
│       │   │   ├── controller/              # Auth, Employee, Department, Leave, Attendance,
│       │   │   │                            #   Task, TaskSubmission, TaskAttachment, TaskComment,
│       │   │   │                            #   Notification, Dashboard, Review, Settings, Profile
│       │   │   ├── dto/
│       │   │   │   ├── request/             # Create/Update*Request, ReassignTaskRequest, etc.
│       │   │   │   └── response/            # *Response, PageResponse, TaskDashboardStatsResponse
│       │   │   ├── entity/                  # JPA entities + enums
│       │   │   │   ├── Task, TaskActivity, TaskAttachment, TaskComment, TaskSubmission
│       │   │   │   ├── Notification, Employee, Department, LeaveRequest, Attendance, PerformanceReview
│       │   │   │   └── enums/               # TaskStatus, TaskPriority, TaskCategory, SubmissionStatus,
│       │   │   │                            #   NotificationType, AttendanceStatus, LeaveStatus, ...
│       │   │   ├── exception/               # GlobalExceptionHandler, custom exceptions
│       │   │   ├── mapper/                  # MapStruct mappers
│       │   │   ├── repository/              # Spring Data JPA repositories
│       │   │   ├── security/                # JwtService, JwtAuthenticationFilter, SecurityUtils
│       │   │   └── service/                 # Business logic interfaces + implementations
│       │   │       └── impl/                # LocalFileStorageService, TaskDeadlineReminderService, ...
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-prod.properties
│       │       └── db/migration/            # Flyway V1–V26 migration scripts
│       └── test/                            # JUnit 5 + Mockito + Testcontainers tests
│
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/                             # axiosInstance, queryClient
│       ├── components/
│       │   ├── auth/                        # Login/Register forms
│       │   ├── common/                      # LoadingScreen, ErrorBoundary
│       │   ├── dashboard/                   # Dashboard widgets
│       │   ├── departments/, employees/     # Module components
│       │   ├── layouts/                     # AppLayout, Sidebar (role-aware), Topbar
│       │   ├── leaves/                      # Leave components
│       │   ├── notifications/               # NotificationBell, NotificationDropdown
│       │   └── tasks/                       # Task list, form, detail, submission, comments, attachments
│       ├── constants/                       # API paths, routes, roles
│       ├── contexts/                        # AuthContext (JWT, role helpers, auto-logout)
│       ├── hooks/                           # useEmployees, useLeaveHooks, useNotificationSound, ...
│       ├── pages/
│       │   ├── auth/                        # LoginPage, RegisterPage, ForgotPasswordPage
│       │   ├── admin/                       # AdminDashboardPage, AdminUsersPage, CompanyPoliciesPage
│       │   ├── ai/                          # AiAssistantPage
│       │   ├── attendance/                  # AttendancePage
│       │   ├── dashboard/                   # DashboardPage
│       │   ├── departments/, employees/     # Department and Employee pages
│       │   ├── employee/                    # EmployeeDashboardPage
│       │   ├── hr/                          # HRDashboardPage
│       │   ├── leaves/                      # LeavesPage, MyLeavesPage, LeaveDetailsPage
│       │   ├── profile/, reviews/           # ProfilePage, ReviewsPage
│       │   ├── settings/                    # SettingsPage
│       │   └── tasks/                       # ManagerTasksPage, ManagerTaskDetailPage,
│       │                                    #   EmployeeTasksPage, EmployeeTaskDetailPage
│       ├── routes/                          # AppRoutes, RoleProtectedRoute, PublicRoute
│       ├── services/                        # API service wrappers (per entity)
│       ├── tests/                           # Vitest unit + component tests
│       ├── theme/                           # MUI theme: palette, typography, components
│       └── utils/                           # Date, validation, formatters, calculations
│
├── docker/                                  # Nginx configuration
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env.example
├── .github/workflows/ci.yml
├── jenkins/Jenkinsfile
├── aws/                                     # IAM, ECR, EC2 bootstrap scripts
├── scripts/                                 # deploy.sh, health-check.sh, rollback.sh
└── postman/                                 # Postman collection
```

---

## Authentication & Authorization

### JWT Authentication Flow

1. **Register** → `POST /api/auth/register` → returns JWT + user info
2. **Login** → `POST /api/auth/login` → returns JWT + user info
3. JWT stored in `localStorage` (key: `emp_portal_token`)
4. Axios interceptor attaches `Authorization: Bearer <token>` to every request
5. `401` response → auto-clear storage + redirect to `/login?redirect=<original-path>`
6. Token expiry is checked on every `AuthContext` render cycle

The backend validates the Bearer token on every request via `JwtAuthenticationFilter`, using HS256 signing with a configurable secret (`app.jwt.secret`). Default token expiry is 24 hours; configurable via `app.jwt.expiration-ms`.

### Role Model

| Role | Description |
|---|---|
| `ADMIN` | Full access to all resources and administration functions |
| `HR` | Employee/department management, leave approval, task creation/management |
| `MANAGER` | Leave approval, task creation/assignment/review, attendance visibility |
| `EMPLOYEE` | Self-service: own profile, own leaves, own tasks, own attendance check-in/out |

### Role Permission Matrix

| Resource | ADMIN | HR | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Employees | Full CRUD | Create / Read / Update | Read | Read own record |
| Departments | Full CRUD | Create / Read / Update | Read | Read |
| Leave Requests | All + approve/reject | All + approve/reject | All + approve/reject | Own requests |
| Attendance | All records | All records | All records (read) | Own check-in/out |
| Performance Reviews | Full CRUD | Create / Read / Update | Create / Read / Update | Own reviews (read) |
| Tasks (create/update/delete) | ✅ | ✅ | ✅ | ❌ |
| Tasks (view) | All tasks | All tasks | All tasks | Own assigned tasks |
| Task status update | ✅ | ✅ | ✅ | Own tasks (limited transitions) |
| Task reassign | ✅ | ✅ | ✅ | ❌ |
| Task submission (submit/resubmit) | ✅ | ✅ | ✅ | Own tasks |
| Task submission (approve/request-changes) | ✅ | ✅ | ✅ | ❌ |
| Task attachments (upload/delete) | ✅ | ✅ | ✅ | ❌ |
| Task attachments (download/list) | ✅ | ✅ | ✅ | Own tasks |
| Task dashboard stats / workload | ✅ | ✅ | ✅ | ❌ |
| Notifications | Own only | Own only | Own only | Own only |
| AI Chat | ✅ | ✅ | ✅ | ✅ |
| RAG document ingestion | ✅ | ✅ | ❌ | ❌ |
| Admin endpoints (`/admin/**`) | ✅ | ❌ | ❌ | ❌ |
| Settings (password change) | ✅ | ✅ | ✅ | ✅ |

### Security Implementation

- **`@EnableMethodSecurity`** with `@PreAuthorize` at controller/service level for fine-grained control
- **IDOR protection** — Employees can only access their own tasks, notifications, leave requests, and submissions; enforced in the service layer
- **Stateless sessions** — `SessionCreationPolicy.STATELESS`; no CSRF risk from cookie sessions
- JSON `401 Unauthorized` and `403 Forbidden` responses (not HTML redirects)
- BCrypt password hashing (cost factor 12)
- CORS configured for explicit origin patterns

---

## Core Modules

### Employee Management

- Server-side paginated, sortable employee table
- Search by name, department filter, status filter
- Create / Edit employee (modal dialog, React Hook Form + Zod validation)
- Delete with confirmation dialog (ADMIN only)
- Full employee detail page
- CSV export (current page)
- Responsive: table on desktop, cards on mobile
- EMPLOYEE role can only read their own record (backend-enforced)

### Department Management

- Paginated sortable table with search + sort by name/code/created
- Create / Edit / Delete with dialogs
- Department detail page with employee count statistics
- Embedded employee list per department
- Optimistic updates with dual cache invalidation

### Attendance Management

Attendance statuses: `PRESENT`, `ABSENT`, `HALF_DAY`, `WORK_FROM_HOME`, `ON_LEAVE`

- **Employee self-service**: check-in and check-out via `POST /api/attendance/checkin` and `POST /api/attendance/checkout`
- **Employee history**: own attendance records via `GET /api/attendance/my`
- **Manager/HR visibility**: all employee attendance records
- Attendance history table with pagination
- **Task integration**: an employee's current check-in status is used by the task assignment rules (see below)

### Leave Management

Leave types: `ANNUAL`, `SICK`, `UNPAID`, `MATERNITY`, `PATERNITY`, `BEREAVEMENT`, `EMERGENCY`

**Full leave lifecycle:**

```
PENDING → APPROVED  (by MANAGER, HR, or ADMIN)
        → REJECTED  (by MANAGER, HR, or ADMIN)
        → CANCELLED (by the employee, PENDING only)
```

- Employee self-service leave submission and cancellation
- Approval / rejection workflow with reason
- Calendar view and timeline view per employee
- Leave balance card and leave statistics by type/status
- My Leaves page at `/leaves/my` endpoint
- CSV export

---

## Task Management — Phase 6A

Managers and HR users create and assign tasks to employees. Employees work on their assigned tasks and update status. The backend enforces which fields employees can and cannot modify.

### Task Fields

| Field | Controlled by |
|---|---|
| Title | Manager/HR (create/update) |
| Description | Manager/HR |
| Guidelines | Manager/HR |
| Acceptance Criteria | Manager/HR |
| Priority | Manager/HR |
| Category | Manager/HR |
| Due Date | Manager/HR |
| Estimated Hours | Manager/HR |
| Assigned Employee | Manager/HR (with attendance check) |
| Status | Manager/HR (full control) + Employee (limited transitions) |

**Employees cannot modify manager-controlled fields** (title, description, guidelines, priority, category, due date). They may only update the task status through permitted transitions.

### Task API Endpoints

| Method | Endpoint | Description | Min. Role |
|---|---|---|---|
| GET | `/tasks` | Paginated task list (employees auto-scoped to own) | EMPLOYEE |
| GET | `/tasks/my` | My assigned tasks | EMPLOYEE |
| GET | `/tasks/created` | Tasks created by me | EMPLOYEE |
| GET | `/tasks/{id}` | Task detail (employees: own tasks only) | EMPLOYEE |
| GET | `/tasks/{id}/activities` | Task activity timeline | EMPLOYEE |
| GET | `/tasks/dashboard-stats` | Aggregate task statistics | MANAGER |
| GET | `/tasks/workload-summary` | Per-employee workload summary | MANAGER |
| GET | `/tasks/workload/{employeeId}` | Workload for a specific employee | MANAGER |
| GET | `/tasks/employee-availability` | Checked-in employees + task counts | MANAGER |
| POST | `/tasks` | Create task | MANAGER |
| PUT | `/tasks/{id}` | Update task | MANAGER |
| PATCH | `/tasks/{id}/status` | Update task status | EMPLOYEE |
| POST | `/tasks/{id}/reassign` | Reassign task (new employee must be checked in) | MANAGER |
| DELETE | `/tasks/{id}` | Delete task | MANAGER |

### Attendance-Aware Task Assignment

A manager/HR can **only assign or reassign a task to an employee who is currently checked in**. This rule is enforced server-side — it is not merely a UI restriction.

- The backend returns `409 Conflict` if the target employee is not checked in at the moment of assignment or reassignment.
- Existing tasks are **not** deleted or invalidated when an employee checks out; only new assignments are blocked.
- Employees who are checked out **cannot start a task** (status transition to `IN_PROGRESS` is blocked by the backend).
- The frontend reflects this restriction in the employee selector (showing check-in status) and disables the "Start" button when the employee is not checked in.

### Task Status & Activity Timeline

**Task lifecycle statuses:**

| Status | Description |
|---|---|
| `DRAFT` | Task created but not yet assigned |
| `ASSIGNED` | Task has been assigned to an employee |
| `IN_PROGRESS` | Employee has started working |
| `SUBMITTED` | Employee has submitted work for review |
| `COMPLETED` | Manager has approved the submission |
| `CHANGES_REQUESTED` | Manager has requested changes; employee must resubmit |
| `REJECTED` | Manager has rejected the task |

**Task Activity (`TaskActivity`)** records are automatically created for important events:

| Event Type | Trigger |
|---|---|
| `TASK_ASSIGNED` | Task is assigned to an employee |
| `TASK_STARTED` | Employee transitions task to `IN_PROGRESS` |
| `TASK_STATUS_CHANGED` | Any status transition |
| `TASK_REASSIGNED` | Task is reassigned to a different employee |

Activity records are immutable — users cannot create, modify, or delete them via the API. The full timeline is retrievable at `GET /tasks/{id}/activities`.

### Task Comments

- Any authenticated user can post a comment on a task they have access to.
- Employees may only comment on tasks assigned to them (IDOR enforced in the service layer).
- Managers/HR/Admin may comment on any task they can access.
- When a comment is posted, the **other party is notified** via a `TASK_COMMENT` notification: if the author is the assignee, the task creator is notified, and vice versa.
- The comment history is retrievable at `GET /tasks/{taskId}/comments`.

### Task Categories & Priority

**Categories:**

| Category | Description |
|---|---|
| `DEVELOPMENT` | Software development and feature work |
| `TESTING` | QA, unit testing, integration testing |
| `DOCUMENTATION` | Technical or user-facing docs |
| `DEVOPS` | Infrastructure, CI/CD, deployment |
| `HR` | Human resources and people-management |
| `SUPPORT` | Customer/internal support, bug fixes |
| `RESEARCH` | Research, investigation, PoC work |
| `OTHER` | Catch-all |

**Priority levels (lowest to highest):** `LOW` → `MEDIUM` → `HIGH` → `URGENT`

> **Migration note:** The `CRITICAL` priority value was renamed to `URGENT` in migration V23. Existing `CRITICAL` rows were back-filled to `URGENT`; the old value was then removed from the ENUM.

### Task Attachments

Managers, HR, and Admins can upload reference files to a task. Employees can download those files but cannot upload or delete them.

| Method | Endpoint | Description | Min. Role |
|---|---|---|---|
| GET | `/tasks/{taskId}/attachments` | List attachment metadata | EMPLOYEE |
| POST | `/tasks/{taskId}/attachments` | Upload attachment | MANAGER |
| GET | `/tasks/{taskId}/attachments/{id}/download` | Download file | EMPLOYEE |
| DELETE | `/tasks/{taskId}/attachments/{id}` | Delete attachment | MANAGER |

Files are stored via `FileStorageService` using the key format `tasks/{taskId}/{uuid}.{ext}`. The current implementation stores files on the local filesystem. See [File Storage Architecture](#file-storage-architecture).

---

## Task Submission System — Phase 6B / 6B.1

Employees submit their completed work for manager review. Multiple submission rounds are supported (submit → changes requested → resubmit → approve).

### Submission Lifecycle

```
Employee (IN_PROGRESS task)
    ↓  POST /tasks/{taskId}/submissions
PENDING_REVIEW
    ↓  Manager reviews
    ├─ POST /task-submissions/{id}/approve      → APPROVED  (task → COMPLETED)
    └─ POST /task-submissions/{id}/request-changes → CHANGES_REQUESTED (task → IN_PROGRESS)
                                                          ↓
                                              PUT /task-submissions/{id}/resubmit
                                                          ↓
                                                   PENDING_REVIEW (new round)
```

### Submission Statuses

| Status | Description |
|---|---|
| `PENDING_REVIEW` | Submitted, awaiting manager review |
| `APPROVED` | Manager approved; task transitions to `COMPLETED` |
| `CHANGES_REQUESTED` | Manager requires changes; task reverts to `IN_PROGRESS` |

### Submission API Endpoints

| Method | Endpoint | Description | Min. Role |
|---|---|---|---|
| POST | `/tasks/{taskId}/submissions` | Submit work for review (multipart/form-data) | EMPLOYEE |
| GET | `/tasks/{taskId}/submissions` | List all submissions for a task | EMPLOYEE |
| GET | `/tasks/{taskId}/submissions/latest` | Get most recent submission | EMPLOYEE |
| PUT | `/task-submissions/{id}/resubmit` | Resubmit after changes requested (multipart/form-data) | EMPLOYEE |
| POST | `/task-submissions/{id}/approve` | Approve submission | MANAGER |
| POST | `/task-submissions/{id}/request-changes` | Request changes (comment required) | MANAGER |
| GET | `/task-submissions/{id}/attachment` | Download submission attachment | EMPLOYEE |

### Submission File Attachments (Phase 6B.1)

Employees can optionally attach a single file to their submission.

**Accepted file types:** `PDF`, `CSV`, `DOCX`, `TXT`

**Maximum file size:** 10 MB (default; configurable via `STORAGE_MAX_FILE_SIZE_BYTES`)

- Both extension and MIME type are validated server-side — the client-supplied `Content-Type` is never trusted exclusively.
- Employees can only download their own submission attachments; managers/HR/admin can download any.
- On resubmit, the employee may replace the attachment; omitting the file part preserves the existing attachment.
- Files are stored via the `FileStorageService` abstraction (see [File Storage Architecture](#file-storage-architecture)).

---

## Notifications — Phase 6A.1

### Backend

In-app notifications are created automatically by the system. Notifications are scoped to the recipient — no user can view another user's notifications.

**Notification types implemented:**

| Type | Trigger |
|---|---|
| `TASK_ASSIGNED` | Task assigned to an employee |
| `TASK_STARTED` | Employee starts a task (sent to task creator/manager) |
| `TASK_SUBMITTED` | Employee submits work (sent to manager) |
| `TASK_APPROVED` | Manager approves submission (sent to employee) |
| `TASK_CHANGES_REQUESTED` | Manager requests changes (sent to employee) |
| `TASK_STATUS_CHANGED` | General status change |
| `TASK_DUE_SOON` | Task approaching due date (24h and 2h reminders) |
| `TASK_OVERDUE` | Task is past its due date |
| `TASK_REASSIGNED` | Task reassigned (sent to original and new assignee) |
| `TASK_COMMENT` | New comment posted on a task (sent to the other party) |

**Notification API endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/notifications` | Paginated notification list (own only) |
| GET | `/notifications/unread-count` | Count of unread notifications |
| PATCH | `/notifications/{id}/read` | Mark a single notification as read |
| PATCH | `/notifications/read-all` | Mark all notifications as read |

### Frontend

- **Notification bell** in the top bar with unread count badge
- **Notification dropdown** showing recent notifications with type icons
- **Mark as read** (single or all)
- **Role-aware navigation** — clicking a notification navigates to the correct task detail page based on the authenticated user's role
- **Notification sound** — uses the browser Web Audio API; respects browser autoplay restrictions; persists muted preference to `localStorage`
- **Mute toggle** accessible from the notification bell

---

## Deadline Reminders

A scheduled service (`TaskDeadlineReminderService`) runs **hourly** (`cron = "0 0 * * * *"`) and processes all non-completed tasks that have a due date set.

**Reminder intervals:**

| Reminder | When | Notification Type |
|---|---|---|
| 24-hour reminder | Due date is tomorrow and `reminder24hSent = false` | `TASK_DUE_SOON` |
| 2-hour reminder | Due date is today and ≤ 120 minutes remain (before 17:00) and `reminder2hSent = false` | `TASK_DUE_SOON` |
| Overdue | Due date is in the past and `overdueNotificationSent = false` | `TASK_OVERDUE` |

**Deduplication:** Each reminder is sent at most once per task. Boolean flags (`reminder_24h_sent`, `reminder_2h_sent`, `overdue_notification_sent`) on the `tasks` table prevent duplicate notifications from being generated across repeated hourly runs.

Completed (`COMPLETED`) and rejected (`REJECTED`) tasks are excluded from reminder processing.

---

## Employee Workload Protection

When a manager assigns or reassigns a task, the system provides workload visibility to help prevent overloading an employee.

**Workload information available to MANAGER/HR/ADMIN:**

- **Active task count** — number of `ASSIGNED` or `IN_PROGRESS` tasks per employee
- **Overdue task count** — non-completed tasks past their due date
- **Workload summary** — list of all employees with active and overdue task counts (`GET /tasks/workload-summary`)
- **Per-employee workload** — detail view for a specific employee (`GET /tasks/workload/{employeeId}`)
- **Employee availability** — all active employees with check-in status and active task count (`GET /tasks/employee-availability`)

> Assignment is **warning-only** — the backend does not block assignment at a specific task count threshold. The attendance check-in rule is the only hard enforcement applied at assignment time. Managers are expected to review the workload information before assigning tasks.

---

## Task Dashboard — Phase 6C–6E

A dedicated task analytics dashboard is available to ADMIN, HR, and MANAGER roles.

**Dashboard statistics endpoint:** `GET /tasks/dashboard-stats`

| Metric | Description |
|---|---|
| `totalTasks` | Total number of tasks in the system |
| `countsByStatus` | Task counts grouped by status (e.g., `{ ASSIGNED: 5, IN_PROGRESS: 3, COMPLETED: 12, ... }`) |
| `overdueCount` | Number of non-completed tasks whose due date has passed |
| `urgentCount` | Number of `URGENT`-priority tasks that are not yet completed |
| `completionPercentage` | Percentage of all tasks that are `COMPLETED` (0–100) |

**Frontend task dashboard features:**

- Role-aware task pages: `ManagerTasksPage` (full management view) and `EmployeeTasksPage` (own tasks only)
- Task detail pages: `ManagerTaskDetailPage` and `EmployeeTaskDetailPage`
- Priority and category filter chips
- Status filter
- Employee assignment selector with check-in status and workload indicator
- Activity timeline panel on task detail
- Submission review panel on manager task detail
- Comment thread on task detail

---

## Manager & HR Module

Managers and HR users have privileged access across the task management system:

- **Task creation** — title, description, guidelines, acceptance criteria, priority, category, due date, estimated hours
- **Task assignment** — employee selector shows check-in status and active task count (workload protection)
- **Task monitoring** — view all tasks with filters by status, priority, category, and assigned employee
- **Task update** — edit all task fields
- **Task reassignment** — reassign to another checked-in employee; both parties notified
- **Task deletion** — remove a task
- **Submission review** — approve or request changes on employee submissions
- **Submission attachment download** — download files submitted by employees
- **Task attachment upload/delete** — attach reference files to tasks
- **Activity timeline** — view full audit log of task events
- **Comments** — participate in task discussion threads
- **Workload visibility** — workload summary across all employees
- **Attendance visibility** — view all employee attendance records

HR and MANAGER roles have the same task management permissions. The key distinction is in employee and department management: HR can create/update employees and departments; MANAGER has read-only access to those resources.

---

## AI Assistant / RAG Infrastructure

> **This is existing, implemented infrastructure.** It is not the planned Phase 7 AI Task Analysis module (see [Phase 7](#phase-7--upcoming-ai-task-analysis) below).

The portal includes an **AI HR Assistant** powered by:

- **Groq API** for chat completions (Llama model — `llama-3.1-8b-instant` by default)
- **Hugging Face Inference API** for text embeddings (`nomic-ai/nomic-embed-text-v1.5`, 768-dimensional vectors)
- **Vector/RAG retrieval** — HR knowledge documents are chunked, embedded, and stored in MySQL; relevant chunks are retrieved by cosine similarity search and injected into the assistant's context

**RAG pipeline:**

```
HR document
    ↓
DocumentChunkingService (chunk-size=1000, overlap=150)
    ↓
HuggingFaceEmbeddingService (nomic-ai/nomic-embed-text-v1.5)
    ↓
KnowledgeChunk (stored in MySQL with embedding vector)
    ↓
VectorKnowledgeRetrievalService (cosine similarity, threshold=0.70, top-k=5)
    ↓
RagPromptContextBuilder → system prompt context
    ↓
GroqClient (Llama chat completion)
    ↓
AiChatResponse
```

**RAG API endpoints:**

| Method | Endpoint | Description | Min. Role |
|---|---|---|---|
| POST | `/ai/chat` | Send a chat message to the AI assistant | EMPLOYEE |
| POST | `/ai/rag/documents` | Ingest a knowledge document | HR |
| GET | `/ai/rag/documents` | List knowledge documents | HR |
| DELETE | `/ai/rag/documents/{id}` | Delete a document | ADMIN |
| POST | `/ai/rag/search` | Semantic search over knowledge base | EMPLOYEE |

**Frontend:** `AiAssistantPage` — full chat UI at `/ai/assistant`

**Important security note:** The Groq API key and Hugging Face token are **never exposed to the frontend**. All AI API calls are made server-side.

---

## File Storage Architecture

All file uploads (task submission attachments and task manager attachments) go through the `FileStorageService` abstraction:

```
Application (TaskSubmissionService / TaskAttachmentService)
    ↓
FileStorageService (interface)
    ↓
LocalFileStorageService (current implementation)
    ↓
Local filesystem: {STORAGE_LOCAL_BASE_DIR}/submissions/{submissionId}/{uuid}.{ext}
```

**Path traversal protection** is built into `LocalFileStorageService` — the resolved path is always validated to remain within the base directory before any file operation.

**S3 migration path:** The storage key format (`submissions/{submissionId}/{uuid}.{ext}`) is intentionally compatible with S3 object keys. Switching to cloud storage only requires:

1. Creating an `S3FileStorageService` implementing the `FileStorageService` interface
2. Annotating both implementations with `@ConditionalOnProperty(name="app.storage.provider", havingValue="local"/"s3")`
3. Setting `STORAGE_PROVIDER=s3` in the environment

No task submission business logic needs to change.

---

## Database & Flyway Migrations

### Migration History (V1–V26)

| Version | Description |
|---|---|
| V1 | Initial schema: `roles`, `users`, `departments`, `employees`, `leave_requests`, `attendance`, `performance_reviews`, `employee_role` |
| V2 | Audit columns + indexes on `roles` table |
| V3 | Fix `performance_reviews.rating` column type |
| V4 | Fix `performance_reviews.reviewer_id` column type |
| V5 | Add missing columns and indexes (audit columns, review fields, attendance indexes) |
| V6 | Fix `reviewer_id` type for Hibernate compatibility + review indexes |
| V7 | Seed default users (admin, HR, manager, employee accounts) |
| V8 | Fix seed user role assignments |
| V9 | Seed additional employee account |
| V10 | Backfill employee records |
| V11 | Ensure default department and fix employee records |
| V13 | Add `password_reset_tokens` table |
| V14 | Create `knowledge_documents` and `knowledge_chunks` tables (RAG) |
| V15 | Add `embedding_vector` column to `knowledge_chunks` (MEDIUMBLOB) |
| V16 | Create `tasks` table |
| V17 | Create `notifications` and `task_activities` tables |
| V18 | Fix `notifications.related_task_id` column type |
| V19 | Create `task_submissions` table |
| V20 | Add task submission attachment columns to `task_submissions` |
| V21 | Create `task_comments` table |
| V22 | Add `category` enum column to `tasks` |
| V23 | Rename `CRITICAL` priority to `URGENT` (3-step safe migration) |
| V24 | Add deadline reminder deduplication flag columns to `tasks` |
| V25 | Create `task_attachments` table |
| V26 | Add index on `tasks.category` for filter performance |

> **Latest migration: V26**

Flyway is configured with `repair-on-migrate=true` (removes failed migration entries automatically) and `baseline-on-migrate=true`.

### Local Development

On first startup, Flyway runs all migrations automatically. MySQL must be running and the configured database/user must exist before starting the application.

---

## API Documentation

- **Backend base path:** `/api`
- **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON spec:** `http://localhost:8080/api/v3/api-docs`

### Authorising in Swagger UI

1. Open Swagger UI at `/api/swagger-ui.html`
2. Call `POST /api/auth/login` to obtain a JWT token
3. Click the **Authorize** button (lock icon)
4. Enter `Bearer <your-token>` in the `BearerAuth` field
5. Click **Authorize** — all subsequent Swagger requests will include the token

All protected endpoints are annotated with `@SecurityRequirement(name = "BearerAuth")` and grouped into tagged sections by resource (Tasks, Task Submissions, Task Attachments, Task Comments, Notifications, Dashboard, etc.).

---

## Security

| Protection | Implementation |
|---|---|
| JWT authentication | HS256, validated on every request via `JwtAuthenticationFilter` |
| Spring Security | `SecurityFilterChain` + `@EnableMethodSecurity` |
| Role-based authorization | URL-level rules + `@PreAuthorize` at controller/service level |
| IDOR protection | Employee-scoped service queries; ownership checked before resource access |
| Attendance enforcement | Server-side check at task assignment/start — not a UI-only restriction |
| Path traversal protection | `LocalFileStorageService.resolveAndValidate()` ensures path stays within base dir |
| Filename sanitization | Only the basename is stored; path separators in filenames are rejected |
| MIME type validation | Extension and MIME type both validated; client `Content-Type` is not trusted alone |
| File size limit | Configurable maximum enforced server-side (default 10 MB) |
| API key protection | Groq API key and HF token are server-side only; never exposed to frontend |
| Swagger BearerAuth | All protected endpoints require `Authorization: Bearer <token>` |
| BCrypt | Password hashing with cost factor 12 |
| Stateless sessions | No server-side session; CSRF not applicable |
| JSON error responses | All 401/403 responses return structured JSON, not HTML redirects |

---

## Testing

### Backend Tests

```bash
cd backend
.\mvnw.cmd test               # Windows
./mvnw test                   # macOS/Linux
mvn clean verify              # Full build + test (CI command)
```

The backend test suite spans **multiple test classes** using JUnit 5 + Mockito for unit/slice tests and Testcontainers (MySQL 8) for integration tests. Test classes include:

- `JwtServiceTest` — JWT token lifecycle
- `AuthServiceTest` — login/register service logic
- `EmployeeServiceTest` — CRUD + ownership checks
- `EmployeeControllerTest` — HTTP layer
- `DashboardControllerTest` — dashboard HTTP layer
- `GlobalExceptionHandlerTest` — error response format
- `RbacSecurityTest` — RBAC enforcement (38 cases)
- `EmployeeRbacTest` — employee-specific RBAC
- `ApiDocumentationTest` — OpenAPI + error responses
- `SettingsServiceTest` / `SettingsControllerTest` — password change
- `ReviewServiceTest` / `ReviewControllerTest` — performance reviews
- `TaskControllerTest` — task management HTTP layer
- `TaskSubmissionControllerTest` — submission HTTP layer
- `NotificationControllerTest` — notification HTTP layer
- `AttendanceControllerTest` — attendance HTTP layer
- `AuthControllerTest` — auth HTTP layer
- `ProfileControllerTest` — profile HTTP layer
- `PasswordResetControllerTest` — password reset HTTP layer
- `NotificationServiceTest` — notification service logic
- `TaskServiceTest` — task service logic
- `TaskSubmissionServiceTest` — submission service logic
- `TaskAttachmentServiceTest` — attachment service logic
- `TaskCommentServiceTest` — comment service logic
- `TaskDeadlineReminderServiceTest` — reminder scheduler logic
- `FileValidationServiceTest` — file validation logic
- `PasswordResetServiceTest` — password reset service logic
- `AiChatControllerTest` / `AiChatServiceTest` — AI controller + service
- `GroqClientTest` — Groq API client
- `AiChatSecurityTest` — AI endpoint security
- `KnowledgeControllerTest` — RAG knowledge controller
- `HuggingFaceEmbeddingServiceTest` — embedding service
- `VectorSimilarityTest` — cosine similarity calculation
- `DatabaseKnowledgeRetrievalServiceTest` — keyword retrieval
- `DocumentChunkingServiceTest` — document chunking
- `KnowledgeIngestionServiceTest` — ingestion pipeline
- `RagPromptContextBuilderTest` — prompt builder
- `VectorKnowledgeRetrievalServiceTest` — vector retrieval
- `AuditingIntegrationTest` — JPA audit field population (`@DataJpaTest` + H2)
- `PersistenceRepositoryTest` — JPA repository layer (`@DataJpaTest` + H2)
- `EmployeeManagementIntegrationTest` — full-stack Testcontainers

> **Note:** Tests marked `@DataJpaTest` and Testcontainers integration tests require a running Docker daemon with MySQL 8 access. All non-environment tests pass without Docker.

Phase 6C–6E was successfully tested. The full test suite was verified to pass in the CI pipeline.

### Frontend Tests

```bash
cd frontend
npm test                  # Single run (vitest run)
npm run test:watch        # Watch mode
npm run test:coverage     # Generate v8 coverage report
```

**Vitest** + **React Testing Library** + **jsdom**. Test files in `src/tests/`:

| Test File | Coverage Area |
|---|---|
| `AuthContext.test.jsx` | Auth context state and token management |
| `AuthFlow.test.jsx` | Authentication flow integration |
| `LoginPage.test.jsx` / `RegisterPage.test.jsx` | Form validation and submission |
| `useDashboard.test.jsx` / `DashboardPage.test.jsx` | Dashboard hooks and page states |
| `dashboardFormatters.test.js` | Dashboard formatting utilities |
| `useEmployees.test.jsx` / `EmployeesPage.test.jsx` / `EmployeeTable.test.jsx` / `EmployeeForm.test.jsx` | Employee module |
| `employeeFormatters.test.js` | Employee formatting utilities |
| `useDepartmentHooks.test.jsx` / `DepartmentsPage.test.jsx` / `DepartmentTable.test.jsx` / `DepartmentForm.test.jsx` | Department module |
| `departmentFormatters.test.js` | Department formatting utilities |
| `useLeaveHooks.test.jsx` / `LeavesPage.test.jsx` / `LeaveTable.test.jsx` / `LeaveForm.test.jsx` | Leave module |
| `leaveCalculations.test.js` | Leave day calculation logic |
| `leaveFormatters.test.js` | Leave formatting utilities |
| `ProfilePage.test.jsx` | Profile page |
| `AiAssistantChat.test.jsx` | AI chat component |
| `CompanyPolicyForm.test.jsx` / `CompanyPolicyList.test.jsx` | Company policy management |
| `ForgotPasswordPage.test.jsx` | Password reset page |
| `NotificationSound.test.jsx` | Notification sound hook |
| `notificationApi.test.js` | Notification API service |
| `knowledgeApi.test.js` | RAG knowledge API service |
| `taskApi.test.js` | Task API service |
| `SubmissionForm.test.jsx` / `SubmissionReview.test.jsx` | Task submission components |
| `TaskActivityTimeline.test.jsx` | Activity timeline component |
| `TaskAttachments.test.jsx` | Task attachments component |
| `TaskChips.test.jsx` / `TaskPriorityChip.test.jsx` | Task chip components |
| `TaskComments.test.jsx` | Task comments component |
| `TaskForm.test.jsx` | Task creation/edit form |
| `EmployeeAvailabilitySelector.test.jsx` | Employee availability selector |
| `axiosInstance.test.js` | HTTP client interceptors |

Phase 6C–6E frontend features are covered by the task-related test files above.

---

## Build & Run Instructions

### Backend (without Docker)

Requires a running MySQL 8 instance on `localhost:3306` with the configured database and user.

```bash
cd backend

# Windows
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`

### Frontend (without Docker)

```bash
cd frontend
npm install
npm run dev
```

- App: `http://localhost:5173`
- Vite proxies `/api/*` → `http://localhost:8080` (configured in `vite.config.js`)

### Running Tests

```bash
# Backend
cd backend
.\mvnw.cmd test        # Windows
./mvnw test            # macOS/Linux

# Frontend
cd frontend
npm test
```

---

## Environment Variables

Copy `.env.example` to `.env` (gitignored — never commit it).

```bash
cp .env.example .env
```

| Variable | Required | Default | Description |
|---|---|---|---|
| `MYSQL_ROOT_PASSWORD` | Yes (Docker) | — | MySQL root password (container init only) |
| `DB_NAME` | No | `emp_portal` | Database name |
| `DB_USER` | No | `emp_user` | App database username |
| `DB_PASSWORD` | Yes | — | App database password |
| `JWT_SECRET` | Yes | — | HS256 signing key (≥ 32 chars). Generate: `openssl rand -base64 48` |
| `CORS_ORIGINS` | No | `http://localhost:5173` | Allowed CORS origins (comma-separated) |
| `GROQ_API_KEY` | Yes (AI) | — | Groq API key for chat completions. **Never commit.** |
| `GROQ_MODEL` | No | `llama-3.1-8b-instant` | Groq model ID |
| `HF_TOKEN` | Yes (RAG) | — | Hugging Face access token for embeddings. **Never commit.** |
| `RAG_EMBEDDING_MODEL` | No | `nomic-ai/nomic-embed-text-v1.5` | HF embedding model ID |
| `RAG_ENABLED` | No | `true` | Enable/disable the RAG knowledge base |
| `RAG_TOP_K` | No | `5` | Max knowledge chunks retrieved per AI request |
| `RAG_RETRIEVAL_STRATEGY` | No | `vector` | `vector` (semantic) or `database` (keyword) |
| `RAG_SIMILARITY_THRESHOLD` | No | `0.70` | Minimum cosine similarity for chunk inclusion |
| `STORAGE_PROVIDER` | No | `local` | File storage provider (`local`; `s3` when implemented) |
| `STORAGE_LOCAL_BASE_DIR` | No | `~/emp-portal/uploads/submissions` | Absolute path for local file storage |
| `STORAGE_MAX_FILE_SIZE_BYTES` | No | `10485760` (10 MB) | Maximum upload file size in bytes |
| `MAIL_HOST` | No | `sandbox.smtp.mailtrap.io` | SMTP host for email (password reset) |
| `MAIL_PORT` | No | `2525` | SMTP port |
| `MAIL_USERNAME` | No | — | SMTP username |
| `MAIL_PASSWORD` | No | — | SMTP password |
| `MAIL_FROM` | No | `noreply@company.local` | From address for system emails |

---

## Docker Setup

### Prerequisites

| Tool | Minimum version |
|---|---|
| Docker Desktop | 24+ |
| Docker Compose v2 | Built into Docker Desktop |

### Quick Start

```bash
# 1. Copy the environment template and fill in secrets
cp .env.example .env
#    Edit .env — set DB_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET, GROQ_API_KEY, HF_TOKEN

# 2. Build images and start all services
docker compose up -d --build

# 3. Watch Flyway migrations on first boot
docker compose logs -f backend

# 4. Verify all services are healthy
docker compose ps
```

### Application URLs (Docker)

| URL | Description |
|---|---|
| `http://localhost` | React SPA |
| `http://localhost/api` | Spring Boot REST API |
| `http://localhost/api/swagger-ui.html` | Swagger UI |
| `http://localhost/api/v3/api-docs` | OpenAPI JSON |
| `http://localhost/api/actuator/health` | Backend health |

### Managing the Stack

```bash
docker compose down              # Stop (data preserved)
docker compose down -v           # ⚠ Stop AND delete all data (removes mysql_data volume)
docker compose up -d --build     # Rebuild after code changes
docker compose logs -f backend   # Stream backend logs
docker compose ps                # Check status
```

---

## NPM Scripts

```bash
npm run dev           # Start Vite dev server (http://localhost:5173)
npm run build         # Production build → dist/
npm run preview       # Preview production build locally
npm run lint          # ESLint (0 warnings policy)
npm run lint:fix      # ESLint with auto-fix
npm run format        # Prettier format all src files
npm run test          # Run all tests once (Vitest)
npm run test:watch    # Run tests in watch mode
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

# Windows Maven wrapper
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd clean package
```

---

## Phase 7 — Upcoming: AI Task Analysis

> ⚠️ **Phase 7 is NOT yet implemented.** The following describes planned, future capability only.

Phase 7 will extend the existing AI/RAG infrastructure to provide automated analysis of employee task submissions for manager-facing insights.

**Planned capabilities:**

- AI-powered analysis of employee task submissions
- Completion-quality assessment against task guidelines and acceptance criteria
- Comparison of submitted work against task description, guidelines, and acceptance criteria
- Suggested modifications and improvement recommendations
- Manager-facing AI review report displayed alongside the submission
- Submission quality scoring
- Grounded analysis using task description, guidelines, submission content, comments, and activity history
- Document content extraction from PDF, DOCX, TXT, and CSV attachments where appropriate

This module will build on the existing `FileStorageService` (for submission file access), `VectorKnowledgeRetrievalService` (for context retrieval), `GroqClient` (for LLM analysis), and the `TaskSubmission` domain model — none of which need to be modified.

---

## Screenshots

> *Screenshots will be updated after UI stabilisation.*

| Screen | Description |
|---|---|
| Login | `docs/screenshots/login.png` |
| Dashboard (Admin) | `docs/screenshots/dashboard-admin.png` |
| Employee List | `docs/screenshots/employees.png` |
| Employee Details | `docs/screenshots/employee-detail.png` |
| Departments | `docs/screenshots/departments.png` |
| Leave Management | `docs/screenshots/leaves.png` |
| Task List (Manager) | `docs/screenshots/manager-tasks.png` |
| Task Detail (Employee) | `docs/screenshots/employee-task-detail.png` |
| Task Submission | `docs/screenshots/task-submission.png` |
| Notification Bell | `docs/screenshots/notifications.png` |
| AI Assistant | `docs/screenshots/ai-assistant.png` |

---

## License

*This project is licensed under the MIT License. See `LICENSE` for details.*

---

## Author

**Enterprise Engineering Team**

Built with ☕ Java and ⚛️ React.
