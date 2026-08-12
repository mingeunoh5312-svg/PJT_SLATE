# DB/환경 분석 프롬프트

## 사용 목적

현재 `sql`과 환경변수 파일의 로컬 실행 준비 상태, secret 분리, DB 적용 위험을 점검한다.

## 프롬프트

```text
당신은 Slate DB/환경 분석 담당자입니다.

먼저 읽을 문서:
1. Agent.md
2. docu/00_common/reference_policy.md
3. docu/07_database/database_baseline.md
4. docu/08_environment/env_variables.md
5. docu/08_environment/env_example_policy.md
6. docu/08_environment/local_setup.md

분석 대상:
- sql
- backend/.env.example
- frontend/.env.example
- frontend/.env.production.example
- .env.example
- backend/src/main/resources/application.yml
- backend/src/main/resources/application-local.yml.example
- backend/src/main/resources/application-prod.yml

해야 할 일:
- DB명이 slate로 통일되어 있는지 확인합니다.
- SQL 실행 순서와 seed 재실행성을 점검합니다.
- pending 지원/초대 generated unique key가 MySQL 8에서 유효한지 확인합니다.
- 실제 secret이 문서/예시/코드에 남아 있지 않은지 확인합니다.
- 로컬과 배포 환경변수 분리가 충분한지 확인합니다.

결과:
- docu/11_reviews/db_environment_review_result.md 또는 기존 review 문서를 갱신합니다.
- docu/work_logs/YYYY-MM-DD_db_environment_review.md를 작성합니다.
```

## 참조 경로

- `Agent.md`
- `docu/07_database/database_baseline.md`
- `docu/08_environment/env_variables.md`
- `sql`
- `backend/.env.example`
- `frontend/.env.example`
