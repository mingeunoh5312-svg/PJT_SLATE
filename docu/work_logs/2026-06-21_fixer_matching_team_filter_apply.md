# 팀 찾기 필터 적용 전 조회 차단

## 변경 파일

- `frontend/src/views/MatchingView.vue`

## 구현 내용

- `/matching/teams` 일반 목록에서 URL의 `applied=1`을 추천 팀 조회 여부의 기준으로 사용한다.
- 최초 진입과 필터 query만 있는 직접 URL은 필터 값을 복원하지만 `memberToTeams` API를 호출하지 않는다.
- 미적용 결과 영역에는 `필터를 설정한 후 적용해주세요.`를 표시한다.
- `필터 적용`은 현재 필터와 `applied=1`을 URL에 저장한 뒤 `memberToTeams` API를 한 번 호출한다.
- `applied=1` URL을 새로고침하거나 직접 열면 필터를 복원하고 결과를 다시 조회한다.
- 적용 당시 필터 서명과 화면의 편집 필터를 분리했다.
- 적용 후 필터가 한 번이라도 바뀌면 기존 결과를 숨기고 `변경한 필터를 다시 적용해주세요.`를 표시한다. 값을 다시 원래대로 돌려도 재적용 전에는 기존 결과를 다시 노출하지 않는다.
- 필터 변경 시 진행 중인 일반 추천 요청 ID를 폐기해 느린 이전 응답이 현재 상태를 덮어쓰지 못하게 했다.
- `필터 초기화`는 필터, 일반 결과, 오류, 알림, AI 상태, 적용 서명을 비우고 URL query 전체를 제거한다. 초기화 과정에서는 추천 API를 호출하지 않는다.
- 내부 `router.replace()` query 서명은 route watcher에서 소비하므로 적용 클릭당 중복 조회가 발생하지 않는다.

## 보존 확인

- `/matching/members`는 기존 기준 팀·모집 역할 선택 및 자동 조회 흐름을 유지한다.
- `view=saved`는 기존 저장 팀 전용 분기에서만 목록 API를 호출한다.
- `view=ai`는 일반 추천 API를 자동 호출하지 않고 기존 AI 요청 버튼과 상태를 유지한다.
- 적용된 일반 결과에서 팀 상세로 이동할 때 `applied=1`과 필터 query를 유지하며 목록 복귀 시 결과를 복원한다.
- 저장 및 지원 함수는 변경하지 않았다.

## 검증

- 최초 `/matching/teams`: `memberToTeams` 호출 분기 진입 전 반환 및 카드 배열 미노출 코드 확인
- 필터 query만 있고 `applied` 없음: 필터 복원 후 API 호출 전 반환 코드 확인
- 적용 클릭: URL에 `applied=1`과 `filterQuery()`를 함께 저장하고 동일 `filterQuery()`로 API를 호출하는 코드 확인
- 적용 URL 직접 접근: 적용 필터 서명 저장 후 API 조회 코드 확인
- 초기화: `refreshRequestId` 증가, 결과·오류·AI 상태 초기화, query `{}` 치환, API 미호출 코드 확인
- 필터 재변경: 요청 무효화 및 기존 결과 숨김 코드 확인
- `cd frontend && npm run build`: PASS
- 브라우저 자동화 연결이 실행 환경 메타데이터 오류로 시작되지 않아 desktop/390x844 overflow, network call count, console 검증은 수행하지 못했다.
- backend, SQL, seed는 이번 작업에서 수정하지 않았다.

