# 기본 이미지·연관형 더미 데이터 Git 인수인계

작성일: 2026-06-25
전달 브랜치: `dev_Slate_0625_default_dummies`
기준 브랜치: `dev_Slate_0624_user1_v2`
기준 커밋: `e03e0a0`

## 목적

이 브랜치는 다음 작업을 공동작업자의 작업 브랜치로 이식하기 위한 전달본이다.

1. 프로필·팀·작업물·포트폴리오·공모전 기본 이미지 5종
2. 기본 이미지 공통 export와 주요 화면 fallback 연결
3. CDD 연관형 기본 더미 데이터 155건
4. CDV 연관형 볼륨 더미 데이터 2,621건
5. seed·검증·namespace 전용 rollback SQL
6. 시나리오, 테스트 계정, 검증 결과, 작업 로그와 기준 문서

전체 DB dump는 계정 데이터와 비밀번호 hash, 감사·운영 로그를 포함하므로 이 Git 브랜치에 넣지 않았다. 더미 데이터 자체는 `sql/18`와 `sql/21`에 모두 들어 있으며 dump 없이 재현할 수 있다.

## Git에서 받는 방법

저장소 루트에서 실행한다.

```bash
git fetch origin
git switch <공동작업자-브랜치>
git merge --no-ff origin/dev_Slate_0625_default_dummies
```

전달 브랜치의 변경만 단일 커밋으로 가져오려면 branch tip을 cherry-pick할 수 있다.

```bash
git fetch origin
git switch <공동작업자-브랜치>
git cherry-pick origin/dev_Slate_0625_default_dummies
```

공동작업자 브랜치에 같은 화면 파일이나 문서 변경이 이미 있으면 자동 merge 결과를 그대로 확정하지 말고 아래 이식 범위와 우선순위를 기준으로 충돌을 해결한다.

## 기본 이미지 이식 범위

### 자산

| 구분 | 경로 |
|---|---|
| 생성 원본 PNG | `Slate/assets/defaults` |
| 실제 프런트 WebP | `Slate/frontend/src/assets/defaults` |
| 공통 export | `Slate/frontend/src/constants/defaultImages.js` |

프런트 번들에서는 WebP만 사용한다. `assets/defaults`의 PNG는 생성 원본 보관용이다.

### 화면 코드

| 파일 | 이식 내용 |
|---|---|
| `frontend/src/layouts/AppLayout.vue` | 로그인 사용자 프로필 기본 이미지 |
| `frontend/src/components/follows/FollowListDialog.vue` | 팔로워·팔로잉 프로필 기본 이미지 |
| `frontend/src/views/HomeView.vue` | 작업물·공모전 기본 이미지 |
| `frontend/src/views/MatchingView.vue` | 사용자·팀·포트폴리오 기본 이미지 |
| `frontend/src/views/TeamsView.vue` | 팀 목록·상세·입력 미리보기 기본 이미지 |
| `frontend/src/views/BoardView.vue` | 작업물·인기 프로필 기본 이미지 |
| `frontend/src/views/ContestView.vue` | 공모전 목록·상세·관리·입력 미리보기 기본 이미지 |
| `frontend/src/views/ProfileView.vue` | 프로필·팀·작업물·포트폴리오 기본 이미지 |
| `frontend/src/views/PublicProfileView.vue` | 공개 프로필·포트폴리오 기본 이미지 |
| `frontend/src/styles/slate.css` | 공통 avatar 이미지 `object-fit: cover` |

표시 우선순위는 다음과 같다.

- 프로필: 등록 이미지 → 프로필 기본 이미지
- 팀: 등록 이미지 → 팀 기본 이미지
- 작업물: 업로드 대표 이미지 → YouTube 썸네일 → 작업물 기본 이미지
- 포트폴리오: 업로드 썸네일 → 외부/YouTube 썸네일 → 포트폴리오 기본 이미지
- 공모전: 업로드 이미지 → 요청 이미지 → 외부 대표 이미지 → 공모전 기본 이미지

실제 URL 로드 실패 시에도 같은 용도의 기본 이미지로 복구한다.

## 더미 데이터 이식 전제

- `dev_Slate_0625_user1_v2` 기준 이식본에서는 기존 `sql/15`~`sql/17` 크롤러/공식 링크/접근 코드 스키마를 보존하기 위해 더미 데이터 파일을 `sql/18`~`sql/23`으로 재번호화했다.
- 대상은 로컬 또는 폐기 가능한 개발용 MySQL 8 `slate` DB다.
- 현재 `sql/01_schema.sql`과 reference 데이터가 적용되어 있어야 한다.
- 기존 프로젝트의 schema와 이 브랜치의 SQL이 충돌하면 seed를 실행하지 말고 schema 차이를 먼저 해결한다.
- 운영 DB에는 적용하지 않는다.
- `sql/99_reset.sql`은 실행하지 않는다.
- CDD는 `cdd-*`, `[CDD]`, `SLATE_CDD`, `CDD_*` namespace만 사용한다.
- CDV는 `cdv-*`, `[CDV]`, `SLATE_CDV`, `CDV_*` namespace만 사용한다.

## 권장 DB 적용 순서

`Project_Slate/Slate`에서 실행한다. 아래 `--login-path` 이름은 공동작업자의 로컬 설정에 맞게 바꿀 수 있다.

### 1. 현재 DB 별도 백업

공동작업자의 기존 로컬 데이터가 필요하면 먼저 개인 보안 경로에 백업한다. 백업 파일은 Git에 추가하지 않는다.

### 2. CDD 기본 세트 적용과 검증

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/18_seed_connected_demo_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
```

성공 기준:

- `CDD_EXPECTED_COUNTS` 11개 항목의 실제값과 기대값 일치
- `CDD_ZERO_ERROR_CHECKS` 23개 항목 모두 0

### 3. CDV 볼륨 세트 적용과 검증

CDD 검증이 성공한 뒤 실행한다.

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/21_seed_connected_demo_volume_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/22_validate_connected_demo_volume_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
```

성공 기준:

- `CDV_EXPECTED_COUNTS` 38개 항목의 실제값과 기대값 일치
- `CDV_ZERO_ERROR_CHECKS` 39개 항목 모두 0
- CDD 회귀 count 일치
- CDD zero-error 전부 0

seed는 같은 namespace를 먼저 정리한 뒤 다시 생성하도록 작성되었지만, 실행 전 백업과 실행 후 검증은 생략하지 않는다.

## rollback

CDV만 제거하고 CDD를 유지:

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/23_rollback_connected_demo_volume_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
```

CDD까지 제거:

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/20_rollback_connected_demo_data.sql
```

rollback은 namespace 전용이며 공동작업자의 비-CDD/CDV 데이터를 제거하도록 설계하지 않았다. 그래도 실행 전에는 로컬 DB를 백업한다.

## 테스트 계정

| 용도 | 계정 |
|---|---|
| CDD 팀장 | `cdd-leader` |
| CDD 회사 | `cdd-company` |
| CDD 제재 상태 | `cdd-moderated` |
| CDV 일반 사용자·팀장 | `cdv-user-01` |
| CDV 다른 팀 상태 | `cdv-user-05`, `cdv-user-11` |
| CDV 회사 | `cdv-company-01` |
| CDV 관리자 | `cdv-admin` |
| CDV 정지 계정 | `cdv-user-25` |

로컬 데모 공통 비밀번호와 세부 ID는 다음 문서에서 확인한다.

- `docu/dummy_data/test_accounts.md`
- `docu/dummy_data/volume_test_accounts.md`

## 이식 후 검증

```bash
cd Slate/backend
mvn test
```

```bash
cd Slate/frontend
npm install
npm run build
```

브라우저에서는 다음을 확인한다.

- 이미지가 없는 프로필·팀·작업물·포트폴리오·공모전에 용도별 기본 이미지 표시
- 잘못된 이미지 URL 로드 실패 후 기본 이미지 복구
- 데스크톱과 모바일에서 crop, 비율, overflow
- CDV 볼륨에서 홈·매칭·팀·게시판·프로필·공모전·관리자 pagination과 정렬
- `cdv-admin`의 관리자 권한 8개와 회사·신고·제재 화면

## 충돌 가능성이 높은 파일

다음 파일은 최근 UI 작업이 많아 공동작업자 브랜치와 충돌할 가능성이 높다.

- `frontend/src/views/HomeView.vue`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/views/BoardView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/ProfileView.vue`
- `docu/README.md`
- `docu/03_mvp_scope/mvp_scope.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`

충돌 시 화면의 기존 API·route·mutation 로직은 공동작업자 최신 구현을 유지하고, 기본 이미지 import·우선순위·`@error` fallback만 이식한다. SQL은 파일 번호 `18`~`23`을 한 묶음으로 유지한다.

## Git 제외 항목

- `database_delivery/`: 전체 DB dump와 checksum의 로컬 비공개 보관 경로
- `package-lock.json`: `Slate` 루트에 실수로 생성된 빈 lockfile
- `backend/target`
- `backend/uploads`
- `frontend/node_modules`
- `frontend/dist`
- 실제 `.env`, `application-local.yml`, API key, DB 비밀번호, JWT secret

`frontend/package-lock.json`은 기존 프런트 의존성 lockfile이며 제외 대상이 아니다.

## 상세 문서

- `docu/work_logs/2026-06-24_frontend_default_image_integration.md`
- `docu/dummy_data/data_scenarios.md`
- `docu/dummy_data/expected_changes.md`
- `docu/dummy_data/validation_result.md`
- `docu/dummy_data/restore_guide.md`
- `docu/dummy_data/volume_data_scenarios.md`
- `docu/dummy_data/volume_expected_changes.md`
- `docu/dummy_data/volume_validation_result.md`
- `docu/dummy_data/volume_restore_guide.md`

## 미수행 또는 남은 검증

- 기본 이미지 없음·잘못된 URL·삭제 직후 상태의 최신 브라우저 회귀
- CDV 계정 기반 전체 route desktop/mobile smoke
- 실제 동시 지원·초대 수락 HTTP E2E
- 실제 YouTube/OpenAI key smoke
- CDD/CDV rollback의 실제 DB 실행

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/dummy_data`
- `docu/work_logs/2026-06-24_frontend_default_image_integration.md`
- `docu/work_logs/2026-06-25_creator_connected_dummy_data_apply.md`
- `docu/work_logs/2026-06-25_creator_connected_dummy_volume_data_apply.md`
