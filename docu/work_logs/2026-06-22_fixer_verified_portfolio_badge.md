# Verified 포트폴리오 배지 검증 및 응답 타입 수정

## 작업 범위

- Verified 배지 API/UI 확인용 선택 실행 더미 SQL 추가 및 로컬 DB 적용
- 포트폴리오 `verified` 응답의 JSON Boolean 타입 보정
- 기존 숫자 Boolean 응답에 대한 프런트 호환 처리
- 매퍼 계약 테스트와 기준 문서 갱신

## 변경 파일

- `sql/07_seed_verified_portfolio_ui_demo.sql`
- `backend/src/main/resources/mappers/ProfileMapper.xml`
- `backend/src/test/java/com/slate/profiles/ProfileMapperPortfolioVerificationContractTest.java`
- `frontend/src/views/ProfileView.vue`
- `docu/README.md`
- `docu/05_backend/backend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/handoff/mvp_documentation_handoff.md`
- `docu/work_logs/2026-06-22_fixer_verified_portfolio_badge.md`

## 원인과 수정

- MySQL의 `CASE ... TRUE/FALSE` 결과가 `resultType="map"`에서 숫자 `1/0`으로 직렬화됐다.
- 프런트의 `item.verified === true` 조건과 실제 `verified: 1` 응답이 맞지 않아 DB가 `VERIFIED`여도 배지가 표시되지 않았다.
- 세 포트폴리오 조회에 공통 `portfolioItemResultMap`을 적용하고 `verified`의 Java 타입을 `Boolean`으로 고정했다.
- 프런트는 수정 전 서버와의 호환을 위해 정확한 Boolean `true` 또는 숫자 `1`만 Verified로 인정한다.

## 더미 데이터

- 로컬 `leader` 계정의 프로필에 `Verified 배지 UI 검증용 더미 작품` 한 건을 추가했다.
- 첨부 화면 확인 후 현재 검증 계정인 `domingo53121`에도 같은 fixture를 별도 적용했다. 기존 `역린` 항목은 크레딧 이름 누락으로 `NOT_VERIFIED`이며 상태를 임의 변경하지 않았다.
- 외부 식별자는 `SLATE-DEMO-VERIFIED-001`이며 재실행 시 중복 생성되지 않는다.
- 기본 대상은 `leader`이며 `@verified_badge_demo_login_id` 세션 변수로 로컬 검증 계정을 지정할 수 있다.
- provider와 설명에 UI fixture임을 표시했다.
- 이 데이터는 실제 KOBIS 호출 성공을 의미하지 않으며 운영 DB 적용 대상이 아니다.

## 실행한 명령과 결과

- `sql/07_seed_verified_portfolio_ui_demo.sql` 로컬 MySQL 적용: PASS
- DB 조회: portfolio item ID 4, verification status `VERIFIED` 확인
- 수정 전 실행 서버 API: `verified: 1` 확인
- 수정본 임시 서버 API: `verified: true`, JSON type `boolean` 확인
- `mvn test`: 61 tests, failures 0, errors 0, skipped 0
- `npm run build`: PASS
- `git diff --check`: PASS

## 남은 검증

- 내장 브라우저가 실행 환경 메타데이터 오류로 시작되지 않아 실제 화면 캡처와 desktop/mobile 시각 검증은 수행하지 못했다.
- 현재 KOBIS API key가 없어 실제 영화·크레딧 조회를 통한 `VERIFIED` 생성 흐름은 수행하지 못했다.

## 참조 경로

- `backend/src/main/java/com/slate/profiles/PortfolioVerificationService.java`
- `backend/src/main/resources/mappers/ProfileMapper.xml`
- `frontend/src/views/ProfileView.vue`
- `sql/01_schema.sql`
- `sql/03_seed_sample_data.sql`
- `docu/work_logs/2026-06-21_fixer_profile_dashboard.md`
