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
2. **01 Auth / Login — SUPER_ADMIN** (or role you need)
3. Role-specific folder (02–04)
4. **05 Negative** — 401/403 checks

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
