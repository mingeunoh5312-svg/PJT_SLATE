# 2차 연관형 볼륨 더미 데이터 생성 프롬프트

## 사용 목적

현재 `slate` DB에는 1차 CDD 연관형 더미 데이터가 이미 반영되어 있다. 이번 작업은 기존 CDD 데이터를 수정하거나 재생성하지 않고, 화면이 더 풍성하게 보이도록 별도 namespace의 2차 볼륨 더미 데이터 초안을 만드는 작업이다.

이 프롬프트의 목표는 단순히 행 수를 늘리는 것이 아니라, 홈·매칭·팀·게시판·프로필·공모전·관리자 화면에서 실제 서비스처럼 보이는 “양이 있는 관계형 데이터 세트”를 설계하고 SQL/문서 초안을 만드는 것이다.

## 생성자에게 전달할 프롬프트

```text
당신은 Slate 프로젝트의 2차 연관형 볼륨 더미 데이터 생성 담당자입니다.

이미 적용된 1차 CDD 더미 데이터는 유지하고, 별도의 CDV namespace로 추가 볼륨 데이터를 설계하세요. 이번 작업에서는 실제 DB에 데이터를 넣지 말고, SQL/문서 초안만 생성한 뒤 멈추세요.

작업 루트:
- /Users/mingeunoh/Documents/PJT_0624/Project_Slate/Slate

## 현재 전제

- 기존 DB명은 `slate`입니다.
- 1차 CDD 더미 데이터는 이미 DB에 적용되어 있습니다.
- CDD 데이터는 최종 검수를 통과한 상태입니다.
- 이번 작업은 CDD를 수정하지 않고 CDV 데이터를 추가하는 2차 볼륨 보강입니다.

## 가장 중요한 원칙

무작위 행을 많이 넣는 작업이 아닙니다.

계정 → 프로필 → 팔로우 → 팀 → 팀원 → 모집 → 슬롯 → 지원/초대 → 일정 → 작업물 승인 → 게시글 → 작업물 → 포트폴리오 → 공모전 → 저장/제출 준비/적합도 → 신고/제재 → 알림/로그의 관계가 실제 서비스처럼 자연스럽게 연결되어야 합니다.

외래키만 맞으면 완료가 아닙니다. 화면에 표시되는 상태, 집계값, 날짜, 역할, 원인과 결과가 서로 모순되지 않아야 합니다.

## 절대 금지

- 실제 DB에 INSERT, UPDATE, DELETE 실행 금지
- `sql/18_seed_connected_demo_volume_data.sql` 실행 금지
- dump 생성 금지
- 새 DB 생성 금지
- `sql/99_reset.sql` 실행 금지
- `DROP`, `TRUNCATE`, `FOREIGN_KEY_CHECKS = 0` 사용 금지
- 기존 CDD 데이터 수정/삭제 금지
- 기존 사용자 생성 계정, 크롤링 공모전, 업로드 이미지 경로 수정 금지
- 프런트엔드/백엔드 코드 수정 금지
- 실제 외부 서비스 URL, 실제 개인정보, 실제 비밀번호 사용 금지

읽기 전용 분석은 허용됩니다.

- schema 확인
- reference code 확인
- 현재 DB count 확인
- 기존 CDD count 확인

단, DB를 변경하는 작업은 이번 단계에서 하지 마세요.

## 먼저 읽을 파일

다음 순서로 읽으세요.

1. `Agent.md`
2. `docu/00_common/reference_policy.md`
3. `docu/07_database/database_baseline.md`
4. `docu/dummy_data/data_scenarios.md`
5. `docu/dummy_data/expected_changes.md`
6. `docu/dummy_data/validation_result.md`
7. `docu/dummy_data/test_accounts.md`
8. `docu/dummy_data/restore_guide.md`
9. `sql/01_schema.sql`
10. `sql/02_seed_reference.sql`
11. `sql/15_seed_connected_demo_data.sql`
12. `sql/16_validate_connected_demo_data.sql`
13. `sql/17_rollback_connected_demo_data.sql`
14. `frontend/src/constants/defaultImages.js`
15. 관련 backend mapper/service:
    - `backend/src/main/java/com/slate/profiles`
    - `backend/src/main/java/com/slate/follows`
    - `backend/src/main/java/com/slate/teams`
    - `backend/src/main/java/com/slate/matching`
    - `backend/src/main/java/com/slate/boards`
    - `backend/src/main/java/com/slate/contests`
    - `backend/src/main/java/com/slate/moderation`
    - `backend/src/main/java/com/slate/notifications`
    - `backend/src/main/resources/mappers`

## 새로 생성할 파일

아래 파일을 새로 만드세요.

- `sql/18_seed_connected_demo_volume_data.sql`
- `sql/19_validate_connected_demo_volume_data.sql`
- `sql/20_rollback_connected_demo_volume_data.sql`
- `docu/dummy_data/volume_data_scenarios.md`
- `docu/dummy_data/volume_expected_changes.md`

필요하면 아래 문서도 추가할 수 있습니다.

- `docu/dummy_data/volume_review_checklist.md`

이번 단계에서는 적용 후 dump, validation result, test account result는 만들지 마세요. 실제 DB 적용 후 별도 단계에서 작성합니다.

## namespace 규칙

기존 CDD와 완전히 분리된 CDV namespace를 사용하세요.

- 계정 login_id: `cdv-*`
- 제목 prefix: `[CDV]`
- 외부 식별자: `CDV-*`
- 포트폴리오 외부 소스: `SLATE_CDV`
- 감사/운영 로그 action/event code: `CDV_*`
- 테스트 이메일: `@slate.test`
- 공통 로컬 비밀번호: `{noop}slate1234`

기존 CDD 데이터를 참조하거나 수정하지 않는 것을 기본 원칙으로 합니다. CDV끼리만 관계가 완성되도록 설계하세요.

## 목표 데이터 규모

아래는 권장 목표량입니다. 현재 스키마와 작성 난이도를 고려해 약간 조정할 수 있지만, 조정 이유를 문서에 남기세요.

| 영역 | 목표량 |
|---|---:|
| CDV 계정 | 32~40개 |
| USER 프로필 | 28~34개 |
| COMPANY 계정 | 3~5개 |
| 팀 | 10~14개 |
| 모집 공고 | 18~28개 |
| 모집 슬롯 | 45~70개 |
| 지원/초대 | 70~110건 |
| 팀 일정 | 40~70건 |
| 팔로우 | 80~140건 |
| 매칭 북마크/액션 로그 | 80~140건 |
| 게시글 | 45~70개 |
| 게시글 리뷰/대댓글 | 120~220개 |
| 게시글 좋아요 | 180~350개 |
| 작업물 | 25~45개 |
| 포트폴리오 | 50~90개 |
| 팀 작업물 승인 요청 | 20~35건 |
| 공모전 요청 | 4~8개 |
| 공모전 | 20~35개 |
| 공모전 저장/제출 준비/적합도 캐시 | 120~250건 |
| 신고/제재 | 8~18건 |
| 알림 | 120~240건 |

너무 많은 데이터로 seed가 불안정해지면 안 됩니다. 목표량보다 중요한 것은 관계 정합성입니다.

## 화면별 보강 목표

### 1. 홈 화면

- 최근 작업물 카드가 충분히 보이도록 작업물/게시글/좋아요/조회수를 다양화합니다.
- 읽지 않은 알림, 팀 활동, 공모전 마감 임박, 저장 공모전이 풍성하게 보이도록 합니다.
- USER, COMPANY, ADMIN 화면에서 각각 확인할 데이터가 있어야 합니다.

### 2. 매칭 화면

- 역할, 장르, 지역, 경험, 합류 가능 시점, 협업 조건이 다양해야 합니다.
- 팀원 찾기와 팀 찾기에서 결과가 너무 적지 않아야 합니다.
- 북마크, 지원 대기, 초대 대기, 수락 완료 상태가 섞여 있어야 합니다.
- 팀별 모집 슬롯이 실제 팀 상태와 맞아야 합니다.

### 3. 팀 화면

- 모집 중, 진행 중, 모집 완료, 종료 예정, 종료 팀을 적절히 섞습니다.
- 각 팀은 active leader가 정확히 1명이어야 합니다.
- `current_member_count`는 active team_member 수와 일치해야 합니다.
- ACCEPTED 지원/초대 대상자는 실제 active team member여야 합니다.
- PENDING 지원/초대 대상자는 아직 해당 팀의 active member가 아니어야 합니다.
- 종료 팀은 open recruitment/slot이 없어야 하고 closure snapshot이 있어야 합니다.

### 4. 게시판/작업물/포트폴리오

- WORK 게시글과 FREE 게시글을 모두 충분히 만듭니다.
- WORK 게시글은 가능한 경우 work_item과 연결합니다.
- 팀 작업물 승인 요청은 requester가 해당 팀 active member여야 합니다.
- 승인된 팀 작업물 요청은 board_post/work_item과 연결합니다.
- `like_count`, `review_count`, `view_count`는 실제 like/review/view 데이터와 맞아야 합니다.
- 포트폴리오는 사용자의 role/profile과 자연스럽게 연결합니다.
- 실제 KOBIS 검증으로 오해될 데이터는 만들지 마세요.

### 5. 공모전

- COMPANY 계정의 공모전 개설 요청과 승인된 공모전을 만듭니다.
- 공모전 대상, 지역, 주최 유형, 마감일을 다양화합니다.
- 저장, 제출 준비, 적합도 캐시가 team/profile 기준으로 충분히 있어야 합니다.
- `contest.save_count`는 실제 `contest_save`와 일치해야 합니다.

### 6. 관리자/운영

- 신고 PENDING/ACCEPTED/REJECTED가 섞여야 합니다.
- ACCEPTED 신고는 실제 moderation action, blind post/review, sanction, notification과 연결합니다.
- 제재 대상 계정 상태와 `user_sanction` 상태가 모순되지 않아야 합니다.
- audit_log/operation_log는 원인 데이터와 연결되는 CDV 코드로만 만듭니다.

## 이미지 정책

이번 CDV seed는 새 업로드 파일을 만들지 않습니다.

이미지 경로 컬럼은 기본적으로 `NULL`로 둡니다.

- `member_profile.profile_image_path`
- `team.representative_image_path`
- `work_item.representative_image_path`
- `portfolio_item.thumbnail_image_path`
- `contest.representative_image_path`
- `contest_open_request.representative_image_path`

목표는 화면의 default 이미지 fallback을 더 많은 데이터에서 확인하는 것입니다.

단, 기존 사용자 업로드 이미지나 크롤링 이미지 경로를 지우거나 default 이미지 경로로 덮어쓰면 안 됩니다.

## SQL 작성 규칙

### seed SQL

`sql/18_seed_connected_demo_volume_data.sql`은 다음 원칙을 지키세요.

- `SET NAMES utf8mb4;`
- `USE slate;`
- `START TRANSACTION;` / `COMMIT;`
- CDV namespace만 정리한 뒤 재삽입하는 방식으로 멱등성을 확보합니다.
- 기존 CDD namespace는 삭제/수정하지 않습니다.
- 고정 숫자 ID에 의존하지 말고 login_id, title, external_reference_id 등 자연키로 변수에 담아 사용합니다.
- reference 데이터는 `role.name`, `genre.name`, `region.public_display_name` 등으로 조회합니다.
- FK 검사를 끄지 않습니다.
- 집계값은 실제 관계 데이터 삽입 후 재계산합니다.
- 삭제 블록은 CDV namespace에만 작동해야 합니다.

### validation SQL

`sql/19_validate_connected_demo_volume_data.sql`은 최소한 아래를 검증하세요.

- 예상 count와 실제 count
- 상태별 분포
- 필수 CDV ID 누락 여부
- team leader active mismatch
- active leader count not one
- team current_member_count mismatch
- slot accepted_count mismatch
- accepted application/invitation missing active member
- pending application/invitation already active member
- ended team open recruitment/slot
- closure snapshot missing
- board like/review count mismatch
- work/post link mismatch
- approved team work request unlinked
- team work request requester not active member
- contest save_count mismatch
- contest request/contest link mismatch
- self follow
- duplicate pending application/invitation issue
- CDV image path should be null
- notification bad target
- CDV가 CDD 데이터를 수정했는지 의심되는 항목

모든 zero-error check는 기대값 0이어야 합니다.

### rollback SQL

`sql/20_rollback_connected_demo_volume_data.sql`은 CDV namespace만 제거해야 합니다.

삭제 순서는 FK 관계를 고려하세요.

rollback 대상:

- `login_id LIKE 'cdv-%'`
- title prefix `[CDV]`
- external id `CDV-*`
- external source `SLATE_CDV`
- action/event code `CDV_*`

CDD 데이터는 rollback 대상이 아닙니다.

## 문서 작성 규칙

### `docu/dummy_data/volume_data_scenarios.md`

다음을 정리하세요.

- CDV 데이터 설계 원칙
- 계정 그룹
- 팀/모집/지원/초대 시나리오
- 게시판/작업물/포트폴리오 시나리오
- 공모전 시나리오
- 관리자/신고/제재/알림 시나리오
- 화면별 확인 포인트
- 이미지 fallback 확인 포인트

### `docu/dummy_data/volume_expected_changes.md`

다음을 정리하세요.

- 현재 DB 요약
- 기존 CDD 데이터 유지 여부
- CDV namespace 예상 INSERT 건수
- 예상 UPDATE 건수
- 예상 DELETE 건수
- 기존 데이터 수정 여부
- 업로드 파일 계획
- 적용 전 백업 계획
- 적용/검증/rollback 계획
- 남은 리스크

## 작업 완료 전 자체 점검

아래 검증을 실행하고 결과를 보고하세요.

```bash
git diff --check -- sql/18_seed_connected_demo_volume_data.sql sql/19_validate_connected_demo_volume_data.sql sql/20_rollback_connected_demo_volume_data.sql docu/dummy_data/volume_data_scenarios.md docu/dummy_data/volume_expected_changes.md
```

금지어 검색:

```bash
rg -n "FOREIGN_KEY_CHECKS|DROP\\s+|TRUNCATE|99_reset|CREATE\\s+DATABASE|LOAD DATA|INTO OUTFILE|GRANT\\s|REVOKE\\s" sql/18_seed_connected_demo_volume_data.sql sql/19_validate_connected_demo_volume_data.sql sql/20_rollback_connected_demo_volume_data.sql
```

주의:

- `DROP`이 dump 파일이 아닌 seed/validate/rollback SQL에 있으면 안 됩니다.
- rollback SQL도 `DROP/TRUNCATE`가 아니라 namespace별 `DELETE`만 사용해야 합니다.

## 완료 보고 형식

완료 시 아래 형식으로 보고하세요.

```md
## 2차 볼륨 더미 데이터 초안 완료 보고

### 생성 파일
- ...

### 목표 규모
- CDV 계정:
- 팀:
- 게시글:
- 작업물:
- 공모전:
- 알림:

### 주요 시나리오
- ...

### 기존 데이터 보호
- CDD 수정 여부:
- 기존 사용자/크롤링 데이터 수정 여부:
- 이미지 경로 정책:

### 검증 SQL
- expected count 검증 포함 여부:
- zero-error 검증 포함 여부:
- rollback 준비 여부:

### 실행한 정적 검증
- git diff --check:
- 금지어 검색:

### 미실행/남은 리스크
- 실제 DB 적용:
- 브라우저 검증:
- dump 생성:
```

## 완료 조건

- CDV volume seed/validate/rollback SQL 초안이 생성됨
- CDV 시나리오 문서와 예상 변경 문서가 생성됨
- 기존 CDD 데이터와 기존 사용자/크롤링 데이터가 수정되지 않도록 설계됨
- 모든 이미지 경로 정책이 default fallback 검증 방향과 맞음
- 실제 DB에는 아무 변경도 적용하지 않음
```
