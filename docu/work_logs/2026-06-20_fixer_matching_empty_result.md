# 팀원 매칭 빈 결과 블록 단순화

## 작업 목적

- `/matching/members` 후보 0건 상태에서 상단 모집 역할 선택과 중복되는 빈 결과 내부 역할 선택 UI를 제거한다.
- 빈 결과 블록에는 제목, 간단한 안내, `필터 초기화` 버튼만 유지한다.

## 참조 파일

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-20_fixer_matching_member_filters.md`

## 변경 파일

- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`
- `docu/work_logs/2026-06-20_fixer_matching_empty_result.md`

## 제거 및 변경 내용

- 빈 결과 블록의 `다른 모집 역할 선택` 접근성 문구와 label을 제거했다.
- 빈 결과 블록 내부의 모집 역할 select를 제거했다.
- 안내를 `필터를 초기화하거나 후보 프로필의 공개 범위를 확인해주세요.`로 단순화했다.
- `필터 초기화` 버튼은 기존 `resetFilters` 호출을 그대로 유지했다.
- 제거된 wrapper와 select에만 필요했던 `.matching-results-empty > div`, `.matching-results-empty select` 스타일을 정리했다.
- 상단 기준 팀·모집 역할 UI와 팀/역할/query/요청 로직은 변경하지 않았다.

## 검증

- `npm run build`: PASS (`vite v8.0.13`, 92 modules)
- `git diff --check`: PASS
- 정적 검색:
  - `다른 모집 역할 선택`, `다른 모집 역할을 선택` 0건
  - 제거 대상 빈 결과 wrapper/select CSS 0건
- 브라우저, 팀장 계정, 공포 장르 조건으로 후보 0건 확인:
  - 빈 결과 자식 요소가 제목 `STRONG`, 안내 `P`, 초기화 `BUTTON` 3개뿐임을 확인
  - 빈 결과 내부 label 0개, select 0개
  - `다른 모집 역할 선택` 화면 문구 0개
  - 상단 모집 역할 select 1개 유지
  - 빈 결과의 `필터 초기화` 클릭 후 URL query, 팀·역할, 장르, 결과가 기존 동작대로 초기화됨
  - desktop 1280px 가로 overflow 0
  - mobile 390x844 가로 overflow 0, 빈 결과 폭 362px
  - console error/warning 0건

## 남은 위험

- 후보 0건은 현재 데모 데이터의 공포 장르 조건으로 검증했다. 다른 필터 조합도 동일한 단일 템플릿 분기를 사용한다.
- 기존 Vite 500kB 초과 chunk 경고는 남아 있으며 이번 변경과 무관하다.

## 변경 범위 확인

- 이번 작업에서 backend 변경 0개
- 이번 작업에서 SQL 변경 0개
- 작업 시작 전 존재하던 다른 사용자 변경은 되돌리거나 정리하지 않았다.
