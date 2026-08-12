# assets — 데모 시드 데이터

이 폴더는 Slate 데모/시딩용 데이터다. 프런트엔드 빌드에 번들되는 UI 에셋은
`frontend/src/assets/`에 별도로 있으며, 이 폴더는 **DB 시딩·데모 표시용**이다.

## 저장소 포함 범위

용량 문제로 **카테고리별 샘플 이미지만** 포함한다(각 디렉토리 소수 파일).
전체 데모 데이터셋은 크롤링/수집으로 재생성하며 저장소에는 담지 않는다.

| 경로 | 내용 | 저장소 포함 |
|---|---|---|
| `defaults/` | 기본 대체 이미지 | 포함 |
| `images/`, `portfolio_images/`, `user_profile_images/`, `work_images/`, `contest_images/`, `team_images/` | 데모 시드 이미지 | 카테고리별 샘플만 |
| `영화 로케이션 촬영 이력.csv` | 로케이션 임포터 입력 (백엔드 테스트에서 사용) | 포함 |
| `region-geocode-preview.json`, `지역 DB 전역화 심층 리서치.md` | 지역/지오코딩 참고 | 포함 |

## 저장소에서 제외한 원본 데이터

용량·라이선스상 아래 GIS 원본은 제외했다. 필요 시 아래 출처에서 받아
`assets/` 하위에 배치한다.

| 데이터 | 출처 |
|---|---|
| 시군구 구역 경계 shapefile (`vworld/LT_C_ADSIGG_INFO/*`) | [국토교통부 vworld](https://www.vworld.kr/) 국가기본도 |
| 영화 로케이션 촬영 이력 shapefile (`영화 로케이션 촬영 이력_shp/*`) | 공공데이터포털 / 영화진흥위원회 공개 데이터 |

`영화 로케이션 촬영 이력.csv`(CSV)는 임포터가 참조하므로 유지한다.
