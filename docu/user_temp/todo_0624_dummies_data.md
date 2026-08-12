# 오늘 해야할 것들


현재 프로젝트를 위한 더미 데이터를 생성합니다.

1. 더미 데이터들이 필요한 부분들을 찾고 어떤 데이터가 데이터가 필요한 지, 분석한 뒤에 이에 맞는 더미 데이터를 생성합니다.

2. 현재 프로젝트에는 이미지를 넣어야 하는 부분들이 있습니다. 이미지가 없는 경우 default 이미지를 넣습니다. 프로젝트 내에서 default 이미지가 필요한 부분들을 정리하세요.

3. default 이미지는 프롬프트를 작성하여 웹에 있는 GPT에게 맡길 예정입니다.

## 진행 상태

| 항목 | 상태 | 근거 |
|---|---|---|
| 더미 데이터 필요 영역 분석 | 완료 | `docu/dummy_data/data_scenarios.md`, `docu/dummy_data/expected_changes.md` |
| 연관형 더미 데이터 seed 작성 | 완료 | `sql/15_seed_connected_demo_data.sql` |
| seed 검증 SQL/rollback 작성 | 완료 | `sql/16_validate_connected_demo_data.sql`, `sql/17_rollback_connected_demo_data.sql` |
| 실제 DB 적용 및 검증 | 완료 | `docu/dummy_data/validation_result.md`, `docu/work_logs/2026-06-25_creator_connected_dummy_data_apply.md` |
| 공동작업자 전달/복구 문서 | 완료 | `docu/dummy_data/restore_guide.md`, `database_delivery/2026-06-25` |
| 2차 볼륨 데이터 설계·seed | 완료 | `docu/dummy_data/volume_data_scenarios.md`, `sql/18_seed_connected_demo_volume_data.sql` |
| 2차 볼륨 데이터 검증/rollback | 완료 | `sql/19_validate_connected_demo_volume_data.sql`, `sql/20_rollback_connected_demo_volume_data.sql` |
| 2차 볼륨 데이터 실제 적용 | 완료 | 38개 테이블 2,621건, CDV count 38개·zero-error 39개 및 CDD 회귀 검증 통과 |
| 2차 볼륨 전달/복구 문서 | 완료 | `docu/dummy_data/volume_validation_result.md`, `docu/dummy_data/volume_restore_guide.md`, `docu/dummy_data/volume_test_accounts.md` |
| 기본 이미지 필요 영역 정리 | 완료 | `docu/user_temp/todo_common_home_matching_team_image_requirements.md` |
| 기본 이미지 생성 프롬프트 작성 | 완료 | `docu/prompt/default_image_generation_prompts.md` |
| 기본 이미지 생성·변환 | 완료 | `assets/defaults`, `frontend/src/assets/defaults` |
| 프런트 화면 연결 | 구현·빌드 완료 | `frontend/src/constants/defaultImages.js`, 화면별 Vue 컴포넌트 |
| 브라우저 시각 회귀 | 미수행 | CDV 계정 기반 전체 route·pagination smoke, 이미지 없음·로드 실패·crop 상태 확인 필요 |
