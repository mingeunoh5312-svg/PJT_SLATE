# 연관형 더미 데이터 blocker 수정 프롬프트

당신은 Slate 프로젝트의 연관형 더미 데이터 수정자입니다. 이미 생성된 더미 데이터 산출물에서 DB 적용 전 검토 중 발견된 blocker만 좁게 수정하세요. 새로운 대규모 시나리오를 추가하거나 실제 DB에 seed를 적용하지 마세요.

## 작업 위치

현재 프로젝트 루트:

`/Users/mingeunoh/Documents/PJT_0624/Project_Slate/Slate`

수정 대상:

- `sql/15_seed_connected_demo_data.sql`
- `sql/16_validate_connected_demo_data.sql`
- 필요 시 `docu/dummy_data/data_scenarios.md`
- 필요 시 `docu/dummy_data/expected_changes.md`

참조 대상:

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/prompt/connected_dummy_data_creator_prompt.md`
- `backend/src/main/java/com/slate/boards/BoardService.java`
- `backend/src/main/resources/mappers/BoardMapper.xml`
- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`

## 현재 상태

생성자는 연관형 더미 데이터 초안을 만들었습니다. 아직 실제 DB에는 적용하지 않은 상태여야 합니다.

생성된 주요 파일:

- `sql/15_seed_connected_demo_data.sql`
- `sql/16_validate_connected_demo_data.sql`
- `sql/17_rollback_connected_demo_data.sql`
- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/expected_changes.md`

검토 결과, 스키마 컬럼 수준의 큰 문제는 보이지 않았지만, 팀 작업물 승인 요청 데이터에 서비스 규칙을 위반하는 관계가 있습니다.

## 반드시 지켜야 할 제한

- 실제 DB에 `sql/15_seed_connected_demo_data.sql`을 적용하지 마세요.
- dump를 생성하지 마세요.
- `sql/99_reset.sql`을 실행하거나 참조 실행하지 마세요.
- 새 DB를 만들지 마세요.
- `DROP`, `TRUNCATE`, `FOREIGN_KEY_CHECKS = 0`를 사용하지 마세요.
- CDD 외 기존 데이터, 기존 샘플 계정, 사용자 생성 계정, 크롤링 공모전 데이터를 수정하지 마세요.
- 프런트엔드/백엔드 Java 코드는 이번 작업에서 수정하지 마세요. 서비스 규칙 확인을 위해 읽기만 하세요.
- 문제가 된 blocker와 그 검증 보강만 수정하세요.

## 발견된 blocker

`team_work_approval_request`에 들어가는 일부 requester가 해당 팀의 `ACTIVE` 멤버가 아닙니다.

문제 행:

1. `[CDD] 한강 현장음 믹스 승인 대기`
   - 현재 requester: `cdd-sound`
   - 문제: `cdd-sound`는 `[CDD] 한강 야간 단편팀`의 초대 `PENDING` 대상자이며 아직 active team member가 아닙니다.

2. `[CDD] 미완성 러프컷 반려`
   - 현재 requester: `cdd-writer`
   - 문제: `cdd-writer`는 `[CDD] 한강 야간 단편팀`의 지원 `PENDING` 사용자이며 아직 active team member가 아닙니다.

서비스 규칙:

- `BoardService.createTeamWorkRequest()`는 `assertActiveTeamMember(userId, teamId)`를 호출합니다.
- 따라서 팀 작업물 승인 요청자는 해당 팀의 `team_member.status = 'ACTIVE'` 멤버여야 합니다.
- DB seed가 서비스 레이어를 우회하더라도, 데모 데이터는 실제 서비스 규칙과 맞아야 합니다.

현재 `[CDD] 한강 야간 단편팀`의 active 멤버:

- `cdd-leader`
- `cdd-actor`
- `cdd-camera`
- `cdd-editor`

## 수정 요구사항

### 1. 승인 요청 requester 수정

`sql/15_seed_connected_demo_data.sql`에서 아래 두 승인 요청의 requester를 `[CDD] 한강 야간 단편팀` active 멤버 중 자연스러운 사용자로 변경하세요.

수정 대상:

- `[CDD] 한강 현장음 믹스 승인 대기`
- `[CDD] 미완성 러프컷 반려`

권장 방향:

- `cdd-sound`와 `cdd-writer`를 active team member로 바꾸지 마세요.
- 이 둘은 각각 `PENDING` 초대/지원 시나리오를 유지해야 합니다.
- 대신 requester를 이미 active 멤버인 `cdd-camera`, `cdd-editor`, `cdd-actor` 중 하나로 바꾸세요.
- 텍스트가 requester와 어색해지면 title/content/body/document scenario 문구를 최소 범위로 조정하세요.

예시:

- 현장음 믹스 승인 대기 요청자를 `cdd-camera` 또는 `cdd-editor`로 변경
- 미완성 러프컷 반려 요청자를 `cdd-actor` 또는 `cdd-editor`로 변경

단, 최종 선택은 전체 시나리오가 자연스럽고 관계가 깨지지 않는 쪽으로 하세요.

### 2. 기존 pending 시나리오 유지

아래 관계는 유지해야 합니다.

- `cdd-sound`: `[CDD] 한강 야간 단편팀` 동시녹음 슬롯의 초대 `PENDING`
- `cdd-writer`: `[CDD] 한강 야간 단편팀` 시나리오 작가 슬롯의 지원 `PENDING`
- `cdd-camera`: 수락된 지원자이며 active team member
- `cdd-editor`: 수락된 초대자이며 active team member
- `cdd-actor`: active team member

수정 후에도 아래 검증 의도가 깨지면 안 됩니다.

- 지원 상태: `ACCEPTED`, `PENDING`, `REJECTED`, `CANCELED`, `EXPIRED`
- 초대 상태: `ACCEPTED`, `PENDING`, `CANCELED`, `EXPIRED`
- 팀 작업물 승인 상태: `APPROVED`, `PENDING`, `REJECTED`, `CANCELED`

### 3. validation SQL 보강

`sql/16_validate_connected_demo_data.sql`의 `CDD_ZERO_ERROR_CHECKS` 결과에 아래 검증을 반드시 추가하세요.

check name:

`team_work_request_requester_not_active_member`

기대값:

`0`

검증 의미:

`[CDD]%` 팀 작업물 승인 요청의 requester가 해당 team의 `ACTIVE` team_member가 아닌 경우를 카운트합니다.

권장 SQL 형태:

```sql
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'team_work_request_requester_not_active_member', COUNT(*)
FROM team_work_approval_request twr
LEFT JOIN team_member tm
  ON tm.team_id = twr.team_id
 AND tm.user_id = twr.requester_user_id
 AND tm.status = 'ACTIVE'
WHERE twr.team_id IN (@cdd_river_team, @cdd_closed_team)
  AND twr.title LIKE '[CDD]%'
  AND tm.team_member_id IS NULL
```

실제 위치는 기존 `CDD_ZERO_ERROR_CHECKS` `UNION ALL` 블록 안에서 문법이 깨지지 않게 넣으세요.

### 4. 문서 동기화

requester 변경으로 설명 문구가 달라지면 다음 문서를 함께 업데이트하세요.

- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/expected_changes.md`

단순 requester 변경만으로 INSERT 건수가 바뀌지 않는다면 예상 INSERT 총량을 바꾸지 마세요. 건수가 바뀌는 수정을 선택했다면 반드시 예상 건수도 같이 맞추세요.

## 검증

수정 후 아래를 실행하거나, 실행이 불가능하면 이유를 명시하세요.

### 필수 정적 검증

```bash
git diff --check -- sql/15_seed_connected_demo_data.sql sql/16_validate_connected_demo_data.sql sql/17_rollback_connected_demo_data.sql docu/dummy_data/data_scenarios.md docu/dummy_data/expected_changes.md
```

다음 검색에서 금지 작업이 없어야 합니다.

```bash
rg -n "FOREIGN_KEY_CHECKS|DROP\\s+|TRUNCATE|99_reset|CREATE\\s+DATABASE|ALTER\\s+TABLE|LOAD DATA|INTO OUTFILE|GRANT\\s|REVOKE\\s" sql/15_seed_connected_demo_data.sql sql/16_validate_connected_demo_data.sql sql/17_rollback_connected_demo_data.sql
```

### 권장 정적 검증

수정된 requester가 모두 active team member인지 SQL 텍스트 기준으로 다시 확인하세요.

확인 기준:

- `[CDD] 한강 야간 단편팀` active 멤버는 `team_member` insert/update 기준으로 확인합니다.
- `team_work_approval_request`의 requester가 그 목록에 포함되어야 합니다.

### DB 확인은 읽기 전용만 허용

실제 DB에 접속할 수 있다면 아래처럼 읽기 전용 확인만 하세요.

- CDD 데이터가 아직 적용되지 않았는지 확인
- reference 데이터가 존재하는지 확인

seed 적용, rollback 실행, dump 생성은 이번 수정자 작업 범위가 아닙니다.

## 완료 보고 형식

완료 시 아래 형식으로 보고하세요.

```md
## 수정 완료 보고

### 수정 파일
- ...

### blocker 수정 내용
- `[CDD] 한강 현장음 믹스 승인 대기`: requester `기존` → `변경`
- `[CDD] 미완성 러프컷 반려`: requester `기존` → `변경`

### 유지한 시나리오
- `cdd-sound` PENDING 초대 유지 여부:
- `cdd-writer` PENDING 지원 유지 여부:
- 지원/초대/승인 상태 다양성 유지 여부:

### validation 보강
- `team_work_request_requester_not_active_member` 추가 여부:
- 기대값:

### 실행한 검증
- `git diff --check`:
- 금지어 검색:
- 기타:

### 미실행/남은 리스크
- ...
```

## 완료 조건

- 문제 requester가 active team member로 변경됨
- `cdd-sound`, `cdd-writer`의 pending 초대/지원 시나리오가 유지됨
- validate SQL에 `team_work_request_requester_not_active_member` 검증이 추가됨
- 문서와 SQL 설명이 서로 어긋나지 않음
- 실제 DB에는 아무 변경도 적용하지 않음
