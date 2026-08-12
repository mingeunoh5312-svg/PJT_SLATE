# MVP 문서 정리 인수인계

## 다음 작업자가 먼저 볼 문서

1. `Agent.md`
2. `docu/README.md`
3. `docu/00_common/reference_policy.md`
4. `docu/00_common/document_structure.md`
5. `docu/00_inventory/source_reference_map.md`
6. `docu/03_mvp_scope/mvp_decisions.md`
7. `docu/13_work_status/current_and_completed_work.md`
8. `docu/11_reviews/code_review_result.md`
9. 작업 역할에 맞는 `docu/prompt/*.md`

## 현재 결론

| 항목 | 결론 |
|---|---|
| 현재 구현 기준 | `Slate` 내부 `backend`, `frontend`, `sql`, `assets` |
| 이식 출처 | `../prototype_3` |
| 실제 복사 | 완료 |
| 복사 제외 | `node_modules`, `target`, `dist`, `uploads`, `application-local.yml`, `.env`, `images_page_ai` |
| 사용자 질문 | `docu/03_mvp_scope/questions_before_mvp.md`에 답변 반영 완료 |
| 역할별 프롬프트 | `docu/prompt`에 작성 |
| Agent 지침 | `Agent.md` 작성 완료 |
| 1차 코드 반영 | 최종명 변경, stream 권한, demo gate, OpenAI fallback, env example, DB 동시성 일부 |
| DB 검증 | 실제 MySQL 8 schema/seed 적용, reset 후 재적용, pending 중복 제약과 슬롯 조건부 update 검증 완료 |
| 최신 기능 | 사용자 팔로우, 실제 데이터 홈, 매칭 탐색·필터·저장 팀, 팀 상세 계획 route, 프로필 대시보드, 크레딧/KOBIS 검증, 엔티티 대표 이미지, 게시판 검색·분류·랭킹, 공모전 이미지·구조화 검색 반영 |
| 포트폴리오 | 사용자 크레딧과 provider 매칭 결과 분리. 실제 `역린 / 이재규 / 감독` KOBIS 일치 및 `VERIFIED` 확인 |
| 이미지 | 프로필·팀·작업물·포트폴리오 업로드/교체/삭제와 실제 API·권한 검증 완료 |
| 게시판 | 샘플 제거, HOME/WORK/FREE/POPULAR과 `/boards/search`, 자유게시판 분류, 작품 유형·장르, 주간/월간/전체 랭킹, 공개 프로필 연결 검증 완료 |
| 공모전 | 실제 OPEN 목록·마감 임박 API, 직접 이미지, 자동 fit 제거와 수동 분석, 대상·지역·주최·상금 구조화 필터 검증 완료 |
| 크롤러/Demo Access | 콘테스트코리아 크롤러와 DB 코드 관리형 Demo Access 선별 이식 완료. targeted backend tests 128개와 frontend build 통과. 실제 DB migration, live crawl, gate 활성화 smoke는 남음 |
| 검증 | 2026-06-24 TODO 잔여 작업 기준 전체 `backend mvn test` 96 tests 통과, 크롤러/Demo Access 선별 이식 후 targeted 128 tests 통과, 최근 프런트 변경별 `npm run build` 통과 |
| 현재 작업 추적 | `docu/13_work_status/current_and_completed_work.md`를 완료/진행/잔여 작업의 1차 요약으로 사용 |

## 다음 권장 순서

1. `docu/13_work_status/current_and_completed_work.md`의 현재 작업 목록을 먼저 확인한다.
2. 크롤러/Demo Access migration 3개를 실제 MySQL에 적용하고 gate 활성화 브라우저 smoke를 수행한다.
3. 관리자 dry-run 크롤러를 제한 건수로 실행하고 결과 메트릭과 로그를 확인한다.
4. 최신 크레딧·Verified·엔티티 이미지 선택과 관리자/파일/AI 화면을 포함해 남은 route의 desktop/mobile 회귀 smoke를 수행한다.
5. 실제 동시 지원/초대 수락 HTTP 요청 E2E를 수행한다.
6. 실제 YouTube/OpenAI key smoke와 KOBIS 실패·모호·quota 조건을 확인한다.
7. 공개 회사 서류 업로드 보강 정책을 확정한다.
8. 운영 seed 분리와 배포 provider, HTTPS, CORS, 로그 rotation을 배포 단계에서 결정한다.

## 참조 경로

- `Agent.md`
- `docu/03_mvp_scope/copy_plan_from_prototype_3.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/03_mvp_scope/excluded_items.md`
- `docu/03_mvp_scope/questions_before_mvp.md`
- `docu/prompt/README.md`
- `docu/prompt/code_review_prompt.md`
- `docu/11_reviews/code_review_result.md`
- `docu/13_work_status/current_and_completed_work.md`
- `docu/work_logs/2026-06-16_slate_initial_mvp_copy.md`
- `docu/work_logs/2026-06-18_db_mysql_preflight.md`
- `docu/work_logs/2026-06-18_backend_follow.md`
- `docu/work_logs/2026-06-19_frontend_follow.md`
- `docu/work_logs/2026-06-20_matching_navigation_ai_integration.md`
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
