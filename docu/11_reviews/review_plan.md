# 코드 리뷰와 검증 계획

## 목적

MVP 제작 전에 현재 `Slate` 내부 코드와 SQL, 환경, 배포 준비 상태를 검토한다. 이식 전 분석 기록의 `prototype_3` 경로는 보존하지만, 새 리뷰의 기본 대상은 `backend`, `frontend`, `sql`, `assets`다.

## 리뷰 순서

| 순서 | 범위 | 확인 |
|---:|---|---|
| 1 | 문서 기준 | `Agent.md`, `docu/00_common`, `docu/00_inventory`, `docu/03_mvp_scope`, `docu/08_environment` 읽기 |
| 2 | 백엔드 | controller/service/mapper/security/config/test 분석 |
| 3 | 프론트 | router/layout/view/api/assets/style 분석 |
| 4 | DB | schema/seed/reset/migration SQL 분석 |
| 5 | 보안/환경 | secret 노출, `.gitignore`, env example, upload path |
| 6 | 배포 | 로컬 시연, 프론트/백엔드 분리 배포, prod profile, build, proxy, file storage |
| 7 | 테스트 | 기존 테스트와 누락된 smoke/integration |

## 우선 리뷰 포인트

| 우선순위 | 항목 | 이유 |
|---:|---|---|
| 1 | secret/API key 노출 | 배포 전 고위험 |
| 2 | 인증/권한 누락 | 관리자/회사/파일 접근 위험 |
| 3 | 파일 업로드/스트리밍 | 대용량, 경로, 권한, 삭제 정책 |
| 4 | 외부 API fallback | KOBIS/YouTube/OpenAI key 미설정과 장애 |
| 5 | DB 동시성/제약 | 지원/초대/좋아요/조회수 중복 |
| 6 | route guard/SPA refresh | 배포 후 404/무한 redirect 위험 |
| 7 | 데모 접속 코드 gate | 배포 데모 seed 노출 방지 |
| 8 | 최종명 변경 범위 | package/artifact/application/DB 이름 잔존 위험 |
| 9 | 테스트 gap | MVP 제작 전 회귀 위험 |

## 필수 검증

| 검증 | 명령/방법 | 상태 |
|---|---|---|
| 백엔드 테스트 | `mvn test` in `backend` | 2026-06-16 통과. 변경 후 재실행 |
| 백엔드 컴파일 | `mvn -DskipTests compile` in `backend` | 변경 후 필요 시 실행 |
| 프론트 빌드 | `npm run build` in `frontend` | 2026-06-16 통과. 변경 후 재실행 |
| SQL 정적 확인 | schema/reset/seed 순서 검토 | 다음 단계에서 실행 |
| API smoke | 로그인, me, 주요 GET/POST | 백엔드/DB 필요 |
| 브라우저 smoke | 주요 route desktop/mobile | 프론트/백엔드 필요 |
| 외부 API smoke | KOBIS/YouTube/OpenAI | key 필요 |
| 데모 gate smoke | 접속 코드 입력 전/후 접근 | 배포 데모 단계 필수 |

## 결과 문서

| 문서 | 내용 |
|---|---|
| `docu/11_reviews/code_review_result.md` | 전체 findings |
| `docu/11_reviews/security_environment_findings.md` | secret/env/upload/security |
| `docu/11_reviews/test_gap_report.md` | 테스트 누락 |
| `docu/work_logs/YYYY-MM-DD_code_review.md` | 실행 로그 |

## 참조 경로

- `docu/prompt/code_review_prompt.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/08_environment/env_variables.md`
- `backend`
- `frontend`
- `sql`
- `assets`
