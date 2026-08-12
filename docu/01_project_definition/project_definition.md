# 프로젝트 정의

## 목적

Slate는 영화 제작 참여자와 팀, 작업물, 공모전, 회사/관리자 운영을 연결하는 서비스다. MVP 제작 기준은 `prototype_3`이며, 이 문서는 초기 구상과 현재 구현 기준을 분리해 정의한다.

## 초기 구상

초기 문서의 핵심 범위는 다음과 같다.

| 영역 | 초기 구상 |
|---|---|
| 계정/권한 | 일반 사용자, 회사, 관리자, 회사 승인 |
| 프로필/포트폴리오 | 제작자 프로필, 역할/장르/지역, 공공데이터 기반 참여작 입력 |
| 팀/모집 | 팀 생성, 모집 슬롯, 지원/초대, 팀 상태 관리 |
| 매칭 | 팀장-팀원, 팀원-팀 양방향 추천, 점수 정책 |
| 게시판/작업물 | 작업물/자유 게시판, 리뷰, 좋아요, 랭킹, 영상 업로드/YouTube |
| 공모전 | 자체/외부 공모전, 저장, 제출 준비, 적합도 |
| 운영 | 알림, 감사 로그, 관리자 권한, 신고/제재 |
| 배포 | AWS EC2, MySQL, 파일 저장, 환경변수 분리 |

## 현재 구현 기준

`prototype_3`에서 확인된 현재 기준은 다음과 같다.

| 영역 | 현재 기준 |
|---|---|
| Frontend | Vue 3 + Vite, Vue Router, App/Auth/Admin layout, 독립 route |
| Backend | Spring Boot 4, Spring MVC, Spring Security JWT, MyBatis, MySQL 8 |
| DB | 원본 `prototype_3`는 `slate_prototype2`, MVP 목표 DB명은 `slate`. 50개 기본 테이블, YouTube 추가 컬럼 SQL, AI 매칭 dummy seed |
| 인증 | loginId 기반 로그인, JWT, account type/status, 관리자 권한 |
| 외부 API | KOBIS, YouTube Data API, OpenAI API를 필수 기능으로 두고 서버 환경변수로 관리 |
| 파일 | 서버 업로드, 용량/길이 제한, ffprobe 선택 설정, 로컬 `uploads` |
| 문서 | `prototype_3/docu`의 workflow summary와 work log가 최신 작업 근거 |

## MVP 제작 기준

MVP 제작은 `prototype_3`를 분석 기준으로 삼고, 최종 앱은 `<SLATE_ROOT>` 하위에 둔다. 아래 항목은 사용자 답변으로 확정됐다.

| 항목 | 확정 기준 |
|---|---|
| 루트 앱 위치 | `<SLATE_ROOT>` |
| 최종명 | `prototype2`, `prototype_2`, `prototype_3`, `slate_prototype2` 계열 이름 변경 |
| 외부 API 실제 사용 범위 | KOBIS, YouTube Data API, OpenAI AI 매칭 모두 필수 |
| 파일 저장 방식 | MVP 제작 단계에서는 로컬 파일 시스템 우선 |
| 관리자/회사 권한 정책 | 회사 승인, 관리자 권한, 제재 정책 유지 |
| 테스트 데이터 | 배포 데모에 포함하되 웹 페이지 접근 전 접속 코드 gate 필수 |
| 배포 | 로컬 시연과 프론트/백엔드 분리 배포 필수. EC2 단일 서버 최후순위 |
| 대표 이미지 | `prototype_3/images_page_ai`는 참조 전용이며 최종 저장소에는 보관하지 않음 |

## 구현 여부 구분

| 구분 | 설명 |
|---|---|
| 구현됨 | `prototype_3` 코드/SQL/프론트에 실제 존재 |
| 부분 구현 | 코드가 있으나 실제 외부 API key, 브라우저, 배포 검증이 남음 |
| 문서 기준 | 루트 `docu/plan` 또는 이전 프로토타입 문서에만 있는 정책 |
| 제외 | MVP 제작에서 복사하지 않거나 운영 배포 전 제거할 항목 |

## 참조 경로

- `docu/plan/02_project_spec.md`
- `docu/plan/features`
- `docu/plan/prototype/06_prototype_result_review_and_first_scope.md`
- `prototype_2/docs/feature_status.md`
- `prototype_2/docs/known_issues.md`
- `prototype_3/docu/README.md`
- `prototype_3/docu/02_workflows/work_units.md`
- `prototype_3/docu/02_workflows/creator_work_summary.md`
- `prototype_3/docu/02_workflows/fixer_work_summary.md`
- `prototype_3/backend`
- `prototype_3/frontend`
- `prototype_3/sql`
- `docu/03_mvp_scope/mvp_decisions.md`
