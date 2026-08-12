# 팀 상세 하드코딩 제거 수정 로그

## 작업 목적

- `/teams/:teamId` 팀 상세 대시보드의 샘플 팀, 임의 숫자, 고정 날짜와 타임라인을 제거한다.
- 기존 팀/모집/지원/초대/멤버/계획 API 응답만으로 요약 화면을 구성한다.

## 읽은 파일

- `docu/prompt/team_detail_hardcoding_fixer_prompt.md`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/services/api.js`
- `frontend/src/styles/slate.css`
- `backend/src/main/resources/mappers/TeamMapper.xml` 읽기 전용
- `backend/src/main/java/com/slate/teams/TeamService.java` 읽기 전용

## 변경 파일

- `frontend/src/views/TeamsView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-20_fixer_team_detail_hardcoding.md`

Backend와 SQL은 이번 작업에서 수정하지 않았다.

## 제거한 하드코딩

- 팀 0건일 때 표시하던 샘플 팀 3개와 샘플 이미지 fallback
- 팀명, 설명, 장르, 지역, 상태, 리더명과 리더 역할의 가짜 fallback
- 멤버 수 `4`, 최대 인원 `6`, 최근 합류 `24.05.26`
- 지원 대기 `5`, 초대 응답 `2`, 진행 중 모집 `2`, 마감 임박 `1`
- 계획이 없을 때 진행률 `68%`
- `D-12`, `와이드 리허설`, 고정 6월 14일
- 최근 업데이트 3개 고정 문구와 날짜
- 5~7월 고정 타임라인과 일정명·기간

## 실제 필드 매핑

- 지원 대기: `applications` 중 `status === 'PENDING'`
- 초대 대기: 현재 팀 `teamInvitations` 중 `status === 'PENDING'`
- 진행 중 모집: `recruitments` 중 `status === 'OPEN'`
- 마감 임박: OPEN 모집 중 현재부터 7일 이내 `deadlineAt`
- 멤버 수: `selectedTeam.members`의 ACTIVE 멤버 수
- 최근 합류: ACTIVE 멤버 `joinedAt` 최댓값
- 진행률: CANCELED 계획을 제외한 계획 중 DONE 비율, 계획이 없으면 `0%`
- 다음 마일스톤: 미완료 계획 중 가장 가까운 `dueAt`
- 최근 업데이트: `updatedAt` 또는 `createdAt` 최신순 최대 3건
- 일정 안내: `dueAt`이 있는 실제 계획 목록
- 팀 기본 정보: `selectedTeam`의 이름, 설명, 장르, 지역, 상태, 최대 인원, 리더 정보

날짜는 `Intl.DateTimeFormat('ko-KR')`로 표시한다. 누락 데이터는 샘플 대신 `정보 없음`, `등록된 팀 설명이 없습니다.`, `등록된 다음 일정이 없습니다.`, `최근 업데이트가 없습니다.`, `등록된 계획 일정이 없습니다.`로 표시한다.

## 검증 결과

- `cd frontend && npm run build`: PASS
- 팀 목록: 실제 팀 2개만 표시, 샘플 팀 미노출
- 데이터 없는 팀 `/teams/8`: 실제 팀 정보, 진행률 `0%`, 다음 일정/업데이트/계획 일정 빈 상태 확인
- 관리 팀 `/teams/1`: 실제 지원 대기 0건, 초대 대기 1건, 진행 중 모집 1건, 마감 임박 0건, ACTIVE 멤버 2명, 최근 합류 2026년 6월 8일 확인
- `/teams/1` 계획: 실제 계획 2건, 다음 마일스톤과 최근 업데이트/일정 표시 확인
- 직접 URL과 새로고침 정상
- 기본 viewport에서 렌더링 확인
- `390x844`: 최초 문서 폭 730px 원인을 수정한 뒤 `scrollWidth === 390`, 가로 overflow 0 확인
- 브라우저 console error/warning 0건
- backend/SQL 이번 작업 변경 0개

## 남은 위험

- 팀 이미지 URL은 현재 팀 응답에 없어 이니셜 placeholder로 표시된다.
- 멤버 응답에는 프로필 직무명이 없어 리더 역할 괄호는 값이 있을 때만 표시한다.
- 테스트 데이터에는 완료된 계획이 없어 0% 이외의 실제 진행률은 계산식과 build로 확인했다.

## 일정 목록 UI 후속 수정

- `dueAt`만 있는 계획을 기간형 타임라인으로 표시하던 UI를 일반 일정 목록으로 교체했다.
- 각 행은 실제 `title`, `dueAt`, 공통 코드 기반 상태 표시명만 사용한다.
- 마감일 오름차순으로 정렬하고 마감일 없는 계획은 뒤에 배치한다.
- 마감일이 없으면 `마감일 없음`, 계획이 없으면 `등록된 계획 일정이 없습니다.`를 표시한다.
- 배경 격자, 전체 폭 막대, 고정 타임라인 CSS를 제거했다.
- 하단 Grid의 늘림을 해제해 일정 카드 높이가 실제 콘텐츠 높이로 줄어들도록 수정했다.
- 실제 팀에서 2026년 6월 25일, 7월 2일 일정 순서를 확인했고 데스크톱 일정 카드 높이는 426px에서 230px로 감소했다.
- `390x844`에서 실제 일정 목록, 문서 폭 390px, console error/warning 0건을 확인했다.
- 현재 로그인 계정의 두 팀 모두 계획이 있어 일정 0건과 마감일 없는 계획은 실제 데이터로 재현하지 않고 조건 렌더링과 정렬 코드로 확인했다.

## dueAt 지점형 타임라인 후속 수정

- 일반 일정 목록을 전체 마감일의 최소·최대 범위를 사용하는 지점형 타임라인으로 변경했다.
- 각 계획의 `dueAt`을 날짜 단위 timestamp로 정규화하고 `(현재 날짜 - 최소 날짜) / (최대 날짜 - 최소 날짜)` 비율로 핀 위치를 계산한다.
- 같은 날짜 계획은 timestamp 기준 `Map`으로 묶어 하나의 핀 카드 안에 제목과 상태를 함께 표시한다.
- 마감일 없는 계획은 타임라인 아래 `마감일 미정` 목록으로 분리한다.
- 모바일에서는 가로축과 절대 위치를 해제하고 2px 세로선과 순차 카드 구조로 전환한다.
- 실제 `/teams/1`에서 2026년 6월 25일 핀 `0%`, 7월 2일 핀 `100%`와 서로 다른 x 좌표를 확인했다.
- `390x844`에서 두 마커가 동일 x 좌표와 서로 다른 y 좌표로 배치되고 문서 폭 390px, console error/warning 0건을 확인했다.
- 현재 데이터에는 같은 날짜 또는 마감일 없는 계획이 없어 해당 상태는 그룹화·분리 계산과 조건 렌더링으로 확인했다.
