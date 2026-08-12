# 프론트엔드 기준

## 기술 스택

| 항목 | 기준 |
|---|---|
| Framework | Vue 3 |
| Build | Vite |
| Router | Vue Router |
| Node | `>=20.19.0` |
| Dev port | `5174` |
| Preview port | `4174` |

## 패키지 기준

| 항목 | 현재 값 |
|---|---|
| package name | `slate-frontend` |
| dev script | `vite --host 0.0.0.0 --port 5174` |
| build script | `vite build` |
| preview script | `vite preview --host 0.0.0.0 --port 4174` |

## route 기준

프론트 route 기준은 `frontend/src/router/index.js`다. 인증이 필요한 route는 `requiresAuth`, 관리자 route는 `requiresAdmin` meta를 사용하고, 배포 데모 gate는 `VITE_DEMO_ACCESS_GATE=true`일 때 `/demo-access`를 먼저 통과해야 한다.

| layout/view | 용도 |
|---|---|
| `AppLayout` | 일반 서비스 화면 |
| `AuthLayout` | 로그인/회원가입 |
| `AdminLayout` | 관리자 화면 |
| `DemoAccessView` | 배포 데모 접속 코드 입력 |

## API 연결

| 항목 | 기준 |
|---|---|
| API client | `frontend/src/services/api.js` |
| token storage | `localStorage`의 `slate.accessToken` |
| demo code storage | `localStorage`의 `slate.demoAccessCode` |
| local API base | 기본값 `''`, Vite proxy로 `/api -> http://localhost:8080` |
| deployed API base | `VITE_API_BASE_URL` 또는 reverse proxy 정책 필요 |
| 외부 API key | 프론트 코드와 env에 넣지 않음 |

## 화면/에셋 기준

| 구분 | 기준 |
|---|---|
| 화면 기준 이미지 | `../prototype_3/images_page_ai`는 참조 전용. 최종 저장소 보관 제외 |
| 구현 에셋 | `frontend/src/assets` |
| 화면 문서 | `docu/02_prototype_comparison/screen_diff.md`, 필요 시 `../prototype_3/docu/04_pages` 읽기 전용 |
| 스타일 | `frontend/src/styles/slate.css` |

## 최근 화면 동작 기준

| 영역 | 현재 동작 |
|---|---|
| 홈 | 실제 사용자·팀·작업물·알림 데이터를 사용한다. 알림 요약 카드는 기존 알림 패널을 열며, YouTube 작업물만 유효한 HTTP(S) 썸네일을 표시한다. |
| 팔로우 | 매칭 후보 상세에서 팔로우/취소가 가능하고, 내 프로필에서 실제 카운트와 팔로워·팔로잉 목록을 조회·변경한다. |
| 매칭 탐색 | `/matching`은 `/matching/teams`로 이동한다. `팀원 찾기`와 `팀 찾기` 안에서 일반 추천과 버튼 실행 방식 AI 추천을 구분한다. |
| 팀 찾기 필터 | 일반 추천은 URL의 `applied=1`이 있을 때만 조회한다. 필터 변경 후에는 재적용 전까지 기존 결과를 숨기며 초기화는 결과와 query를 함께 제거한다. |
| 저장한 팀 | `/matching/teams?view=saved`에서 현재 사용자의 전체 저장 목록을 조회한다. 실제 OPEN 역할을 선택한 경우에만 해당 `teamId`, `recruitmentId`, `slotId`로 지원한다. |
| 팀 상세 | 지원/초대 대기는 접근성 속성을 갖춘 모달로 표시한다. 계획은 `/teams/:teamId/plans`에서 조회·관리하고 일정 query/hash 직접 접근을 지원한다. |
| 프로필 | 샘플 fallback 없이 실제 프로필·팀·작품·포트폴리오를 사용한다. 사용자 `creditName`과 KOBIS 매칭 이름·역할·상태를 상세에서 분리하고 `verified === true`만 Verified로 표시한다. |
| 대표 이미지 | 프로필·팀·작업물·포트폴리오의 실제 업로드 이미지를 사용한다. 작업물은 업로드 이미지 > YouTube 썸네일, 포트폴리오는 업로드 이미지 > YouTube 썸네일 순이며 실패 시 placeholder로 복구한다. |
| 포트폴리오 출처 | 카드에는 `PUBLIC_DATA_MANUAL` 같은 내부 값을 노출하지 않고 실제 외부 출처 또는 직접 등록으로 표시한다. 미검증 항목은 Verified 배지를 표시하지 않는다. |
| 게시판 | HOME/WORK/FREE/POPULAR 탭과 `/boards/search` 검색 결과 route를 사용한다. URL query로 범위·검색어·정렬·작품 유형·장르를 복원하며, HOME 최신 작업물, 주간/월간/전체 인기 작업물과 인기 프로필을 실제 API로 제공한다. WORK에서는 일반 글쓰기를 숨기고 FREE는 밀도형 행 전체 링크를 사용한다. |
| 공모전 목록 | `/contests`는 샘플·추천 hero·fit 표시 없이 실제 OPEN 공모전 전체를 마감일 순 목록으로 표시한다. 검색어와 대상·지역·주최 유형·총상금·1등 상금 필터를 URL query로 복원하며 별도 마감 임박 API를 사용한다. |
| 공모전 필터 데이터 | 아직 외부 크롤링 전이다. 기존 문자열에서 분류나 금액을 추정하지 않으며 회사 요청·승인 공모전 수정·관리자 등록에서 직접 저장한 구조화 값만 세부 필터 대상이 된다. |
| 공모전 이미지 | 업로드 이미지 > `representativeImageUrl` > 공통 기본 이미지 순으로 표시하고 로드 실패도 기본 이미지로 복구한다. 회사 요청/승인 공모전 편집은 JPEG/PNG/WebP 직접 선택·미리보기·교체/삭제를 지원하며 비공개 요청 이미지는 인증 blob으로 재표시한다. |
| 공모전 적합도 | 상세 최초 진입과 새로고침에는 결과가 없다. 사용자가 기준을 선택하고 `적합도 분석`을 실행한 성공 응답만 표시하며 기준 변경 시 즉시 초기화한다. |
| 관리자 | `/admin`은 콘솔형 레이아웃을 사용한다. 권한이 있는 모듈만 업무 레일과 대시보드에 표시하며, 대시보드 수치는 실제 로드 데이터 기준이다. 회사 승인/거절, 공모전 개설 요청 승인/거절, 공모전 종료/재개는 관리자 처리 사유를 입력한 뒤 기존 API로 전송한다. |

## 검증 상태와 남은 검증

| 항목 | 상태 |
|---|---|
| build | `npm run build` 통과 |
| route 직접 접근/새로고침 | 매칭 일부 route 확인 완료, 전체 회귀 smoke 필요 |
| 모바일 overflow | 게시판 HOME·WORK·FREE·POPULAR을 390x844에서 확인해 overflow와 콘솔 오류 0건. 홈·팔로우·일부 매칭도 확인 완료 |
| 게시판 검색 후속 | 1280x720 및 390x844에서 범위 탭, 검색 URL/새로고침 복원, 인기 2열/1열, 기간 query, overflow와 콘솔 오류 0건 확인 |
| 공모전 실제 데이터 UI | 게스트·일반 USER·COMPANY 권한 노출, 실제 목록/마감 임박 정렬, 상세 분석 전후/기준 변경, 1280 및 390x844 overflow와 콘솔 오류 0건 확인 |
| 공모전 구조화 필터 | 대상+지역 다중 조건, URL/새로고침 복원, 초기화, 기존 미분류 데이터 제외, 회사 구조화 입력, 1280 및 390x844 overflow와 콘솔 오류 0건 확인 |
| 인증 세션 mutation | 팔로우 등록·취소는 브라우저 확인 완료. 지원·초대·저장 팀 mutation 전체 회귀 확인 필요 |
| YouTube UI | 실제 API key와 브라우저 검증 필요 |
| AI 추천 UI | 실제 OpenAI key와 실패 UI 확인 필요 |
| 크레딧/Verified | 실제 KOBIS 일치와 build 확인 완료. 최신 화면의 모바일/데스크톱 시각 회귀 확인 필요 |
| 엔티티 이미지 UI | API 업로드/조회/삭제 확인 완료. 브라우저 파일 선택·YouTube 전환·반응형 확인 필요 |
| 관리자 콘솔 UI | mocked 관리자 API 기준 desktop 1440x1000, mobile 390x900에서 레이아웃 smoke와 page error 0건 확인. 실제 백엔드 mutation과 감사 로그 확인은 필요 |
| 데모 접속 gate | `VITE_DEMO_ACCESS_GATE=true`와 backend filter 조합 smoke 필요 |

## 참조 경로

- `frontend/package.json`
- `frontend/vite.config.js`
- `frontend/src/router/index.js`
- `frontend/src/services/api.js`
- `frontend/src/views`
- `frontend/src/layouts`
- `frontend/src/assets`
- `frontend/src/styles/slate.css`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/02_prototype_comparison/screen_diff.md`
- `docu/work_logs/2026-06-19_frontend_follow.md`
- `docu/work_logs/2026-06-20_matching_navigation_ai_integration.md`
- `docu/work_logs/2026-06-20_fixer_team_detail_navigation.md`
- `docu/work_logs/2026-06-21_fixer_matching_saved_teams.md`
- `docu/work_logs/2026-06-21_fixer_matching_team_filter_apply.md`
- `docu/work_logs/2026-06-21_fixer_profile_dashboard.md`
- `docu/work_logs/2026-06-22_fixer_portfolio_credit_roundtrip.md`
- `docu/work_logs/2026-06-22_fixer_profile_media_and_portfolio_display.md`
- `docu/work_logs/2026-06-22_fixer_board_full_integration.md`
- `docu/work_logs/2026-06-22_fixer_board_search_ui_period_ranking_followup.md`
- `docu/work_logs/2026-06-23_fixer_contest_data_ui_image_fit.md`
- `docu/work_logs/2026-06-23_fixer_contest_structured_search_filters.md`
- `docu/work_logs/2026-06-25_fixer_admin_page_ui_cleanup.md`
