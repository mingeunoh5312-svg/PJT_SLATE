# 수정/누락/제거 항목

## `prototype_3` 기준 제거 또는 대체된 항목

| 이전 항목 | 현재 처리 |
|---|---|
| `prototype`의 `X-Prototype-User-Id` | JWT 인증 사용자로 대체 |
| `prototype`의 개발용 seed/reset API | 운영/MVP 복사 기준에서 제외 |
| `prototype`의 단일 Controller/Service/Mapper 구조 | 기능별 패키지와 Mapper로 분리 |
| P00-P11 프로토타입 화면 | 실제 서비스 route와 App/Auth/Admin layout으로 대체 |
| `prototype_2`의 App Shell 0단계 | `prototype_3` 독립 route 개편으로 대체 |
| 과거 프론트 포트 5173/5175/5177 기록 | 현재 기준 5174 |
| 과거 로컬 경로 `E:\...`, `C:\Users\SSAFY\...` | 환경 문서에서 개인별 경로로 분리 |

## `prototype_3`에서 추가됐지만 아직 확인이 필요한 항목

| 항목 | 남은 확인 |
|---|---|
| KOBIS Verified 배지 | 실제 브라우저 레이아웃, 유효 API key smoke |
| YouTube metadata | 실제 API key, 브라우저 네트워크/콘솔, 직접 MySQL row 확인 |
| OpenAI AI 매칭 | 실제 API key 호출, 실패/fallback UI, 비용/쿼터 |
| 관리자 CRUD | 브라우저 클릭/콘솔/네트워크 확인, 검증 데이터 정리 |
| 프론트 route 개편 | 백엔드 연결 mutation smoke |
| 배포 | `.env`/환경변수 주입, 파일 저장, 포트/프록시, HTTPS/CORS |

## 문서에는 있으나 구현 확인이 필요한 항목

| 항목 | 근거 | 확인 필요 |
|---|---|---|
| 이메일/SMS/푸시 알림 | 초기 기능 정책, prototype_2 known issues | 현재 구현은 내부 알림 중심 |
| 비동기 큐/예약 발송 | prototype_2 known issues | 현재 구현 여부 없음 |
| 파일 물리 삭제 배치 | prototype_2/3 known issues | 삭제 예정일은 있으나 배치 정책 필요 |
| S3/CDN/트랜스코딩 | 초기 확장 항목 | 현재 로컬 파일 시스템 기준 |
| 고급 추천 알고리즘 | 초기 확장 항목 | 현재 점수 정책 + OpenAI 추천 |
| 사이트 내 공모전 직접 제출/심사 | 초기 확장 항목 | 현재 이메일 제출 안내/제출 준비 중심 |

## 사용자 판단 필요

| 질문 | 이유 |
|---|---|
| MVP 제작에서 실제 외부 API를 필수로 포함할지 | key/비용/장애 fallback 정책 필요 |
| 서버 업로드를 운영 배포에도 유지할지 | 저장소, 용량, 백업, 삭제 배치 결정 필요 |
| 테스트 계정과 샘플 seed를 운영에도 유지할지 | 배포 보안과 데모 편의성 충돌 |
| `prototype_3` 패키지명 `prototype2`를 변경할지 | 최종 제품명과 마이그레이션 비용 판단 |
| 루트 `Slate/` 폴더를 앱 루트로 쓸지 | 현재 `Slate/`는 빈 `docu`만 있음 |

## 참조 경로

- `prototype/docs/api.md`
- `prototype/docs/known_issues.md`
- `prototype_2/docs/known_issues.md`
- `prototype_2/docs/verification.md`
- `prototype_3/docu/setup.md`
- `prototype_3/docu/02_workflows/handoff_next_summary.md`
- `prototype_3/docu/02_workflows/fixer_work_summary.md`
- `prototype_3/frontend/src/router/index.js`
- `prototype_3/backend/src/main/java/com/slate/prototype2/SlatePrototype2Application.java`
- `prototype_3/sql`
