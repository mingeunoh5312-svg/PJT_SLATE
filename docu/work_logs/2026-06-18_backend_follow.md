# 2026-06-18 백엔드 작업 로그 - 사용자 팔로우

## 작업 범위

- `user_follow` 기반 팔로우 등록, 취소, 상태, 팔로워 목록, 팔로잉 목록 API 구현
- 신규 팔로우 알림과 상태 변경 감사 로그 연동
- Service 단위 테스트 및 전체 Maven 테스트
- DB, 시드, 프런트엔드, 활동 피드는 변경하지 않음

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/03_mvp_scope/mvp_scope.md`
- `docu/05_backend/backend_baseline.md`
- `docu/prompt/follow_backend_prompt.md`
- `../prototype_3/docu/07_Prompt/Follow/01_follow_db_schema_prompt.md` (읽기 전용)
- `sql/01_schema.sql`, `sql/06_follow_schema.sql`
- Profile, Notification, Audit, Security, 공통 예외 코드

## 작업 계획

| 순서 | 작업 | 상태 |
|---:|---|---|
| 1 | 현재 SQL 및 백엔드 패턴 확인 | DONE |
| 2 | Follow Controller/Service/Mapper/XML 구현 | DONE |
| 3 | Service 단위 테스트 추가 | DONE |
| 4 | MyBatis 정적 검토 및 `mvn test` | DONE |
| 5 | 프런트엔드 handoff와 결과 기록 | DONE |

## 변경 파일

- `backend/src/main/java/com/slate/follows/FollowController.java`
- `backend/src/main/java/com/slate/follows/FollowService.java`
- `backend/src/main/java/com/slate/follows/FollowMapper.java`
- `backend/src/main/resources/mappers/FollowMapper.xml`
- `backend/src/test/java/com/slate/follows/FollowServiceTest.java`
- `docu/work_logs/2026-06-18_backend_follow.md`
- `docu/handoff/backend_to_frontend_follow.md`

## 구현 결과

- `POST /api/profiles/{profileId}/follow`
- `DELETE /api/profiles/{profileId}/follow`
- `GET /api/profiles/{profileId}/follow-status`
- `GET /api/profiles/{profileId}/followers`
- `GET /api/profiles/{profileId}/following`
- 활성 USER 계정과 활성 프로필을 실행자로 제한했다.
- 대상 및 목록 항목은 ACTIVE USER + ACTIVE/PUBLIC 프로필로 제한했다.
- `INSERT IGNORE`와 복합 PK로 중복 요청을 원자적·멱등 처리했다.
- 신규 생성에만 SOCIAL 알림과 `USER_FOLLOW_CREATED` 감사를 기록한다.
- 실제 삭제에만 `USER_FOLLOW_DELETED` 감사를 기록하며 취소 알림은 보내지 않는다.
- 목록은 JOIN과 EXISTS를 사용해 N+1 없이 조회하며 개인정보 컬럼은 SELECT하지 않는다.
- limit은 1~50, offset은 0 이상으로 정규화하고 안정 정렬 및 hasMore를 계산한다.

## 실행 명령 및 결과

- `mvn -Dtest=FollowServiceTest test`
  - 최초 Mockito 기반 테스트는 JDK 26의 inline agent self-attach 실패로 초기화 오류가 발생했다.
  - 프로젝트 기존 패턴에 맞춰 동적 Proxy/기록용 테스트 더블로 변경했다.
  - 재실행: 12 tests, failures 0, errors 0.
- `xmllint --noout src/main/resources/mappers/FollowMapper.xml`: PASS
- Mapper interface/XML statement ID 대응 검사: PASS
- FollowMapper XML 개인정보 컬럼 정적 검사: PASS
- `mvn test`: 51 tests, failures 0, errors 0, skipped 0.
- 별도 포트 18080 Spring Boot 기동: PASS
  - 애플리케이션 컨텍스트와 FollowMapper XML 로딩 성공.
  - HTTP API smoke는 현재 macOS `SLATE_DB_PASSWORD`와 `slate_app` 계정 비밀번호 불일치로 DB 연결 단계에서 중단했다.
- `slate-admin` login path를 사용한 실제 MySQL 트랜잭션 검증: PASS
  - 실행자/대상 조회, INSERT IGNORE 2회, 관계/양방향 공개 카운트, 팔로워/팔로잉 목록, 삭제 SQL 실행 성공.
  - 전체 검증은 ROLLBACK했고 최종 `user_follow` 0건을 확인했다.
- `git diff --check`: PASS

## 남은 이슈

- JWT를 포함한 실제 HTTP 등록/취소 E2E는 로컬 `slate_app` 자격 증명 불일치 해소 후 재검증해야 한다.
- 프런트엔드 팔로우 버튼과 팔로워/팔로잉 화면은 후속 단계다.
- 팔로우 기반 활동 피드는 이번 범위에 포함하지 않았다.
