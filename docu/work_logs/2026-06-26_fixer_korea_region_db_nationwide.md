# 지역 DB 전역화와 관리자 지역 관리 작업 로그

작성일: 2026-06-26

## 문서 작성 기준

- `Agent.md`의 작업 로그 규칙에 따라 `docu/work_logs/YYYY-MM-DD_{role}_{summary}.md` 형식으로 작성한다.
- 이번 대화에서 실제 확인, 구현, 생성, 검증한 내용을 중심으로 기록한다.
- 구현된 것, 실제 DB 적용이 필요한 것, 실패하거나 미수행한 검증을 분리한다.
- 실제 `.env`, DB 비밀번호, API key, JWT, 접속 코드 평문 등 secret 값은 기록하지 않는다.
- 외부 데이터 출처는 공개 출처와 상대 경로 기준으로만 남긴다.

## 작업 범위

- 대상 DB:
  - `region`
  - `admin_permission`
  - `common_code`
  - `member_profile.region_id`
  - `team.region_id`
- 대상 기능:
  - 전국 시군구 지역 seed SQL 생성
  - 기존 일부 더미 지역과 신규 전국 지역 데이터 정리
  - 관리자 지역 DB 조회/수정 API
  - 관리자 화면 지역 DB 관리 패널
- 대상 문서/자료:
  - `docu/prompt/korea_region_dataset_research_prompt.md`
  - `assets/지역 DB 전역화 심층 리서치.md`
  - `assets/vworld/LT_C_ADSIGG_INFO/국가기본도_시군구구역경계`
- 제외 범위:
  - 실제 운영 DB에 SQL 실행
  - VWorld API key 기반 live API 호출
  - PostGIS 기반 point-on-surface 산출
  - 관리자 화면 실제 로그인 계정 브라우저 E2E

## 사전 검토와 프롬프트

- 현재 `region` seed가 12개 일부 지역만 갖고 있고, 매칭 거리 계산이 `region.center_lat`, `region.center_lng`를 사용한다는 점을 확인했다.
- `shooting_location` 계열 촬영지 CSV/DB와 `region` 기준정보는 목적이 달라 섞지 않는 방향으로 정리했다.
- Web ChatGPT Pro 심층 리서치용 프롬프트를 `docu/prompt/korea_region_dataset_research_prompt.md`에 작성했다.
- 프롬프트와 함께 업로드할 관련 폴더 목록을 정리했다.
- 사용자가 저장한 `assets/지역 DB 전역화 심층 리서치.md`를 검토하고, 실제 링크/출처 검증이 필요한 항목과 적용 방향을 분리했다.

## 데이터 소스 결정

- 지역 코드 정본은 행정표준코드관리시스템 법정동코드 목록을 사용했다.
  - 접근 경로: `https://www.code.go.kr/stdcode/regCodeL.do`
  - `pageSize=30000` 조회로 현행 법정동 목록 23,848건을 확인했다.
  - 시군구 단위 중 하위 구가 있는 상위 시 행은 제외하고 leaf 시군구 259개를 선택했다.
- 좌표 산출은 사용자가 제공한 VWorld SHP 파일을 사용했다.
  - 사용자가 추가한 데이터: `assets/vworld/LT_C_ADSIGG_INFO/국가기본도_시군구구역경계.zip`
  - 해제 결과: `assets/vworld/LT_C_ADSIGG_INFO/국가기본도_시군구구역경계/TN_SIGNGU_BNDRY.*`
  - 테이블 정의서: `assets/vworld/LT_C_ADSIGG_INFO/국가기본도_시군구구역경계_테이블정의서.xlsx`
- DBF 필드 확인 결과:
  - 코드 필드: `LEGLCD_SE`
  - 명칭 필드: `ADZONE_NM`
  - 좌표계: `Korea_2000_Korea_Unified_Coordinate_System`
- SHP와 현행 코드 대조 결과 VWorld SHP에 없는 현행 코드 8개를 확인했다.
  - 인천광역시 제물포구, 영종구, 서해구, 검단구
  - 경기도 화성시 만세구, 효행구, 병점구, 동탄구
- 누락 8개는 공개 OSM 계열 좌표를 fallback으로 SQL 주석에 명시했다.

## 반영 내용

### 전국 지역 SQL 생성

- `tools/generate-korea-region-sql.js`를 추가했다.
- 생성기는 다음 작업을 수행한다.
  - `code.go.kr` 법정동코드 HTML에서 현행 leaf 시군구 목록 추출
  - `TN_SIGNGU_BNDRY.dbf`에서 코드/명칭 읽기
  - `TN_SIGNGU_BNDRY.shp`에서 polygon 조각을 읽고 코드별 가장 큰 polygon의 centroid 계산
  - Korea 2000 Unified Coordinate System 좌표를 WGS84 위경도로 변환
  - VWorld 누락 8개 현행 코드에 fallback 좌표 적용
  - `sql/27_seed_korea_regions.sql` 생성
- `sql/27_seed_korea_regions.sql`은 현행 leaf 시군구 259개를 upsert한다.
- 사용자의 최종 지시에 따라 기존 12개 더미 지역은 보존하지 않는다.
  - 새 전국 데이터 삽입 또는 갱신
  - 기존 더미 행을 참조하던 `member_profile.region_id`, `team.region_id`를 같은 `public_display_name`의 새 시군구 행으로 재매핑
  - 재매핑 후 기존 더미 행 삭제
- `sql/28_validate_korea_regions.sql`을 추가했다.
  - 활성 지역 수
  - 시도별 지역 수
  - 좌표 범위 이상값
  - 코드/표시명 중복
  - 표시명 중복

### 관리자 권한과 seed 보강

- `REGION_MANAGE` 관리자 세부 권한을 추가했다.
- `sql/02_seed_reference.sql`의 `ADMIN_PERMISSION` 공통 코드에 `REGION_MANAGE`를 추가했다.
- `sql/03_seed_sample_data.sql`의 샘플 관리자 계정 권한에 `REGION_MANAGE`를 추가했다.
- `sql/21_seed_connected_demo_volume_data.sql`의 볼륨 더미 관리자 권한에 `REGION_MANAGE`를 추가했다.
- `sql/22_validate_connected_demo_volume_data.sql`의 관리자 권한 기대 개수를 8개에서 9개로 갱신했다.
- `sql/27_seed_korea_regions.sql`은 기존 관리자 계정에도 `REGION_MANAGE` 권한을 부여한다.

### 관리자 지역 API

- `/api/admin/regions` 관리자 API를 추가했다.
- 추가 파일:
  - `backend/src/main/java/com/slate/references/AdminRegionController.java`
  - `backend/src/main/java/com/slate/references/AdminRegionService.java`
  - `backend/src/main/java/com/slate/references/AdminRegionMapper.java`
  - `backend/src/main/resources/mappers/AdminRegionMapper.xml`
- API 기능:
  - 지역 목록 조회
  - 지역 요약 조회
  - 지역명, 좌표, 표시명, 활성 상태 수정
- 권한 정책:
  - `hasRole('ADMIN')`
  - `AdminPermissionCatalog.REGION_MANAGE`
- 수정 API는 관리자 처리 사유를 필수로 받고 `AuditLogService`에 감사/운영 로그를 남긴다.
- 좌표 입력은 대한민국 서비스 범위 기준으로 대략 검증한다.
  - 위도: 33.0-39.5
  - 경도: 124.0-132.5

### 기존 기준정보 조회 보정

- `ReferenceMapper.xml`의 지역 검색식을 `CONCAT(...)`에서 `CONCAT_WS(...)`로 변경했다.
- 시군구 단위 seed에서 `dong_name`을 빈 문자열로 넣어도 검색식이 null/빈 값에 덜 취약하도록 정리했다.

### 관리자 화면

- `/admin/regions` 라우트를 추가했다.
- `frontend/src/services/api.js`에 관리자 지역 API 함수를 추가했다.
- `frontend/src/views/AdminView.vue`에 지역 DB 관리 패널을 추가했다.
- 화면 기능:
  - 검색어, 시도, 활성 상태 필터
  - 전체/활성/비활성/시도 수와 좌표 범위 요약
  - 지역 코드, 표시명, 시도, 시군구, 위도, 경도, 활성 상태 표시
  - 프로필/팀 참조 수 표시
  - 사유 입력 후 지역 정보 저장
- `frontend/src/styles/slate.css`에 지역 관리 테이블 폭과 입력 요소 스타일을 보강했다.

## 검증 결과

### SQL 정적 검증

- 생성된 `sql/27_seed_korea_regions.sql`을 Node 스크립트로 정적 검증했다.
- 결과:
  - 지역 row: 259개
  - 고유 코드: 259개
  - 고유 표시명: 259개
  - 좌표 범위 이상값: 0개
  - 기존 더미 코드 12개: 포함되지 않음
  - 기존 더미 표시명에 대응되는 새 시군구 코드 12개: 모두 포함
  - `member_profile`, `team` 재매핑 구문 포함
  - 기존 더미 행 삭제 구문 포함

### 프론트엔드 빌드

- 명령: `npm run build` in `frontend`
- 결과: 성공
- 참고:
  - Vite chunk size warning은 기존 번들 크기 경고이며 빌드는 통과했다.

### 백엔드 검증

- 첫 `mvn test`는 샌드박스 네트워크 제한으로 Maven Central parent POM 접근에 실패했다.
- 네트워크 권한을 허용한 뒤 `mvn test`를 다시 실행했다.
  - 메인 소스 컴파일은 통과했다.
  - testCompile 단계에서 기존 `BoardServiceFullIntegrationTest`의 `BoardMapper.selectWorkRanking` 시그니처 불일치로 실패했다.
  - 해당 실패는 이번 지역 DB 변경과 무관한 기존 테스트 코드 문제다.
- `mvn "-Dmaven.test.skip=true" package`는 jar 생성 후 Spring Boot repackage에서 기존 jar rename 잠금 문제로 실패했다.
- `mvn compile`은 성공했다.
- 이후 병렬 재실행 중 sandbox network 제한으로 `mvn compile`이 다시 Maven Central 접근 실패를 냈으나, 네트워크 허용 상태의 `mvn compile` 성공 결과를 백엔드 메인 컴파일 검증으로 기록한다.

## 이번 작업에서 의도적으로 변경하지 않은 내용

- 실제 MySQL DB에 `sql/27_seed_korea_regions.sql`을 실행하지 않았다.
- 실제 운영 관리자 계정으로 `/admin/regions` 브라우저 E2E를 수행하지 않았다.
- VWorld API key나 외부 API secret을 문서화하지 않았다.
- PostGIS `ST_PointOnSurface` 기반 내부점 산출은 도입하지 않았다.
- `region` 테이블의 `dong_name NOT NULL` DDL은 변경하지 않았다. 호환을 위해 시군구 seed는 빈 문자열을 사용한다.
- `shooting_location` 촬영지 데이터와 `region` 기준정보를 통합하지 않았다.

## 남은 확인 사항

- 실제 MySQL에 `sql/27_seed_korea_regions.sql`을 적용하고 `sql/28_validate_korea_regions.sql` 결과를 확인해야 한다.
- 기존 더미 12개를 참조하던 프로필/팀이 새 시군구 행으로 재매핑되는지 실제 DB에서 확인해야 한다.
- VWorld SHP에 없는 현행 신설 구 8개의 fallback 좌표는 관리자 페이지에서 운영자가 보정할 수 있다.
- 실제 관리자 계정으로 `/admin/regions` 목록 조회와 좌표 수정, 감사 로그 기록을 브라우저에서 확인해야 한다.
- 기존 `BoardServiceFullIntegrationTest`의 `selectWorkRanking` 호출 시그니처 불일치는 별도 테스트 정리 작업이 필요하다.
- jar repackage rename 실패는 로컬 jar 잠금 또는 실행 중 프로세스 영향일 수 있어, 배포 패키징 전 별도 확인이 필요하다.

## 변경 파일

- `docu/prompt/korea_region_dataset_research_prompt.md`
- `tools/generate-korea-region-sql.js`
- `sql/02_seed_reference.sql`
- `sql/03_seed_sample_data.sql`
- `sql/21_seed_connected_demo_volume_data.sql`
- `sql/22_validate_connected_demo_volume_data.sql`
- `sql/27_seed_korea_regions.sql`
- `sql/28_validate_korea_regions.sql`
- `backend/src/main/java/com/slate/admin/AdminPermissionCatalog.java`
- `backend/src/main/java/com/slate/references/AdminRegionController.java`
- `backend/src/main/java/com/slate/references/AdminRegionService.java`
- `backend/src/main/java/com/slate/references/AdminRegionMapper.java`
- `backend/src/main/resources/mappers/AdminRegionMapper.xml`
- `backend/src/main/resources/mappers/ReferenceMapper.xml`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/views/AdminView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-26_fixer_korea_region_db_nationwide.md`
- `docu/13_work_status/current_and_completed_work.md`

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_common/document_structure.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/07_database/database_baseline.md`
- `docu/13_work_status/current_and_completed_work.md`
- `docu/prompt/korea_region_dataset_research_prompt.md`
- `assets/지역 DB 전역화 심층 리서치.md`
- `assets/vworld/LT_C_ADSIGG_INFO/국가기본도_시군구구역경계/TN_SIGNGU_BNDRY.shp`
- `assets/vworld/LT_C_ADSIGG_INFO/국가기본도_시군구구역경계/TN_SIGNGU_BNDRY.dbf`
- `assets/vworld/LT_C_ADSIGG_INFO/국가기본도_시군구구역경계/TN_SIGNGU_BNDRY.prj`
- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`
- `backend/src/main/resources/mappers/ReferenceMapper.xml`
- `frontend/src/views/AdminView.vue`
