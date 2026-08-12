# 크레딧·KOBIS·대표 이미지 문서 통합 정리

## 작업 범위

- 포트폴리오 크레딧 왕복 수정, 실제 KOBIS 재검증, 프로필·팀·작업물·포트폴리오 이미지 작업을 현재 기준 문서에 통합했다.
- 애플리케이션 코드, SQL, DB 데이터는 변경하지 않았다.

## 변경 파일

- `docu/README.md`
- `docu/03_mvp_scope/mvp_scope.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/handoff/mvp_documentation_handoff.md`
- `docu/work_logs/2026-06-22_fixer_portfolio_credit_roundtrip.md`
- `docu/work_logs/2026-06-22_documenter_credit_media_status_sync.md`

## 반영 내용

- 최신 backend 전체 테스트 기준을 74개로 갱신했다.
- 사용자 `creditName`과 KOBIS provider 매칭 이름·역할·상태의 분리 계약을 기록했다.
- `08_portfolio_credit_name_schema.sql`, `09_entity_image_schema.sql`의 실제 2회 적용 결과를 기록했다.
- 엔티티 이미지 API, 지원 타입, 5MB JPEG/PNG/WebP 제한, 소유권과 교체/삭제 정합성을 기준 문서에 추가했다.
- 프런트 이미지 우선순위와 Verified 표시 조건, 남은 브라우저 검증을 구분했다.
- `역린 / 이재규 / 감독`이 실제 KOBIS 감독 정보와 일치하고 16:05 기준 `VERIFIED`임을 후속 근거로 남겼다.

## 검증

- 로컬 DB에서 portfolio item 5의 사용자 입력, provider 매칭 필드, `VERIFIED`, KOBIS 원본 감독 목록을 확인했다.
- 기준 문서와 2026-06-22 수정자 작업로그 두 건을 대조했다.
- `git diff --check`로 문서 형식을 확인했다.
- 코드 변경이 없어 test/build는 다시 실행하지 않았다. 최신 실행 결과는 수정자 작업로그의 74 tests와 frontend build를 인용했다.

## 남은 이슈

- 제공된 `검증되지 않음` 화면은 재검증 또는 최신 프론트 반영 전 상태다. 최신 화면 새로고침 후 Verified 표시를 재확인해야 한다.
- 최신 크레딧·이미지 화면의 desktop/390x844, console, Network 검증은 브라우저 도구 연결 문제로 남아 있다.
- 비공개 이미지의 일반 `<img>` 요청에는 Bearer 헤더가 없어 인증 blob loader 또는 서명 URL 정책을 검토할 수 있다.

## 참조 경로

- `docu/work_logs/2026-06-22_fixer_portfolio_credit_roundtrip.md`
- `docu/work_logs/2026-06-22_fixer_profile_media_and_portfolio_display.md`
- `backend/src/main/java/com/slate/profiles`
- `backend/src/main/java/com/slate/media`
- `frontend/src/views/ProfileView.vue`
- `sql/08_portfolio_credit_name_schema.sql`
- `sql/09_entity_image_schema.sql`
