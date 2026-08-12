# 프론트엔드 데스크톱 고정 폭 작업 로그

작성일: 2026-06-24

## 기준 문서

- `Agent.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/prompt/todo_common_home_matching_team_fixer_prompt.md`
- `docu/work_logs/2026-06-23_todo_common_home_matching_team_fixer.md`

## 작업 범위

- 화면 폭이 모바일 전환 기준인 920px보다 크고 1310px 이하인 구간에서 데스크톱 블럭 크기가 줄어들지 않도록 고정했다.
- 사용자가 브라우저에서 가로 스크롤로 화면을 넘겨도 된다는 지시에 따라, 해당 구간의 기준 레이아웃 폭을 1310px로 유지했다.
- 홈 화면에서 1180px 이하 media query로 `최근 등록된 작업물`이 2열로 바뀌어 블럭 높이가 커지던 동작을 고정 구간에서는 1310px 기준 4열로 유지하도록 보정했다.
- 지시 범위 밖의 기능, 문구, 이미지, 디자인 개편은 추가하지 않았다.

## 변경 파일

- `frontend/src/styles/slate.css`

## 변경 내용

- `@media (min-width: 921px) and (max-width: 1310px)` 규칙을 추가했다.
- 고정 구간에서 `.app-shell`, `.auth-app-stage`의 폭을 `1310px`로 고정했다.
- 고정 구간에서 `.app-shell`의 사이드바/본문 grid 기준을 유지했다.
- 고정 구간에서 홈 화면 내부 콘텐츠 표시가 중간 breakpoint를 따라 바뀌지 않도록 다음을 1310px 기준으로 유지했다.
  - `.home-data-dashboard .home-hero` 2열 구성
  - `.home-data-dashboard .home-hero-copy` padding
  - `.home-data-dashboard .home-summary-grid` 4열 구성
  - `.home-data-dashboard .home-work-list` 4열 구성

## 검증

- `npm run build -- --outDir ../.codex-build-check --emptyOutDir` in `Slate/frontend`
  - 결과: 성공.
  - 참고: Vite chunk size 경고는 발생했으나 빌드는 통과했다.
  - `dist`를 수정하지 않기 위해 임시 출력 경로를 사용했고, 검증 후 `Slate/.codex-build-check`를 삭제했다.
- `git diff --check -- Slate\frontend\src\styles\slate.css`
  - 결과: 성공.

## 미수행 검증 및 남은 확인

- 실제 브라우저에서 1310px, 1180px, 960px, 921px, 920px 이하 화면의 시각 검증은 수행하지 않았다.
- 현재 작업 트리에는 이 작업 범위 밖의 변경 파일이 함께 존재하므로, 이번 로그는 `frontend/src/styles/slate.css`의 데스크톱 고정 폭 변경만 대상으로 한다.