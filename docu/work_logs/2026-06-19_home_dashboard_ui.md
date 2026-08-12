# 2026-06-19 홈 대시보드 UI·반응형 작업 로그

## 작업 범위

- 1단계의 실제 API 데이터와 계정별 분기를 유지한 홈 UI 완성
- USER 히어로, 활동 현황, 처리할 활동, 공모전, 작업물 카드 구조 및 시각 계층 개선
- 비로그인 랜딩 히어로, 핵심 기능 3개, 회원가입 CTA 구성
- COMPANY/ADMIN 역할별 CTA와 공개 콘텐츠 재사용
- 섹션별 skeleton, error/retry, empty 상태 및 반응형·접근성 보강
- 백엔드, SQL, 팔로우 UI는 수정하지 않음

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/prompt/home_dashboard_01_data_structure_prompt.md`
- `docu/prompt/home_dashboard_02_ui_responsive_prompt.md`
- `docu/work_logs/2026-06-19_home_dashboard_data.md`
- `frontend/src/views/HomeView.vue`
- `frontend/src/styles/slate.css`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/BoardView.vue`

## 변경 파일

- `frontend/src/views/HomeView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-19_home_dashboard_ui.md`

## 구현 결과

- USER 히어로는 닉네임과 실제 초대·일정 수치로 문구를 구성하고, 팀 찾기·팀 만들기·공모전 CTA를 제공한다.
- 활동 현황 4개는 실제 수치 0을 포함해 표시하고 행동이 필요한 항목만 절제된 강조를 적용한다.
- 활동 목록은 최대 5개로 제한하고 유형, 제목, 설명, D-day/시간, 유효한 route만 표시한다.
- 공모전 카드는 대표 이미지, 주최, 대상, 역할/장르, 상금, 마감일을 표시하며 이미지가 없으면 중립 placeholder를 사용한다.
- 공모전 저장 버튼은 USER에게만 노출하고 `toggleContestSave` API를 재사용하며, 카드 링크와 중첩되지 않는다.
- 작업물은 YouTube 썸네일을 우선하고 작성자, 팀, 미디어 유형, 등록일, 실제 반응 수치를 표시한다.
- 데스크톱 4/2~3열, 태블릿 2열, 모바일 1열을 적용하고 모바일에서는 작업물만 가로 스크롤을 허용한다.
- 홈의 첫 가시 제목을 `h1`, 각 섹션 제목을 `h2`, 카드 제목을 `h3`로 구성했다.

## 실행한 명령과 결과

- `cd frontend && npm run build`: PASS, Vite 92 modules transformed.
- `git diff --check -- frontend/src/views/HomeView.vue frontend/src/styles/slate.css`: PASS.
- `mvn spring-boot:run`: 로컬 브라우저 검증용 backend 기동 PASS.
- 비로그인 데스크톱: 히어로, 핵심 기능, 공개 공모전/작업물, 회원가입 CTA PASS.
- USER(`leader`): 요약 4개, 실제 담당 일정, 공모전 2개, 작업물 4개 PASS.
- USER 공모전 저장 토글: 저장/취소 상태 확인 후 원상 복구 PASS.
- USER(`ai-camera-a`): 0 수치 표시와 `지금 확인할 긴급한 활동이 없습니다.` 빈 상태 PASS.
- COMPANY(`approved-company`): USER 섹션 0개, 저장 버튼 0개, 회사 CTA와 공개 콘텐츠 PASS.
- ADMIN(`admin`): USER 섹션 0개, 저장 버튼 0개, 관리자 CTA와 공개 콘텐츠 PASS.
- backend 미기동 상태: 공모전과 작업물 섹션의 독립 error/retry 상태 PASS.
- 390x844: document 가로 overflow 0, 단일 열, CTA 줄바꿈, 작업물 가로 스크롤 PASS.
- 768px: 활동·공모전·작업물 2열, document 가로 overflow 0 PASS.
- 브라우저 console error: 0건.

## 남은 이슈

- 현재 샘플 공모전은 `representativeImageUrl`이 없어 중립 placeholder로 검증했다. 대표 이미지가 있는 실제 데이터의 네트워크 로딩/오류 표시는 3단계 통합 검증에서 추가 확인할 수 있다.
- 이 작업은 `home_dashboard_02_ui_responsive_prompt.md` 범위다. 전체 계정별 통합 회귀는 `home_dashboard_03_integration_validation_prompt.md`에 남겨두었다.
