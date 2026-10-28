# CodeReview API

A REST API for managing code review requests. Developers create and track requests, reviewers claim requests and record comments or decisions, and administrators manage user roles.

## Features

- User registration and JWT authentication
- Role based access for developers, reviewers, and administrators
- Review request creation, listing, filtering, pagination, updates, and cancellation
- Exclusive reviewer assignment with database locking
- Reviewer comments, decisions, and immutable status history
- Paginated administrative user listing and reviewer promotion
- OpenAPI documentation with Swagger UI
- React and TypeScript web interface for developers, reviewers, and administrators

## Technology

- Java 21 and Spring Boot
- Spring Security and JWT
- Spring Data JPA and PostgreSQL
- Maven
- Docker Compose
- React, TypeScript, Vite, and React Router

## Run with Docker Compose

1. Copy `.env.example` to `.env`.
2. Replace the database and pgAdmin passwords with local values. Keep `.env` out of Git.
3. Keep `APP_BOOTSTRAP_ADMIN_ENABLED=false` unless you need to create the first administrator. To bootstrap one, set it to `true` and provide a strong name, email, and password in `.env`. The administrator is created only when the database has no `ADMIN` user.
4. Start the services:

```bash
docker compose up --build
```

The web interface is available at `http://localhost:3000`. The API is available at `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`; the OpenAPI document is at `http://localhost:8080/v3/api-docs`.

To stop the services while preserving PostgreSQL data:

```bash
docker compose down
```

To delete the database volume as well, use `docker compose down -v`. This permanently removes local database data.

## Run the API locally

Start PostgreSQL with Docker Compose, then run the Spring Boot application with Java 21 and Maven:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell, use:

```powershell
.\mvnw.cmd spring-boot:run
```

The application requires `DB_PASSWORD` and `JWT_SECRET`; it has no built-in credential defaults. `JWT_SECRET` must be Base64 and decode to at least 32 bytes. The sample key in `.env.example` is for disposable local development only; use a separately generated key outside that environment.

For PowerShell, load your local `.env` values and map the PostgreSQL variables before running the API or its tests:

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), 'Process')
}
$env:DB_URL = "jdbc:postgresql://localhost:5432/$env:POSTGRES_DB"
$env:DB_USERNAME = $env:POSTGRES_USER
$env:DB_PASSWORD = $env:POSTGRES_PASSWORD
```

For Bash, export the `.env` values and map the database variables in the current shell:

```bash
set -a
source .env
set +a
export DB_URL="jdbc:postgresql://localhost:5432/$POSTGRES_DB"
export DB_USERNAME="$POSTGRES_USER"
export DB_PASSWORD="$POSTGRES_PASSWORD"
```

The default local database URL is `jdbc:postgresql://localhost:5432/codereview`; override `DB_URL` if your PostgreSQL instance uses a different host or port. Docker Compose passes the database and JWT settings directly to the API container.

## Run the frontend locally

Install Node.js 22.12 or newer and pnpm 11. Then start the API and PostgreSQL as described above. In another terminal:

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

Vite serves the interface at `http://localhost:5173` and proxies `/api` calls to `http://localhost:8080`. In Compose, Nginx serves the built frontend on port `3000` and forwards API requests to the backend container.

The JWT stays in browser memory and is cleared when the page is reloaded or the user signs out. This avoids persistent browser storage and cookie-based CSRF exposure; a page refresh requires the user to log in again. The UI uses the role claim only to show relevant navigation. The backend still checks roles and resource ownership on every protected operation.

## Run tests

Tests use PostgreSQL. Start the database first, then run:

```bash
./mvnw test
```

The GitHub Actions workflow builds and tests the application against PostgreSQL, builds the frontend, and validates both Docker images.

## Main API endpoints

| Method | Endpoint | Access |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Public; creates a `DEVELOPER` |
| `POST` | `/api/auth/login` | Public; returns a Bearer JWT |
| `POST` | `/api/reviews` | `DEVELOPER` |
| `GET` | `/api/reviews?page=0&size=10&status=OPEN` | Authenticated, results scoped by role and ownership |
| `GET` | `/api/reviews/{id}` | Authorized request participants |
| `PATCH` | `/api/reviews/{id}` | Request owner while status is `OPEN` |
| `PATCH` | `/api/reviews/{id}/cancel` | Request owner while cancellation is allowed |
| `PATCH` | `/api/reviews/{id}/claim` | `REVIEWER` |
| `POST` | `/api/reviews/{id}/comments` | Assigned reviewer |
| `POST` | `/api/reviews/{id}/decision` | Assigned reviewer |
| `GET` | `/api/reviews/{id}/history` | Authorized request participants |
| `GET` | `/api/admin/users?page=0&size=10` | `ADMIN` |
| `PATCH` | `/api/admin/users/{userId}/role` | `ADMIN`; promotes a developer to reviewer |

For protected endpoints, log in and send the returned token as:

```http
Authorization: Bearer <token>
```

Example registration, login, and review request creation:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alex Developer","email":"alex@example.com","password":"SafePassword123!"}'

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alex@example.com","password":"SafePassword123!"}' | jq -r '.token')

curl -X POST http://localhost:8080/api/reviews \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Review login flow","description":"Please review token validation.","repositoryUrl":"https://github.com/example/app","pullRequestUrl":"https://github.com/example/app/pull/12"}'
```

The registration endpoint always assigns `DEVELOPER`; it does not accept a role from the client. The bootstrap configuration is the supported way to create the first administrator in a fresh database. After startup, disable the bootstrap setting and remove the bootstrap password from the local environment file.

## Review status flow

- `OPEN` can move to `IN_REVIEW` or `CANCELLED`.
- `IN_REVIEW` can move to `APPROVED`, `CHANGES_REQUESTED`, or `REJECTED`.
- `CHANGES_REQUESTED` can move back to `IN_REVIEW` or to `CANCELLED`.
- `APPROVED`, `REJECTED`, and `CANCELLED` are final in this version.

Each status change is stored in the status history in the same database transaction as the request update. Only one reviewer can claim an open request; a competing claim receives a conflict response.

## Configuration

| Variable | Purpose |
| --- | --- |
| `POSTGRES_DB` | PostgreSQL database name |
| `POSTGRES_USER` | PostgreSQL username |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Base64 encoded HMAC key (at least 32 decoded bytes) |
| `APP_BOOTSTRAP_ADMIN_ENABLED` | Enables first administrator creation when set to `true` |
| `APP_BOOTSTRAP_ADMIN_NAME` | Initial administrator name |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | Initial administrator email |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Initial administrator password; encoded with BCrypt |

Do not commit `.env` files, real credentials, or JWT secrets. The checked-in `.env.example` contains development-only placeholders.
