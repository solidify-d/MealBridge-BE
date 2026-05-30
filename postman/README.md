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

## CSRF

Collection **pre-request script** adds `X-CSRF-Token` from cookie `CSRF-TOKEN` on POST/PATCH/PUT/DELETE (except `/auth/**`).

If CSRF 403: login again to refresh `CSRF-TOKEN` cookie.

## Seed accounts

| Email | Role |
|-------|------|
| `super@mealbridge.local` | SUPER_ADMIN |
| `admin@mealbridge.local` | ADMIN |
| `org@mealbridge.local` | ORGANIZATION |
| `donor@mealbridge.local` | DONOR |

Password: `mealbridge123!` (env var `password`)
