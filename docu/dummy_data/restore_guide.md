# 연관형 더미 데이터 전달/복구 가이드

작성일: 2026-06-25
대상 DB: `slate`

## Git 전달 정책

`dev_Slate_0625_default_dummies` 브랜치에는 전체 DB dump를 포함하지 않는다. dump에는 기존 계정 데이터와 비밀번호 hash, 감사·운영 로그가 포함되므로 `database_delivery`는 Git 제외 대상이다.

공동작업자는 기본적으로 아래 파일로 같은 CDD 더미 데이터를 재현한다.

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/18_seed_connected_demo_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/27_apply_generated_dummy_images.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
```

아래 전체 dump 절차는 신뢰할 수 있는 별도 보안 채널로 dump를 직접 전달받은 경우에만 사용한다.

## 전달 파일

| 구분 | 경로 |
|---|---|
| 적용 전 전체 dump | `database_delivery/2026-06-25/slate_before_connected_dummy.sql` |
| 적용 전 checksum | `database_delivery/2026-06-25/slate_before_connected_dummy.sql.sha256` |
| 적용 후 전체 dump | `database_delivery/2026-06-25/slate_after_connected_dummy.sql` |
| 적용 후 checksum | `database_delivery/2026-06-25/slate_after_connected_dummy.sql.sha256` |
| seed SQL | `sql/18_seed_connected_demo_data.sql` |
| generated image apply | `sql/27_apply_generated_dummy_images.sql` |
| 검증 SQL | `sql/19_validate_connected_demo_data.sql` |
| CDD 전용 rollback SQL | `sql/20_rollback_connected_demo_data.sql` |
| 검증 결과 | `docu/dummy_data/validation_result.md` |
| 테스트 계정 | `docu/dummy_data/test_accounts.md` |

Git 브랜치에 포함되는 파일은 seed, 검증, rollback SQL과 문서다. `database_delivery` 파일은 로컬 비공개 전달물이다.

## 체크섬

```text
56aa64659c6fa212eef7bc038701396de6272defb070016ece569ba2395f14d9  database_delivery/2026-06-25/slate_before_connected_dummy.sql
0d0594af8c153b47e94e3842e35522ffb4d56bd266fef8b853e22d8fe40a7cd5  database_delivery/2026-06-25/slate_after_connected_dummy.sql
```

검증 명령:

```bash
shasum -a 256 -c database_delivery/2026-06-25/slate_before_connected_dummy.sql.sha256
shasum -a 256 -c database_delivery/2026-06-25/slate_after_connected_dummy.sql.sha256
```

## 적용 후 dump로 환경 맞추기

공동작업자가 같은 더미 데이터 상태로 맞춰야 할 때 사용한다.

주의:

- 로컬 개발용 `slate` DB에서만 사용한다.
- 운영/공유 원본 DB에 직접 실행하지 않는다.
- dump restore는 전체 DB 상태를 dump 시점으로 맞추는 작업이다.
- 실행 전 현재 로컬 DB가 필요한 상태라면 별도 백업을 먼저 만든다.

```bash
mysql --login-path=slate-admin < database_delivery/2026-06-25/slate_after_connected_dummy.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
```

검증 기준:

- `CDD_EXPECTED_COUNTS`의 `actual_count`와 `expected_count`가 모두 일치해야 한다.
- `CDD_ZERO_ERROR_CHECKS`의 모든 `issue_count`가 0이어야 한다.
- `team_work_request_requester_not_active_member`가 0이어야 한다.

## 적용 전 상태로 전체 복구하기

seed 적용 전 전체 DB 상태로 되돌려야 할 때 사용한다.

주의:

- 이 방법은 전체 `slate` DB를 적용 전 dump 시점으로 되돌린다.
- seed 이후에 추가된 다른 사용자 작업도 함께 사라질 수 있다.
- 실행 전 반드시 현재 상태를 별도 백업한다.

```bash
mysql --login-path=slate-admin < database_delivery/2026-06-25/slate_before_connected_dummy.sql
```

## CDD 더미 데이터만 제거하기

전체 DB를 되돌리지 않고 이번 seed의 CDD namespace만 제거해야 할 때 사용한다.

제거 범위:

- `login_id LIKE 'cdd-%'`
- 제목 prefix `[CDD]`
- 포트폴리오 외부 소스 `SLATE_CDD`
- 감사/운영 로그 코드 `CDD_*`

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/20_rollback_connected_demo_data.sql
```

rollback 후 확인:

```bash
mysql --login-path=slate-admin --batch --raw slate -e "SELECT COUNT(*) AS cdd_accounts FROM user_account WHERE login_id LIKE 'cdd-%';"
mysql --login-path=slate-admin --batch --raw slate -e "SELECT COUNT(*) AS cdd_teams FROM team WHERE name LIKE '[CDD]%';"
mysql --login-path=slate-admin --batch --raw slate -e "SELECT COUNT(*) AS cdd_posts FROM board_post WHERE title LIKE '[CDD]%';"
mysql --login-path=slate-admin --batch --raw slate -e "SELECT COUNT(*) AS cdd_contests FROM contest WHERE title LIKE '[CDD]%';"
```

각 결과가 0이면 CDD namespace 제거가 완료된 것이다.

## 금지/주의 사항

- 새 DB를 만들기 위한 별도 작업을 하지 않는다.
- `sql/99_reset.sql`을 실행하지 않는다.
- 검증 실패 시 임의로 데이터를 수정하며 계속 진행하지 않는다.
- 전체 dump 복구와 CDD 전용 rollback의 목적을 혼동하지 않는다.
