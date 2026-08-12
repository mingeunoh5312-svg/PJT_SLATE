# 현재 작업 목록과 완료 목록

작성일: 2026-06-26

## 목적

`docu/user_temp/todo.md`, `docu/work_logs`, `docu/handoff`에 흩어진 작업 상태를 현재 `Slate` 기준으로 한 곳에 모은다. 원본 TODO와 작업 로그는 보존하고, 후속 작업자는 이 문서를 현재 작업 추적의 1차 요약으로 본다.

## 상태 기준

| 상태 | 의미 |
|---|---|
| 완료 | 코드 또는 문서 반영과 기록된 검증이 모두 있음 |
| 구현됨/검증 필요 | 코드 반영은 있으나 브라우저, 실제 DB, 외부 API, 전체 회귀 검증 중 일부가 남음 |
| 부분 구현 | 일부 화면, 일부 API, 일부 데이터 흐름만 반영됨 |
| 대기 | 사용자 결정, 자산 준비, 외부 환경, 배포 환경이 필요함 |
| 문서 충돌 | 문서 간 표현이 달라 현재 기준을 명시해야 함 |

## 현재 작업 목록

| 우선순위 | 작업 | 현재 상태 | 남은 확인 |
|---:|---|---|---|
| 0 | Lovable 검정/흰색 디자인 시안 프론트 이식 | 구현됨/검증 필요 | 홈, 탐색, 작업물, 공모전, AI 로케이션, 팀 상위/상세, 공개 프로필, 작업공간, 관리자 상위 시안, auth 화면 테마 정합성과 공통 header/footer, 테마 토큰은 반영됨. 레거시 route 정리와 반응형 시각 QA는 `docu/13_work_status/lovable_design_porting_status.md`에서 추적 |
| 1 | 콘테스트코리아 크롤러와 Demo Access DB 코드 관리 | 구현됨/검증 필요 | `sql/15_contest_crawl_source_schema.sql`, `sql/16_contest_official_link_cleanup.sql`, `sql/17_demo_access_code_management_schema.sql` 실제 MySQL 적용, dry-run 크롤러, `VITE_DEMO_ACCESS_GATE=true` + `slate.demo-access.enabled=true` 브라우저 smoke |
| 2 | Demo Access 보호 이미지/파일 흐름 | 구현됨/검증 필요 | `GET /api/media/images/**` Demo Access 필터 예외와 seed 이미지 런타임 경로 복사를 적용했다. 실제 브라우저 hard refresh와 파일 다운로드류 보호 정책 확인은 후속 |
| 3 | 매칭 페이지 액션/상세/목록 | 구현됨/검증 필요 | 초대한 팀원/지원한 팀 상세 이동, 저장/지원/초대 취소 흐름의 실제 로그인 브라우저 회귀 확인 |
| 4 | 매칭 필터 UI와 기준 팀 구인 현황 | 구현됨/검증 필요 | 지역 필터는 AI 로케이션 탐색의 `지역 / 세부 입력` 복수 선택 방식으로 반영됐다. 장르 필터는 원본 TODO의 자동완성 방식이 아니라 체크박스 복수 선택으로 구현됨. 실제 브라우저 smoke와 장르 방식 사용자 확인 필요 |
| 5 | 팀 상세·계획·목록 UI | 구현됨/검증 필요 | 실제 로그인 데이터로 팀 정보, 전체 일정, 새 계획, 팀 종료 화면 최종 시각 QA 필요 |
| 6 | 데스크톱 고정 폭 정책 | 구현됨/검증 필요 | 1310px, 1180px, 960px, 921px, 920px 이하 실제 브라우저 시각 검증 필요 |
| 7 | 공통 안내/오류/성공 메시지 정책 | 부분 구현 | 팀 종료 확인 UI와 일부 action 주변만 정리됨. 전역 toast/banner 제거와 버튼 하단 메시지 일관화는 남음 |
| 8 | 메인 카드 배경과 기본 이미지 자산 | 대기 | 임의 이미지는 추가하지 않음. 필요 목록은 `docu/user_temp/todo_common_home_matching_team_image_requirements.md`에서 추적 |
| 9 | 실제 외부 API smoke | 대기 | YouTube, OpenAI key/쿼터/fallback, KOBIS 실패·모호 조건 확인 필요 |
| 10 | 실제 동시 HTTP 요청 E2E | 대기 | DB 제약과 조건부 update는 확인됐으나 동시 수락/지원/초대 HTTP E2E는 미수행 |
| 11 | 배포 결정 | 대기 | 운영 seed 분리, provider, HTTPS, CORS, log rotation 결정 필요 |
| 12 | 공개 회사 서류 업로드 보강 | 대기 | 1회성 token, rate limit, 공개 접근 정책 결정 필요 |
| 13 | 지역 DB 전역화 SQL 실제 적용 | 완료 | `sql/27_seed_korea_regions.sql` 실제 MySQL 적용, `sql/28_validate_korea_regions.sql` 검증, 기존 12개 더미 참조 재매핑, 관리자 지역 API smoke 확인 완료. 남은 항목은 `/admin/regions` 실제 브라우저 시각 smoke |

## 완료 목록

| 영역 | 완료 내용 | 검증/근거 |
|---|---|---|
| 앱 이식 | `backend`, `frontend`, `sql`, `assets`, `docu`를 `Slate` 기준으로 이식 | `docu/work_logs/2026-06-16_slate_initial_mvp_copy.md` |
| 최종명 변경 | `com.slate`, `slate-backend`, `slate-frontend`, DB명 `slate` 적용 | `docu/03_mvp_scope/mvp_decisions.md` |
| 문서 구조 | `Agent.md`, `docu/README.md`, 공통 정책, 역할별 prompt, handoff 구성 | `docu/work_logs/2026-06-16_slate_docu_agent_setup.md` |
| DB 기본 검증 | 실제 MySQL 8 schema/seed/reset 적용, pending 중복 제약과 슬롯 조건부 update 확인 | `docu/work_logs/2026-06-18_db_mysql_preflight.md` |
| 팔로우 | 등록, 취소, 상태, 목록 API와 프론트 연결 | `docu/work_logs/2026-06-18_backend_follow.md`, `docu/work_logs/2026-06-19_frontend_follow.md` |
| 홈 | 실제 데이터 기반 홈, 알림/요약/YouTube 썸네일 보강 | `docu/work_logs/2026-06-19_home_dashboard_data.md`, `docu/work_logs/2026-06-20_frontend_home_youtube_thumbnail.md` |
| 프로필 대시보드 | 샘플 fallback 제거, 실제 프로필/팔로우/활동 데이터 중심 표시 | `docu/work_logs/2026-06-21_fixer_profile_dashboard.md` |
| 포트폴리오 크레딧 | 사용자 입력 크레딧과 KOBIS provider 매칭 결과 분리 저장·조회 | `docu/work_logs/2026-06-22_fixer_portfolio_credit_roundtrip.md` |
| Verified 배지 | `VERIFIED` 상태에만 배지 표시, 실제 KOBIS 일치 fixture 확인 | `docu/work_logs/2026-06-22_fixer_verified_portfolio_badge.md` |
| 엔티티 대표 이미지 API | 프로필·팀·작업물·포트폴리오 업로드/교체/삭제 API와 파일 검증 | `docu/work_logs/2026-06-22_fixer_profile_media_and_portfolio_display.md` |
| 게시판 통합 | HOME/WORK/FREE/POPULAR, `/boards/search`, 분류, 장르, 랭킹, 공개 프로필 연결 | `docu/work_logs/2026-06-22_fixer_board_full_integration.md`, `docu/work_logs/2026-06-22_fixer_board_search_ui_period_ranking_followup.md` |
| 공모전 실제 데이터 | 샘플 제거, 실제 OPEN 목록, 마감 임박, 이미지 업로드, 명시 실행형 적합도 | `docu/work_logs/2026-06-23_fixer_contest_data_ui_image_fit.md` |
| 공모전 구조화 검색 | 대상, 지역, 주최, 상금 필터와 URL/새로고침 복원 | `docu/work_logs/2026-06-23_fixer_contest_structured_search_filters.md` |
| 공통·메인·매칭·팀 TODO 1차 | 매칭 하위 탭, 팔로우 탭, 팀 목록 분리, 팀 상세 재배치, 공모전 하트 UI 등 | `docu/work_logs/2026-06-23_todo_common_home_matching_team_fixer.md` |
| 공통·메인·매칭·팀 TODO 잔여 일부 | 알림 패널 노출 제한, 팀 생성/수정 헤더, 팀 종료 확인 UI, 진행/종료 상태 상수 보강 | `docu/work_logs/2026-06-24_todo_common_home_matching_team_fixer_remaining.md` |
| 관리자 페이지 정리 | 권한 기반 메뉴 노출, 실제 데이터 기준 대시보드 수치, 승인/거절/상태 변경 사유 입력, 콘솔형 관리자 UI 재구성 | `docu/work_logs/2026-06-25_fixer_admin_page_ui_cleanup.md` |
| 지역 DB 전역화 기반 구현 | 현행 leaf 시군구 259개 seed SQL 생성, VWorld SHP 기반 좌표 산출, 기존 12개 더미 참조 재매핑 후 삭제, `REGION_MANAGE` 권한, `/api/admin/regions`, `/admin/regions` 관리 화면 구현 | `docu/work_logs/2026-06-26_fixer_korea_region_db_nationwide.md` |
| 더미 이미지·지역 DB·매칭 지역 필터 후속 적용 | Demo Access 이미지 GET 예외, seed 이미지 런타임 반영, CDV 볼륨 seed 재적용, 지역 DB 실제 적용·검증, 매칭 필터 `지역 / 세부 입력` 구조 반영 | `docu/work_logs/2026-06-26_fixer_demo_media_region_matching_followup.md` |

## 문서 중복/충돌 점검

| 항목 | 충돌 또는 중복 | 현재 기준 |
|---|---|---|
| 원본 TODO와 작업 로그 | `docu/user_temp/todo.md`는 원본 요구사항이고, 6/23~6/24 작업 로그는 일부 항목을 완료 처리한다. 원본 파일에는 완료 표시가 없다 | 원본 TODO는 보존한다. 현재 완료/잔여 판단은 이 문서와 관련 work log를 우선한다 |
| Demo Access 검증 결과 | `docu/handoff/demo_access_first_filter_porting_guide.md`는 원본 이식 문서로 `mvn test` 232 tests와 브라우저 smoke를 기록한다. 현재 `Slate` 선별 이식 로그는 targeted 128 tests 통과, 실제 DB migration과 브라우저 smoke 미수행으로 기록한다 | 현재 `Slate`의 검증 상태는 `docu/work_logs/2026-06-24_fixer_user2_crawler_demo_access_port.md`를 우선한다 |
| 보호 미디어 범위 | Demo Access 인수인계는 이미지/영상/파일 blob 요청 전체 흐름을 설명한다. 현재 이식 로그는 공모전/홈 공모전 이미지 적용만 완료로 적는다 | 전체 보호 미디어 완료로 쓰지 않는다. 공모전/홈 적용 완료, 그 외 화면은 smoke 후 추가 적용 |
| 매칭 장르 필터 방식 | `todo.md`는 장르 자동완성 입력을 요구하지만, 최신 매칭 필터 작업은 체크박스 복수 선택으로 구현했다 | 체크박스 구현은 현재 코드 기준이다. 자동완성으로 바꿀지는 사용자 확인 필요 |
| 팀 목록 진행률 | TODO는 팀 계획 진행률을 요구하지만 현재 목록 API에는 계획 진행률 필드가 없어 멤버 충원률 기준으로 표시한다 | 실제 계획 진행률이 필요하면 backend 목록 응답 확장이 별도 작업이다 |
| 테스트 수치 | 문서마다 39, 51, 60, 69, 74, 83, 86, 96, 128, 232 tests가 나온다 | 테스트 수치는 해당 시점의 로그로만 해석한다. 최신 전체 기준은 6/24 TODO 잔여 작업의 96 tests 통과이고, 크롤러/Demo Access 이식 후에는 targeted 128 tests만 현재 `Slate` 기준으로 확인됐다 |
| `docu/00_inventory/document_inventory.md` 문서 개수 | 초기 이식 전 인벤토리 수치가 보존되어 현재 `Slate/docu` 114개 Markdown과 다르다 | 인벤토리는 역사/출처 분류 문서다. 현재 문서 수와 상태 추적은 이 문서를 우선한다 |

## 다음 작업 순서

1. 실제 MySQL에 크롤러/Demo Access migration 3개를 적용하고 DB 기준 문서에 반영한다.
2. Demo Access gate를 프론트/백엔드 모두 켠 상태로 `/`, `/contests`, `/admin/demo-access`, 보호 이미지 blob 로딩을 smoke한다.
3. 관리자 dry-run 크롤러를 제한 건수로 실행하고 결과 메트릭과 운영 로그를 확인한다.
4. 최신 매칭/팀 화면을 로그인 계정으로 브라우저 회귀 확인한다.
5. `mvn test` 전체를 현재 코드 기준으로 재실행해 128개 targeted 이후의 전체 수치를 갱신한다.
6. 보호 이미지가 깨지는 화면이 있으면 `ProtectedImage` 또는 보호 리소스 helper를 추가 적용한다.
7. 외부 API smoke, 동시 HTTP E2E, 배포 결정 항목을 순서대로 처리한다.

## 참조 경로

- `docu/user_temp/todo.md`
- `docu/user_temp/todo_common_home_matching_team_image_requirements.md`
- `docu/work_logs/2026-06-23_todo_common_home_matching_team_fixer.md`
- `docu/work_logs/2026-06-24_todo_common_home_matching_team_fixer_remaining.md`
- `docu/work_logs/2026-06-24_fixer_matching_filter_table_region_dropdown.md`
- `docu/work_logs/2026-06-24_matching_page_action_follow_request_log.md`
- `docu/work_logs/2026-06-24_fixer_team_page_detail_plan_ui.md`
- `docu/work_logs/2026-06-24_fixer_frontend_fixed_desktop_width.md`
- `docu/work_logs/2026-06-24_fixer_user2_crawler_demo_access_port.md`
- `docu/work_logs/2026-06-25_fixer_admin_page_ui_cleanup.md`
- `docu/work_logs/2026-06-26_fixer_demo_media_region_matching_followup.md`
- `docu/handoff/user2_crawler_and_filter_port_2026-06-24.md`
- `docu/handoff/demo_access_first_filter_porting_guide.md`
- `docu/README.md`
- `docu/00_common/document_structure.md`
- `docu/03_mvp_scope/mvp_scope.md`

## 2026-06-25 추가 반영

- 공모전 크롤링 후속 보완 작업 로그를 추가했다.
- 주요 범위: 콘테스트코리아 크롤링 페이지/건수 보정, 사진 단일 주제 제외, 관리자 크롤링 결과 전체 목록/필터/페이지, 관리자 공모전 선택 삭제, 공모전 기본 이미지, 관리자 직접 등록 대표 이미지 업로드, 관리자 생성 공모전 이미지 권한, 포스터 Content-Type 불일치 대응.
- 검증 근거: `docu/work_logs/2026-06-25_fixer_contest_crawler_followup.md`

## 2026-06-25 디자인 검토 추가 반영

- 공통 레이아웃, 좌측바, 홈, 매칭, 팀 페이지 재구성 작업 로그를 추가했다.
- 주요 범위: 1920px 최대 폭과 좌측 정렬, 상단 환영 영역 배경 복구, 좌측바 이미지·프로필 이동, 홈 카드 여백과 버튼 균형, 매칭 팀 없는 검색·AI 추천·상세 상태 동기화·페이지네이션, 팀 내 팀 요약·모집 공고·지원/초대 현황 분리.
- 상태: 프런트 build와 정적 검증은 통과했으며, 실제 로그인 계정 기반 브라우저 smoke와 백엔드 Maven 테스트는 후속 확인이 필요하다.
- 검증 근거: `docu/work_logs/2026-06-25_fixer_design_review_common_home_matching_team.md`

## 2026-06-25 팀 모집과 AI 로케이션 탐색 추가 반영

- 팀 모집 페이지와 팀 지원/초대 현황 페이지 재구성 작업 로그를 추가했다.
- 공동 작업자 브랜치의 AI 로케이션 추천 기능을 현재 로컬 작업물 기준으로 이식하고, 로케이션 DB 기반 후보 수집·저장 후보·지도·추천 카드 흐름을 연결했다.
- 주요 범위: 팀 모집 목록/수정/구인 공고 UI 정리, 지원/초대 현황 화면 재구성, AI 로케이션 prompt 품질 보정, 폐업/철거 등 데이터 주의 표시, 팀 컨텍스트 반영, 전국 검색 지역별 후보 수집, 개인/팀 저장 후보 표시, 지역 combobox UI, 추천 결과 선택 시 지도 확대.
- 상태: 백엔드 targeted 테스트와 프런트 build는 통과했으며, 실제 OpenAI key 기반 live 응답 품질과 로그인 계정 브라우저 시각 회귀 확인은 후속 작업으로 남았다.
- 검증 근거: `docu/work_logs/2026-06-25_fixer_team_recruitment_location_ai.md`

## 2026-06-26 지역 DB 전역화 추가 반영

- 지역 DB 전역화 작업 로그를 추가했다.
- 주요 범위: `code.go.kr` 현행 법정동코드 기준 leaf 시군구 259개 추출, VWorld `국가기본도_시군구구역경계` SHP/DBF 기반 좌표 산출, `sql/27_seed_korea_regions.sql` 최초 1회 적용 SQL 생성, `sql/28_validate_korea_regions.sql` 검증 쿼리, 기존 12개 더미 지역 참조 재매핑 후 삭제, `REGION_MANAGE` 권한, 관리자 지역 조회/수정 API와 `/admin/regions` 화면.
- 당시 상태: SQL 정적 검증, 프런트 build, 백엔드 `mvn compile`은 통과했다. 실제 MySQL 적용과 실제 관리자 계정 브라우저 smoke는 후속 작업으로 남았다.
- 후속 반영: 실제 MySQL 적용과 SQL 검증, 관리자 지역 API smoke는 `2026-06-26 더미 이미지·지역 DB·매칭 필터 후속 반영`에서 완료됐다.
- 당시 알려진 검증 이슈: 전체 `mvn test`는 기존 `BoardServiceFullIntegrationTest`의 `BoardMapper.selectWorkRanking` 시그니처 불일치로 testCompile에서 실패했다. 후속 작업에서 테스트 시그니처를 보정했고, 백엔드 package 검증은 `-DskipTests` 기준으로 통과했다.
- 검증 근거: `docu/work_logs/2026-06-26_fixer_korea_region_db_nationwide.md`

## 2026-06-26 더미 이미지·지역 DB·매칭 필터 후속 반영

- Demo Access 필터가 더미 이미지 API를 차단하던 문제와 런타임 업로드 경로에 seed 이미지 파일이 없던 문제를 정리했다.
- `SLATE_UPLOAD_DIR=uploads` 정책은 유지하고, 더미 데이터 확인을 위해 `backend/uploads/images/seed`에 seed 이미지를 복사하는 방식으로 실제 사용자 업로드 경로 정책을 침해하지 않도록 기록했다.
- 지역 DB 전역화 SQL을 현재 로컬 MySQL에 실제 적용했고, active 지역 259개와 기존 팀/프로필 참조 누락 0건을 검증했다.
- CDV 볼륨 seed 재적용 중 `notification` FK 정리 누락을 수정한 뒤 더미 볼륨 데이터와 생성 이미지 SQL을 다시 적용했다.
- 팀원/팀 매칭 필터의 지역 입력을 AI 로케이션 탐색과 같은 `지역 / 세부 입력` 복수 선택 구조로 변경했다.
- 검증 근거: `docu/work_logs/2026-06-26_fixer_demo_media_region_matching_followup.md`

## 2026-06-27 Lovable 디자인 이식 추가 반영

- `design/design_dark_light_theme`의 검정/흰색 시안을 현재 Vue 프론트에 단계적으로 이식하는 작업 상태 문서를 추가했다.
- 주요 완료 범위: 공통 header/footer, 검정 기본/흰색 선택 테마 토큰, 홈, 탐색, 작업물, 공모전, AI 로케이션, 팀 상위/상세, 공개 프로필, 작업공간, 관리자 상위 화면, auth 화면 테마 정합성.
- 남은 범위: 레거시 route 정리, desktop/mobile 시각 QA.
- 추적 문서: `docu/13_work_status/lovable_design_porting_status.md`
