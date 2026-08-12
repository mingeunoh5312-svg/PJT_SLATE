# 2026-06-25 볼륨 더미 데이터 후속 문서 동기화 로그

## 작업 범위

- 역할: 문서담당자
- 기준 시점: `docu/work_logs/2026-06-25_documenter_current_work_sync.md` 이후
- 대상: CDV 2차 연관형 볼륨 더미 데이터 초안, 관리자 권한 blocker 수정, 실제 DB 적용·검증·전달 결과
- 코드, SQL, DB, dump 파일은 수정하지 않고 문서만 동기화했다.

## 후속 작업 요약

| 작업 | 결과 |
|---|---|
| CDV 볼륨 데이터 설계 | CDD를 유지하는 별도 `cdv-*`, `[CDV]`, `SLATE_CDV` namespace 구성 |
| seed 규모 | 38개 테이블 합계 2,621건 |
| 핵심 데이터 | 계정 37, 프로필 32, 팀 12, 게시글 60, 리뷰 180, 좋아요 300, 조회 로그 480, 작업물 36, 포트폴리오 64, 공모전 24, 알림 180 |
| 관리자 권한 blocker | `cdv-admin` 권한을 백엔드 카탈로그 8개로 제한하고 `DEMO_ACCESS_MANAGE` 제외 |
| 실제 DB 적용 | `sql/18_seed_connected_demo_volume_data.sql` 적용 성공 |
| CDV 검증 | 예상 count 38개 항목 일치, zero-error 39개 항목 모두 0 |
| CDD 회귀 | 예상 count 11개 항목 일치, zero-error 23개 항목 모두 0 |
| 전달 DB | 적용 전·후 전체 dump와 SHA-256 checksum 생성·검증 완료 |
| rollback | `sql/20_rollback_connected_demo_volume_data.sql` 준비 완료, 실제 실행은 하지 않음 |

## 동기화한 문서

| 문서 | 반영 내용 |
|---|---|
| `docu/dummy_data/volume_data_scenarios.md` | 미적용 초안 상태를 실제 적용·검증 완료 상태로 보정하고 전달·복구 경로 추가 |
| `docu/dummy_data/volume_expected_changes.md` | 예상 문구를 실제 변경·검증 결과, dump checksum, rollback 상태 기준으로 갱신 |
| `docu/README.md` | CDD/CDV 데이터 규모, 전달 DB, 남은 CDV 화면 검증 반영 |
| `docu/03_mvp_scope/mvp_scope.md` | CDV 2,621건 적용과 검증 결과를 MVP 구현 상태에 추가 |
| `docu/06_frontend/frontend_baseline.md` | CDV 계정과 대량 NULL 이미지 데이터를 화면·fallback 검증 기준으로 추가 |
| `docu/07_database/database_baseline.md` | SQL 18~20 실행 기준, 실제 적용 결과, dump/rollback 기준 추가 |
| `docu/handoff/mvp_documentation_handoff.md` | 다음 작업자의 우선 확인 문서와 CDV 브라우저 smoke 순서 갱신 |
| `docu/user_temp/todo_0624_dummies_data.md` | 2차 볼륨 데이터 설계·적용·전달 완료 상태 추가 |

## 현재 결론

- CDD 기본 연관형 데이터와 CDV 볼륨 데이터는 모두 실제 `slate` DB 적용 및 SQL 정합성 검증이 완료됐다.
- 현재 로컬 DB는 CDV 적용 후 상태이며, CDV 전용 rollback은 실행하지 않았다.
- 공동작업자는 적용 후 dump로 동일 상태를 재현하거나 적용 전 dump 및 CDV rollback으로 범위를 선택해 복구할 수 있다.
- 남은 핵심 검증은 CDV 계정 기반 전체 route, pagination, 권한별 화면, 기본 이미지 fallback/crop의 desktop/mobile 브라우저 smoke다.

## 참조 경로

- `docu/work_logs/2026-06-25_creator_connected_dummy_volume_data_draft.md`
- `docu/work_logs/2026-06-25_fixer_connected_dummy_volume_admin_permission.md`
- `docu/work_logs/2026-06-25_creator_connected_dummy_volume_data_apply.md`
- `docu/dummy_data/volume_data_scenarios.md`
- `docu/dummy_data/volume_expected_changes.md`
- `docu/dummy_data/volume_validation_result.md`
- `docu/dummy_data/volume_test_accounts.md`
- `docu/dummy_data/volume_restore_guide.md`
- `sql/18_seed_connected_demo_volume_data.sql`
- `sql/19_validate_connected_demo_volume_data.sql`
- `sql/20_rollback_connected_demo_volume_data.sql`
