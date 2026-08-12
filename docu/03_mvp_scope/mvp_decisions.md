# MVP 제작 결정사항

## 기준

이 문서는 `questions_before_mvp.md`에 대한 사용자 답변과 2026-06-16 `Slate` 이식 결과를 현재 작업 기준으로 확정한 기록이다. 현재 앱 루트는 `<SLATE_ROOT>`이며, `<SLATE_ROOT>`는 각 작업자의 로컬 `Project_Slate/Slate` 폴더를 뜻한다.

## 필수 결정

| 항목 | 결정 | 현재 반영 |
|---|---|---|
| 최종 앱 루트 | `Project_Slate/Slate` 하위 | `backend`, `frontend`, `sql`, `assets`, `docu` 기준 |
| 최종명 변경 | `prototype2`, `prototype_2`, `prototype_3`, `slate_prototype2` 계열 이름 변경 | `com.slate`, `slate-backend`, `slate-frontend`, DB명 `slate` 적용 |
| KOBIS | 필수 기능 | backend 환경변수와 fallback/테스트 유지 |
| YouTube Data API | 실제 운영 기능 | backend 환경변수와 metadata 기능 유지 |
| OpenAI AI 매칭 | 필수 기능 | key 누락/호출 실패 시 점수 기반 추천 fallback 반영 |
| 파일 저장 | 우선 로컬 환경만 고려 | `SLATE_UPLOAD_DIR` 기준 |
| 파일 물리 삭제/고아 파일 정리 | 추가 기능 | 현재 필수 범위 제외 |
| 테스트 계정/샘플 seed | 배포 데모 포함 | 배포 시 접속 코드 gate 필수, 현재 gate 구현 반영 |
| 회사 승인/관리자/제재 정책 | 유지 | 최종 단계 재검토 가능 |
| 공모전 제출 | 이메일 제출 안내 유지 | 사이트 내 제출/심사는 제외 |
| 배포 목표 | 로컬 시연, 프론트/백엔드 분리 배포 필수. EC2 단일 서버 최후순위 | 배포 문서 기준 |
| `.env.example` 위치 | 실제 서비스에 가까운 방식 | `.env.example`, `backend/.env.example`, `frontend/.env.example`, `frontend/.env.production.example` |
| `images_page_ai` | 최종 저장소 미보관 | `Slate` 하위 복사 제외 |
| 원천 데이터 자산 | 포함 | `assets`에 이식 |
| 남은 브라우저 검증 | MVP 제작 단계 초기 또는 후기 | 미수행. smoke 계획에 유지 |

## 기능 범위 결정

| 영역 | 결정 |
|---|---|
| 인증 부가 기능 | 이메일 인증, 비밀번호 재설정, 소셜 로그인은 추가 기능 |
| KOBIS 검증 실패 포트폴리오 | 다른 작품과 동일한 일반 표기 |
| 팀 정책 | 현재 구현 유지. 최종 단계에서 재검토 가능 |
| AI 매칭 실패 | 기존 점수 기반 추천 출력 |
| 게시판 공개 범위 | 현재 구현 유지 |
| 관리자 제한 정책 | 현재 구현 유지 |
| 로그 보관/개인정보 정책 | 보류 |

## 최종명 적용 상태

| 항목 | 현재 기준 |
|---|---|
| Java package | `com.slate` |
| Maven artifactId | `slate-backend` |
| Spring application name | `slate-backend` |
| Frontend package name | `slate-frontend` |
| DB name | `slate` |
| 문서/표시명 | `Slate` |

## 배포/환경 결정

| 영역 | 결정 |
|---|---|
| DB | MySQL 서버 |
| HTTPS | 보류. 로컬 실행 우선 |
| CORS | 보류. 현재 로컬 allowed origins 예시는 유지 |
| 업로드 제한 | 현재 300MB, 사용자 1GB, 팀 2GB 유지 |
| ffprobe | 보류. 선택 설정으로 유지 |
| 로그 파일/rotation | 보류 |

## 데모 접속 코드 기준

배포 데모에는 테스트 계정과 샘플 seed를 포함하되, Slate 웹 페이지 접근 전에 접속 코드를 입력해야 한다. 현재 `frontend` route gate, `backend` filter, DB 관리형 접근 코드 기능은 구현되어 있으나, 실제 DB migration 적용과 gate 활성화 smoke는 배포 단계에서 확인한다.

| 항목 | 기준 |
|---|---|
| Backend 설정 | `SLATE_DEMO_ACCESS_ENABLED`, `SLATE_DEMO_ACCESS_CODE` |
| Backend DB 코드 | `demo_access_code`, `/api/admin/demo-access/codes` |
| Frontend 설정 | `VITE_DEMO_ACCESS_GATE` |
| Frontend 화면 | `frontend/src/views/DemoAccessView.vue` |
| Backend filter | `backend/src/main/java/com/slate/security/DemoAccessFilter.java` |
| 남은 확인 | `sql/17_demo_access_code_management_schema.sql` 실제 적용, 배포 환경에서 직접 URL 접근/API 직접 호출 smoke |

## 참조 경로

- `docu/03_mvp_scope/questions_before_mvp.md`
- `docu/03_mvp_scope/mvp_scope.md`
- `docu/03_mvp_scope/copy_plan_from_prototype_3.md`
- `docu/08_environment/env_variables.md`
- `docu/09_deployment/deployment_plan.md`
- `backend/pom.xml`
- `frontend/package.json`
- `sql/00_create_database.sql`
- `docu/13_work_status/current_and_completed_work.md`
