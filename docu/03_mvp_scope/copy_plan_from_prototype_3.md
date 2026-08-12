# prototype_3 이식 계획과 반영 상태

## 전제

이 문서는 `../prototype_3`에서 현재 `Slate` 내부로 복사한 후보와 수정 상태를 정리한다. 최종 앱 루트는 `<SLATE_ROOT>`이며, 이후 작업자는 `../prototype_3`가 아니라 현재 `Slate` 내부 경로를 기본 기준으로 삼는다.

## 필수 이식 완료

| 원본 | 현재 경로 | 이유 | 제외/정리 |
|---|---|---|---|
| `../prototype_3/backend` | `backend` | Spring Boot API 구현 | `target`, `application-local.yml`, IDE 메타데이터 제거 |
| `../prototype_3/frontend` | `frontend` | Vue/Vite 화면 구현 | `node_modules`, `dist`, `*.log`, `.env.local` 제거 |
| `../prototype_3/sql` | `sql` | DB 생성/스키마/시드 기준 | 실제 비밀번호 포함 로컬 복사본 금지 |
| `../prototype_3/assets` | `assets` | CSV/SHP 원천 데이터 | 임시/중복 변환 산출물은 추가 검토 |
| `../prototype_3/.gitignore` | `.gitignore` 및 루트 `.gitignore` 보강 | 생성물/개인 설정 제외 규칙 | 기존 루트 규칙 유지 |

## 이식 후 수정 완료

| 항목 | 현재 기준 | 경로 |
|---|---|---|
| Maven artifact/name | `slate-backend`, `Slate Backend` | `backend/pom.xml` |
| Java package | `com.slate` | `backend/src/main/java/com/slate` |
| Spring application name | `slate-backend` | `backend/src/main/resources/application.yml` |
| Frontend package name | `slate-frontend` | `frontend/package.json` |
| DB명 | `slate` | `sql/00_create_database.sql`, `backend/src/main/resources/application-local.yml.example` |
| Demo access gate | backend filter + frontend route gate | `backend/src/main/java/com/slate/security`, `frontend/src/views/DemoAccessView.vue` |
| OpenAI fallback | key 누락/호출 실패 시 점수 기반 추천 | `backend/src/main/java/com/slate/matching/AiMatchingRecommendationService.java` |
| 파일 스트림 권한 | visibility/소유자/팀/관리자 기준 검증 | `backend/src/main/java/com/slate/boards/WorkFileService.java` |
| `.env.example` | root/backend/frontend/prod 예시 | `.env.example`, `backend/.env.example`, `frontend/.env.example`, `frontend/.env.production.example` |

## 복사 제외 확정

| 제외 | 이유 |
|---|---|
| `../prototype_3/images_page_ai` | 사용자 결정에 따라 최종 저장소에 보관하지 않음. 비교 참조만 허용 |
| `../prototype_3/uploads` | 로컬 업로드 생성물 |
| `frontend/node_modules`, `frontend/dist` | 의존성/빌드 산출물 |
| `backend/target` | Maven 빌드 산출물 |
| `backend/src/main/resources/application-local.yml` | 개인 로컬 설정 |
| `.env`, `.env.local` | 실제 환경값 포함 가능 |

## 남은 확인

| 항목 | 상태 |
|---|---|
| 실제 MySQL schema/seed 적용 | 미수행 |
| 실제 KOBIS/YouTube/OpenAI key smoke | 미수행 |
| 브라우저/모바일 smoke | 미수행 |
| 공개 회사 서류 업로드 보강 정책 | 결정 필요 |
| 배포 provider/HTTPS/CORS/log rotation | 배포 단계에서 결정 |

## 참조 경로

- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/03_mvp_scope/excluded_items.md`
- `docu/work_logs/2026-06-16_slate_initial_mvp_copy.md`
- `backend`
- `frontend`
- `sql`
- `assets`
- `../prototype_3/backend`
- `../prototype_3/frontend`
- `../prototype_3/sql`
- `../prototype_3/assets`
