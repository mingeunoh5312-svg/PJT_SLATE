# 2차 연관형 볼륨 더미 데이터 DB 적용 작업 로그

작성일: 2026-06-25
역할: creator

## 작업 범위

- 적용 직전 `slate` 전체 DB dump 및 checksum 생성
- `sql/18_seed_connected_demo_volume_data.sql` 실제 적용
- CDV 전체 validation 및 기존 CDD 회귀 검증
- 적용 후 전체 DB dump 및 checksum 생성
- 검증 결과, 테스트 계정, 복구 가이드 작성

## 실행 결과

- 적용 전 CDV 계정: 0
- 적용 전 CDD 계정: 9
- seed 실행: 성공
- CDV 예상 count: 38개 항목 모두 일치
- CDV zero-error: 39개 항목 모두 0
- CDD 예상 count: 11개 항목 모두 일치
- CDD zero-error: 23개 항목 모두 0
- 적용 후 CDV 계정: 37
- 적용 후 CDV 팀: 12
- 적용 후 CDV 게시글: 60
- 적용 후 CDV 공모전: 24
- CDV 관리자 권한: 카탈로그 권한 8개

## 백업

- `database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql`
- `database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql.sha256`
- `database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql`
- `database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql.sha256`

두 dump의 SHA-256 검증은 모두 통과했다.

## 생성 문서

- `docu/dummy_data/volume_validation_result.md`
- `docu/dummy_data/volume_test_accounts.md`
- `docu/dummy_data/volume_restore_guide.md`
- `docu/work_logs/2026-06-25_creator_connected_dummy_volume_data_apply.md`

## 남은 확인

- 백엔드와 프런트엔드를 실행한 뒤 홈, 매칭, 팀, 게시판, 프로필, 공모전, 관리자 화면 browser smoke를 수행한다.
- 기본 이미지 fallback과 기존 업로드·크롤링 이미지 우선순위를 화면에서 확인한다.
- dump 파일은 계정 데이터와 비밀번호 hash를 포함하므로 공개 저장소에 커밋하지 않는다.
