# 연관형 더미 데이터 실제 DB 반영 작업 로그

## 작업 범위

- 역할: 생성자
- 작업일: 2026-06-25
- 대상 DB: `slate`
- 사용자 승인 후 실제 DB 반영 단계를 진행했다.

## 실행 순서

1. 현재 `slate` DB 백업
2. `sql/15_seed_connected_demo_data.sql` 실행
3. `sql/16_validate_connected_demo_data.sql` 실행
4. 검증 결과 확인
5. 검증 결과 문서 저장
6. seed 적용 후 DB dump 생성
7. 공동작업자 전달용 문서 작성

## 생성 파일

- `database_delivery/2026-06-25/slate_before_connected_dummy.sql`
- `database_delivery/2026-06-25/slate_before_connected_dummy.sql.sha256`
- `database_delivery/2026-06-25/slate_after_connected_dummy.sql`
- `database_delivery/2026-06-25/slate_after_connected_dummy.sql.sha256`
- `docu/dummy_data/validation_result.md`
- `docu/dummy_data/restore_guide.md`
- `docu/dummy_data/test_accounts.md`

## 체크섬

```text
56aa64659c6fa212eef7bc038701396de6272defb070016ece569ba2395f14d9  database_delivery/2026-06-25/slate_before_connected_dummy.sql
0d0594af8c153b47e94e3842e35522ffb4d56bd266fef8b853e22d8fe40a7cd5  database_delivery/2026-06-25/slate_after_connected_dummy.sql
```

## 검증 결과

- `CDD_EXPECTED_COUNTS`: 모든 `actual_count`가 `expected_count`와 일치
- `CDD_ZERO_ERROR_CHECKS`: 모든 `issue_count = 0`
- `team_work_request_requester_not_active_member = 0`
- 실패 없음

## 기존 데이터 경고

검증 SQL에서 다음 기존 데이터 경고를 확인했다.

- `existing_upload_paths_without_file_manifest_check = 9`
- `example_test_url_rows = 9`

해당 경고는 이번 CDD seed가 만든 데이터가 아니다. 이번 seed는 새 업로드 이미지 경로나 새 외부 URL을 추가하지 않았다.

## 실행한 주요 명령

```bash
mkdir -p database_delivery/2026-06-25
mysqldump --login-path=slate-admin --single-transaction --routines --triggers --databases slate --result-file=database_delivery/2026-06-25/slate_before_connected_dummy.sql
shasum -a 256 database_delivery/2026-06-25/slate_before_connected_dummy.sql > database_delivery/2026-06-25/slate_before_connected_dummy.sql.sha256
mysql --login-path=slate-admin --batch --raw slate < sql/15_seed_connected_demo_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/16_validate_connected_demo_data.sql > /private/tmp/slate_cdd_validation.tsv
awk 'BEGIN{bad=0} /^CDD_EXPECTED_COUNTS/ {if ($3 != $4) {print "COUNT_MISMATCH", $0; bad=1}} /^CDD_ZERO_ERROR_CHECKS/ {if ($3 != 0) {print "ISSUE", $0; bad=1}} END{if (bad) exit 1; print "VALIDATION_OK"}' /private/tmp/slate_cdd_validation.tsv
mysqldump --login-path=slate-admin --single-transaction --routines --triggers --databases slate --result-file=database_delivery/2026-06-25/slate_after_connected_dummy.sql
shasum -a 256 database_delivery/2026-06-25/slate_after_connected_dummy.sql > database_delivery/2026-06-25/slate_after_connected_dummy.sql.sha256
```

## 금지 작업 준수

- 새 DB 생성 작업을 별도로 수행하지 않았다.
- `sql/99_reset.sql`을 실행하지 않았다.
- 수동 `DROP`, `TRUNCATE`, `FOREIGN_KEY_CHECKS = 0`을 실행하지 않았다.
- 검증 실패 후 임의 수정하며 계속 진행한 상황은 없었다.
