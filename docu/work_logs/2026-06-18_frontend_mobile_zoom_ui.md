# 모바일 확대 공통 UI 수정 로그

## 작업 범위

- `Slate/frontend`의 `AppLayout` 공통 모바일 하단 탭 밀도와 콘텐츠 하단 여백을 조정한다.
- 라우팅, 인증, 탭 구성과 동작은 변경하지 않는다.

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/11_reviews/test_gap_report.md`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/styles/slate.css`
- `frontend/src/App.vue`
- `frontend/index.html`

## 재현 조건

- 브라우저 확대 때문에 CSS viewport 폭이 `920px` 이하가 되면 데스크톱 사이드바에서 모바일 하단 탭으로 전환된다.
- 수정 전 `768x1024` viewport에서 하단 탭 높이와 각 탭의 최소 높이는 `66px`, 아이콘은 `18px`, 라벨은 `11px`로 계산됐다.
- 같은 조건에서 `.app-shell`의 하단 여백은 별도 값인 `74px`였다.

## 원인

- 브라우저 확대는 CSS 픽셀의 물리 크기를 키우며, 동시에 CSS viewport를 줄여 `@media (max-width: 920px)` 규칙을 활성화한다.
- 모바일 탭의 높이, 탭 최소 높이, 아이콘, 라벨이 독립적인 고정값이고 셸 여백도 별도 값이라 낮아진 viewport에서 내비게이션 밀도를 조절할 공통 기준이 없었다.
- 활성 탭 배경이 링크 전체 `66px` 영역을 채워 실제 크기보다 더 무겁고 크게 보였다.

## 수정 파일

- `frontend/src/styles/slate.css`
  - 모바일 하단 내비게이션 높이/아이콘/라벨 CSS 변수를 추가했다.
  - `.app-shell` 하단 여백과 `.bottom-tabs` 실제 높이를 같은 변수로 연결했다.
  - 활성 탭 배경을 탭 전체가 아닌 아이콘 영역으로 제한했다.
  - 5개 탭 열에 `minmax(0, 1fr)`와 텍스트 overflow 방어를 적용했다.
- `docu/work_logs/2026-06-18_frontend_mobile_zoom_ui.md`

## 적용한 반응형 기준

- `max-width: 920px`: 기본 모바일 하단 내비게이션 높이 `58px`, 아이콘 `17~18px`, 라벨 `10~11px`.
- `max-width: 920px and max-height: 640px` 또는 `max-width: 360px`: compact 높이 `52px`, 아이콘 `17px`, 라벨 `10px`.
- 두 높이 모두 권장 최소 터치 영역 `44px`보다 크다.
- safe area는 `env(safe-area-inset-bottom)`을 총높이와 탭 내부 여백에 함께 반영한다.
- `.app-shell`의 `padding-bottom`과 `.bottom-tabs` 높이는 `--mobile-bottom-nav-total-height`를 공통 사용한다.

## 실행한 검증

- 수정 전 `768x1024` 홈 DOM/계산 스타일 확인
  - 탭 `66px`, 셸 여백 `74px`, 아이콘 `18px`, 라벨 `11px` 확인.
- `npm run build` 통과
  - Vite 8.0.13, 98 modules transformed.
- `git diff --check` 통과.
- 브라우저 계산 스타일/레이아웃 검증
  - `1440x900`의 100%, 125%, 150%, 175%, 200%에 해당하는 유효 CSS viewport를 홈과 `/boards`에서 확인.
  - `1024x768`의 100%, 125%, 150%, 175%, 200%에 해당하는 유효 CSS viewport를 홈과 `/boards`에서 확인.
  - `768x1024`, `390x844`에서 홈과 `/boards` 확인.
  - 데스크톱 구간은 하단 탭 비노출과 셸 하단 여백 `0`을 확인.
  - 일반 모바일은 탭/셸 여백 `58px`, 낮은 viewport compact는 `52px`로 일치함을 확인.
  - 모든 확인 조건에서 5개 탭 한 줄 유지, 라벨 잘림/탭 겹침/가로 overflow 없음.
- 인증된 일반 데모 계정으로 `/matching`, `/teams`, `/profile`을 확인했고 공개 `/contests`도 확인했다.
  - `390x844`와 `720x450`에서 공통 탭 높이, 셸 여백, 겹침, overflow를 확인.
- 활성 탭은 링크 전체 배경이 투명하고 아이콘 영역만 연한 배경이 적용됨을 계산 스타일과 화면으로 확인.
- 브라우저 console error/warning 없음.
- `AdminLayout.vue`가 `AppLayout`을 래핑하므로 동일한 공통 스타일 적용 구조임을 확인.

## 남은 제약 또는 미확인 사항

- 브라우저 확대는 CSS 픽셀 자체를 확대하므로 물리 크기를 완전히 고정하지 않는다.
- 자동화 브라우저의 확대 단축키가 zoom 상태를 바꾸지 않아, 기준 viewport를 확대율로 나눈 유효 CSS viewport로 100~200% breakpoint 전환을 재현했다.
- `/admin`은 사용한 일반 데모 계정 권한으로 `/`에 리다이렉트되어 관리자 화면 자체의 브라우저 검증은 수행하지 못했다.
