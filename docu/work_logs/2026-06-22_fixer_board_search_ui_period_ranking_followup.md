# 게시판 검색 UI·장르·기간 랭킹 후속 수정 로그

## 작업 범위

- HOME 범위 select를 접근 가능한 `작업물/자유게시판` 가로 탭으로 교체하고 초기화 버튼 제거
- HOME 검색을 `/boards/search`로 이동하고 query를 범위·검색어·정렬·필터의 단일 상태로 사용
- `추천 작업물`을 실제 최신순 `최신 작업물`로 변경
- 작업물 장르 저장 관계, 목록/검색/랭킹 필터, 등록·수정·팀 승인 공개 왕복 구현
- 직접 이미지 > YouTube 메타데이터 썸네일 > ID 기반 장르 mapping > neutral 이미지 fallback 구현
- 사용자 라벨을 `작품 유형`으로 통일하고 WORK의 일반 글쓰기 action 제거
- 인기 작업물 5건/프로필 10건, 주간·월간·전체 좋아요 순위, desktop 2열/mobile 1열 구현
- FREE를 행 전체 링크와 본문 clamp를 사용한 밀도형 목록으로 변경

## 데이터와 API 계약

- 신규 설치: `sql/01_schema.sql`에 `work_genre`, `team_work_approval_genre` 포함
- 기존 DB: `sql/11_board_search_genre_period_schema.sql`
- 목록: `genreId` 추가, 기존 category/public visibility 검증 유지
- 랭킹: `WEEKLY_WORK`, `MONTHLY_WORK`, `POPULAR_WORK`; `like_count DESC, created_at DESC, post_id DESC`
- 대표 장르: `sort_order`, `genre_id` 순 첫 장르를 N+1 없는 scalar subquery로 반환
- 장르 asset은 아직 없으므로 ID 기반 mapping config는 비워 두고 neutral asset으로 안전하게 fallback

## 검증

| 항목 | 결과 |
|---|---|
| backend | `mvn test`: 86개 통과, failure/error/skipped 0 |
| frontend | `npm run build` 통과, 기존 500kB chunk 경고만 존재 |
| mapper/diff | `xmllint --noout`, 관련 파일 `git diff --check` 통과 |
| desktop browser | 1280x720, 검색 URL 이동·새로고침 복원, 범위/기간 active, 인기 2열, overflow 0, console error 0 |
| mobile browser | 390x844, HOME/검색/POPULAR, 인기 1열, overflow 0, console error 0 |
| action | WORK 검색은 `작업물 올리기`만, FREE 검색은 `글쓰기`와 `작업물 올리기`; FREE `보기` 버튼 0개 확인 |
| 실제 MySQL migration | MySQL 8.0.46 `slate` DB에 `11_board_search_genre_period_schema.sql` 2회 연속 적용, 두 실행 모두 오류 0 |
| DB 구조 | InnoDB 관계 테이블 2개, PK 2개, 보조 인덱스 3개, FK 4개 확인 |
| DB 정합성 | 두 관계 테이블 고아 데이터 0건. 실제 기준 행으로 삽입·조회 각 1건 확인 후 rollback, 최종 행 수 각 0건 |

## 남은 검증 범위

- 브라우저 검증은 Vite 화면과 route/query/레이아웃/콘솔을 대상으로 수행했으며, 백엔드가 내려가 있어 실데이터 장르 필터 결과와 등록·수정 UI의 저장 응답은 backend 단위·SQL 계약 테스트로 확인했다.

## 참조

- `docu/prompt/board_search_ui_period_ranking_followup_fixer_prompt.md`
- `docu/user_temp/What_to_do_2.md`
- `docu/work_logs/2026-06-22_fixer_board_full_integration.md`
