# 팀 모집과 AI 로케이션 탐색 수정 작업 로그

작성일: 2026-06-25

## 문서 작성 기준

- `Agent.md`의 작업 로그 규칙에 따라 `docu/work_logs/YYYY-MM-DD_{role}_{summary}.md` 형식으로 작성한다.
- 이번 대화에서 실제 코드, DB 적용, 서버 실행, 검증으로 확인한 내용을 중심으로 기록한다.
- 구현된 것, 미수행 검증, 후속 확인이 필요한 것을 분리한다.
- 실제 `.env`, DB 비밀번호, OpenAI key, 접속 코드 평문 등 secret 값은 기록하지 않는다.
- 로컬 포트와 서버 실행 방식은 환경 정보로만 적고, 개인 경로와 비밀값은 남기지 않는다.

## 작업 범위

- 기준 버전: `dev_Slate_0625_user1_v4` 기준 로컬 `Slate`
- 주요 화면:
  - 팀 모집 페이지
  - 팀 지원/초대 현황 페이지
  - 개인/팀 AI 로케이션 탐색 페이지
  - Demo Access 1차 필터 적용 로컬 서버
- 주요 백엔드 영역:
  - AI 로케이션 추천 후보 수집, 점수 계산, AI prompt payload
  - 저장한 로케이션 후보 조회
  - 팀 컨텍스트 기반 로케이션 추천
  - 로케이션 CSV/DB import 기반 데이터 사용
- 제외 또는 제한:
  - SQL 원본 파일의 임의 직접 수정은 DB 최신화 작업에서 하지 않았다.
  - 실제 secret 값은 문서화하지 않았다.
  - 실제 OpenAI live 응답 품질 검증은 자동화 검증 범위에 포함하지 않았다.

## 반영 내용

### 로컬 실행과 환경 정리

- 프론트엔드 서버는 `5174`, 백엔드 서버는 `8080` 기준으로 실행 상태를 확인했다.
- 프론트엔드는 Demo Access 1차 필터가 적용되는 환경으로 실행했다.
- 백엔드는 `mvn spring-boot:run` 실행 중 Spring devtools 재시작과 MyBatis mapper loading 문제가 발생할 수 있어, 검증 단계에서는 패키징된 jar를 `spring.devtools.restart.enabled=false`로 실행하는 방식으로 안정화했다.
- `.env.example`에는 로컬 실행에 필요한 예시 항목을 기록했다. 실제 환경값과 접속 코드 평문은 기록하지 않았다.

### DB 최신화와 로케이션 데이터

- `dev_Slate_0625_user1_v4` SQL 기준으로 로컬 DB를 최신화했다.
- AI 로케이션 탐색에 필요한 촬영지 데이터 schema/import 흐름을 반영하고, 로컬 DB에 활성 촬영지 데이터가 존재하는 상태에서 기능을 검증했다.
- 폐업, 철거, 공터, 이전 등 현재성 확인이 필요한 촬영 이력은 추천 결과에서 주의 정보로 표시하도록 백엔드와 프론트를 연결했다.

### 팀 모집 페이지

- 상단 `모집 공고` 블럭을 제거하고, 돌아가기 버튼을 페이지 타이틀 줄 오른쪽으로 정렬했다.
- `공고 목록`은 `모집 공고 목록`, `구인 목록`은 `구인 공고 목록`으로 용어를 정리했다.
- 모집 공고 목록과 구인 공고 목록을 동일한 폭의 분리 블럭으로 구성했다.
- 모집 공고 수정/새 모집 공고 블럭과 구인 공고 수정/새 구인 공고 블럭의 폭과 높이를 조정했다.
- 목록 블럭과 수정 블럭은 고정 높이 안에서 휠 스크롤로 하위 항목을 확인할 수 있도록 정리했다.
- 모집 공고의 마감일이 지난 경우 자동으로 마감 상태로 표시되도록 보정했다.
- 새 모집 공고/새 구인 공고 모드에서는 중복 생성 버튼이 출력되지 않도록 수정했다.
- 모집 공고 수정 블럭에서 `상태`를 마지막 입력 항목으로 배치했다.
- `공고 저장`, `구인 공고 저장` 버튼 텍스트는 `저장`으로 통일했다.
- 새 모집 공고 생성 버튼은 `모집 공고 생성`으로 수정하고, 매칭 페이지 AI 추천 버튼 색상 계열과 맞췄다.
- 입력값이 없는 상태에서 생성 버튼을 누르면 안내 메시지가 페이지 상단이 아니라 새 모집 공고 블럭 하단에 표시되도록 수정했다.
- 새 모집 공고의 시작일과 마감일은 현재 일자 이전으로 설정할 수 없게 했고, 시작일 기본값은 현재 일자 및 시간으로 채우도록 했다.
- 개발 단계 안내 메시지 성격의 저장 완료 문구 노출을 제거했다.

### 팀 지원/초대 현황 페이지

- 팀 모집 페이지 재구성 방향과 맞춰 지원/초대 현황 페이지도 목록과 상세/처리 영역을 분리하는 구조로 재정리했다.
- 팀 단위 업무 페이지의 제목, 목록, 처리 패널, 스크롤 동작이 팀 모집 페이지와 유사한 사용 흐름을 갖도록 맞췄다.

### 공동 작업자 AI 로케이션 추천 기능 이식

- 공동 작업자 브랜치 `dev_Slate_0625_user2_AI_location_recommend`의 AI 로케이션 추천 기능을 현재 로컬 작업물 기준으로 이식했다.
- 로케이션 추천 API, 추천 세션, 추천 후보 저장, 저장 후보 조회, 프론트 추천 카드/지도/저장 다이얼로그를 연결했다.
- 팀 화면에서 팀 AI 로케이션 탐색 버튼으로 이동할 수 있는 흐름을 유지하고, 팀 컨텍스트 추천 옵션을 추가했다.

### AI 로케이션 추천 품질 보정

- `비 오는 밤, 새벽 3시, 골목길` 같은 prompt에서 `오는`, `나오는` 같은 불필요 단어가 후보 검색을 오염시키던 문제를 stop word와 장면 태그 기반 분석으로 보정했다.
- `골목/좁은 길`, `시장/상가`, `학교`, `사찰/절`, `호텔/숙박`, `병원`, `공원/숲`, `바다/강변`, `항구/부두`, `공장/창고` 등 필수 장면 태그를 도입했다.
- `비/우천`, `야간/밤`, `추격/긴장` 등 선택 장면 태그를 도입했다.
- 필수 장면 태그가 prompt에 포함된 경우 해당 태그가 없는 후보는 AI 추천 후보군에서 제외하도록 했다.
- 대표 촬영 이력은 폐업, 철거, 공터 등 위험 표현이 있는 이력이 뒤로 밀리도록 정렬했다.
- AI prompt payload에 후보의 `matchedRequiredSceneTags`, `matchedOptionalSceneTags`, `missingRequiredSceneTags`, `dataWarnings`를 포함했다.
- AI 응답 실패 또는 key 미설정 시 score 기반 fallback 추천을 유지하되, fallback 문구와 상세 표시를 사용자 흐름에 맞게 조정했다.

### 전체 지역 검색 후보 수집 보정

- 전체 지역 검색에서 전역 SQL 상위 30건만 AI 분석 후보로 들어가던 문제를 보정했다.
- 지역을 지정하지 않은 경우 각 시도별로 최대 30건씩 후보를 수집해 AI 분석 후보에 포함하도록 변경했다.
- 지역을 명시한 검색은 해당 지역 최대 30건 기준을 유지한다.
- 지역 필터 보너스 점수를 제거해 같은 후보가 전체 검색과 지역 지정 검색에서 서로 다른 base score를 갖지 않도록 했다.
- 최종 recommendation 개수는 사용자 설정 3개 또는 5개를 유지하되, AI가 검토하는 후보 풀은 전국 검색에서 지역별 후보를 더 넓게 반영하도록 했다.

### 팀 컨텍스트 기반 AI 로케이션 탐색

- 팀 탐색 모드에서 `팀 정보 반영` 옵션을 추가했다.
- 팀 탐색에서 별도 지역을 선택하지 않았고 팀 지역이 `지역 무관`이 아닌 경우 팀 공개 지역을 기본 검색 지역으로 사용하도록 했다.
- 팀명, 팀 설명, 팀 장르, 진행 중인 팀 계획 일부를 AI 추천 prompt에 포함하도록 했다.
- 추천 세션의 parsed condition에 팀 컨텍스트 사용 여부, 팀 컨텍스트 요약, effective prompt를 보존했다.

### AI 로케이션 탐색 프론트 UI

- 추천 결과 fallback 안내 문구를 검색 폼 아래, 추천 결과 블럭 위로 이동했다.
- 문구는 `추천 결과를 눌러 촬영 위치를 확인해보세요.`로 변경했다.
- 최초 AI 추천 응답 직후에는 추천 카드가 자동 선택되지 않도록 수정했다.
- 최초 응답 직후 지도에는 전체 후보가 표시되고, 사용자가 추천 카드를 누르면 해당 위치로 확대 이동하도록 했다.
- 개인 로케이션 탐색 페이지의 저장한 후보에는 개인 후보뿐 아니라 사용자가 팀 탐색에서 저장한 후보도 함께 표시되도록 했다.
- 저장 후보 카드에는 팀 후보지인 경우 팀명을 표시하도록 했다.
- 개인 로케이션 탐색 헤더 우측에 소속 팀 선택 영역을 추가해 팀 로케이션 탐색 페이지로 이동할 수 있게 했다.
- 지역 입력은 매칭 페이지의 지역 combobox 방식을 차용해 검색형 입력, 드롭다운, 선택 chip, chip 제거 흐름으로 변경했다.
- `추천 상세 보기` 펼치기 텍스트는 파란색으로 표시되도록 스타일을 조정했다.

## 검증 결과

### 백엔드 테스트

- AI 로케이션 기능 이식 및 품질 보정 후 targeted 테스트를 실행했다.
  - 명령: `mvn "-Dtest=AiLocationRecommendationServiceTest,LocationServiceTest,LocationMapperXmlParseTest,LocationRecommendationPersistenceTest,LocationCsvImportMapperXmlParseTest,LocationCsvImportServiceTest,LocationCsvImportWriterTest" test`
  - 결과: 성공
- 최근 지역별 후보 수집과 저장 후보 보정 후 targeted 테스트를 실행했다.
  - 명령: `mvn "-Dtest=LocationServiceTest,LocationMapperXmlParseTest" test`
  - 결과: 성공
  - 최종 확인 시점 결과: 21 tests, failures 0, errors 0, skipped 0

### 프론트엔드 빌드

- 명령: `npm run build` in `frontend`
- 결과: 성공
- 참고:
  - Vite chunk size warning은 기존 번들 크기 경고이며 빌드는 통과했다.
  - 일부 실행에서 Vite plugin timing 안내가 출력됐으나 빌드 실패는 아니었다.

### 로컬 서버 상태

- 프론트엔드:
  - URL: `http://localhost:5174`
  - `/locations` 응답 200 확인
- 백엔드:
  - URL: `http://localhost:8080`
  - 패키징 jar로 재시작 완료
  - 직접 API 호출은 Demo Access 또는 인증 조건에 따라 403이 날 수 있으며, 이는 접근 gate가 적용된 상태로 해석했다.
- 마지막 확인 시 서버는 다음 포트에서 listen 중이었다.
  - frontend `5174`
  - backend `8080`

## 이번 작업에서 의도적으로 변경하지 않은 내용

- 실제 `.env`, DB 비밀번호, OpenAI key, Demo Access 접속 코드 평문은 문서에 기록하지 않았다.
- DB 최신화 작업에서 SQL 파일 자체를 임의 수정하지 않았다.
- OpenAI 모델 선정 정책, 외부 API 과금/쿼터 정책은 변경하지 않았다.
- 팀 모집/지원/초대 기능의 백엔드 권한 정책 자체는 이번 UI 수정 범위에서 변경하지 않았다.
- 추천 결과의 최종 개수 정책은 3개 또는 5개로 유지했다.

## 남은 확인 사항

- 실제 로그인 계정으로 팀 모집, 지원/초대 현황, 개인/팀 로케이션 탐색의 브라우저 시각 회귀 확인이 필요하다.
- 실제 OpenAI key가 있는 환경에서 prompt별 추천 품질과 fallback 미사용 응답 품질을 추가 확인해야 한다.
- 전국 검색 후보군이 지역별 최대 30건으로 넓어져 AI 요청 payload가 커질 수 있으므로, 실제 API latency와 token 사용량을 점검해야 한다.
- 저장 후보에 개인 후보와 팀 후보가 함께 보이는 정책이 사용자 경험상 충분한지, 필요하면 필터 chip 또는 구분 탭을 추가할 수 있다.
- 백엔드 jar 실행은 현재 로컬 안정화를 위해 devtools restart를 끈 방식으로 확인했다. 개발 편의성을 위해 devtools 재시작 문제는 별도 원인 분석 여지가 있다.

## 변경 파일

- `backend/src/main/java/com/slate/locations/AiLocationRecommendationService.java`
- `backend/src/main/java/com/slate/locations/LocationController.java`
- `backend/src/main/java/com/slate/locations/LocationMapper.java`
- `backend/src/main/java/com/slate/locations/LocationService.java`
- `backend/src/main/resources/mappers/LocationMapper.xml`
- `backend/src/test/java/com/slate/locations/LocationServiceTest.java`
- `frontend/src/views/LocationExploreView.vue`
- `frontend/src/components/locations/LocationMap.vue`
- `frontend/src/components/locations/LocationRecommendationCard.vue`
- `frontend/src/components/locations/LocationSavedCandidateCard.vue`
- `frontend/src/router/index.js`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/styles/slate.css`
- `.env.example`
- 이 작업 로그: `docu/work_logs/2026-06-25_fixer_team_recruitment_location_ai.md`

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/00_common/document_structure.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/13_work_status/current_and_completed_work.md`
- `frontend/src/views/LocationExploreView.vue`
- `frontend/src/components/locations/LocationMap.vue`
- `frontend/src/components/locations/LocationRecommendationCard.vue`
- `frontend/src/components/locations/LocationSavedCandidateCard.vue`
- `frontend/src/styles/slate.css`
- `backend/src/main/java/com/slate/locations`
- `backend/src/main/resources/mappers/LocationMapper.xml`
- `backend/src/test/java/com/slate/locations/LocationServiceTest.java`
