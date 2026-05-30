# MealBridge-BE

MealBridge 백엔드 MSA — Spring Boot 3.4 + Spring Cloud 2024.0.0 + Eureka + Gateway.

## 모듈 구조

| 모듈 | 포트 | 역할 |
|------|------|------|
| `eureka` | 8761 (고정) | 서비스 디스커버리 |
| `apigateway` | 8080 (고정) | API Gateway, CORS, 라우팅 |
| `user-service` | 0 (랜덤) | 인증·RBAC·조직 프로필 |
| `donation-service` | 0 (랜덤) | 잔식 등록·기부·증빙·ESG |
| `matching-service` | 0 (랜덤) | 매칭·수거·알림 |
| `ai-service` | 0 (랜덤) | 푸드스캔·영양·예측 |

비즈니스 서비스 4개는 `server.port: 0`으로 기동 시 OS가 빈 포트를 할당하고, **Eureka에 실제 포트가 등록**됩니다. Gateway는 `lb://서비스명`으로 조회하므로 고정 포트가 필요 없습니다.

직접 접속이 필요하면 Eureka 대시보드(http://localhost:8761) 또는 기동 로그에서 할당 포트를 확인하세요.

각 모듈은 **독립 Gradle 프로젝트** (`be16-fin-3team-NylonMusk-BE`와 동일 패턴).

## 로컬 설정 (NylonMusk 패턴)

`application-local.yml`은 **gitignore** 됩니다. 클론 후 한 번만 설정하세요.

**한 번에 생성 (Git Bash):**

```bash
bash scripts/setup-local-config.sh
```

**또는 모듈별 수동:**

```bash
cp apigateway/src/main/resources/application-local.yml.example apigateway/src/main/resources/application-local.yml
cp user-service/src/main/resources/application-local.yml.example user-service/src/main/resources/application-local.yml
# donation-service, matching-service, ai-service 동일
```

`eureka`는 `application.yml`만 사용합니다 (local 프로필 파일 없음).

## 요구 사항

- JDK 17+

## 빌드 (모듈별)

```bash
cd eureka && ./gradlew build
cd ../apigateway && ./gradlew build
cd ../user-service && ./gradlew build
# ... donation-service, matching-service, ai-service 동일
```

Windows: `gradlew.bat build`

## 로컬 실행 순서

1. `eureka` (8761)
2. `user-service`, `donation-service`, `matching-service`, `ai-service`
3. `apigateway` (8080)

Gateway 경유 health 예:

- http://localhost:8080/user-service/health
- http://localhost:8080/donation-service/health

FE dev server: http://127.0.0.1:3000 (Gateway CORS 허용)

## Postman

[`postman/MealBridge-Local.postman_collection.json`](postman/MealBridge-Local.postman_collection.json) + [`postman/MealBridge-Local.postman_environment.json`](postman/MealBridge-Local.postman_environment.json) — Gateway 경유 API 테스트 (로그인·역할별·CSRF). See [`postman/README.md`](postman/README.md).

## 인프라 (로컬)

```bash
docker compose up -d
bash scripts/setup-local-config.sh --sync
```

- MySQL: `localhost:3310` (host) → container `3306`, user/pass `mealbridge` / `mealbridge`
- Redis: `localhost:6379` (`user-service` RT 저장용)

### 서비스별 MySQL 스키마 (database)

| 서비스 | JDBC database |
|--------|----------------|
| `user-service` | `mealbridge_user` |
| `donation-service` | `mealbridge_donation` |
| `matching-service` | `mealbridge_matching` |
| `ai-service` | `mealbridge_ai` |

최초 기동 시 [`docker/mysql/init/01-schemas.sql`](docker/mysql/init/01-schemas.sql)가 4개 DB를 생성합니다.

**이미 예전 `mealbridge` 단일 DB로 컨테이너를 띄운 적이 있으면** init이 다시 안 돌아갑니다:

```bash
docker compose down -v
docker compose up -d
```

## 인증 (JWT + Gateway 헤더)

NylonMusk-BE 패턴: Gateway가 쿠키 `AT`를 검증하고 downstream에 **`X-User-Id`**, **`X-User-Role`** 헤더를 주입합니다.

| Role | 설명 |
|------|------|
| `ORGANIZATION` | 단체 (받는 쪽) |
| `DONOR` | 잔식 제공 기관 (주는 쪽 — 군부대·학교·관공서 등 확장) |
| `SUPER_ADMIN` | 총 관리자 (하위 `ADMIN` 계정 CRUD) |
| `ADMIN` | 하위 관리자 |

### API (Gateway 경유)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/user-service/auth/login` | 로그인 → `AT`/`RT`/`CSRF-TOKEN` 쿠키 |
| POST | `/user-service/auth/refresh` | AT 재발급 |
| POST | `/user-service/auth/logout` | 로그아웃 |
| GET | `/user-service/admin/users` | 하위 관리자 목록 (`SUPER_ADMIN`) |
| POST | `/user-service/admin/users` | 하위 관리자 생성 (`SUPER_ADMIN`) |

**FE 요청 시**

- `credentials: 'include'` (쿠키 전송)
- POST/PUT/PATCH/DELETE: 헤더 `X-CSRF-Token` = 쿠키 `CSRF-TOKEN` 값

### 시드 계정 (`mealbridge.auth.seed-enabled=true`)

| Email | Role | Password |
|-------|------|----------|
| `super@mealbridge.local` | SUPER_ADMIN | `mealbridge123!` |
| `admin@mealbridge.local` | ADMIN | `mealbridge123!` |
| `org@mealbridge.local` | ORGANIZATION | `mealbridge123!` |
| `donor@mealbridge.local` | DONOR | `mealbridge123!` |

## 브랜치

- `develop` — MSA scaffold merge 완료
- `feature/gateway-jwt-auth` — JWT 인증·RBAC

## 후속 작업

- Kafka, OpenFeign 도메인 연동
- 회원가입·승인 워크플로
- donation/matching/ai 서비스 전면 헤더·Security 적용
