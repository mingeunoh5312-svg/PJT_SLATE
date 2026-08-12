# 화면 차이

## 화면 구조 변화

| 단계 | 화면 기준 | 특징 |
|---|---|---|
| `prototype` | P00-P11 프로토타입 화면 | 사용자 선택, 기준 데이터, 프로필, 팀, 모집, 매칭, 게시판 중심 |
| `prototype_2` | 실제 서비스 App Shell 0단계 | `/`, `/login`, `/register`, `/matching`, `/profile`, `/teams`, `/boards`, `/contests`, `/admin` |
| `prototype_3` | `images_page_ai` 대표 이미지 + 독립 route | App/Auth/Admin layout, 세부 route 분리, 대표 이미지 기반 UI |

## `prototype_3` route 기준

| 영역 | 주요 route |
|---|---|
| 홈 | `/` |
| 인증 | `/login`, `/register`, `/register/user`, `/register/company`, `/register/complete`, `/register/company/pending` |
| 매칭 | `/matching`, `/matching/members`, `/matching/members/:userId`, `/matching/teams`, `/matching/teams/:teamId`, `/matching/ai` |
| 팀 | `/teams`, `/teams/new`, `/teams/:teamId`, `/teams/:teamId/edit`, `/teams/:teamId/members`, `/teams/:teamId/recruitments` |
| 게시판 | `/boards`, `/boards/new`, `/boards/:postId`, `/boards/:postId/edit` |
| 공모전 | `/contests`, `/contests/new-request`, `/contests/requests`, `/contests/company`, `/contests/company/new`, `/contests/:contestId`, `/contests/:contestId/prepare` |
| 프로필 | `/profile`, `/profile/edit`, `/profile/privacy`, `/profile/account`, `/profile/recovery`, `/profile/portfolio`, `/profile/files`, `/profile/youtube`, `/profile/public-data` |
| 관리자 | `/admin`, `/admin/users`, `/admin/posts`, `/admin/teams`, `/admin/companies`, `/admin/reports`, `/admin/files`, `/admin/contests`, `/admin/notifications`, `/admin/roles`, `/admin/logs`, `/admin/score-policies` |

## 대표 이미지 기준

| 페이지 | 대표 이미지 경로 | 현재 상태 |
|---|---|---|
| 홈 | `prototype_3/images_page_ai/01_홈 화면_desktop.png`, `prototype_3/images_page_ai/01_홈 화면.png` | 기준 이미지 있음 |
| 로그인 | `prototype_3/images_page_ai/02_로그인_desktop.png`, `prototype_3/images_page_ai/02_로그인_mobile.png` | 기준 이미지 있음 |
| 회원가입 | `prototype_3/images_page_ai/03_회원가입_desktop.png`, `prototype_3/images_page_ai/03_회원가입_mobile.png` | 기준 이미지 있음 |
| 매칭 | `prototype_3/images_page_ai/04_매칭_desktop.png`, `prototype_3/images_page_ai/04_매칭_mobile.png` | 기준 이미지 있음 |
| 프로필 | `prototype_3/images_page_ai/05_프로필_desktop.png`, `prototype_3/images_page_ai/05_프로필_mobile.png` | 기준 이미지 있음 |
| 팀 | `prototype_3/images_page_ai/06_팀_desktop.png`, `prototype_3/images_page_ai/06_팀_mobile.png` | 기준 이미지 있음 |
| 게시판 | `prototype_3/images_page_ai/07_게시판_desktop.png`, `prototype_3/images_page_ai/07_게시판_mobile.png` | 기준 이미지 있음 |
| 공모전 | `prototype_3/images_page_ai/08_공모전_desktop.png`, `prototype_3/images_page_ai/08_공모전_mobile.png` | 기준 이미지 있음 |
| 관리자 | `prototype_3/images_page_ai/09_관리자_desktop.png`, `prototype_3/images_page_ai/09_관리자_mobile.png` | 기준 이미지 있음 |

## 남은 화면 검증

| 항목 | 상태 |
|---|---|
| `/profile/portfolio` Verified 배지 모바일/데스크톱 레이아웃 | 확인 필요 |
| `/matching/ai` 실제 OpenAI 호출 UI | 확인 필요 |
| `/boards` YouTube 미리보기/등록/수정/삭제 브라우저 검증 | 확인 필요 |
| `/admin` 게시글/회원/팀 CRUD 브라우저 검증 | 확인 필요 |
| route 개편 후 직접 URL 접근, 새로고침, 모바일 overflow | 전체 smoke 필요 |

## 참조 경로

- `prototype/docs/screens.md`
- `prototype_2/docs/screens.md`
- `prototype_2/handoff/prototype_3_front_design_handoff.md`
- `prototype_3/docu/04_pages/page_index.md`
- `prototype_3/docu/05_assets/image_inventory.md`
- `prototype_3/frontend/src/router/index.js`
- `prototype_3/frontend/src/views`
- `prototype_3/frontend/src/layouts`
- `prototype_3/images_page_ai`
- `prototype_3/docu/02_workflows/fixer_work_summary.md`
