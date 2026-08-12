# 사용자 팔로우 프런트엔드 구현 프롬프트

## 사용 목적

구현 완료된 팔로우 백엔드 API를 현재 Vue 프런트엔드에 연결한다. 매칭 후보 상세에서 실제 사용자를 팔로우하고, 내 프로필에서 팔로워·팔로잉 수와 목록을 관리할 수 있게 한다.

이번 작업에서는 홈 화면과 팔로우 활동 피드를 수정하지 않는다. 팔로우 기본 사용자 흐름을 먼저 완성하고 검증하기 위한 단계다.

## 프롬프트

```text
당신은 Slate 프로젝트의 사용자 팔로우 프런트엔드 구현 담당자입니다.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

## 필수 참조 순서

1. Agent.md
2. docu/README.md
3. docu/00_common/reference_policy.md
4. docu/06_frontend/frontend_baseline.md
5. docu/handoff/backend_to_frontend_follow.md
6. docu/prompt/follow_backend_prompt.md
7. frontend/src/services/api.js
8. frontend/src/router/index.js
9. frontend/src/views/MatchingView.vue
10. frontend/src/views/ProfileView.vue
11. frontend/src/views/BoardView.vue
12. frontend/src/styles/slate.css

현재 코드와 사용자의 기존 변경 사항을 먼저 확인하고 작업하세요. 관련 없는 화면과 스타일을 정리하거나 되돌리지 마세요.

## 현재 상태

백엔드에는 다음 인증 필수 API가 구현되어 있습니다.

- POST `/api/profiles/{profileId}/follow`
- DELETE `/api/profiles/{profileId}/follow`
- GET `/api/profiles/{profileId}/follow-status`
- GET `/api/profiles/{profileId}/followers?limit=20&offset=0`
- GET `/api/profiles/{profileId}/following?limit=20&offset=0`

`{profileId}`는 userId가 아니라 프로필 ID입니다. 현재 사용자 ID를 body나 query에 보내지 마세요.

백엔드 Service 테스트 12개와 전체 Maven 테스트 51개가 통과했습니다.
기존 handoff에 남아 있는 DB 비밀번호 불일치 문구는 현재 상태가 아닙니다.
2026-06-19에 `Slate/.env`의 비밀번호로 `slate_app` DB 인증 성공을 다시 확인했습니다.

현재 프런트 상태:

- `api.js`에는 팔로우 API 함수가 없습니다.
- `/profile`은 내 프로필 관리 화면입니다.
- 다른 사용자 후보 상세는 `/matching/members/:userId`에서 표시됩니다.
- 매칭 후보 데이터에는 실제 결과인 경우 `profileId`와 `userId`가 포함됩니다.
- MatchingView에는 fallback sample 후보가 있으며 이 데이터에는 실제 profileId가 없습니다.
- BoardView의 `인기 프로필`과 `팔로우` 버튼은 하드코딩된 샘플 UI이며 실제 API와 연결할 수 없습니다.
- 프로필 이미지 컬럼은 백엔드 팔로우 응답에 없습니다.

## 목표 사용자 흐름

1. 사용자가 팀원 매칭 후보 상세를 연다.
2. 실제 후보의 팔로우 상태와 팔로워 수를 확인한다.
3. 팔로우 또는 팔로우 취소를 실행한다.
4. 내 프로필에서 팔로워 수와 팔로잉 수를 확인한다.
5. 팔로워 또는 팔로잉 목록을 열고 추가 목록을 불러온다.
6. 목록 안에서도 다른 사용자를 팔로우하거나 취소할 수 있다.

## 구현 범위

### 1. API 함수 추가

`frontend/src/services/api.js`의 `slateApi`에 다음 함수를 추가하세요.

권장 함수명과 동작:

```js
followProfile(profileId) {
  return api(`/api/profiles/${profileId}/follow`, { method: 'POST' })
}

unfollowProfile(profileId) {
  return api(`/api/profiles/${profileId}/follow`, { method: 'DELETE' })
}

followStatus(profileId) {
  return api(`/api/profiles/${profileId}/follow-status`)
}

profileFollowers(profileId, { limit = 20, offset = 0 } = {}) {
  const query = new URLSearchParams({ limit: String(limit), offset: String(offset) })
  return api(`/api/profiles/${profileId}/followers?${query.toString()}`)
}

profileFollowing(profileId, { limit = 20, offset = 0 } = {}) {
  const query = new URLSearchParams({ limit: String(limit), offset: String(offset) })
  return api(`/api/profiles/${profileId}/following?${query.toString()}`)
}
```

프로필 ID가 없을 때 URL에 `undefined`가 들어가지 않도록 호출부에서 검증하세요.
기존 공통 `api()`의 토큰·오류 처리 방식을 그대로 사용하고 별도 fetch wrapper를 만들지 마세요.

### 2. 재사용 가능한 팔로우 목록 UI

다음 컴포넌트를 새로 만드는 방향을 우선 고려하세요.

- `frontend/src/components/follows/FollowListDialog.vue`

ProfileView에 모든 목록 로직과 DOM을 밀어 넣는 것보다, 팔로워와 팔로잉이 같은 UI를 공유하도록 구성합니다.

권장 props:

- `open`
- `mode`: `followers` 또는 `following`
- `profileId`
- `title`

권장 emits:

- `close`
- `counts-changed`

컴포넌트 동작:

- 열릴 때 offset 0부터 첫 목록 조회
- mode에 따라 `profileFollowers` 또는 `profileFollowing` 호출
- `items`, `totalCount`, `hasMore`, `limit`, `offset`을 서버 응답 기준으로 관리
- `더 보기` 클릭 시 다음 offset을 요청하고 기존 목록 뒤에 추가
- profileId 또는 mode가 바뀌면 기존 목록과 오류를 초기화
- 닫힌 상태에서는 불필요한 API를 호출하지 않음
- 연속 열기나 느린 응답에서 이전 요청 결과가 새 상태를 덮지 않도록 요청 식별자 등의 방어 적용

목록 항목 표시 정보:

- displayName, 없으면 nickname
- shortIntro
- publicRegionName
- experienceLevel은 기존 공통 코드 표시 이름을 재사용할 수 있을 때만 변환하고, 그렇지 않으면 과장된 한글 경력을 만들지 않음
- followedAt
- `followingByCurrentUser` 기반 팔로우/팔로잉 버튼

프로필 이미지가 없으므로 외부 이미지나 샘플 프로필 이미지를 임의 매핑하지 마세요.
이름 첫 글자를 이용한 원형 이니셜 아바타를 CSS로 표시하세요.

목록 내 팔로우 동작:

- `followingByCurrentUser=false`이면 followProfile 호출
- `followingByCurrentUser=true`이면 unfollowProfile 호출
- 요청 중 해당 행 버튼 비활성화
- 성공 응답의 `following`으로 행 상태 갱신
- 성공 후 `counts-changed` emit
- 실패하면 기존 상태 유지 및 해당 dialog 안에 오류 표시
- 중복 클릭으로 여러 요청이 발생하지 않도록 방어

dialog 접근성:

- `role="dialog"`, `aria-modal="true"`, 제목 연결
- 명시적인 닫기 버튼
- backdrop 클릭과 Escape로 닫기
- 내부 버튼에 식별 가능한 텍스트 또는 aria-label 제공
- dialog가 열렸을 때 body가 불필요하게 가로 스크롤되지 않도록 처리

포커스 트랩을 위한 새 라이브러리는 설치하지 마세요. 현재 의존성 안에서 가능한 접근성을 구현하세요.

### 3. 내 프로필의 팔로우 요약

`frontend/src/views/ProfileView.vue`에서 내 프로필 데이터가 로드되고 `profile.profileId`가 확인된 뒤
`slateApi.followStatus(profile.profileId)`를 호출하세요.

추가 상태 예시:

- followSummary
- followSummaryLoading
- followSummaryError
- followDialogOpen
- followDialogMode

프로필 상단의 `profile-account-summary` 또는 히어로 하단에 다음 실제 수치를 표시하세요.

- 팔로워 `{followerCount}`
- 팔로잉 `{followingCount}`

각 수치는 button으로 구현하고 클릭 시 FollowListDialog를 엽니다.

- 팔로워 클릭 → mode `followers`
- 팔로잉 클릭 → mode `following`

자기 프로필이므로 팔로우 버튼은 표시하지 않습니다.
프로필이 아직 생성되지 않았다면 API를 호출하지 않고 수치를 0으로 하드코딩해 기능이 있는 것처럼 표시하지 마세요.
대신 `프로필을 먼저 완성해주세요`라는 기존 흐름을 유지하세요.

dialog에서 `counts-changed`가 발생하면 followStatus를 다시 호출해 상단 수치를 동기화하세요.
프로필 재생성, 삭제 또는 로그아웃 시 팔로우 관련 상태를 초기화하세요.

### 4. 매칭 후보 상세의 팔로우 버튼

`frontend/src/views/MatchingView.vue`의 팀원 후보 상세, 즉 mode가 `teamToMembers`이고
실제 selectedCard에 profileId가 있는 경우에만 팔로우 기능을 표시하세요.

표시 위치:

- 후보 이름/역할 주변 또는 `matching-detail-actions` 안
- 기존 `저장`, `초대` 동작을 깨뜨리지 않는 위치

표시 정보:

- 팔로우 전: `팔로우`
- 팔로우 중: `팔로잉`
- 요청 중: `처리 중`
- 가능하면 `팔로워 N명`을 작은 보조 텍스트로 표시

상태 로딩:

- selectedCard가 바뀔 때 `selectedCard.profileId` 기준으로 followStatus 호출
- profileId가 없거나 sample 카드이면 호출하지 않음
- memberToTeams 모드의 팀 상세에서는 표시하지 않음
- ownProfile=true면 버튼을 숨김
- 후보 변경 중 이전 요청 응답이 새 후보 상태를 덮지 않도록 방어
- 후보 상세을 떠나거나 매칭 상태가 초기화되면 팔로우 상태도 초기화

토글 동작:

- following=false → followProfile
- following=true → unfollowProfile
- 요청 중 버튼 비활성화
- 성공 응답 전체를 follow 상태에 반영하여 카운트까지 갱신
- 실패 시 기존 상태를 유지하고 기존 notice/error 패턴을 사용해 사용자에게 알림
- 카드 전체 클릭, 저장, 초대 버튼 이벤트와 충돌하지 않도록 처리

fallback sampleCards와 sampleTeamCards에는 실제 profileId가 없으므로 팔로우 버튼이나 가짜 팔로워 수를 표시하지 마세요.

### 5. BoardView의 가짜 팔로우 버튼 정리

`frontend/src/views/BoardView.vue`의 하드코딩된 `popularProfiles`에는 실제 profileId가 없습니다.
따라서 현재 정적 `팔로우` 버튼을 팔로우 API와 억지로 연결하지 마세요.

다음 중 현재 레이아웃을 덜 깨는 방식을 선택하세요.

- 정적 `팔로우` 버튼 제거
- 비대화형 역할 텍스트로 대체

실제 ID가 없는 샘플 사용자에 팔로우가 성공한 것처럼 보이는 UI를 남기지 마세요.
`popularProfiles` 전체를 실제 API로 바꾸는 작업은 이번 범위가 아닙니다.

### 6. 스타일

`frontend/src/styles/slate.css`의 기존 화이트·블루 카드 디자인과 버튼 패턴을 재사용하세요.

필요한 스타일 범위:

- 팔로우/팔로잉 버튼의 두 상태
- 팔로워·팔로잉 count button
- FollowListDialog backdrop와 panel
- 이니셜 아바타
- 목록 row, 빈 상태, 오류, 로딩, 더 보기
- 모바일 1열 대응

기존 CSS에 이미 사용자 변경이 있으므로 관련 선택자만 추가하거나 수정하세요.
관련 없는 profile, matching, board 스타일을 재정렬하거나 대규모 포맷 변경하지 마세요.

## 라우팅 정책

이번 단계에서는 새 공개 프로필 상세 route를 추가하지 마세요.

이유:

- 현재 `/profile`은 내 프로필 관리 전용입니다.
- 백엔드의 일반 프로필 상세 공개 범위 정책을 별도 확인하지 않은 상태에서 새 공개 상세 페이지를 만들면 비공개 정보 노출 위험이 있습니다.
- 매칭 후보 상세에서 이미 필요한 공개 후보 정보를 제공하고 있습니다.

팔로워·팔로잉 목록 항목을 클릭했을 때 존재하지 않는 프로필 route로 이동시키지 마세요.
목록 행은 이번 단계에서 정보 확인과 팔로우 토글까지만 제공합니다.

## 상태 및 오류 처리

- 화면 전체 loading/error와 팔로우 영역 loading/error를 분리
- 팔로우 API 하나의 실패로 MatchingView 또는 ProfileView 전체를 빈 화면으로 만들지 않음
- 404는 `프로필을 확인할 수 없습니다.`처럼 과도한 정보를 노출하지 않는 메시지 사용
- 403은 프로필 완성 또는 계정 상태 확인 안내
- 인증 만료는 기존 공통 API/라우터 흐름을 유지하고 토큰 처리 로직을 중복 구현하지 않음
- 오류가 나도 기존 매칭 저장·초대, 프로필 관리 기능은 계속 사용 가능해야 함

## 반응형과 접근성

- 데스크톱 dialog는 화면 중앙의 적절한 최대 너비 사용
- 모바일에서는 화면 폭에 맞추고 목록과 버튼이 겹치지 않게 배치
- count 영역은 작은 화면에서 2열 또는 자연스러운 줄바꿈
- 팔로우 버튼의 색상만으로 상태를 구분하지 말고 텍스트도 변경
- 모든 interactive element는 button을 사용하고 키보드 접근 가능하게 구성
- 과도한 애니메이션이나 새 UI 라이브러리 추가 금지

## 이번 단계에서 하지 말아야 할 일

- 백엔드 Java, Mapper, SQL 수정
- SecurityConfig 수정
- 홈 화면 수정
- 팔로우 활동 피드 구현
- 팔로우 추천 기능 구현
- 공개 프로필 상세 route 신설
- BoardView 하드코딩 인기 프로필을 실제 데이터라고 가정
- 가짜 profileId, 팔로워 수, 프로필 사진 생성
- sampleCards에 팔로우 상태 하드코딩
- 새로운 상태 관리 라이브러리 또는 UI 라이브러리 설치
- 기존 매칭 알고리즘과 AI 추천 동작 변경
- 관련 없는 목업 데이터와 화면을 광범위하게 정리

## 작업 기록

작업 시작 시 다음 로그를 작성하세요.

- `docu/work_logs/YYYY-MM-DD_frontend_follow.md`

후속 홈/피드 작업자에게 전달할 내용이 있으면 다음 handoff를 작성하세요.

- `docu/handoff/frontend_to_follow_feed_home.md`

활동 피드와 홈 화면을 구현 완료로 기록하지 마세요.

## 검증

### 정적/빌드 검증

frontend 디렉터리에서 실행:

```bash
npm run build
```

### 실제 화면 검증

가능하면 backend와 frontend를 실행하고 다음을 확인하세요.

1. 실제 팀원 매칭 후보 상세에서 팔로우 상태 조회
2. 팔로우 클릭 후 버튼, 팔로워 수, 알림 반영
3. 같은 버튼 연속 클릭 방어
4. 팔로잉 클릭 후 취소 및 카운트 반영
5. 자기 프로필에서 팔로워·팔로잉 수 조회
6. 팔로워 dialog 첫 페이지 및 더 보기
7. 팔로잉 dialog 첫 페이지 및 더 보기
8. dialog 목록 행에서 팔로우/취소
9. sample 후보에는 팔로우 버튼이 나타나지 않음
10. BoardView의 동작 없는 팔로우 버튼이 남아 있지 않음
11. 로그아웃 후 인증 전용 화면 접근 시 기존 login redirect 유지
12. 모바일 폭에서 dialog와 버튼 overflow 없음

테스트용 관계를 생성했다면 검증 후 원래 상태로 복구하세요.
실제 브라우저 검증을 하지 못했다면 build 성공만으로 화면 검증을 완료했다고 쓰지 마세요.

## 완료 조건

- `api.js`에 5개 팔로우 API 함수가 추가됨
- 실제 팀원 매칭 후보 상세에서 팔로우/취소가 동작함
- sample 후보에는 가짜 팔로우 UI가 없음
- 내 프로필에서 실제 팔로워·팔로잉 수를 확인할 수 있음
- 팔로워·팔로잉 목록에 pagination과 빈 상태가 구현됨
- 목록에서 팔로우/취소가 가능함
- 동작 없는 BoardView 팔로우 버튼이 제거됨
- 로딩, 오류, 중복 클릭, 느린 응답 경쟁 상태가 처리됨
- 기존 매칭·프로필·게시판 기능이 유지됨
- 데스크톱과 모바일 레이아웃이 자연스러움
- `npm run build`가 성공하고 결과가 기록됨
- 홈 화면과 활동 피드는 수정하지 않음

## 결과 보고

작업 완료 후 다음을 간결하게 보고하세요.

- 생성·수정한 파일
- 연결한 API와 화면
- 실제 팔로우/취소 검증 결과
- 목록 pagination 검증 결과
- `npm run build` 결과
- 브라우저에서 검증하지 못한 항목과 이유
- 후속 팔로우 피드 백엔드 및 홈 화면 작업 범위
```

## 참조 경로

- `docu/handoff/backend_to_frontend_follow.md`
- `docu/prompt/follow_backend_prompt.md`
- `frontend/src/services/api.js`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/BoardView.vue`
- `frontend/src/styles/slate.css`
