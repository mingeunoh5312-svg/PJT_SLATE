# 팀 지원자 공개 프로필 확인 기능 수정

## 변경 파일

- `backend/src/main/resources/mappers/TeamMapper.xml`
- `backend/src/test/java/com/slate/teams/TeamMapperApplicationProfileContractTest.java`
- `frontend/src/services/api.js`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/styles/slate.css`

## 응답 계약과 권한

- 팀 지원 목록의 기존 `applicantUserId`를 유지하고 `applicantProfileId`를 추가했다.
- `member_profile`을 지원자의 `userId`와 `status = 'ACTIVE'` 조건으로 LEFT JOIN해 활성 프로필이 없으면 null을 반환한다.
- 지원 목록 API의 기존 `TeamService.assertManager()` 검증을 유지하므로 해당 팀의 팀장·부팀장만 지원 목록과 프로필 ID를 받을 수 있다.
- 새 프로필 API를 만들지 않고 기존 `GET /api/profiles/{profileId}`를 재사용한다.

## 프런트 동작

- 각 지원 항목에 `프로필 보기`를 추가하고 기존 지원/초대 모달 안에서 읽기 전용 미리보기를 표시한다.
- 이름, 한 줄·상세 소개, 공개 지역, 역할, 경력, 장르, 협업 조건, 활성 포트폴리오만 렌더링한다.
- 이메일, 전화번호, 계정 및 기타 비공개 필드는 템플릿에 포함하지 않았다.
- `visibility != PUBLIC` 또는 `activityStatus == HIDDEN`이면 상세 정보를 렌더링하지 않고 비공개 상태를 표시한다.
- 프로필 ID 없음, 조회 중, 비공개, API 오류 상태를 분리했다.
- 요청 순번을 비교해 다른 지원자를 선택한 뒤 늦게 도착한 이전 응답이 화면을 덮어쓰지 않게 했다.
- `지원 목록으로`와 모달 닫기 시 선택 프로필 및 요청 상태를 초기화한다.
- 미리보기 안에서도 기존 수락·거절 함수를 그대로 사용하며 처리 성공 후 지원 목록으로 돌아간다.
- 390px 화면에서 프로필 정보, 태그, 포트폴리오를 한 열로 표시하도록 반응형 스타일을 추가했다.

## 검증

- `mvn -Dtest=TeamMapperApplicationProfileContractTest test`: 1건 PASS
- `mvn test`: 56건 PASS, 실패·오류 0건
- `cd frontend && npm run build`: PASS
- 별도 8081 서버 기동 및 팀장 데모 계정 로그인: PASS
- 인증 후 후속 로컬 API 요청이 실행 환경에서 정지해 실제 지원 목록·프로필 데이터 응답 확인은 완료하지 못했다.
- 자동 브라우저 연결 제약으로 desktop/390x844 실제 클릭, overflow, console 검증은 수행하지 못했다.
- 데이터 변경을 유발하는 지원 수락·거절은 실행하지 않았고 기존 함수 연결만 확인했다.
- SQL schema와 seed는 수정하지 않았다.

