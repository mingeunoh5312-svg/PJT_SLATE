# `.env.example` 정책

## 목적

두 작업자가 같은 변수명을 사용하되 실제 값은 각자 로컬 또는 배포 환경에서만 관리하도록 한다.

## 권장 파일

| 파일 | 목적 | 커밋 |
|---|---|---|
| `.env.example` | 공통 변수 안내 또는 문서 링크 | 가능 |
| `backend/.env.example` | 백엔드 실행 변수 안내 | 가능 |
| `frontend/.env.example` | 프론트 로컬 공개 변수 안내 | 가능 |
| `frontend/.env.production.example` | 프론트 배포 build-time 공개 변수 안내 | 가능 |
| `.env` | 개인 로컬 실제값 | 금지 |
| `frontend/.env.local` | 개인 로컬 실제값 | 금지 |
| `frontend/.env.production` | 배포 실제값 | 금지 |
| `backend/src/main/resources/application-local.yml` | 개인 로컬 Spring 설정 | 금지 |
| `backend/src/main/resources/application-local.yml.example` | Spring local 예시 | 가능 |
| `backend/src/main/resources/application-prod.yml` | prod profile 변수 매핑 | 가능 |

## 예시 작성 규칙

- 실제 secret, API key, DB password를 넣지 않는다.
- 비밀값 예시는 `CHANGE_ME`만 사용한다.
- 로컬 절대 경로는 `<LOCAL_PATH>` 또는 `uploads` 같은 상대 경로로 적는다.
- 프론트 예시는 공개되어도 되는 값만 둔다.
- `SLATE_DEMO_ACCESS_CODE`는 backend/배포 secret으로만 관리하고 프론트 env에 넣지 않는다.
- 배포 환경변수는 배포 플랫폼의 secret manager 또는 OS 환경변수로 주입한다.

## backend `.env.example` 기준

```dotenv
SPRING_PROFILES_ACTIVE=local
SLATE_DB_URL=jdbc:mysql://localhost:3306/slate?serverTimezone=Asia/Seoul&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
SLATE_DB_USERNAME=slate_app
SLATE_DB_PASSWORD=CHANGE_ME
SLATE_JWT_SECRET=CHANGE_ME
SLATE_JWT_EXPIRATION_MINUTES=120
SLATE_UPLOAD_DIR=uploads
SLATE_FFPROBE_PATH=ffprobe
SLATE_AUDIT_IP_HASH_SALT=CHANGE_ME
SLATE_CORS_ALLOWED_ORIGINS=http://localhost:5174,http://127.0.0.1:5174
SLATE_DEMO_ACCESS_ENABLED=false
SLATE_DEMO_ACCESS_CODE=CHANGE_ME
KOBIS_API_KEY=CHANGE_ME
YOUTUBE_API_KEY=CHANGE_ME
OPENAI_API_KEY=CHANGE_ME
OPENAI_MODEL=gpt-4o-mini
```

## frontend `.env.example` 기준

```dotenv
VITE_API_BASE_URL=
VITE_DEMO_ACCESS_GATE=false
```

로컬 Vite 개발에서는 `VITE_API_BASE_URL`을 비워 두면 `/api` 요청이 `vite.config.js` proxy를 통해 `http://localhost:8080`으로 전달된다.

## 참조 경로

- `.env.example`
- `backend/.env.example`
- `frontend/.env.example`
- `frontend/.env.production.example`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-local.yml.example`
- `backend/src/main/resources/application-prod.yml`
- `frontend/src/services/api.js`
- `frontend/vite.config.js`
- `docu/08_environment/env_variables.md`
- `docu/03_mvp_scope/mvp_decisions.md`
