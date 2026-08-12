# Slate 문서 허브

## 목적

이 폴더는 `Slate` 앱 내부 작업자가 사용하는 기준 문서 공간이다. 현재 작업 루트는 `<SLATE_ROOT>`이며, 코드 기준 경로는 `backend`, `frontend`, `sql`, `assets`다. 후속 MVP 제작과 검증은 이 `docu` 폴더만 기본 참조한다.

## 먼저 읽을 문서

1. `Agent.md`
2. `docu/00_common/reference_policy.md`
3. `docu/00_common/document_structure.md`
4. `docu/03_mvp_scope/mvp_decisions.md`
5. `docu/03_mvp_scope/mvp_scope.md`
6. `docu/11_reviews/code_review_result.md`
7. `docu/handoff/mvp_documentation_handoff.md`
8. `docu/13_work_status/current_and_completed_work.md`
9. 작업 범위별 기준 문서

## 작업 범위별 기준 문서

| 범위 | 먼저 읽을 문서 |
|---|---|
| Backend | `docu/05_backend/backend_baseline.md`, `docu/11_reviews/security_environment_findings.md` |
| Frontend | `docu/06_frontend/frontend_baseline.md`, `docu/11_reviews/test_gap_report.md` |
| DB/SQL | `docu/07_database/database_baseline.md`, `docu/08_environment/env_variables.md` |
| 환경/배포 | `docu/08_environment`, `docu/09_deployment/deployment_plan.md` |
| 리뷰/검증 | `docu/11_reviews`, `docu/prompt` |
| 이전 prototype 비교 | `docu/02_prototype_comparison` |

## 주요 산출물

| 문서 | 용도 |
|---|---|
| `00_common/reference_policy.md` | 현재 루트와 참조 금지/허용 규칙 |
| `00_inventory/source_reference_map.md` | 작업자별 읽기 순서와 참조 지도 |
| `03_mvp_scope/mvp_decisions.md` | 사용자 답변 기반 확정 결정 |
| `03_mvp_scope/copy_plan_from_prototype_3.md` | `prototype_3`에서 `Slate`로 이식한 내역 |
| `08_environment/env_variables.md` | 로컬/배포 환경변수 |
| `09_deployment/deployment_plan.md` | 배포 준비 계획 |
| `11_reviews/code_review_result.md` | 코드 분석 결과와 반영 상태 |
| `13_work_status/current_and_completed_work.md` | 현재 작업 목록, 완료 목록, 문서 충돌 점검 |
| `prompt/*.md` | 역할별 새 대화창 프롬프트 |
| `work_logs/*.md` | 작업 기록 |
| `handoff/*.md` | 인수인계 |

## 현재 상태

| 항목 | 상태 |
|---|---|
| 앱 이식 | `backend`, `frontend`, `sql`, `assets` 이식 완료 |
| 최종명 변경 | `com.slate`, `slate-backend`, `slate-frontend`, DB명 `slate` 적용 |
| 주요 보안 반영 | 파일 스트림 권한, demo access gate, OpenAI fallback 반영 |
| 검증 | 실제 MySQL 8 적용 및 reset 후 schema/seed 재적용 완료, backend DB 연결·장르 API 검증 완료. 2026-06-24 TODO 잔여 작업 기준 전체 `mvn test` 96 tests 통과, 크롤러/Demo Access 선별 이식 후 targeted 128 tests 통과, 최근 프런트 변경별 `npm run build` 통과 |
| DB 제약 검증 | pending 지원·초대 generated unique 제약과 슬롯 정원 조건부 update 검증 완료 |
| 포트폴리오 | 사용자 크레딧과 KOBIS 매칭 결과 분리 저장·조회 반영. `역린`의 `이재규 + 감독` 실제 KOBIS 일치와 `VERIFIED` 확인 |
| 대표 이미지 | 프로필·팀·작업물·포트폴리오 이미지 업로드/교체/삭제 API, 권한, 파일 검증과 실제 API smoke 완료 |
| 게시판/작업물 | HOME/WORK/FREE/POPULAR, `/boards/search`, 실제 API 목록, 자유게시판 분류, 작품 유형·장르, 주간/월간/전체 랭킹, 공개 프로필 연결 반영 |
| 공모전 | 샘플 제거, 실제 OPEN 목록·마감 임박 API, 이미지 업로드, 명시 실행형 적합도, 구조화 검색 필터 반영. 콘테스트코리아 크롤러/출처/포스터 필드는 코드·SQL 반영 완료, 실제 DB migration/live crawl은 남음 |
| Demo Access | route/API gate와 DB 코드 관리 기능 구현. DB migration 적용, gate 활성화 브라우저 smoke, 보호 이미지 전체 회귀 확인은 남음 |
| 화면 검증 | 홈·팔로우·게시판·공모전·일부 매칭 화면은 데스크톱/390px 브라우저 smoke 완료. 최신 크레딧·엔티티 이미지 화면은 build/API 검증 중심으로 완료 |
| 현재 작업 추적 | `docu/13_work_status/current_and_completed_work.md`에서 완료/진행/잔여 작업과 문서 충돌을 통합 관리 |
| 남은 검증 | 크롤러/Demo Access DB migration과 smoke, 실제 동시 HTTP 요청 E2E, YouTube/OpenAI 외부 API smoke, 관리자/파일/AI/크레딧·엔티티 이미지 최신 화면 회귀 smoke |
| 남은 배포 결정 | 운영 seed 분리 |

## 참조 경로

- `docu/00_common/reference_policy.md`
- `docu/00_common/document_structure.md`
- `Agent.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/11_reviews/code_review_result.md`
- `docu/handoff/mvp_documentation_handoff.md`
- `docu/13_work_status/current_and_completed_work.md`
