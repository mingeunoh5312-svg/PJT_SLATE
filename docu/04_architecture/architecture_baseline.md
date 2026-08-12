# 아키텍처 기준

## 현재 구조

| 계층 | 기준 |
|---|---|
| Frontend | Vue 3 + Vite + Vue Router |
| Backend | Spring Boot 4 + Spring MVC + Spring Security JWT + MyBatis |
| Database | MySQL 8 |
| File storage | 로컬 파일 시스템 `SLATE_UPLOAD_DIR` |
| External APIs | KOBIS, YouTube Data API, OpenAI API |

## 최종 앱 루트와 이름 기준

| 항목 | 현재 기준 |
|---|---|
| 최종 앱 루트 | `<SLATE_ROOT>` |
| Backend | `backend` |
| Frontend | `frontend` |
| SQL | `sql` |
| Assets | `assets` |
| Java package | `com.slate` |
| DB명 | `slate` |

## 런타임 관계

```text
Browser
  -> Vite dev server or static frontend
  -> /api proxy or API base URL
  -> Spring Boot backend
  -> MySQL 8
  -> local upload directory
  -> KOBIS / YouTube / OpenAI
```

## 백엔드 패키지

| 패키지 | 역할 |
|---|---|
| `accounts` | 인증, 계정, 회사 승인, 회사 서류 |
| `admin` | 관리자 세부 권한 |
| `boards` | 게시판, 리뷰, 작업물, 파일, YouTube |
| `common` | 공통 응답, 예외, Jackson 설정 |
| `contests` | 공모전 |
| `matching` | 매칭, 점수 정책, OpenAI AI 추천 |
| `moderation` | 신고, 제재 |
| `notifications` | 내부 알림 |
| `operations` | 감사/운영 로그 |
| `profiles` | 프로필, 포트폴리오, KOBIS 검증 |
| `references` | 기준 데이터 |
| `security` | JWT, 인증 필터, demo access filter |
| `teams` | 팀, 모집, 멤버, 계획 |

## 프론트 구조

| 경로 | 역할 |
|---|---|
| `frontend/src/router/index.js` | 전체 route, demo/auth/admin guard |
| `frontend/src/layouts` | App/Auth/Admin layout |
| `frontend/src/views` | 주요 도메인 화면 |
| `frontend/src/components/auth` | 회원가입 공통 컴포넌트 |
| `frontend/src/services/api.js` | API client, token/demo code 저장, endpoint wrapper |
| `frontend/src/assets` | 화면 fallback 이미지 |
| `frontend/src/styles/slate.css` | 전체 스타일 |

## 주요 아키텍처 리스크

| 리스크 | 처리 방향 |
|---|---|
| 외부 API key 미설정/쿼터 초과 | KOBIS/YouTube/OpenAI 필수 기능이므로 key, 실패 UI, 서버 fallback 검증 필요 |
| DB migration 도구 없음 | SQL 수동 적용 또는 Flyway/Liquibase 도입 여부 결정 |
| 프론트 API base | 로컬은 Vite proxy, 분리 배포는 reverse proxy 또는 `VITE_API_BASE_URL` 결정 |
| 데모 seed 노출 | 배포 데모에서는 frontend gate와 backend filter 모두 활성화 |
| 파일 저장 로컬 의존 | MVP 제작 단계에서는 로컬 유지. 배포 저장소/백업/삭제 정책은 후순위 |
| 공개 회사 서류 업로드 | 1회성 token/rate limit 정책 결정 필요 |
| `images_page_ai` 보관 | 참조 전용으로만 사용하고 최종 저장소에서는 제외 |

## 참조 경로

- `backend/pom.xml`
- `backend/src/main/java/com/slate`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-prod.yml`
- `frontend/package.json`
- `frontend/vite.config.js`
- `frontend/src/router/index.js`
- `frontend/src/services/api.js`
- `sql/01_schema.sql`
- `docu/03_mvp_scope/mvp_decisions.md`
