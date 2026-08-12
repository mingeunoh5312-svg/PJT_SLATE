# 데이터베이스 차이

## 요약

| 단계 | DB명 | 테이블 수 | SQL 파일 |
|---|---|---:|---|
| `prototype` | `slate_proto` | 21 | `00_create_database.sql`, `01_schema.sql`, `02_seed.sql`, `03_reset.sql` |
| `prototype_2` | `slate_prototype2` | 49 | `00_create_database.sql`, `01_schema.sql`, `02_seed_reference.sql`, `03_seed_sample_data.sql`, `99_reset.sql` |
| `prototype_3` | `slate_prototype2` | 50 | `00_create_database.sql`, `01_schema.sql`, `02_seed_reference.sql`, `03_seed_sample_data.sql`, `04_youtube_metadata_schema.sql`, `05_seed_ai_matching_dummy_data.sql`, `99_reset.sql` |

## 테이블 변화

| 비교 | 추가 | 제거 |
|---|---|---|
| `prototype` -> `prototype_2` | 회사 승인, 관리자 권한, 포트폴리오, 공공데이터 sync, 팀 지원/초대/계획/종료, 매칭 북마크/로그/정책 이력, 조회 로그, 신고, 작업물/파일, 공모전, 제재, 알림, 감사/운영 로그 | 없음 |
| `prototype_2` -> `prototype_3` | `portfolio_verification` | 없음 |

## `prototype_3` 추가 SQL

| 파일 | 목적 | MVP 반영 |
|---|---|---|
| `prototype_3/sql/04_youtube_metadata_schema.sql` | 기존 DB에 YouTube metadata 컬럼을 보강하는 마이그레이션 성격 SQL | 포함 후보 |
| `prototype_3/sql/05_seed_ai_matching_dummy_data.sql` | AI 매칭 테스트용 dummy seed | 조건부 포함. 운영 seed와 분리 필요 |

## DB 기준 판단

| 항목 | 기준 |
|---|---|
| 최종 schema | `prototype_3/sql/01_schema.sql` |
| 기준 seed | `prototype_3/sql/02_seed_reference.sql`, `prototype_3/sql/03_seed_sample_data.sql` |
| reset | `prototype_3/sql/99_reset.sql` |
| 운영 마이그레이션 | 현재는 SQL 파일 수동 실행 기준. Flyway/Liquibase 도입 여부 질문 필요 |
| 비밀번호 | 문서/SQL에 실제값 금지. `CHANGE_ME` 또는 환경변수만 허용 |

## 주의할 누락/검증

| 항목 | 상태 |
|---|---|
| 직접 MySQL row 검증 | 일부 최신 기능에서 미수행으로 기록됨 |
| 물리 파일 삭제 배치 | DB는 삭제 예정일을 남기지만 실제 배치 정책 미정 |
| 고아 업로드 파일 정리 | 후속 설계 필요 |
| 동시성 제약 | 초대/지원/조회수 등 서비스 count 기반 항목 검토 필요 |

## 참조 경로

- `prototype/sql/01_schema.sql`
- `prototype/docs/database.md`
- `prototype_2/sql/01_schema.sql`
- `prototype_2/docs/database.md`
- `prototype_3/sql/00_create_database.sql`
- `prototype_3/sql/01_schema.sql`
- `prototype_3/sql/02_seed_reference.sql`
- `prototype_3/sql/03_seed_sample_data.sql`
- `prototype_3/sql/04_youtube_metadata_schema.sql`
- `prototype_3/sql/05_seed_ai_matching_dummy_data.sql`
- `prototype_3/sql/99_reset.sql`
- `prototype_3/docu/02_workflows/fixer_work_summary.md`
