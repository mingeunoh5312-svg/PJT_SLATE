# user2 크롤러·1차 필터 이식 기준

## 목적

현재 기준 브랜치 `dev_Slate_0624_user1_v2`에 팀원 작업물을 선별 이식하기 위한 구조화 기준이다. 기능 동작은 각 기능 브랜치의 구현을 우선한다.

| 기능 | 우선 기준 브랜치 | 반영 상태 |
|---|---|---|
| 콘테스트코리아 크롤링 | `origin/dev_Slate_0624_user2` | 구현됨 |
| 크롤링 출처/포스터/목록 필터 | `origin/dev_Slate_0624_user2` | 구현됨 |
| Demo Access 1차 필터 | `origin/dev_Slate_0624_user2_filter` | 구현됨 |

## 이식 원칙

- 현재 코드 기준은 `dev_Slate_0624_user1_v2`다.
- 단, 크롤링 관련 필터/출처/포스터 정책은 `dev_Slate_0624_user2` 구현을 우선했다.
- Demo Access 1차 필터는 `dev_Slate_0624_user2_filter` 구현을 우선했다.
- 직접 merge/cherry-pick은 하지 않았다. 두 브랜치가 현재 브랜치와 달라 전체 merge 시 user1_v2 작업 일부가 되돌아갈 수 있어 파일 단위로 선별 반영했다.
- 임시 추출 파일은 `codex-tmp/branch-import` 아래에 두었다. 실제 기능 파일은 `backend`, `frontend`, `sql`, `docu` 아래에 반영했다.

## 구현됨

### 콘테스트코리아 크롤러

- `backend/src/main/java/com/slate/contests/ContestKorea*` 크롤링, 파싱, 정규화, 포스터 저장, upsert 서비스를 추가했다.
- `backend/pom.xml`에 `org.jsoup:jsoup:1.22.2`를 추가했다.
- `AdminContestController`에 `POST /api/admin/contests/crawl-sources/contest-korea/run`을 추가했다.
- `ContestMapper`/`ContestMapper.xml`에 출처 기반 조회와 crawler upsert를 추가했다.
- `application*.yml`에 `slate.public-data.contest-korea` 설정 블록을 추가했다.
- `sql/01_schema.sql`과 `sql/15_contest_crawl_source_schema.sql`에 포스터/출처 컬럼과 `uk_contest_source_external`을 반영했다.
- `sql/16_contest_official_link_cleanup.sql`로 콘테스트코리아 원문 URL이 공식 링크로 중복 저장된 경우 정리할 수 있게 했다.

### 크롤링 필터·출처 UI

- 목록 필터에 `deadlineWithinDays`와 `regionMode`를 추가했다.
- `ContestFilterCatalog`, `ContestSearchCriteria`, `ContestService`, `ContestMapper.xml`에 user2 브랜치의 기간/대표 지역 필터 조건을 반영했다.
- `frontend/src/constants/contestFilters.js`에 목록 전용 지역 옵션과 마감 기간 옵션을 추가했다.
- `ContestView.vue`에서 수집 출처, 원문 링크, 포스터 출처, 수집일을 목록/상세에 표시한다.
- 콘테스트코리아 원문 URL은 공식 공고 링크와 분리해 표시한다.
- `/api/media/images/...` 보호 게이트 대응을 위해 공모전/홈 공모전 이미지는 `ProtectedImage`를 사용한다.

### Demo Access 1차 필터

- DB 관리형 접근 코드 테이블과 mapper/service/controller를 추가했다.
- `DemoAccessGateService`를 통해 fallback 코드와 DB 코드를 함께 검증한다.
- `DemoAccessController`와 `DemoAccessFilter`는 gate service를 사용한다.
- 관리자 권한 `DEMO_ACCESS_MANAGE`와 `/admin/demo-access` UI를 추가했다.
- 프론트 API 실패 시 `slate-demo-access-rejected` 이벤트로 접근 코드 화면으로 되돌린다.
- `DemoAccessView.vue`는 user2_filter 브랜치의 “접근 코드” 용어와 에러 정규화 기준을 따른다.

## 부분 구현 또는 주의

| 항목 | 상태 | 메모 |
|---|---|---|
| 실제 콘테스트코리아 live crawl | 미수행 검증 | 네트워크/대상 사이트 상태에 의존하므로 단위 테스트 중심으로 확인했다. |
| 전체 화면 보호 이미지 치환 | 부분 구현 | 공모전과 홈 공모전 이미지는 적용했다. 게시판/프로필/팀 이미지 중 API 이미지 URL은 후속 화면 smoke에서 추가 치환이 필요할 수 있다. |
| 실제 MySQL migration 적용 | 미수행 검증 | SQL 파일 작성과 테스트 컴파일은 확인했지만 실제 DB 적용은 수행하지 않았다. |
| 브라우저 smoke | 미수행 검증 | 이번 작업은 build/test 중심으로 검증했다. |

## 검증

| 명령 | 결과 |
|---|---|
| `npm run build` in `frontend` | 통과 |
| `mvn "-Dtest=ContestKorea*Test,AdminContestKoreaCrawlerServiceTest,DemoAccess*Test,SecurityConfigTest" test` in `backend` | 통과, 128 tests |

## 후속 권장 검증

- 로컬 MySQL에 `sql/15_contest_crawl_source_schema.sql`, `sql/16_contest_official_link_cleanup.sql`, `sql/17_demo_access_code_management_schema.sql` 적용 확인.
- `VITE_DEMO_ACCESS_GATE=true`와 `slate.demo-access.enabled=true` 조합으로 `/`, `/contests`, `/admin/demo-access`, 이미지 로딩 smoke.
- 관리자 계정으로 dry-run 크롤러 실행 후 `contestCrawlerResult`와 운영 로그 확인.
- 저장 실행은 실제 사이트 이용 정책과 수집 범위를 재확인한 뒤 제한 건수로 smoke.

## 참조 경로

- `docu/README.md`
- `docu/00_common/document_structure.md`
- `docu/00_common/reference_policy.md`
- `docu/handoff/demo_access_first_filter_porting_guide.md`
- `backend/src/main/java/com/slate/contests`
- `backend/src/main/java/com/slate/security`
- `backend/src/main/resources/mappers/ContestMapper.xml`
- `backend/src/main/resources/mappers/DemoAccessCodeMapper.xml`
- `frontend/src/views/AdminView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/DemoAccessView.vue`
- `frontend/src/services/api.js`
- `sql/01_schema.sql`
- `sql/15_contest_crawl_source_schema.sql`
- `sql/16_contest_official_link_cleanup.sql`
- `sql/17_demo_access_code_management_schema.sql`
