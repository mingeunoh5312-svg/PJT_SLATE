# Demo media, region DB, matching filter follow-up 작업 로그

작성일: 2026-06-26

## 문서 작성 기준

- `Agent.md`의 작업 로그 규칙에 따라 `docu/work_logs/YYYY-MM-DD_{role}_{summary}.md` 형식으로 작성한다.
- 이번 대화에서 실제 확인, 구현, DB 적용, 서버 재시작, 검증한 내용을 중심으로 기록한다.
- 구현된 것, 실제 DB에 적용한 것, 실패 후 보완한 것, 남은 확인 사항을 분리한다.
- 실제 `.env`, DB 비밀번호, OpenAI key, Demo Access 접속 코드 평문 등 secret 값은 기록하지 않는다.
- 로컬 업로드 루트와 포트는 환경 정보로만 기록하고, 실제 사용자 업로드 경로를 침해하지 않는 방향을 명시한다.

## 작업 범위

- 대상 로컬 앱:
  - frontend `5174`
  - backend `8080`
  - local MySQL `slate`
- 대상 기능:
  - Demo Access 1차 필터와 더미 이미지 표시
  - 생성 더미 이미지 runtime 파일 배치
  - 지역 DB 전역화 SQL 실제 DB 적용
  - CDV 볼륨 더미 데이터 재적용 가능성 보완
  - 관리자 지역 DB API 확인
  - 매칭 페이지 지역 필터를 `지역 / 세부 입력` 구조로 변경
- 제외 범위:
  - 실제 secret 문서화
  - 운영 업로드 루트 변경
  - 전체 브라우저 E2E 회귀
  - 전체 Maven 테스트 실행

## 반영 내용

### 더미 이미지 표시 문제 원인 확인과 수정

- DB에는 `sql/27_apply_generated_dummy_images.sql` 기준 이미지 경로가 이미 반영되어 있음을 확인했다.
- 이미지 경로 count 확인 결과:
  - CDD profile 8건
  - CDV profile 32건
  - team 14건
  - work 37건
  - portfolio 66건
  - contest request 7건
  - contest 25건
- 실제 사이트에서 이미지가 보이지 않던 원인은 두 가지였다.
  - Demo Access filter가 브라우저 `<img>`의 `GET /api/media/images/**` 요청에도 접속 코드를 요구해 403을 반환했다.
  - DB 상대 경로 `images/seed/...`가 백엔드 업로드 루트 기준인데, 생성 이미지 파일은 `Slate/uploads/images/seed`에 있어 backend runtime upload root에서 404가 발생했다.
- `DemoAccessGateService`에서 `GET /api/media/images/**`는 Demo Access 코드 요구 대상에서 제외했다.
- `DemoAccessGateServiceTest`에 public media image GET 예외 테스트를 추가했다.
- `SLATE_UPLOAD_DIR`은 `uploads`로 유지했다.
  - `SLATE_UPLOAD_DIR=Slate/assets` 또는 `../uploads`로 변경하지 않았다.
  - 실제 사용자 업로드 경로 정책을 침해하지 않기 위해 더미 seed 이미지 파일만 `backend/uploads/images/seed` 하위로 복사했다.
  - `backend/uploads`는 runtime 산출물이며 Git 추적 대상에 포함하지 않았다.

### 지역 DB 전역화 SQL 실제 적용

- 기존 DB 상태:
  - 활성 지역 12건
  - 프로필/팀 지역 참조 누락 0건
- `sql/27_seed_korea_regions.sql`을 실제 local MySQL `slate` DB에 적용했다.
- 적용 후 상태:
  - 활성 지역 259건
  - 프로필 지역 참조 누락 0건
  - 팀 지역 참조 누락 0건
  - 관리자 `REGION_MANAGE` 활성 권한 2건
- `sql/28_validate_korea_regions.sql`을 실행했다.
  - 활성 지역 259건
  - 시도별 분포 정상
  - 좌표 범위 이상값과 중복 결과는 출력되지 않았다.

### CDV 볼륨 더미 데이터 재적용 보완

- `sql/21_seed_connected_demo_volume_data.sql` 재실행 중 `notification.recipient_user_id` 외래키 오류가 발생했다.
- 원인:
  - CDV 계정을 삭제하기 전에 CDV 사용자가 수신자 또는 발신자인 일반 알림이 모두 정리되지 않았다.
- `sql/21_seed_connected_demo_volume_data.sql`의 CDV namespace cleanup에서 `notification` 삭제 조건을 보강했다.
  - title `[CDV]%`
  - recipient가 `cdv-%` 계정
  - sender가 `cdv-%` 계정
- 같은 정리 로직을 `sql/23_rollback_connected_demo_volume_data.sql`에도 반영했다.
- 보완 후 `sql/21_seed_connected_demo_volume_data.sql` 재실행이 성공했다.
- 볼륨 더미 재시드 후 이미지 경로가 초기화될 수 있어 `sql/27_apply_generated_dummy_images.sql`을 다시 적용했다.
- `sql/22_validate_connected_demo_volume_data.sql` 검증 결과:
  - CDV expected counts 모두 기대값과 일치
  - CDV zero-error checks 모두 0
  - CDD guard counts 모두 기대값과 일치
  - generated dummy image counts 모두 expected count와 일치

### 관리자 지역 DB API 확인

- 백엔드 jar를 재빌드하고 `8080` 포트로 재시작했다.
- 샘플 관리자 계정으로 로그인 후 `/api/admin/regions/summary`, `/api/admin/regions`를 호출했다.
- 확인 결과:
  - active region 259건
  - inactive region 0건
  - 목록 조회 정상
- `/api/references/regions?limit=300`도 259건을 반환하는 것을 확인했다.

### 매칭 페이지 지역 필터 재구성

- 매칭 페이지의 팀원 찾기와 팀 찾기 필터에서 기존 단일 `regionIds` 선택 UI를 `AI 로케이션 탐색`의 `지역 / 세부 입력` 구조로 변경했다.
- 프론트 상태를 다음과 같이 분리했다.
  - 상위 지역: `selectedTopRegions`, URL query `regionSidos`
  - 세부 지역: `selectedRegionIds`, URL query `regionIds`
- 지역 목록 로딩은 전국 259개 지역을 사용할 수 있도록 `slateApi.regions('', 1000)`로 조정했다.
- 세부 입력 후보는 상위 지역 선택이 있을 경우 해당 시도에 포함되는 시군구만 보여준다.
- 상위 지역과 세부 지역은 복수 선택 가능하며, 선택 chip은 시도 정렬 기준으로 묶어 표시한다.
- 선택 후 입력 포커스를 해제해 같은 입력칸을 다시 클릭할 때 드롭다운이 다시 열리도록 했다.
- 팀 찾기에서 상위 지역 필터가 동작하도록 `MatchingMapper.xml`의 팀/슬롯 후보 응답에 `sidoName`, `sigunguName`을 추가했다.
- `MatchingService`의 지역 필터는 다음 OR 조건으로 처리한다.
  - 세부 지역 ID가 후보의 `regionId`와 일치
  - 상위 지역명이 후보의 `sidoName`과 일치
- 팔로우한 회원 목록의 로컬 필터도 같은 지역 선택 조건을 사용하도록 보정했다.

### 테스트 시그니처 보정

- 백엔드 패키징 중 기존 `BoardServiceFullIntegrationTest`가 `BoardMapper.selectWorkRanking` 변경된 시그니처를 따라가지 못해 testCompile에서 실패했다.
- 테스트 mock/verify 호출부에 `currentUserId` 인자를 반영했다.
- 이 보정 후 `mvn -DskipTests package`가 성공했다.

## 검증 결과

### 더미 이미지 API

- Demo Access filter 예외와 runtime seed 파일 배치 후 다음 media image endpoint가 200 `image/png`를 반환했다.
  - `/api/media/images/profile/27`
  - `/api/media/images/profile/28`
  - `/api/media/images/contest/3`
  - `/api/media/images/contest/4`
  - `/api/media/images/team/8`
  - `/api/media/images/work/8`
- `profile/1`, `contest/1`의 404는 해당 ID가 seed image target이 아니어서 발생한 정상 범위로 판단했다.

### SQL 검증

- `sql/27_seed_korea_regions.sql` 적용 결과: active region 259건
- `sql/28_validate_korea_regions.sql` 실행 결과: 시도별 분포 정상
- `sql/21_seed_connected_demo_volume_data.sql` 재적용 결과: 성공
- `sql/27_apply_generated_dummy_images.sql` 재적용 결과: generated image path count가 expected count와 일치
- `sql/22_validate_connected_demo_volume_data.sql` 실행 결과: CDV zero-error checks 모두 0

### 프론트엔드 빌드

- 명령: `npm run build` in `frontend`
- 결과: 성공
- 참고:
  - Vite chunk size warning은 기존 번들 크기 경고이며 빌드는 통과했다.

### 백엔드 빌드

- 명령: `mvn "-Dtest=DemoAccessGateServiceTest" test`
- 결과: 성공
- 명령: `mvn "-DskipTests" package`
- 결과: 성공
- 참고:
  - jar가 실행 중이면 Windows 파일 잠금으로 package repackage가 실패할 수 있어, 백엔드 프로세스 중지 후 재실행했다.

### 매칭 지역 필터 API 확인

- 샘플 일반 사용자 계정으로 로그인 후 매칭 API를 호출했다.
- `team-to-members` 확인 결과:
  - 전체: 54건
  - `regionSidos=경기도`: 12건
  - `regionSidos=서울특별시`: 28건
- `regionSidos=경기도` 응답 후보의 `publicRegionName`, `sidoName`이 경기도 기준으로 필터링되는 것을 확인했다.

### 서버 상태

- 프론트엔드:
  - `http://localhost:5174`
  - 응답 200 확인
- 백엔드:
  - `http://localhost:8080`
  - 새 jar로 재시작 완료
  - `/api/references/regions?limit=300` 259건 반환 확인

## 이번 작업에서 의도적으로 변경하지 않은 내용

- 실제 `.env`, DB 비밀번호, OpenAI key, Demo Access 접속 코드 평문은 문서에 기록하지 않았다.
- `SLATE_UPLOAD_DIR`을 `Slate/assets` 또는 다른 전역 자산 경로로 바꾸지 않았다.
- 실제 사용자가 업로드하는 프로필/팀/작업물 파일 저장 정책은 변경하지 않았다.
- 더미 seed 이미지 파일 복사는 runtime 확인용으로만 처리했고, Git 추적 대상 산출물로 기록하지 않았다.
- 매칭 점수 계산 가중치나 AI 추천 정책은 이번 지역 필터 UI 변경 범위에서 수정하지 않았다.
- 전체 브라우저 E2E 회귀는 수행하지 않았다.

## 남은 확인 사항

- 실제 브라우저에서 매칭 페이지의 `지역 / 세부 입력` 드롭다운, 다중 선택 chip, URL 복원, 초기화 버튼을 시각 확인해야 한다.
- 팀 찾기에서 `지역 무관` 팀을 상위 지역 필터에 포함할지 제외할지는 정책 확인이 필요하다. 현재 구현은 지역 필터가 있으면 `sidoName` 또는 `regionId`가 일치하는 후보만 통과한다.
- 생성 더미 이미지가 브라우저 캐시에 의해 이전 fallback 이미지로 보일 수 있으므로, 사이트 확인 시 hard refresh가 필요할 수 있다.
- 전체 Maven test는 실행하지 않았고, 이번 작업에서는 targeted test와 package를 기준으로 검증했다.
- `backend/uploads/images/seed` runtime 파일은 로컬 실행용이므로 다른 환경에서는 같은 경로 복사 또는 배포 자산 절차가 필요하다.

## 변경 파일

- `backend/src/main/java/com/slate/security/DemoAccessGateService.java`
- `backend/src/test/java/com/slate/security/DemoAccessGateServiceTest.java`
- `backend/src/test/java/com/slate/boards/BoardServiceFullIntegrationTest.java`
- `backend/src/main/java/com/slate/matching/MatchingService.java`
- `backend/src/main/resources/mappers/MatchingMapper.xml`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`
- `sql/21_seed_connected_demo_volume_data.sql`
- `sql/23_rollback_connected_demo_volume_data.sql`
- `docu/work_logs/2026-06-26_fixer_demo_media_region_matching_followup.md`

## 관련 적용 파일과 SQL

- `sql/27_seed_korea_regions.sql`
- `sql/28_validate_korea_regions.sql`
- `sql/27_apply_generated_dummy_images.sql`
- `sql/22_validate_connected_demo_volume_data.sql`
- `backend/uploads/images/seed` runtime seed image files

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/document_structure.md`
- `docu/00_common/reference_policy.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/13_work_status/current_and_completed_work.md`
- `docu/work_logs/2026-06-25_fixer_team_recruitment_location_ai.md`
- `docu/work_logs/2026-06-26_applier_generated_dummy_images.md`
- `docu/work_logs/2026-06-26_fixer_korea_region_db_nationwide.md`
- `backend/src/main/java/com/slate/security/DemoAccessGateService.java`
- `backend/src/main/java/com/slate/matching/MatchingService.java`
- `backend/src/main/resources/mappers/MatchingMapper.xml`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/views/LocationExploreView.vue`
- `frontend/src/styles/slate.css`
- `sql/21_seed_connected_demo_volume_data.sql`
- `sql/23_rollback_connected_demo_volume_data.sql`
- `sql/27_seed_korea_regions.sql`
- `sql/27_apply_generated_dummy_images.sql`
