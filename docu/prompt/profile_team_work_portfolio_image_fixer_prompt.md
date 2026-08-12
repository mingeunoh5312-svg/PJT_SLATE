# 프로필·팀·작품·포트폴리오 이미지 및 배지 수정 프롬프트

```text
Slate 내 정보(`/profile`)를 중심으로 프로필 이미지, 참여 팀·참여 작품 대표 이미지, 포트폴리오 썸네일 선택, 검증 배지 노출 문제를 직접 수정하세요. 설명이나 프런트 임시 미리보기에서 멈추지 말고 DB, backend, frontend, 테스트, 실제 브라우저 검증, 작업 로그까지 완료하세요.

작업 루트:
- `/Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate`

원본 요구사항:
- `docu/user_temp/What_to_do.md`

먼저 확인:
- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/05_backend/backend_baseline.md`
- `docu/07_database/database_baseline.md`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/views/BoardView.vue`
- `frontend/src/services/api.js`
- `frontend/src/styles/slate.css`
- `backend/src/main/java/com/slate/profiles/**`
- `backend/src/main/java/com/slate/teams/**`
- `backend/src/main/java/com/slate/boards/**`
- `backend/src/main/resources/mappers/ProfileMapper.xml`
- `backend/src/main/resources/mappers/TeamMapper.xml`
- `backend/src/main/resources/mappers/BoardMapper.xml`
- `backend/src/main/java/com/slate/boards/WorkFileService.java`
- `backend/src/main/resources/mappers/WorkFileMapper.xml`
- `sql/01_schema.sql`
- 관련 테스트와 기존 migration SQL

현재 작업 트리에 사용자 변경이 있을 수 있습니다. 시작 시 `git status --short`와 관련 파일 diff를 확인하고 기존 변경을 보존하세요. 특히 `ProfileView.vue`, `ProfileService.java`, `ProfileMapper.xml`, `slate.css`, schema 및 문서의 기존 변경을 되돌리거나 덮어쓰지 마세요.

## 확인된 현재 상태

1. `member_profile`과 프로필 API 계약에는 프로필 이미지 필드가 없다.
2. `/profile`의 프로필 이미지는 표시 이름 첫 글자로 만든 placeholder이며, 편집 화면에는 이미지 선택·업로드·삭제 기능이 없다.
3. 참여 팀 카드도 팀 이름 첫 글자 placeholder만 사용하며, 현재 team schema/API에는 대표 이미지 계약이 없다.
4. 참여 작품 카드는 `youtubeThumbnailUrl || thumbnailUrl`을 시도하지만 일반 업로드 작품용 대표 이미지 저장 계약은 확인되지 않는다. 이미지가 없으면 `WORK` 문자 placeholder를 사용한다.
5. `portfolio_item.thumbnail_url`과 프런트의 `thumbnailUrl` 문자열 입력은 있으나 사용자가 로컬 이미지를 직접 업로드하는 기능은 없다.
6. 포트폴리오 화면은 `verified === true`가 아닌 항목에도 `verificationStatus`가 있으면 미검증·확인 필요·오류 배지를 표시한다.
7. 포트폴리오 목록/카드에서 내부 값인 `PUBLIC_DATA_MANUAL` 또는 `sourceType`이 사용자에게 노출된다.

## 최종 사용자 계약

### 이미지 공통 규칙

- 사용자가 업로드한 이미지는 새로고침, 재로그인, 재배포 후에도 서버 저장소와 DB 참조를 통해 유지한다.
- 브라우저의 `blob:` URL, base64 문자열, `localStorage`, 샘플 에셋 임의 매핑으로 영구 저장을 흉내 내지 않는다.
- 실제 이미지가 있으면 해당 이미지를 표시하고, 없거나 로딩에 실패하면 용도별 고정 default 이미지를 표시한다.
- default 이미지는 사용자/팀/작품마다 샘플 이미지를 순환 배정하지 않는다. 용도별로 의미가 분명한 하나의 기본 에셋 또는 기존 디자인에 맞는 공통 placeholder를 사용한다.
- 업로드 이미지는 인증, 대상 소유권 또는 수정 권한, MIME과 파일 signature, 확장자, 크기 제한을 서버에서 검증한다.
- JPEG, PNG, WebP 등 실제 지원 형식을 명시하고 SVG, 실행 파일, 위장 파일은 허용하지 않는다. 허용 크기는 기존 환경설정과 업로드 정책을 확인해 상수/설정으로 관리한다.
- 저장 파일명은 사용자 원본 파일명을 경로로 사용하지 말고 충돌과 path traversal을 막는 서버 생성 이름을 사용한다.
- 이미지 조회 URL은 frontend가 직접 서버 내부 경로를 조합하지 않도록 backend 응답의 명시적 URL 필드로 제공한다.
- 이미지 교체 시 DB 참조와 실제 파일 정리 순서를 안전하게 처리한다. 다른 엔티티가 참조하는 파일을 삭제하지 않는다.
- 이미지 삭제는 참조를 제거한 뒤 default 이미지로 돌아가야 하며, 없는 이미지를 다시 삭제해도 데이터가 깨지지 않게 처리한다.
- 기존 파일 저장/stream 구조를 재사용할 수 있는지 먼저 검토하되, 영상용 `file_metadata` 정책을 무리하게 우회하거나 이미지에 영상 duration 규칙을 적용하지 않는다.

### 1. 프로필 이미지

- 프로필 생성 및 수정 화면에서 이미지 선택, 즉시 미리보기, 업로드/교체, 삭제를 제공한다.
- 저장 취소, 업로드 실패, 프로필 저장 실패 시 화면과 DB가 서로 다른 이미지 상태로 남지 않게 처리한다.
- 내 프로필 조회와 공개 프로필 조회에 `profileImageUrl` 또는 저장소 전체에서 합의한 동일 이름의 nullable URL 필드를 반환한다.
- `/profile` hero와 `/profile/edit` 미리보기에 실제 프로필 이미지를 표시한다.
- 프로필 이미지가 없는 경우 용도에 맞는 default 프로필 이미지를 표시한다.
- 프로필 삭제/복구 정책과 이미지 참조 처리도 명시하고 테스트한다.
- 동일 프로필이 매칭 후보, 팔로워/팔로잉 목록, 헤더 등에서 노출되는 기존 경로를 검색해 응답에 이미지가 이미 전달되는 범위에서는 일관되게 표시한다. 관련 없는 화면을 샘플 이미지로 채우지는 않는다.

### 2. 참여 팀 이미지

- `/profile`의 참여 중인 팀 카드에서 팀 대표 이미지가 있으면 표시하고, 없으면 default 팀 이미지를 표시한다.
- 현재 team schema/API에 대표 이미지 필드가 없으므로 nullable 영구 저장 계약과 조회 URL을 추가한다.
- 팀 대표 이미지 업로드·교체·삭제는 팀 생성/수정 UI의 자연스러운 위치에서 제공하고 팀 리더 등 기존 수정 권한을 서버에서 재사용한다.
- `/api/teams/mine`, 팀 단건 조회, 팀 목록 등 동일 team DTO/map을 사용하는 주요 조회 경로가 같은 이미지 필드를 반환하도록 mapper 누락을 방지한다.
- 팀 이미지 기능 때문에 기존 팀 생성·수정·삭제, 모집, 지원, 멤버 권한 흐름을 깨뜨리지 않는다.

### 3. 참여 작품 이미지

- `/profile`의 참여 작품 카드에서 작품 대표 이미지 우선순위를 명시적으로 구현한다.
- 권장 우선순위: 사용자가 지정한 대표 이미지 > YouTube 썸네일 > default 작품 이미지.
- 현재 작품 저장 계약에 사용자 지정 대표 이미지가 없다면 nullable 영구 저장 필드와 업로드·교체·삭제 흐름을 추가한다.
- YouTube 작품은 기존 `youtubeThumbnailUrl`을 유지하며 사용자 대표 이미지가 없을 때 자동 fallback으로 사용한다.
- 서버 업로드 영상의 바이너리를 `<img>`로 직접 사용하지 않는다. 별도 대표 이미지가 없다면 default 작품 이미지를 사용한다.
- 작품 수정 권한과 게시글/팀 작품 승인 정책을 우회하지 않는다.
- `/profile`이 현재 전체 WORK 게시글을 받은 뒤 사용자와 팀 ID로 필터링하는 구조도 확인한다. 이미지 작업 범위를 넘어 무리하게 API를 재설계하지 않되 권한이 없는 비공개 작품을 노출하지 않는지 검증한다.

### 4. 포트폴리오 썸네일 선택

- 포트폴리오 등록·수정 화면에서 아래 두 방식을 사용자가 명시적으로 선택할 수 있게 한다.
  - 직접 이미지 업로드
  - 포트폴리오에 입력한 YouTube URL의 영상 썸네일 사용
- 단순한 외부 `thumbnailUrl` 문자열 입력만으로 직접 이미지 업로드 요구사항을 대체하지 않는다.
- YouTube URL은 기존 preview/metadata API를 재사용해 검증된 영상 ID와 썸네일을 사용한다. 임의 문자열 치환만으로 썸네일 URL을 만들지 않는다.
- YouTube가 아닌 URL, 잘못된 영상, 비공개/삭제 영상, metadata 조회 실패 시 오류와 fallback을 명확히 표시한다.
- 직접 이미지와 YouTube 썸네일을 전환했을 때 선택되지 않은 값이 다시 저장되어 우선순위를 뒤집지 않게 한다.
- 수정 화면 재진입 시 현재 선택 방식과 미리보기를 정확히 복원한다.
- 포트폴리오 대시보드, 목록, 상세에서 동일한 최종 썸네일을 표시하고 없으면 default 포트폴리오 이미지를 표시한다.
- 기존 KOBIS 검색, `creditName`, 검증 저장, 등록·수정·삭제 route를 유지한다.

### 5. 포트폴리오 배지와 source type 표시

- 대시보드, 목록, 상세의 모든 포트폴리오 표시 지점을 전수 검색한다.
- `verified === true`인 항목에만 기존 `Verified` 배지를 표시한다.
- `NOT_VERIFIED`, `AMBIGUOUS`, `ERROR`, null 항목에는 어떤 검증 상태 배지도 표시하지 않는다.
- 검증 상태와 DB 값 자체는 삭제하거나 강제로 변경하지 않는다. 이번 요구는 사용자 화면의 배지 노출 규칙이다.
- `PUBLIC_DATA_MANUAL`, `PUBLIC_DATA`, `MANUAL` 같은 내부 `sourceType` 원문을 카드·목록·상세의 배지나 subline에 노출하지 않는다.
- KOBIS 출처 등 사용자에게 필요한 출처 설명이 있다면 내부 enum 원문 대신 기존 한국어 라벨을 사용하되, 요구되지 않은 새 배지를 만들지 않는다.

## DB 및 migration

- `sql/01_schema.sql`에는 신규 설치용 최종 schema를 반영한다.
- 이미 구축된 로컬 DB에도 적용 가능한 별도 멱등 migration SQL을 `sql/`에 추가한다.
- migration 재실행 시 중복 컬럼, FK, 인덱스 오류가 없어야 한다.
- 이미지 참조를 URL 문자열로만 저장할지 파일 metadata FK로 저장할지 기존 파일 저장 구조와 삭제 정책을 조사해 결정하고, 결정 이유를 작업 로그에 기록한다.
- FK를 사용한다면 업로더, 엔티티 소유권, 삭제 상태, 참조 무결성을 고려한다.
- 기존 데이터는 이미지 null 상태로 정상 조회되어 default 이미지가 표시되어야 한다.
- 기존 행에 샘플 이미지 URL을 backfill하지 않는다.

## Backend 구현

- entity별 이미지 업로드/삭제 API 또는 기존 수정 API와 결합된 안전한 계약을 제공한다.
- multipart API와 JSON 수정 API의 책임 및 실행 순서를 일관되게 정한다.
- controller에서 인증 사용자 없이 이미지 변경이 불가능해야 한다.
- service에서 프로필 본인, 팀 리더, 작품/게시글 소유자 등 기존 권한을 검증한다.
- mapper의 등록, 수정, 단건 조회, 목록 조회에서 이미지 필드가 누락되지 않게 한다.
- 응답 URL 생성은 환경별 backend base URL을 하드코딩하지 않는다.
- 파일을 공개 static directory에 무조건 노출하지 말고 현재 인증/공개 범위 정책에 맞는 조회 방식을 사용한다.
- 업로드 실패, DB 실패, 파일 삭제 실패의 보상/정리 정책을 구현하고 테스트한다.
- 감사 로그가 필요한 변경은 기존 `AuditLogService` 패턴을 따른다. 파일명, 경로, binary, secret을 감사 로그에 과도하게 기록하지 않는다.

## Frontend 구현

- 공통으로 재사용 가능한 이미지 picker/preview 코드가 실제 중복을 줄일 때만 작은 component 또는 helper로 분리한다.
- `<input type="file" accept="image/...">`만 신뢰하지 말고 frontend 사전 안내와 backend 최종 검증을 함께 둔다.
- 선택 직후 object URL 미리보기를 제공한다면 교체, 취소, unmount 시 `URL.revokeObjectURL()`로 해제한다.
- 업로드 중 중복 제출 방지, 실패 문구, 삭제 확인, 접근 가능한 버튼 label을 제공한다.
- `<img>`에는 용도에 맞는 대체 텍스트를 제공한다. 장식용 default만 `alt=""`로 처리한다.
- 이미지 비율은 프로필은 정사각형/원형 crop, 팀·작품·포트폴리오는 기존 카드 비율을 유지하도록 `object-fit: cover`를 사용한다.
- 깨진 URL은 `@error` 등으로 default 이미지로 전환하되 무한 오류 반복을 막는다.
- desktop과 390x844에서 버튼, 미리보기, 카드가 overflow하지 않게 한다.

## 필수 테스트

### Backend

1. 정상 JPEG/PNG/WebP 업로드와 조회 URL 반환.
2. 빈 파일, 제한 초과, 허용하지 않은 MIME/확장자, 내용 위장 파일 거부.
3. 다른 사용자의 프로필, 팀, 작품 이미지 변경·삭제 거부.
4. 이미지 최초 등록, 교체, 삭제 후 DB 참조와 실제 파일 상태 일치.
5. 이미지가 null인 기존 프로필·팀·작품·포트폴리오 조회 정상.
6. 프로필/팀/작품/포트폴리오 주요 목록과 단건 응답의 이미지 URL 계약 일치.
7. migration 신규 적용 및 재실행 성공.
8. 포트폴리오 검증 상태와 `verified` Boolean 계약이 기존대로 유지됨.

### Frontend 및 브라우저

1. 프로필 이미지 선택 → 미리보기 → 저장 → 새로고침 → 유지.
2. 프로필 이미지 교체와 삭제 후 즉시 반영 및 default 복귀.
3. 팀 대표 이미지가 있는 팀과 없는 팀이 각각 실제/default 이미지 표시.
4. 작품 대표 이미지, YouTube 썸네일, 이미지 없음의 세 경우가 우선순위대로 표시.
5. 포트폴리오 직접 업로드 → 저장 → 상세 → 수정 재진입 시 선택 방식과 이미지 유지.
6. 포트폴리오 YouTube URL → 썸네일 선택 → 저장 → 상세 → 수정 재진입 정상.
7. 직접 이미지와 YouTube 썸네일 사이를 양방향 전환해도 이전 값이 잘못 노출되지 않음.
8. 검증 완료 항목에만 `Verified` 표시, 나머지 모든 검증 상태에는 배지 없음.
9. `PUBLIC_DATA_MANUAL` 및 내부 source type 원문이 `/profile` 관련 화면 어디에도 표시되지 않음.
10. 이미지 404/조회 실패 시 default 이미지 표시, console error와 무한 요청 없음.
11. desktop과 390x844에서 overflow, 레이아웃 깨짐, 접근 불가능한 file input 없음.

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
- 실제 로그인 계정으로 multipart 요청/응답과 DB 참조 확인
- 브라우저 Network에서 업로드, 교체, 삭제, 조회 상태 코드와 응답 필드 확인
- `git diff --check`
- 관련 테스트가 기존 사용자 변경 때문에 실패하면 원인을 분리해 기록하고, 기존 변경을 되돌려 통과시키지 않는다.

## 금지사항

- frontend 미리보기만 만들고 영구 저장이 된 것처럼 완료 보고 금지
- base64/blob/localStorage를 영구 이미지 저장소로 사용 금지
- 외부 랜덤 이미지, 사용자별 샘플 이미지 순환 fallback, seed 이미지 강제 주입 금지
- URL 문자열 입력만 추가해 직접 업로드 요구사항을 완료 처리 금지
- MIME header나 파일 확장자 하나만 검사하는 구현 금지
- 권한 검사 없는 공용 업로드·삭제 API 금지
- 기존 KOBIS 검증 상태를 삭제하거나 미검증 항목을 `VERIFIED`로 변경 금지
- 프로필 이미지 작업을 이유로 사용자 계정 avatar와 member profile의 책임을 임의로 합치지 않기
- 실제 API key, DB 비밀번호, JWT, `.env` 값 출력·문서화 금지
- 관련 없는 리팩터링, 사용자 변경 되돌리기, 새 라이브러리 설치, commit/push 금지
- `../prototype*` 수정 금지. 비교가 필요하면 읽기 전용으로만 사용

## 완료 보고

`docu/work_logs/YYYY-MM-DD_fixer_profile_media_and_portfolio_display.md`에 다음을 기록하세요.

- 요구사항별 기존 원인과 최종 사용자 동작
- 이미지 저장소와 DB 참조 구조를 선택한 이유
- schema와 migration 변경 사항
- profile/team/work/portfolio API 요청·응답 계약
- 이미지 검증, 권한, 교체·삭제 정리 정책
- default 이미지 위치와 표시 우선순위
- 포트폴리오 썸네일 선택 및 복원 방식
- Verified 및 source type 최종 노출 규칙
- 변경 파일 목록
- test/build/SQL/API/브라우저 검증 결과
- 미수행 검증과 남은 위험
```
