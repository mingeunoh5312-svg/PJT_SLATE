# 연관형 더미 데이터 변경 내역

작성일: 2026-06-25
상태: 사용자 승인 후 `slate` DB 적용 및 검증 완료.

이 문서는 CDD 연관형 더미 데이터의 적용 전 예상 변경 범위와 실제 적용 결과를 함께 보관한다. 최종 검증 결과는 `docu/dummy_data/validation_result.md`, 복구/전달 절차는 `docu/dummy_data/restore_guide.md`를 기준으로 한다.

## 적용 전 DB 상태 요약

적용 전 읽기 전용으로 재확인한 주요 테이블 건수는 다음과 같다.

| 테이블 | 현재 건수 |
|---|---:|
| `user_account` | 25 |
| `member_profile` | 21 |
| `team` | 9 |
| `board_post` | 7 |
| `work_item` | 2 |
| `portfolio_item` | 6 |
| `contest` | 14 |
| `contest_open_request` | 1 |
| `contest_submission_prepare` | 0 |
| `content_report` | 1 |
| `user_sanction` | 0 |
| `notification` | 22 |
| `team_closure_snapshot` | 0 |
| `user_follow` | 0 |
| `work_genre` | 0 |
| `team_work_approval_genre` | 0 |

적용 전 CDD namespace(`login_id LIKE 'cdd-%'`) 계정은 0건이었다. 따라서 이번 적용은 CDD 더미 데이터를 새로 추가하는 기준으로 설계했다.

## 적용 전 상태별 공백

| 영역 | 현재 상태 |
|---|---|
| 사용자 | `USER` 22, `COMPANY` 2, `ADMIN` 1 |
| 계정 상태 | `ACTIVE` 24, `PENDING_APPROVAL` 1 |
| 팀 | `RECRUITING` 8, `IN_PROGRESS` 1, `ENDED` 0 |
| 팀 지원 | `ACCEPTED` 1, `REJECTED` 1 |
| 팀 초대 | `PENDING` 1 |
| 팀 일정 | `TODO` 3, `IN_PROGRESS` 1, `DONE` 1 |
| 팀 작업물 승인 | `PENDING` 1 |
| 공모전 | `OPEN` 14 |
| 신고 | `PENDING` 1 |
| 제재 | 0 |

이번 seed는 위 공백을 보완하기 위해 종료 팀, 팔로우, 작업물 장르, 팀 작업물 승인 장르, 공모전 준비, 신고 승인, 활성 제재 등을 연결된 시나리오로 추가했다.

## 선택 시나리오

1. 모집 중인 단편팀과 역할별 슬롯
   - `[CDD] 한강 야간 단편팀`
   - 지원 상태: `ACCEPTED`, `PENDING`, `REJECTED`, `CANCELED`, `EXPIRED`
   - 초대 상태: `ACCEPTED`, `PENDING`, `CANCELED`, `EXPIRED`
2. 완료된 팀과 종료 스냅샷
   - `[CDD] 완료된 포트폴리오팀`
   - `team_closure_snapshot` 생성
3. 팀 작업물 승인에서 게시글, 작업물, 포트폴리오까지 이어지는 흐름
   - 승인 요청 4상태: `APPROVED`, `PENDING`, `REJECTED`, `CANCELED`
   - `board_post`, `work_item`, `work_genre`, `portfolio_item` 연결
4. 회사 공모전 요청, 관리자 승인, 공모전 노출 흐름
   - 승인 회사 `cdd-company`
   - 공모전 요청과 승인 공모전 상호 연결
   - 저장, 제출 준비, 추천 캐시 생성
5. 신고 승인, 게시글 블라인드, 사용자 제재, 알림/감사 로그 흐름
   - `content_report.status = ACCEPTED`
   - `board_post.status = BLINDED`
   - `user_sanction.status = ACTIVE`

자세한 화면별 검증 목적은 `docu/dummy_data/data_scenarios.md`에 분리했다.

## 예상/검증 INSERT 건수

CDD namespace가 없는 현재 상태 기준으로 예상되는 신규 행 수는 다음과 같다.

| 테이블 | 예상 INSERT |
|---|---:|
| `user_account` | 9 |
| `company_application` | 1 |
| `member_profile` | 8 |
| `profile_role` | 10 |
| `profile_genre` | 14 |
| `profile_collaboration_condition` | 13 |
| `user_follow` | 5 |
| `team` | 2 |
| `team_genre` | 5 |
| `team_member` | 7 |
| `team_recruitment` | 2 |
| `team_recruitment_slot` | 6 |
| `team_application` | 5 |
| `team_invitation` | 4 |
| `team_plan_item` | 6 |
| `team_closure_snapshot` | 1 |
| `matching_bookmark` | 5 |
| `matching_action_log` | 3 |
| `board_post` | 3 |
| `work_item` | 1 |
| `work_genre` | 2 |
| `board_review` | 3 |
| `board_like` | 6 |
| `board_view_log` | 2 |
| `team_work_approval_request` | 4 |
| `team_work_approval_genre` | 4 |
| `portfolio_item` | 2 |
| `contest_open_request` | 1 |
| `contest` | 1 |
| `contest_save` | 3 |
| `contest_submission_prepare` | 2 |
| `contest_fit_cache` | 2 |
| `content_report` | 1 |
| `user_sanction` | 1 |
| `notification` | 7 |
| `audit_log` | 3 |
| `operation_log` | 1 |

총 예상 INSERT는 155건이다. 적용 후 `sql/19_validate_connected_demo_data.sql` 기준 핵심 count 검증은 모두 예상값과 일치했다.

## 예상 UPDATE 건수

대부분은 seed의 멱등성을 위해 CDD namespace 안의 행을 정규화하거나 집계값을 재계산하는 UPDATE다.

| 대상 | 예상 UPDATE | 이유 |
|---|---:|---|
| `user_account` | 1 | `cdd-moderated` 제재 상태를 `TEMP_SUSPENDED`로 정규화 |
| `team` | 2 | CDD 팀 상태, 리더, 지역, `current_member_count` 정규화 |
| `team_recruitment` | 2 | CDD 모집 공고 상태/마감일 정규화 |
| `team_recruitment_slot` | 6 | CDD 슬롯 상태와 `accepted_count` 정규화 |
| `work_item` | 1 | CDD 작업물 대표 정보 정규화 |
| `board_post` | 3 | CDD 게시글 상태와 좋아요/댓글 수 정산 |
| `team_work_approval_request` | 1 | 승인 요청과 생성된 게시글/작업물 연결 |
| `contest_open_request` | 1 | 승인 요청과 생성된 공모전 연결 |
| `contest` | 1 | 공모전 저장 수와 요청자/요청 연결 정규화 |
| `user_sanction` | 1 | CDD 제재 상태와 기간 정규화 |

기존 비-CDD 행을 의도적으로 수정하지 않는다.

## 예상 DELETE 건수

`sql/18_seed_connected_demo_data.sql` 시작부에는 같은 seed를 재실행할 때 중복을 만들지 않기 위한 CDD namespace 정리 블록이 있다.

적용 전 DB 기준 CDD namespace(`login_id LIKE 'cdd-%'`, `[CDD]` 제목, `SLATE_CDD` 외부 소스)는 0건이었으므로 첫 적용 시 예상 DELETE는 0건이었다. 재실행 시에는 기존 CDD 행만 정리한 뒤 다시 생성한다.

## 기존 데이터 수정 여부

의도적으로 수정하는 기존 데이터는 없다. 모든 변경은 다음 namespace 안에서만 수행한다.

- 계정: `login_id LIKE 'cdd-%'`
- 제목: `[CDD]` prefix
- 외부 식별자: `CDD-*`
- 포트폴리오 외부 소스: `SLATE_CDD`
- 감사/운영 로그 코드: `CDD_*`

다만 같은 seed를 반복 실행할 경우에는 기존 CDD 행을 삭제/재삽입하거나 UPDATE해 같은 결과가 되도록 만든다.

## 업로드 파일 계획

이번 적용에서 새 업로드 파일은 생성하지 않는다.

| 영역 | seed 값 | 파일 필요 여부 |
|---|---|---|
| 프로필 이미지 | `NULL` | 없음 |
| 팀 대표 이미지 | `NULL` | 없음 |
| 작업물 대표 이미지 | `NULL` | 없음 |
| 포트폴리오 썸네일 | `NULL` | 없음 |
| 공모전 대표 이미지 | `NULL` | 없음 |

프론트엔드 기본 이미지 fallback을 확인하는 쪽으로 설계했다.

## 기존 데이터 경고

이번 seed가 만들거나 수정하는 문제는 아니지만, 현재 DB/파일 시스템에는 다음 경고가 있다.

| 항목 | 현재 값 |
|---|---:|
| `member_profile.profile_image_path` 비어 있지 않은 경로 | 1 |
| `contest.representative_image_path` 비어 있지 않은 경로 | 8 |
| `portfolio_item.url LIKE '%example.test%'` | 3 |
| `public_data_sync_item.provider_url LIKE '%example.test%'` | 5 |
| `contest`의 `example.test` URL | 1 |

파일 시스템 확인 결과, 실제 존재하는 업로드 파일은 다음 1개뿐이다.

- `backend/uploads/images/profile/2026/06/8ac94885-bdcc-4883-ad0e-17caa0ff51ea.png`

반면 현재 DB의 공모전 대표 이미지 경로 8개는 `backend/uploads` 아래 실제 파일이 없다.

- `images/contest/2026/06/7ad00342-e765-4c11-875d-c89e4de8cb45.jpg`
- `images/contest/2026/06/2dd3b485-b09b-45d7-b0d6-d5d5fc25c5a7.jpg`
- `images/contest/2026/06/6871799a-876b-4609-a027-35bfbe1720ba.jpg`
- `images/contest/2026/06/1d6d1082-e712-481d-92fc-091c3114fb17.png`
- `images/contest/2026/06/2e613cd3-e116-4d0d-bd71-65333fe07090.jpg`
- `images/contest/2026/06/11ec3389-705a-489c-9300-17f9a790d825.jpg`
- `images/contest/2026/06/c1f6c289-4ee6-49a4-8eef-25ee006a6f3c.jpg`
- `images/contest/2026/06/fc52c33e-b5cd-4983-a8c8-22a72dbef0d2.jpg`

이번 작업에서는 위 기존 누락 파일을 수정하지 않고, 검증 결과 문서에 “기존 데이터 경고”로 남겼다.

## 백업/전달 결과

승인 후 실제 적용 직전에 현재 DB를 먼저 백업했고, 적용 후 상태도 별도 dump로 저장했다.

| 구분 | 경로 |
|---|---|
| 적용 전 전체 dump | `database_delivery/2026-06-25/slate_before_connected_dummy.sql` |
| 적용 전 checksum | `database_delivery/2026-06-25/slate_before_connected_dummy.sql.sha256` |
| 적용 후 전체 dump | `database_delivery/2026-06-25/slate_after_connected_dummy.sql` |
| 적용 후 checksum | `database_delivery/2026-06-25/slate_after_connected_dummy.sql.sha256` |

체크섬:

```text
56aa64659c6fa212eef7bc038701396de6272defb070016ece569ba2395f14d9  database_delivery/2026-06-25/slate_before_connected_dummy.sql
0d0594af8c153b47e94e3842e35522ffb4d56bd266fef8b853e22d8fe40a7cd5  database_delivery/2026-06-25/slate_after_connected_dummy.sql
```

## 실제 적용 절차

사용자 승인 후 다음 순서로 실행했다.

1. 적용 전 전체 DB 백업 생성
2. `sql/18_seed_connected_demo_data.sql` 실행
3. `sql/19_validate_connected_demo_data.sql` 실행
4. 검증 결과를 `docu/dummy_data/validation_result.md`에 기록
5. 적용 후 전체 DB dump와 checksum 생성
6. `docu/dummy_data/restore_guide.md`와 `docu/dummy_data/test_accounts.md` 작성

절대 실행하지 않는 작업:

- 새 DB 생성
- `DROP`, `TRUNCATE`, 전체 reset
- `sql/99_reset.sql`
- `FOREIGN_KEY_CHECKS = 0`
- 비-CDD 기존 데이터 수정

## 검증 결과와 유지할 검증 기준

SQL 검증은 완료했다. 결과 요약은 다음과 같다.

- seed 적용 성공
- `CDD_EXPECTED_COUNTS` 모두 예상 count와 일치
- `CDD_ZERO_ERROR_CHECKS` 모두 0
- `team_work_request_requester_not_active_member = 0`
- CDD seed가 만든 실패 없음
- 기존 데이터 경고 2건은 `docu/dummy_data/validation_result.md`에 분리 기록

후속 환경에서 다시 적용하거나 dump로 복구한 경우 다음 기준을 유지한다.

- CDD 예상 건수 확인
- 팀 리더/멤버 수 정합성 확인
- 지원/초대 수락 건과 실제 팀원 연결 확인
- 종료 팀 snapshot 존재 확인
- 모집 종료 팀에 열린 슬롯이 없는지 확인
- 게시글 좋아요/댓글 집계 확인
- 작업물 게시글 연결 확인
- 팀 작업물 승인 요청자가 해당 팀의 `ACTIVE` 멤버인지 확인
- 공모전 저장 수와 실제 저장 행 수 확인
- 승인 공모전 요청과 공모전 상호 연결 확인
- 승인 팀 작업물 요청과 게시글/작업물 연결 확인
- CDD 이미지 경로가 모두 `NULL`인지 확인
- 자기 자신 팔로우가 없는지 확인
- CDD 계정/팀/작업물/공모전이 화면 조회 조건을 만족하는지 확인

애플리케이션 화면 검증은 아직 남아 있다. CDD 테스트 계정을 사용해 다음 화면을 desktop/mobile에서 smoke한다.

- 홈
- 매칭 회원/팀
- 팀 상세
- 게시판 목록/상세
- 프로필/포트폴리오
- 공모전 목록/상세/준비
- 관리자 신고/회원/공모전 요청

## 최종 산출물

생성한 산출물:

- `docu/dummy_data/test_accounts.md`
- `docu/dummy_data/validation_result.md`
- `docu/dummy_data/restore_guide.md`
- `database_delivery/2026-06-25/slate_before_connected_dummy.sql`
- `database_delivery/2026-06-25/slate_before_connected_dummy.sql.sha256`
- `database_delivery/2026-06-25/slate_after_connected_dummy.sql`
- `database_delivery/2026-06-25/slate_after_connected_dummy.sql.sha256`
