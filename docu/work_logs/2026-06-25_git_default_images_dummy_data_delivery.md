# 기본 이미지·더미 데이터 Git 전달 작업 로그

작성일: 2026-06-25
역할: Git 담당
브랜치: `dev_Slate_0625_default_dummies`

## 작업 범위

- `dev_Slate_0624_user1_v2`의 `e03e0a0`에서 전달 브랜치 생성
- 기본 이미지 원본·프런트 자산·화면 fallback 코드 포함
- CDD/CDV seed·검증·rollback SQL과 관련 문서 포함
- 전체 DB dump와 빈 루트 `package-lock.json` Git 제외
- 공동작업자용 이식 인수인계 문서 작성
- 백엔드, 프런트, 현재 DB와 자산 형식 검증

## Git 전달 정책

포함:

- `assets/defaults`
- `frontend/src/assets/defaults`
- `frontend/src/constants/defaultImages.js`
- 기본 이미지 연결 프런트 코드
- `sql/18`~`sql/23`
- `docu/dummy_data`
- 관련 prompt, 작업 로그, 기준 문서
- `docu/handoff/git_to_collaborator_default_images_dummy_data.md`

제외:

- `database_delivery`
  - 전체 계정, 비밀번호 hash, 감사·운영 로그를 포함한 비공개 dump
- `Slate/package-lock.json`
  - 루트 `package.json` 없이 생성된 빈 lockfile
- `backend/target`, `backend/uploads`, `frontend/node_modules`, `frontend/dist`
- 실제 secret과 로컬 환경 설정

`.gitignore`에 `database_delivery/`와 `/package-lock.json`을 추가했다.

## 실행한 명령과 결과

### Git 상태와 원격 확인

```bash
git status --short --branch
git remote -v
git ls-remote --heads origin refs/heads/dev_Slate_0625_default_dummies
git switch -c dev_Slate_0625_default_dummies
```

결과:

- 기준 브랜치: `dev_Slate_0624_user1_v2`
- 기준 커밋: `e03e0a0`
- 같은 이름의 로컬·원격 브랜치 없음 확인
- 새 브랜치 생성 성공

### 백엔드

```bash
cd backend
mvn test
```

결과:

- `Tests run: 98`
- failures 0
- errors 0
- skipped 0
- `BUILD SUCCESS`

### 프런트엔드

```bash
cd frontend
npm run build
```

결과:

- Vite production build 성공
- 62 modules transformed
- 기본 이미지 WebP 5개가 build output에 포함됨
- JavaScript chunk 500 kB 초과 경고가 있으나 build 실패는 아님

### CDD 현재 DB 읽기 검증

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
```

결과:

- 예상 count 11개 항목 모두 일치
- `CDD_ZERO_ERROR_CHECKS` 23개 항목 모두 0
- 기존 데이터 경고: 업로드 경로 9건, `example.test` URL 9건
- 기존 데이터 경고는 CDD seed가 생성한 값이 아님

### CDV 현재 DB 읽기 검증

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/22_validate_connected_demo_volume_data.sql
```

결과:

- `CDV_ZERO_ERROR_CHECKS` 39개 항목 모두 0
- CDD guard count 모두 일치
- 38개 예상 count 중 37개 일치
- `board_view_log`는 seed 직후 기대값 480 대비 현재 482
- 게시글 조회 후 생성된 CDV 조회 로그 2건 증가이며 `board_view_count_mismatch`는 0
- 전달 직전 DB를 임의로 재시드하지 않았고 현재 상태를 그대로 기록함

공동작업자가 `sql/21_seed_connected_demo_volume_data.sql`을 새로 적용한 직후에는 기존 검증 기록대로 480건이 생성된다. 이후 화면 조회가 발생하면 조회 로그는 증가할 수 있다.

### 이미지 형식

```bash
file assets/defaults/* frontend/src/assets/defaults/*
```

결과:

- 원본 5개: PNG
- 프런트 자산:
  - 프로필 800×800 WebP
  - 팀·작업물·포트폴리오·공모전 1200×675 WebP

## 인수인계

공동작업자는 다음 문서를 우선 확인한다.

- `docu/handoff/git_to_collaborator_default_images_dummy_data.md`
- `docu/dummy_data/restore_guide.md`
- `docu/dummy_data/volume_restore_guide.md`
- `docu/dummy_data/test_accounts.md`
- `docu/dummy_data/volume_test_accounts.md`

## 남은 이슈

- 기본 이미지 없음·잘못된 URL·삭제 직후 상태의 최신 브라우저 회귀
- CDV 계정 기반 전체 route desktop/mobile smoke
- CDD/CDV rollback 실제 실행
- 실제 YouTube/OpenAI key smoke
- 실제 동시 지원·초대 수락 HTTP E2E

## 참조 경로

- `Agent.md`
- `.gitignore`
- `docu/handoff/git_to_collaborator_default_images_dummy_data.md`
- `docu/work_logs/2026-06-24_frontend_default_image_integration.md`
- `docu/work_logs/2026-06-25_creator_connected_dummy_data_apply.md`
- `docu/work_logs/2026-06-25_creator_connected_dummy_volume_data_apply.md`
- `docu/dummy_data`
- `sql/18_seed_connected_demo_data.sql`
- `sql/19_validate_connected_demo_data.sql`
- `sql/20_rollback_connected_demo_data.sql`
- `sql/21_seed_connected_demo_volume_data.sql`
- `sql/22_validate_connected_demo_volume_data.sql`
- `sql/23_rollback_connected_demo_volume_data.sql`
