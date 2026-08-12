# 1차 필터(Demo Access Gate) 이식 인수인계 문서

- 작성일: 2026-06-24
- 대상 작업: 1차 필터 구현분을 공동 작업자 코드베이스에 이식
- 기준 범위: `VITE_DEMO_ACCESS_GATE` 프론트 라우터 게이트, `SLATE_DEMO_ACCESS_*` 백엔드 API 게이트, DB 기반 접근 코드 관리
- 참고 작업 로그:
  - `Slate/docu/work_logs/2026-06-24_creator_demo_access_router_filter.md`
  - `Slate/docu/work_logs/2026-06-24_fixer_demo_access_backend_filter.md`
  - `Slate/docu/work_logs/2026-06-24_fixer_demo_access_final_smoke_acceptance.md`
  - `Slate/docu/work_logs/2026-06-24_fixer_demo_access_admin_code_management.md`

## 1. 구현 전/후 요약

### 구현 전

- 사용자가 사이트에 진입하기 전에 별도 접속 코드를 요구하는 전역 라우터 게이트가 없었다.
- API를 직접 호출하면 프론트 화면을 거치지 않고 공개/인증 API에 접근할 수 있었다.
- 접속 코드를 서버에서 검증하는 전용 API와 `X-Slate-Demo-Code` 헤더 계약이 없었다.
- 운영자가 접근 코드를 DB에서 생성/폐기/조회하는 관리 화면과 테이블이 없었다.
- 보호된 이미지/파일 요청은 일반 API 요청과 동일한 Demo Access 헤더 보장이 필요했으나, 별도 blob 요청 경로까지 일관되게 묶여 있지 않았다.

### 구현 후

- 프론트에서 `VITE_DEMO_ACCESS_GATE=true`일 때 모든 라우트 진입 전에 `/demo-access`를 먼저 통과한다.
- 백엔드에서 `SLATE_DEMO_ACCESS_ENABLED=true`일 때 `POST /api/demo/access`와 `OPTIONS`를 제외한 `/api/**` 요청에 `X-Slate-Demo-Code`를 요구한다.
- 프론트의 일반 JSON 요청과 blob 파일/이미지 요청 모두 Demo Access 코드 헤더를 자동 첨부한다.
- 접속 코드 검증은 환경변수 fallback 코드와 DB 발급 코드를 모두 지원한다.
- 관리자는 `/admin/demo-access`에서 접근 코드를 생성, 수정, 폐기할 수 있다.
- DB에는 평문 코드가 저장되지 않고, 해시와 fingerprint만 저장된다. 평문 코드는 생성 응답에서 1회만 노출된다.

## 2. 프론트엔드 변경 파일

### `Slate/frontend/src/router/index.js`

- `DemoAccessView`를 import하고 `/demo-access` 라우트를 추가했다.
- `adminRoutes`에 `/admin/demo-access` 경로를 추가했다.
- `router.beforeEach`의 가장 앞에서 Demo Access 게이트를 검사한다.
- 게이트가 켜져 있고 `sessionStorage`에 `slate.demoAccessCode`가 없으면 기존 목적지를 `redirect` query로 보존한 뒤 `/demo-access`로 보낸다.
- 게이트가 꺼져 있는데 `/demo-access`로 접근하면 `home`으로 돌려보낸다.
- 인증/관리자 검사는 Demo Access 통과 이후에 실행된다.

### `Slate/frontend/src/views/DemoAccessView.vue`

- 접속 안내 화면과 코드 입력 폼을 담당한다.
- `slateApi.verifyDemoAccess(code)`로 서버 검증을 호출한다.
- 검증 성공 시 `sessionStorage`의 `slate.demoAccessCode`에 코드를 저장한다.
- `redirect` query가 내부 경로일 때만 원래 목적지로 복귀하고, 외부 URL 또는 비정상 값은 `/`로 fallback한다.

### `Slate/frontend/src/services/api.js`

- 추가된 상수/저장소:
  - `TOKEN_KEY = slate.accessToken`
  - `DEMO_ACCESS_CODE_KEY = slate.demoAccessCode`
  - `DEMO_ACCESS_GATE = import.meta.env.VITE_DEMO_ACCESS_GATE === 'true'`
- 추가된 함수:
  - `isDemoAccessGateEnabled()`
  - `getDemoAccessCode()`
  - `setDemoAccessCode(code)`
  - `isProtectedApiResourceUrl(value)`
  - `apiBlob(path, options)`
- `api()`와 `apiBlob()` 모두 게이트가 켜져 있고 코드가 있으면 `X-Slate-Demo-Code` 헤더를 자동 첨부한다.
- `POST /api/demo/access` 호출용 `slateApi.verifyDemoAccess(code)`를 추가했다.
- 서버가 접속 코드 관련 `403`을 반환하면 저장된 코드를 제거하고 `slate-demo-access-rejected` 이벤트를 발생시킨다.
- 관리자 접근 코드 API를 추가했다.
  - `adminDemoAccessCodes()`
  - `adminCreateDemoAccessCode(payload)`
  - `adminUpdateDemoAccessCode(codeId, payload)`
  - `adminRevokeDemoAccessCode(codeId, payload)`

### `Slate/frontend/src/App.vue`

- `slate-demo-access-rejected` 이벤트를 받아 stale code를 제거한 뒤 `/demo-access`로 다시 보낸다.
- 현재 경로를 `redirect` query로 보존해 재검증 후 원래 위치로 돌아갈 수 있게 한다.

### `Slate/frontend/src/services/protectedResources.js`

- 보호 리소스 URL이 `/api/**` 또는 API base 하위 URL이면 `apiBlob()`으로 받아 object URL로 변환한다.
- 컴포넌트 unmount 또는 리소스 변경 시 object URL을 revoke한다.

### `Slate/frontend/src/components/media/ProtectedImage.vue`

- 게이트 적용 대상 이미지 URL을 직접 `<img src="/api/...">`로 쓰지 않고 보호 리소스 헬퍼를 통해 blob URL로 렌더링한다.
- 이미지 요청에도 `X-Slate-Demo-Code`가 붙도록 보장한다.

### `Slate/frontend/src/components/media/ProtectedVideo.vue`

- 보호 영상/파일 스트림도 `apiBlob()` 기반으로 받아 object URL로 렌더링한다.
- Demo Access 헤더 누락으로 미디어만 깨지는 문제를 방지한다.

### `Slate/frontend/src/views/AdminView.vue`

- `/admin/demo-access` 섹션을 추가했다.
- 관리자 접근 코드 목록 조회, 생성, 설정 수정, 폐기 UI를 제공한다.
- 생성 직후 평문 코드를 한 번만 보여주고 복사할 수 있게 한다.

## 3. 백엔드 변경 파일

### `Slate/backend/src/main/java/com/slate/security/DemoAccessProperties.java`

- `@ConfigurationProperties(prefix = "slate.demo-access")`로 환경 설정을 바인딩한다.
- 설정값:
  - `enabled`: 1차 필터 활성화 여부
  - `code`: DB 코드가 없거나 임시 운영 시 사용할 fallback 코드
- `matches(candidate)`는 게이트가 켜져 있고 fallback 코드가 설정된 경우에만 true를 반환한다.

### `Slate/backend/src/main/java/com/slate/security/DemoAccessFilter.java`

- `OncePerRequestFilter` 기반 API 1차 필터다.
- 헤더명은 `X-Slate-Demo-Code`로 고정했다.
- `DemoAccessGateService.requiresDemoCode(request)`가 true인 요청만 검사한다.
- 코드가 없거나 틀리면 `403`과 JSON 응답을 반환한다.
- 실패 응답 메시지: `Slate 접속 코드가 필요합니다.`

### `Slate/backend/src/main/java/com/slate/security/DemoAccessGateService.java`

- 실제 게이트 판정을 담당한다.
- 필터 대상:
  - `SLATE_DEMO_ACCESS_ENABLED=true`
  - `/api/**`
  - 단, `OPTIONS`는 제외
  - 단, `POST /api/demo/access`는 검증 API이므로 제외
- 검증 우선순위:
  - 환경변수 fallback 코드(`SLATE_DEMO_ACCESS_CODE`) 일치
  - DB 접근 코드 후보 조회 후 `PasswordEncoder.matches()` 검증
- `verify(headerCode, bodyCode)`는 헤더 코드가 있으면 body 코드보다 우선한다.
- `POST /api/demo/access`에서 DB 코드 검증 성공 시 `used_count`를 증가시킨다.
- 이미 검증되어 헤더로 쓰이는 코드는 요청 허용 검사에서 사용 횟수를 매번 증가시키지 않는다.

### `Slate/backend/src/main/java/com/slate/security/DemoAccessController.java`

- `POST /api/demo/access` 검증 엔드포인트를 제공한다.
- 요청 코드는 `X-Slate-Demo-Code` 헤더 또는 JSON body `{ "code": "..." }`에서 받는다.
- 응답은 공통 `ApiResponse` 형식을 따른다.

### `Slate/backend/src/main/java/com/slate/security/SecurityConfig.java`

- `POST /api/demo/access`만 `permitAll()`로 열었다.
- `GET /api/demo/access` 등 다른 메서드는 열지 않는다.
- `OPTIONS /**`는 CORS preflight를 위해 허용한다.
- `DemoAccessFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록한다.
- CORS 허용 헤더에 `X-Slate-Demo-Code`를 추가한다.

### `Slate/backend/src/main/java/com/slate/security/DemoAccessCodeService.java`

- 관리자 접근 코드 생성/수정/폐기/조회 비즈니스 로직을 담당한다.
- `DEMO_ACCESS_MANAGE` 권한을 가진 관리자만 실행할 수 있다.
- 생성 시 Base64 URL-safe 랜덤 코드를 발급한다.
- DB에는 `code_hash`, `code_fingerprint`만 저장하고 평문 코드는 저장하지 않는다.
- 관리자 조회 응답에서는 `codeHash`, `codeFingerprint`를 제거한다.
- `effectiveStatus`를 계산한다.
  - `ACTIVE`
  - `SCHEDULED`
  - `EXPIRED`
  - `EXHAUSTED`
  - `REVOKED`
- 생성/수정/폐기 시 audit log와 operation log를 남긴다. 로그에도 평문 코드는 남기지 않는다.

### `Slate/backend/src/main/java/com/slate/security/DemoAccessAdminController.java`

- 관리자 접근 코드 REST API를 제공한다.
- 모든 엔드포인트는 `hasRole('ADMIN')`이 필요하고, 서비스 계층에서 `DEMO_ACCESS_MANAGE` 세부 권한을 추가 확인한다.
- 엔드포인트:
  - `GET /api/admin/demo-access/codes`
  - `POST /api/admin/demo-access/codes`
  - `PATCH /api/admin/demo-access/codes/{codeId}`
  - `POST /api/admin/demo-access/codes/{codeId}/revoke`

### `Slate/backend/src/main/java/com/slate/security/DemoAccessCodeMapper.java`
### `Slate/backend/src/main/resources/mappers/DemoAccessCodeMapper.xml`

- 접근 코드 CRUD와 검증 후보 조회 SQL을 담당한다.
- 검증 후보는 fingerprint 기반으로 좁힌 뒤 Java에서 `PasswordEncoder.matches()`로 최종 확인한다.
- 만료, 시작 전, 폐기, 사용 횟수 초과 코드는 검증 후보에서 제외된다.

### `Slate/backend/src/main/java/com/slate/admin/AdminPermissionCatalog.java`

- 관리자 권한 카탈로그에 `DEMO_ACCESS_MANAGE`를 추가했다.
- 권한 표시명: `접근 코드 관리`
- 용도: 배포 전/점검 안내 접근 코드를 생성하고 폐기

### 테스트 파일

- `Slate/backend/src/test/java/com/slate/security/DemoAccessPropertiesTest.java`
- `Slate/backend/src/test/java/com/slate/security/DemoAccessFilterTest.java`
- `Slate/backend/src/test/java/com/slate/security/DemoAccessControllerTest.java`
- `Slate/backend/src/test/java/com/slate/security/DemoAccessGateServiceTest.java`
- `Slate/backend/src/test/java/com/slate/security/DemoAccessCodeServiceTest.java`
- `Slate/backend/src/test/java/com/slate/security/SecurityConfigTest.java`

테스트에서 반드시 유지해야 하는 핵심 케이스는 다음과 같다.

- 게이트 비활성화 시 API 요청 통과
- 게이트 활성화 + 헤더 없음이면 `/api/**` 403
- `POST /api/demo/access`만 필터 예외
- `GET /api/demo/access`는 예외가 아님
- `OPTIONS`는 통과
- 헤더 코드가 body 코드보다 우선
- whitespace-only 코드 거부
- DB 코드의 만료/폐기/사용 횟수 초과 상태 처리
- 관리자 코드 생성 시 평문은 응답에만 포함되고 저장/조회/로그에는 노출되지 않음

## 4. DB/SQL 변경 파일

### `Slate/sql/17_demo_access_code_management_schema.sql`

신규 운영 반영용 SQL이다. 공동 작업자 DB에 반드시 적용해야 한다.

추가 테이블: `demo_access_code`

주요 컬럼:

- `code_id`: PK
- `label`: 관리자 표시 이름
- `code_hash`: PasswordEncoder 해시
- `code_fingerprint`: 후보 조회용 fingerprint
- `status`: `ACTIVE`, `REVOKED` 등 상태
- `starts_at`: 사용 시작 시각
- `expires_at`: 만료 시각
- `max_uses`: 최대 사용 횟수
- `used_count`: 검증 성공 사용 횟수
- `last_used_at`: 마지막 사용 시각
- `created_by`, `updated_by`, `revoked_by`: 관리자 사용자 FK
- `revoke_reason`: 폐기 사유

추가 권한/시드:

- `common_code`의 `ADMIN_PERMISSION` 그룹에 `DEMO_ACCESS_MANAGE` 추가
- 기존 `ADMIN` 계정에 `DEMO_ACCESS_MANAGE` 권한 부여

### `Slate/sql/01_schema.sql`

- 신규 설치용 schema에도 `demo_access_code` 테이블 정의를 포함해야 한다.
- `17_demo_access_code_management_schema.sql`만 운영 반영용으로 적용하고, 새 환경 bootstrap에는 `01_schema.sql` 반영분이 필요하다.

### `Slate/sql/02_seed_reference.sql`

- `ADMIN_PERMISSION` 공통 코드에 `DEMO_ACCESS_MANAGE`를 포함해야 한다.

### `Slate/sql/03_seed_sample_data.sql`

- 샘플 관리자 계정 또는 seed 관리자에게 `DEMO_ACCESS_MANAGE` 권한이 부여되어야 한다.

### `Slate/sql/99_reset.sql`

- reset 시 `demo_access_code` 데이터를 정리해야 한다.
- FK 순서 때문에 `user_account` 삭제 전 접근 코드 테이블을 먼저 정리해야 한다.

## 5. 환경변수/설정 변경

### 프론트엔드

- `VITE_DEMO_ACCESS_GATE=true`
  - 프론트 라우터 게이트와 API 헤더 첨부를 활성화한다.
- 프론트 환경변수에는 실제 접속 코드를 넣지 않는다.
  - 접속 코드는 사용자가 입력하고 `sessionStorage`에만 보관한다.

### 백엔드

- `SLATE_DEMO_ACCESS_ENABLED=true`
  - `/api/**` 직접 호출 필터를 활성화한다.
- `SLATE_DEMO_ACCESS_CODE=<임시 접속 코드>`
  - DB 코드가 준비되지 않은 초기 운영용 fallback 코드다.
  - DB 기반 관리자 코드가 준비되면 짧게만 사용하거나 비워두는 것을 권장한다.

### Spring 설정 파일

- `Slate/backend/src/main/resources/application.yml`
- `Slate/backend/src/main/resources/application-local.yml.example`
- `Slate/backend/src/main/resources/application-prod.yml`

위 파일에서 다음 property가 바인딩되어야 한다.

```yaml
slate:
  demo-access:
    enabled: ${SLATE_DEMO_ACCESS_ENABLED:false}
    code: ${SLATE_DEMO_ACCESS_CODE:}
```

## 6. 요청 흐름

### 브라우저 진입 흐름

1. 사용자가 `/teams`, `/profile`, `/admin` 등 임의 경로로 접근한다.
2. `VITE_DEMO_ACCESS_GATE=true`이고 `sessionStorage.slate.demoAccessCode`가 없으면 `/demo-access?redirect=<원래경로>`로 이동한다.
3. 사용자가 접속 코드를 입력한다.
4. 프론트가 `POST /api/demo/access`를 호출한다.
5. 서버가 환경변수 fallback 코드 또는 DB 코드를 검증한다.
6. 성공 시 프론트가 코드를 `sessionStorage`에 저장한다.
7. 저장 후 원래 목적지로 이동한다.
8. 이후 `api()`와 `apiBlob()` 요청에는 `X-Slate-Demo-Code`가 자동 첨부된다.

### API 직접 호출 흐름

1. `SLATE_DEMO_ACCESS_ENABLED=true`인 상태에서 `/api/**` 요청이 들어온다.
2. `OPTIONS` 또는 `POST /api/demo/access`이면 통과한다.
3. 나머지는 `X-Slate-Demo-Code` 헤더를 확인한다.
4. 환경변수 fallback 코드 또는 DB 코드가 일치하면 다음 필터/JWT 인증으로 넘긴다.
5. 일치하지 않으면 `403`을 반환한다.

## 7. 이식 순서 권장안

1. DB SQL 반영
   - `demo_access_code` 테이블 생성
   - `DEMO_ACCESS_MANAGE` 공통 코드/관리자 권한 추가
2. 백엔드 설정 반영
   - `DemoAccessProperties`
   - `DemoAccessGateService`
   - `DemoAccessFilter`
   - `DemoAccessController`
   - `SecurityConfig` 예외/필터/CORS 헤더
3. 백엔드 관리자 코드 관리 반영
   - `DemoAccessCodeService`
   - `DemoAccessCodeMapper`
   - `DemoAccessCodeMapper.xml`
   - `DemoAccessAdminController`
   - `AdminPermissionCatalog`
4. 프론트 API 계층 반영
   - `api.js`의 Demo Access storage/header/blob/stale-code 처리
5. 프론트 라우터/화면 반영
   - `/demo-access`
   - router first gate
   - `/admin/demo-access`
   - `AdminView` 접근 코드 관리 UI
6. 보호 미디어 반영
   - `protectedResources.js`
   - `ProtectedImage.vue`
   - `ProtectedVideo.vue`
7. 테스트 실행 및 스모크 확인

## 8. 검증 결과

작업 로그 기준 최종 확인 결과:

- `cd Slate/frontend && npm run build`: 통과
- `cd Slate/frontend && VITE_DEMO_ACCESS_GATE=true npm run build`: 통과
- `cd Slate/backend && mvn -Dtest='com.slate.security.*Test' test`: 통과
- `cd Slate/backend && mvn test`: 232 tests 통과
- 브라우저 스모크:
  - `/`, `/matching/teams`, `/teams`, `/boards`, `/contests`, `/profile`, `/admin` 진입 시 `/demo-access?redirect=...` 이동 확인
  - blank/wrong/correct code 처리 확인
  - query/hash redirect 보존 확인
  - 외부 redirect fallback 확인
  - 관리자 `/admin/demo-access` 코드 생성/복사/수정/폐기 확인
- API 스모크:
  - 헤더 없는 `/api/**` 요청 차단 확인
  - 올바른 `X-Slate-Demo-Code` 헤더 요청 통과 확인
  - `POST /api/demo/access`만 공개 확인
  - `GET /api/demo/access`는 공개되지 않음 확인
  - `OPTIONS` preflight 통과 확인

## 9. 주의사항

- `POST /api/demo/access`만 공개해야 한다. `/api/demo/access` 전체를 `permitAll()`로 열면 안 된다.
- 프론트에는 실제 접속 코드를 환경변수로 넣지 않는다.
- `X-Slate-Demo-Code`는 JWT 대체 수단이 아니다. Demo Access 통과 후에도 기존 인증/관리자 권한 검사는 그대로 유지되어야 한다.
- DB에는 평문 접근 코드를 저장하지 않는다.
- 생성 직후 응답에 포함되는 `plainCode`는 사용자에게 1회만 노출해야 한다.
- CORS 허용 헤더에 `X-Slate-Demo-Code`가 없으면 브라우저 요청이 preflight에서 실패할 수 있다.
- blob 이미지/영상/파일 요청도 `apiBlob()` 또는 동일한 헤더 첨부 경로를 타야 한다.
- 운영 DB에는 `Slate/sql/17_demo_access_code_management_schema.sql`을 먼저 적용해야 관리자 코드 관리가 동작한다.
