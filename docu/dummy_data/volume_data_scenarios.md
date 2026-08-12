# 2차 연관형 볼륨 더미 데이터 시나리오

작성일: 2026-06-25
상태: 사용자 승인 후 실제 DB 적용·검증 완료

이 문서는 `sql/21_seed_connected_demo_volume_data.sql`의 CDV 볼륨 데이터 설계와 화면별 확인 목적을 정리한다. 기존 CDD 데이터는 유지하며, CDV 데이터끼리 관계를 완성하는 별도 namespace로 설계했다.

실제 `slate` DB 적용 결과는 목표 규모와 일치했다. `sql/22_validate_connected_demo_volume_data.sql`의 예상 count 38개 항목이 모두 일치했고 zero-error 39개 항목이 모두 0이었으며, 기존 CDD 검증도 함께 통과했다.

## 설계 원칙

- 계정은 `cdv-*`, 제목은 `[CDV]`, 외부 식별자는 `CDV-*`, 포트폴리오 외부 소스는 `SLATE_CDV`를 사용한다.
- CDD 계정·팀·게시글·작업물·포트폴리오·공모전·알림은 생성, 수정, 제거 대상에 포함하지 않는다.
- 고정 DB ID 대신 login ID, 제목, 외부 식별자, reference 이름으로 관계를 찾는다.
- accepted 지원·초대는 실제 active 팀원으로 연결하고 pending 대상자는 해당 팀의 active 팀원이 아니게 구성한다.
- 팀원 수, 슬롯 수락 수, 게시글 반응 수, 공모전 저장 수는 관계 행 생성 후 다시 계산한다.
- 실제 KOBIS 검증, 외부 URL, 실제 개인정보, 실제 업로드 파일을 만들지 않는다.
- 프로필·팀·작업물·포트폴리오·공모전 이미지 경로는 `NULL`로 유지한다.

## 목표 규모

| 영역 | CDV 목표 |
|---|---:|
| 계정 | 37 |
| USER 프로필 | 32 |
| COMPANY 계정 | 4 |
| ADMIN 계정 | 1 |
| 팀 | 12 |
| 모집 공고 / 슬롯 | 24 / 60 |
| 지원 / 초대 | 60 / 36 |
| 팀 일정 | 60 |
| 팔로우 | 128 |
| 매칭 북마크 / 액션 로그 | 96 / 96 |
| 게시글 / 리뷰 / 좋아요 | 60 / 180 / 300 |
| 작업물 / 포트폴리오 | 36 / 64 |
| 팀 작업물 승인 요청 | 24 |
| 공모전 요청 / 공모전 | 6 / 24 |
| 공모전 저장 / 제출 준비 / 적합도 | 120 / 48 / 48 |
| 신고 / 제재 | 12 / 4 |
| 알림 | 180 |

## 계정 그룹

| 계정 | 수량 | 용도 |
|---|---:|---|
| `cdv-user-01`~`cdv-user-12` | 12 | 팀 리더, 팀 상태별 기준 사용자 |
| `cdv-user-13`~`cdv-user-26` | 14 | accepted 지원·초대 팀원, 게시판·작업물 작성자 |
| `cdv-user-27`~`cdv-user-32` | 6 | pending 지원·초대, 운영 상태, 매칭 다양화 |
| `cdv-company-01`~`cdv-company-04` | 4 | 승인 회사, 공모전 개설 요청 |
| `cdv-admin` | 1 | CDV 회사·공모전·신고·제재 처리자, 백엔드 카탈로그 8개 권한 보유 |

모든 계정은 `@slate.test` 이메일과 `{noop}slate1234` 로컬 데모 비밀번호 형식을 사용한다.

`cdv-admin`의 권한은 `AdminPermissionCatalog.CODES`의 8개 코드로 제한하며, DB에 별도로 존재하는 `DEMO_ACCESS_MANAGE`는 삽입하지 않는다.

32개 USER 프로필은 다음 값을 고르게 순환한다.

- 역할: 기획, 연출, 각본, 촬영, 조명, 음향, 미술, 의상, 배우, 편집, VFX, 음악, 마케팅
- 지역: 서울 5개 권역, 경기 2개 권역, 부산, 대구, 광주, 대전, 제주
- 경력: `Y0_3`, `Y3_10`, `Y10_PLUS`
- 합류 가능 시점: 즉시부터 협의 가능까지 6종
- 협업 조건: 무급, 협의, 유급, 수익 배분, 상금 배분, 무관
- 참여 방식: 오프라인, 원격, 혼합

## 팀·모집·지원·초대 시나리오

12개 팀은 이름에 `[CDV] 01`~`[CDV] 12` 번호를 포함한다.

| 팀 범위 | 상태 | 목적 |
|---|---|---|
| 01~04 | `RECRUITING` | 열린 모집과 pending 지원·초대 |
| 05~07 | `IN_PROGRESS` | 진행 중에도 필요한 역할을 모집하는 흐름 |
| 08~09 | `RECRUITMENT_CLOSED` | 모집 완료 후 일정·작업물 진행 |
| 10 | `CLOSING` | 종료 예정 팀의 일정과 승인 요청 |
| 11 | `ENDED` / `NORMAL` | 정상 종료와 closure snapshot |
| 12 | `ENDED` / `DISSOLUTION` | 해체 종료와 closure snapshot |

각 팀은 다음 관계를 가진다.

- active leader 1명
- accepted 지원자 1명과 accepted 초대 대상자 1명
- active 팀원 총 3명
- 모집 공고 2개와 슬롯 5개
- 지원 5상태 각 1건
- 초대 `ACCEPTED`, `PENDING` 각 1건과 `CANCELED`/`EXPIRED` 교차 1건
- 일정 5건

종료 팀의 모집 공고와 슬롯은 모두 닫혀 있고 CDV namespace를 기록한 closure snapshot이 존재한다.

## 게시판·작업물·포트폴리오 시나리오

- WORK 게시글 30개와 FREE 게시글 30개를 생성한다.
- FREE 게시글은 공지, 질문, 정보, 후기, 자유 분류를 순환한다.
- 각 게시글에는 리뷰 3건, 좋아요 5건, 조회 로그 8건이 있다.
- 리뷰는 상위 리뷰 2건과 대댓글 1건으로 구성한다.
- WORK 게시글 30개는 작업물과 1:1 연결한다.
- 게시글 없는 독립 작업물 6개를 추가해 홈과 프로필 작업물 볼륨을 보강한다.
- 작업물마다 장르 2개를 연결한다.
- 프로필마다 수동 포트폴리오 2개를 생성한다.
- 포트폴리오는 `SLATE_CDV`, `CDV-PORT-*` 식별자를 사용하며 KOBIS 검증 행을 만들지 않는다.

팀별 작업물 승인 요청은 2건씩 총 24건이다.

- 첫 번째 요청 12건은 `APPROVED`이며 게시글과 작업물에 연결된다.
- 두 번째 요청은 `PENDING`, `REJECTED`, `CANCELED`가 각 4건이다.
- 모든 requester는 해당 팀의 active 팀원이다.

## 공모전 시나리오

4개 승인 회사가 공모전 개설 요청 6건을 만든다.

| 요청 상태 | 수량 |
|---|---:|
| `APPROVED` | 4 |
| `PENDING` | 1 |
| `REJECTED` | 1 |

승인 요청 4건은 내부 공모전과 양방향으로 연결한다. 추가로 외부 공모전 20개를 생성해 총 24개를 만든다.

- 진행 중 공모전 21개, 종료 공모전 3개
- 대상, 지역, 주최 유형, 상금, 역할, 장르를 순환
- 공모전마다 저장 5건
- 공모전마다 TEAM 제출 준비 1건, PROFILE 제출 준비 1건
- 공모전마다 TEAM 적합도 1건, PROFILE 적합도 1건
- `save_count`는 실제 `contest_save` 수로 다시 계산

## 관리자·신고·제재·알림 시나리오

신고 12건은 `ACCEPTED`, `PENDING`, `REJECTED`가 각 4건이다.

- accepted 게시글 신고 2건은 실제 게시글 `BLINDED` 처리와 연결한다.
- accepted 리뷰 신고 2건은 실제 리뷰 `BLINDED` 처리와 연결한다.
- accepted 신고 대상 작성자 4명에게 제재를 연결한다.
- 제재 상태는 `ACTIVE` 2건, `REVOKED` 1건, `EXPIRED` 1건이다.
- active 제재 대상 계정만 계정 상태를 정지 상태로 맞춘다.

알림은 CDV 비관리자 계정 36개에 5건씩 총 180건을 만든다.

- 추천 프로필
- 팀 활동
- 공모전 마감
- 게시글 반응
- 주간 시스템 요약

PROFILE, TEAM, CONTEST, BOARD_POST target은 모두 실제 CDV 엔티티를 가리킨다.

## 화면별 확인 포인트

### 홈

- 최근 작업물과 게시글 카드가 충분히 노출되는지
- 좋아요, 리뷰, 조회 수 기반 인기 정렬이 달라지는지
- 읽지 않은 알림과 팀 활동, 공모전 마감 정보가 풍성하게 보이는지
- USER, COMPANY, ADMIN 계정별 요약이 비어 있지 않은지

### 매칭

- 32개 프로필의 역할, 지역, 경력, 합류 가능 시점, 협업 조건 필터 결과가 분산되는지
- 팀 찾기에 모집 중·진행 중 팀과 열린 슬롯이 충분히 노출되는지
- 북마크, pending 지원, pending 초대, accepted 상태가 함께 보이는지

### 팀

- 5개 팀 상태가 모두 표현되는지
- 리더, 팀원 수, 모집 공고, 슬롯 수락 수가 일치하는지
- 종료 팀에 열린 모집이 없고 closure snapshot이 보존되는지
- 일정 상태와 작업물 승인 상태가 다양하게 보이는지

### 게시판·프로필

- WORK/FREE 목록과 세부 분류가 충분히 채워지는지
- 리뷰와 대댓글, 좋아요, 조회 수가 실제 관계 건수와 일치하는지
- 작업물과 포트폴리오가 역할·장르에 맞게 보이는지
- CDV 포트폴리오에 verified 배지가 잘못 표시되지 않는지

### 공모전·관리자

- 공모전 검색 필터와 저장 목록, 제출 준비, 적합도 결과가 충분히 보이는지
- 회사 요청의 승인·대기·거절 상태가 구분되는지
- 신고, 숨김, 제재, 알림, 감사 로그가 원인 데이터와 연결되는지

## 이미지 fallback 확인

다음 CDV 경로는 모두 `NULL`을 기대한다.

- `member_profile.profile_image_path`
- `team.representative_image_path`
- `work_item.representative_image_path`
- `portfolio_item.thumbnail_image_path`
- `contest.representative_image_path`
- `contest_open_request.representative_image_path`

프런트엔드의 프로필, 팀, 작업물, 포트폴리오, 공모전 기본 이미지가 각각 표시되는지 확인한다.

## 적용 및 전달 상태

- 적용 전·후 전체 DB dump와 SHA-256 checksum 생성 완료
- 적용 결과: `docu/dummy_data/volume_validation_result.md`
- 테스트 계정: `docu/dummy_data/volume_test_accounts.md`
- 복구 및 CDV 전용 rollback: `docu/dummy_data/volume_restore_guide.md`
- 실제 rollback은 실행하지 않았으며 현재 `slate` DB에는 CDV 데이터가 적용된 상태다.
- SQL 정합성 검증은 완료됐지만 화면별 desktop/mobile 브라우저 smoke는 남아 있다.

## 참조 경로

- `docu/prompt/connected_dummy_volume_data_creator_prompt.md`
- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/validation_result.md`
- `docu/dummy_data/volume_validation_result.md`
- `docu/dummy_data/volume_test_accounts.md`
- `docu/dummy_data/volume_restore_guide.md`
- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`
- `sql/18_seed_connected_demo_data.sql`
- `sql/21_seed_connected_demo_volume_data.sql`
- `sql/22_validate_connected_demo_volume_data.sql`
- `sql/23_rollback_connected_demo_volume_data.sql`
