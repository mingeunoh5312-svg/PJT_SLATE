# 소스 참조 지도

## 목적

현재 `Slate` 작업자가 어떤 문서를 어떤 순서로 참조할지 고정한다. 기본 작업은 `Slate` 내부 경로만 사용하고, 과거 prototype 원본은 비교가 필요한 경우에만 읽기 전용으로 참조한다.

## 작업 시작 읽기 순서

| 순서 | 문서 | 이유 |
|---:|---|---|
| 1 | `Agent.md` | 작업자 공통 지침 |
| 2 | `docu/README.md` | 문서 허브 |
| 3 | `docu/00_common/reference_policy.md` | 현재 루트와 참조 제한 확인 |
| 4 | `docu/00_common/document_structure.md` | 문서 작성 규칙 |
| 5 | `docu/03_mvp_scope/mvp_decisions.md` | 사용자 확정 결정 |
| 6 | `docu/13_work_status/current_and_completed_work.md` | 현재 작업 목록, 완료 목록, 충돌 점검 |
| 7 | `docu/11_reviews/code_review_result.md` | 분석 결과와 반영 상태 |
| 8 | `docu/handoff/mvp_documentation_handoff.md` | 다음 작업 순서 |
| 9 | 작업 범위별 기준 문서 | backend/frontend/DB/env/deploy |

## 현재 구현 기준

| 판단 영역 | 우선 참조 | 보조 참조 |
|---|---|---|
| 기능 범위 | `docu/03_mvp_scope/mvp_scope.md` | `docu/03_mvp_scope/mvp_decisions.md` |
| Backend | `backend/src/main/java`, `backend/src/main/resources` | `docu/05_backend/backend_baseline.md` |
| Frontend | `frontend/src`, `frontend/package.json`, `frontend/vite.config.js` | `docu/06_frontend/frontend_baseline.md` |
| DB | `sql`, `backend/src/main/resources/mappers` | `docu/07_database/database_baseline.md` |
| 환경 | `backend/.env.example`, `frontend/.env.example`, `backend/src/main/resources/application-*.yml` | `docu/08_environment` |
| 배포 | `docu/09_deployment/deployment_plan.md` | `docu/08_environment/env_variables.md` |
| 검증 | `docu/11_reviews`, `docu/work_logs` | `docu/prompt` |
| 현재 작업 추적 | `docu/13_work_status/current_and_completed_work.md` | `docu/user_temp/todo.md`, `docu/work_logs` |

## 역할별 참조 체인

| 역할 | 먼저 읽을 문서 | 이후 읽을 문서 |
|---|---|---|
| Backend 작업자 | `docu/05_backend/backend_baseline.md` | `docu/11_reviews/security_environment_findings.md`, `docu/prompt/backend_review_prompt.md` |
| Frontend 작업자 | `docu/06_frontend/frontend_baseline.md` | `docu/11_reviews/test_gap_report.md`, `docu/prompt/frontend_review_prompt.md` |
| DB/환경 작업자 | `docu/07_database/database_baseline.md`, `docu/08_environment/env_variables.md` | `docu/prompt/db_environment_prompt.md` |
| 배포 작업자 | `docu/09_deployment/deployment_plan.md` | `docu/prompt/deployment_smoke_prompt.md` |
| 리뷰 작업자 | `docu/11_reviews/review_plan.md` | `docu/prompt/code_review_prompt.md` |

## 과거 원본 참조

| 대상 | 처리 |
|---|---|
| `../prototype_3/backend` | 현재 `backend`와 차이 검증이 필요할 때만 읽기 |
| `../prototype_3/frontend` | 현재 `frontend`와 차이 검증이 필요할 때만 읽기 |
| `../prototype_3/sql` | 현재 `sql`과 차이 검증이 필요할 때만 읽기 |
| `../prototype_3/assets` | 이미 `assets`에 이식됨. 추가 확인 필요 시 읽기 |
| `../prototype_3/images_page_ai` | 화면 참조 전용. 복사 금지 |
| `../prototype`, `../prototype_2` | `docu/02_prototype_comparison` 보강이 필요할 때만 읽기 |
| `../docu` | 이전 준비 문서 원본. 기본 참조 금지 |

## 참조 경로

- `docu/00_common/reference_policy.md`
- `docu/00_common/document_structure.md`
- `Agent.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/11_reviews/code_review_result.md`
- `docu/prompt/code_review_prompt.md`
- `docu/13_work_status/current_and_completed_work.md`
