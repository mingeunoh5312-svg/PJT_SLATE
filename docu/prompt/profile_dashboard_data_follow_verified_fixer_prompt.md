# 내 정보 화면 데이터·팔로우·Verified 수정 프롬프트

```text
Slate 내 정보(`/profile`) 화면의 아래 3개 문제를 직접 수정하세요. 설명만 하지 말고 구현과 검증까지 완료하세요.

작업 루트:
- `/Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate`

먼저 확인:
- `Agent.md`, `docu/README.md`, `docu/00_common/reference_policy.md`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/components/follows/FollowListDialog.vue`
- `frontend/src/services/api.js`, `frontend/src/styles/slate.css`
- 필요한 API 계약만 backend controller/service/mapper에서 읽기

## 1. 하드코딩 제거

`ProfileView.vue`의 내 정보 대시보드를 전수 검색해 샘플 팀·작품, 임의 역할/장르/협업 조건, 가짜 숫자·날짜·상태·설명·이미지 fallback을 제거하세요.

- 프로필, 참여 팀, 참여 작품, 포트폴리오는 기존 실제 API 응답만 사용
- `sampleTeams`, `sampleWorks`, `fallbackPortfolioCards` 등 실제 데이터처럼 보이는 샘플 제거
- `selectedRoles` 등이 비었을 때 표시하는 `촬영감독`, `드라마` 같은 임의값 제거
- 누락 값은 `-`, `정보 없음`, `등록된 항목이 없습니다` 등 명확한 빈 상태 표시
- 0과 빈 배열은 유효한 결과로 취급하고 `|| 임의값` 금지
- 외부/샘플 이미지를 대신 넣지 말고 기존 placeholder 또는 이니셜 사용
- 기존 편집·추가·관리·이동 기능은 유지

## 2. 팔로워·팔로잉 영역 복구

백엔드의 기존 follow API와 `FollowListDialog`를 재사용해 실제 화면에 보이도록 수정하세요.

- 현재 `profile?.profileId` 조건 때문에 영역이 사라지는 원인을 API 응답과 로딩 순서까지 확인
- 내 프로필이 존재하면 계정 요약 바로 아래에 `팔로워 N`, `팔로잉 N` 표시
- 클릭 시 각각 실제 목록 dialog 열기, 더 보기·팔로우/취소·수치 재조회 정상 동작
- 로딩/오류/빈 목록을 구분하고 오류 때문에 전체 영역이 사라지지 않게 처리
- `profileId` 없이 API를 호출하거나 URL에 `undefined`를 넣지 않기
- 임의 수치 금지. 프로필 미생성 상태는 기존 안내 흐름 유지

## 3. 포트폴리오 Verified 배지 복구

백엔드에서 제공하는 검증 결과를 기준으로 대시보드와 포트폴리오 목록/상세에 배지를 일관되게 표시하세요.

- 실제 응답 필드(`verified` 또는 현재 DTO의 검증 상태)를 확인해 한 가지 정상 계약으로 연결
- 검증 완료 항목에만 `Verified` 배지 표시, 미검증 항목에는 표시 금지
- 문자열/숫자 추측 fallback으로 잘못 표시하지 말고 backend DTO·mapper와 frontend 매핑 불일치 수정
- 기존 `.verified-badge` 스타일을 재사용하고 카드 제목을 가리지 않도록 반응형 확인
- 검증 API나 DB 로직이 이미 정상이라면 backend/SQL은 수정하지 않기

## 제한

- 관련 없는 화면 리팩터링, 사용자 변경 되돌리기, seed/가짜 데이터 추가, 새 라이브러리 설치, commit/push 금지
- 기존 API와 공통 formatter/component 우선 재사용

## 완료 검증

1. `cd frontend && npm run build`
2. `/profile` 새로고침 및 직접 URL 접근
3. 데이터가 있는 계정과 없는 계정에서 하드코딩 미노출
4. 팔로워·팔로잉 실제 수치 및 목록 dialog 동작 확인
5. 검증/미검증 포트폴리오의 배지 표시가 backend 값과 일치
6. desktop 및 390x844에서 overflow와 console error 없음
7. 변경 파일, 실제 API 매핑, 제거한 하드코딩, 검증 결과를 `docu/work_logs/YYYY-MM-DD_fixer_profile_dashboard.md`에 기록
```
