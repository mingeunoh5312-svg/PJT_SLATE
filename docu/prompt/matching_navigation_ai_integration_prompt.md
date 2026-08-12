# 매칭 탐색 구조·AI 추천 통합 수정 프롬프트

## 사용 목적

현재 매칭 화면의 `시작`, `팀원 찾기`, `팀 찾기`, `AI 추천` 4개 탭을 목적 중심의 `팀원 찾기`, `팀 찾기` 2개 탭으로 정리한다. 독립된 AI 추천 화면은 제거하고, 각 탐색 화면 안에서 현재 맥락에 맞는 AI 추천을 명시적으로 실행하도록 통합한다.

## 프롬프트

```text
당신은 Slate 매칭 화면의 정보 구조와 AI 추천 흐름을 수정하는 담당자입니다. 제안만 하지 말고 현재 코드를 읽은 뒤 구현, 회귀 검증, 문서화까지 완료하세요.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

먼저 읽을 파일:
1. Agent.md
2. docu/00_common/reference_policy.md
3. docu/03_mvp_scope/mvp_decisions.md
4. docu/06_frontend/frontend_baseline.md
5. frontend/src/router/index.js
6. frontend/src/views/MatchingView.vue
7. frontend/src/styles/slate.css
8. frontend/src/services/api.js
9. frontend/src/layouts/AppLayout.vue
10. frontend/src/views/HomeView.vue
11. backend/src/main/java/com/slate/matching/MatchingController.java
12. backend/src/main/java/com/slate/matching/AiMatchingRecommendationService.java

현재 작업 트리에는 사용자의 다른 변경이 있을 수 있습니다. 관련 없는 변경을 되돌리거나 정리하지 말고 이번 매칭 화면 범위만 수정하세요.

## 사용자 확정 사항

현재 상단 구조:

- 시작
- 팀원 찾기
- 팀 찾기
- AI 추천

변경할 구조:

- 팀원 찾기
- 팀 찾기

확정 의도:

1. `시작`은 실제 기능이 아니라 나머지 메뉴를 다시 안내하는 중복 화면이므로 제거합니다.
2. `AI 추천`은 독립적인 목적이 아니라 팀원 또는 팀을 찾는 방법이므로 독립 탭에서 제거합니다.
3. 사용자는 먼저 `팀원 찾기` 또는 `팀 찾기`라는 목적을 선택합니다.
4. 각 화면 안에서 필요할 때 `AI로 팀원 추천받기` 또는 `AI로 팀 추천받기`를 직접 실행합니다.
5. AI API는 화면 진입만으로 자동 호출하지 않습니다.

## 최종 사용자 흐름

### 매칭 메뉴 진입

- 사이드바, 모바일 하단 탭 또는 `/matching` 직접 접근 시 `팀 찾기` 화면을 기본으로 엽니다.
- 불필요한 중간 안내 화면과 카드 3개를 보여주지 않습니다.
- 기본 목적지는 `/matching/teams`입니다.

### 팀원 찾기

- 경로: `/matching/members`
- 대상: 현재 사용자가 소속된 팀에 필요한 팀원 후보
- 기존 팀 선택과 모집 역할 선택을 유지합니다.
- 기존 일반 매칭 결과를 기본으로 표시합니다.
- 같은 화면에서 `AI로 팀원 추천받기` 버튼을 제공합니다.
- AI 요청 형식은 기존 `TEAM_TO_MEMBER`를 재사용합니다.
- AI 실행 전 teamId와 slotId가 반드시 유효해야 합니다.

### 팀 찾기

- 경로: `/matching/teams`
- 대상: 현재 사용자의 프로필 조건에 맞는 모집 팀
- 기존 일반 매칭 결과를 기본으로 표시합니다.
- 같은 화면에서 `AI로 팀 추천받기` 버튼을 제공합니다.
- AI 요청 형식은 기존 `MEMBER_TO_TEAM`을 재사용합니다.
- AI 실행 전 profileId가 반드시 유효해야 합니다.

## 상단 탭 수정

- `시작` 탭을 제거합니다.
- 독립 `AI 추천` 탭을 제거합니다.
- `팀원 찾기`, `팀 찾기` 두 탭만 남깁니다.
- 목록과 상세 화면에서도 현재 목적에 맞는 탭이 활성 상태로 보여야 합니다.
- `role="tablist"`를 유지한다면 각 탭에 적절한 tab semantics를 완성합니다. 단순 내비게이션이라면 무리하게 ARIA tab 패턴을 사용하지 말고 링크/버튼 의미를 정확히 적용합니다.
- 키보드 focus 상태와 모바일 터치 영역을 유지합니다.

## 시작 화면 제거와 라우팅

### `/matching`

- `/matching`은 `/matching/teams`로 redirect 또는 replace 처리합니다.
- 브라우저 뒤로가기에 불필요한 `/matching` 중간 단계가 쌓이지 않게 합니다.
- AppLayout에서 매칭 메뉴 활성 상태가 유지되는지 확인합니다.
- HomeView 등 기존 `팀 찾기` CTA가 `/matching/teams`로 정상 이동하는지 확인합니다.

### 기존 `/matching/ai` 호환성

기존 북마크, 문서, 브라우저 기록 또는 외부 링크가 깨지지 않도록 `/matching/ai`를 즉시 삭제해 404로 만들지 마세요.

- 기존 `?mode=members`는 `/matching/members?view=ai`로 호환 이동합니다.
- 기존 `?mode=teams`는 `/matching/teams?view=ai`로 호환 이동합니다.
- teamId와 slotId query가 있으면 팀원 찾기 쪽으로 함께 전달합니다.
- query가 없거나 잘못된 경우 `/matching/teams`의 일반 탐색으로 보냅니다.
- redirect 후에는 독립 AI 탭이나 독립 AI 화면을 렌더링하지 않습니다.

Vue Router의 route redirect, guard 또는 명시적인 호환 컴포넌트 중 현재 구조에 가장 단순한 방식을 선택하세요. 같은 목적을 위해 여러 우회 로직을 중복하지 마세요.

## 각 탐색 화면의 AI 진입 UI

AI 추천 버튼은 현재 탐색 조건과 가까운 위치에 둡니다.

팀원 찾기:

- 버튼 문구: `AI로 팀원 추천받기`
- 팀 및 모집 역할 selector와 시각적으로 연결
- teamId 또는 slotId가 없으면 disabled 처리하거나 기존의 명확한 오류 안내 사용

팀 찾기:

- 버튼 문구: `AI로 팀 추천받기`
- 현재 프로필 기준임을 짧게 설명
- 프로필이 없으면 프로필 작성 CTA 또는 기존 오류 안내 제공

공통:

- AI 추천 버튼을 눌러야만 `slateApi.aiMatchingRecommendations()`를 호출합니다.
- 버튼의 loading, disabled, 재시도 상태를 제공합니다.
- 중복 클릭과 동시에 여러 요청이 실행되지 않게 합니다.
- 일반 필터 버튼과 AI 버튼의 시각적 우선순위를 구분합니다.
- AI를 과장하거나 결과를 보장하는 문구를 사용하지 않습니다.
- 추천은 기존 후보 중 최대 3개라는 현재 정책을 유지합니다.

## 일반 탐색과 AI 결과 전환

AI 결과는 별도 최상위 메뉴나 별도 목적 화면이 아니라 현재 탐색 화면 내부에 표시합니다.

권장 상태 표현:

- 일반 탐색: `/matching/members` 또는 `/matching/teams`
- AI 결과 표시: 같은 경로에 `?view=ai`

구현 기준:

1. 기본 진입 시 일반 매칭 결과를 표시합니다.
2. AI 버튼을 누르면 현재 페이지 안에서 AI 추천 패널을 표시합니다.
3. `일반 결과로 돌아가기` 동작을 제공합니다.
4. AI 결과가 표시돼도 현재 목적 탭은 그대로 활성화됩니다.
5. 팀원 추천에서는 현재 선택한 teamId와 slotId가 유지됩니다.
6. 새로고침과 뒤로가기에서 query와 화면 상태가 모순되지 않아야 합니다.
7. `view=ai`로 직접 접근했더라도 API를 무조건 자동 호출하지 마세요. 이전 결과를 복원할 수 없다면 추천 실행 준비 상태와 버튼을 보여줍니다.
8. 팀원 찾기와 팀 찾기 사이를 이동하면 이전 목적의 AI 결과, 오류, carousel index를 초기화합니다.

query 이름은 기존 코드와 충돌하지 않는지 확인한 뒤 결정할 수 있지만, `mode`는 현재 추천 대상 의미로 사용되고 있으므로 새 화면 상태에는 `view=ai`처럼 역할이 분명한 이름을 우선합니다.

## AI 추천 패널 재사용

현재 `MatchingView.vue`에 있는 다음 기능을 버리지 말고 현재 탐색 화면 안으로 재배치합니다.

- `requestAiRecommendations`
- 최대 3개 결과
- 이전/다음 이동
- 결과 index 선택
- 추천 이유 표시
- 추천 상세 열기
- loading/error/empty 상태

제거할 요소:

- 독립 AI 페이지 전용 제목과 최상위 탭
- AI 화면 안의 `팀원 추천 / 팀 추천` 대상 전환 스위치

대상 전환 스위치가 필요 없는 이유:

- 팀원 찾기 화면에서는 항상 TEAM_TO_MEMBER입니다.
- 팀 찾기 화면에서는 항상 MEMBER_TO_TEAM입니다.
- 다른 추천 대상을 원하면 상단의 두 목적 탭으로 이동합니다.

AI 결과 상세 이동:

- TEAM 대상 → `matching-teams-detail`
- PROFILE/USER 대상 → `matching-members-detail`
- 기존 targetType 처리와 실제 route param을 유지합니다.

## 기존 기능 보존

다음 기능은 이번 정보 구조 변경 후에도 회귀하면 안 됩니다.

- 팀원 일반 매칭 조회
- 팀 일반 매칭 조회
- 팀/모집 역할 선택
- 후보 상세 직접 URL 접근
- 팀 상세 직접 URL 접근
- 북마크/저장
- 팀원 초대
- 팀 지원
- 프로필 팔로우/취소
- 매칭 점수와 추천 이유
- AI 실패 시 backend의 점수 기반 fallback
- 로그인 redirect
- 사이드바와 모바일 하단 매칭 메뉴 활성 상태

상세 화면에서 AI 패널을 억지로 함께 표시하지 마세요. 상세 화면은 기존 후보 상세 역할에 집중하고 목록으로 돌아왔을 때 목적과 가능한 query 상태를 보존합니다.

## 상태 관리와 경쟁 조건

- 일반 매칭 loading/error와 AI loading/error를 분리합니다.
- 필터 또는 팀/슬롯이 바뀌면 이전 AI 결과를 초기화합니다.
- 느린 이전 AI 응답이 새 목적이나 새 조건의 화면을 덮지 않게 request id 또는 AbortController 방식으로 방어합니다.
- component unmount 이후 상태를 변경하지 않습니다.
- AI 요청 실패가 일반 매칭 결과를 지우지 않게 합니다.
- 일반 결과가 비어 있는 상태와 AI 결과가 비어 있는 상태를 구분합니다.

## 반응형 및 접근성

- 데스크톱과 모바일 모두 상단 목적 탭은 2개만 표시합니다.
- 390x844에서 탭, selector, 필터 버튼, AI 버튼이 가로로 넘치지 않아야 합니다.
- AI 결과 carousel 또는 카드가 document overflow를 만들지 않아야 합니다.
- loading 상태에 `aria-busy` 또는 명확한 상태 텍스트를 제공합니다.
- disabled AI 버튼의 이유를 화면 문구로 알 수 있게 합니다.
- 아이콘만 있는 이전/다음 버튼에는 aria-label을 제공합니다.
- 색상만으로 일반/AI 결과 상태를 구분하지 않습니다.

## 금지 사항

- 독립 `시작` 화면 또는 시작 카드 유지
- 독립 `AI 추천` 상단 탭 유지
- 팀원/팀 대상 선택을 AI 패널 안에서 다시 중복 제공
- 페이지 진입 시 자동으로 AI API 호출
- AI 응답을 하드코딩하거나 샘플 결과를 실제 결과처럼 표시
- 기존 AI backend API 계약을 이유 없이 변경
- 새로운 전역 상태 관리 또는 UI 라이브러리 추가
- 관련 없는 Matching 디자인 전면 재작성
- 관련 없는 backend, SQL, 홈, 팀 화면 수정
- 기존 사용자 변경사항 revert
- 사용자 지시 없는 commit, push, PR

## 권장 구현 순서

1. 현재 MatchingView의 route별 조건과 watch/load 흐름을 도식화합니다.
2. `/matching`과 `/matching/ai`의 호환 라우팅 정책을 먼저 확정합니다.
3. `isMatchingHomeRoute`, `isAiRoute`, `openAiPage`, AI 대상 전환 로직 중 제거·대체 대상을 정리합니다.
4. 상단을 두 목적 탭으로 변경합니다.
5. 일반 목록 화면 안에 목적별 AI 실행 버튼과 패널을 통합합니다.
6. query, 뒤로가기, 새로고침, 상세 이동 상태를 정리합니다.
7. 경쟁 상태와 loading/error/empty를 검증합니다.
8. build와 실제 브라우저 회귀 검증을 수행합니다.

## 필수 검증

Frontend:

```bash
cd frontend
npm run build
```

라우팅:

1. `/matching` → `/matching/teams`
2. `/matching/members` → 일반 팀원 탐색
3. `/matching/teams` → 일반 팀 탐색
4. `/matching/ai?mode=members&teamId=1&slotId=2` → 팀원 찾기의 AI 준비 상태로 호환 이동하며 query 보존
5. `/matching/ai?mode=teams` → 팀 찾기의 AI 준비 상태로 호환 이동
6. 잘못된 AI mode → 팀 찾기 일반 화면
7. 상세 직접 URL과 뒤로가기 정상 동작

기능:

1. 팀원 찾기에서 팀/모집 역할 선택 후 AI 추천 실행
2. 팀/슬롯 미선택 시 요청 차단과 안내
3. 팀 찾기에서 현재 프로필 기준 AI 추천 실행
4. 프로필 누락 시 요청 차단과 안내
5. AI 최대 3개 결과와 추천 이유 확인
6. AI 결과에서 올바른 상세 화면 이동
7. 일반 결과로 복귀
8. 목적 탭 변경 시 이전 AI 상태 제거
9. AI API 실패 후 일반 결과 유지
10. 빠른 중복 클릭과 느린 응답 경쟁 상태 확인

회귀:

1. 북마크/저장
2. 팀원 초대
3. 팀 지원
4. 팔로우/취소
5. 로그인 redirect
6. 사이드바 및 모바일 하단 탭
7. desktop/tablet/390x844 overflow
8. console error 0건

Backend API 계약을 수정하지 않았다면 backend 변경은 하지 마세요. 불가피하게 backend를 수정했다면 이유를 기록하고 다음도 실행합니다.

```bash
cd backend
mvn test
```

## 완료 조건 및 보고

- 매칭 상단에 `팀원 찾기`, `팀 찾기`만 남음
- `/matching` 중간 시작 화면이 제거되고 팀 찾기로 바로 진입
- AI 추천이 각 탐색 목적 내부에 통합됨
- AI 호출은 사용자의 명시적 버튼 클릭으로만 발생
- 기존 `/matching/ai` 링크가 호환 이동
- 일반 매칭과 기존 저장·지원·초대·팔로우 기능 유지
- frontend build 성공
- 수행한 브라우저 검증과 미수행 항목을 구분해 보고

작업 후 `docu/work_logs/YYYY-MM-DD_matching_navigation_ai_integration.md`를 작성하세요. 다음 내용을 포함합니다.

- 변경 파일
- 제거한 route 상태와 UI
- `/matching`, `/matching/ai` 호환 정책
- 각 목적별 AI payload
- 일반/AI 결과 상태 전환 방식
- 경쟁 상태 방어 방식
- build 및 브라우저 검증 결과
- 남은 문제와 위험
```

## 참조 파일

- `frontend/src/router/index.js`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`
- `frontend/src/services/api.js`
- `backend/src/main/java/com/slate/matching`
