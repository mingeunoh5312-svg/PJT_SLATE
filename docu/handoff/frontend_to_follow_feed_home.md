# 프런트엔드 → 팔로우 피드/홈 인수인계

## 현재 완료 범위

- 팔로우 등록·취소·상태·팔로워·팔로잉 API가 `frontend/src/services/api.js`에 연결돼 있다.
- 실제 매칭 팀원 후보 상세에서 팔로우/취소가 가능하다.
- 내 프로필에서 실제 카운트와 팔로워·팔로잉 목록을 확인하고 목록 안에서 토글할 수 있다.
- `FollowListDialog.vue`가 pagination, 경쟁 상태, 오류, 접근성, 모바일 레이아웃을 공통 처리한다.
- BoardView의 실제 ID 없는 샘플 팔로우 버튼은 제거했다.

## 재사용 API

```js
slateApi.followProfile(profileId)
slateApi.unfollowProfile(profileId)
slateApi.followStatus(profileId)
slateApi.profileFollowers(profileId, { limit, offset })
slateApi.profileFollowing(profileId, { limit, offset })
```

URL에는 profileId를 사용하며 현재 userId를 body/query에 보내지 않는다.

## 후속 활동 피드 범위

- 현재 백엔드에는 팔로우 기반 활동 피드 API가 없다.
- 피드 구현 전 공개 가능한 활동 종류, 정렬/커서, 탈퇴·비공개 전환 처리와 개인정보 노출 범위를 먼저 확정해야 한다.
- `user_follow`를 기반으로 하되 Board 좋아요나 Matching bookmark를 팔로우 관계처럼 사용하지 않는다.
- 피드 API가 준비되기 전 홈 화면에 정적 팔로우 활동을 실제 데이터처럼 표시하지 않는다.

## 후속 홈 화면 범위

- 이번 작업에서는 `HomeView.vue`를 수정하지 않았다.
- 홈 피드는 별도 로딩·오류·빈 상태를 두고 기존 홈 전체 로딩과 분리한다.
- 실제 profileId가 없는 홈 샘플 카드에는 팔로우 토글을 연결하지 않는다.
- 팔로우 상태가 변경되면 필요 시 공통 사용자 이벤트를 추가하되 기존 `slate-auth-changed`, `slate-profile-changed` 흐름과 충돌하지 않게 한다.

## 검증 메모

- 실제 이지은 후보 팔로우/취소와 Profile 카운트·목록 동기화를 브라우저에서 검증했다.
- 테스트 관계와 생성된 알림·감사 로그는 검증 후 정리했다.
- 모바일 390x844에서 dialog 가로 overflow가 없음을 확인했다.
- 데이터가 20건 이하라 실제 `더 보기` 클릭은 미검증이며 21건 이상 fixture에서 후속 smoke가 필요하다.
