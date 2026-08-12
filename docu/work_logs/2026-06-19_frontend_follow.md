# 2026-06-19 프런트엔드 작업 로그 - 사용자 팔로우

## 작업 범위

- 팔로우 API 5개를 공통 API 서비스에 연결
- 실제 매칭 팀원 후보 상세에서 팔로우/취소 제공
- 내 프로필에서 팔로워·팔로잉 수와 목록 제공
- 공용 팔로우 목록 dialog와 pagination 구현
- BoardView의 실제 ID 없는 샘플 팔로우 버튼 제거
- 홈 화면과 팔로우 활동 피드는 변경하지 않음

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/handoff/backend_to_frontend_follow.md`
- `docu/prompt/follow_backend_prompt.md`
- `docu/prompt/follow_frontend_prompt.md`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/BoardView.vue`
- `frontend/src/styles/slate.css`

## 작업 계획

| 순서 | 작업 | 상태 |
|---:|---|---|
| 1 | 현재 API·라우트·화면·스타일 구조 확인 | DONE |
| 2 | API 함수와 FollowListDialog 구현 | DONE |
| 3 | ProfileView와 MatchingView 연결 | DONE |
| 4 | BoardView 가짜 동작 제거 및 반응형 스타일 | DONE |
| 5 | build 및 실제 브라우저 검증 | DONE |
| 6 | handoff와 결과 기록 | DONE |

## 변경 파일

- `frontend/src/services/api.js`
- `frontend/src/components/follows/FollowListDialog.vue`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/BoardView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-19_frontend_follow.md`
- `docu/handoff/frontend_to_follow_feed_home.md`

## 구현 결과

- 팔로우 등록, 취소, 상태, 팔로워 목록, 팔로잉 목록 API 5개를 공통 `slateApi`에 추가했다.
- 실제 숫자형 `profileId`가 있는 팀원 매칭 상세에만 팔로우 상태·카운트·토글을 표시한다.
- sample 카드와 팀 상세에는 팔로우 UI를 표시하지 않는다.
- 내 프로필에 실제 팔로워·팔로잉 카운트 버튼을 추가하고 공용 dialog로 목록을 연다.
- dialog는 첫 페이지, 더 보기, 빈 상태, 오류, 행별 팔로우 토글, 요청 경쟁 방어, 중복 클릭 방어를 구현했다.
- dialog에 role/aria 연결, 명시적 닫기, backdrop/Escape 닫기, 초기 닫기 버튼 포커스, body overflow 잠금을 적용했다.
- BoardView의 실제 ID 없는 인기 프로필 `팔로우` 버튼을 비대화형 `추천 프로필` 표시로 교체했다.
- 홈 화면, 활동 피드, 새 공개 프로필 route는 추가하지 않았다.

## 실행 명령 및 결과

- `npm run build`: PASS, 99 modules transformed.
- `git diff --check`: PASS.
- 실제 브라우저 데스크톱 검증: PASS.
  - leader 로그인과 `/profile` 실제 카운트 0/0 조회.
  - 빈 팔로워 dialog, 닫기 버튼 초기 포커스, Escape 닫기 확인.
  - 실제 매칭 후보 이지은(profileId 3)에서 팔로우 → 팔로잉, 팔로워 0→1 확인.
  - 내 프로필 팔로잉 0→1, 목록 항목/이니셜/공개 정보 표시 확인.
  - 목록 행에서 취소 후 버튼과 상단 카운트 1→0 동기화 확인.
  - Board 인기 프로필 5행에서 동작 가능한 팔로우 버튼 0개, 정적 표시 5개 확인.
  - 로그아웃 후 `/profile` 접근이 `/login?redirect=/profile`로 이동하는지 확인.
- 실제 브라우저 모바일 390x844 검증: PASS.
  - dialog 좌우 경계 0~390px, document scrollWidth 390px로 가로 overflow 없음.
  - dialog open 동안 body overflow 잠금 확인.
- 브라우저 console error: 0건.
- 테스트 관계, SOCIAL 알림 1건, 팔로우 감사 로그 2건을 검증 후 정확한 조건으로 정리했다.
- 검증용 백엔드 프로세스 정상 종료.

## 남은 이슈

- 현재 공개 테스트 관계가 20개 이하라 실제 `더 보기` 버튼을 브라우저에서 클릭하는 시나리오는 만들지 않았다.
- pagination은 서버의 `limit`, `offset`, `hasMore` 응답을 사용하는 코드와 build로 검증했으며 21건 이상 데이터에서 후속 클릭 smoke가 필요하다.
- 빠른 연속 클릭은 행/상세별 loading guard와 버튼 disabled로 방어했으나 네트워크 지연을 인위적으로 주입한 E2E는 수행하지 않았다.
- 팔로우 활동 피드 백엔드와 홈 화면 노출은 후속 작업이다.
