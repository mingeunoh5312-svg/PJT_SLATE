# 팀 상세 하드코딩 제거 프롬프트

```text
Slate `/teams/:teamId` 상세 화면의 하드코딩 데이터를 프런트에서 제거하세요. 설명에 그치지 말고 구현, build, 브라우저 검증, 작업 로그까지 완료하세요.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

우선 읽기:
- `frontend/src/views/TeamsView.vue`
- `frontend/src/services/api.js`의 teams API
- 응답 필드 확인이 필요할 때만 `backend/src/main/resources/mappers/TeamMapper.xml` 읽기 전용

수정 범위:
- `frontend/src/views/TeamsView.vue`
- 필요한 경우 `frontend/src/styles/slate.css`
- `docu/work_logs/YYYY-MM-DD_fixer_team_detail_hardcoding.md`

금지:
- backend, SQL, seed 수정
- 임의 숫자·날짜·팀·일정 추가
- 관련 없는 사용자 변경 정리
- commit/push

반드시 제거할 하드코딩:
- `applications.length || 5`, `myInvitations.length || 2`
- `openRecruitments.length || 2`, `마감 임박 1`
- 멤버 수 fallback `4`, 최대 인원 fallback `6`, `최근 합류 24.05.26`
- 계획이 없을 때 진행률 `68%`
- `D-12`, `와이드 리허설`, 고정 날짜와 최근 업데이트 3개
- 5~7월 고정 타임라인과 일정명·기간
- 팀명, 설명, 리더명/역할, 장르, 지역, 상태의 가짜 fallback

실제 데이터 매핑:
- 지원 대기: 해당 팀 `applications` 중 `status === 'PENDING'`
- 초대: 전체 사용자 `myInvitations`가 아니라 해당 팀 `teamInvitations`를 상태별로 집계하고 문구도 실제 의미와 맞춤
- 진행 중 모집: `recruitments` 중 실제 `OPEN`
- 마감 임박: OPEN 모집의 `deadlineAt`이 현재부터 7일 이내인 건수
- 멤버 수: `activeMembers.length`
- 최근 합류: ACTIVE 멤버의 `joinedAt` 최댓값
- 진행률: 실제 `plans` 상태로 계산. 계획이 없으면 `0%` 또는 명확한 빈 상태
- 다음 일정: 미완료 plan 중 가장 가까운 `dueAt`
- 최근 업데이트/일정: 실제 plans만 사용. API 필드로 만들 수 없는 타임라인은 실제 계획 목록 또는 빈 상태로 교체
- 팀 기본 정보: `selectedTeam` 응답만 사용

원칙:
- 0은 유효한 값이므로 `|| 임의값`을 사용하지 마세요.
- 누락 데이터는 `정보 없음`, `등록된 계획 없음`, `최근 합류 정보 없음`처럼 표시하세요.
- 날짜는 공통 formatter로 한국어 사용자 형식으로 표시하세요.
- loading/API 오류/빈 데이터 상태를 구분하세요.
- 기존 팀 수정, 모집 관리, 멤버 관리, 계획 관리, 종료/재개 권한과 이동 동작을 보존하세요.
- 샘플 팀이나 가짜 이미지 fallback이 실제 팀처럼 보이는 경로도 함께 제거하고 이니셜 placeholder 또는 빈 상태를 사용하세요.

검증:
1. `cd frontend && npm run build`
2. 데이터가 있는 팀과 없는 팀 상세 비교
3. 지원·초대·모집·멤버·최근 합류·계획 값이 실제 응답과 일치
4. 0건에서 임의 숫자/날짜 미노출
5. 새로고침과 직접 URL 정상
6. desktop/390x844 overflow 및 console error 0건
7. backend/SQL 이번 작업 변경 0개 확인

로그에는 변경 파일, 제거한 하드코딩, 필드 매핑, build/브라우저 결과, API에 없어 빈 상태로 처리한 항목을 기록하세요.
```
