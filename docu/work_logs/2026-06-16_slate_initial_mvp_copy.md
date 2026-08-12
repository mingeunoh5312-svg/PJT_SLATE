# 2026-06-16 Slate 초기 MVP 이식 작업 로그

## 작업 범위

`prototype_3` 기준 구현을 `<SLATE_ROOT>` 하위로 복사하고, 코드 분석 문서의 우선 차단 항목 일부를 최종 앱 후보에 반영했다. 원본 `prototype_3` 구현 코드는 수정하지 않았다.

## 수행 내용

| 작업 | 결과 |
|---|---|
| 필수 후보 복사 | `backend`, `frontend`, `sql`, `assets`, `.gitignore`를 `Slate/` 하위로 복사 |
| 제외 대상 정리 | 복사된 `node_modules`, `dist`, `target`, `application-local.yml`, IDE 메타데이터 삭제 |
| 최종명 변경 | Java package `com.slate`, backend `slate-backend`, frontend `slate-frontend`, DB명 `slate` 적용 |
| 파일 스트리밍 권한 | 공개 게시글/작업물만 anonymous 허용, 소유자/작성자/팀 멤버/관리자는 허용 |
| 데모 접속 코드 gate | 백엔드 `X-Slate-Demo-Code` filter와 프론트 `/demo-access` route 추가 |
| OpenAI fallback | API key 누락/호출 실패 시 기존 점수 기반 추천 반환 |
| 환경 분리 | `Slate/.env.example`, `backend/.env.example`, `frontend/.env.example`, `application-prod.yml` 추가 |
| DB 동시성 방어 | pending 지원/초대 generated unique key, 슬롯 정원 조건부 update 추가 |
| IP hash 정책 | 게시글 조회 hash도 audit salt 기반 `RequestLogContext` 사용 |

## 검증

| 범위 | 명령 | 결과 |
|---|---|---|
| Backend | `mvn test` in `backend` | 통과. 39 tests, failures 0, errors 0 |
| Frontend install | `npm install` in `frontend` | 통과 |
| Frontend build | `npm run build` in `frontend` | 통과 |
| 이름 잔존 검색 | `rg prototype2/prototype_2/slate_prototype2` in `Slate` | 잔존 없음 |

## 남은 이슈

| 항목 | 상태 |
|---|---|
| 공개 회사 서류 업로드 보강 | 미반영. 1회성 token/rate limit 정책 결정 필요 |
| 실제 DB 적용 | 미수행. 로컬 MySQL 환경 필요 |
| 실제 외부 API smoke | 미수행. KOBIS/YouTube/OpenAI key 필요 |
| 브라우저/모바일 smoke | 미수행 |
| HTTPS/CORS/log rotation | 사용자 답변대로 보류 |

## 참조 경로

- `docu/11_reviews/code_review_result.md`
- `docu/11_reviews/security_environment_findings.md`
- `docu/11_reviews/test_gap_report.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/03_mvp_scope/copy_plan_from_prototype_3.md`
- `backend`
- `frontend`
- `sql`
- `assets`
