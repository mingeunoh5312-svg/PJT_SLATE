# 2차 연관형 볼륨 더미 데이터 적용 검증 결과

작성일: 2026-06-25 09:58 KST
대상 DB: `slate`
적용 SQL: `sql/21_seed_connected_demo_volume_data.sql`, `sql/27_apply_generated_dummy_images.sql`
검증 SQL: `sql/22_validate_connected_demo_volume_data.sql`

## 결과 요약

- CDV seed 적용: 성공
- 예상 count 검증: 38개 항목 모두 일치
- `CDV_ZERO_ERROR_CHECKS`: 39개 항목 모두 0
- 기존 CDD 예상 count 검증: 11개 항목 모두 일치
- 기존 `CDD_ZERO_ERROR_CHECKS`: 23개 항목 모두 0
- 적용 전·후 전체 dump 및 SHA-256 checksum 생성: 성공
- rollback 실행: 미실행
- 실패 및 적용 중단 사유: 없음

## 주요 CDV 생성 결과

| 항목 | 실제 | 예상 | 결과 |
|---|---:|---:|---|
| 계정 | 37 | 37 | OK |
| 회사 신청 | 4 | 4 | OK |
| 관리자 권한 | 8 | 8 | OK |
| 프로필 | 32 | 32 | OK |
| 팀 | 12 | 12 | OK |
| 모집 공고 / 슬롯 | 24 / 60 | 24 / 60 | OK |
| 지원 / 초대 | 60 / 36 | 60 / 36 | OK |
| 팀 일정 | 60 | 60 | OK |
| 게시글 / 리뷰 / 좋아요 / 조회 | 60 / 180 / 300 / 480 | 동일 | OK |
| 작업물 / 포트폴리오 | 36 / 64 | 36 / 64 | OK |
| 팀 작업물 승인 요청 | 24 | 24 | OK |
| 공모전 요청 / 공모전 | 6 / 24 | 6 / 24 | OK |
| 공모전 저장 / 제출 준비 / 적합도 | 120 / 48 / 48 | 동일 | OK |
| 신고 / 제재 | 12 / 4 | 12 / 4 | OK |
| 알림 | 180 | 180 | OK |
| 감사 로그 / 운영 로그 | 28 / 4 | 28 / 4 | OK |

예상 INSERT 표의 38개 테이블 합계는 2,621건이다.

## 상태 분포 확인

- 팀: 모집 중 4, 진행 중 3, 모집 종료 2, 종료 예정 1, 종료 2
- 지원: `ACCEPTED`, `PENDING`, `REJECTED`, `CANCELED`, `EXPIRED` 각 12
- 초대: `ACCEPTED` 12, `PENDING` 12, `CANCELED` 6, `EXPIRED` 6
- 작업물 승인 요청: `APPROVED` 12, 나머지 세 상태 각 4
- 게시글: 공개 58, 블라인드 2
- 리뷰: 공개 178, 블라인드 2
- 공모전: 진행 중 21, 종료 3
- 신고: 승인·대기·거절 각 4
- 제재: 활성 2, 해제 1, 만료 1

## 핵심 zero-error 결과

아래를 포함한 39개 검사가 모두 0이다.

- 관리자 비카탈로그 권한, 누락 권한, 비활성 권한
- 팀장 및 현재 팀원 수 불일치
- 슬롯 수락 인원 불일치 및 정원 초과
- 수락 지원·초대와 active 팀원 관계 불일치
- pending 대상자의 active 팀원 중복
- 종료 팀의 열린 모집 및 snapshot 누락
- 게시글 좋아요·리뷰·조회 집계 불일치
- 작업물과 게시글 연결 불일치
- 승인 요청자의 active 팀원 여부
- 공모전 저장 수 및 승인 요청 연결 불일치
- 자기 팔로우와 중복 pending 관계
- CDV 이미지 경로 및 외부 미디어 값 존재
- 알림 target 누락
- 신고·블라인드·제재 관계 불일치
- CDV와 CDD namespace 교차 연결
- 기존 CDD 기준 count 변화

## 관리자 권한

`cdv-admin`에는 다음 8개 권한만 적용됐다.

- `ADMIN_PERMISSION_MANAGE`
- `COMPANY_APPROVAL`
- `CONTENT_MODERATION`
- `CONTEST_MANAGE`
- `LOG_VIEW`
- `NOTIFICATION_SEND`
- `SCORE_POLICY`
- `USER_SANCTION`

DB에 별도로 존재하는 `DEMO_ACCESS_MANAGE`는 CDV 관리자에게 삽입되지 않았다.

## 이미지 정책

2026-06-26 이미지 적용 이후 CDV의 프로필, 팀, 작업물, 포트폴리오, 공모전 요청, 공모전 이미지 경로는 `images/seed/.../*.png` 상대 경로로 채워진다. 파일 복사본은 `uploads/images/seed/...` 아래에 있으며, `SLATE_UPLOAD_DIR` 전역 설정은 변경하지 않는다.

현재 로컬 DB에서 `cdv_generated_image_path_missing_or_invalid`는 0이다. 다만 로컬 상호작용 데이터가 추가된 상태라 `board_like`와 `board_view_log` 고정 기대 count는 각각 302/300, 483/480으로 초과한다. 이를 원래 볼륨 검증 기준으로 되돌리려면 CDV seed reset 승인이 필요하다.

## 기존 CDD 회귀 검증

- CDD 예상 count 11개 항목 모두 일치
- CDD zero-error 23개 항목 모두 0
- 기존 CDD 계정 9건 유지
- CDD guard count 전체 일치

기존 데이터 경고인 업로드 경로 manifest 미확인 9건과 `example.test` URL 9건은 이번 CDV seed가 만든 값이 아니다.

## 백업 검증

| 구분 | 파일 | SHA-256 |
|---|---|---|
| 적용 전 | `database_delivery/2026-06-25/slate_before_connected_dummy_volume.sql` | `239b71df9f3fba8b3dc7386ec8fa83658816ab31e7efca6ed46113d9599e305d` |
| 적용 후 | `database_delivery/2026-06-25/slate_after_connected_dummy_volume.sql` | `de378b86c681b384fe63e7cef7be028e0ce41f3331a1963d0641a89009c7f0b5` |

- 두 checksum 검증 모두 `OK`
- 적용 전 dump에는 CDV 식별자 없음
- 적용 후 dump에는 CDV 식별자 있음
