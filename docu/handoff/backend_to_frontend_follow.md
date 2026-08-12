# 백엔드 → 프런트엔드 인수인계 - 사용자 팔로우

> 상태 갱신(2026-06-19): 아래 API의 프런트 연결은 완료됐다. 현재 화면 기준은 `docu/handoff/frontend_to_follow_feed_home.md`와 `docu/work_logs/2026-06-19_frontend_follow.md`를 우선하며, 팔로우 기반 활동 피드만 후속 범위로 남아 있다.

## 공통 계약

- 모든 endpoint는 JWT 인증이 필요하다.
- URL의 `{profileId}`는 `userId`가 아니라 프로필 ID다.
- 현재 사용자 ID를 body나 query로 보내지 않는다.
- 응답은 `{ success, data, message }` 형태의 `ApiResponse`다.
- 공개 가능한 ACTIVE USER + ACTIVE/PUBLIC 프로필만 조회된다.

## Endpoint

### 팔로우 등록

```http
POST /api/profiles/{profileId}/follow
```

요청 body는 없다. 신규 생성은 `changed: true`, 이미 팔로우 중이면 `changed: false`다.

### 팔로우 취소

```http
DELETE /api/profiles/{profileId}/follow
```

실제 삭제는 `changed: true`, 이미 취소된 상태면 `changed: false`다.

등록과 취소의 `data`:

```json
{
  "profileId": 12,
  "userId": 35,
  "following": true,
  "changed": true,
  "followerCount": 8,
  "followingCount": 3
}
```

카운트는 URL이 가리키는 대상 프로필 기준이며 현재 공개 가능한 관계만 센다.

### 상태 조회

```http
GET /api/profiles/{profileId}/follow-status
```

```json
{
  "profileId": 12,
  "userId": 35,
  "following": true,
  "ownProfile": false,
  "followerCount": 8,
  "followingCount": 3
}
```

자기 프로필은 `ownProfile: true`, `following: false`다. 이 경우 팔로우 버튼을 숨긴다.

### 팔로워/팔로잉 목록

```http
GET /api/profiles/{profileId}/followers?limit=20&offset=0
GET /api/profiles/{profileId}/following?limit=20&offset=0
```

- limit 기본 20, 서버에서 1~50으로 정규화
- offset 기본 0, 음수는 0으로 정규화
- `created_at DESC`, 사용자 ID DESC 순서

```json
{
  "items": [
    {
      "profileId": 7,
      "userId": 18,
      "nickname": "영화인",
      "displayName": "영화인",
      "shortIntro": "촬영과 조명을 맡고 있습니다.",
      "publicRegionName": "서울특별시 종로구",
      "experienceLevel": "Y3_10",
      "followedAt": "2026-06-18T10:00:00",
      "followingByCurrentUser": true
    }
  ],
  "totalCount": 1,
  "limit": 20,
  "offset": 0,
  "hasMore": false
}
```

프로필 이미지 필드는 현재 제공하지 않는다.

## 오류 처리

- 400: 자기 자신 팔로우
- 401: 인증 정보 없음
- 403: ACTIVE USER가 아니거나 활성 프로필 없음
- 404: 대상 공개 프로필 없음. 비공개·비활성 여부는 별도로 노출하지 않음

중복 등록과 없는 관계 취소는 오류가 아니라 200 멱등 응답이다.

## 알림과 감사 로그

- 실제 신규 팔로우에만 대상 사용자에게 SOCIAL 알림이 발송된다.
- 중복 POST에서는 알림이 재발송되지 않는다.
- 취소 시 알림을 보내거나 기존 알림을 삭제하지 않는다.
- 실제 생성/삭제에만 감사 로그가 기록된다.

## 검증 상태와 후속 범위

- Service 단위 테스트 12개 및 전체 Maven 테스트 51개 통과.
- 실제 MySQL에서 모든 FollowMapper SQL을 트랜잭션으로 실행하고 롤백했다.
- 2026-06-19 재확인 결과 `Slate/.env`의 `SLATE_DB_PASSWORD`로 `slate_app` DB 인증에 성공했다. 기존 비밀번호 불일치 기록은 현재 차단 요소가 아니다.
- 이 문서 작성 당시에는 팔로우 버튼, 목록 화면, 활동 피드가 미구현이었다. 이후 버튼과 목록 화면은 구현됐고 활동 피드만 미구현이다.
