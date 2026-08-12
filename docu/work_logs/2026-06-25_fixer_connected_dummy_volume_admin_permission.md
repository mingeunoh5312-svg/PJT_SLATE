# 2차 볼륨 더미 데이터 관리자 권한 blocker 수정 작업 로그

작성일: 2026-06-25
역할: fixer

## 작업 범위

- CDV 관리자 권한 seed 범위를 백엔드 카탈로그와 일치시킴
- 관리자 권한 validation과 관련 문서 정합성 보강
- 실제 DB 적용, rollback, dump, 애플리케이션 코드 수정 제외

## 발견된 blocker

현재 DB에는 활성 `ADMIN_PERMISSION` 코드가 9개지만, 백엔드 `AdminPermissionCatalog.CODES`가 인정하는 권한은 8개다.

기존 seed는 모든 활성 권한을 선택하므로 `DEMO_ACCESS_MANAGE`까지 삽입되어 `admin_permission` 예상 8건과 전체 INSERT 예상 2,621건이 각각 9건과 2,622건으로 달라질 수 있었다.

## 참조 경로

- `Agent.md`
- `docu/prompt/connected_dummy_volume_data_creator_prompt.md`
- `docu/prompt/connected_dummy_volume_data_fixer_prompt.md`
- `docu/dummy_data/volume_data_scenarios.md`
- `docu/dummy_data/volume_expected_changes.md`
- `docu/work_logs/2026-06-25_creator_connected_dummy_volume_data_draft.md`
- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`
- `sql/18_seed_connected_demo_volume_data.sql`
- `sql/19_validate_connected_demo_volume_data.sql`
- `sql/20_rollback_connected_demo_volume_data.sql`
- `backend/src/main/java/com/slate/admin/AdminPermissionCatalog.java`

## 수정한 파일

- `sql/18_seed_connected_demo_volume_data.sql`
- `sql/19_validate_connected_demo_volume_data.sql`
- `docu/dummy_data/volume_data_scenarios.md`
- `docu/dummy_data/volume_expected_changes.md`
- `docu/work_logs/2026-06-25_fixer_connected_dummy_volume_admin_permission.md`

`sql/20_rollback_connected_demo_volume_data.sql`은 기존 CDV 관리자 권한 전체 제거 범위가 적절해 수정하지 않았다.

## 수정 내용

`cdv-admin`에게 다음 백엔드 카탈로그 권한 8개만 삽입하도록 seed 조건을 제한했다.

- `COMPANY_APPROVAL`
- `USER_SANCTION`
- `CONTENT_MODERATION`
- `SCORE_POLICY`
- `CONTEST_MANAGE`
- `NOTIFICATION_SEND`
- `LOG_VIEW`
- `ADMIN_PERMISSION_MANAGE`

`DEMO_ACCESS_MANAGE`는 DB에서 삭제하거나 비활성화하지 않고 CDV seed 대상에서만 제외했다.

validation의 `CDV_ZERO_ERROR_CHECKS`에는 다음 검사를 추가했다.

- `cdv_admin_non_catalog_permission`
- `cdv_admin_missing_catalog_permission`
- `cdv_admin_inactive_permission`

예상값은 `admin_permission` 8건과 전체 INSERT 2,621건으로 유지했다.

## 실행한 검증 명령과 결과

수정 후 다음 정적 검증을 실행했다.

```bash
git diff --check -- \
  sql/18_seed_connected_demo_volume_data.sql \
  sql/19_validate_connected_demo_volume_data.sql \
  sql/20_rollback_connected_demo_volume_data.sql \
  docu/dummy_data/volume_data_scenarios.md \
  docu/dummy_data/volume_expected_changes.md \
  docu/work_logs/2026-06-25_fixer_connected_dummy_volume_admin_permission.md
```

결과: 출력 없음, 통과.

대상 파일이 Git 미추적 상태이므로 각 파일에 `git diff --no-index --check /dev/null <file>`과 후행 공백 검색도 추가 실행했다. whitespace 오류는 없었다.

```bash
rg -n "FOREIGN_KEY_CHECKS|DROP\\s+|TRUNCATE|99_reset|CREATE\\s+DATABASE|LOAD DATA|INTO OUTFILE|GRANT\\s|REVOKE\\s" \
  sql/18_seed_connected_demo_volume_data.sql \
  sql/19_validate_connected_demo_volume_data.sql \
  sql/20_rollback_connected_demo_volume_data.sql
```

결과: 일치 항목 없음, 통과.

Node 기반 정적 검사를 추가로 실행했다.

- seed: 142 statements, 따옴표·괄호 균형 통과
- validation: 6 statements, 따옴표·괄호 균형 통과
- rollback: 45 statements, 따옴표·괄호 균형 통과
- 백엔드 카탈로그, seed, validation의 권한 8개 집합 일치
- seed의 `DEMO_ACCESS_MANAGE` 제외 확인
- validation의 `admin_permission` 기대값 8 확인
- `cdv_admin_non_catalog_permission` 존재 확인
- 예상 INSERT 표 38개 테이블 합계 2,621건 확인

## 실제 DB 미적용 확인

- `sql/18_seed_connected_demo_volume_data.sql` 미실행
- `sql/19_validate_connected_demo_volume_data.sql`의 실제 DB 조회 미실행
- `sql/20_rollback_connected_demo_volume_data.sql` 미실행
- dump, checksum, 새 DB 생성 미수행
- DB 연결 명령 자체를 실행하지 않아 실제 데이터 변경 없음

## 남은 이슈

- 실제 seed 적용과 validation은 사용자 승인 및 백업 이후 별도 단계에서 수행해야 한다.
- 실제 적용 전 현재 DB의 관리자 권한 코드 상태를 읽기 전용으로 다시 확인할 수 있다.
