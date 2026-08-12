# 팀원 매칭 기준 팀·모집 역할 변경 결함 수정 프롬프트

## 사용 목적

`/matching/members`에서 기준 팀이나 모집 역할을 한 번 선택한 뒤 다시 변경하기 어렵고, 필터 초기화 후에도 기존 기준값이 URL과 화면 상태에 남거나 자동 복원되는 결함을 프런트엔드에서만 수정한다.

## 프롬프트

```text
당신은 Slate 팀원 매칭 화면의 기준 팀·모집 역할 선택 결함을 수정하는 프런트엔드 수정자입니다. 원인 설명이나 제안에서 멈추지 말고 현재 코드를 읽은 뒤 좁은 범위로 구현하고, 실제 조작 검증과 작업 로그까지 완료하세요.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

## 절대 제한

- 백엔드는 절대로 수정하지 마세요.
- `backend/**`, `sql/**`, 백엔드 설정, Mapper, Java 테스트는 수정 금지입니다.
- 현재 제공되는 API 계약과 응답만 사용하세요.
- 이 작업을 이유로 백엔드 API, DB 스키마, seed 데이터를 추가하거나 변경하지 마세요.
- 프런트 문제를 백엔드 변경으로 우회하지 마세요.
- 관련 없는 사용자 변경을 되돌리거나 정리하지 마세요.
- 사용자 지시 없는 commit, push, PR을 하지 마세요.

수정 허용 범위:
- `frontend/src/views/MatchingView.vue`
- 필요한 경우에만 `frontend/src/styles/slate.css`
- 필요한 경우에만 `frontend/src/router/index.js`
- 작업 결과 기록을 위한 `docu/work_logs/**`

`frontend/src/services/api.js`는 현재 API 호출 계약 확인을 위해 우선 읽기만 하세요. 기존 함수의 명백한 프런트 직렬화 결함이 확인되지 않는 한 수정하지 마세요.

## 먼저 읽을 파일

1. `Agent.md`
2. `docu/00_common/reference_policy.md`
3. `docu/03_mvp_scope/mvp_decisions.md`
4. `docu/06_frontend/frontend_baseline.md`
5. `docu/work_logs/2026-06-20_frontend_matching_algorithm_filters.md`
6. `docu/work_logs/2026-06-20_fixer_matching_member_filters.md`
7. `frontend/src/views/MatchingView.vue`
8. `frontend/src/services/api.js`
9. `frontend/src/router/index.js`
10. `frontend/src/styles/slate.css`

외부 참조가 필요하면 아래 문서만 읽기 전용으로 확인하세요. 해당 폴더는 절대로 수정하지 마세요.

- `../prototype_3/docu/README.md`
- `../prototype_3/docu/03_roles/fixer.md`
- `../prototype_3/docu/04_pages/matching.md`

## 확인된 현상

팀원 찾기 화면에서 다음 문제가 보고됐습니다.

1. 기준 팀을 한 번 설정하면 이후 다른 관리 가능 팀으로 변경하기 어렵습니다.
2. 모집 역할을 한 번 설정하면 같은 팀의 다른 OPEN 모집 역할로 다시 변경하기 어렵습니다.
3. 필터 초기화를 실행해도 기준 팀과 모집 역할이 기존 값으로 남거나 URL query에서 다시 복원됩니다.
4. 화면 상태, URL의 `teamId`/`slotId`, 실제 일반 매칭 API 요청값이 서로 다를 가능성이 있습니다.

현재 코드에서 특히 다음 흐름을 추적하세요.

- `load()`의 query 복원 및 기본값 자동 선택
- `loadRecruitments()`가 `route.query.slotId`를 읽는 시점
- `onTeamChange()`와 `onSlotChange()`의 비동기 실행 순서
- `resetFilters()`와 `applyFilters()`의 책임 범위
- `matchingListQuery()`, `replaceCurrentQuery()`, `syncMemberQuery()`
- route query watcher와 `suppressNextRouteLoad`
- 느린 모집 역할 요청과 일반 매칭 요청의 request id 처리

## 목표 동작

### 기준 팀 변경

- 관리 가능한 팀이 2개 이상이면 사용자가 기준 팀을 반복해서 변경할 수 있어야 합니다.
- A팀 → B팀 → A팀처럼 여러 번 변경해도 선택값이 이전 팀으로 되돌아가면 안 됩니다.
- 기준 팀 변경 즉시 이전 팀의 모집 역할 목록과 선택값을 폐기하세요.
- 새 팀의 모집 역할 목록을 불러오는 동안 이전 팀의 역할이 화면이나 요청에 사용되면 안 됩니다.
- 새 팀의 모집 역할을 불러온 뒤 사용자가 새 역할을 선택할 수 있어야 합니다.
- 이전 URL의 `slotId`가 새 팀 선택이나 새 역할 선택을 덮어쓰면 안 됩니다.

### 모집 역할 변경

- 같은 팀에 OPEN이고 잔여 인원이 있는 역할이 여러 개라면 반복 변경할 수 있어야 합니다.
- 역할 변경 후 화면의 선택값, URL의 `slotId`, 일반 매칭 API의 `slotId`가 모두 같아야 합니다.
- 역할 변경 때마다 이전 일반 매칭 결과와 AI 추천 상태를 안전하게 초기화하고 새 일반 결과를 조회하세요.
- 느린 이전 요청 응답이 최신 역할의 결과를 덮지 않게 기존 request id 방어를 유지하거나 보강하세요.

### 필터 초기화

이번 작업에서 `필터 초기화`는 전체 팀원 매칭 조건을 새로 선택할 수 있는 상태로 되돌리는 동작으로 정의합니다.

- `selectedTeamId`와 `selectedSlotId`를 미선택 상태로 초기화합니다.
- 모집 공고/역할 목록과 기존 팀원 매칭 결과를 초기화합니다.
- 장르, 지역, 경력, 합류 가능 시점, 협업 조건, 지역 검색어도 기존처럼 초기화합니다.
- URL에서 `teamId`, `slotId`와 추가 필터 query를 제거합니다.
- 초기화 직후 route watcher나 `load()`가 첫 번째 팀과 역할을 자동으로 다시 선택하면 안 됩니다.
- 기준 팀과 역할이 다시 선택되기 전에는 팀원 일반 매칭과 AI 추천 요청을 보내지 않습니다.
- 화면에는 기준 팀과 모집 역할을 다시 선택해야 한다는 명확한 상태를 표시합니다.

초기화 이후 사용자가 기준 팀을 선택하면 새 팀의 역할을 불러오고, 역할까지 선택한 시점에만 일반 매칭을 실행하세요. 필터 적용 버튼을 사용하는 현재 UX와 충돌한다면 기존 정보 위계를 유지하면서 가장 일관된 한 가지 흐름으로 정리하고 작업 로그에 선택 이유를 남기세요.

### 최초 진입과 직접 URL

- 유효한 `/matching/members?teamId={teamId}&slotId={slotId}` 직접 접근은 기존처럼 복원할 수 있어야 합니다.
- query의 팀이 현재 사용자가 관리할 수 없는 팀이면 사용하지 마세요.
- query의 역할이 해당 팀의 유효한 OPEN 슬롯이 아니면 사용하지 마세요.
- query가 없는 최초 진입에서 기존처럼 첫 번째 팀/역할을 자동 선택할지는 현재 UX와 기존 로그를 고려해 유지할 수 있습니다.
- 단, 사용자가 명시적으로 `필터 초기화`한 직후에는 자동 선택하면 안 됩니다.
- 이를 구분하기 위해 컴포넌트 내부 상태를 사용할 수 있지만 새로운 전역 상태 관리 도구는 추가하지 마세요.

## 구현 원칙

- 기준 팀, 모집 역할, 추가 필터의 책임을 함수 단위로 명확히 분리하세요.
- `loadRecruitments()`가 항상 오래된 route query를 암묵적으로 읽지 않도록 호출 목적을 분명히 하세요. 필요하면 복원할 선호 slotId를 인자로 전달하는 방식을 검토하세요.
- 사용자의 select 변경 이벤트와 route query 복원 이벤트를 구분하세요.
- 단일 boolean 억제 플래그가 연속 route 변경에서 잘못 소비될 가능성을 점검하세요.
- 숫자형 select 값과 문자열 query 값의 비교를 일관되게 처리하세요.
- loading 중 중복 입력, 빠른 팀 전환, 이전 요청의 늦은 응답을 방어하세요.
- 팀이 1개뿐인 경우 기존 고정 카드 표현을 유지해도 됩니다. 다만 초기화 후에는 사용자가 그 팀을 다시 선택할 수 있는 명확한 동작을 제공해야 합니다.
- 현재 매칭 화면의 대표 이미지 구조, 카드 배치, 반응형 레이아웃을 불필요하게 재작성하지 마세요.

## 보존해야 할 기능

- `/matching/members` 일반 팀원 매칭
- `/matching/teams` 팀 찾기
- 후보 상세 route와 query 보존
- AI 팀원/팀 추천
- 저장, 초대, 지원
- 팔로우/취소
- 점수, 추천 이유, 추가 필터
- 팀 0개, OPEN 역할 0개, API 오류 상태
- desktop/tablet/mobile 반응형

## 금지 사항

- 백엔드 또는 SQL 수정
- API 응답을 프런트에서 하드코딩
- 샘플 팀·슬롯을 실제 선택지처럼 추가
- `window.location.reload()`로 상태 문제 우회
- 전체 매칭 화면 재작성
- 새로운 전역 상태 관리 도입
- route watcher를 제거해 새로고침/직접 URL 복원을 깨뜨리는 변경
- 매칭 요청에 유효하지 않은 teamId 또는 다른 팀의 slotId 전송
- 초기화 직후 자동 기본값 재선택

## 권장 구현 순서

1. 현재 선택 상태와 route query가 변경되는 모든 경로를 표로 정리합니다.
2. 최초 복원, 사용자 팀 변경, 사용자 역할 변경, 필터 적용, 필터 초기화를 서로 구분합니다.
3. 기준 팀 변경 시 이전 역할과 결과를 먼저 폐기하도록 수정합니다.
4. 모집 역할 로딩이 오래된 `route.query.slotId`에 의존하지 않도록 정리합니다.
5. 역할 변경 후 query와 API 요청값을 최신 값으로 동기화합니다.
6. 전체 초기화 상태와 자동 기본값 선택을 구분합니다.
7. 빠른 연속 변경과 느린 응답 경쟁 상태를 검증합니다.
8. 프런트 build와 실제 브라우저 조작을 검증합니다.
9. 작업 로그를 작성합니다.

## 필수 검증

Frontend build:

```bash
cd /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate/frontend
npm run build
```

백엔드 코드를 수정하지 않으므로 Maven test는 이번 작업의 필수 항목이 아닙니다. 백엔드 파일 변경이 감지되면 작업을 중단하고 해당 변경이 기존 사용자 변경인지 확인하세요. 절대로 되돌리거나 추가 수정하지 마세요.

브라우저 검증:

1. `/matching/members` 진입
2. 관리 가능한 팀이 2개 이상인 계정에서 A팀 → B팀 → A팀 변경
3. 각 팀 변경 시 이전 팀 역할이 즉시 제거되는지 확인
4. 새 팀 역할 목록 로드 후 역할 1 → 역할 2 → 역할 1 변경
5. 각 변경 후 select, URL query, 네트워크 요청의 `teamId`/`slotId` 일치 확인
6. 팀/역할 선택 후 추가 필터 적용
7. `필터 초기화` 후 팀, 역할, 추가 필터, 결과, URL query 초기화 확인
8. 초기화 직후 첫 팀/역할이 자동으로 재선택되지 않는지 확인
9. 초기화 후 다시 팀과 역할을 선택해 정상 결과 조회
10. 빠르게 팀을 연속 변경해 마지막 팀의 역할과 결과만 남는지 확인
11. 빠르게 역할을 연속 변경해 마지막 역할의 결과만 남는지 확인
12. 유효 query 직접 접근 및 새로고침 복원
13. 잘못된 teamId/slotId query의 안전한 처리
14. 팀 1개, 팀 0개, OPEN 역할 1개, OPEN 역할 0개 상태
15. AI 추천 실행 전후 팀/역할 변경 시 이전 AI 상태 제거
16. 후보 상세 진입 후 목록 복귀 시 최신 query 유지
17. desktop, tablet, 390x844에서 overflow와 조작 영역 확인
18. console error/warning 0건 확인

검증 데이터에 관리 가능한 팀 2개가 없다면 백엔드나 SQL을 수정해 만들지 마세요. 가능한 기존 계정을 찾고, 없다면 해당 분기는 코드 단위 검증으로 남기고 실제 미검증 사실을 로그에 명시하세요.

## 완료 조건

- 기준 팀을 여러 번 변경할 수 있음
- 모집 역할을 여러 번 변경할 수 있음
- 팀 변경 시 이전 팀 역할이 남지 않음
- 화면 선택값, URL query, API 요청값이 항상 일치함
- 필터 초기화 시 기준 팀, 모집 역할, 추가 필터와 결과가 모두 초기화됨
- 초기화 직후 자동 기본값이 다시 들어오지 않음
- 직접 URL과 새로고침 복원이 유지됨
- 느린 이전 요청이 최신 선택을 덮지 않음
- 기존 매칭/AI/상세/저장/초대/지원/팔로우 기능이 회귀하지 않음
- backend와 SQL 변경 파일이 0개임
- `npm run build` 성공

## 작업 기록

작업 후 다음 파일을 작성하세요.

- `docu/work_logs/YYYY-MM-DD_fixer_matching_basis_selection.md`

로그에 반드시 포함할 내용:

- 작업 목적
- 읽은 문서와 코드
- 재현한 원인
- 변경 파일
- 최초 복원과 사용자 초기화를 구분한 방식
- 팀/역할/query 동기화 방식
- 경쟁 상태 방어 방식
- build 결과
- 브라우저 검증 결과와 미검증 항목
- backend/SQL 변경 0개 확인
- 남은 위험

완료 보고에서는 수정한 결함, 사용자가 실제 화면에서 테스트하는 방법, 실행한 검증, 남은 리스크를 간결하게 구분하세요.
```

## 참조 파일

- `frontend/src/views/MatchingView.vue`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-20_frontend_matching_algorithm_filters.md`
- `docu/work_logs/2026-06-20_fixer_matching_member_filters.md`

