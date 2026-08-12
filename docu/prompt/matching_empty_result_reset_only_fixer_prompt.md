# 팀 찾기 프런트 통합 수정 프롬프트

저장 위치: `/Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate/docu/prompt/matching_empty_result_reset_only_fixer_prompt.md`

## 프롬프트

```text
당신은 Slate 매칭 화면의 프런트엔드 수정자입니다. `/matching/teams` 팀 찾기 화면을 백엔드의 실제 매칭 응답에 맞게 정리하고, `/matching/members` 팀원 찾기와 최대한 동일한 UI·UX를 갖도록 구현하세요. 제안에서 멈추지 말고 코드 확인, 구현, build, 브라우저 검증, 작업 로그 작성까지 완료하세요.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

## 절대 제한

- 백엔드와 SQL은 절대로 수정하지 마세요.
- `backend/**`, `sql/**`, Mapper, Java 테스트, seed 데이터, 백엔드 설정은 수정 금지입니다.
- 현재 백엔드가 반환하는 필드와 기존 API 계약만 사용하세요.
- 프런트 문제를 API 또는 DB 변경으로 우회하지 마세요.
- 관련 없는 사용자 변경을 되돌리거나 정리하지 마세요.
- 사용자 지시 없는 commit, push, PR을 하지 마세요.

수정 허용 범위:
- `frontend/src/views/MatchingView.vue`
- 필요한 경우 `frontend/src/styles/slate.css`
- 필요한 경우 `frontend/src/router/index.js`
- 명백한 프런트 직렬화 문제가 있을 때만 `frontend/src/services/api.js`
- 작업 로그 `docu/work_logs/YYYY-MM-DD_fixer_matching_team_search.md`

## 먼저 읽을 파일

1. `Agent.md`
2. `docu/00_common/reference_policy.md`
3. `docu/03_mvp_scope/mvp_decisions.md`
4. `docu/06_frontend/frontend_baseline.md`
5. `docu/work_logs/2026-06-20_frontend_matching_algorithm_filters.md`
6. `docu/work_logs/2026-06-20_fixer_matching_member_filters.md`
7. `frontend/src/views/MatchingView.vue`
8. `frontend/src/styles/slate.css`
9. `frontend/src/router/index.js`
10. `frontend/src/services/api.js`

백엔드 파일은 응답 계약 확인을 위해 읽기만 허용합니다.

- `backend/src/main/java/com/slate/matching/MatchingController.java`
- `backend/src/main/java/com/slate/matching/MatchingService.java`
- `backend/src/main/resources/mappers/MatchingMapper.xml`

## 작업 목표

팀 찾기와 팀원 찾기는 다음 UX 패턴을 공유해야 합니다.

- 동일한 상단 목적 탭
- 동일한 필터 카드의 정보 위계와 버튼 배치
- 동일한 로딩·오류·빈 결과 표현
- 동일한 추천 카드 레이아웃
- 동일한 적합도·추천 이유 표시
- 동일한 목록 → 상세 → 목록 이동
- 동일한 URL query 복원 및 필터 초기화 경험
- 동일한 저장 액션 피드백
- 팀원 찾기는 `초대`, 팀 찾기는 `지원`으로 목적에 맞게 분기

단, 팀 찾기는 현재 로그인 사용자의 프로필을 기준으로 하므로 팀원 찾기의 `기준 팀·모집 역할` 선택기를 억지로 복제하지 마세요. 대신 현재 프로필 기준 추천이라는 설명을 명확히 표시하세요.

## 확인된 문제와 필수 수정

### 1. 샘플 팀 fallback 제거

현재 백엔드 `member-to-teams` 결과가 0건이면 `sampleTeamCards`가 표시됩니다.

- 팀 찾기에서 하드코딩된 샘플 팀 3개를 실제 추천 결과처럼 표시하지 마세요.
- 샘플 팀의 저장·지원 버튼이 성공한 것처럼 안내하는 동작을 제거하세요.
- 샘플 팀만을 위한 분기, 이미지, 텍스트가 다른 화면에서 사용되지 않는다면 정리하세요.
- 실제 결과가 0건이면 팀원 찾기와 동일한 빈 결과 블록을 표시하세요.
- 빈 결과 블록에는 제목, 짧은 안내 문구, `필터 초기화` 버튼만 표시하세요.
- 빈 결과 블록 안에 별도 역할 선택 select나 다른 중복 조작을 넣지 마세요.

권장 문구:

- 제목: `선택한 조건에 맞는 팀이 없습니다.`
- 안내: `필터를 초기화하거나 모집 중인 팀의 조건을 다시 확인해주세요.`
- 버튼: `필터 초기화`

### 2. 팀 찾기 헤더 문구 수정

팀 찾기에서도 현재 `팀에 조건과 가장 잘 맞는 인재를 추천해요.`가 표시됩니다.

- 팀원 찾기: 기존 인재 추천 문구 유지
- 팀 찾기: 현재 프로필에 맞는 모집 팀을 추천한다는 문구로 분기

예시:

`내 프로필과 활동 조건에 가장 잘 맞는 모집 팀을 추천해요.`

### 3. 실제 백엔드 팀 데이터 사용

백엔드의 `member-to-teams` 응답에는 다음 필드가 이미 포함됩니다.

- `teamId`, `teamName`, `teamDescription`
- `regionId`, `publicRegionName`, `regionAnyYn`
- `recruitmentId`, `recruitmentTitle`, `deadlineAt`, `workStartAt`
- `slotId`, `roleId`, `roleName`
- `requiredCount`, `acceptedCount`, `remainingCount`
- `requiredExperienceLevel`, `collaborationCondition`, `roleDuration`
- `teamGenres`
- `score`, `scoreBadge`, `reasons`, `actions`

팀 목록과 상세 화면에서 하드코딩 fallback 대신 위 실제 필드를 사용하세요.

- 팀 설명은 `teamDescription`을 우선 표시하세요.
- 모집 공고 제목과 모집 역할을 구분해 표시하세요.
- 지역, 주요 장르, 잔여 모집 인원, 모집 조건을 실제 필드로 표시하세요.
- 점수와 추천 이유는 백엔드 응답을 그대로 사용하세요.
- 누락 데이터는 `정보 없음`처럼 정직한 상태로 표시하고 임의 데이터를 만들지 마세요.

### 4. 팀 상세 화면을 팀 전용 내용으로 수정

현재 팀 상세가 팀원 후보 상세 template을 공용으로 사용하면서 다음 부적절한 내용을 표시합니다.

- 하드코딩된 `포트폴리오 요약` 카드
- `/profile`로 이동하는 `전체 포트폴리오 보기` 링크
- 팀 응답에 없는 가짜 강점 또는 프로필 중심 섹션
- `shortIntro`를 전제로 한 설명 표시

팀 상세에서는 위 내용을 제거하고 실제 팀·모집 슬롯 정보로 구성하세요.

권장 팀 상세 정보:

- 팀명과 팀 설명
- 활동 지역과 팀 장르
- 모집 공고 제목
- 모집 역할
- 잔여 모집 인원
- 요구 경력
- 협업 조건
- 역할 기간과 작업 시작일·마감일
- 적합도와 추천 이유
- 저장과 지원 CTA

팀원 상세는 기존 프로필·포트폴리오 구조를 유지하세요. 모드별 `v-if` 또는 계산된 view model을 사용해 팀 상세와 팀원 상세의 내용이 섞이지 않게 하세요.

### 5. 같은 팀의 여러 모집 슬롯 식별

백엔드는 팀이 아니라 OPEN 모집 슬롯 단위로 추천 결과를 반환합니다. 같은 팀에 사용자의 역할과 맞는 슬롯이 여러 개면 같은 `teamId`가 반복될 수 있습니다.

- 팀 카드 Vue key에 `teamId`뿐 아니라 `slotId`를 포함하세요.
- 선택한 카드와 상세에서 어떤 슬롯인지 유지하세요.
- `/matching/teams/:teamId` 상세 이동 시 `slotId`를 query에 보존하는 방식을 우선 사용하세요.
- 상세 후보 선택은 `teamId`와 `slotId`를 함께 비교하세요.
- 새로고침, 직접 접근, 목록 복귀 후에도 같은 슬롯이 유지돼야 합니다.
- 지원 요청은 화면에 표시된 슬롯의 `teamId`, `recruitmentId`, `slotId`를 정확히 전송해야 합니다.
- 다른 슬롯의 ID나 첫 번째 슬롯을 임의로 사용하지 마세요.
- 기존 백엔드 API나 route path를 변경할 필요는 없습니다.

### 6. 필터 UI·UX 통일

팀 찾기도 팀원 찾기와 같은 필터 적용·초기화 흐름을 사용하세요.

현재 백엔드가 팀 찾기에 지원하는 필터:

- `genreIds`
- `regionIds`
- `experienceLevel`
- `collaborationCondition`

구현 기준:

- 장르, 지역, 협업 조건은 기존 프런트 연결을 유지하세요.
- 팀 찾기에서도 요구 경력 필터를 제공하려면 기존 `experienceLevel` 계약을 그대로 사용하세요.
- 백엔드가 팀 찾기 필터로 처리하지 않는 값을 임의로 전송하지 마세요.
- 팀원 찾기의 합류 가능 시점 필터를 단순히 복제하지 마세요.
- 필터 적용 후 URL query, select 상태, API 요청값이 같아야 합니다.
- 필터 초기화 후 추가 필터 query와 결과가 초기화돼야 합니다.
- 지역 검색의 기존 데이터 범위 문제는 이번 작업에서 확장하지 마세요.

### 7. 목록·상세 공통 UX

- 실제 팀 결과에는 샘플 프로필 이미지를 붙이지 마세요.
- 팀 이미지가 응답에 없으면 팀명 이니셜 placeholder를 사용하세요.
- 팀 찾기 카드의 `상세 보기`, `저장`, `지원` 버튼 의미를 유지하세요.
- loading 중 이전 결과가 실제 최신 결과처럼 남지 않게 하세요.
- API 오류와 결과 0건을 구분하세요.
- 목록으로 돌아갈 때 현재 필터와 `slotId`를 보존하세요.
- 상세 직접 접근에서 일치하는 팀·슬롯이 없으면 명확한 오류와 목록 이동을 제공하세요.

## 보존해야 할 기능

- `/matching/members` 팀원 찾기 전체 흐름
- 기준 팀·모집 역할 반복 변경
- 팀원 찾기 빈 결과의 `필터 초기화` 단일 CTA
- `/matching/teams` 실제 일반 매칭 조회
- `/matching/members/:userId`, `/matching/teams/:teamId` 직접 접근
- 장르·지역·경력·협업 조건 필터
- AI 팀원 추천과 AI 팀 추천
- 저장, 초대, 지원
- 프로필 팔로우/취소
- 적합도, 추천 이유
- 로그인 redirect
- desktop/tablet/mobile 반응형

## 금지 사항

- 백엔드·SQL·seed 수정
- 하드코딩 샘플을 실제 결과처럼 표시
- 샘플 저장·지원 성공 처리
- 팀 상세에 프로필 포트폴리오 표시
- 팀 상세에서 현재 사용자 `/profile`로 연결
- 같은 팀의 여러 슬롯을 `teamId` 하나로만 식별
- 지원 payload에 fallback ID 사용
- `window.location.reload()`로 상태 문제 우회
- 새로운 전역 상태 관리 또는 UI 라이브러리 도입
- 매칭 화면 전체 디자인 재작성
- 관련 없는 화면 수정

## 권장 구현 순서

1. `member-to-teams` 실제 응답과 프런트 사용 필드를 표로 정리합니다.
2. 샘플 fallback과 샘플 액션을 제거합니다.
3. 팀 찾기 전용 빈 결과 상태를 추가합니다.
4. 목록 카드 문구와 실제 팀 필드 매핑을 수정합니다.
5. `teamId + slotId` 기준 식별과 query 보존을 정리합니다.
6. 팀 상세를 실제 팀·모집 슬롯 데이터로 재구성합니다.
7. 팀 찾기 필터를 백엔드 지원 범위에서 팀원 찾기 UX와 통일합니다.
8. 저장·지원 payload와 목록 복귀를 검증합니다.
9. build와 브라우저 회귀 검증을 수행합니다.
10. 작업 로그를 작성합니다.

## 필수 검증

Frontend build:

```bash
cd /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate/frontend
npm run build
```

백엔드는 수정하지 않으므로 Maven test는 필수 항목이 아닙니다. 작업 전후 `git diff --name-only`로 `backend/**`, `sql/**`에 이번 작업으로 추가된 변경이 없는지 확인하세요. 기존 사용자 변경은 되돌리지 마세요.

브라우저 검증:

1. `/matching/teams`에서 실제 API 결과 표시
2. 백엔드 결과 0건에서 샘플 팀 미노출
3. 결과 0건에서 제목·안내·`필터 초기화` 버튼만 표시
4. 팀 찾기 전용 헤더 문구 표시
5. 장르·지역·경력·협업 조건 적용과 URL/API query 일치
6. 필터 초기화 후 query와 결과 갱신
7. 실제 팀 설명, 모집 공고, 역할, 지역, 장르, 잔여 인원 표시
8. 같은 팀에 슬롯 2개 이상일 때 카드 key 경고 없이 각각 구분
9. 두 슬롯 각각의 상세 진입과 새로고침 시 선택 슬롯 유지
10. 상세 → 목록 복귀 시 필터와 슬롯 query 유지
11. 팀 상세에서 하드코딩 포트폴리오와 `/profile` 링크 미노출
12. 상세에서 실제 팀·모집 조건 표시
13. 저장 API의 `targetType=TEAM`, `targetId=teamId` 확인
14. 지원 API의 `teamId`, `recruitmentId`, `slotId` 정확성 확인
15. API 오류와 빈 결과 상태 구분
16. AI 팀 추천 실행과 상세 이동 회귀 확인
17. `/matching/members` 팀원 찾기 회귀 확인
18. desktop, tablet, 390x844에서 overflow·겹침 확인
19. console error/warning 0건 확인

같은 팀의 복수 슬롯이나 결과 0건을 만들 검증 데이터가 없다면 backend와 SQL을 수정하지 마세요. 가능한 기존 계정을 사용하고, 확인할 수 없는 항목은 코드 검증 결과와 미검증 이유를 작업 로그에 정확히 남기세요.

## 완료 조건

- 팀 찾기와 팀원 찾기의 필터·로딩·오류·빈 결과·카드·상세 이동 UX가 일관됨
- 팀 찾기에서 하드코딩 샘플과 가짜 액션이 제거됨
- 팀 상세가 실제 백엔드 팀·모집 슬롯 데이터로 구성됨
- 하드코딩 포트폴리오와 잘못된 `/profile` 링크가 제거됨
- 같은 팀의 여러 슬롯이 `teamId + slotId`로 구분됨
- 화면에 표시된 슬롯과 지원 payload가 일치함
- 팀 찾기 결과 0건에서 `필터 초기화` 단일 CTA가 표시됨
- 기존 팀원 찾기와 AI·저장·초대·지원·팔로우가 회귀하지 않음
- backend와 SQL의 이번 작업 변경이 0개임
- `npm run build` 성공

## 작업 기록

작업 후 다음 파일을 작성하세요.

- `docu/work_logs/YYYY-MM-DD_fixer_matching_team_search.md`

로그에 반드시 포함할 내용:

- 작업 목적
- 읽은 문서와 코드
- 확인한 백엔드 응답 필드
- 제거한 샘플·하드코딩 UI
- 팀원 찾기와 통일한 UI·UX
- 팀 상세 실제 데이터 매핑
- `teamId + slotId` 식별 및 query 보존 방식
- 저장·지원 payload 검증
- build 결과
- 브라우저 검증 결과와 미검증 항목
- backend/SQL 변경 0개 확인
- 남은 위험

완료 보고에서는 수정 내용, 실제 테스트 방법, 실행한 검증, 남은 리스크를 간결하게 구분하세요.
```

