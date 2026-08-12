# 환경변수 목록

## 원칙

실제 값은 문서에 남기지 않는다. 로컬과 배포는 같은 변수명을 사용하되 값은 각 환경에서 주입한다. 프론트에는 공개되어도 되는 `VITE_` 변수만 둔다.

## 백엔드

| 변수 | 필수 | 기본값/예시 | 설명 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 권장 | `local`, `prod` | 실행 profile |
| `SLATE_DB_URL` | 필수 | `jdbc:mysql://localhost:3306/slate?...` | MySQL JDBC URL |
| `SLATE_DB_USERNAME` | 필수 | `slate_app` | DB 사용자 |
| `SLATE_DB_PASSWORD` | 필수 | `CHANGE_ME` | DB 비밀번호 |
| `SLATE_JWT_SECRET` | 필수 | `CHANGE_ME_LOCAL_SECRET` | JWT 서명 secret |
| `SLATE_JWT_EXPIRATION_MINUTES` | 선택 | `120` | JWT 만료 분 |
| `SLATE_UPLOAD_DIR` | 필수 | `uploads` | 파일 업로드 저장 경로 |
| `SLATE_FFPROBE_PATH` | 선택 | `ffprobe` | 영상 길이 검증용 ffprobe 경로 |
| `SLATE_AUDIT_IP_HASH_SALT` | 필수 | `CHANGE_ME_LOCAL_AUDIT_SALT` | IP hash salt |
| `SLATE_CORS_ALLOWED_ORIGINS` | 로컬/배포별 | `http://localhost:5174,http://127.0.0.1:5174` | 허용 origin 목록 |
| `SLATE_DEMO_ACCESS_ENABLED` | 배포 데모 필수 | `false`, `true` | backend demo access filter 활성화 |
| `SLATE_DEMO_ACCESS_CODE` | 초기 배포/비상 fallback | `CHANGE_ME` | DB 접근 코드가 준비되지 않았을 때 쓰는 fallback 코드. 프론트에는 노출 금지 |
| `KOBIS_API_KEY` | 필수 | `CHANGE_ME` | KOBIS API key |
| `KOBIS_BASE_URL` | 선택 | `http://www.kobis.or.kr/kobisopenapi/webservice/rest` | KOBIS base URL |
| `CONTESTKOREA_CRAWLER_ENABLED` | 선택 | `false`, `true` | 콘테스트코리아 크롤러 실행 허용 |
| `CONTESTKOREA_BASE_URL` | 선택 | `https://www.contestkorea.com` | 콘테스트코리아 base URL |
| `CONTESTKOREA_LIST_PATH` | 선택 | `/sub/list.php` | 목록 경로 |
| `CONTESTKOREA_CATEGORY_CODE` | 선택 | `031210001` | 기본 카테고리 코드 |
| `CONTESTKOREA_INT_GBN` | 선택 | `1` | 콘테스트코리아 목록 파라미터 |
| `CONTESTKOREA_USER_AGENT` | 선택 | `SlateBot/1.0 (contact: helpdesk@slate.local)` | 크롤러 User-Agent. 운영 전 실제 연락 정책 확인 필요 |
| `CONTESTKOREA_REQUEST_DELAY_MILLIS` | 선택 | `1500` | 요청 간 지연 |
| `CONTESTKOREA_CONNECT_TIMEOUT_MILLIS` | 선택 | `5000` | 연결 timeout |
| `CONTESTKOREA_READ_TIMEOUT_MILLIS` | 선택 | `10000` | 읽기 timeout |
| `CONTESTKOREA_MAX_PAGES` | 선택 | `1` | 1회 수집 최대 페이지 |
| `CONTESTKOREA_MAX_ITEMS_PER_RUN` | 선택 | `30` | 1회 수집 최대 항목 |
| `CONTESTKOREA_POSTER_DOWNLOAD_ENABLED` | 선택 | `true` | 포스터 다운로드 여부 |
| `CONTESTKOREA_REQUIRED_PERMISSION_TEXT` | 선택 | `콘테스트코리아 출처 표기` | 출처/사용 조건 안내 문구 |
| `CONTESTKOREA_SOURCE_NAME` | 선택 | `CONTESTKOREA` | source 식별자 |
| `CONTESTKOREA_SOURCE_ATTRIBUTION` | 선택 | `출처: 콘테스트코리아` | 목록/상세 출처 표시 |
| `YOUTUBE_API_KEY` | 필수 | `CHANGE_ME` | YouTube Data API key |
| `YOUTUBE_BASE_URL` | 선택 | `https://www.googleapis.com/youtube/v3` | YouTube base URL |
| `OPENAI_API_KEY` | 필수 | `CHANGE_ME` | OpenAI API key |
| `OPENAI_BASE_URL` | 선택 | `https://api.openai.com/v1` | OpenAI API base URL |
| `OPENAI_MODEL` | 선택 | `gpt-4o-mini` | AI 매칭 추천 모델 |

## 프론트엔드

| 변수 | 필수 | 기본값/예시 | 설명 |
|---|---|---|---|
| `VITE_API_BASE_URL` | 배포 방식별 | 빈 값 | 빈 값이면 같은 origin 또는 Vite proxy `/api` 사용 |
| `VITE_DEMO_ACCESS_GATE` | 배포 데모 필수 | `false`, `true` | 프론트 접속 코드 route gate 사용 여부 |

프론트에는 `KOBIS_API_KEY`, `YOUTUBE_API_KEY`, `OPENAI_API_KEY`, `SLATE_DEMO_ACCESS_CODE`를 넣지 않는다.

## 배포 환경 추가 검토

| 변수/설정 | 필요성 |
|---|---|
| `SERVER_PORT` | Spring Boot 포트 override 필요 시 |
| `JAVA_OPTS` | 메모리/GC 설정 |
| 로그 경로 | 파일 로그를 쓸 경우 |
| 업로드 백업 경로 | 로컬 디스크 백업 정책을 둘 경우 |

## 현재 결정 반영

| 항목 | 결정 |
|---|---|
| DB | MySQL 서버 사용 |
| 파일 저장 | MVP 제작 단계에서는 로컬 파일 시스템 기준 유지 |
| 외부 API | KOBIS, YouTube, OpenAI 모두 필수 변수 |
| 콘테스트코리아 크롤러 | 기본 비활성. 운영 전 수집 범위, 출처 표기, 요청 간격, dry-run 검증 필요 |
| ffprobe | 필수화 보류. 변수는 유지하되 설정 없을 때 대체 흐름 확인 |
| HTTPS/CORS/log rotation | 보류 |

## 참조 경로

- `backend/.env.example`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-local.yml.example`
- `backend/src/main/resources/application-prod.yml`
- `frontend/.env.example`
- `frontend/.env.production.example`
- `frontend/src/services/api.js`
- `frontend/vite.config.js`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/work_logs/2026-06-24_fixer_user2_crawler_demo_access_port.md`
- `docu/13_work_status/current_and_completed_work.md`
