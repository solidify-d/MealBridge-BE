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

## 브랜치

- `feature/msa-scaffold` — MSA 빈 껍데기 초기 세팅

## 후속 작업

- MySQL / Redis / Kafka 연동
- OpenFeign, JWT, 도메인 API
- Docker Compose
