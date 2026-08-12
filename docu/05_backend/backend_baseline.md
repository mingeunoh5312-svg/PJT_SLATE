# 백엔드 기준

## 기술 스택

| 항목 | 기준 |
|---|---|
| Java | 17 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring MVC |
| Security | Spring Security + JWT |
| Persistence | MyBatis |
| DB Driver | MySQL Connector/J |
| Build | Maven |

## 애플리케이션 기준

| 항목 | 현재 값 |
|---|---|
| artifactId | `slate-backend` |
| application name | `slate-backend` |
| main class | `com.slate.SlateApplication` |
| package root | `com.slate` |
| server port | `8080` |
| default profile | `local` |
| prod profile | `application-prod.yml` 존재. 실제 값은 환경변수로 주입 |

## API 그룹

| 그룹 | 경로 |
|---|---|
| 인증 | `/api/auth` |
| demo access | `/api/demo/access`, `/api/admin/demo-access/codes` |
| 회사 서류 | `/api/auth/company-applications/*`, `/api/company/application/documents`, `/api/admin/company-applications/*/documents` |
| 기준 데이터 | `/api/references` |
| 프로필 | `/api/profiles` |
| 공개 프로필 | `GET /api/profiles/public/{profileId}` |
| 엔티티 이미지 | `/api/media/images/{entityType}/{entityId}` |
| 팔로우 | `/api/profiles/{profileId}/follow`, `/follow-status`, `/followers`, `/following` |
| 팀 | `/api/teams` |
| 매칭 | `/api/matching`, `/api/matching/bookmarks`, `/api/admin/matching/policies` |
| 게시판/작업물 | `/api/boards`, `/api/admin/boards/posts`, `/api/admin/work-files` |
| 공모전 | `/api/contests`, `/api/admin/contests`, `/api/admin/contests/crawl-sources/contest-korea/run` |
| 알림 | `/api/notifications` |
| 관리자 권한 | `/api/admin/permissions` |
| 신고/제재 | `/api/admin/moderation` |
| 로그 | `/api/admin/logs` |

## 외부 API

| API | 서버 설정 | 프론트 노출 |
|---|---|---|
| KOBIS | `KOBIS_API_KEY`, `KOBIS_BASE_URL` | 금지 |
| YouTube | `YOUTUBE_API_KEY`, `YOUTUBE_BASE_URL` | 금지 |
| OpenAI | `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL` | 금지 |

## 테스트 상태

| 범위 | 확인 |
|---|---|
| KOBIS role matcher | 테스트 파일 존재 |
| YouTube properties/client/preview/update/delete/search | 테스트 파일 존재 |
| OpenAI properties/AI recommendation service | 테스트 파일 존재 |
| 파일 스트림 권한 | `WorkFileServiceStreamAuthorizationTest` 추가 |
| 팔로우 | 등록·취소·상태·목록 service 테스트 및 실제 MySQL transaction 검증 완료 |
| 매칭 저장 팀 | 저장 목록·중복 저장·소유자 한정 취소 controller/service/mapper 계약 테스트 완료 |
| 포트폴리오 크레딧 | schema/service/verification/mapper 계약 테스트 완료 |
| 엔티티 이미지 | JPEG/PNG/WebP, 크기·시그니처·소유권·교체/삭제 정합성 테스트 완료 |
| 게시판 통합 | 검색·정렬·분류, 공지 권한, 좋아요 recount, 랭킹 SQL, 공개 프로필 계약 테스트 완료 |
| 공모전 | 이미지 저장 경로, 긴급 목록, fit 수동 실행, 구조화 검색 필터, 회사 요청/관리자 수정 계약 테스트 완료 |
| 크롤러/Demo Access | `ContestKorea*Test`, `AdminContestKoreaCrawlerServiceTest`, `DemoAccess*Test`, `SecurityConfigTest` targeted 128 tests 통과 |
| 전체 테스트 | 2026-06-24 TODO 잔여 작업 기준 `mvn test` 96 tests 통과. 크롤러/Demo Access 선별 이식 후 전체 `mvn test`는 별도 재실행 필요 |

## 최근 확정 API 동작

| 기능 | 현재 계약 |
|---|---|
| 팔로우 등록/취소 | 활성 USER와 공개 활성 프로필만 대상. 중복 등록과 이미 취소된 요청은 멱등 처리 |
| 팔로우 목록 | `limit` 1~50, `offset` 0 이상. 개인정보 컬럼 없이 안정 정렬하며 `hasMore` 반환 |
| 팔로우 알림/감사 | 신규 등록에만 SOCIAL 알림과 생성 감사, 실제 삭제에만 삭제 감사 기록 |
| 저장 팀 목록 | `GET /api/matching/bookmarks?targetType=TEAM`은 현재 사용자 소유 저장 팀과 OPEN 역할을 반환 |
| 저장 취소 | `DELETE /api/matching/bookmarks/TEAM/{teamId}`는 사용자·타입·대상을 모두 조건으로 삭제 |
| 중복 저장 | `POST /api/matching/bookmarks`는 신규와 중복을 구분하고 중복 시 액션·감사 로그를 추가하지 않음 |
| 포트폴리오 크레딧 | `creditName`은 사용자 입력으로 `portfolio_item`에 보존하고, `providerPersonName`·`providerRoleName`은 KOBIS 실제 매칭 결과로 분리 반환 |
| KOBIS 검증 | `VERIFIED`, `NOT_VERIFIED`, `AMBIGUOUS`, `ERROR`를 구분하며 검증 실패가 포트폴리오 저장을 막지 않음 |
| 엔티티 이미지 | `PROFILE`, `TEAM`, `WORK`, `PORTFOLIO`, `CONTEST`, `CONTEST_REQUEST`의 등록·교체·삭제·스트리밍 지원. 최대 5MB JPEG/PNG/WebP 및 소유권 검증 |
| 공모전 공개 목록 | 기본 `OPEN`, 마감일 오름차순이며 fit cache를 조인하거나 점수를 자동 계산하지 않는다. `GET /api/contests/urgent`는 미래의 OPEN 공모전만 마감일·ID 순으로 최대 5건 반환한다. |
| 공모전 요청 이미지 | 회사가 요청 소유자로서 업로드한다. 승인 시 저장 경로를 생성 공모전으로 이전하고 요청 참조만 지우며, 거절 시 요청 참조를 지운 뒤 commit 후 파일을 삭제한다. |
| 공모전 적합도 | 기본 상세 GET에는 점수를 포함하지 않는다. 사용자 기준 검증 후 `POST /api/contests/{contestId}/fit`을 명시적으로 호출한 응답만 화면에 노출한다. |
| 공모전 구조화 검색 | `target`, `region`, `organizerType`은 그룹 내 OR·그룹 간 AND로 검색한다. 총상금/1등 상금 범위를 지원하며 허용 코드와 금액 범위를 service에서 검증한다. |
| 콘테스트코리아 크롤러 | 관리자 실행 API로 크롤링·파싱·정규화·포스터 저장·upsert를 수행한다. 실제 live run은 아직 검증하지 않았다. |
| Demo Access 코드 관리 | 환경변수 fallback 코드와 DB 발급 코드를 함께 검증한다. 관리자 코드는 해시/fingerprint로 저장하고 평문은 생성 응답에서만 1회 노출한다. |
| 게시판 목록 | `category`, `keyword`, `sort`, `freeCategory`, `workType`, `genreId`를 검증하며 범위를 넘는 필터와 미지원 값은 거부 |
| 작업물 장르/랭킹 | `work_genre`를 생성·수정·팀 승인 공개 흐름에서 보존하고, 주간 7일·월간 30일·전체 작업물 순위를 좋아요/최신일/post ID 순으로 제공 |
| 인기 순위 | 공개 WORK를 `like_count DESC, created_at DESC, post_id DESC`로 정렬하고 공개 프로필은 활성 공개 팔로워 수로 정렬 |

## 코드 리뷰 포인트

| 항목 | 이유 |
|---|---|
| 공개 회사 서류 업로드 | 1회성 token/rate limit 없이 공개 endpoint가 유지되는지 확인 |
| 외부 API fallback | KOBIS/YouTube/OpenAI 필수 기능의 key 미설정/쿼터 초과/네트워크 실패 처리 확인 |
| 파일 업로드 | content type, 크기, 경로 traversal 검토. 물리 삭제/고아 파일 정리는 추가 기능 |
| 감사 로그 | 원본 IP 미저장, hash salt 관리 확인 |
| DB transaction | 지원/초대/좋아요/조회수 동시성 검토 |
| 데모 접근 gate | `SLATE_DEMO_ACCESS_ENABLED=true` 배포에서 API 직접 호출 차단 확인 |
| prod 실행 | 실제 secret 누락 시 빠르게 실패하는지 확인 |

## 참조 경로

- `backend/pom.xml`
- `backend/src/main/java/com/slate`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-local.yml.example`
- `backend/src/main/resources/application-prod.yml`
- `backend/src/main/resources/mappers`
- `backend/src/test`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/11_reviews/code_review_result.md`
- `docu/work_logs/2026-06-18_backend_follow.md`
- `docu/work_logs/2026-06-21_fixer_matching_saved_teams.md`
- `docu/work_logs/2026-06-22_fixer_portfolio_credit_roundtrip.md`
- `docu/work_logs/2026-06-22_fixer_profile_media_and_portfolio_display.md`
- `docu/work_logs/2026-06-22_fixer_board_full_integration.md`
- `docu/work_logs/2026-06-22_fixer_board_search_ui_period_ranking_followup.md`
- `docu/work_logs/2026-06-23_fixer_contest_data_ui_image_fit.md`
- `docu/work_logs/2026-06-23_fixer_contest_structured_search_filters.md`
- `docu/work_logs/2026-06-24_fixer_user2_crawler_demo_access_port.md`
- `docu/13_work_status/current_and_completed_work.md`
