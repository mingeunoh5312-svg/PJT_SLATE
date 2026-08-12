# API 차이

## 요약

| 단계 | API 구조 | 엔드포인트 스캔 수 | 특징 |
|---|---|---:|---|
| `prototype` | `/api/prototype` 단일 컨트롤러 | 32 | `X-Prototype-User-Id` 헤더, 개발용 seed/reset 포함 |
| `prototype_2` | 기능별 `/api/*`, `/api/admin/*` | 144 | JWT, 회사 승인, 팀/매칭/게시판/공모전/알림/관리자 API |
| `prototype_3` | 기능별 `/api/*`, `/api/admin/*` | 168 | YouTube, AI 추천, 관리자 CRUD, KOBIS movie search 추가 |

스캔 수는 Java controller의 `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` 라인 수 기준이다. 실제 route 수 산정은 중첩 mapping과 class-level mapping을 함께 확인해야 한다.

## 주요 API 변화

| 영역 | `prototype` | `prototype_2` | `prototype_3` |
|---|---|---|---|
| 인증 | 없음 | `/api/auth/register`, `/api/auth/login`, `/api/auth/me` | 유지 |
| 기준 데이터 | `/api/prototype/codes`, `/regions`, `/roles`, `/genres` | `/api/references/*` | 유지 |
| 프로필 | `/api/prototype/profiles/*` | `/api/profiles/*`, `/public-data/search` | `/public-data/kobis/movies`, 검증 저장/조회 확장 |
| 팀 | `/api/prototype/teams/*` | `/api/teams/*` | 유지, route 개편은 프론트 중심 |
| 매칭 | `/api/prototype/matching/*` | `/api/matching/*`, `/api/admin/matching/policies/*` | `/api/matching/ai/recommendations` 추가 |
| 게시판 | `/api/prototype/boards/*` | `/api/boards/*`, `/api/admin/work-files`, 신고 API | `/api/boards/youtube/preview`, YouTube metadata 검색 추가 |
| 공모전 | 없음 | `/api/contests/*`, `/api/admin/contests/*` | 유지 |
| 알림/로그 | 없음 | `/api/notifications/*`, `/api/admin/logs/*` | 유지 |
| 관리자 | 없음 | 회사/권한/신고/파일/공모전/알림 | 게시글/회원/팀 CRUD 상세 보강 |

## 운영 전환 주의

| 항목 | 처리 |
|---|---|
| `X-Prototype-User-Id` | `prototype_3` 기준에서는 사용하지 않음 |
| 개발용 seed/reset API | 운영/MVP 복사 기준에서 제외 |
| 프런트 API base | `VITE_API_BASE_URL` 또는 Vite proxy 정책을 환경 문서에서 결정 |
| 외부 API key | 프론트에 노출 금지. 서버 환경변수만 사용 |
| API DTO 안정화 | 일부 매칭 응답이 `Map<String, Object>` 기반이라는 이전 이슈가 있어 코드 리뷰 필요 |

## 참조 경로

- `prototype/docs/api.md`
- `prototype/backend/src/main/java/com/slate/prototype/controller/PrototypeController.java`
- `prototype_2/docs/api.md`
- `prototype_2/backend/src/main/java`
- `prototype_3/backend/src/main/java`
- `prototype_3/frontend/src/services/api.js`
- `prototype_3/docu/02_workflows/creator_work_summary.md`
- `prototype_3/docu/02_workflows/fixer_work_summary.md`
