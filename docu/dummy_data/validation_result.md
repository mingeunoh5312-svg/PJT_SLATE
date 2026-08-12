# 연관형 더미 데이터 적용 검증 결과

작성일: 2026-06-26 KST
대상 DB: `slate`
적용 SQL: `sql/18_seed_connected_demo_data.sql`, `sql/27_apply_generated_dummy_images.sql`
검증 SQL: `sql/19_validate_connected_demo_data.sql`

## 결과 요약

- seed 적용: 성공
- 예상 count 검증: 성공
- `CDD_ZERO_ERROR_CHECKS`: 전체 0
- `team_work_request_requester_not_active_member`: 0
- DB 적용 후 중단 사유: 없음
- 실패: 없음
- 경고: 기존 데이터 경고 2건

## 예상 count 검증

| 항목 | 실제 | 예상 | 결과 |
|---|---:|---:|---|
| `user_account` | 9 | 9 | OK |
| `member_profile` | 8 | 8 | OK |
| `team` | 2 | 2 | OK |
| `team_application` | 5 | 5 | OK |
| `team_invitation` | 4 | 4 | OK |
| `team_work_approval_request` | 4 | 4 | OK |
| `contest_open_request_APPROVED` | 1 | 1 | OK |
| `contest_save` | 3 | 3 | OK |
| `contest_submission_prepare` | 2 | 2 | OK |
| `content_report_ACCEPTED` | 1 | 1 | OK |
| `active_sanction` | 1 | 1 | OK |

## 상태 분포

| 테이블 | 상태 | 건수 |
|---|---|---:|
| `team_application` | `ACCEPTED` | 1 |
| `team_application` | `CANCELED` | 1 |
| `team_application` | `EXPIRED` | 1 |
| `team_application` | `PENDING` | 1 |
| `team_application` | `REJECTED` | 1 |
| `team_invitation` | `ACCEPTED` | 1 |
| `team_invitation` | `CANCELED` | 1 |
| `team_invitation` | `EXPIRED` | 1 |
| `team_invitation` | `PENDING` | 1 |
| `team_plan_item` | `CANCELED` | 1 |
| `team_plan_item` | `DONE` | 2 |
| `team_plan_item` | `HOLD` | 1 |
| `team_plan_item` | `IN_PROGRESS` | 1 |
| `team_plan_item` | `TODO` | 1 |
| `team_work_approval_request` | `APPROVED` | 1 |
| `team_work_approval_request` | `CANCELED` | 1 |
| `team_work_approval_request` | `PENDING` | 1 |
| `team_work_approval_request` | `REJECTED` | 1 |

## Zero-error 체크

| 체크 | issue_count |
|---|---:|
| `missing_required_cdd_ids` | 0 |
| `team_leader_active_mismatch` | 0 |
| `active_leader_count_not_one` | 0 |
| `team_current_member_count_mismatch` | 0 |
| `slot_accepted_count_mismatch` | 0 |
| `slot_accepted_count_over_required` | 0 |
| `accepted_application_missing_active_member` | 0 |
| `accepted_invitation_missing_active_member` | 0 |
| `ended_team_missing_snapshot` | 0 |
| `ended_team_open_recruitment_or_slot` | 0 |
| `board_like_count_mismatch` | 0 |
| `board_review_count_mismatch` | 0 |
| `work_post_link_mismatch` | 0 |
| `approved_work_request_unlinked` | 0 |
| `team_work_request_requester_not_active_member` | 0 |
| `contest_save_count_mismatch` | 0 |
| `approved_contest_request_link_mismatch` | 0 |
| `self_follow` | 0 |
| `cdd_generated_image_path_missing_or_invalid` | 0 |
| `notification_bad_team_target` | 0 |
| `notification_bad_profile_target` | 0 |
| `notification_bad_contest_target` | 0 |
| `notification_bad_sanction_target` | 0 |

## 기존 데이터 경고

다음 항목은 CDD seed 실패가 아니라, 적용 전부터 있던 기존 데이터 경고다.

| 경고 | 건수 |
|---|---:|
| `existing_upload_paths_without_file_manifest_check` | 189 |
| `example_test_url_rows` | 9 |

해석:

- 현재 DB에는 이미지 적용 후 업로드 상대 경로 189건이 있다.
- 이번 작업으로 CDD/CDV 더미 이미지가 `uploads/images/seed/...` 아래 복사되어 DB 상대 경로와 매칭된다.
- 기존 `example.test` URL 9건은 이번 CDD seed가 추가한 값이 아니다.
- 이번 이미지 적용 SQL은 새 외부 URL을 추가하지 않았다.

## 실행 결과 원문

```text
section	item	actual_count	expected_count
CDD_EXPECTED_COUNTS	user_account	9	9
CDD_EXPECTED_COUNTS	member_profile	8	8
CDD_EXPECTED_COUNTS	team	2	2
CDD_EXPECTED_COUNTS	team_application	5	5
CDD_EXPECTED_COUNTS	team_invitation	4	4
CDD_EXPECTED_COUNTS	team_work_approval_request	4	4
CDD_EXPECTED_COUNTS	contest_open_request_APPROVED	1	1
CDD_EXPECTED_COUNTS	contest_save	3	3
CDD_EXPECTED_COUNTS	contest_submission_prepare	2	2
CDD_EXPECTED_COUNTS	content_report_ACCEPTED	1	1
CDD_EXPECTED_COUNTS	active_sanction	1	1
section	table_name	status	row_count
CDD_STATUS_COUNTS	team_application	ACCEPTED	1
CDD_STATUS_COUNTS	team_application	CANCELED	1
CDD_STATUS_COUNTS	team_application	EXPIRED	1
CDD_STATUS_COUNTS	team_application	PENDING	1
CDD_STATUS_COUNTS	team_application	REJECTED	1
CDD_STATUS_COUNTS	team_invitation	ACCEPTED	1
CDD_STATUS_COUNTS	team_invitation	CANCELED	1
CDD_STATUS_COUNTS	team_invitation	EXPIRED	1
CDD_STATUS_COUNTS	team_invitation	PENDING	1
CDD_STATUS_COUNTS	team_plan_item	CANCELED	1
CDD_STATUS_COUNTS	team_plan_item	DONE	2
CDD_STATUS_COUNTS	team_plan_item	HOLD	1
CDD_STATUS_COUNTS	team_plan_item	IN_PROGRESS	1
CDD_STATUS_COUNTS	team_plan_item	TODO	1
CDD_STATUS_COUNTS	team_work_approval_request	APPROVED	1
CDD_STATUS_COUNTS	team_work_approval_request	CANCELED	1
CDD_STATUS_COUNTS	team_work_approval_request	PENDING	1
CDD_STATUS_COUNTS	team_work_approval_request	REJECTED	1
section	check_name	issue_count
CDD_ZERO_ERROR_CHECKS	missing_required_cdd_ids	0
CDD_ZERO_ERROR_CHECKS	team_leader_active_mismatch	0
CDD_ZERO_ERROR_CHECKS	active_leader_count_not_one	0
CDD_ZERO_ERROR_CHECKS	team_current_member_count_mismatch	0
CDD_ZERO_ERROR_CHECKS	slot_accepted_count_mismatch	0
CDD_ZERO_ERROR_CHECKS	slot_accepted_count_over_required	0
CDD_ZERO_ERROR_CHECKS	accepted_application_missing_active_member	0
CDD_ZERO_ERROR_CHECKS	accepted_invitation_missing_active_member	0
CDD_ZERO_ERROR_CHECKS	ended_team_missing_snapshot	0
CDD_ZERO_ERROR_CHECKS	ended_team_open_recruitment_or_slot	0
CDD_ZERO_ERROR_CHECKS	board_like_count_mismatch	0
CDD_ZERO_ERROR_CHECKS	board_review_count_mismatch	0
CDD_ZERO_ERROR_CHECKS	work_post_link_mismatch	0
CDD_ZERO_ERROR_CHECKS	approved_work_request_unlinked	0
CDD_ZERO_ERROR_CHECKS	team_work_request_requester_not_active_member	0
CDD_ZERO_ERROR_CHECKS	contest_save_count_mismatch	0
CDD_ZERO_ERROR_CHECKS	approved_contest_request_link_mismatch	0
CDD_ZERO_ERROR_CHECKS	self_follow	0
CDD_ZERO_ERROR_CHECKS	cdd_generated_image_path_missing_or_invalid	0
CDD_ZERO_ERROR_CHECKS	notification_bad_team_target	0
CDD_ZERO_ERROR_CHECKS	notification_bad_profile_target	0
CDD_ZERO_ERROR_CHECKS	notification_bad_contest_target	0
CDD_ZERO_ERROR_CHECKS	notification_bad_sanction_target	0
section	check_name	row_count
GLOBAL_EXISTING_DATA_WARNINGS	existing_upload_paths_without_file_manifest_check	189
GLOBAL_EXISTING_DATA_WARNINGS	example_test_url_rows	9
```
