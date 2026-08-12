# 사용자 팔로우 백엔드 구현 프롬프트

## 사용 목적

`user_follow` DB 스키마를 기반으로 사용자 간 단방향 팔로우 등록, 취소, 상태 및 목록 조회 API를 구현한다.
이번 작업은 팔로우 기능의 백엔드 기반까지만 완성하며 프런트엔드와 활동 피드는 후속 단계로 분리한다.

## 프롬프트

```text
당신은 Slate 프로젝트의 사용자 팔로우 백엔드 구현 담당자입니다.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

## 필수 참조 순서

1. Agent.md
2. docu/README.md
3. docu/00_common/reference_policy.md
4. docu/03_mvp_scope/mvp_scope.md
5. docu/05_backend/backend_baseline.md
6. ../prototype_3/docu/07_Prompt/Follow/01_follow_db_schema_prompt.md
7. sql/01_schema.sql
8. sql/06_follow_schema.sql
9. backend/src/main/java/com/slate/profiles/ProfileController.java
10. backend/src/main/java/com/slate/profiles/ProfileService.java
11. backend/src/main/java/com/slate/notifications/NotificationService.java
12. backend/src/main/java/com/slate/operations/AuditLogService.java
13. backend/src/main/java/com/slate/security/SecurityConfig.java
14. backend/src/main/java/com/slate/common/GlobalExceptionHandler.java

외부 `prototype_3` 문서는 사용자가 명시적으로 지정한 DB 설계 참고 자료이므로 읽기 전용으로만 사용합니다.
실제 구현 기준은 항상 현재 `Slate` 내부의 SQL과 Java 코드입니다.

## 확인된 DB 구조

현재 다음 SQL 반영이 완료되어 있습니다.

- `sql/01_schema.sql`: `user_follow` 신규 생성 DDL
- `sql/06_follow_schema.sql`: 기존 DB 적용 DDL
- `sql/99_reset.sql`: `user_follow` 삭제 순서

테이블 구조:

- `follower_user_id bigint NOT NULL`
- `following_user_id bigint NOT NULL`
- `created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP`
- 복합 PK: `(follower_user_id, following_user_id)`
- 두 컬럼 모두 `user_account.user_id` 외래키
- 자기 자신 팔로우 방지 CHECK
- 팔로워/팔로잉 최신순 조회 인덱스

Java 구현 전에 실제 SQL이 위 내용과 일치하는지 다시 확인하세요.
불일치가 있다면 임의로 SQL을 변경하지 말고 현재 SQL을 기준으로 Java 코드를 맞추고 차이를 결과에 보고하세요.

## 목표

로그인한 일반 사용자(USER)가 공개된 활성 사용자 프로필을 팔로우하거나 취소하고,
특정 프로필의 팔로우 상태, 팔로워 목록, 팔로잉 목록을 조회할 수 있게 합니다.

정책:

- 승인 없는 단방향 팔로우입니다.
- 자기 자신을 팔로우할 수 없습니다.
- 중복 팔로우 요청은 오류 대신 현재 상태를 반환하는 멱등 동작으로 처리합니다.
- 이미 취소된 관계의 재취소도 오류 대신 현재 상태를 반환합니다.
- 팔로우 대상은 ACTIVE 상태의 USER 계정이면서 ACTIVE/PUBLIC 프로필이어야 합니다.
- 팔로우 실행자도 ACTIVE 상태의 USER 계정과 ACTIVE 프로필을 가져야 합니다.
- 프로필이 비공개 또는 비활성으로 바뀌어도 기존 관계를 자동 삭제하지 않습니다.
- 목록과 카운트에서는 현재 공개 가능한 ACTIVE/PUBLIC 프로필만 노출합니다.
- 목록 응답에 이메일, 전화번호, 로그인 ID 같은 개인정보를 포함하지 않습니다.

## 구현 파일

전용 패키지 `com.slate.follows`를 만들고 다음 파일을 구현하세요.

- `backend/src/main/java/com/slate/follows/FollowController.java`
- `backend/src/main/java/com/slate/follows/FollowService.java`
- `backend/src/main/java/com/slate/follows/FollowMapper.java`
- `backend/src/main/resources/mappers/FollowMapper.xml`
- `backend/src/test/java/com/slate/follows/FollowServiceTest.java`
- 필요한 경우 `backend/src/test/java/com/slate/follows/FollowControllerTest.java`

기존 Profile, Account Mapper에 팔로우 쿼리를 섞지 말고 팔로우 도메인 전용 Mapper를 사용하세요.
프로젝트의 기존 `Map<String, Object>` 응답 스타일과 `ApiResponse` 형식을 우선 따르세요.
불필요한 DTO 계층이나 새 라이브러리는 추가하지 마세요.

## API 계약

프로필 화면의 식별자가 `profileId`이므로 URL 경로 변수는 사용자 ID가 아니라 프로필 ID를 사용합니다.
Service에서 해당 프로필의 `userId`를 조회하여 `user_follow` 테이블에 저장하세요.

### 1. 팔로우 등록

POST /api/profiles/{profileId}/follow

- 인증 필수
- 요청 body 없음
- `profileId`는 팔로우 대상 프로필 ID
- INSERT IGNORE 또는 동등한 원자적 방식으로 중복 경쟁을 안전하게 처리
- 실제 신규 관계가 생성된 경우에만 알림과 감사 로그 기록

응답 data 예시:

{
  "profileId": 12,
  "userId": 35,
  "following": true,
  "changed": true,
  "followerCount": 8,
  "followingCount": 3
}

이미 팔로우 중이면 `following=true`, `changed=false`를 반환합니다.

### 2. 팔로우 취소

DELETE /api/profiles/{profileId}/follow

- 인증 필수
- 해당 로그인 사용자의 관계만 삭제
- 실제 관계가 삭제된 경우에만 감사 로그 기록
- 알림 이력은 삭제하지 않음

응답은 등록 API와 같은 필드를 사용하고 다음 상태를 반환합니다.

- `following=false`
- 실제 삭제 시 `changed=true`
- 기존 관계가 없으면 `changed=false`

### 3. 팔로우 상태와 수 조회

GET /api/profiles/{profileId}/follow-status

- 현재 SecurityConfig 정책에 따라 인증 필수 상태를 유지
- 대상 프로필 존재 및 공개 가능 여부 검증

응답 data 예시:

{
  "profileId": 12,
  "userId": 35,
  "following": true,
  "ownProfile": false,
  "followerCount": 8,
  "followingCount": 3
}

자기 프로필이면 `ownProfile=true`, `following=false`로 반환합니다.

### 4. 팔로워 목록

GET /api/profiles/{profileId}/followers?limit=20&offset=0

### 5. 팔로잉 목록

GET /api/profiles/{profileId}/following?limit=20&offset=0

목록 API 공통 정책:

- 인증 필수
- 기본 limit 20, 최소 1, 최대 50
- offset 기본 0, 음수는 0으로 정규화
- `created_at DESC` 다음 사용자 ID DESC로 안정 정렬
- 대상 프로필이 ACTIVE/PUBLIC인지 확인
- 목록 항목도 ACTIVE USER + ACTIVE/PUBLIC 프로필만 반환
- N+1 쿼리 없이 한 번의 목록 쿼리로 필요한 정보를 조회

응답 data 예시:

{
  "items": [
    {
      "profileId": 7,
      "userId": 18,
      "nickname": "영화인",
      "displayName": "영화인",
      "shortIntro": "촬영과 조명을 맡고 있습니다.",
      "publicRegionName": "서울",
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

프로필 이미지 컬럼은 현재 스키마에 없으므로 응답에 가짜 이미지 URL을 추가하지 마세요.
역할 정보는 목록 쿼리를 복잡하게 만들거나 N+1을 발생시키지 않는 범위에서만 포함하고,
필수가 아니라면 이번 단계에서 제외하세요.

## Mapper 구현 요구사항

최소한 다음 동작을 제공하세요. 메서드명은 프로젝트 스타일에 맞게 조정할 수 있습니다.

- 팔로우 실행자 계정과 활성 프로필 조회
- 대상 `profileId`로 계정/프로필 조회
- 관계 존재 여부 조회
- `INSERT IGNORE` 기반 관계 생성
- follower/following 조건의 관계 삭제
- 공개 가능한 팔로워 수 조회
- 공개 가능한 팔로잉 수 조회
- 팔로워 목록 조회
- 팔로잉 목록 조회

프로필 검증 조회에는 다음 조건을 고려하세요.

- `user_account.account_type = 'USER'`
- `user_account.account_status = 'ACTIVE'`
- `member_profile.status = 'ACTIVE'`
- 대상 및 목록 공개 조건: `member_profile.visibility = 'PUBLIC'`

목록의 `followingByCurrentUser`는 현재 로그인 사용자와 항목 사용자의 관계를 EXISTS로 계산하세요.
이메일, 전화번호, 로그인 ID는 SELECT하지 마세요.

## Service 구현 요구사항

- 등록과 취소는 `@Transactional`로 처리합니다.
- null 인증 사용자를 가정해 NullPointerException을 내지 말고 필요한 경우 UNAUTHORIZED를 사용합니다.
- 존재하지 않거나 공개할 수 없는 대상 프로필은 NOT_FOUND로 처리하여 비공개 프로필 존재 여부를 과도하게 노출하지 않습니다.
- 일반 USER가 아니거나 활성 프로필이 없는 실행자는 FORBIDDEN 또는 프로젝트 정책에 맞는 명확한 오류로 처리합니다.
- 자기 자신 팔로우는 BAD_REQUEST로 처리합니다.
- 중복 등록과 없는 관계 취소는 멱등 처리합니다.
- DB 기본키를 최종 중복 방어선으로 사용하며, 사전 exists 조회만으로 동시성을 보장한다고 가정하지 마세요.
- `followerCount`, `followingCount`, `hasMore`는 실제 쿼리 결과로 계산합니다.

## 알림 연동

신규 팔로우가 실제로 생성된 경우에만 기존 `NotificationService.send(...)`를 재사용하여
팔로우 대상 사용자에게 알림을 보냅니다.

권장 값:

- recipientUserId: 팔로우 대상 userId
- senderUserId: 현재 로그인 userId
- notificationType: `SOCIAL`
- title: `새 팔로워가 생겼습니다.`
- body: `{실행자 표시 이름}님이 회원님을 팔로우하기 시작했습니다.`
- targetType: `PROFILE`
- targetId: 실행자의 profileId

알림 본문에 이메일 등 개인정보를 넣지 마세요.
중복 POST에서는 알림을 다시 생성하지 마세요.
팔로우 취소 시 알림을 발송하거나 과거 알림을 삭제하지 마세요.

## 감사 로그

기존 `AuditLogService.recordAudit(...)` 패턴을 재사용하세요.

- 신규 생성: `USER_FOLLOW_CREATED`
- 실제 취소: `USER_FOLLOW_DELETED`
- targetType: `PROFILE`
- targetId: 대상 profileId

실제 상태가 바뀐 경우에만 기록하고, 로그 payload에 개인정보를 포함하지 마세요.

## Controller 구현 요구사항

- `@RestController`와 `ApiResponse`를 사용합니다.
- 현재 사용자 ID는 요청 body나 query parameter로 받지 않고 반드시 `CurrentUser`에서 가져옵니다.
- 등록, 취소, 상태, 목록 endpoint를 위 API 계약과 일치시킵니다.
- SecurityConfig의 `.anyRequest().authenticated()`로 보호되므로 불필요한 permitAll 규칙을 추가하지 마세요.
- 프런트엔드 요구를 추측하여 API를 추가 확장하지 마세요.

## 오류 상태 기준

- 400 BAD_REQUEST: 자기 자신 팔로우, 잘못된 limit/offset을 정규화할 수 없는 경우
- 401 UNAUTHORIZED: 인증 정보 없음
- 403 FORBIDDEN: 팔로우 기능을 사용할 수 없는 계정 또는 활성 프로필 없음
- 404 NOT_FOUND: 대상 공개 프로필 없음
- 500을 유발할 수 있는 DB 제약 예외는 가능한 범위에서 도메인 오류 또는 멱등 응답으로 정리

기존 `SlateException`과 `GlobalExceptionHandler`를 재사용하고, 이번 기능만을 위해 전역 예외 정책을 크게 변경하지 마세요.

## 테스트

최소한 `FollowServiceTest`에서 다음을 검증하세요.

1. 정상 팔로우 생성
2. 신규 생성 시 알림과 감사 로그가 한 번만 호출됨
3. 중복 팔로우 요청은 `changed=false`이고 알림이 재발송되지 않음
4. 정상 팔로우 취소
5. 없는 관계 취소는 `changed=false`
6. 자기 자신 팔로우 거부
7. 존재하지 않는 대상 프로필 거부
8. 비공개, 비활성 프로필 거부
9. USER가 아니거나 활성 프로필이 없는 실행자 거부
10. 팔로우 상태의 ownProfile 처리
11. 팔로워 목록 pagination 정규화와 hasMore 계산
12. 팔로잉 목록이 개인정보를 포함하지 않는 응답 구조인지 확인

가능하면 Controller 테스트에서 다음도 확인하세요.

- URL과 HTTP method가 계약과 일치함
- CurrentUser의 userId가 Service로 전달됨
- 응답이 `ApiResponse` 구조를 사용함

실제 MySQL 통합 테스트 환경이 없다면 이를 숨기지 말고 Service 단위 테스트와 MyBatis XML 정적 검토 결과를 보고하세요.

## 이번 단계에서 하지 말아야 할 일

- DB 스키마 또는 seed 수정
- 프런트엔드 `api.js` 수정
- 정적 팔로우 버튼 연결
- 팔로워/팔로잉 화면 구현
- 홈 화면 수정
- 팔로우 활동 피드 API 구현
- 팔로우 추천 알고리즘 구현
- 차단, 음소거, 비공개 팔로우 승인 기능 추가
- 팔로워/팔로잉 수 캐시 컬럼 추가
- 기존 matching bookmark나 board like 동작 변경
- SecurityConfig에서 팔로우 endpoint를 공개 endpoint로 변경

## 작업 기록

작업 시작 시 다음 로그를 생성하세요.

- `docu/work_logs/YYYY-MM-DD_backend_follow.md`

다음 프런트엔드 작업자에게 전달할 계약이 있다면 다음 문서를 작성하세요.

- `docu/handoff/backend_to_frontend_follow.md`

로그와 handoff에는 구현되지 않은 활동 피드를 구현 완료로 기록하지 마세요.

## 검증 명령

backend 디렉터리에서 다음을 실행하세요.

./mvnw test

프로젝트에 Maven Wrapper가 없으면 다음을 실행하세요.

mvn test

테스트 실패가 기존 실패인지 신규 실패인지 구분하고, 실패를 숨기지 마세요.

## 완료 조건

- 등록, 취소, 상태, 팔로워 목록, 팔로잉 목록 API가 계약대로 구현됨
- 모든 변경 API가 인증 사용자 ID만 사용함
- 자기 자신, 중복, 비공개/비활성 대상 정책이 구현됨
- 동시 중복 요청을 DB 복합 PK와 INSERT IGNORE로 안전하게 처리함
- 신규 팔로우에만 알림과 감사 로그가 생성됨
- 목록에 개인정보가 노출되지 않음
- pagination과 안정적인 정렬이 구현됨
- 신규 단위 테스트가 추가되고 `mvn test` 결과가 보고됨
- DB, 프런트엔드, 홈, 활동 피드는 수정하지 않음

## 결과 보고

작업 완료 후 다음을 간결하게 보고하세요.

- 생성하거나 수정한 파일
- 구현한 endpoint 목록
- 핵심 권한 및 공개 범위 정책
- 알림과 감사 로그 동작
- 실행한 테스트와 결과
- 실행하지 못한 검증과 이유
- 프런트엔드가 사용할 최종 요청/응답 계약
- 후속 단계로 남은 팔로우 UI와 활동 피드
```

## 참조 경로

- `Agent.md`
- `sql/01_schema.sql`
- `sql/06_follow_schema.sql`
- `backend/src/main/java/com/slate/profiles`
- `backend/src/main/java/com/slate/notifications`
- `backend/src/main/java/com/slate/operations`
- `../prototype_3/docu/07_Prompt/Follow/01_follow_db_schema_prompt.md`
