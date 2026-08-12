# Slate API 레퍼런스

모든 엔드포인트는 `/api` 프리픽스를 가진다.
인증은 `Authorization: Bearer <JWT>` 헤더, 데모 게이트는 `X-Slate-Demo-Code` 헤더를 사용한다.
응답 봉투: `{ success, message, data }`.

---

## 계정 · 인증 (`accounts`)

### `AuthController` — `/api/auth`
| Method | Path | 설명 |
|---|---|---|
| POST | `/register` | 사용자/회사 가입 |
| POST | `/login` | JWT 로그인 |
| GET | `/me` | 현재 계정 조회 |
| PATCH | `/me` | 계정 수정 |
| DELETE | `/me` | 회원 탈퇴 |

### `CompanyDocumentController` — 회사 서류
| Method | Path | 설명 |
|---|---|---|
| POST | `/api/auth/company-applications/{applicationId}/documents` | 가입 시 서류 업로드 |
| GET / POST | `/api/company/application/documents` | 본인 서류 조회/업로드 |
| DELETE | `/api/company/application/documents/{documentId}` | 본인 서류 삭제 |
| GET | `/api/admin/company-applications/{applicationId}/documents` | (관리자) 서류 목록 |
| GET | `/api/admin/company-applications/documents/{documentId}/download` | (관리자) 서류 다운로드 |

### `AdminAccountController` — `/api/admin/company-applications`
| Method | Path | 설명 |
|---|---|---|
| GET | `` | 회사 신청 목록 |
| POST | `/{applicationId}/decision` | 회사 승인/거절 |

### `AdminUserController` — `/api/admin/users`
| Method | Path | 설명 |
|---|---|---|
| GET | `` / `/{userId}` | 사용자 목록/상세 |
| PUT | `/{userId}` | 사용자 수정 |
| POST | `/{userId}/deactivate` · `/{userId}/restore` | 비활성/복구 |

## 프로필 · 포트폴리오 (`profiles`)

### `ProfileController` — `/api/profiles`
| Method | Path | 설명 |
|---|---|---|
| GET | `/me` · `/{profileId}` · `/public/{profileId}` | 내/특정/공개 프로필 |
| PUT | `/{profileId}` | 프로필 수정 |
| DELETE | `/me` | 프로필 소프트 삭제 |
| GET / POST | `/me/portfolio-items` | 포트폴리오 조회/등록 |
| PUT / DELETE | `/me/portfolio-items/{id}` | 포트폴리오 수정/삭제 |
| GET | `/public-data/search` | 공공데이터 검색 |
| GET | `/public-data/kobis/movies` | KOBIS 영화 검색 |
| POST | `/me/portfolio-items/from-public-data` | 공공데이터로 포트폴리오 추가 |

## 팔로우 (`follows`)

### `FollowController` — `/api/profiles`
| Method | Path | 설명 |
|---|---|---|
| POST / DELETE | `/{profileId}/follow` | 팔로우/언팔로우 |
| GET | `/{profileId}/follow-status` · `/followers` · `/following` | 상태·팔로워·팔로잉 |

## 팀 · 모집 (`teams`)

### `TeamController` — `/api/teams`
| Method | Path | 설명 |
|---|---|---|
| GET | `/mine` · `/{teamId}` | 내 팀 / 팀 상세 |
| PUT / DELETE | `/{teamId}` | 팀 수정/삭제 |
| GET / POST | `/{teamId}/recruitments` | 모집 조회/생성 |
| PUT / DELETE | `/recruitments/{recruitmentId}` | 모집 수정/삭제 |
| POST | `/recruitments/{recruitmentId}/slots` | 슬롯 추가 |
| PUT / DELETE | `/recruitment-slots/{slotId}` | 슬롯 수정/삭제 |
| GET | `/{teamId}/applications` | 지원 목록 |
| POST | `/applications/{applicationId}/decision` | 지원 결정 |
| GET | `/{teamId}/invitations` · `/invitations/mine` | 초대 목록 |
| POST | `/invitations/{id}/decision` | 초대 결정 |
| PUT | `/{teamId}/members/{memberUserId}` | 멤버 역할 변경 |
| POST | `/{teamId}/leave` · `/transfer-leader` · `/close` · `/reopen` | 탈퇴·양도·종료·재개 |
| GET | `/{teamId}/closure-snapshots` | 종료 스냅샷 |
| GET / POST | `/{teamId}/plans` | 팀 일정 조회/생성 |
| PUT | `/plans/{planItemId}` | 일정 수정 |
| PATCH | `/plans/{planItemId}/status` | 일정 상태 변경 |

### `AdminTeamController` — `/api/admin/teams`
목록·상세·수정, `hide`/`close`/`restore`, 삭제.

## 매칭 (`matching`)

### `MatchingController` — `/api/matching`
| Method | Path | 설명 |
|---|---|---|
| GET | `/team-to-members` · `/member-to-teams` | 필터 기반 추천 |
| GET | `/policies/active` | 활성 점수 정책 |
| POST | `/ai/recommendations` | OpenAI AI 추천 |
| POST / GET | `/bookmarks` | 북마크 저장/조회 |
| DELETE | `/bookmarks/{targetType}/{targetId}` | 북마크 삭제 |
| POST / GET / DELETE | `/invitations`, `/applications` | 초대·지원 관리 |

### `ScorePolicyAdminController` — `/api/admin/matching/policies`
`/active`(조회·수정), `/preview`, `/{policyId}/rollback`, `/history`.

## 게시판 · 작업물 (`boards`)

### `BoardController` — `/api/boards`
| Method | Path | 설명 |
|---|---|---|
| GET | `/posts` · `/posts/my-works` · `/posts/{postId}` | 목록·내 작업물·상세 |
| POST / PUT / DELETE | `/posts` · `/posts/{postId}` | 글 작성/수정/삭제 |
| GET / POST | `/posts/{postId}/reviews` | 리뷰 조회/작성 |
| PUT / DELETE | `/reviews/{reviewId}` | 리뷰 수정/삭제 |
| POST | `/posts/{postId}/likes/toggle` | 좋아요 토글 |
| POST / GET / DELETE | `/work-files`, `/work-files/mine`, `/work-files/{fileId}` | 작업 파일 |
| POST | `/work-files/{fileId}/restore` | 파일 복구 |
| GET | `/work-files/{fileId}/stream` | 파일 스트리밍 |
| POST | `/youtube/preview` | YouTube 메타데이터 |
| — | `/team-work-requests/*` | 팀 작업물 크레딧 승인 흐름 |
| POST | `/posts/{postId}/reports` · `/reviews/{reviewId}/reports` | 신고 |
| GET | `/rankings` | 랭킹(인기 작업물 등) |

### 관리자
`AdminBoardController` — `/api/admin/boards/posts` (목록·상세·수정·hide/restore·삭제)
`AdminWorkFileController` — `/api/admin/work-files` (목록·용량 요약·hold/restore·삭제)

## 공모전 (`contests`)

### `ContestController` — `/api/contests`
| Method | Path | 설명 |
|---|---|---|
| GET | `` · `/urgent` · `/{contestId}` · `/bases` | 목록·마감임박·상세·기준 |
| POST | `/{contestId}/save/toggle` | 저장 토글 |
| POST | `/{contestId}/fit` | 적합도 분석 |
| POST | `/{contestId}/prepare` | 제출 준비 |
| GET / POST | `/open-requests/mine` · `/open-requests` | 회사 오픈 신청 |
| GET / PUT / POST | `/manage/*` | 회사 공모전 관리 |

### `AdminContestController` — `/api/admin/contests`
목록·생성·일괄삭제, `/requests`, 수정·상태변경, 신청 결정,
`POST /crawl-sources/contest-korea/run` (jsoup 크롤러 실행).

## 촬영지 (`locations`)

### `LocationController`
| Method | Path | 설명 |
|---|---|---|
| POST | `/api/locations/ai/recommendations` | AI 촬영지 추천 |
| GET | `/api/locations/sessions/{sessionId}` | 추천 세션 |
| POST / GET | `/api/locations/candidates` | 개인 후보 저장/조회 |
| GET | `/api/teams/{teamId}/locations` | 팀 저장 후보 |

## 미디어 (`media`)

### `MediaImageController` — `/api/media/images`
`POST / DELETE / GET /{entityType}/{entityId}` — 엔티티 이미지 업로드/삭제/스트리밍.

## 알림 (`notifications`)

### `NotificationController` — `/api/notifications`
| Method | Path | 설명 |
|---|---|---|
| GET | `` · `/unread-count` | 목록·미읽음 수 |
| PATCH | `/{id}/read` · `/read-all` · `/{id}/hide` | 읽음·전체읽음·숨김 |
| POST / GET | `/admin/send` · `/admin/templates` · `/admin/recipients/preview` · `/admin/batches` | (관리자) 발송·템플릿·미리보기·배치 |

## 참조 데이터 (`references`)

`ReferenceController` — `/api/references`: `/codes` · `/regions` · `/roles` · `/genres`
`AdminRegionController` — `/api/admin/regions`: 목록·요약·수정

## 관리자 · 권한

`AdminPermissionController` — `/api/admin/permissions`: `/catalog` · `/me` · `/users`, 사용자 권한 수정

## 신고 · 제재 (`moderation`)

### `ModerationController` — `/api/admin/moderation`
신고 목록·결정, 사용자 목록, 제재 조회·부여·해제.

## 운영 로그 (`operations`)

`AdminLogController` — `/api/admin/logs`: `/audit` · `/operations`

## 데모 접근 (`security`)

`DemoAccessController` — `/api/demo/access` (접근 코드 확인)
`DemoAccessAdminController` — `/api/admin/demo-access/codes` (코드 목록·생성·수정·해제)
