# Backend 분석 프롬프트

## 사용 목적

현재 `backend`의 보안, 외부 API, 파일 처리, 테스트 누락을 집중 점검한다.

## 프롬프트

```text
당신은 Slate backend 분석 담당자입니다.

먼저 읽을 문서:
1. Agent.md
2. docu/00_common/reference_policy.md
3. docu/05_backend/backend_baseline.md
4. docu/08_environment/env_variables.md
5. docu/11_reviews/code_review_result.md
6. docu/11_reviews/security_environment_findings.md

분석 대상:
- backend/pom.xml
- backend/src/main/java/com/slate
- backend/src/main/resources/application.yml
- backend/src/main/resources/application-local.yml.example
- backend/src/main/resources/application-prod.yml
- backend/src/main/resources/mappers
- backend/src/test

해야 할 일:
- SecurityConfig, JwtAuthenticationFilter, DemoAccessFilter의 공개/보호 endpoint를 점검합니다.
- 파일 업로드/다운로드/스트리밍 권한과 path traversal 방어를 점검합니다.
- 공개 회사 서류 업로드의 1회성 token/rate limit 필요성을 정리합니다.
- KOBIS/YouTube/OpenAI key 관리, fallback, 로그 노출을 점검합니다.
- MyBatis mapper와 service transaction 경계를 점검합니다.
- mvn test 실행 가능 여부와 누락 테스트를 기록합니다.

결과:
- docu/11_reviews/backend_review_result.md 또는 기존 review 문서를 갱신합니다.
- docu/work_logs/YYYY-MM-DD_backend_review.md를 작성합니다.
```

## 참조 경로

- `Agent.md`
- `docu/05_backend/backend_baseline.md`
- `docu/08_environment/env_variables.md`
- `backend`
