# 참조 정책

## 기준 루트

| 항목 | 경로 |
|---|---|
| 현재 작업 루트 | `<SLATE_ROOT>` |
| Backend | `backend` |
| Frontend | `frontend` |
| SQL | `sql` |
| Assets | `assets` |
| 문서 | `docu` |
| 역할별 프롬프트 | `docu/prompt` |
| Agent 지침 | `Agent.md` |

`<SLATE_ROOT>`는 각 작업자의 로컬 `Project_Slate/Slate` 폴더를 뜻한다.

## 참조 우선순위

| 우선순위 | 참조 대상 | 기준 |
|---:|---|---|
| 1 | 현재 `Slate` 코드와 SQL | `backend`, `frontend`, `sql`, `assets` |
| 2 | 현재 `Slate` 문서 | `docu`, `Agent.md` |
| 3 | 이식 전 분석 기록 | `docu/11_reviews`, `docu/work_logs` |
| 4 | 과거 prototype 비교 문서 | `docu/02_prototype_comparison`, `docu/00_inventory` |
| 5 | `../prototype*` 원본 | 사용자 요청 또는 비교 검증이 필요할 때만 |

## 금지 또는 제한

| 대상 | 처리 |
|---|---|
| 실제 `.env`, `.env.local`, `application-local.yml` | 실제값 기록/커밋 금지 |
| `node_modules`, `dist`, `target`, `uploads` | 생성물. 참조/커밋 금지 |
| `../docu` | 이전 준비 문서 원본. 현재 작업 기본 참조 금지 |
| `../prototype_3/images_page_ai` | 화면 참조 전용. `Slate` 저장소 보관 금지 |
| `../prototype`, `../prototype_2`, `../prototype_3` 구현 코드 | 원본 수정 금지. 비교가 필요할 때 읽기만 허용 |
| Slate 루트 밖 문서 | 사용자 확인 전 직접 참조 금지 |

## 프롬프트 사용 규칙

- 새 대화창용 프롬프트는 `docu/prompt` 아래에 둔다.
- 프롬프트는 현재 `Slate` 경로를 기준으로 작성한다.
- 프롬프트가 외부 prototype 원본을 읽어야 한다면 “읽기 전용”과 이유를 명시한다.

## 참조 경로

- `docu/README.md`
- `docu/00_common/document_structure.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/03_mvp_scope/mvp_decisions.md`
