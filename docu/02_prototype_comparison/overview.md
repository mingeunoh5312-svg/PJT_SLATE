# 프로토타입 비교 개요

## 결론

MVP 제작 기준은 `prototype_3`다. `prototype`은 구조 검증용, `prototype_2`는 기능 확장 구현 기준, `prototype_3`는 최종 화면/라우팅/추가 기능과 최신 검증 로그 기준으로 사용한다.

## 단계별 요약

| 단계 | 핵심 목적 | 구현 범위 | 검증/문서 상태 | MVP 반영 |
|---|---|---|---|---|
| 초기 구상 | 서비스 범위와 정책 정의 | 문서 중심 | 기능 정책, DB 초안, 프로토타입 범위 문서 | 질문과 범위 판단에 반영 |
| `prototype` | 최소 구조 검증 | 21개 테이블, `/api/prototype`, Vue P00-P11 | Maven/Vite/API 검증 기록 | 구조 비교용 |
| `prototype_2` | 실제 1차 기능 확장 | 49개 테이블, JWT, 회사/관리자, 팀/매칭/게시판/공모전/알림/로그 | SQL, 백엔드, 프론트, 주요 API, 화면 안정화 검증 | 기능 기준의 이전 버전 |
| `prototype_3` | 최종 화면과 추가 기능 기준 | 50개 테이블, Verified/KOBIS, YouTube, OpenAI AI 매칭, 관리자 CRUD, 라우트 개편 | 생성자/수정자 로그, 일부 브라우저/외부 API 검증 미완 | 최종 기준 |

## 주요 변화

| 변화 | 설명 |
|---|---|
| 인증 | `prototype`의 `X-Prototype-User-Id`에서 `prototype_2/3`의 JWT 인증으로 변경 |
| DB | 21개 테이블에서 49개, `prototype_3`에서 `portfolio_verification` 포함 50개로 확장 |
| API 구조 | 단일 `/api/prototype` 컨트롤러에서 기능별 `/api/*`, `/api/admin/*`로 분리 |
| 화면 | P00-P11 프로토타입 route에서 실제 서비스 route와 App/Auth/Admin layout으로 변경 |
| 외부 API | KOBIS, YouTube, OpenAI 설정이 `prototype_3`에서 서버 환경변수 기반으로 추가 |
| 문서 구조 | `prototype_3/docu`가 최종 작업 로그와 handoff를 가장 많이 보유 |

## 남은 큰 리스크

| 리스크 | 상태 |
|---|---|
| 실제 OpenAI API Key 기반 AI 추천 | 미수행 |
| 실제 YouTube API Key 기반 브라우저 미리보기/등록 검증 | 부분 미수행 |
| KOBIS 실제 API Key 기반 배지 레이아웃 브라우저 확인 | 부분 미수행 |
| 프론트 전면 개편 이후 전체 route mutation smoke | 남음 |
| 배포 환경에서 파일 저장/물리 삭제/ffprobe | 설계 필요 |
| 루트 앱 승격 위치 | 사용자 확인 필요 |

## 참조 경로

- `docu/plan/prototype/06_prototype_result_review_and_first_scope.md`
- `prototype/docs/api.md`
- `prototype/docs/database.md`
- `prototype/docs/verification.md`
- `prototype_2/docs/feature_status.md`
- `prototype_2/docs/verification.md`
- `prototype_2/docs/known_issues.md`
- `prototype_3/docu/README.md`
- `prototype_3/docu/02_workflows/work_units.md`
- `prototype_3/docu/02_workflows/handoff_next_summary.md`
- `prototype_3/docu/02_workflows/creator_work_summary.md`
- `prototype_3/docu/02_workflows/fixer_work_summary.md`
