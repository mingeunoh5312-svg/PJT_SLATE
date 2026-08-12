# prototype_3 Code Review Result

작성일: 2026-06-16
기준 문서: `docu/prompt/code_review_prompt.md`
대상: `prototype_3` 백엔드, 프론트엔드, SQL, 환경/배포 문서

## 결론

`prototype_3`는 MVP 이전 기준 기능의 상당 부분이 구현되어 있으나, 그대로 `Slate` 최종본으로 복사하기 전에 보안/운영 차단 항목을 먼저 반영해야 한다. 특히 작업물 파일 스트리밍 접근 제어, 데모 접근 코드 게이트, OpenAI 실패 fallback 정책, 운영 환경 분리, DB 동시성 제약은 MVP 이식 전에 결정 또는 수정이 필요하다.

## 2026-06-16 Slate 이식 반영 상태

아래 항목은 `prototype_3` 원본이 아니라 `Slate/` 하위 최종 앱 후보에 반영했다.

| 항목 | 반영 상태 | 경로 |
|---|---|---|
| `prototype_3` 필수 후보 복사 | 완료 | `backend`, `frontend`, `sql`, `assets` |
| `prototype2` 계열 최종명 변경 | 완료 | `backend`, `frontend`, `sql` |
| 파일 스트리밍 권한 | 반영 | `backend/src/main/java/com/slate/boards/WorkFileService.java`, `backend/src/test/java/com/slate/boards/WorkFileServiceStreamAuthorizationTest.java` |
| 데모 접속 코드 gate | 반영 | `backend/src/main/java/com/slate/security`, `frontend/src/views/DemoAccessView.vue`, `frontend/src/router/index.js` |
| OpenAI 실패 fallback | 반영 | `backend/src/main/java/com/slate/matching/AiMatchingRecommendationService.java` |
| 운영 환경 분리 | 부분 반영 | `backend/.env.example`, `frontend/.env.example`, `backend/src/main/resources/application-prod.yml` |
| 팀 지원/초대 중복과 슬롯 정원 방어 | 부분 반영 | `sql/01_schema.sql`, `backend/src/main/resources/mappers/MatchingMapper.xml`, `backend/src/main/resources/mappers/TeamMapper.xml` |
| 게시글 조회 IP hash salt 적용 | 반영 | `backend/src/main/java/com/slate/boards/BoardService.java` |

검증:

- `backend`: `mvn test` 통과. 39 tests, failures 0, errors 0.
- `frontend`: `npm install`, `npm run build` 통과.

남은 항목:

- 공개 회사 서류 업로드의 1회성 토큰/rate limit 정책.
- 실제 MySQL schema 적용 검증.
- 실제 KOBIS/YouTube/OpenAI key smoke.
- 브라우저/모바일 smoke.
- 배포 provider, HTTPS/CORS 최종값, 로그 rotation 정책.

검증 실행 결과:

- `prototype_3/backend`: `mvn test` 통과. 총 36개 테스트, 실패 0, 오류 0.
- `prototype_3/frontend`: `npm run build` 실패. Vite가 `prototype_3/frontend/dist/local-run` 삭제 단계에서 `EPERM`으로 중단됨. 권한 상승 재시도 후에도 동일했다. 기존 `node` 프로세스와 Vite 로그 파일이 남아 있어 파일 잠금 가능성이 있다.
- 실행하지 않은 검증: 브라우저 smoke, 모바일 viewport, DB migration 실제 적용, 실제 KOBIS/YouTube/OpenAI 키 기반 API 호출, 파일 업로드/스트리밍 E2E, 동시성 테스트.

## Severity Findings

### P1. 작업물 파일 스트리밍에 게시글/팀/소유자 접근 제어가 없다

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/BoardController.java:211`에서 `GET /api/boards/work-files/{fileId}/stream`을 제공한다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:35`에서 `GET /api/boards/**`가 공개 허용된다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/WorkFileService.java:231-247`은 파일 상태가 `ACTIVE`이고 경로가 존재하면 바로 inline 응답을 반환한다.

영향:

`file_id`를 알거나 추측할 수 있으면 `PRIVATE`, `COMPANY`, 팀 전용, 승인 전 작업물과 연결된 파일도 스트리밍될 수 있다. MVP 이식 전에 스트림 요청에 현재 사용자, 게시글 visibility, 팀 멤버십, 작성자/관리자 권한을 반영해야 한다.

권장:

- 스트림 endpoint를 인증 필요로 변경하거나 공개 파일만 별도 공개 URL로 분리한다.
- `WorkFileService.stream`에 requester를 전달하고 `work_item`, `board_post`, `team_member` 기준 접근 가능 여부를 검증한다.
- 보안 테스트를 추가한다. 최소 케이스: 공개 게시글 파일 허용, 비공개 타인 파일 거부, 팀 파일 비멤버 거부, 삭제/보류 파일 거부.

### P1. 데모 계정이 프론트에 노출되지만 배포 전 접근 코드 게이트가 없다

근거:

- `prototype_3/frontend/src/views/LoginView.vue:22-28`, `40`, `56-59`, `144-157`에서 데모 계정과 공통 비밀번호를 UI에 노출한다.
- `prototype_3/sql/03_seed_sample_data.sql:3-16`과 `prototype_3/sql/05_seed_ai_matching_dummy_data.sql:5-18`에 `{noop}` 기반 샘플 계정이 포함되어 있다.
- `prototype_3/frontend/src/router/index.js:319-357`에는 인증/관리자 route guard만 있고, 배포 데모 접근 코드 게이트는 없다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:30-39`에도 데모 access code filter가 없다.

영향:

데모 배포가 공개 URL로 노출되면 누구나 관리자/회사/일반 테스트 계정으로 접근할 수 있다. 문서상 배포 데모에는 Slate 페이지 진입 전 access code gate가 필요하므로 MVP 이전 차단 항목이다.

권장:

- 프론트 route gate와 백엔드 filter를 함께 둔다. 프론트만 두면 API 직접 호출을 막지 못한다.
- demo seed는 demo profile에서만 적용하고 운영 profile에서는 배제한다.
- `LoginView`의 빠른 계정 선택은 access code 통과 후에만 노출한다.

### P1. OpenAI 실패 시 기존 점수 기반 fallback 정책과 구현이 다르다

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/matching/AiMatchingRecommendationService.java:80`은 OpenAI 응답을 받아 추천을 만든다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/matching/AiMatchingRecommendationService.java:115-123`은 API key 누락 또는 OpenAI 호출 실패를 예외로 전파한다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/matching/AiMatchingRecommendationService.java:164-165`, `180-189`의 fallback은 응답 파싱 실패 시에만 작동한다.
- `prototype_3/backend/src/test/java/com/slate/prototype2/matching/AiMatchingRecommendationServiceTest.java:128-138`도 API key 누락 예외 전파를 기대한다.

영향:

MVP 결정 문서의 “AI 실패 시 기존 점수 기반 추천 출력”과 다르게, 실제 API 장애/키 누락 시 사용자 추천 요청은 실패한다.

권장:

- 정책을 하나로 확정한다. 권장안은 OpenAI 호출 실패, 4xx/5xx, key 누락 모두 기존 점수 기반 후보를 반환하고, UI에는 “AI 사유 생성 실패, 기본 추천 사용” 상태를 표시하는 방식이다.
- 위 정책에 맞춰 테스트 기대값을 바꾼다.

### P2. 공개 기업 서류 업로드 endpoint가 식별자만으로 쓰기 가능하다

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:32`가 `POST /api/auth/company-applications/*/documents`를 공개 허용한다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/accounts/CompanyDocumentController.java:29-39`가 공개 업로드를 제공한다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/accounts/CompanyDocumentService.java:66-71`은 신청 ID와 사업자번호 일치만 확인한다.

영향:

신청 ID와 사업자번호가 노출되거나 추측되면 인증 없이 반복 업로드가 가능하다. 파일 크기/확장자 검증은 있으나 rate limit, 접근 코드, 신청자 세션 검증이 없다.

권장:

- 공개 업로드가 반드시 필요하면 1회성 업로드 토큰, access code, rate limit, 감사 로그를 추가한다.
- 회사 가입 이후 보완 서류는 인증된 회사 계정 flow로만 허용한다.

### P2. 운영 환경 분리가 부족하다

근거:

- `prototype_3/backend/src/main/resources/application.yml:5`가 기본 profile을 `local`로 둔다.
- `prototype_3/backend/src/main/resources/application.yml:30`, `36`에 로컬 개발용 JWT secret/audit salt 기본값이 있다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/security/SecurityConfig.java:51-64`의 CORS origin이 localhost로 고정되어 있다.
- `prototype_3/frontend/src/services/api.js:1`은 `VITE_API_BASE_URL`이 없으면 same-origin으로 요청한다.
- `prototype_3/frontend/vite.config.js:6-14`의 proxy는 개발 서버 전용이다.

영향:

운영 배포에서 profile, secret, CORS, API base URL이 명시되지 않으면 로컬 설정 또는 잘못된 API 경로로 동작할 수 있다.

권장:

- `application-prod.yml` 또는 env-only 운영 profile을 추가하고 secret/audit salt/API key는 기본값 없이 필수화한다.
- CORS allowed origins를 env로 분리한다.
- frontend `.env.production.example`에 `VITE_API_BASE_URL`을 명시한다.

### P2. 팀 지원/초대와 슬롯 수락에 DB 동시성 방어가 부족하다

근거:

- `prototype_3/sql/01_schema.sql:361-407`의 `team_application`, `team_invitation`에는 pending 중복을 막는 unique 제약이 없다.
- `prototype_3/backend/src/main/resources/mappers/MatchingMapper.xml:260-285`는 insert 전에 count로 중복 확인한다.
- `prototype_3/backend/src/main/resources/mappers/TeamMapper.xml:595-605`는 `accepted_count = accepted_count + 1` 후 `accepted_count >= required_count`이면 닫는다.

영향:

동시 요청이 들어오면 중복 지원/초대 또는 정원 초과 수락이 발생할 수 있다. 애플리케이션 count 검사는 DB 제약을 대체하지 못한다.

권장:

- pending 상태 중복 방지를 위한 generated column 또는 상태별 unique 전략을 정한다.
- 슬롯 수락은 `WHERE accepted_count < required_count AND status = 'OPEN'` 조건부 update와 affected row 확인을 사용한다.

### P2. 조회 로그 IP hash 정책이 audit hash와 다르다

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/operations/RequestLogContext.java:23-24`, `53-60`은 audit salt를 사용해 IP를 hash한다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/BoardService.java:599-607`은 게시글 조회 로그에서 salt 없이 IP/User-Agent 또는 userId를 SHA-256 처리한다.
- `prototype_3/backend/src/main/resources/mappers/BoardMapper.xml:619-638`은 이 hash를 24시간 조회 중복 방지에 사용한다.

영향:

같은 입력에 대해 장기간 안정적인 hash가 남아 개인정보 정책 일관성이 깨질 수 있다.

권장:

- 게시글 조회 로그도 `RequestLogContext.hashValue` 또는 별도 salt 기반 hash를 사용한다.
- 24시간 window가 목적이면 `view_window_start`를 시간 bucket으로 저장하거나 TTL/정리 정책을 명확히 한다.

### P3. `prototype2/prototype_2/slate_prototype2` 명칭이 아직 남아 있다

근거:

- `prototype_3/backend/pom.xml:15-18`
- `prototype_3/backend/src/main/resources/application.yml:3`, `23`, `29`
- `prototype_3/backend/src/main/resources/mappers/*.xml:6`
- `prototype_3/sql/00_create_database.sql:1`, `6`, `9`
- `prototype_3/frontend/package.json:2`
- `prototype_3/frontend/src/views/LoginView.vue:64`

영향:

MVP 이식 후 final root가 `Slate`라는 문서 기준과 다르다. package namespace, mapper namespace, DB명, artifact명, UI 문구를 한 번에 정리해야 한다.

권장:

- 복사 단계에서 `com.slate.prototype2`를 최종 namespace로 변경하고 mapper namespace까지 동기화한다.
- DB명은 운영/로컬 예시 모두 `slate` 계열로 정한다.

### P3. 파일 업로드의 duration/file type 검증이 운영 의존적이다

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/WorkFileService.java:266-302`는 ffprobe 실패 시 client duration을 신뢰한다.
- `prototype_3/backend/src/main/java/com/slate/prototype2/boards/WorkFileService.java:41-43`, `314-321`은 확장자와 content type만 확인하며 `application/octet-stream`도 허용한다.

영향:

운영 서버에 ffprobe가 없거나 실패하면 3분 제한이 클라이언트 제공값에 의존한다.

권장:

- ffprobe를 운영 필수 의존성으로 문서화하거나 서버 검증 실패 시 업로드 실패로 통일한다.
- MIME magic 또는 media probe 기반 검증을 추가한다.

### P3. 프론트 인증 오류 처리가 status 정보를 잃는다

근거:

- `prototype_3/frontend/src/services/api.js:13-23`, `26-35`는 실패 시 plain `Error`만 던진다.
- `prototype_3/frontend/src/App.vue:32-56`은 mount/loadMe 실패 때만 token을 정리한다.
- `prototype_3/frontend/src/App.vue:131-133` route 변경 시 인증 재검증은 없다.

영향:

401/403 만료, 회사 승인 대기, 권한 없음 같은 상태별 UX가 흐려지고, 일부 화면에서 stale token 상태가 늦게 정리될 수 있다.

권장:

- API error에 status/code/payload를 포함한다.
- 401은 전역 token clear, 403은 권한 없음 UI로 분리한다.

### P3. OpenAI 오류 응답 본문 로그가 과도할 수 있다

근거:

- `prototype_3/backend/src/main/java/com/slate/prototype2/matching/OpenAiClient.java:54-60`은 OpenAI 오류 응답 body를 truncate 후 warn 로그로 남긴다.
- prompt에는 팀/프로필 후보 데이터가 포함된다.

영향:

외부 API 오류 응답에 요청 일부나 민감한 후보 정보가 섞이면 운영 로그에 남을 수 있다.

권장:

- status, request id, error code 중심으로 로깅하고 body는 debug 또는 마스킹한다.

## 구현 상태 분류

### 구현됨

- Spring Boot 4, MyBatis, MySQL 기반 백엔드 골격.
- JWT 인증, role 기반 admin route 보호, 서비스 단 세부 관리자 permission.
- KOBIS 검색/검증 client와 portfolio verification 저장.
- YouTube metadata preview 및 작업물 등록/수정 연동.
- OpenAI AI 추천 client와 기본 unit test.
- 로컬 파일 업로드, quota, soft delete, admin moderation 일부.
- Vue 3/Vite 프론트, route guard, 주요 화면/asset 포함.
- SQL schema, reference/sample/AI dummy seed, reset script.

### 부분 구현

- AI 추천 fallback: 파싱 실패 fallback은 있으나 OpenAI 호출 실패 fallback은 없다.
- KOBIS 검증: 키 누락/결과 없음은 상태 저장하지만 예외 발생 시 최신 상태가 남지 않을 수 있다.
- YouTube: metadata 연동은 있으나 실제 API smoke와 장애 UX 검증이 없다.
- 파일 접근 제어: 업로드/목록/삭제 owner 검증은 있으나 streaming 공개 범위 검증이 빠져 있다.
- audit hash: 운영 로그 hash에는 salt가 있으나 게시글 조회 hash에는 없다.
- 배포 설정: local/dev proxy 중심이고 prod CORS/API base/secret/profile 분리가 부족하다.

### 문서만 있음

- 최종 root `<SLATE_ROOT>`.
- `prototype2` 계열 명칭 제거 계획.
- 데모 access code gate.
- local demo, frontend/backend 분리 배포, EC2 단일 서버 후순위 전략.
- HTTPS, log rotation, backup, 운영 `.env.example` 정책.

### 미구현 또는 확인 필요

- 배포 데모 접근 코드 게이트.
- frontend/backend 운영 env example.
- DB migration 도구 또는 반복 적용 전략.
- 파일 orphan 정리, physical delete batch.
- 실제 외부 API 키 기반 smoke.
- 브라우저/모바일 smoke.
- 팀 지원/수락 동시성 테스트.

## Open Questions

- 작업물 파일 stream은 공개 게시글에 연결된 파일만 비로그인 허용할 것인가, 모든 파일 stream을 인증 필요로 둘 것인가?
- 회사 서류 public upload는 유지해야 하는가, 아니면 회사 계정 인증 후 업로드로 단순화할 것인가?
- OpenAI key 누락도 “AI 실패 fallback”에 포함할 것인가?
- 데모 seed와 sample seed를 최종 `Slate` 저장소에 포함하되 profile로 분리할 것인가, 별도 demo SQL로 분리할 것인가?
- `COMPANY` visibility 게시글은 승인 회사 전체 공개인지, 작성 회사/팀 한정인지 정책 확인이 필요하다.
