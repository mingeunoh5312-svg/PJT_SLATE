# 대한민국 전역 지역 기준 데이터 리서치 프롬프트

아래 내용을 웹 ChatGPT Pro 확장 모델에 그대로 입력하세요. 가능하면 Deep Research와 웹 검색을 켠 상태로 수행하고, 파일 업로드/데이터 분석 기능이 있으면 이 저장소의 관련 파일을 함께 업로드해 현재 구조를 대조하세요.

---

당신은 Slate 프로젝트의 DB 기준 데이터와 위치/거리 계산 설계를 검토하는 리서처입니다. 목표는 현재 일부 지역만 저장된 `region` 기준 데이터를 대한민국 전역을 반영하는 안정적인 데이터셋으로 확장하는 방법을 찾는 것입니다. 단순 제안이 아니라 공식 출처, 라이선스, 데이터 정제 방식, DB 반영 전략, 검증 쿼리까지 제시하세요.

## 프로젝트 맥락

- 작업 루트는 각 작업자의 로컬 `Project_Slate/Slate` 폴더입니다.
- 현재 프로젝트는 Spring Boot, MyBatis, MySQL 8, Vue/Vite를 사용합니다.
- 지역 기준 데이터는 매칭, 팀 생성/수정, 프로필, 로케이션 추천 화면에서 사용됩니다.
- 현재 `region` seed는 대한민국 전체가 아니라 일부 지역 12개 행만 포함합니다.
- 별도로 `assets/영화 로케이션 촬영 이력.csv` 기반 촬영지 데이터 import가 있으며, 이 데이터는 `shooting_location`/`shooting_location_history`로 적재됩니다. 이 촬영지 데이터와 매칭용 행정구역 기준 데이터는 목적이 다릅니다.

## 현재 확인된 구조

### `sql/01_schema.sql`

`region` 테이블:

```sql
CREATE TABLE IF NOT EXISTS region (
  region_id bigint NOT NULL AUTO_INCREMENT,
  region_code varchar(20) NOT NULL,
  sido_name varchar(50) NOT NULL,
  sigungu_name varchar(80) NOT NULL,
  dong_name varchar(80) NOT NULL,
  center_lat decimal(10,7) NOT NULL,
  center_lng decimal(10,7) NOT NULL,
  public_display_name varchar(150) NOT NULL,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (region_id),
  UNIQUE KEY uk_region_code (region_code),
  KEY idx_region_public (public_display_name),
  KEY idx_region_coord (center_lat, center_lng)
);
```

주요 FK:

- `member_profile.region_id`는 `region.region_id`를 참조합니다.
- `team.region_id`는 nullable이며 `region.region_id`를 참조합니다.

### `sql/02_seed_reference.sql`

현재 `region` seed는 12개 행뿐입니다.

```sql
INSERT INTO region (region_code, sido_name, sigungu_name, dong_name, center_lat, center_lng, public_display_name) VALUES
('1111010100', '서울특별시', '종로구', '청운효자동', 37.5840090, 126.9706260, '서울특별시 종로구'),
('1114016200', '서울특별시', '중구', '명동', 37.5636560, 126.9850280, '서울특별시 중구'),
('1168010100', '서울특별시', '강남구', '역삼동', 37.5006130, 127.0364310, '서울특별시 강남구'),
('1144012000', '서울특별시', '마포구', '서교동', 37.5550200, 126.9224310, '서울특별시 마포구'),
('1121510100', '서울특별시', '광진구', '화양동', 37.5445810, 127.0708090, '서울특별시 광진구'),
('4113510800', '경기도', '성남시 분당구', '삼평동', 37.4000590, 127.1086220, '경기도 성남시 분당구'),
('4128112800', '경기도', '고양시 덕양구', '화정동', 37.6375710, 126.8326120, '경기도 고양시 덕양구'),
('2611014000', '부산광역시', '중구', '남포동', 35.0981850, 129.0325710, '부산광역시 중구'),
('2711015700', '대구광역시', '중구', '삼덕동', 35.8658440, 128.6085560, '대구광역시 중구'),
('2915510900', '광주광역시', '남구', '양림동', 35.1396990, 126.9120940, '광주광역시 남구'),
('3011014000', '대전광역시', '동구', '대동', 36.3293670, 127.4433150, '대전광역시 동구'),
('5011013700', '제주특별자치도', '제주시', '이도이동', 33.4996210, 126.5311880, '제주특별자치도 제주시');
```

### 매칭/거리 계산 영향

`backend/src/main/java/com/slate/matching/MatchingService.java`는 `centerLat`, `centerLng`를 받아 haversine 거리 점수를 계산합니다.

- 팀 지역 무관 또는 프로필 `travelRange=ANYWHERE`면 거리 점수 1.0
- 좌표가 없으면 0.7 fallback
- `KM_10`, `KM_30`, `KM_100`, `ANYWHERE` 기준으로 점수 산정
- 따라서 `region`이 일부 지역만 있으면 지역 선택지, 지역 필터, 거리 점수 모두 왜곡됩니다.

### 참조 API/UI 영향

`backend/src/main/resources/mappers/ReferenceMapper.xml`:

```sql
SELECT
  region_id AS regionId,
  region_code AS regionCode,
  sido_name AS sidoName,
  sigungu_name AS sigunguName,
  dong_name AS dongName,
  center_lat AS centerLat,
  center_lng AS centerLng,
  public_display_name AS publicDisplayName
FROM region
WHERE active_yn = 'Y'
ORDER BY sido_name, sigungu_name, dong_name
LIMIT #{limit}
```

`ReferenceService.regions()`는 limit를 최대 100으로 제한합니다. 전국 시군구가 200개 이상이면 UI/API limit 정책도 함께 조정해야 할 가능성이 있습니다.

프론트:

- `frontend/src/services/api.js`: `slateApi.regions(keyword = '', limit = 50)`
- `frontend/src/views/MatchingView.vue`: 지역 검색/선택 필터
- `frontend/src/views/TeamsView.vue`: 팀 지역 select
- `frontend/src/views/LocationExploreView.vue`: 시도/시군구 옵션을 `regions('', 100)`에서 파생

### 촬영지 추천 데이터와 구분

`sql/24_location_recommendation_schema.sql`는 아래 테이블을 추가합니다.

- `shooting_location`
- `shooting_location_history`
- `location_search_session`
- `location_recommendation_result`
- `saved_location_candidate`

CSV import 코드:

- `backend/src/main/java/com/slate/locations/importer/LocationCsvImportService.java`
- `backend/src/main/java/com/slate/locations/importer/LocationCsvImportRunner.java`
- `backend/src/main/java/com/slate/locations/importer/LocationCsvImportProperties.java`

확인된 CSV 상태:

- `assets/영화 로케이션 촬영 이력.csv`
- 총 13,761행
- 위도/경도/시도 누락 0건
- distinct 시도 17개
- distinct 시도+시군구 227개
- 이 CSV는 촬영지 추천 후보 데이터로 유용하지만, 회원/팀 매칭용 전국 행정구역 기준 데이터의 공식 원천으로 그대로 쓰기에는 부적합할 수 있습니다. 촬영 이력이 없는 지역이 빠질 수 있고, 행정구역 개편/명칭 최신성을 보장하지 않습니다.

## 리서치 과제

1. 대한민국 전역을 반영하는 `region` 기준 데이터의 적정 범위를 결정하세요.
   - 후보 A: 시군구 단위만 저장
   - 후보 B: 읍면동/행정동 또는 법정동 단위까지 저장
   - 후보 C: 시도/시군구/읍면동 계층형 테이블로 schema 개편
   - 현재 서비스의 매칭/팀/프로필 UI에 가장 적합한 최소 변경안을 추천하세요.

2. 공식 또는 신뢰 가능한 데이터 출처를 조사하세요.
   - 행정구역 코드: 행정안전부 행정표준코드, 법정동/행정동 코드 등
   - 경계/좌표: 국가공간정보포털, 국토지리정보원, 통계청 SGIS, VWorld 등
   - 최신 시도/시군구 명칭과 개편 이력
   - 각 출처의 다운로드 URL, 갱신 주기, 라이선스/이용 조건, 자동화 가능성을 표로 정리하세요.

3. `center_lat`, `center_lng` 산출 기준을 제안하세요.
   - 단순 행정청 주소 좌표
   - 행정구역 polygon centroid
   - point-on-surface/representative point
   - 면적이 크거나 섬이 포함된 지역에서 centroid가 바다나 외부에 떨어질 수 있는 문제
   - 거리 매칭 서비스 관점에서 어떤 기준이 가장 방어 가능한지 설명하세요.

4. 현재 schema 유지 여부를 판단하세요.
   - `region_code varchar(20)`에 어떤 공식 코드를 넣을지
   - 현재 `dong_name NOT NULL`을 유지할 경우 시군구 단위 row에서 어떤 값을 넣을지
   - `public_display_name` 중복 가능성
   - `region_id` FK가 이미 seed/sample/profile/team 데이터에서 쓰이므로 기존 ID 안정성을 어떻게 보존할지
   - `active_yn`으로 폐지/개편 지역을 비활성 관리할지

5. 구현 전략을 제시하세요.
   - 신규 SQL 파일명 예: `sql/27_seed_korea_regions.sql` 또는 기존 `02_seed_reference.sql` 교체
   - `INSERT ... ON DUPLICATE KEY UPDATE` 방식의 멱등 seed
   - 기존 12개 region_code와 신규 공식 데이터 간 매핑/교체 전략
   - 기존 `member_profile.region_id`, `team.region_id` 데이터 보존 또는 마이그레이션 방법
   - `ReferenceService.regions()` limit 100 제한 조정 필요 여부
   - 프론트 지역 검색 UI가 전국 200개 이상 시군구를 다룰 때 UX/성능 문제를 피하는 방법

6. 검증 전략을 제시하세요.
   - 전체 active 지역 수 기대값
   - 17개 시도 포함 여부
   - 각 시도별 시군구 count
   - 좌표 범위 및 한국 영역 bounding box 검증
   - `public_display_name` 중복 검증
   - `member_profile`/`team` FK 고아 데이터 검증
   - 매칭 거리 점수 회귀 테스트 아이디어
   - UI 검색/limit 회귀 테스트 아이디어

7. 최종 산출물 형식은 아래 순서로 작성하세요.
   - 결론 요약
   - 권장 데이터 출처와 근거
   - 권장 granularity와 schema 변경 여부
   - 데이터 생성 파이프라인
   - SQL/API/frontend 변경안
   - 검증 SQL 및 테스트 케이스
   - 운영/라이선스 위험
   - Codex 구현자에게 전달할 구체 작업 지시서

## 중요한 제약

- 임의 좌표를 생성하지 마세요.
- 공식 출처를 확인할 수 없는 데이터는 사용하지 마세요.
- 행정구역 명칭은 현재 기준으로 검증하세요.
- 시군구 단위와 읍면동 단위를 섞을 경우 거리 계산과 UI에 어떤 문제가 생기는지 반드시 설명하세요.
- 촬영지 CSV는 촬영 장소 추천용 데이터이며, 전국 행정구역 기준 데이터의 대체재인지 별도 보강재인지 명확히 판단하세요.
- 최종 답변에는 출처 링크와 접근일을 포함하세요.
