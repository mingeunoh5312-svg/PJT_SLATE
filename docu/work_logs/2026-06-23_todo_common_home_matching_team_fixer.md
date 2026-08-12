# 공통·메인·매칭·팀 TODO 구현 재개 로그

작성일: 2026-06-23

## 읽은 문서와 기준

- `Slate/docu/prompt/todo_common_home_matching_team_fixer_prompt.md`
- `Slate/docu/user_temp/260623 Codex Log.md`
- `Slate/docu/user_temp/todo.md`
- 이전 로그 기준 필수 문서: `Agent.md`, `docu/README.md`, `docu/00_common/reference_policy.md`, `docu/00_common/document_structure.md`, `docu/03_mvp_scope/mvp_decisions.md`, `docu/03_mvp_scope/mvp_scope.md`, `docu/05_backend/backend_baseline.md`, `docu/06_frontend/frontend_baseline.md`, `docu/09_deployment/deployment_plan.md`

## 사용자 질문과 답변

- 안내 페이지는 합리적으로 진행하되 버튼 문구를 `입력`으로 하고, 관리자가 지정한 코드를 입력하는 구성으로 진행.
- 메인 카드 배경 이미지는 임의 기본 이미지를 설정하지 않고, 필요한 이미지 목록과 저장 경로를 별도 문서로 정리.
- 팀 종료 페이지는 추천안 기준인 `/teams/:teamId/close`로 분리하고 메뉴에는 노출하지 않음.

## 변경 파일

- `frontend/src/views/MatchingView.vue`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/styles/slate.css`
- `backend/src/main/java/com/slate/matching/MatchingService.java`
- `docu/user_temp/todo_common_home_matching_team_image_requirements.md`

## 구현 항목 매핑

- 매칭 페이지
  - 팀원 찾기 하위 탭 `전체`/`팔로우`를 추가.
  - `팔로우` 탭은 기존 `profileFollowing` API를 재사용하고, 공개 프로필 API로 역할/장르/협업 조건을 보강해 기존 카드/필터 UI에 연결.
  - 팀 1개 보유 사용자도 기준 팀 select UI를 보도록 변경.
  - 기준 팀 기본 선택은 `createdAt` 최신순 기준으로 정리.
  - 장르, 모집 역할, 작업 일정, 협업 조건 선택값을 버튼 아래 칩으로 표시하고 X로 개별 해제 가능하게 구현.
  - `AFTER_1M`의 화면 표시명을 `1개월 이후`에서 `6개월 이내`로 치환.
  - `AI로 팀원 추천받기`, `AI로 팀 추천받기` 버튼 문구를 `AI 추천`으로 축약.
  - `필터 초기화` 문구를 `초기화`로 정리.
  - 팀 찾기 필터 변경 후 기존 결과를 유지한다는 안내 문구로 수정.
  - 중간 패치에서 누락된 `MatchingView.vue` 문법 오류를 고쳐 `npm run build` 통과.

- 공모전 저장 연동
  - 공모전 목록과 상세의 저장 버튼을 메인 페이지와 같은 하트 버튼 UI로 통일.
  - 공모전 페이지의 `저장한 공모전` 탭은 기존 저장 상태를 그대로 사용.
  - 사용자가 요청한 이미지 정책에 맞춰 새 임의 기본 이미지를 추가하지 않고, 공모전 이미지가 없거나 로딩에 실패하면 텍스트 placeholder를 표시.

- 팀 페이지
  - 내 팀 목록의 관심 팀 별표와 팀 아카이브 안내 제거.
  - 진행 중인 팀과 종료된 팀을 분리하고, 진행 중인 팀은 최신 생성순 최대 3개만 표시.
  - 종료된 팀 블럭에 `최신순/오래된순` 정렬 선택 추가.
  - 팀 목록 카드를 대표 이미지, 팀 정보, 진행률 원형 구조로 정리.
  - 팀 대표 이미지에 확대 모달 연결.
  - 팀 상세 상단 버튼을 `팀 목록`, `팀 정보 수정`으로 정리하고 모집/멤버/계획 관리 버튼은 상단에서 제거.
  - `지원/초대 대기`, `모집 슬롯 요약`, `멤버 현황`을 `모집 대기`, `모집 현황`, `멤버 보기` 별도 블럭으로 이동.
  - 팀 설명 줄바꿈 보존, 긴 단어 줄바꿈, 500자 초과 `더 보기` 처리 추가.
  - 계획 진행률에서 `다음 마일스톤` 문구 제거, D-Day와 일정 제목/일자를 진행률 옆에 배치.
  - `계획 조작` 카드 제거, 일정 안내 하단 우측에 `팀 작업 종료하기` 버튼 추가.
  - `팀 작업 종료하기`가 팀 수정 화면으로 이동하던 문제를 `/teams/:teamId/close` 전용 route 이동으로 수정.
  - 팀 종료 블럭을 팀 정보 수정 화면에서 제거하고 `teams-close` 화면에만 표시.
  - 종료 확인 UI를 `팀 작업을 종료하시겠습니까?`, `취소`, `작업 종료`로 단순화.
  - 종료 팀 복구 UI에서 `복구 스냅샷`, `복구 방식`, 스냅샷 JSON 노출을 제거하고 `팀 복구` 단일 방식으로 정리.
  - 정상 종료 안내는 `팀 작업이 종료된 팀입니다.`, 해체 안내는 `해체된 팀입니다.`로 구분.
  - 팀 생성/수정 장르 선택에서 체크박스를 제거하고 활성/비활성 버튼 스타일로 변경.

- 공통 이미지 정책
  - 이미지 확대 모달 공통 스타일 추가.
  - 필요한 기본/배경 이미지 목록은 `docu/user_temp/todo_common_home_matching_team_image_requirements.md`에 정리.

## API와 응답 필드

- 이번 재개 작업에서 새 백엔드 API는 추가하지 않았다.
- 기존 매칭 API의 `joinAvailability`, `collaborationCondition` 필터가 복수 값 전체를 처리하도록 `MatchingService` 필터 로직을 수정했다.
- 팀 종료/복구는 기존 `closeTeam`, `reopenTeam`, `teamClosureSnapshots` API를 유지한다.
- 사용자 UI에서는 최신 종료 스냅샷만 내부적으로 사용하고 스냅샷 세부 정보는 노출하지 않는다.

## 계정별 동작

- 팀 리더: 팀 상세에서 `팀 작업 종료하기`로 `/teams/:teamId/close`에 진입해 종료 가능. 종료 후 복구 사유 입력과 `팀 복구` 가능.
- 팀 부리더/관리 가능 사용자: 모집 대기, 모집 현황, 멤버 보기 진입 가능. 종료 실행은 팀장 전용 안내 표시.
- 일반 팀원: 팀 상세 상단 정보와 설명, 일정 정보를 확인.

## 반응형 기준

- 팀 목록/상세/빠른 진입 카드의 grid 구조를 유지하고 기존 모바일 media query에서 1열로 전환되도록 보강.
- 이미지 확대 모달은 viewport 기준 `max-width`, `max-height`를 사용한다.

## 실행한 명령과 결과

- `npm run build` in `Slate/frontend`
  - 결과: 성공.
  - 참고: Vite chunk size 경고가 있으나 기존 번들 크기 경고이며 빌드는 통과.
- `mvn test` in `Slate/backend`
  - 결과: 실패.
  - 사유: Maven parent POM 다운로드가 네트워크 제한으로 차단됨. `repo.maven.apache.org` 접근이 `Permission denied: getsockopt`로 실패.
- Browser smoke via local dev server `http://127.0.0.1:5173`
  - 확인 route: `/`, `/matching/teams`, `/teams`, `/teams/1/close`
  - 확인 viewport: 1310x900, 960x900, 390x844
  - 결과: 모든 확인 조합에서 `#app` 렌더링, document 가로 overflow 0, console error 0.
  - 인증 필요 route는 비로그인 상태에서 `/login?redirect=...`로 이동함을 확인.
- Browser smoke via local dev server `http://127.0.0.1:5174`
  - 확인 route: `/`, `/matching/members`, `/matching/members?view=following`, `/matching/teams`, `/matching/teams?view=saved`, `/teams`, `/teams/1/close`, `/contests`, `/contests?view=saved`
  - 확인 viewport: 1310x900, 960x900, 390x844
  - 결과: 모든 확인 조합에서 `#app` 렌더링, document 가로 overflow 0.
  - 인증 필요 route는 비로그인 상태에서 `/login?redirect=...`로 이동함을 확인.
  - `/`, `/contests`, `/contests?view=saved`의 console error는 백엔드 8080 미기동에 따른 Vite proxy `502 Bad Gateway`와 정적 리소스 `404`로 확인.

## 미수행 검증과 남은 위험

- 백엔드 dev server는 8080에서 LISTEN 상태가 아니며, Maven 의존성 다운로드 제한 때문에 재기동하지 못했다. 로그인 후 팀 데이터 로딩, 팀 종료/복구 API 호출, 팔로우 탭 실제 데이터 표시까지의 end-to-end 브라우저 검증은 수행하지 못했다.
- 팀 목록 카드의 진행률은 현재 목록 API에 계획 진행률 필드가 없어 멤버 충원률 기준으로 표시한다. 실제 계획 진행률을 목록 카드에 표시하려면 백엔드 목록 응답에 팀별 계획 진행률 필드를 추가해야 한다.
- 메인 카드 배경 이미지와 공모전/작업물/팀 기본 이미지는 사용자가 이미지를 준비한 뒤 코드/API 연결이 필요하다.
