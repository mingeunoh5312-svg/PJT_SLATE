# 2026-06-27 Lovable 디자인 이식 작업 로그

## 문서 작성 기준

- `Agent.md`의 작업 로그 규칙에 따라 `docu/work_logs/YYYY-MM-DD_{role}_{summary}.md` 형식으로 작성한다.
- 현재 작업 상태와 남은 작업은 `docu/13_work_status/lovable_design_porting_status.md`를 우선 기준으로 추적한다.
- 실제 `.env`, DB 비밀번호, API key, JWT secret은 참조하거나 기록하지 않는다.

## 작업 범위

- `design/design_dark_light_theme` Lovable 결과물을 현재 Vue 3 프론트엔드에 단계적으로 이식한다.
- 기능 구현보다 화면 시안 확인 가능성을 우선한다.
- React/TanStack/Tailwind 원본을 Vue SFC, Vue Router, CSS 토큰으로 변환한다.
- 상위 route는 Lovable 시안으로 전환하되, 기존 상세/관리/팀별 기능 route는 필요한 경우 레거시 화면으로 유지한다.

## 지금까지 완료한 작업

| 영역 | 결과 |
|---|---|
| 디자인 검토 | Lovable 결과물은 현재 Vue 앱에 그대로 복사할 수 없고 구조와 토큰을 변환해야 함을 확인 |
| 서버 | 프론트 dev server `5174` 실행, 백엔드는 `.env` 참조 없이 `--spring.config.import=` 방식으로 `8080` 실행 |
| 테마 | `frontend/src/styles/theme.css`에 검정 기본 테마와 `html.light` 라이트 테마 토큰 추가 |
| 공통 레이아웃 | `frontend/src/layouts/AppLayout.vue`를 Lovable 상단 header/footer 중심 구조로 변경 |
| 홈 | `frontend/src/views/HomeView.vue`를 Lovable `index.tsx` 기반 정적 시안으로 교체 |
| 탐색 | `frontend/src/views/DiscoverView.vue` 추가, `/discover` route 연결 |
| 작업물 | `frontend/src/views/WorksView.vue` 추가, `/works` route 연결, `/boards`는 nav 숨김 |
| 공모전 | `frontend/src/views/ContestsView.vue` 추가, `/contests` 상위 route 연결, 하위 공모전 기능 route 유지 |
| AI 로케이션 | `frontend/src/views/AiLocationView.vue` 추가, `/locations` 상위 route 연결, `/teams/:teamId/locations`는 기존 기능 화면 유지 |
| 팀 | `frontend/src/views/TeamShowcaseView.vue` 추가, `/teams`와 `/teams/:teamId`를 Lovable 팀 상세 시안으로 연결, 팀 생성/수정/모집/멤버/일정/팀별 로케이션 기능 route 유지 |
| 공개 프로필 | `frontend/src/views/PublicProfileShowcaseView.vue` 추가, `/profiles/:profileId`를 Lovable 공개 프로필 시안으로 연결, 포트폴리오 상세 기능 route 유지 |
| 작업공간 | `frontend/src/views/WorkspaceShowcaseView.vue` 추가, 신규 `/workspace` route와 상단 `작업공간` 메뉴 연결 |
| 관리자 | `frontend/src/views/AdminShowcaseView.vue` 추가, `/admin` 상위 route를 Lovable 운영 콘솔 시안으로 연결, 기존 `/admin/...` 하위 route와 관리자 권한 meta 유지 |
| 로그인/회원가입 | Lovable 결과물에 별도 auth route가 없음을 확인하고 기존 인증 기능 화면을 유지, `theme.css` auth override로 검정 기본/흰색 선택 테마 정합성 적용, 로그인 모바일 하단 레거시 탭 제거 |
| 문서 | `docu/13_work_status/lovable_design_porting_status.md` 추가, 중앙 상태 문서에 추적 항목 추가 |

## 실행한 명령과 결과

| 명령 | 결과 |
|---|---|
| `npm.cmd run build` | 통과. Vite chunk size 경고는 남음 |
| `Invoke-WebRequest http://localhost:5174/` | 200 |
| `Invoke-WebRequest http://localhost:5174/discover` | 200 |
| `Invoke-WebRequest http://localhost:5174/works` | 200 |
| `Invoke-WebRequest http://localhost:5174/contests` | 200 |
| `Invoke-WebRequest http://localhost:5174/locations` | 200 |
| `Invoke-WebRequest http://localhost:5174/teams/1/locations` | 200. SPA 응답 확인, route는 기존 `LocationExploreView` 유지 |
| `Invoke-WebRequest http://localhost:5174/teams` | 200 |
| `Invoke-WebRequest http://localhost:5174/teams/t-blueroom` | 200 |
| `Invoke-WebRequest http://localhost:5174/profiles/p-yoon` | 200 |
| `Invoke-WebRequest http://localhost:5174/profiles/1` | 200 |
| `Invoke-WebRequest http://localhost:5174/profiles/1/portfolio/1` | 200. SPA 응답 확인, route는 기존 `PublicProfileView` 유지 |
| `Invoke-WebRequest http://localhost:5174/workspace` | 200 |
| `Invoke-WebRequest http://localhost:5174/admin` | 200. SPA 응답 확인, route는 `AdminShowcaseView` 연결 |
| `Invoke-WebRequest http://localhost:5174/admin/users` | 200. SPA 응답 확인, route는 기존 `AdminView` 유지 |
| `Invoke-WebRequest http://localhost:5174/login` | 200 |
| `Invoke-WebRequest http://localhost:5174/register` | 200 |
| `Invoke-WebRequest http://localhost:5174/register/user` | 200 |
| `Invoke-WebRequest http://localhost:5174/register/company` | 200 |
| `Invoke-WebRequest http://localhost:5174/register/complete` | 200 |
| `Invoke-WebRequest http://localhost:5174/register/company/pending` | 200 |
| `Invoke-WebRequest http://localhost:5174/demo-access` | 200 |
| `Invoke-WebRequest http://localhost:5174/profile` | 200. SPA 응답 확인, 기존 내 프로필 route 영향 확인 |
| `Invoke-WebRequest http://localhost:8080/ -SkipHttpErrorCheck` | 403. Spring Security 차단 응답이므로 서버 reachable 확인 |

## 남은 작업

| 우선순위 | 작업 | 비고 |
|---:|---|---|
| 1 | 레거시 화면 정리 | `/matching`, `/boards` 등 nav 숨김 route와 신규 route 역할 정리 |
| 2 | 반응형·시각 QA | desktop/mobile overflow, header/footer, 텍스트 충돌 확인 필요 |

## 이번 작업에서 의도적으로 변경하지 않은 내용

- 백엔드 API 계약, DB, SQL은 변경하지 않았다.
- Lovable 결과물 외 별도 참조 파일은 업로드하거나 복사하지 않았다.
- `.env` 실제 값은 읽거나 문서화하지 않았다.
- 기존 팀별 AI 로케이션 기능 route, 공모전 하위 기능 route, 공개 포트폴리오 상세 기능 route는 삭제하지 않았다.

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_common/document_structure.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/13_work_status/current_and_completed_work.md`
- `docu/13_work_status/lovable_design_porting_status.md`
- `design/design_dark_light_theme/src/routes/index.tsx`
- `design/design_dark_light_theme/src/routes/discover.tsx`
- `design/design_dark_light_theme/src/routes/works.tsx`
- `design/design_dark_light_theme/src/routes/contests.tsx`
- `design/design_dark_light_theme/src/routes/ai.location.tsx`
- `design/design_dark_light_theme/src/routes/admin.tsx`
- `design/design_dark_light_theme/src/lib/mock.ts`
- `frontend/src/router/index.js`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/styles/theme.css`
