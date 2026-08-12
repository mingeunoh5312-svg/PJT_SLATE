# 2차 연관형 볼륨 더미 데이터 초안 작업 로그

작성일: 2026-06-25
역할: creator

## 작업 범위

- 기존 CDD를 유지하는 별도 CDV namespace 설계
- CDV seed, validation, rollback SQL 초안 작성
- 볼륨 데이터 시나리오와 예상 변경 문서 작성
- 현재 DB 읽기 전용 count 확인
- SQL 금지어, 공백 오류, 괄호·문자열 균형, MySQL PREPARE 구문 검사

실제 DB 적용, rollback 실행, dump 생성, 프런트엔드·백엔드 수정은 범위에서 제외했다.

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/07_database/database_baseline.md`
- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/expected_changes.md`
- `docu/dummy_data/validation_result.md`
- `docu/dummy_data/test_accounts.md`
- `docu/dummy_data/restore_guide.md`
- `docu/prompt/connected_dummy_volume_data_creator_prompt.md`
- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`
- `sql/15_seed_connected_demo_data.sql`
- `sql/16_validate_connected_demo_data.sql`
- `sql/17_rollback_connected_demo_data.sql`
- `frontend/src/constants/defaultImages.js`
- 관련 backend service, mapper interface, MyBatis mapper

## 생성 파일

- `sql/18_seed_connected_demo_volume_data.sql`
- `sql/19_validate_connected_demo_volume_data.sql`
- `sql/20_rollback_connected_demo_volume_data.sql`
- `docu/dummy_data/volume_data_scenarios.md`
- `docu/dummy_data/volume_expected_changes.md`
- `docu/work_logs/2026-06-25_creator_connected_dummy_volume_data_draft.md`

## 설계 결과

- 계정 37개: USER 32, COMPANY 4, ADMIN 1
- 팀 12개, 모집 24개, 슬롯 60개
- 지원 60건, 초대 36건, 팀 일정 60건
- 팔로우 128건, 매칭 북마크·액션 로그 각 96건
- 게시글 60개, 리뷰 180건, 좋아요 300건, 조회 로그 480건
- 작업물 36개, 포트폴리오 64개, 팀 작업물 승인 요청 24건
- 공모전 요청 6개, 공모전 24개, 공모전 관계 데이터 216건
- 신고 12건, 제재 4건, 알림 180건
- 전체 예상 INSERT 2,621건

## 실행한 명령과 결과

### 현재 DB 읽기 전용 조회

주요 테이블 count와 namespace count를 `SELECT`로 확인했다.

- CDD 계정 9건, CDD 팀 2건
- CDV 계정 0건, CDV 팀 0건

### 정적 검사

```bash
git diff --check -- sql/18_seed_connected_demo_volume_data.sql sql/19_validate_connected_demo_volume_data.sql sql/20_rollback_connected_demo_volume_data.sql
```

결과: 통과

```bash
rg -n "FOREIGN_KEY_CHECKS|DROP\\s+|TRUNCATE|99_reset|CREATE\\s+DATABASE|LOAD DATA|INTO OUTFILE|GRANT\\s|REVOKE\\s" sql/18_seed_connected_demo_volume_data.sql sql/19_validate_connected_demo_volume_data.sql sql/20_rollback_connected_demo_volume_data.sql
```

결과: 일치 항목 없음

Node 기반 문자열·괄호 균형 검사:

- seed 143 statements
- validation 6 statements
- rollback 45 statements
- 따옴표, backtick, 괄호 불균형 없음

### MySQL PREPARE 구문 검사

각 SQL 파일의 DML/SELECT 문을 실제 실행하지 않고 `PREPARE`와 `DEALLOCATE PREPARE`만 수행했다.

결과:

- seed DML/SELECT 파싱 성공
- validation SELECT 파싱 성공
- rollback DML 파싱 성공
- 구문 오류 없음

첫 sandbox 내부 시도는 MySQL socket 접근 제한으로 실패했다. 권한 승인 후 동일한 PREPARE 전용 검사를 다시 실행해 성공했다.

### DB 미변경 재확인

PREPARE 검사 후 읽기 전용 count:

- CDV 계정 0
- CDV 팀 0
- CDV 게시글 0
- CDV 공모전 0

따라서 실제 데이터 변경은 발생하지 않았다.

## 결과

- CDV namespace seed/validation/rollback 초안 작성 완료
- CDD 및 기존 사용자·크롤링·업로드 경로 보호 범위 반영
- 이미지 경로 `NULL` fallback 정책 반영
- expected count, 상태 분포, zero-error, CDD guard 검증 반영
- 프런트엔드·백엔드 코드 변경 없음

## 남은 이슈

- 실제 MySQL 8 seed 적용 검증 미수행
- 실제 validation 결과 미생성
- 적용 전·후 dump와 checksum 미생성
- CDV 테스트 계정 ID와 핵심 엔티티 ID 미확정
- 홈, 매칭, 팀, 게시판, 프로필, 공모전, 관리자 브라우저 smoke 미수행
- 실제 적용은 별도 사용자 승인과 백업 이후 진행 필요
