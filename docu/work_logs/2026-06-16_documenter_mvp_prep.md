# 2026-06-16 문서 담당자 작업 로그

## 작업 범위

Slate MVP 제작 전 문서 구조를 루트 `docu` 아래에 정리했다. 원본 구현 코드는 수정하지 않았다.

## 수행 내용

| 작업 | 결과 |
|---|---|
| 기존 `docu` 구조 확인 | `plan`, `plan/features`, `plan/prototype`, `plan/technical`, `plan/old` 확인 |
| prototype 문서 수집 | `prototype`, `prototype_2`, `prototype_3` 문서와 코드/SQL/에셋 구조 확인 |
| 문서 구조 생성 | `docu/00_inventory`부터 `docu/12_agent`까지 생성 |
| 비교 문서 작성 | 기능/API/DB/화면/누락 항목 비교 작성 |
| MVP 범위 문서 작성 | 포함/제외/복사 계획/질문 목록 작성 |
| 환경/배포 문서 작성 | 환경변수, `.env.example` 정책, 로컬/배포 계획 작성 |
| 다음 프롬프트/Agent 초안 작성 | 코드 리뷰 프롬프트, 리뷰 계획, `Agent.md` 초안 작성 |
| 사용자 답변 반영 | `mvp_decisions.md` 작성, 질문 목록/범위/환경/배포/Agent/리뷰 문서 갱신 |

## 확인한 주요 사실

| 항목 | 결과 |
|---|---|
| `prototype` DB | 21개 테이블 |
| `prototype_2` DB | 49개 테이블 |
| `prototype_3` DB | 50개 테이블, `portfolio_verification` 추가 |
| API mapping 스캔 | `prototype` 32, `prototype_2` 144, `prototype_3` 168 |
| `Slate/` 폴더 | 현재 빈 `docu`만 존재 |
| 최종 앱 루트 | 사용자 답변으로 `<SLATE_ROOT>` 하위 확정 |
| 외부 API | KOBIS, YouTube Data API, OpenAI AI 매칭 필수 기능 확정 |
| 배포 우선순위 | 로컬 시연과 프론트/백엔드 분리 배포 필수, EC2 단일 서버 최후순위 |
| 최종 제외 자산 | `prototype_3/images_page_ai`는 참조 전용, 최종 저장소 미보관 |
| Git 상태 | `prototype_3` 일부 문서/설정과 `prototype_2/backend/bin` 등 기존 변경/미추적 항목 존재 |

## 미수행

| 항목 | 이유 |
|---|---|
| 테스트 실행 | 이번 작업은 문서 정리 범위 |
| 코드 수정 | 사용자 요청상 원본 구현 수정 금지 |
| 실제 복사 | 사용자 승인 전 계획만 작성 |
| 외부 API 검증 | API key와 실행 환경 필요 |

## 참조 경로

- `docu/00_inventory/document_inventory.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `prototype_3/docu/README.md`
- `prototype_3/docu/setup.md`
- `prototype_3/docu/02_workflows/creator_work_summary.md`
- `prototype_3/docu/02_workflows/fixer_work_summary.md`
- `prototype_3/backend`
- `prototype_3/frontend`
- `prototype_3/sql`
