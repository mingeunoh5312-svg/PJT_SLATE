# MVP 제작 범위

## 기준

현재 MVP 제작 기준은 `Slate` 내부 구현이다. 과거 `../prototype_3`는 이식 출처와 비교 이력으로만 남기며, 새 작업의 기본 참조는 `backend`, `frontend`, `sql`, `assets`, `docu`다.

## 확정 결정

| 항목 | 기준 |
|---|---|
| 최종 앱 루트 | `<SLATE_ROOT>` |
| 최종명 | `Slate`, `com.slate`, `slate-backend`, `slate-frontend`, DB명 `slate` |
| 외부 API | KOBIS, YouTube Data API, OpenAI AI 매칭 모두 필수 |
| 파일 저장 | MVP 제작 단계에서는 로컬 파일 시스템 우선 |
| 배포 목표 | 로컬 시연과 프론트/백엔드 분리 배포 필수. EC2 단일 서버는 최후순위 |
| 샘플 seed | 배포 데모 포함. 배포 단계에서 접속 코드 gate 필수 |
| 대표 이미지 | `../prototype_3/images_page_ai`는 참조 전용. `Slate` 저장소 보관 금지 |
| 원천 자산 | `assets`의 CSV/SHP 포함 |

## 포함 범위

| 영역 | 포함 기준 | 상태 | 주 경로 |
|---|---|---|---|
| 백엔드 | Spring Boot 4, Security JWT, MyBatis, MySQL 연동 | 구현됨 | `backend` |
| 프론트엔드 | Vue 3 + Vite, Vue Router, App/Auth/Admin layout | 구현됨 | `frontend` |
| DB/SQL | DB명 `slate`, schema, seed, reset, YouTube 추가 SQL | 구현·통합 검증됨 | `sql`, `docu/work_logs/2026-06-18_db_mysql_preflight.md` |
| 인증/계정 | 일반/회사/관리자, 회사 승인, JWT | 구현됨 | `backend/src/main/java/com/slate/accounts`, `frontend/src/views/LoginView.vue` |
| 프로필/포트폴리오 | 프로필 CRUD, 실제 데이터 대시보드, 팔로우, 크레딧 보존, KOBIS 검증, 대표 이미지 | 부분 구현 | `backend/src/main/java/com/slate/profiles`, `backend/src/main/java/com/slate/follows`, `backend/src/main/java/com/slate/media`, `frontend/src/views/ProfileView.vue` |
| 팀/매칭 | 팀 생명주기, 지원/초대, 계획, 필터 기반 추천, 저장 팀, 점수 정책, OpenAI AI 추천 필수 | 부분 구현 | `backend/src/main/java/com/slate/teams`, `backend/src/main/java/com/slate/matching`, `frontend/src/views/MatchingView.vue` |
| 게시판/작업물 | 게시글/리뷰/좋아요/신고, 파일/YouTube 작업물, 검색·분류·장르·랭킹 | 구현·검증 확대 | `backend/src/main/java/com/slate/boards`, `frontend/src/views/BoardView.vue` |
| 공모전 | 목록/상세/저장/제출 준비/회사/관리자 관리, 이미지, 구조화 검색, 적합도 분석, 콘테스트코리아 크롤러 | 구현·검증 확대 | `backend/src/main/java/com/slate/contests` |
| 알림/관리자 | 내부 알림, 권한, 로그, 제재, CRUD | 부분 구현 | `backend/src/main/java/com/slate/admin`, `backend/src/main/java/com/slate/operations` |
| 문서/작업 규칙 | `docu` 기준 문서, 역할별 prompt, Agent 지침 | 구현됨 | `docu`, `Agent.md` |

## 제외 범위

| 제외 | 이유 |
|---|---|
| `../prototype`, `../prototype_2`, `../prototype_3` 원본 구현 코드 수정 | 최종 작업 기준이 아니며 원본 보존 필요 |
| 개발용 seed/reset API | 운영/MVP 기준 불필요 |
| 실제 비밀값 | `.env`, API key, DB 비밀번호, JWT secret은 문서/커밋 금지 |
| 로컬 생성물 | `node_modules`, `target`, `dist`, `uploads`, logs |
| 외부 알림 채널 | 이메일/SMS/푸시/비동기 큐는 현재 구현 기준 아님 |
| S3/CDN/트랜스코딩 | 문서상 확장 항목이며 현재 구현 없음 |
| 운영 물리 삭제 배치와 고아 파일 정리 | 추가 기능. 현재 MVP 필수 범위 제외 |
| 사이트 내 공모전 제출/심사 | 이메일 제출 안내를 유지하므로 제외 |
| 인증 부가 기능 | 이메일 인증, 비밀번호 재설정, 소셜 로그인은 추가 기능 |
| `images_page_ai` 최종 보관 | 제작 중 참조 후 최종 저장소에는 미보관 |

## 구현 상태 판정

| 기능 | 판정 | 근거 |
|---|---|---|
| JWT 로그인/권한 | 구현됨 | `backend/src/main/java/com/slate/security`, `backend/src/main/java/com/slate/accounts` |
| KOBIS Verified | 구현됨/검증 필요 | 상태별 코드·테스트와 `역린 / 이재규 / 감독` 실제 일치 확인. 최신 브라우저 레이아웃 회귀 확인 남음 |
| YouTube 작업물 | 필수/부분 구현 | 코드/테스트와 DB schema 적용 확인 완료, 실제 key/브라우저 확인 남음 |
| OpenAI AI 추천 | 필수/부분 구현 | 테스트/build 있음, 실제 key 호출 남음. 실패 시 점수 기반 추천 |
| 사용자 팔로우 | 구현됨 | 등록·취소·상태·목록 API와 프런트 연결, service/MySQL 및 브라우저 검증 완료 |
| 필터 기반 매칭/저장 팀 | 구현됨/검증 필요 | 실제 API 연결과 build 완료. 저장 팀·필터 적용 최신 변경의 브라우저 회귀 확인 남음 |
| 프로필 실제 데이터 대시보드 | 구현됨/검증 필요 | 샘플 fallback 제거와 build 완료. desktop/mobile 시각 검증 남음 |
| 사용자 크레딧 왕복 | 구현됨/검증 필요 | DB/migration/API mapper/폼 복원과 상태별 테스트 완료. 실제 브라우저 Network 재확인 남음 |
| 엔티티 대표 이미지 | 구현됨/검증 필요 | 프로필·팀·작업물·포트폴리오 API/권한/파일 검증 완료. 브라우저 파일 선택과 비공개 이미지 표시 정책 보완 가능 |
| 게시판 실제 API 화면 | 구현됨 | 샘플 fallback 제거, HOME/WORK/FREE/POPULAR과 `/boards/search`, 자유 분류, 작품 유형·장르, 주간/월간/전체 랭킹, 공개 프로필 route/API 검증 완료 |
| 공모전 실제 데이터 화면 | 구현됨 | 샘플·추천 hero·자동 fit 제거, 실제 OPEN 목록·마감 임박 API, 이미지 업로드, 구조화 검색 필터, 명시 실행형 적합도 검증 완료 |
| 관리자 게시글/회원/팀 CRUD | 부분 구현 | API 구현 있음, 브라우저 검증/테스트 데이터 정리 남음 |
| Demo access gate | 구현됨/검증 필요 | route/API gate와 DB 코드 관리 기능 구현. `sql/17_demo_access_code_management_schema.sql` 실제 적용과 gate 활성화 smoke 남음 |
| 배포 | 계획 확정 | 로컬 시연/분리 배포 필수. HTTPS/CORS/log rotation/ffprobe는 보류 |
| 콘테스트코리아 크롤러 | 구현됨/검증 필요 | 크롤링/파싱/정규화/upsert와 관리자 실행 API, 출처/포스터 SQL 반영. 실제 MySQL migration과 live dry-run 남음 |

## 남은 결정과 검증

| 항목 | 이유 |
|---|---|
| 실제 동시 HTTP 요청 E2E | pending 중복 방지와 슬롯 조건부 update는 검증 완료. 동시 수락 HTTP 요청은 미수행 |
| 실제 외부 API smoke | KOBIS 일치 호출 1건 확인. YouTube/OpenAI key·quota·fallback과 KOBIS 실패 조건 확인 필요 |
| 브라우저/모바일 smoke | 게시판·공모전 주요 route는 완료. 관리자/파일/AI/크레딧·엔티티 이미지 최신 화면 확인 필요 |
| 크롤러/Demo Access 이식 후속 | targeted backend tests 128개와 frontend build는 통과. 실제 DB migration, Demo Access 활성화 브라우저 smoke, 크롤러 dry-run, 보호 이미지 전체 회귀 확인 필요 |
| 공개 회사 서류 업로드 보강 | 1회성 token/rate limit 정책 결정 필요 |
| 배포 provider | 프론트/백엔드 분리 배포 대상 선택 필요 |
| 운영 seed 분리 | 데모 seed와 운영 seed의 배포 기준 결정 필요 |

## 참조 경로

- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/03_mvp_scope/copy_plan_from_prototype_3.md`
- `docu/11_reviews/code_review_result.md`
- `docu/work_logs/2026-06-18_db_mysql_preflight.md`
- `docu/work_logs/2026-06-18_backend_follow.md`
- `docu/work_logs/2026-06-19_frontend_follow.md`
- `docu/work_logs/2026-06-21_fixer_matching_saved_teams.md`
- `docu/work_logs/2026-06-21_fixer_matching_team_filter_apply.md`
- `docu/work_logs/2026-06-21_fixer_profile_dashboard.md`
- `docu/work_logs/2026-06-22_fixer_portfolio_credit_roundtrip.md`
- `docu/work_logs/2026-06-22_fixer_profile_media_and_portfolio_display.md`
- `docu/work_logs/2026-06-22_fixer_board_full_integration.md`
- `docu/work_logs/2026-06-22_fixer_board_search_ui_period_ranking_followup.md`
- `docu/work_logs/2026-06-23_fixer_contest_data_ui_image_fit.md`
- `docu/work_logs/2026-06-23_fixer_contest_structured_search_filters.md`
- `docu/work_logs/2026-06-24_fixer_user2_crawler_demo_access_port.md`
- `docu/13_work_status/current_and_completed_work.md`
- `backend`
- `frontend`
- `sql`
- `assets`
