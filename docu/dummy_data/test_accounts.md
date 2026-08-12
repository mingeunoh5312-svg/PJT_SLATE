# 연관형 더미 데이터 테스트 계정

작성일: 2026-06-25
공통 비밀번호: `slate1234`

> DB seed에는 `{noop}slate1234`로 저장되어 있다. 로컬 개발 환경의 기존 샘플 계정 정책과 같은 형식이다.

## 계정 목록

| login_id | user_id | 유형 | 상태 | nickname | profile_id | 주요 용도 |
|---|---:|---|---|---|---:|---|
| `cdd-leader` | 112 | USER | ACTIVE | CDD 유현서 | 22 | 모집 중 팀 리더, 지원/초대/알림 확인 |
| `cdd-camera` | 113 | USER | ACTIVE | CDD 민재촬영 | 23 | 수락된 촬영감독 지원자, 팀 작업 승인 대기 요청자 |
| `cdd-sound` | 114 | USER | ACTIVE | CDD 소리담 | 24 | 동시녹음 초대 PENDING, 알림 확인 |
| `cdd-editor` | 115 | USER | ACTIVE | CDD 편집윤 | 25 | 완료팀 리더, 승인 작업물/포트폴리오 작성자 |
| `cdd-writer` | 116 | USER | ACTIVE | CDD 작가린 | 26 | 지원 PENDING, 초대 EXPIRED 확인 |
| `cdd-actor` | 117 | USER | ACTIVE | CDD 배우준 | 27 | 팀원, EXPIRED 지원/CANCELED 승인 요청 확인 |
| `cdd-reporter` | 118 | USER | ACTIVE | CDD 신고자 | 28 | 신고자, REJECTED/CANCELED 흐름 확인 |
| `cdd-moderated` | 119 | USER | TEMP_SUSPENDED | CDD 제재대상 | 29 | 신고 승인 후 제재 대상 |
| `cdd-company` | 120 | COMPANY | ACTIVE | CDD 도시필름랩 | - | 승인 회사, 공모전 요청/승인 확인 |

## 핵심 데이터 ID

| 구분 | ID | 이름/제목 | 상태 |
|---|---:|---|---|
| 팀 | 10 | `[CDD] 한강 야간 단편팀` | `RECRUITING` |
| 팀 | 11 | `[CDD] 완료된 포트폴리오팀` | `ENDED` / `NORMAL` |
| 게시글 | 8 | `[CDD] 한강 야간 리허설 컷` | `WORK` / `PUBLISHED` |
| 작업물 | 3 | `[CDD] 한강 야간 리허설 컷` | `PUBLISHED` |
| 게시글 | 9 | `[CDD] 야간 촬영 체크리스트 공유` | `FREE` / `PUBLISHED` |
| 게시글 | 10 | `[CDD] 운영 정책 검토용 숨김 게시글` | `FREE` / `BLINDED` |
| 공모전 요청 | 3 | `[CDD] 도시 단편 제작지원 요청` | `APPROVED` |
| 공모전 | 34 | `[CDD] 도시 단편 제작지원 공모` | `OPEN` |
| 제재 | 1 | `cdd-moderated` 임시 이용 제한 | `ACTIVE` |

## 팀 지원 상태

| application_id | 계정 | 역할 | 상태 |
|---:|---|---|---|
| 5 | `cdd-camera` | 촬영감독 | `ACCEPTED` |
| 6 | `cdd-writer` | 시나리오 작가 | `PENDING` |
| 7 | `cdd-reporter` | 시나리오 작가 | `REJECTED` |
| 8 | `cdd-reporter` | 영상 편집 | `CANCELED` |
| 9 | `cdd-actor` | 시나리오 작가 | `EXPIRED` |

## 팀 초대 상태

| invitation_id | 계정 | 역할 | 상태 |
|---:|---|---|---|
| 4 | `cdd-editor` | 영상 편집 | `ACCEPTED` |
| 5 | `cdd-sound` | 동시녹음 | `PENDING` |
| 6 | `cdd-reporter` | 동시녹음 | `CANCELED` |
| 7 | `cdd-writer` | 시나리오 작가 | `EXPIRED` |

## 팀 작업물 승인 요청

| request_id | requester | 제목 | 상태 |
|---:|---|---|---|
| 2 | `cdd-editor` | `[CDD] 한강 야간 컷 공개 승인` | `APPROVED` |
| 3 | `cdd-camera` | `[CDD] 한강 현장음 믹스 승인 대기` | `PENDING` |
| 4 | `cdd-editor` | `[CDD] 미완성 러프컷 반려` | `REJECTED` |
| 5 | `cdd-actor` | `[CDD] 배우 리허설 요청 취소` | `CANCELED` |

## 알림 확인 포인트

| notification_id | 수신자 | 유형 | 제목 | 읽음 |
|---:|---|---|---|---|
| 24 | `cdd-leader` | SOCIAL | `[CDD] 새 팔로워가 생겼습니다.` | N |
| 25 | `cdd-leader` | TEAM | `[CDD] 새 팀 지원이 도착했습니다.` | N |
| 26 | `cdd-camera` | TEAM | `[CDD] 팀 지원이 수락되었습니다.` | Y |
| 27 | `cdd-sound` | TEAM | `[CDD] 팀 초대가 도착했습니다.` | N |
| 28 | `cdd-company` | ADMIN | `[CDD] 공모전 요청이 승인되었습니다.` | N |
| 29 | `cdd-moderated` | ADMIN | `[CDD] 계정 이용이 제한되었습니다.` | N |
| 30 | `cdd-sound` | TEAM | `[CDD] 팀 작업이 정상 종료되었습니다.` | Y |

## 추천 테스트 흐름

1. `cdd-leader`
   - 한강 야간 단편팀 리더 화면
   - 지원 PENDING, 초대 PENDING, 새 알림 확인
2. `cdd-camera`
   - 수락된 지원자 상태
   - 팀 작업물 승인 대기 요청 확인
3. `cdd-sound`
   - 초대 PENDING 알림
   - 팀 종료 알림 읽음 상태 확인
4. `cdd-editor`
   - 완료팀 리더
   - 승인된 작업물/포트폴리오 확인
5. `cdd-company`
   - 회사 계정
   - 승인된 공모전 요청과 생성 공모전 확인
6. `cdd-moderated`
   - 임시 이용 제한 상태 확인
