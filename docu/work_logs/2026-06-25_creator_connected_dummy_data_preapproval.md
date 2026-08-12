# 연관형 더미 데이터 생성자 승인 전 작업 로그

## 작업 범위

- 역할: 생성자
- 기준 프롬프트: `docu/prompt/connected_dummy_data_creator_prompt.md`
- 목표: 현재 `slate` DB와 코드/문서를 분석한 뒤, 실제 DB 적용 전 승인 검토용 seed, 검증, 롤백, 시나리오, 예상 변경 문서를 준비한다.
- 실제 DB mutation은 수행하지 않았다.
- `DROP`, `TRUNCATE`, reset, `sql/99_reset.sql`, `FOREIGN_KEY_CHECKS = 0`, 새 DB 생성은 수행하지 않았다.

## 현재 상태

- 승인 전 준비 완료.
- 현재 `slate` DB에는 CDD 더미 데이터를 아직 적용하지 않았다.
- CDD namespace(`login_id LIKE 'cdd-%'`, `[CDD]` 제목, `SLATE_CDD` 외부 소스)는 현재 0건이다.
- 다음 단계는 사용자 승인 후 백업 생성, seed 적용, 검증, 최종 전달본 생성이다.

## 생성/수정한 파일

- `sql/15_seed_connected_demo_data.sql`
- `sql/16_validate_connected_demo_data.sql`
- `sql/17_rollback_connected_demo_data.sql`
- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/expected_changes.md`

## 참조한 주요 문서

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_common/document_structure.md`
- `docu/03_mvp_scope/mvp_scope.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/04_setup/local_setup.md`
- `docu/04_setup/env_variables.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/user_temp/todo_0624_dummies_data.md`
- `docu/user_temp/todo_common_home_matching_team_image_requirements.md`
- `docu/prompt/default_image_generation_prompts.md`

## 현재 DB 읽기 전용 확인 결과

주요 테이블 현재 건수:

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

상태별 공백:

- 팀: `RECRUITING` 8, `IN_PROGRESS` 1, `ENDED` 0
- 팀 지원: `ACCEPTED` 1, `REJECTED` 1
- 팀 초대: `PENDING` 1
- 팀 일정: `TODO` 3, `IN_PROGRESS` 1, `DONE` 1
- 팀 작업물 승인: `PENDING` 1
- 공모전: `OPEN` 14
- 신고: `PENDING` 1
- 제재: 0

## 기존 데이터 경고

이번 seed가 만든 문제는 아니지만, 현재 DB/파일 상태에서 다음을 확인했다.

- `member_profile.profile_image_path` 비어 있지 않은 경로: 1건
- `contest.representative_image_path` 비어 있지 않은 경로: 8건
- 실제 존재하는 업로드 파일: `backend/uploads/images/profile/2026/06/8ac94885-bdcc-4883-ad0e-17caa0ff51ea.png` 1개
- 기존 공모전 대표 이미지 경로 8개는 `backend/uploads` 아래 실제 파일이 없다.
- 기존 `example.test` URL은 총 9건이다.

이번 seed는 새 업로드 이미지 경로나 새 외부 URL을 추가하지 않는다.

## 설계한 CDD 시나리오

1. 모집 중인 단편팀과 역할별 슬롯
2. 지원/초대 상태 다양화
3. 일정 진행과 정상 종료 팀
4. 팀 작업물 승인에서 게시글, 작업물, 포트폴리오까지 이어지는 흐름
5. 회사 공모전 요청, 관리자 승인, 공모전 생성
6. 신고 승인, 게시글 블라인드, 사용자 제재, 알림/감사 로그

## 예상 변경

- 예상 INSERT: 155건
- 예상 UPDATE: CDD namespace 내부 정규화/집계용 19건
- 예상 DELETE: 첫 적용 기준 0건
- 재실행 시에는 CDD namespace만 정리 후 재생성한다.
- 비-CDD 기존 행은 의도적으로 수정하지 않는다.

## 수행한 검증

- `sql/16_validate_connected_demo_data.sql`을 현재 DB에 읽기 전용으로 실행해 문법 확인 완료.
  - 적용 전이므로 CDD 예상 건수는 0으로 출력되는 것이 정상이다.
  - `missing_required_cdd_ids = 1`도 적용 전 기준 정상이다.
- seed/rollback 금지어 검색 완료.
  - `FOREIGN_KEY_CHECKS`, `DROP`, `TRUNCATE`, `CREATE DATABASE`, `sql/99_reset`, `slate_dummy`, 새 URL/이미지 경로 하드코딩 매치 없음.
- seed SQL 컬럼명 750개를 `information_schema.columns`와 대조했다.
  - 누락 컬럼 0개.
- `git diff --check` 통과.

## 주요 보강 사항

- `contest_fit_cache.expires_at`을 짧은 분 단위 만료에서 `NOW() + INTERVAL 30 DAY`로 조정했다.
- seed 시작부에 CDD namespace 정리 블록을 추가해 같은 seed를 재실행해도 CDD 범위에서 같은 결과가 되도록 보강했다.
- `docu/dummy_data/expected_changes.md`에 예상 DELETE, 기존 데이터 경고, 백업 계획, 검증 계획을 명시했다.

## 승인 후 계획

사용자 승인 후에만 다음을 수행한다.

1. `database_delivery/2026-06-25/slate_before_connected_dummy.sql` 백업 생성
2. `sql/15_seed_connected_demo_data.sql` 적용
3. `sql/16_validate_connected_demo_data.sql` 실행
4. 검증 결과를 `docu/dummy_data/validation_result.md`에 기록
5. 복구 방법을 `docu/dummy_data/restore_guide.md`에 기록
6. 필요 시 전달 패키지 생성

## 중단/보류 사유

`connected_dummy_data_creator_prompt.md`는 현재 `slate` DB에 실제 적용하기 전에 명시적 사용자 승인을 요구한다. 따라서 승인 전 단계에서 멈췄고, 실제 DB 백업/적용/최종 전달본 생성은 아직 수행하지 않았다.
