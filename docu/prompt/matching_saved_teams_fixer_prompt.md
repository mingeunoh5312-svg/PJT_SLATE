# 저장한 팀 목록·취소 기능 구현 프롬프트

```text
Slate 팀 매칭에 저장한 팀 목록과 저장 취소 기능을 구현하세요. 기존 `matching_bookmark`를 재사용하고 구현·test·build·브라우저 검증·로그까지 완료하세요.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

먼저 확인:
- `frontend/src/views/MatchingView.vue`
- `frontend/src/services/api.js`
- `backend/.../matching/MatchingController.java`
- `MatchingService.java`, `MatchingMapper.java`, `MatchingMapper.xml`
- `matching_bookmark` schema는 읽기만 확인

목표 UX:
- `/matching/teams` 목록 상단에 `추천 팀` / `저장한 팀` 하위 탭 추가
- 저장 탭 URL: `/matching/teams?view=saved`
- 기존 AI view, 필터, 팀 상세, 지원 기능 보존

Backend 최소 구현:
1. 현재 로그인 사용자의 TEAM 북마크 조회 API 추가
   - 예: `GET /api/matching/bookmarks?targetType=TEAM`
   - bookmarkId, savedAt, teamId, 팀명·설명·상태·지역·장르와 현재 OPEN 모집 역할 반환
   - 다른 사용자의 북마크 조회 금지
2. 저장 취소 API 추가
   - 예: `DELETE /api/matching/bookmarks/TEAM/{teamId}`
   - 현재 사용자 소유 행만 삭제
3. 기존 POST 중복 저장은 `INSERT IGNORE` 결과를 구분해 이미 저장된 경우 정확한 응답/문구 제공
4. schema/seed 변경 금지

Frontend:
- 저장 탭 진입 시에만 저장 목록 조회
- loading/error/0건 상태 구분
- 0건 문구: `저장한 팀이 없습니다.`
- 저장 카드에 저장 시각, 실제 팀 상태와 OPEN 모집 역할 표시
- `상세 보기`, `지원`, `저장 취소` 제공
- 저장 취소 후 목록에서 즉시 제거하고 추천 목록의 저장 상태도 갱신
- 추천 카드 저장 버튼은 이미 저장됨/저장 완료 상태를 구분
- 모집 종료·삭제·OPEN 역할 없음 상태에서는 지원 비활성 및 이유 표시
- 저장은 TEAM 단위이므로 OPEN 역할이 여러 개면 사용자가 역할을 선택한 뒤 정확한 teamId/recruitmentId/slotId로 지원
- 샘플 팀이나 임의 fallback 데이터 사용 금지

수정 예상 범위:
- matching backend controller/service/mapper와 관련 테스트
- `frontend/src/views/MatchingView.vue`
- `frontend/src/services/api.js`
- 필요한 경우 `frontend/src/styles/slate.css`
- `docu/work_logs/YYYY-MM-DD_fixer_matching_saved_teams.md`

금지:
- 새 DB 테이블 또는 전역 상태 관리 추가
- `/teams` 내 팀 목록과 저장 팀 혼합
- 다른 사용자 북마크 노출
- 첫 모집 슬롯을 임의 지원 payload로 사용
- 관련 없는 변경 정리, commit/push

검증:
- backend 관련 테스트와 `frontend npm run build`
- 추천 팀 저장 → 저장 탭 노출 → 새로고침 유지 → 저장 취소
- 중복 저장 문구와 DB 중복 0건 확인
- 저장 팀 상세 및 정확한 역할 지원 payload 확인
- OPEN 역할 없음·종료 팀·저장 0건 확인
- 일반 추천/AI/팀원 매칭 회귀 확인
- desktop/390x844 overflow, console error 0건
- SQL 변경 0개 확인

로그에는 API 계약, 권한 조건, TEAM 단위 저장과 슬롯 선택 방식, 변경 파일, test/build/브라우저 결과와 미검증 mutation을 기록하세요.
```
