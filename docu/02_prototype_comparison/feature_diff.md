# 기능 차이

## 영역별 비교

| 영역 | 초기 구상 | `prototype` | `prototype_2` | `prototype_3` 기준 |
|---|---|---|---|---|
| 계정/인증 | 회원가입, 로그인, 회사 승인, 관리자 | 인증 제외, 사용자 헤더 | JWT, loginId, 회사 승인, 문서 업로드, 관리자 승인 | 유지. 회사/일반 가입 route 분리, AuthLayout 적용 |
| 프로필 | 역할/장르/지역, 공공데이터 참여작 | 프로필 CRUD, 역할/장르 | 포트폴리오 CRUD, 로컬 공공데이터 대체 검색 | KOBIS 검색/검증, `portfolio_verification`, Verified 배지 추가 |
| 팀 | 팀 생성, 모집, 지원/초대, 팀 상태 | 팀/모집 최소 | 팀 생명주기, 지원/초대, 멤버/계획/종료/재개 | 유지. 팀 route 세분화 |
| 매칭 | 양방향 추천, 점수 정책 | 후보 조회와 점수 계산 | 저장, 초대, 지원, 관리자 점수 정책 | OpenAI AI 추천 API/UI, slotId 역할군 필터 보강 |
| 게시판 | 작업물/자유, 리뷰, 좋아요, 랭킹 | 게시글/리뷰/좋아요 최소 | CRUD, 신고, 조회수 중복 방지, 파일/팀 작업물 | YouTube 미리보기/메타데이터/검색/수정/삭제 표시 추가 |
| 작업물 파일 | YouTube/서버 업로드, 용량 제한 | 제외 | 서버 업로드, 3분/300MB, 용량 제한, 팀 승인 | 유지. YouTube 메타데이터 컬럼 추가 |
| 공모전 | 자체/외부, 적합도, 저장, 제출 준비 | 제외 | 목록/상세/저장/제출 준비, 회사/관리자 관리 | route 분리와 화면 개편. 실제 AI 적합도는 확인 필요 |
| 알림/로그 | 내부 알림, 감사 로그 | 제외 | 알림, 관리자 발송, 감사/운영 로그 | 유지 |
| 관리자 | 승인, 삭제, 제재, 권한, 로그 | 제외 | 회사 승인, 신고, 제재, 파일, 알림, 권한, 로그 | 게시글/회원/팀 관리자 CRUD 보강 |
| 화면 | 모바일/서비스 UI 정책 | P00-P11 | App Shell 0단계 | 대표 이미지 기반 화면, 독립 route 개편 |

## `prototype_3`에서 추가된 기능

| 추가 기능 | 구현 근거 | 검증 상태 |
|---|---|---|
| KOBIS 기반 Verified 경력 배지 | `portfolio_verification`, KOBIS client, Profile API/UI | 통합 API 검증. 실제 배지 레이아웃 브라우저 확인 남음 |
| YouTube 작업물 메타데이터 | YouTube config/client/preview/register/read/update/delete/search | 목 YouTube API 기반 smoke. 실제 API Key/브라우저 확인 남음 |
| OpenAI AI 매칭 추천 | OpenAI config/client, `/api/matching/ai/recommendations`, `/matching/ai` | 테스트/build 통과. 실제 API Key 호출 남음 |
| 관리자 게시글/회원/팀 CRUD | AdminBoard/AdminUser/AdminTeam API와 `/admin/*` UI | API smoke 완료. 브라우저 조작 검증 남음 |
| 프론트 독립 route 개편 | App/Auth/Admin layout, 세부 route | build/diff check. 일부 브라우저 smoke 남음 |

## `prototype_3` 기준 부분 구현

| 항목 | 이유 |
|---|---|
| 실제 외부 API 성공 흐름 | OpenAI/YouTube/KOBIS key가 필요 |
| 배포 파일 저장 | 로컬 `uploads` 기준이며 운영 저장소/물리 삭제 배치 미정 |
| 전체 권한/상태 동시성 | 서비스 count 기반 중복 차단이 있어 DB 제약 보강 필요 |
| 브라우저 전체 회귀 | 프론트 route 개편 후 실제 백엔드 mutation smoke가 남음 |

## 참조 경로

- `docu/plan/features`
- `prototype/docs/known_issues.md`
- `prototype_2/docs/feature_status.md`
- `prototype_2/docs/known_issues.md`
- `prototype_3/docu/02_workflows/creator_work_summary.md`
- `prototype_3/docu/02_workflows/fixer_work_summary.md`
- `prototype_3/docu/02_workflows/work_units.md`
