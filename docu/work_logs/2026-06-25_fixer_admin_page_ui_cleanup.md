# 관리자 페이지 정리와 콘솔 UI 재구성 작업 로그

작성일: 2026-06-25

## 문서 작성 기준

- 사용자 지시가 명시한 관리자 페이지 정리와 미구현/부족 기능 보완 범위만 기록한다.
- 구현된 화면/프론트 동작과 미수행 검증을 분리해 기록한다.
- 백엔드 API 계약, DB 스키마, 관리자 권한 정책은 변경 완료 범위로 쓰지 않는다.
- 실제 토큰, 접속 코드, 환경변수, secret 값은 기록하지 않는다.

## 작업 범위

- 대상 화면:
  - `/admin`
  - `/admin/users`
  - `/admin/posts`
  - `/admin/teams`
  - `/admin/companies`
  - `/admin/reports`
  - `/admin/files`
  - `/admin/contests`
  - `/admin/demo-access`
  - `/admin/notifications`
  - `/admin/roles`
  - `/admin/logs`
  - `/admin/score-policies`
- 주요 파일:
  - `frontend/src/views/AdminView.vue`
  - `frontend/src/styles/slate.css`
- 제외 범위:
  - 백엔드 컨트롤러/서비스/매퍼
  - DB 스키마
  - 관리자 권한 코드 자체의 정책 변경
  - 실제 운영 데이터로 승인/거절 mutation을 실행하는 E2E 검증

## 반영 내용

### 관리자 대시보드 데이터 정리

- 관리자 대시보드의 임의 fallback 수치(`12건`, `27건`, `128건` 등)를 제거하고 실제 로드된 데이터 기준으로만 표시하도록 정리했다.
- 권한이 없는 관리자 메뉴는 노출하지 않도록 `adminPanelPermissions`와 `visibleAdminNavigationItems`를 추가했다.
- 직접 URL 접근으로 권한 없는 관리 영역에 들어온 경우 필요한 권한 안내와 대시보드 이동 버튼을 표시하도록 했다.

### 운영 액션 사유 입력 보완

- 회사 승인/거절 처리에 관리자 처리 사유 입력을 추가했다.
- 공모전 개설 요청 승인/거절 처리에 관리자 처리 사유 입력을 추가했다.
- 공모전 종료/재개 처리에 상태 변경 사유 입력을 추가했다.
- 기존 고정 문구였던 `샘플 관리자 승인`, `보완 필요`, `관리자 종료 처리`, `관리자 재개 처리` 대신 화면 입력값을 API payload로 전달하도록 바꿨다.
- 사유가 비어 있으면 API 호출 전에 오류 메시지를 표시하고 실행하지 않도록 했다.

### 관리자 UI 재구성

- 기존의 큰 가로 메뉴와 대형 카드 중심 대시보드를 `상단 콘솔 헤더 + 업무 내비게이션 레일 + 조밀한 요약/작업 패널` 구조로 변경했다.
- 데스크톱에서는 왼쪽 업무 내비게이션 레일과 오른쪽 작업 영역을 2열로 배치했다.
- 모바일에서는 업무 레일과 요약 지표를 가로 스크롤 스트립으로 전환해 첫 화면에서 우선 처리 영역이 지나치게 아래로 밀리지 않도록 했다.
- `검수와 계정`, `콘텐츠 운영`, `시스템 관리` 그룹으로 관리자 모듈을 재배치했다.
- 권한 태그는 전체 작업 화면 상단 카드에서 제거하고, 데스크톱 레일 하단 compact 표시로 이동했다.
- 기존 회원/팀/게시글/공모전/파일/알림/권한/로그/점수 정책의 실제 관리 폼과 API 연결은 유지했다.

## 검증 결과

- 프론트엔드 빌드:
  - 명령: `npm run build` in `frontend`
  - 결과: 성공
  - 참고: Vite chunk size 경고는 기존 번들 크기 경고이며 빌드는 통과했다.
- 로컬 개발 서버:
  - 확인: `http://127.0.0.1:5174` 응답 200
- 브라우저 레이아웃 smoke:
  - 도구: Playwright + 로컬 Chrome channel
  - 방식: 관리자 인증/권한/API 응답을 mock 처리하고 `/admin` 레이아웃 확인
  - 데스크톱: `1440x1000`에서 `.admin-console-layout` 렌더링과 page error 0건 확인
  - 모바일: `390x900`에서 레일 높이 62px, 요약 스트립 높이 110px로 축소 확인
  - 참고: 기본 Playwright bundled Chromium은 설치되어 있지 않아 실패했고, 로컬 Chrome channel로 재검증했다.
- Demo Access gate:
  - `VITE_DEMO_ACCESS_GATE=true` 환경에서 `/admin`이 `/demo-access`로 redirect되는 것을 확인했다.
  - 레이아웃 smoke에서는 sessionStorage에 mock demo access code를 주입해 관리자 화면을 확인했다.
- 백엔드 검증:
  - 수행하지 않음.
  - 사유: 이번 작업은 프론트 화면 구조, 스타일, 기존 API payload 입력 보완으로 제한했고 백엔드/API/DB를 수정하지 않았다.

## 이번 작업에서 의도적으로 변경하지 않은 내용

- 관리자 권한 코드 목록과 권한 정책은 변경하지 않았다.
- 회사 승인, 공모전 요청, 공모전 상태 변경 API endpoint는 변경하지 않았다.
- 관리자 게시글/팀/회원/파일/신고/알림/권한/점수 정책의 backend 계약은 변경하지 않았다.
- 실제 운영 데이터에 대해 승인/거절/상태 변경 mutation을 실행하지 않았다.
- `dist` 생성물은 빌드 결과로만 생성됐으며 문서 작업 범위에는 포함하지 않는다.

## 남은 확인 사항

- 실제 백엔드와 관리자 계정으로 회사 승인/거절, 공모전 요청 승인/거절, 공모전 종료/재개 사유가 감사 로그에 의도대로 남는지 확인해야 한다.
- `/admin/users`, `/admin/posts`, `/admin/teams` 등 상세 관리 route의 실제 데이터 기반 시각 QA가 필요하다.
- Demo Access gate를 켠 실제 프론트/백엔드 조합에서 관리자 접근과 보호 리소스 회귀 smoke가 필요하다.
- 관리자 화면은 여전히 `AdminView.vue` 단일 파일에 많은 기능이 집중되어 있어, 이후 컴포넌트 분리 리팩터링 여지가 있다.

## 변경 파일

- `frontend/src/views/AdminView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-25_fixer_admin_page_ui_cleanup.md`
- `docu/13_work_status/current_and_completed_work.md`
- `docu/06_frontend/frontend_baseline.md`

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_common/document_structure.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/13_work_status/current_and_completed_work.md`
- `docu/06_frontend/frontend_baseline.md`
- `frontend/src/views/AdminView.vue`
- `frontend/src/styles/slate.css`
- `frontend/package.json`
