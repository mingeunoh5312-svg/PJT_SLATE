# 2차 볼륨 더미 데이터 전달 및 복구 가이드

작성일: 2026-06-25
대상 DB: `slate`

## Git 전달 정책

`dev_Slate_0625_default_dummies` 브랜치에는 전체 DB dump를 포함하지 않는다. 공동작업자는 CDD 적용·검증 후 CDV seed를 실행해 볼륨 데이터를 재현한다.

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/18_seed_connected_demo_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/21_seed_connected_demo_volume_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/27_apply_generated_dummy_images.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/22_validate_connected_demo_volume_data.sql
```

dump에는 기존 계정 데이터와 비밀번호 hash가 포함되므로 `database_delivery`는 Git 제외 대상이며, 아래 dump 복구 절차는 별도 보안 전달을 받은 경우에만 사용한다.

## 전달 파일

| 구분 | 경로 |
|---|---|
| 적용 전 전체 dump | `database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql` |
| 적용 전 checksum | `database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql.sha256` |
| 적용 후 전체 dump | `database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql` |
| 적용 후 checksum | `database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql.sha256` |
| CDV seed | `sql/21_seed_connected_demo_volume_data.sql` |
| generated image apply | `sql/27_apply_generated_dummy_images.sql` |
| CDV validation | `sql/22_validate_connected_demo_volume_data.sql` |
| CDV rollback | `sql/23_rollback_connected_demo_volume_data.sql` |
| 적용 결과 | `docu/dummy_data/volume_validation_result.md` |
| 테스트 계정 | `docu/dummy_data/volume_test_accounts.md` |

Git 브랜치에 포함되는 파일은 seed, 검증, rollback SQL과 문서다. `database_delivery` 파일은 로컬 비공개 전달물이다.

## checksum 확인

```bash
shasum -a 256 -c database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql.sha256
shasum -a 256 -c database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql.sha256
```

두 결과가 모두 `OK`여야 한다.

## 공동작업자 DB를 적용 후 상태로 맞추기

주의:

- 로컬 또는 폐기 가능한 개발 DB에서만 실행한다.
- 전체 dump 복구는 현재 `slate` DB를 dump 시점 상태로 맞춘다.
- 공동작업자의 현재 데이터가 필요하면 복구 전에 별도 백업한다.
- dump에는 기존 계정 데이터와 비밀번호 hash가 포함되므로 안전한 경로로만 전달한다.

```bash
mysql --login-path=slate-admin < database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql
mysql --login-path=slate-admin --batch --raw slate < sql/22_validate_connected_demo_volume_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
```

성공 기준:

- CDV 예상 count 38개 항목 전부 일치
- CDV zero-error 39개 항목 전부 0
- CDD 예상 count 전부 일치
- CDD zero-error 전부 0

## 적용 직전 상태로 전체 복구하기

이 dump는 기존 CDD는 유지하고 CDV만 적용되기 직전의 전체 DB 상태다.

```bash
mysql --login-path=slate-admin < database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
```

전체 복구는 dump 이후 추가된 다른 데이터도 제거할 수 있으므로 실행 전에 현재 DB를 별도 백업한다.

## CDV namespace만 제거하기

전체 DB를 되돌리지 않고 CDV 데이터만 제거할 때 사용한다.

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/23_rollback_connected_demo_volume_data.sql
```

rollback 후 확인:

```bash
mysql --login-path=slate-admin --batch --raw slate -e "
SELECT COUNT(*) AS cdv_accounts FROM user_account WHERE login_id LIKE 'cdv-%';
SELECT COUNT(*) AS cdv_teams FROM team WHERE name LIKE '[CDV]%';
SELECT COUNT(*) AS cdv_posts FROM board_post WHERE title LIKE '[CDV]%';
SELECT COUNT(*) AS cdv_contests FROM contest WHERE title LIKE '[CDV]%';
"
```

각 결과가 0이어야 한다. rollback 후 기존 CDD 검증도 다시 실행한다.

## 금지 및 주의

- 운영 DB에 전체 dump를 바로 복구하지 않는다.
- `sql/99_reset.sql`을 실행하지 않는다.
- 검증 실패 상태를 성공으로 간주하지 않는다.
- 전체 dump 복구와 CDV 전용 rollback의 영향을 혼동하지 않는다.
- `database_delivery` dump 파일을 공개 저장소에 커밋하지 않는다.
