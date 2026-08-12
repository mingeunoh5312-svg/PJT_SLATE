# 팀원 찾기 기준 팀·매칭 필터 수정 로그

## 작업 목적

- 팀원 찾기의 팀 선택을 팀장/부팀장 권한 기반 `기준 팀` 영역으로 변경한다.
- 장르, 지역, 협업 조건을 실제 기준 데이터와 일반 매칭 API에 연결한다.
- AI 추천과 후보 상세 이동을 명시적이고 접근 가능한 동작으로 정리한다.

## 읽은 파일

- `frontend/src/views/MatchingView.vue`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/styles/slate.css`
- `backend/src/main/java/com/slate/matching/MatchingController.java`
- `backend/src/main/java/com/slate/matching/MatchingService.java`
- `backend/src/main/resources/mappers/MatchingMapper.xml`
- `sql/02_seed_reference.sql`
- 팀/기준 데이터 응답 확인을 위한 관련 Team/Reference 코드와 mapper

## 변경 파일

- `frontend/src/views/MatchingView.vue`
- `frontend/src/services/api.js`
- `frontend/src/styles/slate.css`
- `backend/src/main/java/com/slate/matching/MatchingService.java`
- `backend/src/test/java/com/slate/matching/MatchingServiceTeamToMembersTest.java`

## 구현 내용

- `/api/teams/mine` 응답 중 `myTeamRole`이 `LEADER`/`SUB_LEADER`이고 팀 상태가 활동 가능한 팀만 기준 팀으로 사용한다.
- 관리 팀 1개는 고정 카드, 2개 이상은 변경 select, 0개는 팀 생성 CTA를 표시한다.
- 모집 공고 `OPEN`, 슬롯 `OPEN`, 잔여 인원 1명 이상인 역할만 선택 가능하게 했다.
- 장르는 `slateApi.genres()`, 지역은 `slateApi.regions(keyword, 100)`, 협업 조건은 `slateApi.codes(['COLLABORATION_CONDITION'])`을 사용한다.
- 지역은 검색어 기반 재조회가 가능하며, 후보 프로필의 공개 `regionId`와 선택한 `regionIds`를 정확 일치로 비교한다. 상위 시도/시군구 확장 일치는 적용하지 않았다.
- 일반 매칭 query 계약은 `teamId`, `slotId`, `genreIds`, `regionIds`, `collaborationCondition`이며 빈 값은 전송하지 않는다.
- 필터 상태를 URL query와 동기화하고 새로고침/직접 접근에서 복원한다.
- AI 추천은 유효한 기준 팀과 OPEN 모집 슬롯이 있을 때만 사용자 클릭으로 실행한다. 일반 필터는 AI 요청에 임의로 전달하지 않는다.
- 후보 카드 전체 클릭을 제거하고 `상세 보기` 버튼을 추가했다. 상세 route에도 기준 팀/슬롯/필터 query를 유지한다.
- 실제 후보가 없으면 샘플 후보 대신 명시적 빈 상태와 필터 초기화/역할 변경 동작을 표시한다.
- 백엔드는 기준 팀 관리 권한, 팀/슬롯 일치, OPEN 상태, 잔여 인원을 검증한다.

## 검증

- `frontend`: `npm run build` PASS
- `backend`: `mvn test` PASS, 55 tests
- 추가 테스트 4개 PASS: 팀장/부팀장 권한, 마감 슬롯, 다른 팀 슬롯, regionId 정확 일치
- 브라우저 기준 팀 1개: `남산 새벽팀` 고정 카드와 OPEN 슬롯 3개 확인
- 브라우저 관리 팀 0개: 팀 생성 CTA, 모집 역할 빈 상태, AI 비활성 이유 확인
- 장르/지역/협업 조건 적용 후 URL과 API query 확인:
  - `teamId=1&slotId=1&genreIds=1&regionIds=4&collaborationCondition=PAID`
- 필터 결과 0건에서 샘플 미노출과 빈 상태 확인
- 필터 초기화, 새로고침 query 복원, 후보 상세 `/matching/members/100?teamId=1&slotId=1` 확인
- AI 추천 클릭 후 `view=ai` 전환과 최대 3개 이내 결과 확인
- 기본 viewport와 `390x844`에서 가로 overflow 0 확인
- 브라우저 console error/warning 0 확인

## 남은 이슈

- 현재 시드에는 한 계정이 관리하는 팀이 2개 이상인 사례가 없어 `기준 팀 변경` select는 코드/빌드 검증만 수행했다.
- OPEN 모집 역할 0개 팀은 현재 로그인 가능한 시드 계정으로 독립 브라우저 검증하지 못했으며, 관리 팀 0개에서 동일 빈 상태 분기는 확인했다.
- 저장/초대는 기존 구현을 유지했으며 이번 검증에서 데이터 변경을 발생시키지 않았다.
