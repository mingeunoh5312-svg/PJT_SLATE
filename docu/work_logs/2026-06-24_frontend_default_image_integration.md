# 프런트엔드 기본 이미지 통합 작업 로그

- 작업일: 2026-06-24
- 범위: 프로필·팀·작업물·포트폴리오·공모전 기본 이미지 생성 자료와 프런트 표시 연결

## 작업 목적

사용자가 대표 이미지를 등록하지 않았거나 외부 이미지·YouTube 썸네일 로드에 실패했을 때 문자형 placeholder 대신 서비스 공통 기본 이미지를 표시한다.

## 기본 이미지 자산

| 용도 | 프런트 자산 | 규격 | 원본 보관 |
|---|---|---:|---|
| 프로필 | `frontend/src/assets/defaults/default-profile.webp` | 800×800 WebP | `assets/defaults/default-profile.webp.png` |
| 팀 | `frontend/src/assets/defaults/default-team.webp` | 1200×675 WebP | `assets/defaults/default-team.webp.png` |
| 작업물 | `frontend/src/assets/defaults/default-work.webp` | 1200×675 WebP | `assets/defaults/default-work.webp.png` |
| 포트폴리오 | `frontend/src/assets/defaults/default-portfolio.webp` | 1200×675 WebP | `assets/defaults/default-portfolio.webp.png` |
| 공모전 | `frontend/src/assets/defaults/default-contest.webp` | 1200×675 WebP | `assets/defaults/default-contest.webp.png` |

- 원본 파일은 생성 결과 보관용 PNG이며 프런트 번들에는 포함하지 않는다.
- 실제 화면용 파일은 지정 크기로 변환한 WebP다.
- `frontend/src/constants/defaultImages.js`에서 5개 자산을 공통 export한다.

## 표시 우선순위

| 영역 | 우선순위 |
|---|---|
| 프로필 | 등록 프로필 이미지 → 프로필 기본 이미지 |
| 팀 | 팀 썸네일/대표 이미지 → 팀 기본 이미지 |
| 작업물 | 업로드 대표 이미지 → YouTube 썸네일 → 작업물 기본 이미지 |
| 포트폴리오 | 업로드 썸네일 → 외부/YouTube 썸네일 → 포트폴리오 기본 이미지 |
| 공모전 | 업로드 이미지 → 요청 이미지 → 외부 대표 이미지 → 공모전 기본 이미지 |

실제 이미지 URL 로드에 실패한 경우에도 해당 영역의 기본 이미지로 복구한다. 팀·공모전 이미지 확대 동작은 실제 등록 이미지가 있을 때만 제공하고 기본 이미지는 단순 표시용으로 유지한다.

## 화면 연결 범위

| 화면/컴포넌트 | 반영 내용 |
|---|---|
| `AppLayout.vue` | 로그인 계정 영역의 프로필 기본 이미지 |
| `FollowListDialog.vue` | 팔로워·팔로잉 목록의 프로필 기본 이미지 |
| `HomeView.vue` | 최근 작업물과 공모전 카드 기본 이미지 |
| `MatchingView.vue` | 사용자·팀 카드, 상세, 보낸 초대/지원, 포트폴리오 요약 기본 이미지 |
| `TeamsView.vue` | 팀 목록·상세·생성/수정 미리보기 기본 이미지 |
| `BoardView.vue` | 작업물 카드·작성 미리보기와 인기 프로필 기본 이미지 |
| `ContestView.vue` | 목록·마감 임박·상세·회사 요청/관리·입력 미리보기 기본 이미지 |
| `ProfileView.vue` | 내 프로필, 팀, 작업물, 포트폴리오와 편집 미리보기 기본 이미지 |
| `PublicProfileView.vue` | 공개 프로필과 포트폴리오 기본 이미지 |

프로필 이미지가 실제 `<img>` 요소로 통일되면서 공통 avatar 스타일에 `object-fit: cover`를 추가했다.

## 생성 자료와 요구사항 문서

- `docu/prompt/default_image_generation_prompts.md`
  - 기본 이미지 5종의 개별 생성 프롬프트, 크기, 금지 요소, 검수 기준을 기록했다.
- `docu/user_temp/todo_common_home_matching_team_image_requirements.md`
  - 기본 이미지 저장 경로와 실제 연결 상태를 최신화했다.
- `docu/prompt/README.md`
  - 기본 이미지 생성 프롬프트 문서를 목록에 추가했다.

## 검증

- `file assets/defaults/* frontend/src/assets/defaults/*`
  - 원본 5개가 PNG이며, 프런트 자산 5개가 지정 크기의 WebP임을 확인했다.
- `rg`로 기본 이미지 export와 화면별 import/사용 위치를 확인했다.
- `npm run build -- --outDir ../.codex-build-check --emptyOutDir` in `frontend`
  - 결과: 성공, 62 modules transformed.
  - 기본 이미지 5개가 빌드 자산으로 출력됨을 확인했다.
  - 500 kB 초과 JavaScript chunk 경고는 남아 있으나 빌드는 통과했다.

## 미수행 검증과 남은 작업

- 실제 브라우저에서 이미지 없음·잘못된 URL·이미지 삭제 직후의 화면을 데스크톱/모바일로 회귀 확인하지 않았다.
- 기본 이미지의 카드별 crop, 원형 프로필 crop, 확대 버튼 비노출 상태를 시각 검수해야 한다.
- `docu/user_temp/todo_0624_dummies_data.md`의 더미 데이터 분석·생성 작업은 이번 기본 이미지 작업에 포함되지 않았다.
- 원본 PNG 파일명은 다운로드 당시의 `.webp.png` 이중 확장자를 유지하고 있다. 자산 배포에는 사용하지 않지만 추후 원본 보관 규칙을 정할 수 있다.

## 참조 경로

- `docu/prompt/default_image_generation_prompts.md`
- `docu/user_temp/todo_0624_dummies_data.md`
- `docu/user_temp/todo_common_home_matching_team_image_requirements.md`
- `frontend/src/constants/defaultImages.js`
- `frontend/src/assets/defaults`
- `frontend/src/components/follows/FollowListDialog.vue`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/views/BoardView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/HomeView.vue`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/PublicProfileView.vue`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/styles/slate.css`
