# 연관형 더미 데이터·공동 작업자 전달 DB 생성 프롬프트

## 사용 목적

현재 `slate` MySQL 데이터베이스를 새로 만들거나 초기화하지 않고, 기존 데이터와 화면을 분석하여 부족한 더미 데이터를 유기적인 서비스 시나리오로 보강한다.

이 프롬프트는 분석, 데이터 설계, SQL 초안, 사용자 승인, 백업, 실제 적용, 무결성 검증, 화면 검증, 최종 DB dump와 공동 작업자용 전달 문서 생성까지 한 번에 맡기기 위한 통합 프롬프트다.

## 생성자에게 전달할 프롬프트

```text
당신은 Slate 프로젝트의 연관형 더미 데이터 생성 및 공동 작업자용 DB 전달본 제작 담당자입니다.

분석이나 SQL 제안에서 끝내지 말고 아래 단계에 따라 작업을 끝까지 수행하세요. 단, 현재 `slate` DB를 실제로 변경하기 직전에 반드시 한 번 사용자 승인을 받아야 합니다. 승인 전에는 읽기 전용 분석과 문서·SQL 초안 작성까지만 진행하세요.

작업 루트:
- <SLATE_ROOT>
- <SLATE_ROOT>는 현재 작업자의 `Project_Slate/Slate` 폴더입니다.

DB 기준:
- DBMS: MySQL 8
- 기존 DB명: `slate`
- 새 DB를 생성하지 않습니다.
- `slate_dummy`, 임시 복제 DB, 테스트 DB를 새로 만들지 않습니다.
- 기존 `slate` DB를 reset, drop, truncate 하지 않습니다.

## 가장 중요한 목표

이번 작업의 목표는 단순히 테이블별 행 개수를 늘리는 것이 아닙니다.

사용자 → 프로필 → 팔로우 → 팀 → 팀원 → 모집 공고 → 모집 슬롯 → 지원·초대 → 일정 → 작업물 → 게시글 → 포트폴리오 → 공모전 → 저장·제출 준비 → 신고·제재 → 알림·로그로 이어지는 관계가 실제 서비스 시나리오처럼 자연스럽고 일관되어야 합니다.

외래키만 통과한다고 완료가 아닙니다. 화면에 표시되는 상태, 집계값, 역할, 날짜, 원인과 결과 데이터까지 서로 모순이 없어야 합니다.

가능한 적은 데이터로 현재 화면과 주요 상태를 충분히 검증할 수 있는 “최소하지만 풍부한 연관 데이터 세트”를 만드세요. 비슷한 행을 무작정 수십·수백 건 생성하지 마세요.

## 먼저 읽을 문서

다음 순서로 읽으세요.

1. `Agent.md`
2. `docu/README.md`
3. `docu/00_common/reference_policy.md`
4. `docu/00_common/document_structure.md`
5. `docu/03_mvp_scope/mvp_decisions.md`
6. `docu/03_mvp_scope/mvp_scope.md`
7. `docu/07_database/database_baseline.md`
8. `docu/08_environment/local_setup.md`
9. `docu/08_environment/env_variables.md`
10. `docu/06_frontend/frontend_baseline.md`
11. `docu/05_backend/backend_baseline.md`
12. `docu/user_temp/todo_0624_dummies_data.md`
13. `docu/user_temp/todo_common_home_matching_team_image_requirements.md`
14. `docu/prompt/default_image_generation_prompts.md`
15. 최근 `docu/work_logs` 중 프로필, 팔로우, 홈, 매칭, 팀, 게시판, 공모전, 이미지 작업 기록

## 먼저 확인할 코드와 데이터

### SQL

- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`
- `sql/03_seed_sample_data.sql`
- `sql/05_seed_ai_matching_dummy_data.sql`
- `sql/07_seed_verified_portfolio_ui_demo.sql`
- `sql/08_portfolio_credit_name_schema.sql`
- `sql/09_entity_image_schema.sql`
- `sql/10_board_full_integration_schema.sql`
- `sql/11_board_search_genre_period_schema.sql`
- `sql/12_contest_image_schema.sql`
- `sql/13_contest_search_filter_schema.sql`
- `sql/14_remove_contest_benefit_extra_schema.sql`
- `sql/99_reset.sql`

### Backend

- `backend/src/main/java/com/slate/accounts`
- `backend/src/main/java/com/slate/profiles`
- `backend/src/main/java/com/slate/follows`
- `backend/src/main/java/com/slate/teams`
- `backend/src/main/java/com/slate/matching`
- `backend/src/main/java/com/slate/boards`
- `backend/src/main/java/com/slate/contests`
- `backend/src/main/java/com/slate/moderation`
- `backend/src/main/java/com/slate/notifications`
- `backend/src/main/java/com/slate/media`
- `backend/src/main/resources/mappers`

### Frontend

- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/views/HomeView.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/PublicProfileView.vue`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/views/BoardView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/AdminView.vue`
- `frontend/src/constants/defaultImages.js`

### 현재 DB와 파일 저장소

- 현재 `slate` DB의 테이블별 건수와 상태별 건수를 읽기 전용으로 확인합니다.
- FK, unique key, generated column, nullable 조건을 확인합니다.
- `backend/uploads` 또는 실제 `SLATE_UPLOAD_DIR`의 파일과 DB 경로가 일치하는지 확인합니다.
- 실제 secret, 비밀번호, API key, `.env` 값을 출력하거나 문서화하지 않습니다.
- 저장된 MySQL login-path가 있으면 그것을 사용하되 login-path 이름 외 인증 정보를 기록하지 않습니다.

## 절대 금지

- 새 데이터베이스 생성
- `slate` DB drop/reset/truncate
- `sql/99_reset.sql` 실행
- 기존 사용자의 실제 입력 데이터를 이유 없이 삭제하거나 덮어쓰기
- 사용자 승인 전 INSERT, UPDATE, DELETE, dump 생성, 업로드 파일 변경
- 승인 전 DB에 영향을 주는 API mutation 실행
- FK 검사를 끈 상태로 데이터 삽입
- 고정 숫자 ID가 현재 DB에서도 같다고 가정
- 존재하지 않는 이미지 파일 경로 저장
- 기본 프런트 이미지 경로를 DB의 업로드 이미지 경로로 저장
- `example.test`처럼 실제 표시에서 깨지는 가짜 외부 이미지 URL 추가
- 실제 개인정보, 실제 연락처, 실제 사업자등록번호, 실제 비밀번호, 실제 API key 사용
- 실제 KOBIS 검증이 아닌 데이터를 실제 검증 성공처럼 위장
- 원인 데이터가 없는 알림, 감사 로그, 운영 로그 생성
- 사용자의 관련 없는 작업 트리 변경을 되돌리거나 정리
- dump, 업로드 압축 파일, secret을 Git stage 또는 commit

## 전체 작업 절차

### 1단계: 현재 상태 읽기 전용 분석

현재 코드, schema, 기존 seed, 실제 `slate` DB를 분석하세요.

다음을 표로 정리합니다.

- 테이블별 현재 건수
- 주요 상태별 건수
- 현재 데이터로 이미 검증 가능한 화면
- 데이터가 없거나 편중되어 검증하기 어려운 화면과 상태
- 깨진 관계, 잘못된 집계값, 고아 데이터
- DB 이미지 경로는 있으나 실제 파일이 없는 항목
- 외부 이미지 URL이 비어 있거나 유효하지 않은 항목
- 기존 데이터 중 보존해야 할 사용자 입력 데이터

더미 데이터가 필요한지 판단할 때 “테이블이 비어 있다”만 보지 말고 실제 frontend와 backend가 해당 데이터를 읽고 표시하는지 확인하세요.

### 2단계: 연관 데이터 시나리오 설계

서로 독립된 행 목록이 아니라 4~7개 정도의 대표 서비스 시나리오를 설계하세요.

예시 범위:

1. 모집 중인 단편 제작팀과 조건이 맞는 지원자
2. 팀장의 초대가 수락되어 실제 팀원이 된 흐름
3. 지원 거절·취소·만료 상태를 확인할 수 있는 흐름
4. 일정이 TODO → IN_PROGRESS → DONE으로 진행되는 팀
5. 정상 종료 또는 해체되어 closure snapshot이 존재하는 팀
6. 팀 작업물 승인 후 게시글·작업물·포트폴리오로 이어지는 흐름
7. 사용자 또는 팀과 장르·역할이 맞는 공모전 저장·제출 준비 흐름
8. 게시글 신고 → 관리자 처리 → 필요 시 제재·알림으로 이어지는 흐름
9. 회사 공모전 요청 → 승인 → 실제 공모전 생성으로 이어지는 흐름

모든 예시를 억지로 구현할 필요는 없습니다. 현재 DB와 화면에서 부족한 상태를 가장 적은 데이터로 채울 수 있는 시나리오를 선택하세요.

각 시나리오 문서에는 다음을 포함합니다.

- 등장 계정과 계정 유형
- 프로필 역할·장르·지역·경력
- 소속 팀과 팀 내 역할
- 모집 역할과 지원·초대 상태
- 게시글·작업물·포트폴리오 연결
- 공모전과 저장·제출 준비 연결
- 발생 알림과 로그의 원인
- 기대하는 화면과 확인 방법

### 3단계: 반드시 지킬 관계·불변 조건

#### 계정·프로필·팔로우

- 일반 사용자 프로필은 해당 `user_account.user_id`와 1:1로 연결합니다.
- 공개 매칭 후보는 ACTIVE 계정, ACTIVE 프로필, PUBLIC 공개 범위, VISIBLE 활동 상태, 완료된 프로필 조건을 만족해야 합니다.
- 프로필의 역할, 장르, 협업 조건은 소개와 포트폴리오 내용에 어울려야 합니다.
- 팔로우는 자기 자신을 대상으로 하지 않으며 중복되지 않아야 합니다.
- 팔로워·팔로잉 수는 실제 `user_follow` 관계로 계산되어야 합니다.

#### 회사·관리자

- 회사 계정 상태와 `company_application.status`가 모순되지 않아야 합니다.
- 승인된 회사만 승인 회사 전용 기능과 공모전 관리 시나리오에 사용합니다.
- 회사 승인·거절 데이터에는 실제 관리자와 검토 시각·사유가 있어야 합니다.
- 회사 서류 경로를 넣는다면 실제 파일이 전달 패키지에 존재해야 합니다. 파일을 준비하지 않는다면 문서 경로를 만들지 마세요.
- 관리자 작업은 필요한 권한을 가진 ACTIVE 관리자 계정과 연결합니다.

#### 팀·팀원

- `team.leader_user_id`는 해당 팀의 ACTIVE `LEADER` 팀원이어야 합니다.
- 한 팀에는 ACTIVE LEADER가 정확히 1명이어야 합니다.
- ACTIVE 팀원 수와 `team.current_member_count`가 일치해야 합니다.
- 현재 인원은 최대 인원을 초과할 수 없습니다.
- 팀 장르, 설명, 지역, 기간, 모집 역할은 서로 자연스럽게 연결되어야 합니다.
- 탈퇴 팀원은 `status`, `left_at`이 일관되어야 하며 현재 인원에 포함하지 않습니다.

#### 모집·지원·초대

- 모집 공고는 해당 팀에 속하고 생성자는 팀장 또는 허용된 팀 관계자여야 합니다.
- 모집 슬롯의 역할은 팀 설명과 실제 필요 역할에 맞아야 합니다.
- `accepted_count`는 실제 수락된 지원·초대와 팀원 반영 결과를 기준으로 일치시킵니다.
- `accepted_count <= required_count`를 지킵니다.
- 정원이 찬 슬롯은 OPEN 상태로 방치하지 않습니다.
- PENDING 지원·초대의 generated unique key 중복을 만들지 않습니다.
- ACCEPTED 지원자는 실제 ACTIVE 팀원으로 연결되어야 합니다.
- ACCEPTED 초대 대상도 실제 ACTIVE 팀원으로 연결되어야 합니다.
- REJECTED에는 적절한 결정자, 결정 시각, 사유를 넣습니다.
- CANCELED는 사용자 취소 흐름과 맞아야 합니다.
- EXPIRED는 모집 마감 또는 초대 유효 기간과 논리적으로 어울려야 합니다.
- 팀장 자신에게 지원 또는 초대하는 데이터를 만들지 않습니다.

#### 팀 계획·종료

- 일정 담당자는 해당 팀의 ACTIVE 팀원이거나 NULL이어야 합니다.
- TODO, IN_PROGRESS, DONE, HOLD, CANCELED 상태와 due date가 모순되지 않아야 합니다.
- 기한 초과 일정은 DONE 또는 CANCELED가 아닌 상태로 과거 due date를 가질 수 있습니다.
- ENDED 팀은 `end_type`과 `team_closure_snapshot`을 가져야 합니다.
- 종료 팀의 OPEN 모집 공고·슬롯은 닫혀 있어야 합니다.
- 종료 snapshot JSON은 실제 종료 당시 팀·멤버·모집·일정 정보와 논리적으로 일치해야 합니다.

#### 게시판·작업물

- WORK 게시글과 연결된 `work_item.board_post_id`가 일치해야 합니다.
- 작업물 소유자, 게시글 작성자, 팀 소속 관계가 자연스러워야 합니다.
- 팀 작업물 승인 요청이 APPROVED라면 생성된 게시글·작업물과 연결합니다.
- 승인 대기·거절·취소 데이터는 결과 게시글이나 작업물이 잘못 생성되지 않아야 합니다.
- 작업물 장르는 작품 설명과 팀 장르에 어울려야 합니다.
- `board_post.like_count`는 활성 `board_like` 수와 일치해야 합니다.
- `board_post.review_count`는 공개 상태 리뷰 수와 일치해야 합니다.
- 답글의 `parent_review_id`는 같은 게시글의 리뷰를 가리켜야 합니다.
- 조회수와 `board_view_log`는 완전히 동일할 필요가 있는지 현재 서비스 계산 방식을 확인하고, 문서화된 기준을 따릅니다.

#### 포트폴리오·검증

- 포트폴리오는 소유 프로필의 역할과 실제 작품 참여 내용에 어울려야 합니다.
- 작업물과 포트폴리오를 같은 작품으로 표현하면 제목, 역할, 설명, 링크가 서로 모순되지 않아야 합니다.
- 사용자 입력 `credit_name`과 외부 제공자 인물명은 의미를 구분합니다.
- 실제 KOBIS 응답으로 검증하지 않은 데이터는 실제 VERIFIED로 만들지 않습니다.
- UI 전용 fixture가 필요하면 `KOBIS_UI_FIXTURE`처럼 명확히 표시하고 실제 검증과 구분합니다.
- `thumbnail_image_path`를 넣는 경우 실제 파일이 있어야 합니다.
- 외부 또는 YouTube 썸네일은 `thumbnail_url`에 저장하고, 업로드 이미지보다 낮은 우선순위로 사용합니다.

#### 공모전

- 회사 요청 공모전은 승인된 회사 계정과 연결합니다.
- APPROVED 요청은 실제 `contest`와 `approved_contest_id`, `source_request_id`로 양방향 추적 가능해야 합니다.
- 공모전 저장 수 `save_count`는 실제 `contest_save` 수와 일치해야 합니다.
- `contest_submission_prepare`의 사용자, contest, basis가 실제로 존재해야 합니다.
- TEAM basis를 사용하면 해당 사용자가 그 팀과 관계가 있는지 현재 서비스 정책을 확인합니다.
- `contest_fit_cache`의 기준 프로필·팀, 장르·역할·지역과 추천 사유가 실제 데이터에 맞아야 합니다.
- OPEN/ENDED 상태와 마감일이 모순되지 않아야 합니다.
- 구조화 대상·지역·상금 값은 텍스트 설명과 일치해야 합니다.
- 외부 크롤링 공모전은 출처와 외부 URL을 명확히 구분합니다.

#### 신고·제재·알림·로그

- 신고 대상 게시글 또는 리뷰는 실제로 존재해야 합니다.
- ACCEPTED 신고는 관리자 조치, 검토자, 검토 시각, 처리 메모와 일관되어야 합니다.
- 제재가 있으면 대상 사용자, 사유, 기간, 생성 관리자와 상태가 맞아야 합니다.
- REVOKED 또는 EXPIRED 제재는 해제·만료 시각과 상태가 맞아야 합니다.
- 알림은 실제 지원, 초대, 팀 종료, 공모전 승인, 신고 처리 등 원인 데이터와 연결합니다.
- 알림의 `target_type`, `target_id`는 화면 이동이 가능한 실제 대상이어야 합니다.
- 알림 delivery batch가 있으면 recipient/sent/chunk 집계가 실제 생성 알림과 맞아야 합니다.
- 감사 로그와 운영 로그는 실제로 수행된 더미 시나리오 사건만 기록합니다.

#### 이미지·파일

- 이미지 선택 우선순위는 현재 frontend 구현을 따릅니다.
  - 프로필: 업로드 프로필 이미지 → 기본 이미지
  - 팀: 업로드 대표 이미지 → 기본 이미지
  - 작업물: 업로드 대표 이미지 → YouTube 썸네일 → 기본 이미지
  - 포트폴리오: 업로드 썸네일 → 외부/YouTube 썸네일 → 기본 이미지
  - 공모전: 업로드 이미지 → 요청 이미지 → 크롤링/외부 대표 이미지 → 기본 이미지
- 기본 이미지는 `frontend/src/assets/defaults`에 있으며 DB 경로로 저장하지 않습니다.
- DB 이미지 경로가 non-null이면 실제 업로드 루트 아래 파일이 반드시 존재해야 합니다.
- 최종 dump에 파일 바이너리는 포함되지 않으므로 필요한 업로드 파일은 별도 압축본과 manifest로 전달합니다.
- 실제 파일을 제공하지 않을 경우 DB의 업로드 경로 컬럼을 NULL로 두어 기본 이미지가 표시되게 합니다.
- 크롤링·외부 이미지 URL을 넣을 경우 HTTP(S) URL이어야 하며 적용 시점에 실제 로딩을 확인합니다.
- 깨진 외부 URL이나 존재하지 않는 로컬 경로를 “이미지 있음” 상태로 남기지 않습니다.

### 4단계: SQL 초안과 검증 SQL 작성

승인 전에 다음 파일의 초안을 작성할 수 있습니다.

- `sql/15_seed_connected_demo_data.sql`
- `sql/16_validate_connected_demo_data.sql`
- `sql/17_rollback_connected_demo_data.sql`
- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/expected_changes.md`

seed SQL 기준:

- 기존 schema를 변경하지 않는 DML 중심 SQL로 작성합니다.
- schema 변경이 정말 필요하면 seed에 섞지 말고 사용자 승인 보고에 별도 항목으로 올립니다.
- 전체 DML은 가능한 한 하나의 명시적 transaction으로 묶습니다.
- 자연키 또는 전용 fixture 식별자를 사용해 현재 auto increment ID에 의존하지 않습니다.
- 계정 login ID, 외부 식별자, 제목에는 충돌을 피할 수 있는 일관된 demo namespace를 사용합니다.
- INSERT 후 생성 ID가 필요하면 `SELECT`와 SQL 변수를 사용합니다.
- 재실행해도 중복 생성되지 않도록 멱등성을 확보합니다.
- `INSERT IGNORE`로 오류를 숨기지 말고, 중복을 의도적으로 허용하는 이유가 있을 때만 사용합니다.
- 기존 데이터 변경이 필요하면 대상과 이유를 `expected_changes.md`에 행 단위 또는 조건 단위로 명시합니다.
- 집계값은 마지막에 실제 관계 테이블을 기준으로 재계산합니다.
- 실행 중 오류가 나면 transaction이 rollback되어야 합니다.
- 실행 날짜에 따라 필요한 상대 날짜는 `NOW()`, `CURRENT_DATE`, `INTERVAL`을 사용합니다.
- 공동 작업자가 seed를 나중에 다시 실행해도 OPEN 공모전과 모집 일정이 즉시 만료되지 않게 합니다.

rollback SQL 기준:

- 이번 seed가 생성한 전용 namespace 데이터만 역순으로 삭제합니다.
- 기존 데이터는 삭제하지 않습니다.
- seed가 기존 행을 수정한다면 변경 전 값을 안전하게 복구할 수 있는 방식이 있어야 합니다.

검증 SQL 기준:

- 결과가 0이어야 하는 오류 검증과 기대 건수를 확인하는 검증을 구분합니다.
- 최소한 다음을 검증합니다.
  - FK 고아 데이터
  - 중복 관계
  - 자기 팔로우
  - 팀장과 ACTIVE LEADER 불일치
  - 팀 현재 인원 집계 불일치
  - 슬롯 accepted_count 불일치 또는 정원 초과
  - ACCEPTED 지원·초대와 ACTIVE 팀원 불일치
  - 종료 팀의 OPEN 모집
  - 종료 팀 snapshot 누락
  - 게시글 like/review 집계 불일치
  - WORK 게시글과 work_item 연결 불일치
  - 공모전 save_count 불일치
  - 공모전 요청 승인 연결 불일치
  - 알림 target 고아 데이터
  - 업로드 이미지·파일 DB 경로와 실제 파일 manifest 불일치
  - fixture namespace별 기대 건수

### 5단계: DB 변경 직전 단 한 번의 승인 요청

분석, 시나리오 설계, SQL 초안, 검증 SQL 초안을 완료한 뒤 작업을 멈추고 사용자에게 아래 내용을 한 번에 보고하세요.

보고 형식:

1. 현재 DB 핵심 현황
2. 선택한 서비스 시나리오
3. 테이블별 예상 INSERT 건수
4. 테이블별 예상 UPDATE 건수
5. 기존 데이터 중 수정되는 행과 수정 이유
6. 생성·수정할 SQL 및 문서 파일
7. 추가하거나 패키징할 업로드 파일
8. 발견한 기존 데이터 오류와 처리 제안
9. 백업 명령과 저장 위치
10. 적용 후 검증 계획

마지막에 다음 취지로 명시적 승인을 요청하세요.

“위 계획으로 현재 `slate` DB를 백업한 뒤 더미 데이터를 실제 적용하고 최종 전달본을 생성해도 될까요?”

중요:

- 사용자의 명시적 승인 전에는 DB INSERT, UPDATE, DELETE를 실행하지 않습니다.
- 승인 전에는 mysqldump와 업로드 파일 복사·압축도 실행하지 않습니다.
- 추가 질문을 여러 차례 나누지 말고, 합리적인 기본안을 세워 승인 요청 한 번에 필요한 내용을 모두 포함합니다.

### 6단계: 승인 후 백업

사용자가 승인하면 DB 변경보다 먼저 백업합니다.

권장 전달 작업 경로:

- `<SLATE_ROOT>/database_delivery/YYYY-MM-DD/`

이 디렉터리는 Git stage/commit 대상이 아닙니다. 필요하면 `.gitignore`에 `database_delivery/`를 추가하세요.

사전 백업 파일:

- `slate_before_connected_dummy.sql`
- `slate_before_connected_dummy.sql.sha256`

mysqldump는 가능한 경우 다음 성격의 옵션을 사용합니다.

- `--single-transaction`
- `--routines`
- `--triggers`
- `--events`
- `--hex-blob`
- `--default-character-set=utf8mb4`
- `--no-tablespaces`
- `--databases slate`

명령에 실제 비밀번호를 직접 쓰거나 로그에 노출하지 마세요. 저장된 login-path 또는 안전한 로컬 인증 방식을 사용하세요.

백업 후 다음을 확인합니다.

- 명령 성공
- dump 파일 크기가 0이 아님
- SHA-256 생성
- dump에 `slate` schema와 주요 테이블 정의·데이터가 포함됨

업로드 파일을 변경하거나 추가할 예정이면 적용 전 업로드 디렉터리 manifest 또는 별도 백업도 만드세요.

### 7단계: 실제 seed 적용

- 승인된 `sql/15_seed_connected_demo_data.sql`만 실행합니다.
- 적용 전에 다시 DB명이 `slate`인지 확인합니다.
- 다른 DB에 적용하지 않습니다.
- 실행 오류가 발생하면 즉시 중단하고 rollback 상태를 확인합니다.
- 임의로 SQL을 부분 실행해 성공한 것처럼 만들지 않습니다.
- 적용 후 테이블별 before/after 건수를 기록합니다.
- seed를 두 번째 실행하여 멱등성을 확인합니다. 두 번째 실행 후 중복 데이터나 예기치 않은 변경이 없어야 합니다.

### 8단계: 관계·집계·파일 검증

- `sql/16_validate_connected_demo_data.sql`을 실행합니다.
- 오류 검증 결과는 모두 0이어야 합니다.
- 기대 건수 검증은 설계 문서와 일치해야 합니다.
- `CHECK TABLE` 또는 필요한 MySQL 무결성 확인을 실행합니다.
- DB 이미지 경로와 실제 파일 존재 여부를 별도 manifest로 확인합니다.
- 외부 이미지 URL을 사용했다면 실제 HTTP(S) 로딩을 확인하고 실패 URL을 남기지 않습니다.

검증 실패 시:

- 원인을 분석합니다.
- seed 또는 데이터만 수정합니다.
- 사용자 승인 범위를 넘는 schema 변경은 하지 않습니다.
- rollback 후 수정 seed를 재적용할지, 현재 transaction 내에서 안전하게 수정할지 판단 근거를 기록합니다.
- 실패 사실과 수정 내역을 숨기지 않습니다.

### 9단계: backend·frontend·브라우저 검증

최소 검증:

```text
cd backend
mvn test
```

```text
cd frontend
npm run build
```

서버가 실행 가능한 환경이면 대표 계정과 주요 화면을 smoke 검증합니다.

대표 확인 화면:

- 일반 사용자 홈
- 내 프로필과 공개 프로필
- 팔로워·팔로잉
- 팀원 찾기와 팀 찾기
- 저장한 팀, 초대한 팀원, 지원한 팀
- 팀 목록·상세·모집·일정·종료
- 게시판 HOME·WORK·FREE·POPULAR·검색·상세
- 공모전 목록·저장·상세·제출 준비
- 회사 공모전 요청·관리
- 관리자 회사 승인·신고·제재·알림·팀·게시글·공모전 관리

화면 검증 기준:

- 더미 데이터가 의도한 시나리오와 상태로 보임
- 링크와 상세 대상이 실제 존재함
- 사용자 업로드 이미지 또는 크롤링 이미지가 있으면 기본 이미지 대신 실제 이미지가 표시됨
- 이미지가 없거나 로딩에 실패한 경우에만 기본 이미지가 표시됨
- 콘솔 오류 없음
- 주요 API 오류 없음

모든 화면을 억지로 수동 확인할 필요는 없습니다. 데이터 시나리오별 핵심 화면을 우선 확인하고 미검증 범위를 문서에 남기세요.

### 10단계: 공동 작업자용 최종 전달본 생성

모든 검증이 통과한 뒤 다음 파일을 생성합니다.

DB:

- `database_delivery/YYYY-MM-DD/slate_complete.sql`
- `database_delivery/YYYY-MM-DD/slate_complete.sql.sha256`

업로드 파일이 실제로 필요한 경우:

- `database_delivery/YYYY-MM-DD/slate_uploads.tar.gz`
- `database_delivery/YYYY-MM-DD/slate_uploads.tar.gz.sha256`
- `database_delivery/YYYY-MM-DD/uploads_manifest.tsv`

업로드 경로 데이터가 하나도 없다면 빈 압축 파일을 만들지 말고 README에 “별도 업로드 파일 없음”이라고 기록합니다.

공동 작업자 문서:

- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/test_accounts.md`
- `docu/dummy_data/validation_result.md`
- `docu/dummy_data/restore_guide.md`
- `database_delivery/YYYY-MM-DD/README.md`

작업 로그:

- `docu/work_logs/YYYY-MM-DD_dummy_data_connected_delivery.md`

최종 README에 포함할 내용:

- MySQL 8 요구사항
- 문자셋 `utf8mb4`
- DB명 `slate`
- dump 복원 명령 예시
- seed만 다시 적용하는 방법
- 테스트 계정 목록과 공통 데모 비밀번호 정책
- 계정별 확인 가능한 기능
- 업로드 압축 파일 복원 위치
- 환경변수나 secret은 포함되지 않았다는 안내
- dump와 업로드 압축 파일의 SHA-256 확인 방법
- 생성 시점
- 상대 날짜 seed와 snapshot dump의 차이
- 알려진 제한과 미검증 항목

복원 예시는 실제 비밀번호를 포함하지 않습니다.

### 11단계: 최종 완료 보고

최종 보고에는 다음만 간결하게 정리합니다.

- 추가한 대표 시나리오
- 테이블별 주요 추가 건수
- 기존 데이터 수정 여부
- seed 2회 실행 결과
- 관계 검증 결과
- backend test 결과
- frontend build 결과
- 브라우저 smoke 결과
- 사전 백업 경로
- 최종 dump 경로
- 업로드 압축 파일 경로 또는 없음
- 공동 작업자 README 경로
- 남은 위험과 미검증 항목

DB 변경, dump 생성, 파일 패키징을 실제로 완료하지 않았다면 완료했다고 표현하지 마세요.
```

## 예상 산출물

```text
sql/
├── 15_seed_connected_demo_data.sql
├── 16_validate_connected_demo_data.sql
└── 17_rollback_connected_demo_data.sql

docu/dummy_data/
├── data_scenarios.md
├── expected_changes.md
├── test_accounts.md
├── validation_result.md
└── restore_guide.md

database_delivery/YYYY-MM-DD/
├── slate_before_connected_dummy.sql
├── slate_before_connected_dummy.sql.sha256
├── slate_complete.sql
├── slate_complete.sql.sha256
├── slate_uploads.tar.gz
├── slate_uploads.tar.gz.sha256
├── uploads_manifest.tsv
└── README.md
```

업로드 경로 데이터가 없다면 `slate_uploads.tar.gz`, checksum, manifest는 생략한다.
