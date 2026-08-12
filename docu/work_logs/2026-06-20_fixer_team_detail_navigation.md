# 팀 상세 모달·계획 페이지 이동 수정

## 변경 파일

- `frontend/src/views/TeamsView.vue`
- `frontend/src/router/index.js`
- `frontend/src/styles/slate.css`

## 변경 내용

- 팀 상세의 `지원/초대 대기`를 상세 하단 삽입 방식에서 `Teleport` 기반 모달로 변경했다.
- 기존 `applications`, `teamInvitations`, `myInvitations` 상태와 `decideApplication`, `decideInvitation` 함수를 그대로 사용했다.
- 모달에 닫기 버튼, 배경 클릭 닫기, ESC 닫기, `role="dialog"`, `aria-modal="true"`를 적용했다.
- 모달을 열면 포커스 가능한 닫기 버튼으로 포커스를 이동한다.
- `/teams/:teamId/plans` (`teams-plans`) route를 추가하고 기존 계획 목록·폼·상태 변경 UI를 해당 route에서 재사용했다.
- `계획 보기`, `계획 보기로 이동`, `전체 일정 보기`를 새 route로 연결했다.
- `전체 일정 보기`는 `?view=schedule#team-plan-schedule`로 이동하고 일정 영역을 강조한다.
- 일정 전용 query로 진입하면 렌더링 완료 후 일정 영역으로 직접 스크롤한다.
- 계획 페이지 상단의 `상세` 버튼으로 기존 팀 상세 화면에 복귀할 수 있다.
- 데스크톱 모달과 560px 이하 하단 시트 스타일을 추가하고 내부 목록만 스크롤되도록 구성했다.

## 기능 보존

- 지원 수락·거절과 받은 초대 수락·거절은 기존 API 함수 및 권한 조건을 유지한다.
- 계획 등록·수정·상태 변경은 기존 폼, 상태, API 함수를 복제 없이 재사용한다.
- 종료 팀의 요청 모달 제한과 계획 수정 제한을 유지한다.
- route 직접 접근 시 기존 `load()`가 `teamId`로 팀, 모집, 지원·초대, 계획 데이터를 다시 불러온다.
- backend, SQL, seed는 이번 작업에서 수정하지 않았다.

## 검증

- `cd frontend && npm run build`: 성공
- 중단 후 재개한 최종 `cd frontend && npm run build`: 성공
- 실행 중인 개발 서버에서 `/teams/1`: HTTP 200
- 실행 중인 개발 서버에서 `/teams/1/plans?view=schedule`: HTTP 200
- Vue 템플릿 컴파일 및 신규 route 번들 생성: 성공
- `activeTeamPanel`, 상세 하단 `requests/plans` 삽입 조건 잔존 여부: 없음
- 세 계획/일정 버튼의 `teams-plans` 연결과 schedule query/hash: 코드 확인 완료
- 닫기 버튼, 배경 클릭, ESC 및 dialog ARIA: 코드 확인 완료
- desktop 및 390x844 대응 스타일: 정적 확인 완료
- 자동 브라우저 연결 기능이 로컬 브라우저 모듈을 불러오지 못해 실제 클릭, 새로고침, overflow, console 검증은 수행하지 못했다.
- 데이터 변경을 유발하는 지원·초대 수락/거절과 계획 CRUD·상태 변경은 실행하지 않았다. 기존 함수 연결과 비활성화 조건만 확인했다.
