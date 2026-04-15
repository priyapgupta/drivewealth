# ConfigFlow POC

## What We Are Trying to Accomplish

ConfigFlow is a **centralized feature flag and configuration management service**. The goal is to let engineering teams **release features safely and gradually** — without redeploying code.

In a traditional deployment, enabling a new feature means shipping a new build. That's slow and risky. With ConfigFlow:

- A feature starts hidden (`rolloutPercent = 0`)
- It can be enabled for a small slice of real users (`rolloutPercent = 5`) to test in production
- If stable, the rollout widens (`25` → `50` → `100`)
- If something breaks, it is killed instantly by setting rollout back to `0` — no redeploy needed

Each change is tracked in an immutable audit log so you always know who changed what and when.

**The rollout decision is deterministic:** a given user always lands in the same bucket regardless of how many times the flag is evaluated. This is done by hashing `userId + flagKey` with CRC32 and checking if the result falls within the rollout percentage. A user who sees a feature at 10% rollout will still see it at 20% (their bucket doesn't change).

---

## How to Build and Run

The **React dashboard** is packaged as static files served at `/`. A normal **Maven build** runs `npm install` and `npm run build` in `frontend/` (via `frontend-maven-plugin`) unless you skip it.

### Prerequisites

- **Docker:** Docker Desktop, port `8080` free
- **Local JAR:** Java 21, Maven 3.9+
- **Optional (Vite dev server):** Node.js 20+ and npm

### Run with Docker (recommended)

```powershell
# From the project root
docker compose up --build
```

Wait until the logs show `Started ConfigFlowApplication`. Then open:

- **Dashboard:** [http://localhost:8080/](http://localhost:8080/)
- **JSON health:** [http://localhost:8080/api/health](http://localhost:8080/api/health)

### Frontend-only dev (Vite)

Use this when you’re actively developing the React UI and want hot reload.

Prereqs:
- Backend is running on `http://localhost:8080` (Docker or `mvn spring-boot:run`)

```powershell
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

Notes:
- The Vite dev server proxies `/api`, `/admin`, and `/h2-console` to `http://localhost:8080` (see `frontend/vite.config.ts`).
- If your backend is not on `8080`, update the proxy target in `frontend/vite.config.ts` (or set `VITE_API_BASE`).

### Run locally (single JAR)

```powershell
mvn package -DskipTests
java -jar target/configflow-poc-0.1.0.jar
```

Backend-only package (no dashboard in the JAR):

```powershell
mvn package -DskipTests -Dskip.npm=true
```

### Local dev (hot reload)

**Terminal 1 — API:**

```powershell
mvn spring-boot:run
```

**Terminal 2 — Vite (proxies `/admin` and `/api` to port 8080):**

```powershell
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

### Stop Docker

```powershell
docker compose down
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | React dashboard (static UI) |
| GET | `/api/health` | JSON health / service info |
| GET | `/admin/configs` | List all config entries |
| GET | `/admin/config/{key}` | Get a single config by key |
| POST | `/admin/config/{key}` | Create or update a config (upsert) |
| GET | `/admin/audit` | View full audit trail |
| GET | `/api/demo?userId={id}&feature={key}` | Check if a feature is enabled for a user (defaults to `feature.newCheckout`) |
| GET | `/api/features?userId={id}` | List all feature flags with enabled/disabled for a user |

---

## Test Data & Curl Examples

The app seeds 4 config entries on startup via `data.sql`. You can verify them and create more using the commands below.

### Health Check

```powershell
curl http://localhost:8080/api/health
```

### View All Seeded Configs

```powershell
curl http://localhost:8080/admin/configs
```

Expected — 4 entries:

| Key | Value | Rollout |
|-----|-------|---------|
| `feature.newCheckout` | `true` | 10% |
| `payments.payments.prod.timeoutMs` | `5000` | 100% |
| `retail.website.staging.experiment.signupColor` | `blue` | 25% |
| `loans.backend.feature.fastPath` | `false` | 0% |

### Create a New Feature Flag

```powershell
curl -X POST http://localhost:8080/admin/config/feature.newCheckout `
  -H "Content-Type: application/json" `
  -d '{"value":"true","rolloutPercent":0,"updatedBy":"tester"}'
```

### Gradually Roll Out

```powershell
# Enable for 20% of users
curl -X POST http://localhost:8080/admin/config/feature.newCheckout `
  -H "Content-Type: application/json" `
  -d '{"value":"true","rolloutPercent":20,"updatedBy":"tester"}'

# Widen to 100%
curl -X POST http://localhost:8080/admin/config/feature.newCheckout `
  -H "Content-Type: application/json" `
  -d '{"value":"true","rolloutPercent":100,"updatedBy":"tester"}'

# Kill the feature (instant rollback)
curl -X POST http://localhost:8080/admin/config/feature.newCheckout `
  -H "Content-Type: application/json" `
  -d '{"value":"true","rolloutPercent":0,"updatedBy":"tester"}'
```

### Test the Feature Flag Per User

```powershell
# Same userId always gets the same result (defaults to feature.newCheckout)
curl "http://localhost:8080/api/demo?userId=alice"
curl "http://localhost:8080/api/demo?userId=bob"
curl "http://localhost:8080/api/demo?userId=charlie"

# Evaluate any feature key (must contain 'feature')
curl "http://localhost:8080/api/demo?userId=alice&feature=feature.checkout1"

# Get the full list of feature flags for a user
curl "http://localhost:8080/api/features?userId=alice"
```

Example response:
```json
{
  "userId": "alice",
  "feature": "feature.newCheckout",
  "enabled": true,
  "result": "NEW checkout flow"
}
```

### Get a Single Config

```powershell
curl http://localhost:8080/admin/config/feature.newCheckout
```

### View Audit Trail

```powershell
curl http://localhost:8080/admin/audit
```

---

## Design Notes

- **Deterministic rollout:** CRC32 hash of `userId|flagKey` % 100 ensures consistent per-user bucketing
- **Audit log:** Every upsert writes an immutable row to `AUDIT_ENTRY` — full history preserved
- **H2 file-based DB:** Data persists across restarts inside the container at `./data/configflow`
- **Limitations:** No authentication, no multivariate targeting; intended as a POC

