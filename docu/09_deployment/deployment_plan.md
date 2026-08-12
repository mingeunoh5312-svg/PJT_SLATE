# 배포 계획

## 확정 배포 기준

| 항목 | 결정 |
|---|---|
| 1차 목표 | 로컬 시연 환경 안정화 |
| 필수 배포 형태 | 프론트엔드/백엔드 분리 배포 |
| 최후순위 배포 형태 | 단일 EC2 서버 |
| DB | MySQL 서버 |
| HTTPS | 보류. 우선 로컬 실행 기준 |
| CORS | 보류. 분리 배포 설계 시 재검토 |
| 파일 저장 | MVP 제작 단계에서는 로컬 파일 시스템 기준 |
| 데모 seed | 배포 데모 포함 |
| 데모 접근 제한 | Slate 웹 페이지 접근 전 접속 코드 gate 필수 |

## 현재 배포 준비 상태

| 항목 | 상태 |
|---|---|
| 백엔드 빌드 | 2026-06-24 TODO 잔여 작업 기준 전체 `mvn test` 96 tests 통과. 크롤러/Demo Access 선별 이식 후 targeted 128 tests 통과, 전체 회귀는 재실행 필요 |
| 프론트 빌드 | `npm install`, `npm run build` 통과 |
| DB SQL | 실제 MySQL 8 적용 및 `99_reset.sql` 이후 schema/seed 재적용 완료. table 50개와 주요 seed 건수 일치 |
| DB 제약/API | pending 지원·초대 generated unique 제약, 슬롯 정원 조건부 update, backend DB 연결 및 장르 API 검증 완료 |
| 크롤러/Demo Access SQL | `15_contest_crawl_source_schema.sql`, `16_contest_official_link_cleanup.sql`, `17_demo_access_code_management_schema.sql` 작성 완료. 실제 MySQL 적용은 남음 |
| 환경변수 | `.env.example`, service별 example, `application-prod.yml` 있음 |
| prod profile | `backend/src/main/resources/application-prod.yml` 추가됨. 실제 secret은 환경변수 필요 |
| 파일 저장 | 로컬 `uploads` 기준 |
| HTTPS/domain | 보류 |
| CORS | 보류 |
| reverse proxy | 분리 배포 방식 확정 시 설계 |

## 배포 우선순위

| 우선순위 | 형태 | 기준 |
|---:|---|---|
| 1 | 로컬 시연 | `backend`, `frontend`, `sql`을 로컬에서 실행 |
| 2 | 프론트/백엔드 분리 배포 | 프론트 정적 배포와 백엔드 API 서버를 분리 |
| 3 | 단일 EC2 서버 | 최후순위. 한 서버에 프론트 정적 파일, 백엔드, MySQL 또는 외부 MySQL 연결 |

## 프론트/백엔드 분리 배포 기준안

| 구성 | 기준 |
|---|---|
| Frontend | Vite build 산출물 정적 배포 |
| Backend | Spring Boot jar 또는 컨테이너 실행 |
| API 연결 | `VITE_API_BASE_URL` 또는 reverse proxy |
| DB | MySQL 서버 |
| Upload | 백엔드 서버의 로컬 업로드 경로. 백업/용량 정책은 배포 단계에서 별도 결정 |
| Secrets | 백엔드 환경변수 또는 배포 secret |
| Demo gate | `VITE_DEMO_ACCESS_GATE=true`, `SLATE_DEMO_ACCESS_ENABLED=true`, DB 접근 코드 관리 우선. `SLATE_DEMO_ACCESS_CODE`는 초기/비상 fallback |

## 단일 EC2 배포 최후순위 가정안

| 구성 | 권장 |
|---|---|
| OS | Ubuntu 또는 Windows Server 중 사용자 선택 |
| Backend | Spring Boot jar, systemd 또는 서비스 등록 |
| Frontend | Vite build 산출물을 Nginx/Apache에서 정적 서빙 |
| API proxy | `/api`를 backend `localhost:8080`으로 reverse proxy |
| DB | 같은 서버 MySQL 또는 외부 MySQL |
| Upload | 서버 내 `/var/slate/uploads` 같은 고정 경로 |
| Secrets | OS 환경변수 또는 배포 secret 관리 |
| Logs | backend logs, web server logs, DB backup 분리 |

## 배포 전 필수 결정

| 결정 | 이유 |
|---|---|
| 배포 provider | 프론트/백엔드 분리 배포 대상 서비스 선택 |
| 데모 접속 코드 실제값 | 배포 secret으로 관리해야 함 |
| DB 계정/권한 | MySQL 서버 계정, schema 생성 권한, 백업 권한 |
| 외부 API key | KOBIS/YouTube/OpenAI key, 비용, quota, fallback |
| 파일 저장 백업 | 로컬 디스크 백업/복구/용량 정책 |
| 운영 seed 분리 | 데모 seed와 운영 seed 분리 |
| CORS/proxy | 분리 배포 방식에 따라 재검토 |
| HTTPS | 현재 보류. 실제 공개 배포 전 재검토 |

## 배포 체크리스트

| 단계 | 확인 |
|---|---|
| 1 | `backend/.env` 또는 실행 환경변수 준비 |
| 2 | `frontend/.env.production` 또는 build-time `VITE_API_BASE_URL` 결정 |
| 3 | DB schema/seed 적용 |
| 4 | `mvn clean test` |
| 5 | `npm run build` |
| 6 | 백엔드 health/API smoke |
| 7 | 프론트 route 새로고침/직접 접근 smoke |
| 8 | 로그인/관리자/파일 업로드 smoke |
| 9 | KOBIS/YouTube/OpenAI 실제 key와 fallback smoke |
| 10 | Demo Access DB 코드 생성/수정/폐기와 `X-Slate-Demo-Code` API 차단 smoke |
| 11 | 콘테스트코리아 크롤러 dry-run, 출처/포스터/수집일 표시 smoke |
| 12 | 로그/업로드/DB 백업 경로 확인 |

로컬 통합 환경에서는 기본 체크리스트 3단계와 backend DB 연결/API smoke를 완료했다. 크롤러/Demo Access 추가 SQL 15~17은 실제 DB 적용이 남아 있다. 실제 배포 대상 DB에는 배포 시 다시 적용하며, 슬롯 정원 초과 방어의 실제 동시 HTTP 요청 E2E는 별도 검증이 남아 있다. 외부 API key 및 최신 브라우저 smoke도 아직 수행하지 않았다.

## 배포 리스크

| 리스크 | 대응 |
|---|---|
| 실제 secret 유출 | `.env`, `application-local.yml`, `frontend/.env.production` 커밋 금지 |
| 업로드 파일 누락 | 배포/재시작/백업 정책 확정 |
| 대용량 업로드 | 웹 서버와 Spring multipart limit 동시 설정 |
| 외부 API quota | 실패 fallback과 rate limit |
| SPA route 새로고침 404 | web server fallback to `index.html` 설정 |
| CORS 오류 | 현재 보류. 분리 배포 시 same-origin proxy 또는 allowed origin 환경변수화 |
| 데모 seed 노출 | 접속 코드 gate 없이는 배포 데모 공개 금지. DB 코드 관리 migration 적용 전에는 fallback 코드 의존을 짧게만 허용 |
| 크롤러 외부 사이트 의존 | 운영 전 수집 범위, 요청 간격, 출처 표기, dry-run 로그를 확인 |

## 참조 경로

- `docu/08_environment/env_variables.md`
- `docu/08_environment/env_example_policy.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/work_logs/2026-06-18_db_mysql_preflight.md`
- `docu/work_logs/2026-06-24_fixer_user2_crawler_demo_access_port.md`
- `docu/13_work_status/current_and_completed_work.md`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-local.yml.example`
- `backend/src/main/resources/application-prod.yml`
- `frontend/package.json`
- `frontend/vite.config.js`
- `frontend/.env.production.example`
- `sql`
- `.gitignore`
