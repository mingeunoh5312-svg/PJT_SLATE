# 공통·메인·매칭·팀 TODO 잔여 작업 로그

작성일: 2026-06-24

## 기준 문서

- `docu/prompt/todo_common_home_matching_team_fixer_prompt.md`
- `docu/user_temp/todo.md`
- `docu/user_temp/260623 Codex Log.md`
- `docu/work_logs/2026-06-23_todo_common_home_matching_team_fixer.md`

## 이번 추가 변경

- 우측 상단 알림 패널에 Home과 동일한 노출 제한과 이동 경로를 적용했다.
  - 일반 사용자에게 회사/기업 승인 검토성 알림을 숨긴다.
  - 팀 추천 알림은 `matching-teams?view=saved`로 이동한다.
  - 팀/공모전/게시글 알림은 각 상세 route로 이동한다.
- 팀 생성/수정 화면에서 별도 `Teams/팀 생성` 툴바를 제거하고 `팀 정보` 폼 헤더 안으로 병합했다.
  - 헤더는 `Teams / 팀 생성` 또는 `Teams / 팀 정보 수정`으로 표시한다.
  - 우측에 `팀 목록`, `삭제`, `팀 저장` 액션을 배치했다.
  - 상태, 팀 이름, 팀 대표 이미지, 지역 묶음, 기간/최대 인원 묶음, 설명 순서로 재배치했다.
- 팀 종료 확인 UI를 화면 폭에 따라 분기했다.
  - 데스크톱은 종료 버튼 아래 popover 확인 UI.
  - 모바일은 하단 modal 확인 UI.
  - 확인 문구와 버튼은 `팀 작업을 종료하시겠습니까?`, `취소`, `작업 종료`를 유지한다.
- 팀 목록 진행/종료 분리 필터에서 누락됐던 진행 상태 상수를 DB 기준 코드로 보강했다.
  - 진행 중 상태: `RECRUITING`, `IN_PROGRESS`, `RECRUITMENT_CLOSED`, `CLOSING`
  - 종료 상태: `ENDED`

## 변경 파일

- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/styles/slate.css`

## 검증

- `npm run build` in `Slate/frontend`
  - 결과: 성공.
  - 참고: Vite chunk size 경고는 기존 번들 크기 경고이며 빌드는 통과.
- `mvn test` in `Slate/backend`
  - 결과: 성공.
  - 총 96개 테스트 통과.
- 서버 상태 확인
  - `http://127.0.0.1:5174/`: 200
  - `http://127.0.0.1:8080/api/references/genres`: 200
- Chrome headless smoke
  - 비로그인 route: `/`, `/matching/members`, `/matching/members?view=following`, `/matching/teams`, `/matching/teams?view=saved`, `/teams`, `/teams/new`, `/teams/1`, `/teams/1/close`, `/contests`, `/contests?view=saved`
  - 로그인 route: `/matching/members`, `/matching/members?view=following`, `/matching/teams`, `/matching/teams?view=saved`, `/teams`, `/teams/new`, `/teams/1`, `/teams/1/close`
  - viewport: 1310x900, 960x900, 390x844
  - 결과: 확인 route 모두 `#app` 렌더링, document 가로 overflow 0.
  - `/teams`의 `ACTIVE_TEAM_STATUSES is not defined` console error를 발견해 수정했고, 재확인에서 JS console error는 재현되지 않았다.

## 남은 위험과 추적

- 공통 안내/오류/성공 문구 전체를 hover/modal로 전환하는 작업은 범위가 전역에 걸쳐 있어, 이번에는 팀 종료 확인 UI와 이미지 preview/주요 action 주변 메시지 개선 범위에서 처리했다.
- 메인 카드 배경 이미지는 사용자가 요청한 대로 임의 기본 이미지를 추가하지 않았다. 필요한 이미지 목록은 `docu/user_temp/todo_common_home_matching_team_image_requirements.md`에서 추적한다.
- 팀원 매칭의 기준 모집 역할은 현재 API 초대/AI 추천 계약상 대표 `slotId`가 필요하다. UI 필터의 복수 역할 선택은 지원하지만, 실제 초대 기준 slot은 기존 선택 계약을 유지한다.
