# user2 크롤러·Demo Access 이식 작업 로그

## 작업 목적

`dev_Slate_0624_user1_v2`를 기준으로 `dev_Slate_0624_user2`의 콘테스트코리아 크롤링 기능과 `dev_Slate_0624_user2_filter`의 Demo Access 1차 필터 기능을 반영했다. 사용자의 정정에 따라 기능별 동작은 각 feature 브랜치 작업물이 우선되도록 보정했다.

## 수행 내용

| 영역 | 상태 | 내용 |
|---|---|---|
| 브랜치 준비 | 구현됨 | `origin/dev_Slate_0624_user2_filter`를 fetch하고 두 feature 브랜치 파일을 `codex-tmp/branch-import`로 추출했다. |
| 크롤러 백엔드 | 구현됨 | `ContestKorea*` 서비스/파서/테스트, jsoup 의존성, admin run API, mapper upsert를 반영했다. |
| 크롤러 SQL | 구현됨 | contest 포스터/출처 컬럼, source unique key, migration SQL, official link cleanup SQL을 반영했다. |
| 크롤러 프론트 | 구현됨 | 관리자 공모전 패널에 외부 크롤링 실행 영역과 결과 메트릭/항목 표시를 추가했다. |
| 공모전 필터 | 구현됨 | `deadlineWithinDays`, `regionMode` 필터와 SQL 조건을 user2 브랜치 기준으로 보정했다. |
| 출처 표시 | 구현됨 | 목록/상세에서 출처, 원문 링크, 포스터 출처, 수집일을 분리 표시한다. |
| Demo Access 백엔드 | 구현됨 | DB 코드 관리 mapper/service/controller, gate service, filter/controller 위임, 권한 코드를 반영했다. |
| Demo Access 프론트 | 구현됨 | `/admin/demo-access`, 접근 코드 CRUD UI, API 실패 이벤트 처리, 입력 화면 문구를 반영했다. |
| 보호 이미지 | 부분 구현 | 공모전/홈 공모전 이미지는 `ProtectedImage`로 교체했다. 다른 화면의 API 이미지 smoke는 후속 검증 필요. |

## 수정한 주요 경로

- `backend/pom.xml`
- `backend/src/main/java/com/slate/contests/*ContestKorea*`
- `backend/src/main/java/com/slate/contests/AdminContestController.java`
- `backend/src/main/java/com/slate/contests/ContestService.java`
- `backend/src/main/resources/mappers/ContestMapper.xml`
- `backend/src/main/java/com/slate/security/*DemoAccess*`
- `backend/src/main/resources/mappers/DemoAccessCodeMapper.xml`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/App.vue`
- `frontend/src/views/AdminView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/DemoAccessView.vue`
- `frontend/src/views/HomeView.vue`
- `frontend/src/components/media/ProtectedImage.vue`
- `frontend/src/services/protectedResources.js`
- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`
- `sql/03_seed_sample_data.sql`
- `sql/15_contest_crawl_source_schema.sql`
- `sql/16_contest_official_link_cleanup.sql`
- `sql/17_demo_access_code_management_schema.sql`
- `sql/99_reset.sql`

## 검증 결과

| 검증 | 결과 | 비고 |
|---|---|---|
| `npm run build` (`frontend`) | 통과 | Vite chunk size 경고만 있음. |
| Maven targeted tests (`backend`) | 통과 | `ContestKorea*Test`, `AdminContestKoreaCrawlerServiceTest`, `DemoAccess*Test`, `SecurityConfigTest` 기준 128 tests, Failures 0, Errors 0. |
| Maven 비승격 실행 | 실패 | sandbox network 제한으로 parent POM 해석 실패. 권한 상승 후 통과. |
| 실제 DB migration | 미수행 | SQL 파일 작성만 확인. |
| 실제 크롤링 live run | 미수행 | 외부 사이트/네트워크 의존. |
| 브라우저 smoke | 미수행 | 빌드/단위 테스트 중심 검증. |

## 이슈 및 보정

- `ContestSearchCriteria`와 `ContestService.contests` 시그니처가 바뀌며 기존 테스트 컴파일이 실패했다.
- 기존 user1_v2 테스트 계약을 유지하기 위해 호환 생성자와 오버로드를 추가했다.
- `ProtectedImage`의 error emit이 DOM 이벤트가 아닐 수 있어 `ContestView`의 이미지 에러 핸들러에 방어 코드를 추가했다.
- `AdminView`의 접근 코드 기본 만료 시각은 UTC 변환 영향을 피하도록 로컬 `datetime-local` 문자열로 생성한다.

## 남은 확인

- 실제 MySQL에 schema/migration 적용 후 `demo_access_code`와 `contest` source 컬럼 확인.
- `slate.demo-access.enabled=true`, `VITE_DEMO_ACCESS_GATE=true` 환경에서 접근 코드 인증, 만료 코드 거절, 이미지 blob 로딩 확인.
- 관리자에서 dry-run 크롤러 실행 후 결과 메트릭과 운영 로그 확인.
- 게시판/프로필/팀 등 공모전 외 화면의 보호 이미지 URL은 브라우저 smoke 중 실패가 확인되면 `ProtectedImage` 추가 적용.

## 참조 경로

- `docu/README.md`
- `docu/00_common/document_structure.md`
- `docu/00_common/reference_policy.md`
- `docu/handoff/user2_crawler_and_filter_port_2026-06-24.md`
- `docu/handoff/demo_access_first_filter_porting_guide.md`
- `backend/src/main/java/com/slate/contests`
- `backend/src/main/java/com/slate/security`
- `frontend/src/views/AdminView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/services/api.js`
- `sql`
