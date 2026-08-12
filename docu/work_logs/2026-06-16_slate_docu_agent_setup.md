# 2026-06-16 Slate 내부 문서와 Agent 설정 작업 로그

## 작업 범위

`Slate` 내부에서 바로 사용할 문서 구조와 `Agent.md`를 정리했다. 루트 준비 문서의 내용을 `Slate/docu`로 복사한 뒤 현재 구현 기준에 맞게 수정했고, 역할별 프롬프트 저장소를 `docu/prompt`로 구성했다.

## 수행 내용

| 작업 | 결과 |
|---|---|
| 문서 기준 경로 정리 | 현재 기준을 `backend`, `frontend`, `sql`, `assets`, `docu`로 고정 |
| 참조 정책 보강 | `../prototype*`와 `../docu`는 기본 참조 금지 또는 읽기 전용 비교로 제한 |
| 역할별 prompt 폴더 | `docu/prompt`와 README/역할별 프롬프트 작성 |
| 기준 문서 수정 | MVP 범위, 아키텍처, backend/frontend/DB/env/deploy 문서를 현재 `Slate` 기준으로 수정 |
| Agent 작성 | `Agent.md`를 `Slate` 루트에 작성 |
| handoff 수정 | 다음 작업자 읽기 순서와 남은 검증을 현재 기준으로 갱신 |

## 남은 이슈

| 항목 | 상태 |
|---|---|
| 실제 MySQL 적용 | 미수행 |
| 실제 외부 API smoke | 미수행 |
| 브라우저/모바일 smoke | 미수행 |
| 공개 회사 서류 업로드 보강 정책 | 사용자 결정 필요 |
| 배포 provider/HTTPS/CORS/log rotation | 배포 단계에서 결정 |

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/prompt`
- `docu/handoff/mvp_documentation_handoff.md`
