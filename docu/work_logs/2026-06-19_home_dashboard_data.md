# 2026-06-19 홈 대시보드 데이터·상태 구조 작업 로그

## 작업 범위

- 기존 하드코딩 홈 추천 데이터와 임의 수치를 실제 API 데이터로 교체
- guest, USER, COMPANY, ADMIN 홈 상태 분리
- USER 팀·초대·알림·일정 수치 및 활동 가공
- 공개 공모전과 공개 작업물의 독립 loading/error/empty 상태 구현
- 홈 전용 백엔드·SQL 추가, 추천 매칭, 팔로우 활동 피드는 구현하지 않음

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/prompt/home_dashboard_01_data_structure_prompt.md`
- `frontend/src/views/HomeView.vue`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/App.vue`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/BoardView.vue`

## 작업 계획

| 순서 | 작업 | 상태 |
|---:|---|---|
| 1 | 기존 홈과 실제 API 계약 확인 | DONE |
| 2 | 계정별 상태와 공개/USER 독립 로딩 구현 | DONE |
| 3 | 실제 요약 수치와 활동 가공 구현 | DONE |
| 4 | 기본 분기 UI와 섹션별 상태 구현 | DONE |
| 5 | build 및 계정별 브라우저 검증 | DONE |

## 변경 파일

- `frontend/src/views/HomeView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-19_home_dashboard_data.md`

## 구현 결과

- 공개 공모전과 최신 작업물을 모든 계정에서 독립적으로 조회한다.
- 인증 전용 홈 API는 USER 계정에서만 호출한다.
- USER 홈은 참여 팀, 대기 초대, 읽지 않은 알림, 7일 이내 및 기한 초과 담당 일정을 실제 데이터로 계산한다.
- 비로그인, USER, COMPANY, ADMIN 히어로와 CTA를 분리했다.
- `Promise.allSettled`와 request id를 사용해 부분 실패와 사용자 전환 경쟁 상태를 방어한다.
- 공모전, 작업물, USER 요약, USER 활동의 loading/error/empty 상태를 분리했다.
- USER 활동 조회 실패 시 `새 활동이 없습니다`로 오인하지 않도록 활동 오류를 별도 표시한다.
- 기존 홈의 추천 프로필, 적합도, 임의 공모전과 프로젝트 카드를 제거했다.
- 추천 매칭과 팔로우 활동 피드는 추가하지 않았다.

## 실행 명령 및 결과

- `npm run build`: PASS, Vite 92 modules transformed.
- `git diff --check`: PASS.
- 비로그인 브라우저 검증: PASS.
  - 인증 전용 USER 영역 미노출
  - 공개 공모전 및 최근 공개 작업물 표시
- USER(`leader`) 브라우저 검증: PASS.
  - 참여 팀 1, 대기 초대 0, 읽지 않은 알림 0, 마감 임박 일정 1 표시
  - 실제 담당 일정과 팀 상세 route 표시
- COMPANY(`approved-company`) 브라우저 검증: PASS.
  - USER 영역 0개, 회사 CTA와 공개 섹션 표시
- ADMIN(`admin`) 브라우저 검증: PASS.
  - USER 영역 0개, 관리자 CTA와 공개 섹션 표시
- 로그아웃 상태 초기화 검증: PASS.
  - USER 요약 카드와 활동 영역이 즉시 제거되고 guest 히어로 표시
- API 실패 격리 검증: PASS.
  - 별도 임시 프런트에서 API 연결을 실패시켜도 홈 히어로 유지
  - 공모전과 작업물 오류가 각 섹션에 독립 표시
- 정상 계정별 브라우저 console error: 0건.

## 남은 이슈

- 이 문서는 1단계 데이터·상태 구조 완료 기록이다. 최종 시각 완성도와 전체 반응형 검증은 2단계 UI 프롬프트 범위다.
