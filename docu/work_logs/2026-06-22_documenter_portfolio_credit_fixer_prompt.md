# 포트폴리오 크레딧 수정자 프롬프트 작성

## 작업 범위

- 포트폴리오 크레딧 이름 유실 원인을 수정자가 바로 구현할 수 있는 작업 프롬프트로 정리했다.
- 애플리케이션 코드, SQL, DB 데이터는 변경하지 않았다.

## 변경 파일

- `docu/prompt/portfolio_credit_roundtrip_verification_fixer_prompt.md`
- `docu/prompt/README.md`
- `docu/work_logs/2026-06-22_documenter_portfolio_credit_fixer_prompt.md`

## 프롬프트 주요 내용

- 사용자 입력 `creditName`과 KOBIS 매칭 결과 `providerPersonName`의 분리 계약
- 입력 이름을 `portfolio_item`에 보존하는 schema/migration 방향
- 등록·조회·수정 왕복과 검증 상태별 backend/frontend 요구사항
- VERIFIED, NOT_VERIFIED, AMBIGUOUS, ERROR별 필수 테스트
- 실제 참여자가 아닌 항목의 강제 Verified 및 유실 데이터 추측 복원 금지
- API, DB, 브라우저, 전체 test/build 완료 조건

## 실행한 명령과 결과

- 기존 fixer 프롬프트와 `docu/prompt/README.md` 형식 확인
- 관련 frontend/backend/mapper/schema와 로컬 검증 결과 재확인
- `git diff --check`로 문서 형식 검증

## 남은 이슈

- 실제 구현과 migration 적용은 후속 수정자 작업이다.
- 기존 portfolio item ID 5에서 이미 유실된 사용자 입력 이름은 현재 데이터로 복구할 수 없다.

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/prompt/profile_dashboard_data_follow_verified_fixer_prompt.md`
- `docu/work_logs/2026-06-22_fixer_verified_portfolio_badge.md`
- `frontend/src/views/ProfileView.vue`
- `backend/src/main/java/com/slate/profiles/PortfolioVerificationService.java`
- `backend/src/main/resources/mappers/ProfileMapper.xml`
- `sql/01_schema.sql`
