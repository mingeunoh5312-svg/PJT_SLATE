# 문서 구조와 작성 규칙

## 목적

`Slate` 내부 작업자가 같은 참조 구조를 사용하도록 `docu` 폴더 구조, 읽기 순서, 작성 규칙, 환경값 분리 원칙을 고정한다.

## 폴더 구조

| 폴더 | 목적 |
|---|---|
| `docu/00_common` | 공통 읽기 순서, 참조 정책, 작성 규칙 |
| `docu/00_inventory` | 기존 문서 인벤토리와 참조 지도 |
| `docu/01_project_definition` | 프로젝트 정의와 초기 구상 요약 |
| `docu/02_prototype_comparison` | prototype별 차이와 누락/삭제 항목 |
| `docu/03_mvp_scope` | MVP 포함/제외 범위, 사용자 답변 기반 결정, 이식 계획 |
| `docu/04_architecture` | 현재 `Slate` 아키텍처 기준 |
| `docu/05_backend` | 백엔드 구현 기준 |
| `docu/06_frontend` | 프론트 구현 기준 |
| `docu/07_database` | DB/SQL 기준 |
| `docu/08_environment` | 로컬/배포 환경변수와 `.env` 정책 |
| `docu/09_deployment` | 배포 계획과 체크리스트 |
| `docu/11_reviews` | 코드 리뷰/검증 결과와 계획 |
| `docu/prompt` | 역할별 새 대화창 프롬프트 |
| `docu/12_agent` | `Agent.md` 초안과 이전 기준 보관 |
| `docu/13_work_status` | 현재 작업 목록, 완료 목록, 문서 충돌 점검 |
| `docu/work_logs` | 작업 로그 |
| `docu/handoff` | 다음 작업자 인수인계 |

## 두 작업자 읽기 순서

| 작업자 | 1차 읽기 | 2차 읽기 | 작성/수정 담당 |
|---|---|---|---|
| 작업자 A: 범위/문서 담당 | `00_common`, `03_mvp_scope`, `11_reviews/code_review_result.md`, `13_work_status/current_and_completed_work.md` | `08_environment`, `09_deployment`, `handoff` | 범위, 질문, Agent, handoff, 현재 작업 추적 |
| 작업자 B: 코드/검증 담당 | `00_common/reference_policy.md`, `04_architecture`, `05_backend`, `06_frontend`, `07_database`, `13_work_status/current_and_completed_work.md` | `11_reviews`, `prompt`, `work_logs` | 코드 분석, 테스트, 검증 |

## 작성 규칙

- 모든 새 문서는 `docu` 하위에 작성한다.
- 현재 구현 코드는 `backend`, `frontend`, `sql`, `assets`를 기준으로 본다.
- `../prototype`, `../prototype_2`, `../prototype_3`, `../docu`는 역사 비교용이다. 새 작업의 기본 참조로 삼지 않는다.
- 어떤 문서를 참조하면 문서 끝에 `참조 경로` 섹션을 남긴다.
- 구현된 것, 미구현인 것, 문서에만 있는 것을 분리한다.
- 현재 작업 목록과 완료 목록은 `docu/13_work_status/current_and_completed_work.md`에 모으고, 원본 TODO와 work log는 증거 자료로 보존한다.
- 실패한 검증, 미수행 검증, 누락된 자료를 숨기지 않는다.
- 로컬 경로, API key, DB 비밀번호, `.env`, 업로드 경로, 포트, 외부 API 설정은 `docu/08_environment`로 분리한다.
- 실제 비밀값은 문서에 쓰지 않는다. 예시는 `CHANGE_ME`, `${ENV_NAME}`, `<local-only>` 형식으로만 쓴다.

## 상태 표기

| 상태 | 의미 |
|---|---|
| 구현됨 | 현재 `Slate` 코드/SQL/화면 또는 API에 실제 구현이 있음 |
| 부분 구현 | 일부 흐름만 구현됐거나 외부 API/브라우저 검증이 남음 |
| 문서 기준 | 문서에는 있으나 현재 코드 구현 확인이 안 됨 |
| 미구현 | 코드와 화면에서 확인되지 않음 |
| 질문 필요 | 사용자 판단 없이는 범위를 정할 수 없음 |

## 참조 경로

- `docu/00_common/reference_policy.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/11_reviews/code_review_result.md`
- `docu/13_work_status/current_and_completed_work.md`
