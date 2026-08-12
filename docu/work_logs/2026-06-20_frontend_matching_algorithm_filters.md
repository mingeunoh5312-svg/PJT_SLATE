# 팀원 매칭 알고리즘 필터 프런트 보강

## 목적

- 팀원 찾기 입력 조건을 기존 `teamToMembers` API 계약과 정확히 연결한다.
- 기준 팀/모집 역할과 추가 필터를 시각적·동작적으로 분리한다.
- 실제 추천 점수와 근거, 후보 조건을 임의 fallback 없이 표시한다.

## 변경 파일

- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`

기존 `frontend/src/services/api.js`의 빈 값 제외 및 배열 query 직렬화를 재사용했다. Router와 backend 계약은 변경하지 않았다.

## 필터 매핑

| UI | 기준 API | matching query |
|---|---|---|
| 장르 | `slateApi.genres()` | `genreIds` |
| 지역 | `slateApi.regions(keyword, 100)` | `regionIds` |
| 경력 | `EXPERIENCE_LEVEL` | `experienceLevel` |
| 합류 가능 시점 | `JOIN_AVAILABILITY` | `joinAvailability` |
| 협업 조건 | `COLLABORATION_CONDITION` | `collaborationCondition` |

- 지역은 backend 계약에 맞춰 공개 프로필의 `regionId` 정확 일치로 안내·전달한다.
- 경력은 최소 경력이 아니라 선택 단계 정확 일치다.
- 빈 값은 query에서 제외한다.

## 구현 내용

- 기준 정보와 추가 필터 제목/설명을 분리했다.
- 관리 팀 고정 카드에 팀 상태와 공개 지역을 추가했다.
- 팀, 모집 역할, 장르, 지역, 공통 코드의 loading/error 상태를 분리했다.
- 경력과 합류 가능 시점 필터를 추가했다.
- 필터 적용/초기화/AI 추천 버튼을 한 실행 영역으로 구성했다.
- 잘못된 teamId, slotId, genreIds, regionIds, 공통 코드는 기준 데이터 로딩 후 안전한 값으로 정리하고 URL을 정규화한다.
- URL query 변경 시 중복 일반 매칭 요청이 발생하지 않도록 내부 route 동기화 요청을 구분했다.
- 느린 모집/일반/AI 요청은 request id로 이전 응답을 무시하고 unmount 후 상태 변경을 차단한다.
- 후보 결과는 점수 내림차순으로 표시하고 backend `reasons`를 최대 4개 사용한다.
- 후보 카드에 실제 경력, 합류 가능 시점, 협업 조건, scoreBadge를 표시한다.
- 누락 지역/장르/점수/이미지는 각각 정보 없음 문구 또는 이니셜 placeholder로 표시한다.
- pagination 계약이 없는 결과 새로고침/더 보기 버튼을 제거했다.
- 상세 화면의 지역·장르·점수 fallback도 제거하고 현재 query를 유지한다.
- AI payload에는 `type`, `teamId`, `slotId`만 사용하며 일반 필터는 전달하지 않는다.

## 검증

- `npm run build` PASS
- 잘못된 query 직접 접근 후 `teamId=1&slotId=1`로 정규화 확인
- 복합 필터 query 확인:
  - `genreIds=1`
  - `regionIds=4`
  - `experienceLevel=Y0_3`
  - `joinAvailability=IMMEDIATE`
  - `collaborationCondition=UNPAID`
- 복합 조건 적용 후 후보가 이지은 1명으로 변경되는 것 확인
- 새로고침 후 filter/query/result 복원 확인
- 필터 초기화 후 추가 query 제거 및 기본 후보 2명 복원 확인
- 상세 `/matching/members/3?teamId=1&slotId=1` 이동과 후보 조건 표시 확인
- AI 추천은 `view=ai&teamId=1&slotId=1` 및 최대 3명 이내 결과 확인
- 관리 팀 0개 계정에서 팀 만들기 CTA와 AI 비활성 확인
- desktop 1280, tablet 768, mobile 390x844 가로 overflow 0 확인
- console error/warning 0 확인

## 남은 확인

- 현재 시드에는 한 계정이 관리하는 팀이 2개 이상인 경우와 관리 팀은 있지만 OPEN 슬롯이 0개인 경우가 없어 해당 분기는 코드/빌드 검증만 했다.
- 저장/초대는 버튼과 기존 payload 코드를 보존했으며 검증 데이터 생성을 피하기 위해 실제 mutation은 실행하지 않았다.
