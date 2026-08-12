# Slate 전국 `region` 기준 데이터 확장 설계 리서치

## 결론 요약

Slate의 현재 기능 범위와 기존 DB/FK 구조를 기준으로 보면, **최소 변경으로 가장 방어 가능한 안은 `region`을 전국 `시군구` 단위로 통일 확장하는 것**입니다. 현재 서비스는 팀 생성/수정, 프로필, 매칭, 로케이션 탐색에서 모두 하나의 “대표 지역”을 요구하고 있고, 매칭 점수는 각 지역의 중심 좌표 간 haversine 거리만 사용하므로, `읍면동`까지 내려가면 데이터양과 UI 복잡도가 급증하는 반면 실익은 크지 않습니다. 반대로 `시군구`는 전국 단위 선택지로 충분히 세분화되어 있고, 현재 프론트 구조도 크게 바꾸지 않고 수용할 수 있습니다. 또한 행정안전부의 법정동 코드 OpenAPI는 실시간 갱신·이용허락범위 제한 없음으로 제공되고, VWorld의 시군구/읍면동 경계 API는 2026-06-15 기준 갱신일을 공개하고 있어 현재성 확인이 가능합니다. citeturn24view0turn17view1turn17view0

다만 **코드와 경계는 출처를 분리해서 다루는 것이 좋습니다.** 코드의 정본은 행정안전부 행정표준코드로 잡고, 좌표는 공식 경계에서 산출해야 합니다. 좌표 산출 기준은 **polygon centroid가 아니라 polygon 내부점 계열(point-on-surface / representative point)** 이 더 적합합니다. PostGIS 공식 문서에 따르면 centroid는 다각형 내부에 있지 않을 수 있고, point-on-surface는 폴리곤 내부에 놓이는 점이 보장됩니다. 이 차이는, 실제로 매칭 서비스가 “지역 대표점 간 거리”를 쓰는 현재 구조에서 매우 중요합니다. citeturn13search2turn13search0

실무적으로는 다음 원칙을 권장합니다. **정본 코드 소스는 행정안전부**, **정본 좌표 소스는 현재성 우선이면 VWorld 시군구 경계, 라이선스 보수성 우선이면 SGIS 공개 패키지**, **저장 granularity는 시군구 고정**, **기존 `region_id`는 가능한 한 유지**, **기존 12개 샘플 row는 같은 row를 공식 시군구 코드로 in-place 치환**입니다. 이렇게 하면 `member_profile.region_id`, `team.region_id` FK를 대규모로 흔들지 않으면서 전국 확장이 가능합니다. 촬영지 CSV는 전국 행정구역 기준 데이터의 **대체재가 아니라 보강재**로만 다루는 것이 맞습니다.

## 권장 데이터 출처와 근거

아래 표는 이번 과제에 실제로 써야 할 출처를 **코드 정본 / 변경 추적 / 경계 / 좌표 검증**으로 나누어 정리한 것입니다. 웹 출처 링크는 각 인용에 포함되어 있고, **접근일은 모두 2026-06-25**입니다.

| 용도 | 출처 | 현재성 | 라이선스/이용조건 | 자동화 적합성 | 판단 |
|---|---|---|---|---|---|
| 행정구역 코드 정본 | 행정안전부 `행정표준코드_법정동코드` OpenAPI | 실시간 | 이용허락범위 제한 없음 | 높음. REST API, 페이지네이션 가능 | **최우선 정본** citeturn24view0 |
| 코드 변경 추적 | 행정안전부 “행정기관(행정동) 및 관할구역(법정동) 변경내역” 공지 + `jscode*.zip` | 월별/수시 변경 반영 | 행정안전부 공지 문서. zip에 text/xlsx 포함 | 중간. 공지 스크랩 또는 수동 확인 필요 | **delta 추적용** citeturn26view1turn26view0 |
| 현재 시군구 경계 API | VWorld 2D API `LT_C_ADSIGG_INFO` | 2026-06-15 갱신일 공개 | API 키/도메인 등록 필요, VWorld 이용조건 적용 | 높음. API 호출 가능 | **현재성 가장 좋음** citeturn17view1turn16view1turn5search2 |
| 현재 읍면동 경계 API | VWorld 2D API `LT_C_ADEMD_INFO` | 2026-06-15 갱신일 공개 | API 키/도메인 등록 필요, VWorld 이용조건 적용 | 높음 | 시군구 좌표를 읍면동 dissolve로 만들 때 사용 가능 citeturn17view0turn16view0turn5search2 |
| 월간 법정경계 파일 | VWorld `행정구역_읍면동(법정동)` SHP | 2026-06 기준, 2026-06-14 갱신 | CC BY-NC-ND | 중간. 파일 다운로드 ETL | **신선하지만 라이선스 주의** citeturn19view0 |
| 공개 배포 경계 패키지 | 국가데이터처 `SGIS 행정구역 통계 및 경계` | 반기, 현재 공개 패키지는 2025 기준 경계 포함 | 이용허락범위 제한 없음 | 중간. 파일 패키지 ETL | **라이선스 가장 편함, 다만 현재성은 다소 뒤처질 수 있음** citeturn3view1 |
| 주민등록 통계 검증 | 행정안전부 주민등록 인구통계 | 월간 공표 | 공공 서비스 이용 | 중간 | 17개 시도/월간 시군구 현황 검증 보조용 citeturn28view1turn28view0 |
| 구형 통계 경계 | `(센서스경계)시군구경계` | 연간. 예시 속성에 BASE_YEAR 2016 | CC BY-NC-ND | 중간 | **현재 법정 기준 정본으로는 비권장** citeturn21view1 |

실무 권고는 두 줄로 요약됩니다. **코드 정본은 무조건 행정안전부 OpenAPI**로 가져오고, **좌표는 VWorld 현재 경계 또는 SGIS 공개 패키지에서 산출**하는 방식이 가장 합리적입니다. 행정안전부 OpenAPI는 `region_cd`, `sido_cd`, `sgg_cd`, `umd_cd`, `ri_cd`, `locatadd_nm` 등을 직접 제공하고, 이용허락범위 제한 없음·실시간 업데이트·REST 호출을 지원하므로 seed 생성의 기준축으로 적합합니다. citeturn24view0

반면 경계는 출처 성격이 갈립니다. VWorld 2D API의 시군구/읍면동 레이어는 제공처를 행정안전부로 표기하고 갱신일을 2026-06-15로 공개하고 있어 **현재성 측면에서는 가장 좋습니다**. 또 속성명이 `sig_cd`, `emd_cd`, `full_nm`처럼 현재 코드 체계와 바로 맞물려 매핑이 쉽습니다. citeturn17view1turn17view0

하지만 **라이선스와 운영 리스크는 별도로 봐야 합니다.** VWorld의 월간 법정경계 다운로드 페이지는 `CC BY-NC-ND`를 명시하고 있어, Slate가 상업 서비스일 가능성이 조금이라도 있다면 이를 seed DB에 영구 반영하는 것은 법무 검토 없이 밀어붙이기 어렵습니다. 반대로 SGIS 공개 패키지는 공공데이터포털에서 이용허락범위 제한 없음을 명시하지만, 현재 공개 설명은 “2025년 기준 경계”를 포함한다고 되어 있어 **현재성은 VWorld보다 뒤처질 수 있습니다**. citeturn19view0turn3view1

또 하나 중요한 실무 포인트는 **변경 공지의 “효력일(as-of)” 처리**입니다. 행정안전부 공지와 code.go 자료검색에는 2026-02-01 화성시 일반구 신설 같은 이미 시행된 변경뿐 아니라, 2026-07-01 시행 예정 안내도 함께 노출됩니다. 따라서 생성 파이프라인은 “가장 최신 텍스트를 긁는 방식”이 아니라, **`as_of_date` 기준으로 현재 유효한 코드만 채택하는 방식**이어야 합니다. 그렇지 않으면 현재 날짜보다 미래의 행정구역이 seed에 섞일 수 있습니다. citeturn26view1turn25view0

## 권장 granularity와 schema 변경 여부

후보 A, B, C를 비교하면 다음과 같습니다.

**후보 A: 시군구 단위만 저장**은 이번 프로젝트에 가장 잘 맞습니다. `MatchingService`가 실제 주소나 경계 기반 근접 탐색이 아니라 대표 위경도 1쌍 간의 거리만 사용하고, 프론트도 단일 지역 선택 UI를 전제로 하고 있기 때문입니다. 전국 시군구는 대략 “수백 개” 수준이라서 검색형 select, 2단 드롭다운, 캐시 프리로드로 충분히 다룰 수 있습니다. 반면 읍면동까지 내리면 수천 건 규모가 되어 현재 `regions('', 80/100)` 식 호출과 UI 구조가 곧바로 부담이 됩니다. 행정안전부도 행정동 변경을 월별 공지로 계속 반영하고 있고, 주민등록 통계도 행정동/법정동을 월간 단위로 운영하므로, 하위 단위로 갈수록 운영비가 커집니다. citeturn26view1turn28view1

**후보 B: 읍면동/행정동 또는 법정동까지 저장**은 지금 시점에서는 과합니다. 서비스가 현재 요구하는 것은 “정밀 주소”가 아니라 “활동 거점 지역”에 가깝고, 매칭 또한 `KM_10`, `KM_30`, `KM_100`, `ANYWHERE` 같은 버킷형 점수이므로, 시군구 단위만으로도 충분히 동작합니다. 더구나 행정동은 지방자치단체 조례에 의해 수시 변동된다는 점이 행정안전부 공지에 명시되어 있어, 행정동 중심 모델은 seed 최신화와 폐지 처리 비용이 큽니다. citeturn26view1turn26view0

**후보 C: 시도/시군구/읍면동 계층 테이블로 전면 개편**은 장기적으로는 가장 정교하지만, 이번 과제의 “최소 변경”과는 거리가 있습니다. 팀/프로필/매칭/레퍼런스 API/프론트 select 모두를 손봐야 하고, 기존 `region_id` FK도 재설계가 필요할 수 있습니다. 이번 확장 목적이 “현재 일부 row만 존재하는 seed를 전국 단위로 안정화”하는 것이라면, 이는 2차 단계로 미루는 편이 낫습니다.

따라서 **권장안은 A**입니다. 구체적으로는 다음과 같이 정리할 수 있습니다.

`region` 테이블은 그대로 유지하되, **저장 granularity를 시군구로 통일**합니다. `region_code`에는 행정안전부 법정동 코드 OpenAPI가 제공하는 **10자리 `region_cd` 중 시군구 레벨 코드**를 넣습니다. 이때 시군구 row는 API의 분해 필드 기준으로 `sgg_cd != '000'`, `umd_cd = '000'`, `ri_cd = '00'` 인 행만 채택하면 됩니다. 이런 방식이면 추후 확대가 필요해도 같은 코드 체계를 계속 사용할 수 있습니다. citeturn24view0

현재 스키마에서 가장 애매한 컬럼은 `dong_name NOT NULL`입니다. 시군구만 저장할 경우 `dong_name`은 의미가 없습니다. 이 컬럼에 임의의 행정동명이나 `'전체'` 같은 더미값을 넣으면, 검색, 정렬, 표시, 검증에서 계속 부작용이 납니다. 따라서 **권장 DDL은 `dong_name`을 NULL 허용으로 완화**하는 것입니다. 다만 현재 `ReferenceMapper.xml`의 검색식이 `CONCAT(...)`를 사용하므로, 이 경우 `COALESCE(dong_name, '')` 또는 `CONCAT_WS`로 함께 수정해야 합니다. 만약 DDL을 전혀 건드릴 수 없는 조직 규칙이 있다면 차선책으로 빈 문자열 `''` 을 넣을 수는 있지만, 이는 어디까지나 호환용 타협입니다.

`public_display_name`은 **항상 `시도 + 공백 + 시군구`** 형식으로 재생성하는 것이 좋습니다. 이 방식은 현재 demo seed들이 `public_display_name = '서울특별시 종로구'` 형태로 `region_id`를 찾는 패턴과도 호환됩니다. full name 기준으로 만들면 동명이구/동명이시 문제를 피할 수 있고, 현재 VWorld API가 보여주는 `full_nm` 예시도 같은 방향입니다. citeturn17view1

`region_id`는 **절대 재번호를 매기지 않는 것**이 핵심입니다. 현재 12개 샘플 row는 동 단위 코드지만 표시명은 시군구 수준입니다. 이 12건은 모두 **한 개의 시군구 코드로 안전하게 축약 가능**하므로, 새 row를 만들지 말고 **같은 `region_id` row를 공식 시군구 코드로 in-place 갱신**해야 합니다. 그러면 `member_profile.region_id`, `team.region_id`는 그대로 살아 있고, sample/demo seed도 대부분 손대지 않아도 됩니다.  

`active_yn`은 미래 변경까지 생각하면 그대로 유지하는 편이 좋습니다. 향후 code.go / 행정안전부 변경 공지에 따라 코드가 폐지되거나 통합될 수 있는데, **일대일 승계가 불명확한 경우는 자동 마이그레이션보다 `active_yn='N'` 비활성 관리가 더 안전**합니다. 예를 들어 1→N 분할 개편에서는 기존 사용자 데이터를 어느 새 코드로 옮겨야 하는지 자동으로 결정할 수 없기 때문입니다.

그리고 반드시 짚고 넘어가야 할 점이 하나 있습니다. **시군구 row와 읍면동 row를 같은 `region` 테이블에서 섞어 쓰면 안 됩니다.** 이유는 세 가지입니다. 첫째, 거리 계산이 불공정해집니다. 읍면동 대표점은 훨씬 세밀하고, 시군구 대표점은 더 거칩니다. 둘째, UI 검색 결과가 혼합 수준으로 보여 사용자가 “서울특별시 강남구”와 “서울특별시 강남구 역삼동”을 같은 차원의 선택지로 오해하게 됩니다. 셋째, `public_display_name`, 정렬, limit, 추천 로직이 모두 왜곡됩니다. 현재 서비스 구조에서는 **한 granularity로 통일**하는 것이 맞습니다.

## 데이터 생성 파이프라인

권장 파이프라인은 “코드 정본 → 현재성 delta → 경계 결합 → 대표점 산출 → seed 생성 → 검증” 순서입니다.

먼저 **코드 정본 수집 단계**입니다. 배치 스크립트는 행정안전부 `StanReginCd` OpenAPI에서 전체 법정동 코드를 가져오고, 그중 **시군구 레벨만 필터링**합니다. 필터 기준은 `sgg_cd != '000' AND umd_cd = '000' AND ri_cd = '00'` 입니다. 이렇게 하면 시도/읍면동/리는 떨어지고 시군구만 남습니다. 이 API는 실시간 업데이트와 이용허락범위 제한 없음을 명시하고 있어 seed 생성 자동화의 기준으로 쓰기에 충분합니다. citeturn24view0

다음은 **변경 공지 delta 적용 단계**입니다. 실무에서는 OpenAPI만으로도 대부분 충분하겠지만, seed 생성일 바로 직전/직후 행정구역 개편을 놓치지 않으려면 행정안전부의 `jscode*.zip` 공지를 함께 확인하는 것이 좋습니다. 특히 행정안전부는 2026-02-01 화성시 일반구 신설과 같이 실제 효력일 단위로 공지를 내고, zip 안에 text/xlsx 포맷도 함께 제공합니다. 다만 code.go 검색 결과에는 2026-07-01 시행 예정 공지가 이미 섞여 있으므로, 파이프라인에는 **반드시 `as_of_date`** 가 있어야 합니다. 즉, seed 생성일이 2026-06-25이면 2026-07-01 효력 공지는 반영하지 않습니다. citeturn26view1turn25view0

경계 수집은 **두 갈래 전략**을 권합니다.

첫 번째는 **현재성 우선 경로**입니다. VWorld 2D API의 `LT_C_ADSIGG_INFO` 또는 `LT_C_ADEMD_INFO`는 행정안전부 제공처, 2026-06-15 갱신일, EPSG:4326 반환 옵션, `sig_cd`/`emd_cd`/`full_nm` 속성을 제공하므로 가장 다루기 쉽습니다. 시군구 단위면 `LT_C_ADSIGG_INFO`를 그대로 쓰고, 읍면동 법정경계에서 시군구를 재구성하고 싶다면 `LT_C_ADEMD_INFO` 또는 월간 SHP를 dissolve해서 5자리 시군구 코드 기준으로 합치면 됩니다. 다만 API 키/도메인 등록이 필요합니다. citeturn17view1turn17view0turn16view1turn16view0

두 번째는 **라이선스 보수 경로**입니다. 공공데이터포털의 `국가데이터처_SGIS 행정구역 통계 및 경계`는 이용허락범위 제한 없음으로 공개되며, 경계 SHP와 코드집을 포함합니다. 이 경로는 법무 리스크가 적지만, 현재 공개 패키지 설명상 2025 기준 경계를 포함하고 있어, 오늘 기준 최신 행정구역을 1:1로 반영해야 한다면 화성 일반구 같은 변경은 별도 보완이 필요할 수 있습니다. citeturn3view1

대표점 산출은 **polygon centroid가 아니라 representative point 계열**로 해야 합니다. 이유는 단순합니다. PostGIS 공식 문서에 따르면 centroid는 폴리곤 내부에 있지 않을 수 있고, point-on-surface는 내부에 놓이는 점이 보장됩니다. 따라서 배치에서는 다음 순서를 추천합니다.  
첫째, 시군구별로 경계를 dissolve 합니다.  
둘째, 멀티폴리곤이면 **가장 면적이 큰 polygon 파트**를 선택합니다.  
셋째, 그 파트에 대해 `point-on-surface`를 계산합니다.  
넷째, 계산은 원본 투영좌표계(EPSG:5186 또는 5179 등)에서 수행하고, 최종 점만 WGS84(EPSG:4326)로 변환해 `center_lat`, `center_lng`로 저장합니다.  
이 방식은 “바다에 떨어지는 centroid” 문제를 피하면서도, 섬 지역에서 아주 작은 부속 도서가 대표점이 되는 문제를 줄입니다. 앞의 “largest part” 선택은 공식 규격이라기보다 운영상 방어적인 선택입니다. citeturn13search2turn13search0turn19view0turn16view1

이 파이프라인의 산출물은 세 개면 충분합니다.  
첫째, `korea_regions_sigungu.csv` 같은 정규화된 중간 산출물.  
둘째, 멱등적으로 적용 가능한 `INSERT ... ON DUPLICATE KEY UPDATE` SQL.  
셋째, 생성 시점·출처·행 수·라이선스를 적은 `manifest.json`.  
이 manifest에 `as_of_date`, `code_source`, `geometry_source`, `expected_active_count`, `source_license` 를 남겨 두면 이후 검증과 운영이 훨씬 쉬워집니다.

## SQL API frontend 변경안

가장 먼저 권장하는 것은 **SQL seed 파일의 분리 방식**입니다. 지금 실행 순서가 `01_schema.sql → 02_seed_reference.sql → 03_seed_sample_data.sql ...` 로 이어진다면, `region` seed를 `sql/27_seed_korea_regions.sql` 에 새로 두는 방식은 순서상 어색합니다. sample/demo seed들이 `region`을 이미 참조하고 있기 때문입니다. 따라서 둘 중 하나를 택하는 편이 좋습니다.

하나는 **`02_seed_reference.sql` 내부의 12행 `region` insert를 generated block으로 교체**하는 방식입니다. 이 경우 신규 환경 bootstrap은 그대로 유지됩니다.  
다른 하나는 **`sql/02a_seed_regions.sql` 같은 파일을 `02` 바로 뒤에 두고, 설치 순서를 명시적으로 업데이트**하는 방식입니다.  
이번 프로젝트에서는 후자가 더 관리하기 쉽지만, 운영 문서와 실행 스크립트도 같이 바꿔야 합니다.

스키마는 다음 정도만 권장합니다.

```sql
ALTER TABLE region
  MODIFY dong_name varchar(80) NULL;
```

그리고 `ReferenceMapper.xml`의 검색식은 null-safe로 바꾸는 편이 좋습니다.

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
  <if test="keyword != null and keyword != ''">
    AND CONCAT_WS(' ',
      sido_name,
      sigungu_name,
      COALESCE(dong_name, ''),
      public_display_name
    ) LIKE CONCAT('%', #{keyword}, '%')
  </if>
ORDER BY sido_name, sigungu_name, COALESCE(dong_name, '')
LIMIT #{limit}
```

기존 12개 row의 `region_id`를 보존하려면, 먼저 **기존 동 코드 → 공식 시군구 코드 매핑**을 적용한 뒤, 같은 row를 표준 데이터로 덮어써야 합니다. 예시는 아래와 같습니다.

```sql
CREATE TEMPORARY TABLE region_code_migration (
  old_region_code varchar(20) PRIMARY KEY,
  new_region_code varchar(20) NOT NULL
);

INSERT INTO region_code_migration (old_region_code, new_region_code) VALUES
('1111010100', '1111000000'),
('1114016200', '1114000000'),
('1168010100', '1168000000'),
('1144012000', '1144000000'),
('1121510100', '1121500000'),
('4113510800', '4113500000'),
('4128112800', '4128100000'),
('2611014000', '2611000000'),
('2711015700', '2711000000'),
('2915510900', '2915500000'),
('3011014000', '3011000000'),
('5011013700', '5011000000');
```

그 다음 stage table에 로드한 표준 데이터로 **in-place update** 합니다.

```sql
UPDATE region r
JOIN region_code_migration m
  ON m.old_region_code = r.region_code
JOIN region_stage s
  ON s.region_code = m.new_region_code
SET
  r.region_code = s.region_code,
  r.sido_name = s.sido_name,
  r.sigungu_name = s.sigungu_name,
  r.dong_name = s.dong_name,
  r.center_lat = s.center_lat,
  r.center_lng = s.center_lng,
  r.public_display_name = s.public_display_name,
  r.active_yn = 'Y';
```

그 후 나머지 row를 업서트합니다.

```sql
INSERT INTO region (
  region_code, sido_name, sigungu_name, dong_name,
  center_lat, center_lng, public_display_name, active_yn
)
SELECT
  region_code, sido_name, sigungu_name, dong_name,
  center_lat, center_lng, public_display_name, active_yn
FROM region_stage
ON DUPLICATE KEY UPDATE
  sido_name = VALUES(sido_name),
  sigungu_name = VALUES(sigungu_name),
  dong_name = VALUES(dong_name),
  center_lat = VALUES(center_lat),
  center_lng = VALUES(center_lng),
  public_display_name = VALUES(public_display_name),
  active_yn = VALUES(active_yn);
```

이 방식의 장점은 명확합니다. **기존 `region_id`를 사용하는 `member_profile`, `team` FK는 그대로 살아 있고**, demo seed가 `public_display_name`으로 지역을 찾는 방식도 계속 작동합니다. 반면 새로 전국 row가 추가되면서도 seed는 멱등적으로 재실행할 수 있습니다.

API 쪽에서는 `ReferenceService.regions()` 의 `max 100` 은 반드시 바꿔야 합니다. 시군구-only라고 해도 전국 active row는 더 이상 12개가 아니고, 현재 행정구역 개편까지 반영하면 “예전 229 근처” 같은 상수로 보장할 수도 없습니다. 화성시 일반구 신설 같은 변화가 이미 시행되었고, 행정구역 변경 공지도 계속 나오기 때문입니다. 따라서 서버 max는 **최소 300, 여유 있게는 500** 이 좋습니다. citeturn26view1turn25view0

프론트는 화면별로 나눠 대응하는 것이 깔끔합니다.

`MatchingView.vue`, `TeamsView.vue`, `ProfileView.vue` 는 **키워드 검색형**으로 유지하고, 초기 빈 호출은 작게, 키워드 입력 시 limit 50~100으로 가져오면 됩니다.  
`LocationExploreView.vue` 는 **전국 시군구 전체를 한 번 받아 17개 시도와 그 하위 시군구로 파생**하는 구조가 더 낫습니다. 현재 행정안전부 주민등록 통계에서도 17개 시도가 월간 기준으로 확인되므로, 시도 목록 파생 자체는 안정적입니다. citeturn28view1

즉, 프론트 변경의 핵심은 “수천 건을 full dump 하지 않고도 전국 탐색이 되게 하는 것”입니다. 시군구-only를 채택하면 이 문제는 비교적 작게 끝납니다. 읍면동까지 저장하면 별도 tree endpoint, lazy loading, 검색 색인, 페이징 UX를 새로 짜야 하므로, 최소 변경안이 아니게 됩니다.

## 검증 SQL 및 테스트 케이스

검증은 **하드코딩된 옛 상수**가 아니라, **이번 seed를 생성한 snapshot manifest** 를 기준으로 해야 합니다. 특히 전국 시군구 수는 과거 센서스 자료에서는 228/229처럼 보일 수 있지만, 2026-02 화성 일반구 신설처럼 실제 변경이 발생하면 더 이상 고정 상수로 둘 수 없습니다. 구형 센서스경계는 연간 갱신, 예시 속성에 `BASE_YEAR 2016` 이 보이며, 라이선스도 `CC BY-NC-ND` 이라 이번 과제의 현재 법정 기준 정본으로 쓰기 어렵습니다. citeturn21view1

가장 먼저 할 검증은 **행 수와 17개 시도 포함 여부**입니다. 행정안전부 주민등록 인구통계의 월간 현재 화면은 2026년 5월 기준 서울특별시부터 제주특별자치도까지 17개 광역단위를 보여주므로, 최소한 `DISTINCT sido_name = 17` 은 반드시 확인해야 합니다. citeturn28view1

```sql
-- active 지역 총수
SELECT COUNT(*) AS active_region_count
FROM region
WHERE active_yn = 'Y';

-- 17개 시도 포함 여부
SELECT COUNT(DISTINCT sido_name) AS sido_count
FROM region
WHERE active_yn = 'Y';

SELECT sido_name, COUNT(*) AS sigungu_count
FROM region
WHERE active_yn = 'Y'
GROUP BY sido_name
ORDER BY sido_name;
```

다음은 **코드 형식과 granularity 일관성**입니다. 이번 권장안은 시군구-only 이므로, active row는 모두 10자리 숫자이면서 **앞 5자리 + 뒤 5자리 0** 패턴이어야 합니다.

```sql
SELECT COUNT(*) AS invalid_region_code_count
FROM region
WHERE active_yn = 'Y'
  AND region_code NOT REGEXP '^[0-9]{5}00000$';
```

`public_display_name` 중복은 없어야 합니다.

```sql
SELECT public_display_name, COUNT(*) AS cnt
FROM region
WHERE active_yn = 'Y'
GROUP BY public_display_name
HAVING COUNT(*) > 1;
```

좌표 QA는 너무 엄격한 “정답 bbox” 보다는, **대한민국 영역을 크게 벗어나는 오염값을 걸러내는 휴리스틱** 으로 두는 것이 좋습니다. 예를 들어 경도 120도, 위도 50도 같은 잘못된 좌표를 잡아내는 용도입니다.

```sql
SELECT region_id, region_code, public_display_name, center_lat, center_lng
FROM region
WHERE active_yn = 'Y'
  AND (
    center_lat NOT BETWEEN 32.0 AND 39.5
    OR center_lng NOT BETWEEN 124.0 AND 132.5
  );
```

FK 고아 검증도 반드시 필요합니다.

```sql
SELECT mp.profile_id, mp.region_id
FROM member_profile mp
LEFT JOIN region r ON r.region_id = mp.region_id
WHERE r.region_id IS NULL;

SELECT t.team_id, t.region_id
FROM team t
LEFT JOIN region r ON r.region_id = t.region_id
WHERE t.region_id IS NOT NULL
  AND r.region_id IS NULL;
```

비활성 지역 참조도 확인해야 합니다.

```sql
SELECT mp.profile_id, mp.region_id, r.public_display_name, r.active_yn
FROM member_profile mp
JOIN region r ON r.region_id = mp.region_id
WHERE r.active_yn <> 'Y';

SELECT t.team_id, t.region_id, r.public_display_name, r.active_yn
FROM team t
JOIN region r ON r.region_id = t.region_id
WHERE t.region_id IS NOT NULL
  AND r.active_yn <> 'Y';
```

검색/정렬 검증은 아래 정도면 충분합니다.

```sql
-- 전국 전체 로딩 시 충분한 수가 나오는지
SELECT COUNT(*) AS returned_count
FROM (
  SELECT region_id
  FROM region
  WHERE active_yn = 'Y'
  ORDER BY sido_name, sigungu_name, COALESCE(dong_name, '')
  LIMIT 300
) x;

-- 대표 검색어가 정상 동작하는지
SELECT public_display_name
FROM region
WHERE active_yn = 'Y'
  AND CONCAT_WS(' ',
      sido_name,
      sigungu_name,
      COALESCE(dong_name, ''),
      public_display_name
  ) LIKE '%성남%'
ORDER BY public_display_name;
```

매칭 회귀 테스트는 코드 레벨에서 시나리오 기반으로 두는 것이 좋습니다. 핵심은 좌표값 그 자체보다 **점수 경계조건** 입니다.

- 같은 `region_id` 를 가진 팀/프로필 → 거리 점수 1.0  
- `regionAnyYn='Y'` 또는 `travelRange='ANYWHERE'` → 거리 점수 1.0  
- 좌표 null → fallback 0.7  
- 전국 seed로 교체한 뒤에도 기존 샘플 12개 사용자/팀의 지역 참조가 깨지지 않음  
- 기존 동 코드가 시군구 공식 코드로 바뀐 뒤 demo 화면에서 동일한 표시명이 유지됨

UI 회귀 테스트는 다음이면 충분합니다.

- `/references/regions?limit=300` 이 전국 시군구를 모두 반환  
- `LocationExploreView` 가 17개 시도를 파생  
- 시도 선택 후 시군구 옵션이 누락 없이 보임  
- `MatchingView` / `TeamsView` 에서 키워드 검색 시 지역이 누락되지 않음  
- 빈 검색 시 서버 기본 limit가 과도하게 큰 payload를 만들지 않음

## 운영 및 라이선스 위험

이번 과제의 가장 큰 리스크는 **데이터 정확성보다 라이선스와 기준일 불일치**입니다.

첫째, **VWorld 월간 법정경계 다운로드는 `CC BY-NC-ND`** 입니다. 이는 비영리·변경금지 조건이므로, Slate의 사용 목적이 상업적일 가능성이 있거나, 가공 결과물을 서비스 DB에 영구 저장하는 것이 “변경된 이용”으로 해석될 수 있다면 법무 검토 없이 정본 seed로 채택하는 것은 신중해야 합니다. VWorld API 자체도 키와 도메인 등록을 요구하고, 이용약관에 따라 서비스 사용을 전제합니다. 따라서 **VWorld는 현재성은 좋지만 라이선스 리스크가 존재**합니다. citeturn19view0turn16view1turn5search2

둘째, **SGIS 공개 패키지는 이용허락범위 제한 없음** 이라 라이선스는 더 편하지만, 현재 공공데이터포털 설명이 “2025년 기준 경계”를 포함한다고 되어 있어 오늘 시점의 행정개편을 항상 즉시 반영한다고 보기는 어렵습니다. 즉, **SGIS는 라이선스 안정성, VWorld는 현재성** 이라는 trade-off가 명확합니다. citeturn3view1

셋째, **미래 효력일 공지의 오반영** 위험이 있습니다. code.go 검색 결과에는 이미 2026-07-01 시행 예정 변경안도 노출됩니다. seed 생성기가 최신 공지만 보고 현재 데이터를 만들면, 아직 시행되지 않은 구역이 DB에 들어갈 수 있습니다. 따라서 `as_of_date` 를 고정하고, 효력일이 그 이후인 공지는 무조건 제외해야 합니다. citeturn25view0

넷째, **구형 통계경계의 오사용** 위험이 있습니다. `(센서스경계)시군구경계` 는 샘플 속성에 `BASE_YEAR 2016` 이 보이고, 연간 갱신 구조이며, 라이선스도 `CC BY-NC-ND` 입니다. 이 자료는 통계 지도나 참고용으로는 의미가 있지만, “현재 법정 기준 + 현재 서비스 seed” 의 1차 원천으로 쓰기에는 맞지 않습니다. citeturn21view1

따라서 운영 정책은 아래처럼 분리하는 것이 가장 안전합니다.

- **정본 코드:** 행정안전부 OpenAPI  
- **정본 좌표:**  
  - 서비스가 비상업/내부용이거나 법무 승인이 있으면 VWorld current layer  
  - 그 외에는 SGIS 공개 패키지 + 최신 변경분 별도 보류/수동 반영  
- **변경 감시:** 월 1회 배치 + 변경 공지 확인  
- **비가역 자동 마이그레이션 금지:** 1→N, N→1 개편은 운영자 검토 후 처리  
- **촬영지 CSV와 혼용 금지:** 촬영지 데이터는 추천 후보용, 행정구역 기준 seed 대체 불가

## Codex 구현자에게 전달할 구체 작업 지시서

아래 작업 지시서는 지금 프로젝트 구조를 기준으로 바로 실행 가능한 수준으로 적었습니다.

**데이터 생성기부터 만든다.**  
`tools/region_seed/` 아래에 Python 스크립트를 추가한다. 입력은 `as_of_date`, 코드 소스, 경계 소스 경로 또는 API 설정이다. 출력은  
`sql/generated/region_sigungu_seed.sql`,  
`sql/generated/region_sigungu_manifest.json`,  
`sql/generated/region_sigungu.csv`  
세 개다.

**정본 코드는 행정안전부로 고정한다.**  
행정안전부 `StanReginCd` OpenAPI에서 전체 row를 읽고, `sgg_cd != '000' AND umd_cd = '000' AND ri_cd = '00'` 인 row만 남긴다. `region_code` 는 `region_cd` 10자리를 그대로 저장한다. `sido_name`, `sigungu_name` 은 `locatadd_nm` 또는 분해 필드로 생성하되, 최종 `public_display_name` 은 반드시 `"{시도} {시군구}"` 형식으로 출력한다. citeturn24view0

**경계 소스는 설정형으로 분리한다.**  
기본 구현은 두 프로파일을 지원한다.  
`GEOMETRY_SOURCE=vworld` 이면 현재성 우선.  
`GEOMETRY_SOURCE=sgis` 이면 라이선스 보수 경로.  
문서에 두 경로의 장단점을 분명히 적는다. VWorld 경로는 API 키/도메인 또는 월간 SHP 파일을 받도록 하고, SGIS 경로는 공개 패키지 SHP를 입력으로 받게 한다. citeturn17view1turn3view1turn19view0

**대표점 계산은 centroid 금지.**  
dissolve 후 멀티폴리곤이면 largest polygon part를 고르고, 그 파트에 대해 representative point / point-on-surface 를 계산한다. 계산은 원본 투영좌표계에서 수행하고, 마지막에 WGS84로 바꿔 `center_lat`, `center_lng` 를 7자리 소수까지 저장한다. 임의 좌표 생성은 금지한다. citeturn13search2turn13search0

**기존 12개 `region_id` 는 보존한다.**  
새 row를 만들지 말고, 현재 row를 공식 시군구 코드로 업데이트하는 migration SQL을 먼저 생성한다. 그 다음 전국 시군구를 `INSERT ... ON DUPLICATE KEY UPDATE` 로 업서트한다. 이렇게 하면 `member_profile.region_id`, `team.region_id` 는 그대로 유지된다.

**SQL 파일 순서를 수정한다.**  
`sql/02_seed_reference.sql` 안의 `region` 12행 insert는 제거한다.  
대신 `sql/02a_seed_regions.sql` 또는 `sql/02_seed_regions.sql` 를 추가하고, bootstrap 문서/스크립트가 `02` 다음에 이 파일을 실행하게 바꾼다.  
`27` 번대로 파일을 두는 방식은 sample/demo seed보다 늦게 실행될 수 있으니 피한다.

**스키마 최소 수정만 한다.**  
가능하면 아래 DDL을 추가한다.

```sql
ALTER TABLE region
  MODIFY dong_name varchar(80) NULL;
```

만약 DDL 변경이 어렵다면 generator가 `dong_name=''` 로 넣도록 하고, 그래도 mapper 쿼리는 `COALESCE` 기반으로 바꾼다.

**백엔드 레퍼런스 API를 조정한다.**  
`backend/src/main/java/com/slate/references/ReferenceService.java` 에서 `regions()` max limit를 100에서 500으로 올린다. 기본 limit는 20 유지 가능하다.  
`backend/src/main/resources/mappers/ReferenceMapper.xml` 에서 search concat을 null-safe로 바꾼다.

**프론트 호출 정책을 화면별로 나눈다.**  
`frontend/src/services/api.js` 는 `regions(keyword = '', limit = 50)` 시그니처를 유지해도 된다.  
다만  
`LocationExploreView.vue` 는 `regions('', 300)` 또는 전용 전체조회 호출로 1회 preload 후 17개 시도/시군구를 파생한다.  
`MatchingView.vue`, `TeamsView.vue`, `ProfileView.vue` 는 키워드 검색 UX를 유지하고, 빈 호출은 작게 가져온다.

**검증 스크립트를 별도 파일로 둔다.**  
`sql/28_validate_region_seed.sql` 을 추가해 아래를 검증하게 한다.

- active row 총수 = manifest의 expectedActiveCount  
- `DISTINCT sido_name = 17`  
- `region_code` 형식이 모두 `^[0-9]{5}00000$`  
- `public_display_name` 중복 0  
- 한국 bbox 밖 좌표 0  
- `member_profile`, `team` 고아 FK 0  
- inactive region 참조 0

**테스트 acceptance criteria를 명문화한다.**

- 새 DB bootstrap 후 `region` active row가 전국 시군구를 모두 포함한다.  
- 기존 demo seed가 깨지지 않는다.  
- `/references/regions?limit=300` 이 전국 데이터를 다 반환한다.  
- `LocationExploreView` 에서 17개 시도가 보인다.  
- 매칭 거리 점수는 null/fallback/ANYWHERE 규칙을 기존과 동일하게 유지한다.  
- 촬영지 CSV import 로직은 손대지 않는다. `shooting_location*` 와 `region` 은 목적이 다르다.

**운영 문서에 기준일과 출처를 적는다.**  
배포 문서 또는 `docu/07_database/` 아래 문서에 아래 4가지를 반드시 남긴다.

- 코드 기준일(`as_of_date`)  
- 코드 출처  
- 경계/좌표 출처  
- 라이선스 판단 memo

이 네 가지가 없으면, 몇 달 뒤 지역 수가 바뀌었을 때 “버그인지, 행정개편 반영인지”를 구분하기 어려워집니다.  

웹 출처 링크는 각 인용에 포함되어 있으며, **접근일은 모두 2026-06-25**입니다.