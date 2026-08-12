# 문서 인벤토리

## 목적

Slate MVP 제작 전에 참조할 문서를 `docu` 하위 기준으로 재분류한다. 이 문서는 이식 전 문서와 prototype 원본의 인벤토리를 보존하며, 현재 작업 기준은 `Slate` 내부 `backend`, `frontend`, `sql`, `assets`, `docu`다.

## 수집 기준

| 구분 | 수집 결과 | 판정 |
|---|---:|---|
| 현재 `Slate/docu` 문서 | 114개 Markdown | 현재 작업 문서. 완료/진행/잔여 상태는 `docu/13_work_status/current_and_completed_work.md` 우선 |
| `docu` 기존 문서 | 30개 Markdown | 초기 기획, 기능 정책, 첫 프로토타입 범위 판단 근거 |
| `prototype` 문서 | 9개 Markdown | 구조 검증용 1차 프로토타입 기록 |
| `prototype_2` 문서 | 38개 Markdown | 기능 구현 확대, 환경/검증/known issues 근거 |
| `prototype_3` 문서 | 202개 Markdown | 최종 MVP 제작 기준. 다수의 prompt, handoff, work log 포함 |

생성물과 의존성 폴더인 `node_modules`, `target`, `bin`, `dist`, `uploads`는 문서 인벤토리에서 제외한다.

이 문서의 과거 원본 수치는 이식 전 출처 분류를 보존하기 위한 값이다. 현재 작업 상태와 최신 검증 수치는 `docu/13_work_status/current_and_completed_work.md`, `docu/work_logs`, 현재 `Slate` 코드/SQL을 우선한다.

## 최신성 기준

| 우선순위 | 기준 | 이유 |
|---:|---|---|
| 1 | 현재 `Slate` 내부 코드, SQL, `docu` | 실제 작업 기준 |
| 2 | `../prototype_3` 실제 코드, SQL, 문서 | 이식 출처와 차이 검증용 |
| 3 | `../prototype_2` 문서/코드 | 기능 구현 확장과 known issues 기준 |
| 4 | `../prototype` 문서/코드 | 최초 구조 검증 기준 |
| 5 | `../docu/plan` | 초기 구상, 정책, 1차 구현 범위 근거 |

## 루트 `docu` 문서

| 경로 | 목적 | 참조 가치 | 중복/주의 | 최신성 | MVP 반영 |
|---|---|---|---|---|---|
| `docu/plan/02_project_spec.md` | 상위 명세 | 초기 서비스 목표 확인 | 구현 여부와 혼동 금지 | 초기 | 반영 |
| `docu/plan/03_feasibility_summary.md` | 실현 가능성 요약 | 배포/기술 리스크 확인 | 최신 구현과 차이 가능 | 초기 | 부분 반영 |
| `docu/plan/04_contest_file_review.md` | 공모전 자료 검토 | 공모전 기능 배경 | 구현 기준은 `prototype_3` 우선 | 초기 | 참고 |
| `docu/plan/05_mobile_ux_policy.md` | 모바일 UX 정책 | 반응형 기준 | `prototype_3` 화면 검증과 대조 필요 | 초기 | 반영 |
| `docu/plan/06_architecture_pattern_review.md` | MVC/MVP 검토 | 아키텍처 선택 배경 | 현재 코드는 Spring MVC/MyBatis | 초기 | 참고 |
| `docu/plan/09_next_steps_and_intermediate_outputs.md` | 다음 단계 계획 | 작업 흐름 배경 | 현재 구조와 다름 | 과거 | 참고 |
| `docu/plan/10_active_reference_status.md` | 참조 상태 점검 | 당시 active reference 확인 | 최신 기준은 이 인벤토리 | 과거 | 대체 |
| `docu/plan/features/*.md` | 기능별 상세 정책 | 기능 범위 질문 도출 | 구현/미구현 구분 필요 | 초기 | 반영 |
| `docu/plan/prototype/*.md` | 최초 프로토타입 범위/검토 | prototype 결과와 1차 구현 범위 판단 | 최종 기준 아님 | 과거 | 비교 반영 |
| `docu/plan/technical/db_draft.md` | DB 초안 | 초기 테이블 설계 배경 | 실제 테이블은 `prototype_3/sql` 우선 | 초기 | 참고 |
| `docu/plan/old/*.md` | 이전 논의 기록 | 의사결정 흔적 | 충돌 시 최신 기준 아님 | 과거 | 보관 |

## `prototype` 문서

| 경로 | 목적 | 참조 가치 | 중복/주의 | 최신성 | MVP 반영 |
|---|---|---|---|---|---|
| `prototype/README.md` | 최초 프로토타입 개요 | 최소 구조 파악 | 현재 구현 범위보다 작음 | 과거 | 비교 |
| `prototype/docs/setup.md` | 최초 실행 방법 | 초기 환경 흐름 | DB/포트/인증 방식이 과거 기준 | 과거 | 참고 |
| `prototype/docs/database.md` | 21개 테이블 기준 | DB 확장 전 기준 | `slate_proto`, `slate_user` 기준 | 과거 | 비교 |
| `prototype/docs/api.md` | `/api/prototype` API | 단일 컨트롤러 구조 확인 | `X-Prototype-User-Id`는 운영 제외 | 과거 | 비교 |
| `prototype/docs/screens.md` | P00-P11 화면 | 초기 화면 흐름 | `prototype_3` route와 다름 | 과거 | 비교 |
| `prototype/docs/verification.md` | 최초 검증 결과 | 빌드/API 검증 흔적 | JDK 25 등 과거 환경 포함 | 과거 | 참고 |
| `prototype/docs/known_issues.md` | 제외/이슈 | 운영 전환 금지 항목 | 이후 prototype에서 일부 해결 | 과거 | 비교 |
| `prototype/prompts/prototype_work_prompts.md` | 작업 프롬프트 | 작업 방식 참고 | 최종 프롬프트 기준 아님 | 과거 | 보관 |
| `prototype/sql/README.md` | SQL 실행 안내 | 최초 DB 실행 순서 | 현재 DB명과 다름 | 과거 | 참고 |

## `prototype_2` 문서

| 경로 | 목적 | 참조 가치 | 중복/주의 | 최신성 | MVP 반영 |
|---|---|---|---|---|---|
| `prototype_2/context/*.md` | 프로젝트 개요, 구현 범위, 기술 구조, 협업 규칙 | 기능 확장 기준 | 일부 파일 제목이 `prototype_2`로 유지됨 | 이전 | 반영 |
| `prototype_2/docs/README.md` | 문서 진입점 | 전체 문서 지도 | 최종 진입점은 `prototype_3/docu/README.md` | 이전 | 참고 |
| `prototype_2/docs/api.md` | 기능별 API 문서 | API 범위 비교 | `prototype_3` 추가 API 누락 가능 | 이전 | 비교 |
| `prototype_2/docs/database.md` | 49개 테이블 DB 문서 | DB 확장 기준 | `portfolio_verification` 없음 | 이전 | 비교 |
| `prototype_2/docs/setup.md` | 로컬 실행 방법 | JDK/Node/MySQL 기준 | 로컬 경로가 다른 PC 기준 포함 | 이전 | 환경 문서로 흡수 |
| `prototype_2/docs/verification.md` | SQL/백엔드/프론트/브라우저 검증 | 검증 기준과 통과 항목 | 최종 화면 검증은 `prototype_3` 필요 | 이전 | 반영 |
| `prototype_2/docs/known_issues.md` | 미구현/축소 구현/환경 이슈 | MVP 전 질문 도출 | 일부 이슈는 `prototype_3`에서 해결 | 이전 | 반영 |
| `prototype_2/docs/feature_status.md` | 영역별 구현 상태 | 기능 구현 여부 분리 | `prototype_3` 추가 기능 반영 필요 | 이전 | 비교 |
| `prototype_2/docs/environment_check.md` | 환경 점검 기록 | 개인별 환경값 분리 근거 | 비밀번호 예시/로컬 경로 분리 필요 | 이전 | 환경 문서로 흡수 |
| `prototype_2/docs/screens.md` | 화면 문서 | 이전 화면 흐름 | `prototype_3` 이미지 기준으로 대체 | 이전 | 비교 |
| `prototype_2/handoff/*.md` | 역할 간 인수인계 | 구현 맥락 | 최신 handoff는 `prototype_3/docu/handoff` 우선 | 이전 | 참고 |
| `prototype_2/work_logs/*.md` | 작업 로그 | 검증 흔적 | 세부 추적용 | 이전 | 필요 시 참고 |
| `prototype_2/prompts/*.md` | 역할별 프롬프트 | 협업 방식 참고 | 최신 프롬프트는 `prototype_3/docu/07_Prompt` | 이전 | 보관 |

## `prototype_3` 문서

| 경로 | 목적 | 참조 가치 | 중복/주의 | 최신성 | MVP 반영 |
|---|---|---|---|---|---|
| `prototype_3/docu/README.md` | 최종 문서 진입점 | 읽기 순서, 현재 진행 요약 | MVP용으로 루트 `docu`에 재정리 필요 | 최신 | 반영 |
| `prototype_3/docu/setup.md` | 로컬 실행 설정 | 환경변수/포트/검증 순서 | 개인 경로 값은 환경 문서로 분리 | 최신 | 반영 |
| `prototype_3/docu/00_common/*.md` | 작업/참조/디자인 정책 | 협업 규칙 기준 | MVP 루트 기준으로 재작성 필요 | 최신 | 반영 |
| `prototype_3/docu/01_context/*.md` | prototype_2 상태와 front handoff 요약 | 이전 구현 이해 | `prototype_2` 이름 잔존 | 최신 요약 | 반영 |
| `prototype_3/docu/02_workflows/*.md` | 작업 큐, 요약, 참조 지도 | 최신 상태 파악의 핵심 | 원본 로그보다 우선 읽기 | 최신 | 반영 |
| `prototype_3/docu/03_roles/*.md` | 역할별 시작 문서 | 두 작업자 협업 기준 | MVP 역할에 맞게 축약 필요 | 최신 | 반영 |
| `prototype_3/docu/04_pages/*.md` | 페이지별 기준 | 화면/route/검증 기준 | 대표 이미지와 실제 구현을 함께 봐야 함 | 최신 | 반영 |
| `prototype_3/docu/05_assets/image_inventory.md` | 이미지 인벤토리 | 복사 후보와 에셋 상태 | 실제 서비스용 여부 미확정 | 최신 | 반영 |
| `prototype_3/docu/06_templates/*.md` | 로그/handoff 템플릿 | 문서 작성 규칙 | MVP용 템플릿은 선택 반영 | 최신 | 참고 |
| `prototype_3/docu/07_Prompt/**/*.md` | 구현/검증 프롬프트 | 다음 코드 분석과 재구현 프롬프트 근거 | 전부 읽기보다 관련 기능만 선택 | 최신 | 참고 |
| `prototype_3/docu/handoff/*.md` | 상세 인수인계 | 결함/구현 세부 추적 | `02_workflows/handoff_next_summary.md` 우선 | 최신 | 필요 시 |
| `prototype_3/docu/work_logs/*.md` | 실제 작업 로그 | 검증 실패/남은 작업 근거 | 원본 로그는 세부 추적용 | 최신 | 필요 시 |

## 코드와 SQL 참조 대상

| 경로 | 목적 | MVP 제작 반영 |
|---|---|---|
| `backend` | Spring Boot 4, Security JWT, MyBatis 백엔드 | 현재 작업 기준 |
| `frontend` | Vue 3 + Vite 프론트 | 현재 작업 기준 |
| `sql` | MySQL 8 schema/seed/reset/추가 SQL | 현재 작업 기준 |
| `assets` | 원천 CSV/SHP 자산 | 현재 작업 기준 |
| `../prototype_3/backend`, `../prototype_3/frontend`, `../prototype_3/sql`, `../prototype_3/assets` | 이식 출처 | 차이 검증 시 읽기 전용 |
| `../prototype_3/images_page_ai` | 화면 대표 이미지 | 참조 전용. 최종 저장소 복사 제외 |
| `uploads`, `frontend/node_modules`, `frontend/dist`, `backend/target` | 로컬 생성물 | 복사/커밋 제외 |

## 참조 경로

- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_inventory/source_reference_map.md`
- `backend`
- `frontend`
- `sql`
- `assets`
- `../prototype_3/docu`
- `../prototype_3/backend`
- `../prototype_3/frontend`
- `../prototype_3/sql`
- `../prototype_3/assets`
- `../prototype_3/images_page_ai`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/13_work_status/current_and_completed_work.md`
