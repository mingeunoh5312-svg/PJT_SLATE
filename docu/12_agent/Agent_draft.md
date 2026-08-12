# Agent.md 반영본 안내

## 상태

2026-06-16 기준으로 `Slate` 루트에서 사용할 실제 지침 파일은 `Agent.md`로 작성했다. 이 문서는 초안 보관과 참조 위치 안내 용도이며, 새 작업자는 `Agent.md`를 우선 읽는다.

## 핵심 기준

| 항목 | 기준 |
|---|---|
| 앱 루트 | `<SLATE_ROOT>` |
| 현재 구현 | `backend`, `frontend`, `sql`, `assets` |
| 문서 | `docu` |
| 역할별 프롬프트 | `docu/prompt` |
| 과거 prototype | 비교가 필요할 때만 읽기 전용 |
| 외부 준비 문서 | `../docu` 기본 참조 금지 |

## Agent.md에 포함된 항목

- 역할과 작업 루트
- 필수 읽기 순서
- 현재 기준 경로
- 금지 사항
- 작업 로그와 handoff 규칙
- 환경변수와 secret 정책
- 로컬/배포 차이
- 질문이 필요한 상황
- 기본 검증 명령

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/prompt/README.md`
