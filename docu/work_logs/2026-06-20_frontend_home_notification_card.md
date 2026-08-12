# 홈 읽지 않은 알림 카드 UX 수정

## 작업 범위

- 홈 `내 활동 현황`의 `읽지 않은 알림` 카드를 기존 상단 알림 패널과 연결한다.
- 알림 route, 알림 수 계산, 종 버튼과 패널 기능은 변경하지 않는다.

## 참조 경로

- `frontend/src/views/HomeView.vue`
- `frontend/src/App.vue`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/styles/slate.css`

## 원인

- `읽지 않은 알림` 요약 항목의 `to`가 없어 일반 `article`로 렌더링됐고 클릭 이벤트가 없었다.
- 알림 패널 상태와 목록 갱신은 상위 `App.vue`에 있어 홈에서 직접 접근할 수 없었다.

## 변경 내용

- `HomeView`가 알림 요약 카드를 native `button`으로 렌더링하고 `open-notifications`를 emit한다.
- `App.vue`의 `RouterView`가 이벤트를 받아 패널 상태를 `true`로 설정하고 기존 목록 갱신 함수를 호출한다.
- 기존 종 버튼은 같은 열기 함수를 사용하되 열린 상태에서는 기존처럼 닫힌다.
- 요약 카드 button의 기본 스타일을 초기화하고 hover/focus-visible 상태를 추가한다.
- 모바일에서 스크롤 후 연 패널이 화면 위로 벗어나지 않도록 viewport 기준 위치와 하단 탭을 제외한 최대 높이를 적용한다.

## 검증

- `cd frontend && npm run build`: 통과.
- 일반 데모 계정 홈에서 알림 요약 카드가 `button type="button"`과 전용 `aria-label`로 렌더링되는지 확인했다.
- 카드 마우스 클릭 시 기존 상단 알림 패널이 열리는지 확인했다.
- 패널이 열린 상태에서 카드를 다시 클릭해도 닫히지 않는지 확인했다.
- 종 버튼으로 기존 열기/닫기가 모두 유지되는지 확인했다.
- Enter와 Space로 패널이 열리고 focus-visible의 3px outline이 적용되는지 확인했다.
- 참여 팀, 받은 팀 초대, 마감 임박 일정 카드가 모두 기존 `RouterLink /teams`로 남는지 확인하고 각 카드를 실제 클릭해 `/teams` 이동을 확인했다.
- 팀원 데모 계정에서 기존 알림 목록 로딩과 `모두 읽음` 동작 후 패널 유지 및 console error/warning 없음을 확인했다.
- `390x844` 모바일에서 카드 클릭 후 패널이 viewport 내부에 표시되고 하단 탭과 겹치지 않으며 가로 overflow가 없음을 확인했다.
- `git diff --check`: 통과.

## 남은 사항

- 검증 데이터의 알림은 이미 읽음 상태여서 개별 `읽음` 버튼 클릭은 재검증하지 않았다. 관련 핸들러와 패널 내부 기능은 변경하지 않았다.
