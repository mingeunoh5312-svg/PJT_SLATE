# Security And Environment Findings

작성일: 2026-06-16
대상: `prototype_3`

## High Priority

### 1. 파일 스트리밍 공개 범위

`GET /api/boards/work-files/{fileId}/stream`은 `GET /api/boards/**` 공개 허용 규칙에 포함되고, 서비스는 `ACTIVE` 파일 여부만 확인한다.

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:35`
- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/BoardController.java:211`
- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/WorkFileService.java:231-247`

필수 조치:

- 스트림 요청에 requester를 전달한다.
- 공개 게시글에 연결된 파일만 anonymous 허용하거나 전체 stream을 인증 필요로 바꾼다.
- 팀 작업물은 팀 멤버/승인자/관리자 권한을 확인한다.

### 2. 데모 접근 코드 게이트 부재

프론트 로그인 화면은 데모 계정과 공통 비밀번호를 노출하고, SQL seed에도 같은 계정이 포함된다. 현재는 Slate 페이지 진입 전 access code gate가 없다.

근거:

- `prototype_3/frontend/src/views/LoginView.vue:22-28`
- `prototype_3/frontend/src/views/LoginView.vue:40`
- `prototype_3/frontend/src/views/LoginView.vue:56-59`
- `prototype_3/frontend/src/views/LoginView.vue:144-157`
- `prototype_3/sql/03_seed_sample_data.sql:3-16`
- `prototype_3/frontend/src/router/index.js:319-357`
- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:30-39`

필수 조치:

- 프론트 route gate와 백엔드 filter를 함께 추가한다.
- access code session/token은 짧은 TTL과 별도 cookie/header 정책을 둔다.
- demo seed는 demo profile에서만 실행되게 분리한다.

### 3. 공개 기업 서류 업로드

기업 신청 서류 업로드가 인증 없이 열려 있고, 신청 ID와 사업자번호만 검증한다.

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:32`
- `prototype_3/backend/src/main/java/com/slate/prototype2/accounts/CompanyDocumentController.java:29-39`
- `prototype_3/backend/src/main/java/com/slate/prototype2/accounts/CompanyDocumentService.java:66-71`

권장 조치:

- 공개 업로드 유지 시 1회성 token, rate limit, audit event를 추가한다.
- 가능하면 인증된 회사 계정 보완 서류 flow로 통일한다.

## Medium Priority

### 4. 운영 secret/profile 기본값

`application.yml`에 로컬 profile과 개발용 secret fallback이 남아 있다. 실제 값은 문서화하지 않고, 기본값 존재만 기록한다.

근거:

- `prototype_3/backend/src/main/resources/application.yml:5`
- `prototype_3/backend/src/main/resources/application.yml:30`
- `prototype_3/backend/src/main/resources/application.yml:36`
- `prototype_3/backend/src/main/resources/application-local.yml.example:3-11`

권장 조치:

- 운영 profile에서는 JWT secret, audit salt, DB password, 외부 API key에 기본값을 두지 않는다.
- `.env.example`은 placeholder만 제공한다.
- `spring.profiles.default=local`은 최종 운영 artifact에서 제거하거나 local 파일로 이동한다.

### 5. CORS와 API base URL 운영 분리

CORS origin은 localhost로 고정되어 있고, frontend API base는 env가 없으면 same-origin이다.

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:51-64`
- `prototype_3/frontend/src/services/api.js:1`
- `prototype_3/frontend/vite.config.js:6-14`

권장 조치:

- `SLATE_CORS_ALLOWED_ORIGINS` 같은 env로 분리한다.
- frontend `.env.local.example`, `.env.production.example`을 나눈다.
- 분리 배포 시 `VITE_API_BASE_URL` 누락을 빌드/런타임에서 감지한다.

### 6. OpenAI 오류 로그

OpenAI 오류 응답 body가 warn 로그에 남는다.

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/matching/OpenAiClient.java:54-60`

권장 조치:

- 운영 로그에는 status, provider error code, request id만 남긴다.
- body는 debug 로그 또는 마스킹된 일부 필드로 제한한다.

### 7. IP hash 정책 불일치

audit log는 salt 기반 hash를 사용하지만 board view log는 salt 없는 SHA-256을 사용한다.

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/operations/RequestLogContext.java:23-24`
- `prototype_3/backend/src/main/java/com/slate/prototype2/operations/RequestLogContext.java:53-60`
- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/BoardService.java:599-607`

권장 조치:

- 동일한 salt 기반 hash 정책을 재사용한다.
- view log 보존 기간과 삭제 정책을 운영 문서에 명시한다.

## Environment And Deployment Risks

### 로컬 실행

- backend local 예시는 `slate_prototype2` DB명을 사용한다.
- frontend dev server는 5174 고정이며 `/api`를 localhost 8080으로 proxy한다.
- ffprobe는 파일 duration 검증에 사실상 필요하지만 설치 검증 스크립트가 없다.

근거:

- `prototype_3/backend/src/main/resources/application-local.yml.example:3`
- `prototype_3/sql/00_create_database.sql:1`
- `prototype_3/frontend/vite.config.js:6-14`
- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/WorkFileService.java:266-302`

### 분리 배포

- Vite history mode를 사용하므로 정적 서버 fallback이 필요하다.
- CORS/API base URL이 운영 도메인에 맞게 분리되어야 한다.
- file upload root가 backend 서버 로컬 디스크일 때 backup/restore 정책이 필요하다.

근거:

- `prototype_3/frontend/src/router/index.js:300`
- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:51-64`

### EC2 단일 서버

- reverse proxy, HTTPS, upload directory 권한, log rotation, backup cron이 별도 작업으로 남아 있다.
- 현재 문서상 EC2 단일 서버는 마지막 우선순위이므로 MVP 전 최소 local/demo 기준을 먼저 닫는 편이 현실적이다.

## Do Not Expose

다음 값은 문서나 로그에 실제 값을 출력하지 않는다.

- `SLATE_JWT_SECRET`
- `SLATE_AUDIT_IP_HASH_SALT`
- `KOBIS_API_KEY`
- `YOUTUBE_API_KEY`
- `OPENAI_API_KEY`
- DB password

이번 리뷰 문서에는 실제 secret 값을 기록하지 않았다.
