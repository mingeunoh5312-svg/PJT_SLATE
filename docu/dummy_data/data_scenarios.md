# 연관형 더미 데이터 시나리오

작성일: 2026-06-25

이 문서는 `sql/18_seed_connected_demo_data.sql`로 적용한 CDD 연관형 더미 데이터의 시나리오와 화면 검증 목적을 정리한다.

적용 상태:

- 2026-06-25 사용자 승인 후 `slate` DB에 적용 완료.
- `sql/19_validate_connected_demo_data.sql` 검증 결과, 예상 count가 모두 일치하고 `CDD_ZERO_ERROR_CHECKS`가 모두 0임을 확인했다.
- 적용 전/후 dump와 복구 절차는 `docu/dummy_data/restore_guide.md`에 정리했다.

## 설계 원칙

- 더미 namespace는 계정 login ID `cdd-*`, 제목 prefix `[CDD]`, 외부 식별자 `CDD-*`만 사용한다.
- 기존 샘플 계정, 사용자 생성 계정(`domingo5312`, `domingo53121`, `asd123`, `test1`)과 크롤링 공모전 데이터는 수정하지 않는다.
- 업로드 이미지 경로는 새로 저장하지 않는다. 프로필, 팀, 작업물, 포트폴리오, 공모전은 프론트 기본 이미지 fallback을 검증한다.
- 외부 URL은 새로 추가하지 않는다. 제출 이메일은 `.test` 도메인의 데모 문자열만 사용한다.
- 실제 KOBIS 검증이 아닌 포트폴리오는 `VERIFIED`로 만들지 않는다.
- 회사 승인, 공모전 승인, 신고 처리, 제재, 알림, 감사 로그는 원인 데이터와 함께 생성한다.

## 등장 계정

| login_id | 유형 | 상태 | 역할 |
|---|---|---|---|
| `cdd-leader` | USER | ACTIVE | 한강 야간 단편팀 리더/프로듀서 |
| `cdd-camera` | USER | ACTIVE | 수락된 촬영감독 지원자 |
| `cdd-sound` | USER | ACTIVE | 초대 대기 중인 동시녹음 후보 |
| `cdd-editor` | USER | ACTIVE | 초대 수락 팀원, 승인 작업물 작성자 |
| `cdd-writer` | USER | ACTIVE | 지원 대기 및 만료 초대 확인용 작가 |
| `cdd-actor` | USER | ACTIVE | 팀원, 만료 지원 확인용 배우 |
| `cdd-reporter` | USER | ACTIVE | 신고 및 거절/취소 흐름 확인용 사용자 |
| `cdd-moderated` | USER | TEMP_SUSPENDED | 신고 승인 후 제재 대상 |
| `cdd-company` | COMPANY | ACTIVE | 승인 회사 및 공모전 요청자 |

공통 데모 비밀번호는 기존 샘플 정책과 같은 `{noop}slate1234` 기준이다. 배포용 secret이나 실제 비밀번호가 아니다.

## 시나리오 1. 모집 중인 한강 야간 단편팀

- 팀: `[CDD] 한강 야간 단편팀`
- 상태: `RECRUITING`
- 리더: `cdd-leader`
- 장르: 드라마, 스릴러, 청춘/학원
- 지역: 서울 마포구
- 멤버: 리더, 배우, 촬영감독, 편집
- 모집 공고: `[CDD] 한강 야간 단편 촬영/후반 모집`
- 슬롯:
  - 촬영감독: `ACCEPTED` 지원 반영, 정원 충족으로 `CLOSED`
  - 영상 편집: `ACCEPTED` 초대 반영, 정원 충족으로 `CLOSED`
  - 동시녹음: 초대 `PENDING`, 슬롯 `OPEN`
  - 시나리오 작가: 지원 `PENDING`, 슬롯 `OPEN`

기대 확인 화면:

- `/matching/members`에서 팀 기준 후보와 초대 상태 확인
- `/matching/teams`에서 저장한 팀, 지원한 팀 상태 확인
- `/teams/:teamId`에서 멤버/모집/지원/초대 요약 확인
- 홈 USER 요약의 참여 팀, 초대, 읽지 않은 알림, 일정 수치 확인

## 시나리오 2. 지원·초대 상태 다양화

생성 상태:

- 지원: `ACCEPTED`, `PENDING`, `REJECTED`, `CANCELED`, `EXPIRED`
- 초대: `ACCEPTED`, `PENDING`, `CANCELED`, `EXPIRED`

불변 조건:

- `ACCEPTED` 지원자와 초대 대상자는 실제 `ACTIVE` 팀원이다.
- `PENDING` 지원·초대는 generated unique key가 겹치지 않는다.
- `REJECTED` 지원에는 결정자, 결정 시각, 사유가 있다.
- `CANCELED`, `EXPIRED`는 종료된 팀 또는 사용자의 취소 흐름과 맞게 둔다.

## 시나리오 3. 일정 진행과 정상 종료 팀

- 진행 팀 일정:
  - `DONE`: 촬영 콘티 1차 확정
  - `IN_PROGRESS`: 야간 로케이션 리허설
  - `TODO`: 사운드 체크리스트 공유
  - `HOLD`: 비 예보 대응 플랜
- 종료 팀: `[CDD] 완료된 포트폴리오팀`
  - 상태: `ENDED`
  - 종료 유형: `NORMAL`
  - 모집 공고/슬롯: 모두 `CLOSED`
  - `team_closure_snapshot`에 팀, 멤버, 모집, 일정 요약 JSON 저장

기대 확인 화면:

- `/teams/:teamId/plans`
- `/teams/:teamId/close`
- 관리자 팀 목록의 종료 팀 상태

## 시나리오 4. 팀 작업물 승인 → 게시글 → 작업물 → 포트폴리오

- 승인 요청: `[CDD] 한강 야간 컷 공개 승인`
- 상태: `APPROVED`
- 연결:
  - `team_work_approval_request.board_post_id`
  - `team_work_approval_request.work_id`
  - `board_post` WORK 게시글
  - `work_item` MANUAL 작업물
  - `work_genre`
  - `portfolio_item`
- 추가 상태:
  - 승인 대기 요청: `cdd-camera`가 요청한 `[CDD] 한강 현장음 믹스 승인 대기`
  - 반려 요청: `cdd-editor`가 요청한 `[CDD] 미완성 러프컷 반려`
  - 취소 요청

기대 확인 화면:

- `/boards` HOME/WORK/POPULAR
- `/boards/search`
- `/boards/:postId`
- `/profile`
- `/profiles/:profileId`
- 팀 작업물 승인 관리

## 시나리오 5. 회사 공모전 요청 → 승인 → 공모전 생성

- 회사 계정: `cdd-company`
- 회사 승인: `company_application.status = APPROVED`
- 요청: `[CDD] 도시 단편 제작지원 요청`
- 승인 공모전: `[CDD] 도시 단편 제작지원 공모`
- 연결:
  - `contest_open_request.approved_contest_id`
  - `contest.source_request_id`
  - `contest.requester_company_user_id`
- 구조화 필터:
  - 대상: `ADULT`, `UNIVERSITY`
  - 지역: `SEOUL`, `GYEONGGI`
  - 주최 유형: `COMPANY`
- 저장/준비:
  - `contest_save` 3건
  - `contest_submission_prepare` 2건
  - `contest_fit_cache` TEAM/PROFILE 2건

기대 확인 화면:

- `/contests`
- `/contests?view=saved`
- `/contests/:contestId`
- `/contests/:contestId/prepare`
- 회사 공모전 요청 내역/기업 공모전 관리
- 관리자 공모전 요청 관리

## 시나리오 6. 신고 → 관리자 처리 → 제재 → 알림

- 신고 대상: `[CDD] 운영 정책 검토용 숨김 게시글`
- 신고자: `cdd-reporter`
- 신고 상태: `ACCEPTED`
- 조치: `BLIND_POST`
- 제재 대상: `cdd-moderated`
- 제재: `TEMP_SUSPENDED`, `ACTIVE`, 10일 후 만료 예정
- 알림: 제재 대상 사용자에게 `USER_SANCTION` target으로 발송
- 감사/운영 로그: `CDD_*` action/event code로만 생성

기대 확인 화면:

- 관리자 신고 관리
- 관리자 회원/제재 관리
- 알림 패널

## 이미지와 파일 정책

이번 seed는 업로드 파일을 추가하지 않는다.

| 영역 | seed 값 | 화면 기대 |
|---|---|---|
| 프로필 이미지 | NULL | 기본 프로필 이미지 |
| 팀 대표 이미지 | NULL | 기본 팀 이미지 |
| 작업물 대표 이미지 | NULL | 기본 작업물 이미지 |
| 포트폴리오 썸네일 | NULL | 기본 포트폴리오 이미지 |
| 공모전 대표 이미지 | NULL | 기본 공모전 이미지 |

## 참조 경로

- `docu/prompt/connected_dummy_data_creator_prompt.md`
- `docu/07_database/database_baseline.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/dummy_data/validation_result.md`
- `docu/dummy_data/restore_guide.md`
- `sql/18_seed_connected_demo_data.sql`
- `sql/19_validate_connected_demo_data.sql`
- `sql/20_rollback_connected_demo_data.sql`
