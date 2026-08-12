# 배포/Smoke 검증 프롬프트

## 사용 목적

로컬 시연과 프론트/백엔드 분리 배포 전 smoke 검증 계획을 세우고 실행 결과를 기록한다.

## 프롬프트

```text
당신은 Slate 배포 smoke 담당자입니다.

먼저 읽을 문서:
1. Agent.md
2. docu/00_common/reference_policy.md
3. docu/09_deployment/deployment_plan.md
4. docu/08_environment/local_setup.md
5. docu/08_environment/env_variables.md
6. docu/11_reviews/test_gap_report.md

검증 대상:
- backend
- frontend
- sql
- demo access gate
- KOBIS/YouTube/OpenAI 실제 key가 필요한 흐름

해야 할 일:
- 로컬 MySQL schema/seed 적용 결과를 기록합니다.
- backend mvn test와 frontend npm run build 결과를 기록합니다.
- backend API smoke와 frontend 브라우저 smoke를 분리해 기록합니다.
- VITE_DEMO_ACCESS_GATE=true, SLATE_DEMO_ACCESS_ENABLED=true 조합으로 demo gate를 확인합니다.
- 실제 외부 API key가 없으면 실행하지 못한 검증으로 명시합니다.
- 배포 provider/HTTPS/CORS가 아직 보류인지 확인하고 임의 결정하지 않습니다.

결과:
- docu/11_reviews/deployment_smoke_result.md를 작성합니다.
- docu/work_logs/YYYY-MM-DD_deployment_smoke.md를 작성합니다.
```

## 참조 경로

- `Agent.md`
- `docu/09_deployment/deployment_plan.md`
- `docu/08_environment/local_setup.md`
- `backend`
- `frontend`
- `sql`
