# 저장한 팀 목록·취소 기능 수정

## 변경 파일

- `backend/src/main/java/com/slate/matching/MatchingController.java`
- `backend/src/main/java/com/slate/matching/MatchingService.java`
- `backend/src/main/java/com/slate/matching/MatchingMapper.java`
- `backend/src/main/resources/mappers/MatchingMapper.xml`
- `backend/src/test/java/com/slate/matching/MatchingServiceBookmarkTest.java`
- `backend/src/test/java/com/slate/matching/MatchingBookmarkMapperContractTest.java`
- `frontend/src/services/api.js`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`

## API 계약

- `GET /api/matching/bookmarks?targetType=TEAM`
  - 현재 인증 사용자의 `matching_bookmark.user_id`만 조회한다.
  - `bookmarkId`, `savedAt`, 팀 ID·이름·소개·상태·지역, 장르, 현재 OPEN 모집 역할을 반환한다.
  - OPEN 역할에는 정확한 `teamId`의 `recruitmentId`, `slotId`, 역할, 잔여 인원, 요구 조건과 일정을 포함한다.
- `DELETE /api/matching/bookmarks/TEAM/{teamId}`
  - `user_id`, `target_type`, `target_id`를 모두 조건으로 현재 사용자 소유 행만 삭제한다.
  - `removed`로 실제 삭제와 이미 취소된 상태를 구분한다.
- 기존 `POST /api/matching/bookmarks`
  - `INSERT IGNORE` 영향 행 수로 신규 저장과 중복 저장을 구분한다.
  - 신규는 `created=true`, 중복은 `alreadySaved=true`와 `이미 저장된 항목입니다.`를 반환한다.
  - 중복 저장에는 액션·감사 로그를 추가하지 않는다.
- 일반 팀 추천 응답에는 현재 사용자 기준 `savedByCurrentUser`를 추가했다.

## 프런트 동작

- `/matching/teams`에 `추천 팀` / `저장한 팀` 하위 탭을 추가했다.
- 저장 탭 URL은 `/matching/teams?view=saved`이며 이 화면에서만 전체 저장 목록 API를 호출한다.
- loading, API error, `저장한 팀이 없습니다.` 빈 상태를 분리했다.
- 저장 카드에 저장 시각, 실제 팀 상태, 지역, 장르, OPEN 역할과 잔여 인원을 표시한다.
- `상세 보기`는 카드 안에서 현재 OPEN 역할 상세를 펼친다.
- 지원 역할은 자동 선택하지 않는다. 사용자가 선택한 `openRoles` 항목의 `teamId`, `recruitmentId`, `slotId`만 지원 payload에 사용한다.
- 팀 종료·삭제·모집 종료·OPEN 역할 없음·역할 미선택 상태에서는 지원을 비활성화하고 이유를 표시한다.
- 저장 취소 성공 시 목록에서 즉시 제거하고 추천 결과의 저장 상태도 false로 갱신한다.
- 추천 카드와 상세의 저장 버튼은 `저장 ♡`와 `저장됨 ♥`을 구분한다.
- 기존 초대 코드에 남아 있던 임의 `recruitmentId = 1` fallback도 제거했다.

## 검증

- 관련 테스트: 8건 PASS
- 전체 backend `mvn test`: 60건 PASS, 실패·오류 0건
- frontend `npm run build`: PASS
- 실제 8081 API 스모크, 팀장 데모 계정:
  - TEAM 저장 목록 초기 0건 확인
  - 팀 #1 신규 저장: `created=true`, 목록 1건
  - 같은 팀 중복 저장: `created=false`, `alreadySaved=true`, 목록은 계속 1건
  - 저장 목록에서 실제 팀 상태·지역·장르 2개·OPEN 역할 3개 확인
  - 저장 취소: `removed=true`, 목록이 원래 0건으로 복원됨
- 추천 데이터가 0건인 현재 계정에서는 추천 카드의 저장 상태 실데이터를 확인하지 못했다.
- 데이터 변경을 유발하는 지원 mutation은 실행하지 않았고, 선택한 역할 객체에서 세 ID를 만드는 코드만 확인했다.
- 브라우저 자동화 연결이 실행 환경 메타데이터 오류로 시작되지 않아 desktop/390x844 overflow와 console 검증은 수행하지 못했다.
- SQL schema와 seed는 이번 작업에서 수정하지 않았다.

