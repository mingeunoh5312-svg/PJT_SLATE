# 2026-06-16 Code Review Work Log

## Scope

`docu/prompt/code_review_prompt.md`의 지시에 따라 `prototype_3`를 MVP 이식 전 기준으로 리뷰했다. 구현 코드는 수정하지 않고 문서만 작성했다.

## Documents Read

- `docu/00_common/document_structure.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/03_mvp_scope/mvp_scope.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/03_mvp_scope/copy_plan_from_prototype_3.md`
- `docu/03_mvp_scope/questions_before_mvp.md`
- `docu/04_architecture/architecture_baseline.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/08_environment/env_variables.md`
- `docu/09_deployment/deployment_plan.md`
- `docu/11_reviews/review_plan.md`

## Code Areas Reviewed

- backend security/JWT/CORS.
- admin permission catalog and service-level permission checks.
- KOBIS, YouTube, OpenAI clients and related services.
- board/work file upload, stream, moderation flow.
- company document upload/download flow.
- matching/team application/invitation SQL and mapper flow.
- frontend router guard, API client, login demo UI, Vite config.
- SQL schema, YouTube metadata migration, sample seed, AI dummy seed, reset script.

## Validation

### Backend

`prototype_3/backend`에서 `mvn test`를 실행했다.

- 최초 실행: Maven central 접근 차단으로 parent POM resolve 실패.
- 승인 후 재실행: 성공.
- 결과: 36 tests, failures 0, errors 0, skipped 0.

### Frontend

`prototype_3/frontend`에서 `npm run build`를 실행했다.

- 최초 실행: 실패.
- 승인 후 재실행: 동일 실패.
- 실패 지점: Vite `prepare-out-dir`.
- 오류: `prototype_3/frontend/dist/local-run` 삭제 `EPERM`.
- 확인: `dist/local-run`에는 Vite server 로그와 `vite-server.mjs`가 있었고, 기존 node 프로세스 1개가 실행 중이었다.

## Outputs Created

- `docu/11_reviews/code_review_result.md`
- `docu/11_reviews/security_environment_findings.md`
- `docu/11_reviews/test_gap_report.md`
- `docu/work_logs/2026-06-16_code_review.md`

## Main Findings

- P1: work file stream authorization missing.
- P1: demo account exposure without access code gate.
- P1: OpenAI call failure does not fall back to score-based recommendations.
- P2: public company document upload is write-enabled with weak proof.
- P2: local/default security environment settings remain in shared config.
- P2: team application/invitation and slot acceptance need DB-level concurrency controls.
- P2: board view IP hash is unsalted while audit hash is salted.
- P3: `prototype2` naming remains across backend, mapper, SQL, frontend.
- P3: frontend build script currently fails because `dist/local-run` cannot be cleared.

## Not Changed

- No implementation source files were edited.
- No SQL schema or seed files were edited.
- No generated build output was intentionally cleaned or deleted.
- Existing node process was not stopped.

## Follow-up Order

1. Decide and implement file stream authorization.
2. Add demo access code gate in frontend and backend.
3. Align OpenAI fallback behavior with MVP decision.
4. Split local/demo/prod environment files and remove production defaults.
5. Add DB constraints/conditional updates for team concurrency.
6. Resolve frontend `dist/local-run` lock and rerun production build.
7. Add targeted backend tests and minimum browser smoke.
