# 제외 항목

## 복사/커밋 제외

| 경로/항목 | 제외 이유 |
|---|---|
| `frontend/node_modules` | npm 의존성 설치 산출물 |
| `frontend/dist` | 빌드 산출물 |
| `backend/target` | Maven 빌드 산출물 |
| `uploads` | 로컬 업로드 파일 저장소 |
| `backend/src/main/resources/application-local.yml` | 개인 로컬 설정. 실제값 포함 가능 |
| `.env`, `.env.local`, `.env.production` | 실제 환경값 포함 가능 |
| `../prototype_3/images_page_ai` | 최종 저장소 미보관 결정. 비교 참조만 허용 |
| `../prototype*/frontend/node_modules`, `../prototype*/frontend/dist`, `../prototype*/backend/target` | 과거 prototype 생성물 |

## 기능 제외 또는 보류

| 기능 | 판정 | 이유 |
|---|---|---|
| 개발용 seed/reset API | 제외 | 운영/MVP 기준 불필요 |
| 이메일/SMS/푸시 알림 | 보류 | 현재 내부 알림 구현 중심 |
| 비동기 큐/예약 발송 | 보류 | 구현 확인 없음 |
| S3/CDN/트랜스코딩 | 보류 | 현재 로컬 파일 시스템 기준 |
| 물리 파일 삭제 배치 | 보류 | 정책과 구현 필요 |
| 고급 추천 알고리즘 | 보류 | 현재 점수 정책 + OpenAI 추천 |
| 사이트 내 공모전 심사/수상작 운영 | 보류 | 현재 제출 준비/이메일 안내 중심 |
| 이메일 인증/비밀번호 재설정/소셜 로그인 | 추가 기능 | MVP 필수 인증 범위 제외 |
| ffprobe 운영 필수화 | 보류 | 현재는 선택 설정 |
| HTTPS/CORS 세부 정책 | 보류 | 로컬 실행 우선 |
| 감사/운영 로그 보관 기간과 rotation 정책 | 보류 | 사용자 답변 기준 후순위 |

## 문서 처리

| 문서군 | 처리 |
|---|---|
| `docu/02_prototype_comparison` | 과거 prototype 차이 분석으로 보관 |
| `docu/11_reviews` | 이식 전 분석 결과와 현재 반영 상태를 함께 보관 |
| `docu/prompt` | 새 대화창에서 사용할 역할별 프롬프트만 보관 |
| `../docu` | 이전 준비 문서 원본. 현재 작업 기본 참조 금지 |
| `../prototype_3/docu` | 이미 필요한 내용은 `docu`에 복사/요약했으므로 필요 시 읽기 전용 |

## 참조 경로

- `docu/03_mvp_scope/copy_plan_from_prototype_3.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `.gitignore`
- `backend/.env.example`
- `frontend/.env.example`
