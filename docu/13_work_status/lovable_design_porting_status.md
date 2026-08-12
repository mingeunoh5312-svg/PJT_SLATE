# Lovable 디자인 이식 진행 현황

작성일: 2026-06-27

## 목적

`design/design_dark_light_theme`의 Lovable 결과물을 현재 Vue 3 프론트엔드에 이식하는 작업 상태를 추적한다. 이번 이식은 화면 시안 확인을 우선하며, 기능 구현이나 API 계약 변경을 목표로 하지 않는다.

## 작업 기준

| 항목 | 기준 |
|---|---|
| 작업 범위 | `Slate` 폴더 내부만 수정 |
| 디자인 기준 | `design/design_dark_light_theme`의 화면 구성 |
| 구현 방식 | React/TanStack/Tailwind 결과물을 Vue Router/Vue SFC/CSS로 변환 |
| 데이터 기준 | 백엔드 없이 확인 가능한 정적/mock 데이터 우선 |
| 테마 기준 | 검정 테마를 기본값, 흰 테마를 `html.light` 선택값으로 준비 |
| 기존 기능 | 필요한 하위 route와 기존 기능 화면은 제거하지 않고 숨김 또는 병행 유지 |
| 비밀값 | `.env` 실제 값, API key, DB 비밀번호, JWT secret은 참조·기록하지 않음 |

## 완료한 작업

| 상태 | 작업 | 변경/확인 |
|---|---|---|
| 구현됨/검증 필요 | Lovable 결과물 검토 | `design/design_dark_light_theme`가 React 기반이므로 현재 Vue 앱에 그대로 복사할 수 없고, 화면 구조와 토큰을 Vue/CSS로 변환해야 함을 확인 |
| 구현됨/검증 필요 | 프론트 서버 | `VITE_DEMO_ACCESS_GATE=false` 상태로 `http://localhost:5174/` dev server 실행 |
| 구현됨/검증 필요 | 백엔드 서버 | `.env`를 참조하지 않도록 `--spring.config.import=`와 `local` profile로 jar 실행, `http://localhost:8080/` 응답 403으로 서버 reachable 확인 |
| 구현됨/검증 필요 | 테마 기반 | `frontend/src/styles/theme.css` 추가, 검정 기본 테마와 `html.light` 라이트 테마 토큰 정의 |
| 구현됨/검증 필요 | 테마 초기화 | `frontend/index.html`에서 `localStorage('slate-theme')` 기준으로 `html.dark`/`html.light` 초기화 |
| 구현됨/검증 필요 | 공통 레이아웃 | `frontend/src/layouts/AppLayout.vue`를 Lovable의 상단 header/footer 중심 구조로 재구성 |
| 구현됨/검증 필요 | 홈 | `frontend/src/views/HomeView.vue`를 Lovable `index.tsx` 기반 정적 시안으로 교체 |
| 구현됨/검증 필요 | 탐색 | `frontend/src/views/DiscoverView.vue` 추가, `/discover` route 연결 |
| 구현됨/검증 필요 | 작업물 | `frontend/src/views/WorksView.vue` 추가, `/works` route 연결, 기존 `/boards`는 nav 숨김 |
| 구현됨/검증 필요 | 공모전 | `frontend/src/views/ContestsView.vue` 추가, 상위 `/contests`만 Lovable 시안으로 연결, 기존 공모전 상세/관리 route는 레거시 화면 유지 |
| 구현됨/검증 필요 | AI 로케이션 | `frontend/src/views/AiLocationView.vue` 추가, 상위 `/locations`만 Lovable 시안으로 연결, 기존 `/teams/:teamId/locations`는 레거시 기능 화면 유지 |
| 구현됨/검증 필요 | 팀 | `frontend/src/views/TeamShowcaseView.vue` 추가, `/teams`와 `/teams/:teamId`를 Lovable 팀 상세 시안으로 연결, 기존 팀 생성/수정/모집/멤버/일정/팀별 로케이션 route는 레거시 기능 화면 유지 |
| 구현됨/검증 필요 | 공개 프로필 | `frontend/src/views/PublicProfileShowcaseView.vue` 추가, `/profiles/:profileId`를 Lovable 공개 프로필 시안으로 연결, 기존 `/profiles/:profileId/portfolio/:portfolioItemId`는 레거시 기능 화면 유지 |
| 구현됨/검증 필요 | 작업공간 | `frontend/src/views/WorkspaceShowcaseView.vue` 추가, 신규 `/workspace` route와 상단 `작업공간` 메뉴 연결 |
| 구현됨/검증 필요 | 관리자 | `frontend/src/views/AdminShowcaseView.vue` 추가, `/admin` 상위 route를 Lovable 운영 콘솔 시안으로 연결, 기존 `/admin/...` 하위 route와 `adminMeta` 권한 흐름은 유지 |
| 구현됨/검증 필요 | 로그인/회원가입 | Lovable 결과물에 별도 auth route가 없음을 확인하고 기존 인증 기능 화면을 유지, `theme.css` auth override로 검정 기본/흰색 선택 테마 정합성 적용, 로그인 모바일 하단 레거시 탭 제거 |
| 구현됨/검증 필요 | 빌드 검증 | 각 단계에서 `npm.cmd run build` 통과. Vite chunk size 경고는 기존 수준의 경고로 남음 |
| 구현됨/검증 필요 | route 확인 | `/`, `/discover`, `/works`, `/contests`, `/locations`, `/teams`, `/teams/t-blueroom`, `/profiles/p-yoon`, `/profiles/1`, `/workspace`, `/admin`, `/login`, `/register`, `/register/user`, `/register/company`, `/register/complete`, `/register/company/pending`, `/demo-access` dev server 응답 200 확인 |

## 남은 작업

| 우선순위 | 작업 | 상태 | 남은 확인 |
|---:|---|---|---|
| 1 | 레거시 화면 정리 | 부분 구현 | nav에서 숨긴 `/matching`, `/boards`와 Lovable 신규 route의 역할 중복 정리 필요 |
| 2 | 반응형·시각 QA | 검증 필요 | desktop/mobile Playwright 또는 브라우저 시각 확인, 텍스트 overflow, header/footer 충돌 확인 |
| 3 | 문서 동기화 | 진행 중 | 이 문서와 `docu/13_work_status/current_and_completed_work.md`, work log를 작업마다 갱신 |

## 다음 작업 원칙

1. 작업 시작 전 이 문서의 `남은 작업`에서 다음 항목을 선택한다.
2. 코드 변경 전 Lovable 원본 route와 현재 Vue route를 함께 확인한다.
3. 기능 구현보다 화면 확인 가능성을 우선한다.
4. route 상위 화면만 Lovable 시안으로 바꾸고, 인증·상세·관리 하위 기능은 필요한 경우 기존 화면을 유지한다.
5. 변경 후 `npm.cmd run build`와 해당 route의 dev server 응답을 확인한다.
6. 완료한 항목은 이 문서의 완료/남은 작업 표와 work log에 반영한다.

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_common/document_structure.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/13_work_status/current_and_completed_work.md`
- `design/design_dark_light_theme/src/routes`
- `frontend/src/router/index.js`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/styles/theme.css`
