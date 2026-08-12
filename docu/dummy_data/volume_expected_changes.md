# 2차 연관형 볼륨 더미 데이터 변경 내역

작성일: 2026-06-25
상태: 실제 DB 적용·검증 및 전달 dump 생성 완료

## 적용 전 DB 요약

2026-06-25 읽기 전용 조회 기준이다.

| 테이블 | 현재 건수 |
|---|---:|
| `user_account` | 34 |
| `member_profile` | 29 |
| `team` | 11 |
| `team_recruitment` | 14 |
| `team_recruitment_slot` | 30 |
| `team_application` | 8 |
| `team_invitation` | 5 |
| `team_plan_item` | 11 |
| `user_follow` | 5 |
| `matching_bookmark` | 16 |
| `matching_action_log` | 17 |
| `board_post` | 10 |
| `board_review` | 9 |
| `board_like` | 27 |
| `work_item` | 3 |
| `portfolio_item` | 8 |
| `team_work_approval_request` | 5 |
| `contest_open_request` | 2 |
| `contest` | 15 |
| `contest_save` | 6 |
| `contest_submission_prepare` | 2 |
| `contest_fit_cache` | 11 |
| `content_report` | 2 |
| `user_sanction` | 1 |
| `notification` | 33 |

namespace 확인:

- CDD 계정 9건, CDD 팀 2건
- CDV 계정 0건, CDV 팀 0건

## 기존 CDD 유지

기존 CDD 데이터는 수정하거나 재생성하지 않는다.

- `login_id LIKE 'cdd-%'` 제거 대상 제외
- `[CDD]` 제목 제거 대상 제외
- `SLATE_CDD` 포트폴리오 제거 대상 제외
- `CDD_*` 감사·운영 로그 제거 대상 제외

`sql/22_validate_connected_demo_volume_data.sql`에는 CDD 핵심 count를 다시 확인하는 `CDD_GUARD_COUNTS`와 `cdd_baseline_count_mismatch`를 포함했다.

## CDV 적용 및 검증 INSERT

첫 적용의 예상 INSERT 합계는 2,621건이며, 적용 후 38개 count 항목이 모두 기대값과 일치했다.

| 테이블 | 예상 INSERT |
|---|---:|
| `user_account` | 37 |
| `company_application` | 4 |
| `admin_permission` | 8 |
| `member_profile` | 32 |
| `profile_role` | 40 |
| `profile_genre` | 64 |
| `profile_collaboration_condition` | 64 |
| `user_follow` | 128 |
| `team` | 12 |
| `team_genre` | 24 |
| `team_member` | 36 |
| `team_recruitment` | 24 |
| `team_recruitment_slot` | 60 |
| `team_application` | 60 |
| `team_invitation` | 36 |
| `team_plan_item` | 60 |
| `team_closure_snapshot` | 2 |
| `matching_bookmark` | 96 |
| `matching_action_log` | 96 |
| `board_post` | 60 |
| `board_review` | 180 |
| `board_like` | 300 |
| `board_view_log` | 480 |
| `work_item` | 36 |
| `work_genre` | 72 |
| `team_work_approval_request` | 24 |
| `team_work_approval_genre` | 48 |
| `portfolio_item` | 64 |
| `contest_open_request` | 6 |
| `contest` | 24 |
| `contest_save` | 120 |
| `contest_submission_prepare` | 48 |
| `contest_fit_cache` | 48 |
| `content_report` | 12 |
| `user_sanction` | 4 |
| `notification` | 180 |
| `audit_log` | 28 |
| `operation_log` | 4 |

목표 범위보다 조회 로그가 많은 이유는 모든 CDV 게시글의 `view_count`를 실제 로그 건수와 맞추기 위해 게시글당 8건을 두었기 때문이다.

`admin_permission` 8건은 백엔드 `AdminPermissionCatalog.CODES`에 정의된 권한만 명시적으로 삽입한다. DB의 활성 권한 수가 늘어나더라도 `DEMO_ACCESS_MANAGE` 같은 비카탈로그 권한은 CDV 관리자에게 추가하지 않는다.

## 적용 시 UPDATE

첫 적용에서 CDV 내부 집계와 상태를 맞추기 위한 UPDATE 대상은 다음과 같다.

| 대상 | 예상 변경 행 | 이유 |
|---|---:|---|
| `team` | 12 | active 팀원 수 재계산 |
| `team_recruitment_slot` | 60 | accepted 수와 슬롯 상태 재계산 |
| `board_post` | 60 | 좋아요·리뷰·조회 집계 계산 |
| `contest_open_request` | 4 | 승인 요청과 공모전 연결 |
| `contest` | 24 | 저장 수 재계산 |
| `board_post` | 2 | accepted 신고의 게시글 숨김 |
| `board_review` | 2 | accepted 신고의 리뷰 숨김 |
| `board_post` | 2 | 숨김 리뷰가 있는 게시글의 리뷰 수 변경 |
| `user_account` | 32 | active 제재와 계정 상태 정합화 |

실제 값이 달라지는 예상 행은 약 198건이다. SQL의 matched rows와 MySQL 클라이언트의 changed rows 표시는 다를 수 있다.

비-CDV 행을 갱신하는 UPDATE는 없다.

## 적용 시 DELETE

현재 CDV namespace가 0건이므로 첫 적용 전 정리 블록의 예상 DELETE는 0건이다.

재실행 시에는 기존 CDV 행만 관계 역순으로 제거한 뒤 동일한 결과를 다시 만든다.

- 계정: `login_id LIKE 'cdv-%'`
- 제목: `[CDV]` prefix
- 외부 식별자: `CDV-*`
- 포트폴리오 외부 소스: `SLATE_CDV`
- 감사·운영 코드: `CDV_*`

`sql/23_rollback_connected_demo_volume_data.sql`도 같은 범위만 제거한다.

## 기존 데이터 수정 여부

의도적인 기존 데이터 수정은 없다.

- CDD 데이터: 수정 없음
- 기존 사용자 생성 계정: 수정 없음
- 기존 크롤링 공모전: 수정 없음
- 기존 업로드 이미지 경로: 수정 없음
- reference code: 수정 없음
- 프런트엔드·백엔드 코드: 수정 없음

## 업로드 파일 계획

새 업로드 파일을 만들지 않는다.

| 영역 | CDV 값 |
|---|---|
| 프로필 이미지 | `NULL` |
| 팀 대표 이미지 | `NULL` |
| 작업물 대표 이미지 | `NULL` |
| 포트폴리오 썸네일 | `NULL` |
| 공모전 대표 이미지 | `NULL` |
| 공모전 요청 대표 이미지 | `NULL` |

외부 이미지 URL과 외부 공모전 URL도 추가하지 않는다.

## 백업 결과

사용자 승인 후 적용 직전과 직후의 `slate` 전체 DB dump와 checksum을 생성했다.

| 구분 | 경로 | SHA-256 |
|---|---|---|
| 적용 전 | `database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql` | `239b71df9f3fba8b3dc7386ec8fa83658816ab31e7efca6ed46113d9599e305d` |
| 적용 후 | `database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql` | `de378b86c681b384fe63e7cef7be028e0ce41f3331a1963d0641a89009c7f0b5` |

두 checksum 검증은 모두 통과했다.

## 적용·검증·rollback

실제 적용:

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/21_seed_connected_demo_volume_data.sql
```

실제 검증:

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/22_validate_connected_demo_volume_data.sql
```

필요 시 CDV namespace 전용 rollback:

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/23_rollback_connected_demo_volume_data.sql
```

seed와 validation은 실행 완료했으며 rollback은 실행하지 않았다.

검증 성공 기준:

- `CDV_EXPECTED_COUNTS`의 실제값과 기대값이 모두 일치
- `CDV_ZERO_ERROR_CHECKS`의 모든 값이 0
- `cdv-admin`의 비카탈로그·누락·비활성 권한 검사가 모두 0
- `CDD_GUARD_COUNTS`가 기존 CDD 기준과 일치
- rollback 후 CDV namespace count가 모두 0

실제 결과:

- CDV 예상 count 38개 항목 모두 일치
- CDV zero-error 39개 항목 모두 0
- CDD 예상 count 11개 항목 모두 일치
- CDD zero-error 23개 항목 모두 0
- `cdv-admin` 백엔드 카탈로그 권한 8개 적용

## 남은 확인과 주의

- 화면별 정렬과 pagination이 충분한 볼륨을 자연스럽게 보여주는지는 브라우저 검증이 필요하다.
- 신고·제재 화면의 표시 문구와 알림 target 라우팅은 실제 API 응답으로 확인해야 한다.
- CDD guard count는 2026-06-25 검수 완료 기준을 사용하므로, CDD를 의도적으로 변경한 뒤에는 기대값 재검토가 필요하다.
- dump에는 계정 데이터와 비밀번호 hash가 포함되므로 공개 저장소에 커밋하지 않는다.

## 산출물

- `sql/21_seed_connected_demo_volume_data.sql`
- `sql/22_validate_connected_demo_volume_data.sql`
- `sql/23_rollback_connected_demo_volume_data.sql`
- `docu/dummy_data/volume_data_scenarios.md`
- `docu/dummy_data/volume_expected_changes.md`
- `docu/dummy_data/volume_validation_result.md`
- `docu/dummy_data/volume_test_accounts.md`
- `docu/dummy_data/volume_restore_guide.md`
- `database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql`
- `database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql`
