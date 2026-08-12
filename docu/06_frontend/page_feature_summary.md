# Slate 접근 가능 페이지 기능 정리

## 기준

- 분석 범위: `Slate/frontend/src/router/index.js`, `Slate/frontend/src/views/*.vue`, `Slate/frontend/src/services/api.js`
- 기준 시점: 로컬 코드 기준 정적 분석
- 브라우저 실행, 로그인 계정별 E2E 검증은 수행하지 않았다.
- `VITE_DEMO_ACCESS_GATE=true`이면 `/demo-access`에서 접속 코드를 통과하기 전 모든 페이지가 `/demo-access?redirect=...`로 이동한다.

## 접근 조건

| 표기 | 의미 |
| --- | --- |
| 공개 | 로그인 없이 접근 가능한 route. 일부 버튼은 로그인 후 동작한다. |
| 게스트 | 비로그인 사용자를 위한 route. 로그인 상태면 홈으로 이동한다. |
| 인증 | 로그인 토큰이 필요하다. 미로그인 상태면 `/login?redirect=...`로 이동한다. |
| 관리자 | 로그인 + `ADMIN` 계정이 필요하다. 세부 관리자 기능은 화면 내부 permission도 확인한다. |
| 기업 | route 자체는 인증이지만, 화면 주요 기능은 `COMPANY` 계정에서만 열린다. |

## 공통 동작

- 일반 서비스 화면은 `AppLayout`, 인증 화면은 `AuthLayout`, 관리자 화면은 `AdminLayout` 계열 meta로 렌더링된다.
- 일반 레이아웃은 좌측/모바일 내비게이션, 로그인/회원가입 진입, 내 프로필 이동, 알림 목록과 읽음 처리, 알림 대상 route 이동을 제공한다.
- 인증 필요 route에서 토큰이 없거나 `me()` 확인에 실패하면 로그인으로 보낸다.
- 관리자 route는 `me().accountType === 'ADMIN'`이 아니면 홈으로 보낸다.

## 주요 페이지별 기능

### 홈

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/` | 공개 | 게스트에게 Slate 사용 흐름과 최신 공개 콘텐츠를 보여준다. 로그인 사용자는 내 프로필, 내 팀, 받은 초대, 미확인 알림, 팀 계획 같은 활동 요약과 바로가기 카드를 본다. 마감 임박 공모전과 최근 작업물을 조회하고, 로그인 사용자는 공모전 저장과 이미지 미리보기를 사용할 수 있다. |

### 인증과 가입

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/login` | 게스트 | 아이디/비밀번호 로그인, 데모 계정 자동 입력, 비밀번호 표시 토글, 아이디/비밀번호 찾기 안내, 기업 승인 대기 안내 링크를 제공한다. 로그인 성공 후 redirect query가 있으면 해당 route로 이동한다. |
| `/register` | 게스트 | 일반 회원가입과 기업 회원가입 유형을 선택한다. |
| `/register/user` | 게스트 | 일반 사용자 회원가입을 처리한다. 가입 후 자동 로그인하고 가입 완료 페이지로 이동한다. |
| `/register/company` | 게스트 | 기업 사용자 회원가입을 처리한다. 공공데이터 기반 회사 검색, 회사 직접 입력, 사업자등록번호 등 기업 정보를 입력하고 승인 대기 상태로 가입한다. |
| `/register/complete` | 공개 | 일반 회원가입 완료 후 다음 이동 경로를 안내한다. 로그인 상태면 내 정보로, 아니면 로그인으로 이동한다. |
| `/register/company/pending` | 공개 | 기업 계정이 관리자 승인 검토 중임을 안내한다. |
| `/demo-access` | 공개 | 데모 접근 코드 입력 화면이다. 코드 검증에 성공하면 원래 redirect 경로로 이동한다. 데모 gate가 꺼져 있으면 홈으로 이동한다. |

### 매칭

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/matching` | 인증 | `/matching/teams`로 이동하는 redirect route다. |
| `/matching/members` | 인증 | 팀 기준으로 팀원을 추천한다. 관리 가능한 팀과 모집 slot을 선택하고 장르, 역할, 지역, 경력, 참여 가능 일정, 협업 조건 필터를 적용한다. 추천 점수, 추천 이유, 포트폴리오 요약, 팔로우, 팀원 초대, 초대 취소, 페이지네이션을 제공한다. |
| `/matching/members/:userId` | 인증 | 추천 팀원 또는 팔로우/초대 목록에서 특정 회원의 상세 정보를 본다. 후보 조건, 팀/모집 정보, 추천 이유, 강점, 협업 조건, 포트폴리오 요약, 팔로우 및 초대 액션을 제공한다. |
| `/matching/teams` | 인증 | 내 프로필 기준으로 팀을 추천한다. 장르, 역할, 지역, 경력, 협업 조건 필터를 적용하고 팀 저장, 팀 지원, 지원 취소, 상세 이동을 제공한다. |
| `/matching/teams/:teamId` | 인증 | 추천 팀의 상세 정보를 본다. 팀 장르, 지역, 모집 정보, 추천 점수, 저장/지원 액션을 제공한다. |
| `/matching/ai` | 인증 | 기존 AI 추천 URL 호환용 redirect route다. query에 따라 `/matching/members?view=ai` 또는 `/matching/teams?view=ai`로 이동한다. |

매칭 query 기반 화면:

| 경로 예시 | 기능 |
| --- | --- |
| `/matching/members?view=ai` | OpenAI 기반 팀원 추천을 명시적으로 요청하고 AI 추천 결과를 목록/상세로 연다. |
| `/matching/teams?view=ai` | OpenAI 기반 팀 추천을 명시적으로 요청한다. |
| `/matching/members?view=following` | 내가 팔로우한 회원 목록을 필터와 함께 보여준다. |
| `/matching/members?view=invited` | 내가 초대한 팀원 목록을 상태 필터와 함께 보여주고 대기 중 초대를 취소할 수 있다. |
| `/matching/teams?view=saved` | 저장한 팀 목록을 보여주고 저장 해제, 상세 보기, 역할 선택 후 지원을 제공한다. |
| `/matching/teams?view=applied` | 내가 지원한 팀 목록을 상태 필터와 함께 보여주고 대기 중 지원을 취소할 수 있다. |

### 팀

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/teams` | 인증 | 내 팀 요약, 다음 처리 업무, 활성/종료 팀 목록을 보여준다. 팀 생성, 팀 상세, 팀 찾기, 받은 초대 화면으로 이동한다. |
| `/teams/new` | 인증 | 팀명, 설명, 상태, 지역, 장르, 예상 기간, 정원, 대표 이미지를 입력해 팀을 생성한다. |
| `/teams/invitations` | 인증 | 받은 팀 초대 목록을 보여주고 수락/거절을 처리한다. 최근 처리한 초대도 확인한다. |
| `/teams/:teamId` | 인증 | 팀 상세 대시보드다. 팀 소개, 리더 공개 프로필 이동, 대표 이미지 미리보기, 멤버/모집/지원 현황/계획/로케이션 진입, 계획 진행률, 일정 안내, 팀 종료 진입을 제공한다. |
| `/teams/:teamId/edit` | 인증 | 팀 정보를 수정하고 대표 이미지 업로드/삭제, 팀 삭제 요청을 처리한다. |
| `/teams/:teamId/close` | 인증 | 팀 종료 또는 재개를 처리한다. 종료 유형과 사유를 입력하고 종료 snapshot 기반 재개를 지원한다. |
| `/teams/:teamId/members` | 인증 | 팀원 목록, 역할 변경, 팀원 내보내기, 팀 나가기, 리더 위임을 처리한다. |
| `/teams/:teamId/recruitments` | 인증 | 모집 공고 목록과 모집 공고 생성/수정/삭제를 관리한다. 각 공고의 구인 slot 생성/수정/삭제도 함께 관리한다. |
| `/teams/:teamId/requests` | 인증 | 팀 지원 대기 목록과 초대 대기 목록을 분리해 보여준다. 지원자 공개 프로필 확인, 지원 수락/거절, 초대 상태 확인을 제공한다. |
| `/teams/:teamId/plans` | 인증 | 팀 전체 일정과 계획 진행 상태를 보여준다. `?view=schedule`이면 일정 영역으로 스크롤한다. 계획 상태 변경도 처리한다. |
| `/teams/:teamId/plans/new` | 인증 | 새 팀 계획을 만든다. 제목, 설명, 일정, 상태를 입력한다. |
| `/teams/:teamId/plans/:planItemId/edit` | 인증 | 기존 팀 계획을 수정한다. |
| `/teams/:teamId/locations` | 인증 | 팀 컨텍스트의 AI 로케이션 탐색 화면이다. 팀 장면 의도와 저장 후보를 팀 기준으로 다룬다. |

### AI 로케이션 탐색

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/locations` | 인증 | 개인 로케이션 탐색 화면이다. 장면 prompt, 추천 개수, 상위 지역/세부 지역 필터를 입력해 AI 촬영지 추천을 요청한다. 추천/저장 후보 탭, 지도 표시, 후보 선택 시 지도 이동, 개인 또는 참여 팀 후보지 저장을 제공한다. |
| `/teams/:teamId/locations` | 인증 | 팀 로케이션 탐색 화면이다. 팀 맥락 포함 옵션, 팀 저장 후보 조회, 개인 탐색으로 이동, 팀 상세로 돌아가기를 제공한다. |

### 게시판

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/boards` | 공개 | 게시판 홈이다. 최신 작업물, 자유 게시판 최신 글, 인기 작업물, 인기 프로필을 보여준다. 작업물/자유/인기 탭으로 전환하고, 게시글 작성과 팀 작업물 승인 도구로 이동한다. |
| `/boards/search` | 공개 | 통합 검색 결과 화면이다. 검색 범위, 키워드, 정렬, 자유글 분류, 작업물 유형, 장르 필터를 query로 복원한다. |
| `/boards/new` | 인증 | 작업물 또는 자유 게시글을 등록한다. 작업물은 장르/유형, 대표 이미지, YouTube 미리보기, 영상 파일 업로드, 팀 작업물 승인 요청을 지원한다. |
| `/boards/:postId` | 공개 | 게시글 상세다. 작업물 미디어, YouTube/파일 정보, 좋아요, 리뷰 작성/수정/삭제, 게시글/리뷰 신고, 작성자 프로필 이동을 제공한다. 작성자 또는 권한자에게 수정/삭제 액션을 제공한다. |
| `/boards/:postId/edit` | 인증 | 게시글 수정 화면이다. 기존 작업물 이미지, YouTube, 파일 연결 상태를 복원해 수정한다. |

게시판 query 기반 화면:

| 경로 예시 | 기능 |
| --- | --- |
| `/boards?tab=WORK` | 작업물 게시판 목록을 보여준다. 작품 유형, 장르, 검색어, 정렬을 적용한다. |
| `/boards?tab=FREE` | 자유 게시판 목록을 보여준다. 자유글 분류, 검색어, 정렬을 적용한다. |
| `/boards?tab=POPULAR` | 인기 작업물과 인기 프로필을 보여준다. 주간/월간/전체 기간, 작품 유형, 장르 필터를 적용한다. |

### 공개 프로필

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/profiles/:profileId` | 공개 | 공개 프로필을 보여준다. 프로필 이미지, 이름, 소개, 지역, 경력, 역할 태그, 상세 소개, 팔로우/팔로우 취소와 팔로워 수를 제공한다. |
| `/profiles/:profileId/portfolio/:portfolioItemId` | 공개 | 공개 포트폴리오 상세를 보여준다. 썸네일, 역할/크레딧/source 정보, 설명, 외부 작품 링크를 제공한다. |

### 공모전

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/contests` | 공개 | 공모전 목록이다. 마감 임박 공모전, 접수 중 공모전, 저장한 공모전 보기, 검색어, 대상, 지역, 상금, 마감 기간, 공모전 유형 필터, 페이지 크기/페이지네이션, 외부 출처 링크, 저장 버튼을 제공한다. |
| `/contests/:contestId` | 공개 | 공모전 상세다. 대표 이미지, 출처/수집 정보, 모집 대상, 상금, 일정, 외부 원문 링크, 저장, 적합도 분석 실행, 제출 준비 진입, 수정 요청 진입, 기업 소유 공모전 수정 진입을 제공한다. |
| `/contests/:contestId/prepare` | 인증 | 제출 준비 화면이다. 프로필 또는 팀 기준으로 적합도 분석을 실행하고 체크리스트와 메모를 저장한다. |
| `/contests/:contestId/edit-request` | 인증 | 일반 사용자의 공모전 수정 요청 안내 화면이다. 현재 코드에서는 별도 제출 API가 아니라 안내 패널로 구성되어 있다. |
| `/contests/new-request` | 인증/기업 | 기업 공모전 개설 요청 화면이다. 기업 계정이 아니면 기업 전용 안내를 보여준다. |
| `/contests/company/new` | 인증/기업 | 기업 공모전 개설 요청 화면이다. 회사 승인 서류 업로드/삭제, 공모전 요청 폼, 대표 이미지 미리보기를 제공한다. |
| `/contests/requests` | 인증/기업 | 내가 제출한 기업 공모전 개설 요청 내역을 보여준다. 요청 상태, 승인된 공모전 이동, 검토 사유를 확인한다. |
| `/contests/company` | 인증/기업 | 승인된 기업 공모전 관리 목록이다. 내가 관리하는 공모전 수정, 종료 요청을 제공한다. |
| `/contests/company/:contestId/edit` | 인증/기업 | 승인된 기업 공모전 정보를 수정한다. 대표 이미지 업로드/삭제, 일정/대상/지역/상금 등 공모전 정보를 수정한다. |

### 내 정보와 포트폴리오

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/profile` | 인증 | 내 정보 대시보드다. 프로필 요약, 팔로워/팔로잉 다이얼로그, 프로필 수정, 포트폴리오 추가, 참여 팀, 참여 작품, 포트폴리오 요약, YouTube 포트폴리오와 계정 관리 바로가기를 제공한다. |
| `/profile/edit` | 인증 | 프로필 생성/수정 화면이다. 표시 이름, 소개, 역할, 장르, 지역, 경력/협업 조건, 프로필 이미지 업로드/삭제를 처리한다. |
| `/profile/privacy` | 인증 | 공개 범위 설정 화면이다. 프로필 폼과 함께 공개 설정 값을 수정한다. |
| `/profile/account` | 인증 | 닉네임, 이메일, 비밀번호 변경과 회원 탈퇴를 처리한다. 변경 시 현재 비밀번호 확인을 받는다. |
| `/profile/works` | 인증 | 내가 참여하거나 등록한 작업물 목록을 보여주고 작업물 업로드 화면으로 이동한다. |
| `/profile/recovery` | 인증 | 프로필 삭제/복구 안내와 프로필 삭제 요청을 제공한다. |
| `/profile/portfolio` | 인증 | 내 포트폴리오 목록을 관리한다. 상세 이동, 수정, 삭제를 제공한다. |
| `/profile/portfolio/new` | 인증 | 새 포트폴리오를 등록한다. KOBIS 영화 검색, 직접 입력, YouTube 썸네일 사용, 대표 이미지 업로드를 지원한다. |
| `/profile/portfolio/:portfolioId` | 인증 | 내 포트폴리오 상세를 보여준다. 수정/삭제와 연결 정보 확인을 제공한다. |
| `/profile/portfolio/:portfolioId/edit` | 인증 | 기존 포트폴리오를 수정한다. |
| `/profile/files` | 인증 | 내 작업물 파일 저장 공간을 관리한다. 파일 업로드, 상태 필터, 삭제, 복구를 제공한다. |
| `/profile/youtube` | 인증 | YouTube URL 미리보기 후 포트폴리오 항목으로 연결한다. 등록된 YouTube 포트폴리오 목록과 연결 삭제를 제공한다. |
| `/profile/public-data` | 인증 | KOBIS 등 공공데이터 검색 결과를 포트폴리오 항목으로 추가한다. |

### 관리자

관리자 route는 모두 `requiresAdmin`이 적용된다. 화면 내부에서는 영역별 권한도 확인한다.

| 경로 | 세부 권한 | 기능 |
| --- | --- | --- |
| `/admin` | 관리자 | 관리자 대시보드다. 우선 처리 업무, 업무 메뉴, 권한 요약, 필요한 관리 영역 안내를 제공한다. |
| `/admin/users` | `USER_SANCTION` | 회원 목록을 검색/필터링하고 상세 관리로 이동한다. |
| `/admin/users/:userId` | `USER_SANCTION` | 회원 상세를 조회하고 닉네임, 연락처, 계정 유형/상태 등을 수정한다. 비활성화/복구와 제재 연결을 제공한다. |
| `/admin/users/:userId/edit` | `USER_SANCTION` | 회원 상세 수정 route다. 동일한 회원 상세/수정 패널을 편집 맥락으로 연다. |
| `/admin/posts` | `CONTENT_MODERATION` | 게시글 목록을 검색/필터링한다. |
| `/admin/posts/:postId` | `CONTENT_MODERATION` | 게시글 상세를 조회/수정하고 숨김, 삭제, 복구를 사유 입력과 함께 처리한다. |
| `/admin/teams` | `CONTENT_MODERATION` | 팀 목록을 검색/필터링한다. |
| `/admin/teams/:teamId` | `CONTENT_MODERATION` | 팀 상세를 조회/수정하고 숨김, 종료, 삭제, 복구를 처리한다. |
| `/admin/companies` | `COMPANY_APPROVAL` | 기업 승인 신청 목록을 확인한다. 제출 서류 조회/다운로드, 승인/거절 사유 입력과 확정을 제공한다. |
| `/admin/reports` | `CONTENT_MODERATION` | 콘텐츠 신고 목록을 처리한다. 신고 반려/처리 확정을 제공한다. |
| `/admin/files` | `CONTENT_MODERATION` | 작업물 파일 목록, 상태/업로더/팀 필터, 저장 용량 요약을 보여준다. 파일 보관, 복구, 삭제를 사유와 함께 처리한다. |
| `/admin/contests` | `CONTEST_MANAGE` | 공모전 관리 개요와 공모전 관리 하위 섹션 진입을 제공한다. |
| `/admin/contests/manual` | `CONTEST_MANAGE` | 관리자 수동 공모전 등록/수정 폼이다. 대표 이미지 업로드와 대상/지역/상금/일정을 입력한다. |
| `/admin/contests/crawler` | `CONTEST_MANAGE` | 콘테스트코리아 외부 공모전 크롤러를 실행한다. dry-run, 페이지 수, 최대 건수, 결과 상태 필터와 페이지네이션을 제공한다. |
| `/admin/contests/list` | `CONTEST_MANAGE` | 등록/수집 공모전 목록을 관리한다. 일괄 선택 삭제, 공모전 수정, 상태 전환을 처리한다. |
| `/admin/contests/requests` | `CONTEST_MANAGE` | 회사 공모전 개설 요청을 검토하고 승인/거절한다. 승인 시 공모전 생성 흐름과 연결된다. |
| `/admin/demo-access` | `DEMO_ACCESS_MANAGE` | 데모 접근 코드를 생성, 수정, 폐기하고 최신 코드를 복사한다. 시작/만료 시각과 최대 사용 횟수를 관리한다. |
| `/admin/notifications` | `NOTIFICATION_SEND` | 관리자 알림을 발송한다. 템플릿 적용, 대상 범위/계정/사용자 ID 지정, 수신자 미리보기, 최근 발송 배치 확인을 제공한다. |
| `/admin/ui-assets` | 관리자 | 사이드바/메뉴 이미지 자산을 업로드, 제거, 전체 초기화한다. |
| `/admin/regions` | `REGION_MANAGE` | 전국 지역 DB를 조회/필터링하고 표시명, 좌표, 활성 여부 등을 수정한다. 지역 요약 정보도 보여준다. |
| `/admin/roles` | `ADMIN_PERMISSION_MANAGE` | 관리자 세부 권한 카탈로그와 사용자별 권한을 조회하고 저장 확정한다. |
| `/admin/logs` | `LOG_VIEW` | 감사 로그와 운영 로그를 필터링해 조회한다. |
| `/admin/score-policies` | `SCORE_POLICY` | 매칭 점수 정책을 조회/수정한다. 정책 영향 미리보기, 배포, 변경 이력, 롤백을 제공한다. |

### 오류 페이지

| 경로 | 접근 | 기능 |
| --- | --- | --- |
| `/:pathMatch(.*)*` | 공개 | 등록되지 않은 경로에 대한 404 페이지다. "페이지를 찾을 수 없습니다" 안내와 홈 이동을 제공한다. |

## 구현 파일 매핑

| 기능 영역 | 주요 파일 |
| --- | --- |
| 라우트/가드 | `frontend/src/router/index.js` |
| 공통 앱 상태/레이아웃 선택 | `frontend/src/App.vue` |
| 일반 레이아웃/알림/내비게이션 | `frontend/src/layouts/AppLayout.vue` |
| 홈 | `frontend/src/views/HomeView.vue` |
| 인증/가입 | `frontend/src/views/LoginView.vue`, `RegisterView.vue`, `UserRegisterView.vue`, `CompanyRegisterView.vue`, `RegisterCompleteView.vue`, `CompanyPendingView.vue`, `DemoAccessView.vue` |
| 매칭 | `frontend/src/views/MatchingView.vue` |
| 팀 | `frontend/src/views/TeamsView.vue` |
| AI 로케이션 | `frontend/src/views/LocationExploreView.vue`, `frontend/src/components/locations/*.vue` |
| 게시판 | `frontend/src/views/BoardView.vue` |
| 공개 프로필 | `frontend/src/views/PublicProfileView.vue` |
| 공모전 | `frontend/src/views/ContestView.vue` |
| 내 정보/포트폴리오 | `frontend/src/views/ProfileView.vue` |
| 관리자 | `frontend/src/views/AdminView.vue` |
| API 클라이언트 | `frontend/src/services/api.js` |
