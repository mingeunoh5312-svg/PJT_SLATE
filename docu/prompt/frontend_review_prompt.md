# Frontend 분석 프롬프트

## 사용 목적

현재 `frontend`의 route guard, demo gate, 환경변수, build, 화면 smoke 위험을 집중 점검한다.

## 프롬프트

```text
당신은 Slate frontend 분석 담당자입니다.

먼저 읽을 문서:
1. Agent.md
2. docu/00_common/reference_policy.md
3. docu/06_frontend/frontend_baseline.md
4. docu/08_environment/env_variables.md
5. docu/11_reviews/test_gap_report.md

분석 대상:
- frontend/package.json
- frontend/vite.config.js
- frontend/src/router/index.js
- frontend/src/services/api.js
- frontend/src/views
- frontend/src/layouts
- frontend/src/styles/slate.css
- frontend/.env.example
- frontend/.env.production.example

해야 할 일:
- VITE_API_BASE_URL, VITE_DEMO_ACCESS_GATE 동작을 확인합니다.
- /demo-access, auth guard, admin guard의 충돌 가능성을 확인합니다.
- 외부 API key가 프론트에 노출되지 않는지 확인합니다.
- npm install, npm run build, 필요 시 dev/preview smoke를 기록합니다.
- 로그인, 관리자, 파일, YouTube, AI 추천, 모바일 overflow smoke 계획을 작성합니다.

결과:
- docu/11_reviews/frontend_review_result.md 또는 기존 review 문서를 갱신합니다.
- docu/work_logs/YYYY-MM-DD_frontend_review.md를 작성합니다.
```

## 참조 경로

- `Agent.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/08_environment/env_variables.md`
- `frontend`
