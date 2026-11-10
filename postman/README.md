# MealBridge Postman

## Import

1. Postman → **Import** → select:
   - `MealBridge-Local.postman_collection.json`
   - `MealBridge-Local.postman_environment.json`
2. Environment: **MealBridge Local** 선택
3. Settings → turn on **Automatically follow redirects** (default OK)
4. Cookies: Postman stores `AT` / `RT` / `CSRF-TOKEN` per domain `localhost` after login

## Prerequisites

```bash
docker compose up -d
# Eureka → user-service → donation-service → matching-service → ai-service → apigateway
```

Gateway: `http://localhost:8080` (`baseUrl` in environment)

## Recommended flow

1. **00 Health** — Gateway + services UP 확인
2. **01 Auth** — Register (optional) → **Login — SUPER_ADMIN** to approve → role login
3. **02 Super Admin** — pending registrations / approve / sub-admin CRUD
4. Role-specific folder (03–04)
5. **05 Negative** — 401/403 checks

### Signup → approval

1. **Register — Organization** or **Register — Donor** (201, `PENDING`; saves `pendingUserId`)
2. Try login as that user → should fail until approved
3. **Login — SUPER_ADMIN** → **Approve Registration**
4. Login as the new user → 200 + cookies

## Response JSON

All APIs return NylonMusk community-style envelope:

| Field | Example |
|-------|---------|
| `status_code` | `200`, `401`, `404` |
| `status_message` | `"Login successful"` |
| `result` | payload object or `null` |

Tests use `json.result.*` (not `statusCode`).

## CSRF

Collection **pre-request script** adds `X-CSRF-Token` on POST/PATCH/PUT/DELETE (except `/auth/**`).

Resolution order: env `csrfToken` → cookie `CSRF-TOKEN` at `baseUrl` → cookie fallback. **Login — SUPER_ADMIN** saves `csrfToken` to the environment.

| Symptom | Cause |
|---------|--------|
| `403 Invalid CSRF token` on PATCH | No login, or `X-CSRF-Token` header missing / mismatched cookie |
| `400 Invalid userId` | CSRF passed; path UUID is malformed (`not-a-uuid`) |
| `405 Method Not Allowed` | CSRF passed; empty `pendingUserId` → URL `/registrations/approve` (no handler) |

**Approve Registration** throws in pre-request if `pendingUserId` is empty — run **Register — Organization** or set env manually.

Negative checks: folder **06 Negative (CSRF & path)**.

If CSRF 403 persists: login again, re-import collection/environment, check Postman Cookies for `localhost`.

## Seed accounts

| Email | Role |
|-------|------|
| `super@mealbridge.local` | SUPER_ADMIN |
| `admin@mealbridge.local` | ADMIN |
| `org@mealbridge.local` | ORGANIZATION |
| `donor@mealbridge.local` | DONOR |

Password: `mealbridge123!` (env var `password`)
