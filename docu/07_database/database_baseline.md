# 데이터베이스 기준

## 기본 기준

| 항목 | 값 |
|---|---|
| DBMS | MySQL 8 |
| DB명 | `slate` |
| 로컬 사용자 예시 | `slate_app` |
| 문자셋 | `utf8mb4` 권장 |
| schema 기준 | `sql/01_schema.sql` |

## SQL 실행 순서

초기 생성:

```powershell
mysql -u root -p < sql/00_create_database.sql
mysql -u slate_app -p slate < sql/01_schema.sql
mysql -u slate_app -p slate < sql/02_seed_reference.sql
mysql -u slate_app -p slate < sql/03_seed_sample_data.sql
```

초기화 후 재적용:

```powershell
mysql -u slate_app -p slate < sql/99_reset.sql
mysql -u slate_app -p slate < sql/01_schema.sql
mysql -u slate_app -p slate < sql/02_seed_reference.sql
mysql -u slate_app -p slate < sql/03_seed_sample_data.sql
```

조건부:

```powershell
mysql -u slate_app -p slate < sql/04_youtube_metadata_schema.sql
mysql -u slate_app -p slate < sql/05_seed_ai_matching_dummy_data.sql
mysql -u slate_app -p slate < sql/08_portfolio_credit_name_schema.sql
mysql -u slate_app -p slate < sql/09_entity_image_schema.sql
mysql -u slate_app -p slate < sql/10_board_full_integration_schema.sql
mysql -u slate_app -p slate < sql/11_board_search_genre_period_schema.sql
mysql -u slate_app -p slate < sql/12_contest_image_schema.sql
mysql -u slate_app -p slate < sql/13_contest_search_filter_schema.sql
mysql -u slate_app -p slate < sql/14_remove_contest_benefit_extra_schema.sql
mysql -u slate_app -p slate < sql/15_contest_crawl_source_schema.sql
mysql -u slate_app -p slate < sql/16_contest_official_link_cleanup.sql
mysql -u slate_app -p slate < sql/17_demo_access_code_management_schema.sql
mysql -u slate_app -p slate < sql/07_seed_verified_portfolio_ui_demo.sql
mysql -u slate_app -p slate < sql/27_apply_generated_dummy_images.sql
```

`08_portfolio_credit_name_schema.sql`은 기존 DB의 `portfolio_item.credit_name` 컬럼을 추가하는 멱등 마이그레이션이다. 신규 DB는 `01_schema.sql`에 같은 컬럼이 포함되므로 실행해도 중복 변경되지 않는다.

`09_entity_image_schema.sql`은 프로필, 팀, 작업물, 포트폴리오의 대표 이미지 경로 컬럼을 기존 DB에 추가하는 멱등 마이그레이션이다. 신규 DB는 `01_schema.sql`에 같은 컬럼이 포함된다.

`10_board_full_integration_schema.sql`은 자유게시판 `free_category`, 작업물 및 승인 요청 `work_type`, 관련 인덱스와 공통 코드를 추가한다. 기존 FREE와 작업물은 각각 `FREE`, `OTHER`로 보정하고 게시글 좋아요 집계도 활성 관계 기준으로 다시 맞춘다.

`11_board_search_genre_period_schema.sql`은 작업물-장르 다대다 관계 `work_genre`와 팀 작업물 승인 중 장르를 보존하는 `team_work_approval_genre`를 추가한다. 복수 장르의 대표 장르는 `sort_order`, `genre_id` 순 첫 항목이다.

`12_contest_image_schema.sql`은 `contest`와 `contest_open_request`에 직접 업로드 이미지 상대 경로 `representative_image_path`를 추가한다. 기존 `representative_image_url`은 외부 URL 출처용으로 유지하며 서버가 외부 이미지를 다운로드하지 않는다.

`27_apply_generated_dummy_images.sql`은 CDD/CDV 더미 데이터에 생성 이미지 상대 경로를 적용한다. `SLATE_UPLOAD_DIR` 전역 변경 없이 `uploads/images/seed/...` 아래 복사본을 백엔드 런타임 파일로 사용하고, DB에는 `images/seed/.../*.png` 상대 경로를 저장한다.

`13_contest_search_filter_schema.sql`은 두 공모전 테이블에 대상·지역 JSON 코드, 주최 유형, 총상금·1등 상금 숫자를 추가한다. 크롤링 전 기존 행은 추정 backfill하지 않는다.

`14_remove_contest_benefit_extra_schema.sql`은 앞선 개발 과정에서 추가됐을 수 있는 특전 및 추가 정보 컬럼을 기존 DB에서 제거하는 멱등 마이그레이션이다. 신규 DB의 `01_schema.sql`에는 해당 컬럼이 없다.

`15_contest_crawl_source_schema.sql`은 콘테스트코리아 크롤링 출처, 외부 식별자, 포스터 출처, 수집 시각 등 source 기반 upsert에 필요한 컬럼과 unique key를 추가한다. 신규 DB는 `01_schema.sql`에 같은 구조가 포함된다.

`16_contest_official_link_cleanup.sql`은 콘테스트코리아 원문 URL이 공식 링크와 중복 저장된 기존 데이터를 정리하기 위한 선택 실행 SQL이다.

`17_demo_access_code_management_schema.sql`은 DB 관리형 Demo Access 접근 코드 테이블과 `DEMO_ACCESS_MANAGE` 관리자 권한 seed를 추가한다. DB에는 평문 접근 코드를 저장하지 않고 hash/fingerprint만 저장한다.

`07_seed_verified_portfolio_ui_demo.sql`은 Verified 배지의 API/UI 표시만 확인하는 선택 실행 fixture다. 기본 대상은 `leader`이며, 실행 전에 `@verified_badge_demo_login_id`를 설정하면 다른 로컬 계정으로 대상을 바꿀 수 있다. 실제 KOBIS API 검증 성공 데이터가 아니며 운영 DB에는 적용하지 않는다.

## 테이블 영역

| 영역 | 대표 테이블 |
|---|---|
| 기준 데이터 | `common_code_group`, `common_code`, `region`, `role_category`, `role`, `genre` |
| 계정/회사 | `user_account`, `company_application`, `company_application_document`, `admin_permission` |
| 프로필 | `member_profile`, `profile_role`, `profile_genre`, `profile_collaboration_condition`, `portfolio_item`, `portfolio_verification`, `public_data_sync_item` |
| 팀 | `team`, `team_genre`, `team_member`, `team_recruitment`, `team_recruitment_slot`, `team_application`, `team_invitation`, `team_plan_item`, `team_closure_snapshot` |
| 매칭 | `matching_score_policy`, `matching_score_policy_item`, `matching_score_policy_history`, `matching_bookmark`, `matching_action_log` |
| 게시판/작업물 | `board_post`, `board_review`, `board_like`, `board_view_log`, `content_report`, `work_item`, `work_genre`, `file_metadata`, `team_work_approval_request`, `team_work_approval_genre` |
| 공모전 | `contest`, `contest_open_request`, `contest_save`, `contest_fit_cache`, `contest_submission_prepare` |
| 운영 | `user_sanction`, `notification_template`, `notification_delivery_batch`, `notification`, `audit_log`, `operation_log`, `demo_access_code` |

## 마이그레이션 판단

현재는 SQL 파일을 직접 순서대로 실행하는 구조다. 배포 전 다음 중 하나를 결정해야 한다.

| 선택 | 장점 | 단점 |
|---|---|---|
| 수동 SQL 유지 | 단순함 | 배포/버전 추적 취약 |
| Flyway 도입 | Spring Boot와 통합 쉬움 | 파일명/순서 재구성 필요 |
| Liquibase 도입 | 상세 변경 관리 | 초기 설정 부담 |

## MySQL 8 통합 검증 결과

2026-06-18 실제 MySQL 8 환경에서 다음 항목을 검증했다.

| 항목 | 결과 |
|---|---|
| schema/seed 적용 | 실제 MySQL 8 적용 완료. `99_reset.sql` 실행 후 `01_schema.sql`~`05_seed_ai_matching_dummy_data.sql` 재적용 완료 |
| schema | base table 50개 확인 |
| 주요 seed | 공통 코드 그룹 38건, 공통 코드 133건, 역할 28건, 장르 19건, 전체 sample 사용자 21건, AI dummy 사용자 8건 확인 |
| pending 중복 방지 | 지원 `uq_application_pending`, 초대 `uq_invitation_pending` generated unique 제약의 중복 차단 확인 |
| 슬롯 정원 | 조건부 update가 정원 도달 전 1건, 도달 후 0건을 반영하는 것 확인 |
| backend 연결 | 실제 DB 연결 및 `GET /api/references/genres`의 `success=true`, 장르 19건 응답 확인 |
| 회귀 테스트 | `mvn test` 39개 통과, failures/errors/skipped 0 |

2026-06-22 추가 마이그레이션 검증:

| 항목 | 결과 |
|---|---|
| 포트폴리오 크레딧 | `08_portfolio_credit_name_schema.sql` 2회 적용, `portfolio_item.credit_name` 1개와 임시 procedure 제거 확인 |
| 엔티티 이미지 | `09_entity_image_schema.sql` 2회 적용, 프로필·팀·작업물·포트폴리오 경로 컬럼 4개와 임시 procedure 제거 확인 |
| 게시판 통합 migration | `10_board_full_integration_schema.sql` 2회 적용 완료. 컬럼 3개, 인덱스 2개, 분류 코드 12개, backfill·좋아요 집계·임시 procedure 정리 확인 |
| 게시판 장르 관계 migration | `11_board_search_genre_period_schema.sql` 실제 `slate` DB 2회 적용 완료. 관계 테이블 2개, PK 2개, 보조 인덱스 3개, FK 4개, 고아 데이터 0건 확인 |
| 공모전 이미지 migration | `12_contest_image_schema.sql` 실제 `slate` DB 2회 적용 완료. `contest`, `contest_open_request`에 nullable `varchar(500)` 경로 컬럼이 각 1개임을 확인 |
| 공모전 검색 migration | `13_contest_search_filter_schema.sql` 실제 `slate` DB 2회 적용 완료. 두 테이블에 검색 컬럼 8개씩과 공모전 인덱스 3개를 확인했으며 기존 행의 추정 구조화 값은 0건 |
| 공모전 필터 범위 조정 | `14_remove_contest_benefit_extra_schema.sql`로 이전 개발 중 생성됐을 수 있는 특전·추가 정보 컬럼 제거 경로를 보존. 신규 schema에는 해당 컬럼 없음 |
| Verified fixture | `07_seed_verified_portfolio_ui_demo.sql` 재실행 시 계정별 중복 없이 1건 유지 확인. 운영 적용 금지 |
| 실제 KOBIS 결과 | portfolio item 5 `역린`, 사용자 크레딧 `이재규`, 역할 `감독`이 KOBIS 감독과 일치해 `VERIFIED` 확인 |

2026-06-24 크롤러/Demo Access 선별 이식 후 상태:

| 항목 | 결과 |
|---|---|
| 크롤러 SQL | `01_schema.sql`, `15_contest_crawl_source_schema.sql`, `16_contest_official_link_cleanup.sql`에 코드 반영 |
| Demo Access 코드 관리 SQL | `01_schema.sql`, `02_seed_reference.sql`, `03_seed_sample_data.sql`, `17_demo_access_code_management_schema.sql`, `99_reset.sql`에 코드 반영 |
| 실제 MySQL 적용 | 미수행. 로컬 DB에 15~17 migration 적용 확인 필요 |

실제 DB 비밀번호와 환경변수 값은 문서에 기록하지 않는다. 상세 근거는 `docu/work_logs/2026-06-18_db_mysql_preflight.md`를 따른다.

## 남은 검증

| 항목 | 이유 |
|---|---|
| 실제 동시 HTTP 요청 E2E | 슬롯 정원 방어는 transaction 수준 조건부 update로 검증했으며 동시 수락 HTTP 요청은 미수행 |
| 크롤러/Demo Access migration | `15`, `16`, `17` SQL의 실제 MySQL 적용과 재실행 안전성 확인 필요 |
| 운영 seed 분리 | 배포 데모 seed와 운영 seed 구분 필요 |

## 참조 경로

- `sql/00_create_database.sql`
- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`
- `sql/03_seed_sample_data.sql`
- `sql/04_youtube_metadata_schema.sql`
- `sql/05_seed_ai_matching_dummy_data.sql`
- `sql/07_seed_verified_portfolio_ui_demo.sql`
- `sql/08_portfolio_credit_name_schema.sql`
- `sql/09_entity_image_schema.sql`
- `sql/10_board_full_integration_schema.sql`
- `sql/11_board_search_genre_period_schema.sql`
- `sql/12_contest_image_schema.sql`
- `sql/13_contest_search_filter_schema.sql`
- `sql/14_remove_contest_benefit_extra_schema.sql`
- `sql/15_contest_crawl_source_schema.sql`
- `sql/16_contest_official_link_cleanup.sql`
- `sql/17_demo_access_code_management_schema.sql`
- `sql/99_reset.sql`
- `backend/src/main/resources/mappers`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/work_logs/2026-06-18_db_mysql_preflight.md`
- `docu/work_logs/2026-06-22_fixer_portfolio_credit_roundtrip.md`
- `docu/work_logs/2026-06-22_fixer_profile_media_and_portfolio_display.md`
- `docu/work_logs/2026-06-24_fixer_user2_crawler_demo_access_port.md`
- `docu/13_work_status/current_and_completed_work.md`
