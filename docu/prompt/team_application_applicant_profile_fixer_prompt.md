# 팀 지원자 공개 프로필 확인 기능 프롬프트

```text
Slate 팀의 지원/초대 관리에서 팀장이 지원자의 공개 프로필을 확인한 뒤 수락·거절할 수 있게 구현하세요. 현재 코드를 재사용해 좁게 수정하고 build/test/브라우저 검증/로그까지 완료하세요.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

먼저 확인:
- `frontend/src/views/TeamsView.vue`
- `frontend/src/services/api.js`
- `backend/src/main/resources/mappers/TeamMapper.xml`의 `selectApplicationsByTeamId`
- `ProfileController`, `ProfileService`의 `GET /api/profiles/{profileId}` 응답

구현:
1. 지원 목록 응답에 `applicantProfileId`만 최소 추가
   - `member_profile`을 지원자 userId로 연결
   - 활성 프로필이 없으면 null 허용
   - 기존 `applicantUserId`와 지원 계약 보존

2. 지원 항목에 `프로필 보기` 버튼 추가
   - 클릭 시 기존 `GET /api/profiles/{profileId}`로 조회
   - 지원/초대 모달 안에서 지원자 공개 프로필 미리보기 표시
   - 이름, 소개, 공개 지역, 역할, 경력, 장르, 협업 조건, 공개 포트폴리오만 표시
   - 이메일·전화번호·비공개 데이터는 표시하지 않음
   - 프로필 없음/비공개/loading/API 오류 상태를 구분
   - 닫기 또는 `지원 목록으로` 동작 제공
   - 프로필을 본 뒤 기존 수락·거절을 그대로 실행할 수 있어야 함

3. 권한
   - 해당 팀의 팀장/부팀장만 지원 목록과 프로필 미리보기를 볼 수 있어야 함
   - 다른 팀 지원자의 정보에 접근하지 못하게 기존 팀 권한 검증 유지

수정 예상 범위:
- `frontend/src/views/TeamsView.vue`
- `frontend/src/services/api.js`
- 필요한 경우 `frontend/src/styles/slate.css`
- `backend/src/main/resources/mappers/TeamMapper.xml`
- 관련 backend 테스트
- `docu/work_logs/YYYY-MM-DD_fixer_team_applicant_profile.md`

금지:
- 새 프로필 API를 만들지 말고 기존 profileId 조회 API 재사용
- 전체 프로필 편집 화면 재사용 금지
- 이메일 등 비공개 정보 노출 금지
- SQL schema/seed 수정 금지
- 관련 없는 사용자 변경 정리, commit/push 금지

검증:
- 지원자 프로필이 있는 경우 실제 공개 정보 표시
- 프로필 없음/비공개 상태 확인
- 다른 지원자를 연속 선택해 이전 응답이 남지 않는지 확인
- 프로필 확인 후 수락·거절 UI 정상
- 모달 닫기 후 팀 상세 유지
- backend 관련 테스트와 `frontend npm run build`
- desktop/390x844 overflow, console error 0건
- SQL 변경 0개 확인

로그에는 추가 응답 필드, 공개 정보 범위, 권한 검증, build/test/브라우저 결과와 미검증 mutation을 기록하세요.
```
