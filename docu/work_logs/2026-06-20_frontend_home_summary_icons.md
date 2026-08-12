# 홈 활동 요약 카드 SVG 아이콘 수정

## 작업 범위

- `내 활동 현황` 네 카드의 영문 약어를 의미 기반 인라인 SVG 아이콘으로 교체한다.
- 카드 수치, API, 순서, 링크와 알림 패널 연결은 변경하지 않는다.

## 참조 경로

- `frontend/src/views/HomeView.vue`
- `frontend/src/styles/slate.css`
- `frontend/src/layouts/AppLayout.vue`

## 구현

- `userSummary.icon`을 `team`, `invitation`, `notification`, `deadline` key로 변경했다.
- 기존 `AppLayout` 종 아이콘과 같은 `0 0 24 24` 인라인 SVG 패턴을 사용했다.
- 네 SVG는 `currentColor`, `1.8` 선 굵기, round linecap/linejoin을 공유한다.
- 0건은 muted blue, 초대·알림이 있으면 blue, 일정이 있으면 orange, 실제 overdue 일정이 있을 때만 red tone을 사용한다.

## 검증

- `cd frontend && npm run build`: 통과.
- 데스크톱에서 네 카드 모두 `viewBox="0 0 24 24"`, SVG `22x22px`, stroke `1.8px`, `fill: none`, `currentColor` 적용을 확인했다.
- 네 SVG에 `aria-hidden="true"`, `focusable="false"`가 적용되고 접근 가능한 카드 제목과 설명이 유지되는지 확인했다.
- 0건 네 카드가 모두 muted blue와 `tone-neutral`을 사용해 과도하게 강조되지 않는지 확인했다.
- 일반 데모 계정에서 일정 1건이 `tone-urgent`와 orange 아이콘으로 표시되는지 확인했다.
- 현재 데이터에는 받은 초대/읽지 않은 알림 1건 이상과 overdue 일정이 없어 blue action/red overdue의 실제 데이터 화면은 확인하지 못했고, 조건 분기와 CSS 규칙을 정적으로 확인했다.
- 알림 카드의 마우스 클릭과 Enter가 기존 알림 패널을 열고 열린 상태를 유지하는지 확인했다.
- 참여 팀 카드 클릭 시 기존 `/teams` 이동이 유지되는지 확인했다.
- `390x844`에서 아이콘 배경 `38x38px`, SVG `22x22px`, 카드 4개 정렬, 카드/문서 가로 overflow 없음과 알림 패널 열기를 확인했다.
- 브라우저 console error/warning 없음.
- `git diff --check`: 통과.

## 남은 사항

- blue action/red overdue 상태는 해당 건수를 가진 검증 데이터가 준비되면 실제 화면 색상을 추가 확인할 수 있다.
