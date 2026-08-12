# 팀 찾기 프런트 통합 수정

## 작업 목적

- `/matching/teams`를 실제 `member-to-teams` 응답만 사용하는 화면으로 정리한다.
- 팀원 찾기와 필터·카드·빈 결과·상세 이동 UX를 통일한다.
- 팀 추천을 `teamId + slotId`로 식별하고 실제 모집 슬롯 정보로 상세를 구성한다.

## 읽은 문서와 코드

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/work_logs/2026-06-20_frontend_matching_algorithm_filters.md`
- `docu/work_logs/2026-06-20_fixer_matching_member_filters.md`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`
- `frontend/src/router/index.js`
- `frontend/src/services/api.js`
- 응답 계약 확인용 `MatchingController`, `MatchingService`, `MatchingMapper.xml`

## 확인한 백엔드 응답 필드

- 팀: `teamId`, `teamName`, `teamDescription`, `teamStatus`, `regionId`, `regionAnyYn`, `publicRegionName`, `expectedDuration`, `teamGenres`
- 모집 공고: `recruitmentId`, `recruitmentTitle`, `deadlineAt`, `workStartAt`
- 모집 슬롯: `slotId`, `roleId`, `roleName`, `requiredCount`, `acceptedCount`, `remainingCount`, `requiredExperienceLevel`, `collaborationCondition`, `roleDuration`, `slotStatus`
- 추천: `score`, `scoreBadge`, `reasons`, `actions`

## 변경 파일

- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-20_fixer_matching_team_search.md`

## 제거한 샘플·하드코딩 UI

- `sampleTeamCards`와 샘플 팀 이미지 fallback을 제거했다.
- 결과 0건에서 샘플 팀을 표시하거나 저장·지원 성공처럼 처리하던 분기를 제거했다.
- 팀 상세에서는 프로필 강점, 포트폴리오 카드, `/profile` 링크를 표시하지 않는다.
- 실제 팀 이미지가 없으면 팀명 이니셜 placeholder만 표시한다.

## 팀원 찾기와 통일한 UI·UX

- 현재 프로필을 추천 기준 카드로 명시했다.
- 장르·지역·요구 경력·협업 조건과 적용·초기화·AI 버튼 배치를 팀원 찾기 패턴과 맞췄다.
- 팀 찾기 요구 경력을 `experienceLevel` query/API 값으로 연결했다.
- 로딩 시 이전 팀 결과를 먼저 비우고 오류와 0건 빈 상태를 구분했다.
- 0건 블록은 제목, 안내, `필터 초기화` 단일 CTA만 표시한다.
- 팀 찾기 헤더를 현재 프로필에 맞는 모집 팀 추천 문구로 분기했다.

## 팀 카드와 상세 실제 데이터 매핑

- 카드: 팀명, 팀 설명, 공고명, 지역/지역 무관, 팀 장르, 모집 역할, 잔여 인원, 요구 경력, 협업 조건, 점수, 추천 이유
- 상세: 위 정보와 역할 기간, 작업 시작일, 모집 마감일을 추가 표시한다.
- `EXPERIENCE_LEVEL`, `COLLABORATION_CONDITION`, `DURATION` 기준 코드 표시명을 사용한다.
- 누락 값은 `정보 없음`으로 표시하며 임의 데이터를 만들지 않는다.

## `teamId + slotId` 식별과 query 보존

- Vue key를 `team-{teamId}-slot-{slotId}`로 구성했다.
- 팀 상세 후보 선택은 route `teamId`와 query `slotId`를 함께 비교한다.
- 카드 상세 이동 시 현재 필터에 해당 카드의 `slotId`를 추가한다.
- slotId 없는 직접 상세 접근은 일치하는 첫 실제 슬롯을 선택한 뒤 query를 정규화한다.
- 상세 새로고침과 목록 복귀에서 필터와 `slotId`를 유지한다.
- 잘못된 slotId는 다른 슬롯으로 바꾸지 않고 명확한 `후보를 찾을 수 없습니다.` 상태를 표시한다.

## 저장·지원 payload 검증

- 저장은 `targetType='TEAM'`, `targetId=item.teamId`를 사용한다.
- 지원은 화면 카드의 `item.teamId`, `item.recruitmentId`, `item.slotId`를 그대로 사용하며 fallback ID가 없다.
- 데이터 변경 방지를 위해 실제 저장·지원 mutation은 실행하지 않았다.

## 검증

- `npm run build`: PASS (`vite v8.0.13`, 89 modules)
- `git diff --check`: PASS
- 브라우저, `camera` 계정:
  - 실제 팀 추천 4건과 실제 팀/공고/슬롯 필드 표시 확인
  - 샘플 팀 3개 미노출 확인
  - 팀 전용 헤더와 현재 프로필 기준 카드 확인
  - 장르·지역·요구 경력·협업 조건 select와 URL query 동기화 확인
  - `regionIds=2&experienceLevel=Y0_3&collaborationCondition=NEGOTIABLE`에서 실제 2건 확인
  - 0건에서 제목·안내·초기화 버튼 3개만 표시되고 샘플 카드 0개 확인
  - 초기화 후 query 제거 및 실제 팀 4건 복원 확인
  - `/matching/teams/4?slotId=9` 상세와 새로고침에서 촬영감독 슬롯 유지 확인
  - 필터 포함 상세 이동·새로고침·목록 복귀에서 필터와 slotId 보존 확인
  - 잘못된 `slotId=999` 직접 접근에서 명확한 빈 상세 확인
  - 팀 상세에서 포트폴리오와 상세 내부 `/profile` 링크 0개 확인
  - AI 팀 추천 3건과 추천 팀 상세 이동 확인
  - `/matching/members` 기준 팀·역할 미선택 상태 회귀 확인
  - desktop 1280, tablet 768, mobile 390x844 가로 overflow 0
  - tablet/mobile route 상세가 숨지 않고 각각 740px/362px 폭으로 표시됨 확인
  - console error/warning 0건

## 미검증 항목과 남은 위험

- 현재 프로필에 매칭되는 동일 팀 복수 슬롯이 없어 두 슬롯을 실제 브라우저에서 각각 열지는 못했다. key, 상세 비교, query 보존은 `teamId + slotId` 코드 경로로 검증했다.
- API 오류는 의도적으로 서버를 중단하지 않아 실제 브라우저에서 만들지 않았고, `error`가 있을 때 팀 0건 블록을 숨기는 분기를 확인했다.
- 현재 백엔드는 `matchesMemberToTeamFilters()`를 `enrichSlotTeam()`보다 먼저 실행해 원본 slot에 `teamGenres`가 없는 상태로 장르를 검사한다. 따라서 장르 query를 적용하면 현재 데이터에서 0건이 된다. 프런트 query 직렬화는 정상이며 백엔드 수정 금지 범위라 변경하지 않았다.
- 기존 Vite 500kB 초과 chunk 경고는 이번 작업과 무관하다.

## 변경 범위 확인

- 이번 작업에서 backend 변경 0개
- 이번 작업에서 SQL 변경 0개
- 작업 시작 전 존재하던 backend/SQL 및 다른 사용자 변경은 되돌리거나 정리하지 않았다.
