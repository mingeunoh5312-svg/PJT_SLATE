# 코드 분석 프롬프트

## 사용 목적

새 대화창에서 현재 `Slate` 코드를 분석하고, MVP 제작/로컬 시연/분리 배포 전 위험과 수정 필요 지점을 점검하기 위한 프롬프트다. 이 프롬프트는 `../prototype_3`가 아니라 현재 `Slate` 내부 구현을 기준으로 사용한다.

## 프롬프트

```text
당신은 Slate 프로젝트의 코드 분석 담당자입니다.

작업 루트:
- 각자의 로컬 Project_Slate/Slate 폴더를 <SLATE_ROOT>로 둡니다.

먼저 반드시 읽을 문서:
1. Agent.md
2. docu/README.md
3. docu/00_common/reference_policy.md
4. docu/00_common/document_structure.md
5. docu/00_inventory/source_reference_map.md
6. docu/03_mvp_scope/mvp_decisions.md
7. docu/03_mvp_scope/mvp_scope.md
8. docu/04_architecture/architecture_baseline.md
9. docu/05_backend/backend_baseline.md
10. docu/06_frontend/frontend_baseline.md
11. docu/07_database/database_baseline.md
12. docu/08_environment/env_variables.md
13. docu/09_deployment/deployment_plan.md
14. docu/11_reviews/review_plan.md

분석 대상:
- backend
- frontend
- sql
- assets
- .env.example
- backend/.env.example
- frontend/.env.example
- frontend/.env.production.example
- backend/src/main/resources/application-*.yml

참조 원칙:
- 현재 구현 기준은 Slate 내부 경로입니다.
- ../prototype_3, ../prototype_2, ../prototype 원본은 차이 확인이 필요할 때만 읽기 전용으로 참조합니다.
- ../docu는 이전 준비 문서 원본이므로 기본 참조하지 않습니다.
- 실제 API key, DB password, JWT secret, .env 값은 찾더라도 출력하지 않습니다.
- 어떤 문서를 참조하면 결과에 참조 경로를 남깁니다.
- 구현된 것, 문서에만 있는 것, 미구현/부분 구현을 분리합니다.

분석 기준:
- KOBIS, YouTube Data API, OpenAI AI 매칭은 필수 기능입니다.
- DB명은 slate, package는 com.slate, backend는 slate-backend, frontend는 slate-frontend 기준입니다.
- 배포 데모에는 접속 코드 gate가 필요합니다.
- 로컬 시연과 프론트/백엔드 분리 배포를 필수 기준으로 분석하고, EC2 단일 서버는 최후순위로 둡니다.
- images_page_ai는 최종 저장소 보관 대상이 아닙니다.

해야 할 일:
1. backend 구조를 분석합니다.
   - Spring Boot 설정, Security/JWT, DemoAccessFilter, 권한 검사, controller/service/mapper 경계
   - KOBIS, YouTube, OpenAI client의 key 관리와 fallback
   - 파일 업로드, ffprobe, 저장 경로, 다운로드/스트리밍 권한
   - 공개 회사 서류 업로드의 token/rate limit 필요성
   - 감사/운영 로그, IP hash, 개인정보 노출 위험
   - 테스트 커버리지와 누락된 테스트

2. frontend 구조를 분석합니다.
   - Vue Router demo/auth/admin guard와 layout
   - VITE_API_BASE_URL, VITE_DEMO_ACCESS_GATE, Vite proxy 정책
   - token/demo code localStorage 사용, 인증 만료/에러 처리
   - 외부 API key가 프론트에 노출되지 않는지
   - route 새로고침, 직접 접근, 모바일 overflow 위험

3. SQL을 분석합니다.
   - 00_create_database.sql, 01_schema.sql, seed/reset 순서
   - 04_youtube_metadata_schema.sql의 마이그레이션 안전성
   - 05_seed_ai_matching_dummy_data.sql의 운영 포함 여부
   - pending 지원/초대 중복 방어와 슬롯 정원 동시성
   - seed 재실행 가능성

4. 환경과 배포를 분석합니다.
   - application.yml, application-local.yml.example, application-prod.yml
   - .env.example 정책, 배포 환경변수, secret 분리
   - 로컬 우선 실행, 분리 배포, CORS 보류, HTTPS 보류, upload directory, 로그/백업

5. 결과 문서를 작성하거나 갱신합니다.
   - docu/11_reviews/code_review_result.md
   - docu/11_reviews/security_environment_findings.md
   - docu/11_reviews/test_gap_report.md
   - docu/work_logs/YYYY-MM-DD_code_review.md

출력 형식:
- Findings first. 심각도 순서로 파일/라인 또는 경로를 포함합니다.
- 구현됨/부분 구현/문서 기준/미구현을 분리합니다.
- 사용자가 결정해야 하는 항목은 질문 목록으로 정리합니다.
- 실행하지 못한 검증은 이유와 함께 남깁니다.
```

## 참조 경로

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/00_inventory/source_reference_map.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/04_architecture/architecture_baseline.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/08_environment/env_variables.md`
- `docu/09_deployment/deployment_plan.md`
- `docu/11_reviews/review_plan.md`
