# 팀 상세 모달·계획 페이지 이동 수정 프롬프트

```text
Slate 팀 상세의 하단 삽입 UI를 모달과 전용 페이지로 분리하세요. 기존 기능을 재사용해 좁게 구현하고 build·브라우저 검증·작업 로그까지 완료하세요.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

읽을 파일:
- `frontend/src/views/TeamsView.vue`
- `frontend/src/router/index.js`
- `frontend/src/styles/slate.css`의 team 관련 부분

수정 범위:
- 위 3개 프런트 파일
- `docu/work_logs/YYYY-MM-DD_fixer_team_detail_navigation.md`

구현:
1. `지원/초대 대기` 클릭
   - 상세 하단에 섹션을 추가하지 말고 현재 지원/초대 관리 내용을 모달로 표시
   - 기존 데이터, 수락/거절, 권한 조건을 그대로 재사용
   - 닫기 버튼, 배경 클릭, ESC, `role="dialog"`, `aria-modal="true"`, 포커스 가능한 닫기 버튼 제공
   - 모달 닫기 후 상세 화면 상태 유지

2. `계획 보기`, `계획 보기로 이동`, `전체 일정 보기` 클릭
   - 모두 새 route `/teams/:teamId/plans` (`teams-plans`)로 이동
   - `전체 일정 보기`는 `?view=schedule`을 붙여 일정 영역으로 바로 이동하거나 강조
   - 현재 상세 하단의 팀 계획 UI를 새 route에서 그대로 재사용
   - 계획 목록·등록·수정·상태 변경, 권한, 종료 팀 제한을 보존
   - 새 페이지에 팀 상세로 돌아가는 버튼 제공

정리:
- `activeTeamPanel === 'requests'/'plans'`로 상세 하단에 렌더링하던 구조 제거
- 같은 폼과 API 호출을 복제하지 말고 기존 상태·함수를 사용
- route 직접 접근/새로고침에서 teamId로 정상 로드
- 접근 권한 없음, 팀 없음, loading, API 오류 상태 유지

금지:
- backend, SQL, seed 수정
- 새 전역 상태 관리나 UI 라이브러리 추가
- 관련 없는 사용자 변경 정리
- commit/push

검증:
- `cd frontend && npm run build`
- 지원/초대 모달 열기·닫기·ESC·배경 닫기와 수락/거절 UI 확인
- 모달이 페이지 하단에 중복 표시되지 않는지 확인
- 세 계획/일정 버튼의 route 이동 확인
- `/teams/{id}/plans` 직접 접근·새로고침·상세 복귀 확인
- 계획 CRUD/상태 변경 회귀 확인
- desktop 및 390x844 overflow, console error 0건 확인
- backend/SQL 이번 작업 변경 0개 확인

로그에는 변경 파일, 모달 전환 방식, 추가 route, 기존 기능 재사용 방식, build/브라우저 결과와 미검증 mutation을 기록하세요.
```
