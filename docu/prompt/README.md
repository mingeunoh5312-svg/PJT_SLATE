# 역할별 프롬프트 안내

## 목적

이 폴더는 새 대화창에서 역할별로 바로 사용할 프롬프트를 보관한다. 모든 프롬프트는 현재 `Slate` 내부 경로를 기준으로 하며, 과거 prototype 원본은 필요한 경우 읽기 전용 비교 자료로만 다룬다.

## 프롬프트 목록

| 파일 | 용도 |
|---|---|
| `code_review_prompt.md` | 전체 코드/보안/환경/테스트 분석 |
| `backend_review_prompt.md` | backend API, 보안, 외부 API, 파일 처리 분석 |
| `frontend_review_prompt.md` | frontend route, UI smoke, env, build 분석 |
| `db_environment_prompt.md` | SQL, MySQL, env, secret 분리 분석 |
| `deployment_smoke_prompt.md` | 로컬 시연과 분리 배포 전 smoke 계획/검증 |
| `follow_backend_prompt.md` | 사용자 팔로우 등록·취소·상태·목록 백엔드 구현 |
| `follow_frontend_prompt.md` | 매칭 후보 팔로우와 내 프로필 팔로워·팔로잉 UI 구현 |
| `home_dashboard_01_data_structure_prompt.md` | 홈 실제 API 데이터와 로그인 상태 분기 구현 |
| `home_dashboard_02_ui_responsive_prompt.md` | 홈 대시보드·랜딩 UI와 반응형 구현 |
| `home_dashboard_03_integration_validation_prompt.md` | 홈 계정별 통합 검증과 수정 |
| `matching_navigation_ai_integration_prompt.md` | 매칭 시작/AI 독립 탭을 제거하고 팀원·팀 탐색 내부로 AI 추천 통합 |
| `matching_basis_selection_fixer_prompt.md` | 팀원 매칭 기준 팀·모집 역할 변경 및 전체 필터 초기화 결함을 프런트에서만 수정 |
| `matching_empty_result_reset_only_fixer_prompt.md` | 팀 찾기를 실제 백엔드 데이터 기반으로 정리하고 팀원 찾기와 UI·UX 통합 |
| `portfolio_credit_roundtrip_verification_fixer_prompt.md` | 포트폴리오 사용자 크레딧 이름 보존·조회와 KOBIS 매칭 결과 분리 수정 |
| `profile_remove_work_search_button_fixer_prompt.md` | 프로필 화면에서 불필요한 작업물 검색 버튼 제거 |
| `profile_team_work_portfolio_image_fixer_prompt.md` | 프로필·팀·작업물·포트폴리오 대표 이미지 업로드/표시 수정 |
| `board_full_integration_fixer_prompt.md` | 게시판 HOME/WORK/FREE/POPULAR, 검색·분류·랭킹·공개 프로필 통합 수정 |
| `board_search_ui_period_ranking_followup_fixer_prompt.md` | 게시판 검색 route, 장르, 기간 랭킹, WORK/FREE UX 후속 수정 |
| `contest_data_ui_image_fit_fixer_prompt.md` | 공모전 샘플 제거, 실제 데이터 목록, 이미지, 적합도 UI 수정 |
| `todo_common_home_matching_team_fixer_prompt.md` | `todo.md` 기반 공통·메인·매칭·팀 페이지 수정 |

## 작성 규칙

- 프롬프트는 `Agent.md`와 `docu/00_common/reference_policy.md`를 먼저 읽도록 한다.
- 분석 대상은 `backend`, `frontend`, `sql`, `assets`, `docu`를 우선한다.
- 외부 폴더를 참조해야 하면 읽기 전용 이유를 프롬프트에 명시한다.
- 결과 문서 위치는 `docu/11_reviews`, 작업 로그 위치는 `docu/work_logs`로 둔다.

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/11_reviews/review_plan.md`
