# 공모전 실제 데이터·이미지·적합도 UI 통합 수정 프롬프트

```text
Slate 공모전 페이지의 하드코딩 제거, 실제 DB/API 목록, 목록형 UI, 공모전 이미지, 기업 계정 개설 요청, 마감 임박 목록과 적합도 노출 정책을 한 번의 연속 작업으로 직접 수정하세요. 설명이나 frontend 임시 처리에서 멈추지 말고 필요한 DB migration, backend, frontend, 테스트, 실제 브라우저 검증과 작업 로그까지 완료하세요.

작업 루트:
- `/Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate`

원본 요구사항:
- `docu/user_temp/What_to_do_3.md`

UI 참고 이미지:
- `/Users/mingeunoh/Desktop/스크린샷 2026-06-22 오후 9.36.32.png`

참고 이미지는 정보가 정돈된 공모전 목록형 레이아웃과 시각적 밀도를 참고하기 위한 자료입니다. 이미지에 있는 대상·지역·주최·상금 등 대규모 필터 전체를 복제하지 마세요. 이번 요구사항은 기존 하단의 기준·상태·구분·정렬 바를 삭제하는 것입니다.

## 이번 작업에서 제외

- 공모전 사이트 크롤러 구현
- 외부 사이트 HTML 파싱
- 스케줄러를 통한 자동 수집
- 크롤링 이미지 다운로드 작업

크롤링은 추후 별도 작업으로 진행합니다. 다만 향후 크롤러가 `representativeImageUrl` 또는 합의한 이미지 출처 필드를 저장할 수 있는 기존 구조는 훼손하지 마세요.

## 먼저 확인

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- 최신 contest 관련 work log
- `frontend/src/views/ContestView.vue`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/styles/slate.css`
- `backend/src/main/java/com/slate/contests/**`
- `backend/src/main/resources/mappers/ContestMapper.xml`
- `backend/src/main/java/com/slate/media/**`
- `backend/src/main/resources/mappers/MediaImageMapper.xml`
- `sql/01_schema.sql`
- contest/media 관련 migration SQL과 테스트

시작 시 `git status --short`와 관련 파일 diff를 확인하세요. 현재 작업 트리에는 사용자와 다른 수정자의 미완료 변경이 있을 수 있습니다. 기존 공모전 저장, 제출 준비, 기업 개설 요청, 관리자 승인, 기업 공모전 관리, 문서 업로드, 감사 로그, 알림 기능을 되돌리거나 과거 prototype 코드로 덮어쓰지 마세요.

## 확인된 현재 상태

1. `sampleContests`, `sampleUrgentContests`와 여러 contest asset이 실제 공모전처럼 노출된다.
2. API 결과가 비면 샘플 공모전으로 대체된다.
3. 저장 공모전 수, 준비 수, 오늘 마감 수 등에 `6`, `2`, `1` 같은 가짜 fallback이 있다.
4. 목록이 `추천 공모전`과 가짜 적합도 중심으로 구성돼 있다.
5. `urgentContests`는 실제 마감일 전용 조회가 아니라 일반 목록 일부를 잘라 사용한다.
6. 목록 상단에 상태·유형·장르·정렬·검색 filter card가 있다.
7. 상단 route nav에 `공모전 목록`과 로그인 사용자용 `개설 요청`이 함께 표시된다.
8. backend는 회사 계정만 개설 요청할 수 있도록 이미 검증하지만 frontend는 일반 로그인 사용자에게도 개설 요청 nav를 노출할 수 있다.
9. 대표 이미지는 URL 문자열 입력만 있고 기업/기관 사용자의 실제 이미지 파일 업로드가 없다.
10. 적합도 계산 API와 cache는 존재하지만 목록 카드에 적합도가 미리 표시되고 샘플 적합도까지 생성한다.

## 최종 사용자 계약

### 1. 하드코딩 제거와 실제 데이터 연결

- `ContestView.vue`를 전수 검색해 아래 실제 데이터처럼 보이는 하드코딩을 제거한다.
  - `sampleContests`
  - `sampleUrgentContests`
  - 샘플 제목, 기관, 마감일, 상금, 태그, 적합도, badge, 설명
  - 샘플 contest image 순환 mapping
  - 저장 수, 준비 수, 오늘 마감 수의 가짜 숫자 fallback
  - 샘플 상세와 샘플 적합도 계산 분기
- API 응답이 비었을 때 샘플 데이터를 표시하지 않는다.
- 빈 배열은 정상적인 빈 결과로 처리하고 loading, error, empty 상태를 구분한다.
- 실제 API의 0, false, 빈 배열을 `|| 임의값`으로 덮지 않는다.
- 현재 화면에 필요한 공모전 목록, 저장 상태, 마감일, 주최기관, 상금, 장르/대상 정보는 DB/API 응답만 사용한다.
- 불필요해진 샘플 image import는 제거하되 다른 화면이 사용하는 asset 파일은 삭제하지 않는다.

### 2. 추천이 아닌 공모전 목록

- 메인 heading을 `공모전 목록`으로 구성하고 `추천 공모전`, `오늘의 추천 공모전`, `내 팀 기준 적합도 높은 공모전` 문구와 추천 hero를 제거한다.
- 목록은 공개 가능하고 현재 노출 정책에 맞는 실제 공모전 전체를 표시한다.
- 기본 정렬은 현재 진행 중인 공모전을 마감일 오름차순으로 보여주는 방향을 우선한다.
- 종료 공모전 처리, 페이지 크기, 더 보기 또는 pagination은 기존 API 계약을 확인해 실제 데이터 손실 없이 구현한다.
- frontend에서 `slice(0, 4)`만 하고 전체 목록이라고 표시하지 않는다.
- 공모전 카드/행 클릭 또는 상세 보기 action은 실제 `contestId`로 상세 route에 이동한다.

### 3. 참고 이미지 기반 목록형 UI

- 참고 이미지의 정돈된 목록 구조, 명확한 섹션 heading, 항목 간 divider, 정보 밀도만 Slate 디자인에 맞게 반영한다.
- 외부 사이트의 tab 문구, 색상, 필터 종류와 콘텐츠를 그대로 복제하지 않는다.
- 각 공모전 목록 항목에는 실제 데이터가 있는 범위에서 다음 정보를 우선 표시한다.
  - 대표 이미지
  - 제목
  - 주최기관
  - 요약
  - 대상 또는 관련 장르
  - 상금
  - 마감일과 D-day
  - 저장 action
  - 상세 이동
- 긴 제목과 요약은 line clamp로 정돈하되 상세 정보가 유실되지 않게 한다.
- desktop에서는 목록 정보가 한눈에 보이고 mobile에서는 이미지·본문·metadata가 자연스럽게 세로 배치되게 한다.

### 4. 하단 기준·상태·구분·정렬 바 삭제

- 공모전 목록 화면의 기존 filter card에서 다음 사용자 노출 control을 제거한다.
  - 기준
  - 상태
  - 구분/유형
  - 정렬
- 해당 control과 연결된 불필요한 route query, watch, frontend 상태를 정리한다.
- CSS로만 숨기고 접근 가능한 DOM이나 tab order에 남겨두지 않는다.
- 공모전 검색 유지 여부는 현재 UX를 확인하되, 검색을 유지한다면 목록 상단에 간결하게 배치하고 제거 대상 filter와 다시 묶지 않는다.
- backend가 관리자 또는 다른 화면에서 사용하는 filter/sort API는 삭제하지 않는다.

### 5. 공모전 대표 이미지

#### 공통 표시 규칙

- 최종 이미지 우선순위는 다음과 같다.
  1. 기업/기관 사용자가 직접 업로드한 이미지
  2. 향후 크롤러 또는 관리자가 저장한 `representativeImageUrl`
  3. 공통 default 공모전 이미지
- 이미지가 없거나 URL 로딩에 실패하면 공통 default 이미지로 전환한다.
- 공모전별 샘플 이미지를 index로 순환 배정하지 않는다.
- default 이미지를 모든 DB 행에 중복 저장하거나 업로드하지 말고 presentation fallback으로 사용하는 방향을 우선한다.
- 깨진 URL fallback은 무한 `error` 반복을 만들지 않는다.

#### 기업/기관 직접 업로드

- `COMPANY` 계정의 개설 요청 작성 및 승인된 공모전 수정 화면에 이미지 선택, 미리보기, 업로드/교체, 삭제 UI를 제공한다.
- 현재 시스템에서 기업·기관 계정은 `accountType=COMPANY`로 취급한다. 새 account type을 임의로 만들지 않는다.
- 개설 요청은 아직 `contestId`가 없으므로 승인 전 `requestId` 단계에서 이미지 소유권을 안전하게 연결할 수 있는 계약을 설계한다.
- 권장 방식 중 현재 media 구조에 가장 자연스러운 하나를 선택한다.
  - 요청을 먼저 생성한 뒤 `CONTEST_REQUEST` 이미지 endpoint에 업로드
  - 요청 생성 multipart endpoint에서 payload와 이미지를 함께 처리
- 승인 시 요청 이미지가 생성된 contest에 안전하게 승계되거나 동일 저장 객체를 참조하도록 한다.
- 거절/삭제/교체된 요청 이미지의 정리 정책을 구현한다.
- 승인된 공모전 수정은 소유한 회사 계정만 이미지를 변경할 수 있어야 한다.

#### 보안

- 이미지 MIME, 확장자, 실제 file signature, 크기 제한을 backend에서 검증한다.
- JPEG, PNG, WebP 등 지원 형식을 명시하고 SVG, 실행 파일, 위장 파일은 거부한다.
- 서버 생성 파일명을 사용하고 path traversal과 덮어쓰기를 막는다.
- 다른 회사의 개설 요청 또는 공모전 이미지를 교체·삭제할 수 없어야 한다.
- 파일 저장과 DB 갱신 실패 시 orphan 파일이나 깨진 참조가 남지 않게 한다.
- frontend object URL은 교체, 취소, unmount 시 해제한다.

#### 크롤링 호환성

- 이번 작업에서 크롤러와 외부 이미지 다운로드를 구현하지 않는다.
- 향후 크롤링 데이터가 `representativeImageUrl`을 저장하면 동일 목록/상세 image resolver에서 표시될 수 있게 한다.
- 외부 이미지 URL은 http/https 형식과 길이를 검증하되 SSRF를 유발하는 서버측 임의 다운로드는 이번 범위에서 수행하지 않는다.

### 6. 기업 계정용 우측 상단 개설 요청

- 목록 페이지 우측 상단 action 영역에 `공모전 개설 요청` 버튼을 배치한다.
- `props.currentUser?.accountType === 'COMPANY'`인 경우에만 표시한다.
- 일반 USER, 비로그인 사용자에게는 표시하지 않는다.
- ADMIN의 관리 기능은 기존 관리자 route에서 유지하며 이 버튼을 일반 사용자용으로 노출하지 않는다.
- frontend 표시만 믿지 말고 backend의 `requireCompany()` 권한 검증을 유지한다.
- 버튼 클릭 시 기존 회사 공모전 개설 요청 route로 이동한다.
- 기존 하단 또는 route nav의 중복 개설 요청 진입점은 제거한다.
- desktop에서는 heading 우측, mobile에서는 heading 아래 자연스러운 위치로 배치한다.

### 7. 마감 임박 공모전

- 공모전 전체 목록 아래에 `마감 임박 공모전` 섹션을 배치한다.
- 일반 목록 일부를 임의로 잘라 사용하지 않는다.
- 실제 DB에서 다음 조건으로 조회한다.
  - status가 `OPEN`
  - deadline이 현재 이후
  - deadline 오름차순
  - 동률 시 contestId 등 결정적 순서
- 표시 개수는 현재 레이아웃에 맞게 3~5개로 명확히 정하고 API limit을 전달한다.
- D-day는 서버 또는 일관된 공통 함수로 실제 deadline에서 계산한다.
- 이미 마감된 공모전과 샘플 날짜를 마감 임박에 표시하지 않는다.
- 해당 데이터가 없으면 명확한 빈 상태를 표시한다.

### 8. 목록의 적합도 제거

- 공모전 목록, 마감 임박 목록, dashboard 카드, 통계 등 목록 route의 모든 적합도 숫자·원형 gauge·badge를 제거한다.
- 목록 조회 시 basis/profile/team을 자동 선택해 fit cache를 조회하지 않는다.
- 목록 정렬 기본값에서 `fit`을 제거한다.
- 샘플 적합도와 `fallback.fit`을 모두 제거한다.
- 적합도 DB table과 계산 API 자체는 삭제하지 않는다.

### 9. 상세 버튼 명칭 변경

- 상세 화면의 `적합도 산정`, `적합도 갱신`, 유사 문구를 사용자 action 기준으로 `적합도 분석`으로 통일한다.
- 분석 중에는 `분석 중` 등 명확한 상태를 표시하고 중복 요청을 막는다.
- 기준 선택이 필요하면 버튼 가까이에 프로필/팀 기준 선택을 둔다.
- 기준이 없거나 프로필이 없는 경우 실행 가능한 것처럼 보이지 않게 안내한다.

### 10. 사용자가 분석한 뒤에만 적합도 표시

- 공모전 상세 최초 진입 시 적합도 결과를 자동 표시하지 않는다.
- 목록에서 상세로 들어갈 때 `fitScore`를 전달하거나 미리 계산하지 않는다.
- 사용자가 상세에서 기준을 선택하고 `적합도 분석` 버튼을 누른 경우에만 계산 API를 호출한다.
- API 성공 후에만 상세 화면 오른쪽 상단에 적합도 점수와 분석 이유를 표시한다.
- 분석 실패 시 이전에 없던 점수를 임의로 생성하지 않고 오류를 표시한다.
- 기준을 변경하면 이전 기준의 점수를 현재 점수처럼 보여주지 않는다. 다시 분석하도록 초기화한다.
- backend GET 상세 API는 기본 요청에서 모든 기준의 fit 결과를 자동 포함하지 않는다.
- cache를 재사용할 수는 있으나, 사용자가 명시적으로 분석을 요청한 POST 응답으로만 현재 화면에 노출한다.
- 새로고침 시 자동 노출 여부는 “버튼을 누른 후에만” 원칙을 우선한다. 서버 cache가 존재한다는 이유만으로 기본 상세 응답에 표시하지 않는다.

### 11. 상단 `공모전 목록` 버튼 삭제

- 목록 페이지 상단 route nav의 `공모전 목록` 버튼을 삭제한다.
- 현재 페이지 heading이 목록임을 명확히 보여 별도 자기 자신 링크가 필요 없게 한다.
- 상세, 제출 준비, 기업 관리 하위 route에서 목록으로 돌아가는 명시적 back action은 유지한다.
- 목록 button 삭제 때문에 요청 내역, 기업 공모전 관리, 관리자 route 접근이 깨지지 않게 한다.

## DB 및 backend

- `sql/01_schema.sql`에 신규 설치용 이미지 저장 계약을 반영한다.
- 기존 DB용 멱등 migration SQL을 `sql/`에 추가한다.
- 현재 media 저장 구조를 재사용할 수 있으면 entity type에 `CONTEST`, 필요 시 `CONTEST_REQUEST`를 안전하게 추가한다.
- URL 이미지와 업로드 이미지의 출처/우선순위를 구분할 별도 필드가 필요하면 nullable 호환 구조로 추가한다.
- 기존 `representative_image_url`과 충돌하거나 같은 의미를 두 필드에 중복 저장하지 않게 최종 계약을 문서화한다.
- 공모전 목록 API는 fit 계산과 분리한다.
- 마감 임박 조회는 backend query에서 조건·정렬·limit을 적용한다.
- 개설 요청 승인 시 이미지 연결 승계를 transaction과 파일 정리 정책에 맞게 처리한다.
- 회사 소유권, 관리자 승인 권한, 공개/종료 상태 정책을 유지한다.
- audit log에 binary, 내부 경로, 민감한 원본 파일명을 과도하게 기록하지 않는다.
- migration 재실행 시 중복 컬럼, index, FK 오류가 없어야 한다.

## 필수 테스트

### Backend

1. 실제 OPEN 공모전 목록이 마감일 기준으로 조회되고 fit 필드가 자동 포함되지 않음.
2. 빈 DB 결과가 빈 배열로 반환되고 샘플 데이터가 없음.
3. 마감 임박 query가 미래 OPEN 항목만 deadline 오름차순으로 반환.
4. 마감·종료 공모전이 마감 임박 목록에서 제외됨.
5. 일반 USER와 비로그인의 개설 요청 API 거부, COMPANY 허용.
6. 회사 A가 회사 B의 요청/공모전 이미지 변경·삭제 불가.
7. 정상 JPEG/PNG/WebP 업로드, 교체, 삭제와 default 복귀.
8. 빈 파일, 크기 초과, MIME/확장자 위장 파일 거부.
9. 개설 요청 승인 시 이미지가 생성 contest에 정상 연결.
10. 요청 거절/이미지 교체 시 DB 참조와 실제 파일 정리.
11. 상세 기본 조회는 fit 미노출, 분석 POST 성공 후 선택 기준 결과 반환.
12. 기준 변경 시 다른 cache 결과가 현재 결과로 오인되지 않음.
13. migration 신규 적용과 재실행 성공.

### Frontend 및 브라우저

1. 목록에 샘플 공모전, 가짜 숫자, 가짜 이미지, 가짜 적합도 없음.
2. `추천 공모전` hero와 문구가 없고 실제 `공모전 목록` 표시.
3. 하단 기준·상태·구분·정렬 bar가 DOM에서 제거됨.
4. 상단 `공모전 목록` 자기 링크가 없음.
5. COMPANY에서만 우측 상단 `공모전 개설 요청` 표시.
6. 일반 USER와 비로그인에서는 개설 요청 미표시.
7. 회사 개설 요청 이미지 선택·미리보기·저장·재진입 정상.
8. 승인된 회사 공모전 이미지 교체·삭제 정상.
9. URL 이미지, 업로드 이미지, 이미지 없음, URL 실패 시 우선순위와 default 정상.
10. 하단 마감 임박 목록이 실제 deadline 순서와 일치.
11. 목록과 마감 임박 영역 어디에도 적합도 미표시.
12. 상세 최초 진입에는 적합도 미표시.
13. `적합도 분석` 성공 이후에만 우측 상단 점수·이유 표시.
14. 기준 변경 후 이전 점수 숨김 및 재분석 필요.
15. desktop과 390x844에서 목록, 상단 action, 이미지, 분석 결과 overflow 없음.
16. console error와 중복 요청 없음.

## 실행 및 검증

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run build
```

- mapper XML 문법 검사
- migration SQL 신규 적용 및 재실행 검사
- 실제 COMPANY 계정과 일반 USER 계정으로 권한 차이 확인
- 브라우저 Network에서 목록 GET이 fit 계산을 호출하지 않는지 확인
- 상세 진입과 분석 버튼 클릭 전후 API 요청 및 응답 차이 확인
- DB에서 contest/open request 이미지 참조, deadline, fit cache 확인
- `git diff --check`

## 금지사항

- 크롤러, scheduler, 외부 HTML parser 구현 금지
- 외부 사이트에서 이미지를 자동 다운로드하는 로직 추가 금지
- 샘플 공모전과 샘플 이미지를 API 실패 fallback으로 유지 금지
- 가짜 적합도, 저장 수, 준비 수, 마감 수 표시 금지
- default 이미지를 모든 contest row에 중복 저장 금지
- 이미지 URL 입력만 유지하고 직접 업로드를 완료 처리 금지
- frontend의 COMPANY 조건만 추가하고 backend 권한 검증 생략 금지
- 목록 조회 또는 상세 최초 진입 시 적합도 자동 계산/노출 금지
- fit cache가 있다는 이유만으로 사용자 분석 전 점수 노출 금지
- 참고 이미지의 모든 필터와 외부 사이트 문구를 그대로 복제 금지
- 기존 저장, 제출 준비, 기업 요청, 관리자 승인, 회사 관리, 문서 업로드 기능 삭제 금지
- 관련 없는 리팩터링, 사용자 변경 되돌리기, 새 라이브러리 설치, commit/push 금지
- 실제 secret, JWT, API key, DB 비밀번호 출력·문서화 금지
- `../prototype*` 수정 금지

## 완료 보고

`docu/work_logs/YYYY-MM-DD_fixer_contest_data_ui_image_fit.md`에 다음을 기록하세요.

- 11개 요구사항별 수정 결과
- 제거한 하드코딩과 샘플 목록
- 공모전 목록 및 마감 임박 API 계약
- 참고 이미지에서 반영한 UI 요소와 제외한 요소
- URL/업로드/default 이미지 우선순위
- 개설 요청 이미지 저장·승인 승계·삭제 정책
- COMPANY 권한과 frontend 노출 규칙
- 적합도 분석 전후 데이터 및 UI 계약
- schema/migration/API 변경
- 변경 파일 목록
- test/build/SQL/API/브라우저 검증 결과
- 크롤링이 이번 범위에서 제외됐다는 기록
- 미수행 검증과 남은 위험
```
