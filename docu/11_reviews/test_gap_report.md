# Test Gap Report

작성일: 2026-06-16
대상: `prototype_3`

## Executed

### Backend

명령:

```powershell
mvn test
```

위치: `prototype_3/backend`

결과:

- 성공
- 총 36 tests
- failures 0
- errors 0
- skipped 0

포함된 테스트:

- `BoardControllerYoutubePreviewTest`
- `BoardServicePostSearchTest`
- `BoardServiceYoutubeDeleteTest`
- `BoardServiceYoutubeUpdateTest`
- `YoutubeClientTest`
- `YoutubePropertiesTest`
- `AiMatchingRecommendationServiceTest`
- `OpenAiPropertiesTest`
- `KobisRoleMatcherTest`

메모:

- 최초 sandbox 실행은 Maven 중앙 저장소 접근 차단으로 실패했다.
- 권한 승인 후 재실행하여 통과했다.

### Frontend

명령:

```powershell
npm run build
```

위치: `prototype_3/frontend`

결과:

- 실패
- Vite transform은 96 modules까지 진행됨
- `vite:prepare-out-dir` 단계에서 `prototype_3/frontend/dist/local-run` 삭제가 `EPERM`으로 실패
- 권한 승인 후 재시도해도 동일
- `Get-Process node`에서 기존 node 프로세스 1개가 확인됐고, `dist/local-run`에는 Vite server 로그/헬퍼 파일이 남아 있었다.

판단:

- 현재 실패는 소스 compile 오류라기보다 기존 local-run 산출물 또는 파일 잠금 문제일 가능성이 크다.
- 다만 공식 build script는 실패 상태이므로 배포 전 반드시 정리해야 한다.

## Backend Test Gaps

### Security

- 파일 stream authorization 테스트가 없다.
  - 공개 게시글 파일 anonymous 허용 여부.
  - 비공개/회사/팀 파일 anonymous 거부.
  - 팀 비멤버 거부.
  - 삭제/보류 파일 거부.
- 공개 기업 서류 업로드 abuse 케이스가 없다.
  - 잘못된 사업자번호 거부.
  - 반복 업로드 제한.
  - 문서 타입/확장자/content type/magic 검증.
- 데모 access code gate 테스트가 없다.
- admin role과 granular permission 조합 테스트가 일부 서비스 수준에만 분산되어 있고 route-level 통합 테스트가 부족하다.

### External API

- KOBIS 실제 API key smoke가 없다.
- KOBIS client 장애 시 portfolio verification 상태 전이 테스트가 부족하다.
- YouTube 실제 API key smoke가 없다.
- YouTube API 장애 시 게시글/팀 작업물 등록 UX 테스트가 부족하다.
- OpenAI API 호출 실패 시 fallback 정책 테스트가 문서 결정과 다르다.
  - 현재 테스트는 API key 누락 예외 전파를 기대한다.
  - 문서 결정대로라면 기존 점수 기반 추천 fallback을 기대해야 한다.

### Files

- ffprobe 미설치/실패 시 client duration 신뢰 정책 테스트가 없다.
- 3분 초과 파일의 server-probed duration 거부 테스트가 필요하다.
- `application/octet-stream` 업로드의 실제 video 여부 검증 테스트가 없다.
- orphan file cleanup, soft delete 후 physical delete 대상 처리 테스트가 없다.

### Database And Concurrency

- 팀 지원/초대 중복 요청 동시성 테스트가 없다.
- 슬롯 정원 초과 수락 race 테스트가 없다.
- 게시글 조회수 중복 count race 테스트가 없다.
- SQL seed/reset 반복 적용 테스트가 없다.
- YouTube metadata migration은 idempotent procedure 형태지만 실제 schema 적용 테스트가 없다.

### Auth And Account State

- 만료/변조 JWT 통합 테스트가 부족하다.
- 승인 대기 회사 계정이 회사 전용 API에서 일관되게 차단되는지 테스트가 필요하다.
- 제재 계정의 API 접근 제한 테스트가 필요하다.

## Frontend Test Gaps

현재 `prototype_3/frontend`에서 명시적인 unit/e2e test 파일은 확인하지 못했다. `rg --files prototype_3/frontend | rg "(test|spec|vitest|playwright|cypress)"` 결과는 실제 테스트 파일이 아니라 `ContestView.vue`와 asset 파일명만 반환했다.

필요 테스트:

- login/register/account status flow.
- admin route guard와 permission 없는 관리자 UX.
- AI 추천 실패 fallback 표시.
- YouTube preview 실패 표시.
- 파일 업로드 3분 제한과 progress/error UX.
- work file stream 접근 거부 UX.
- desktop/mobile viewport smoke.
- 새로고침 시 history fallback 동작.

## Manual Smoke Checklist

MVP 후 최소 smoke:

- backend local 기동 후 `/api/auth/login`, `/api/auth/me`.
- frontend local 접속, 데모 access code 통과, 로그인.
- 일반 사용자 profile 생성/수정, KOBIS 검색/검증.
- 팀 생성, 모집 슬롯 생성, 지원/초대/수락.
- AI 추천 호출. OpenAI key 있음/없음 모두 확인.
- YouTube preview와 YouTube 작업물 등록.
- 서버 업로드 작업물 등록, stream 권한 확인.
- 관리자 회사 승인, 콘텐츠 신고 처리, 파일 moderation.
- frontend production build, static preview, hard refresh.

## Unexecuted

- 실제 DB apply: `00_create_database.sql`부터 seed까지 실행하지 않았다.
- 실제 외부 API 호출: KOBIS, YouTube, OpenAI 실키 호출은 하지 않았다.
- 브라우저 screenshot smoke는 하지 않았다.
- 모바일 viewport 확인은 하지 않았다.
- 업로드 파일 실물 검증은 하지 않았다.
- concurrent request 검증은 하지 않았다.
