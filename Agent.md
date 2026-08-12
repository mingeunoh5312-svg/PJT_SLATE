# Agent.md

## 역할

당신은 Slate 프로젝트의 MVP 제작 및 검증 작업자다. 현재 작업 루트는 각자의 로컬 `Project_Slate/Slate` 폴더이며, 이 문서에서는 이를 `<SLATE_ROOT>`로 부른다. 새 작업의 기본 기준은 현재 `Slate` 내부 구현과 `docu` 문서다.

## 필수 읽기 순서

1. `docu/README.md`
2. `docu/00_common/reference_policy.md`
3. `docu/00_common/document_structure.md`
4. `docu/00_inventory/source_reference_map.md`
5. `docu/03_mvp_scope/mvp_decisions.md`
6. `docu/03_mvp_scope/mvp_scope.md`
7. `docu/13_work_status/current_and_completed_work.md`
8. `docu/11_reviews/code_review_result.md`
9. `docu/handoff/mvp_documentation_handoff.md`
10. 작업 범위별 기준 문서와 `docu/prompt/*.md`

## 기준 경로

| 구분 | 경로 |
|---|---|
| Backend | `backend` |
| Frontend | `frontend` |
| SQL | `sql` |
| Assets | `assets` |
| 문서 | `docu` |
| 역할별 프롬프트 | `docu/prompt` |
| 환경변수 문서 | `docu/08_environment` |
| 배포 문서 | `docu/09_deployment/deployment_plan.md` |

## 확정 작업 기준

| 항목 | 기준 |
|---|---|
| 최종명 | `Slate`, `com.slate`, `slate-backend`, `slate-frontend`, DB명 `slate` |
| 외부 API | KOBIS, YouTube Data API, OpenAI AI 매칭은 필수 기능 |
| 파일 저장 | MVP 제작 단계에서는 로컬 파일 시스템 기준 |
| 배포 | 로컬 시연과 프론트/백엔드 분리 배포 필수. EC2 단일 서버는 최후순위 |
| 데모 seed | 배포 데모 포함. 웹 페이지 접근 전 접속 코드 gate 필수 |
| `images_page_ai` | 참조 전용. `Slate` 저장소에 복사/보관하지 않음 |
| `assets` | CSV/SHP 원천 자산으로 포함 |

## 금지 사항

- 사용자 승인 없이 `../prototype`, `../prototype_2`, `../prototype_3` 원본 구현 코드를 수정하지 않는다.
- `../docu`는 이전 준비 문서 원본이므로 기본 참조하지 않는다.
- Slate 루트 밖 문서를 직접 참조해야 하면 먼저 사용자에게 질문한다.
- 실제 API key, DB 비밀번호, JWT secret, `.env` 값을 문서나 답변에 쓰지 않는다.
- `node_modules`, `target`, `dist`, `uploads`, `application-local.yml`을 복사하거나 커밋 대상으로 삼지 않는다.
- 문서에만 있는 기능을 구현 완료로 쓰지 않는다.
- 실패한 검증이나 미수행 검증을 숨기지 않는다.

## 작업 로그 규칙

- 새 작업을 하면 `docu/work_logs/YYYY-MM-DD_{role}_{summary}.md`를 작성한다.
- 다음 작업자에게 넘길 내용이 있으면 `docu/handoff/{role}_to_{role}_{summary}.md` 또는 기존 handoff 문서를 갱신한다.
- 모든 로그에는 작업 범위, 참조 경로, 실행한 명령, 결과, 남은 이슈를 적는다.

## 환경변수 정책

- 실제 값은 로컬 OS 환경변수, IDE Run Configuration, 배포 secret manager 중 하나로 주입한다.
- 예시는 `.env.example`, `backend/.env.example`, `frontend/.env.example`, `frontend/.env.production.example`, `application-local.yml.example`에만 둔다.
- 프론트에는 `KOBIS_API_KEY`, `YOUTUBE_API_KEY`, `OPENAI_API_KEY`, `SLATE_DEMO_ACCESS_CODE`를 넣지 않는다.
- `SLATE_DEMO_ACCESS_CODE`는 backend/배포 secret으로만 관리한다.
- 로컬/배포 차이는 `docu/08_environment` 문서에 반영한다.

## 로컬과 배포 차이

| 항목 | 로컬 | 배포 |
|---|---|---|
| Frontend | Vite dev server `5174` | 정적 build + reverse proxy 또는 별도 hosting |
| Backend | Spring Boot `8080` | service/jar 또는 컨테이너 |
| API | Vite proxy `/api` | same-origin proxy 또는 `VITE_API_BASE_URL` |
| DB | 로컬 MySQL | 운영 MySQL 서버 |
| Upload | `uploads` 또는 로컬 경로 | 운영 저장소/백업/삭제 정책 필요 |
| Secret | 로컬 환경변수 | 배포 secret |
| Demo access | 기본 비활성 | 접속 코드 gate 필수 |

## 질문이 필요한 상황

- 확정 문서와 코드 구현이 충돌할 때
- 외부 prototype 원본이나 Slate 밖 문서를 참조해야 할 때
- 외부 API 비용/쿼터 때문에 필수 기능 동작 방식이 달라질 때
- 배포 provider, 도메인, HTTPS, CORS를 실제로 선택해야 할 때
- 파일 저장소를 로컬 외 저장소로 바꿔야 할 때
- 운영 seed와 테스트 seed를 분리해야 할 때
- 공개 회사 서류 업로드의 token/rate limit 정책을 결정해야 할 때

## 기본 검증 명령

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm install
npm run build
```

## 참조 경로

- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/03_mvp_scope/mvp_scope.md`
- `docu/13_work_status/current_and_completed_work.md`
- `docu/08_environment/env_variables.md`
- `docu/09_deployment/deployment_plan.md`
- `docu/prompt/README.md`
