# 연관형 더미 데이터 blocker 수정 작업 로그

## 작업 범위

- `docu/prompt/connected_dummy_data_fixer_prompt.md` 지시에 따라 CDD seed 적용 전 blocker만 좁게 수정했다.
- 실제 DB에는 `sql/15_seed_connected_demo_data.sql`을 적용하지 않았다.
- dump 생성, reset, rollback 실행, schema 변경, Java/프런트엔드 코드 수정은 하지 않았다.

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/prompt/connected_dummy_data_fixer_prompt.md`
- `docu/prompt/connected_dummy_data_creator_prompt.md`
- `backend/src/main/java/com/slate/boards/BoardService.java`
- `backend/src/main/resources/mappers/BoardMapper.xml`
- `sql/15_seed_connected_demo_data.sql`
- `sql/16_validate_connected_demo_data.sql`
- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/expected_changes.md`

## 수정 내용

- `[CDD] 한강 현장음 믹스 승인 대기` requester를 `cdd-sound`에서 `cdd-camera`로 변경했다.
- `[CDD] 미완성 러프컷 반려` requester를 `cdd-writer`에서 `cdd-editor`로 변경했다.
- requester 변경에 맞춰 두 승인 요청의 content 문구를 최소 범위로 조정했다.
- `sql/16_validate_connected_demo_data.sql`의 `CDD_ZERO_ERROR_CHECKS`에 `team_work_request_requester_not_active_member` 검증을 추가했다.
- `docu/dummy_data/data_scenarios.md`와 `docu/dummy_data/expected_changes.md`에 requester/검증 설명을 동기화했다.

## 유지한 시나리오

- `cdd-sound`는 `[CDD] 한강 야간 단편팀` 동시녹음 슬롯의 `PENDING` 초대 대상자로 유지했다.
- `cdd-writer`는 `[CDD] 한강 야간 단편팀` 시나리오 작가 슬롯의 `PENDING` 지원자로 유지했다.
- 지원 상태 `ACCEPTED`, `PENDING`, `REJECTED`, `CANCELED`, `EXPIRED`를 유지했다.
- 초대 상태 `ACCEPTED`, `PENDING`, `CANCELED`, `EXPIRED`를 유지했다.
- 팀 작업물 승인 상태 `APPROVED`, `PENDING`, `REJECTED`, `CANCELED`를 유지했다.

## 실행한 명령

```bash
sed -n '1,240p' docu/prompt/connected_dummy_data_fixer_prompt.md
git status --short
rg --files
rg -n "한강 현장음 믹스 승인 대기|미완성 러프컷 반려|team_work_approval_request|cdd-sound|cdd-writer|cdd-camera|cdd-editor|cdd-actor" sql/15_seed_connected_demo_data.sql
rg -n "CDD_ZERO_ERROR_CHECKS|team_work|requester|active" sql/16_validate_connected_demo_data.sql
rg -n "한강 현장음 믹스 승인 대기|미완성 러프컷 반려|cdd-sound|cdd-writer|cdd-camera|cdd-editor|cdd-actor|승인" docu/dummy_data/data_scenarios.md docu/dummy_data/expected_changes.md
rg -n "createTeamWorkRequest|assertActiveTeamMember|teamWork" backend/src/main/java/com/slate/boards/BoardService.java backend/src/main/resources/mappers/BoardMapper.xml
git diff --check -- sql/15_seed_connected_demo_data.sql sql/16_validate_connected_demo_data.sql sql/17_rollback_connected_demo_data.sql docu/dummy_data/data_scenarios.md docu/dummy_data/expected_changes.md
rg -n "FOREIGN_KEY_CHECKS|DROP\\s+|TRUNCATE|99_reset|CREATE\\s+DATABASE|ALTER\\s+TABLE|LOAD DATA|INTO OUTFILE|GRANT\\s|REVOKE\\s" sql/15_seed_connected_demo_data.sql sql/16_validate_connected_demo_data.sql sql/17_rollback_connected_demo_data.sql
rg -n "[ \\t]+$" sql/15_seed_connected_demo_data.sql sql/16_validate_connected_demo_data.sql sql/17_rollback_connected_demo_data.sql docu/dummy_data/data_scenarios.md docu/dummy_data/expected_changes.md
rg -n "@cdd_river_team, @cdd_camera_user, '\\[CDD\\] 한강 현장음 믹스 승인 대기'|@cdd_river_team, @cdd_editor_user, '\\[CDD\\] 미완성 러프컷 반려'|team_work_request_requester_not_active_member|@cdd_river_team, @cdd_river_recruitment, @slot_river_sound, @cdd_sound_user.*'PENDING'|@cdd_river_team, @cdd_river_recruitment, @slot_river_writer, @cdd_writer_user.*'PENDING'" sql/15_seed_connected_demo_data.sql sql/16_validate_connected_demo_data.sql
```

## 검증 결과

- `git diff --check`: 통과, 출력 없음.
- 금지어 검색: 매치 없음. `rg` exit code `1`은 검색 결과 없음으로 정상이다.
- trailing whitespace 검색: 매치 없음.
- SQL 텍스트 기준 확인:
  - `cdd-camera`, `cdd-editor`, `cdd-actor`는 `[CDD] 한강 야간 단편팀`의 `ACTIVE` team member로 유지됨.
  - 변경된 두 requester는 모두 `[CDD] 한강 야간 단편팀`의 `ACTIVE` team member임.
  - `cdd-sound`의 `PENDING` 초대와 `cdd-writer`의 `PENDING` 지원은 유지됨.

## 남은 이슈

- CDD seed 산출물은 현재 Git 기준 untracked 상태다.
- 이번 수정자 작업 범위상 실제 DB 적용, rollback 실행, dump 생성, DB mutation 검증은 수행하지 않았다.
