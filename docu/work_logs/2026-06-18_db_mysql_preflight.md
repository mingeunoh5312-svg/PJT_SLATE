# DB MySQL Preflight

작성일: 2026-06-18
역할: DB 통합 검증

## 작업 범위

- MySQL 설치, 버전, 서비스 및 접속 상태 확인
- 관리자 계정 접속 가능 여부와 기존 `slate` DB/`slate_app` 계정 조회 가능 여부 확인
- SQL 파일 순서, 의존성, 재실행 위험 정적 점검
- 백엔드 DB 환경변수와 설정 경로 확인
- 파괴적 작업 없이 다음 SQL 적용 단계 진행 가능 여부 판단

## 참조 경로

- `Agent.md`
- `docu/README.md`
- `docu/07_database/database_baseline.md`
- `docu/08_environment/env_variables.md`
- `docu/08_environment/local_setup.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `sql`
- `backend/.env.example`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-local.yml`
- `backend/src/main/resources/application-local.yml.example`
- `backend/src/main/resources/application-prod.yml`

## 실행한 명령

실제 비밀값이 출력되지 않도록 설정값은 존재 여부 또는 placeholder 여부만 확인했다.

- `mysql --version`, `mysqld --version`
- `launchctl print system/com.oracle.oss.mysql.mysqld`
- `lsof -nP -iTCP:3306 -sTCP:LISTEN`
- `mysqladmin` TCP 및 Unix socket ping
- MySQL socket 후보와 credential 파일 존재 여부 확인
- `SLATE_DB_*` 환경변수 설정 여부 확인
- backend 설정 파일 존재 여부 및 DB 변수 binding 정적 확인
- `rg`, `sed`, `tail`을 이용한 SQL 순서, DDL, seed, reset 정적 확인
- SQL 파일 SHA-256 기록
- `git status --short`

## 초기 Preflight 결과

### MySQL 환경

- MySQL Community Server 및 client `8.0.46`이 설치되어 있다.
- 사용자 터미널에서 MySQL server 실행 상태와 PID를 확인했다.
- 검증 sandbox에서는 로컬 socket/TCP 접근이 제한되었으나, 승인된 읽기 전용 TCP 접속은 성공했다.
- MySQL data directory는 존재하지만 현재 사용자에게 읽기 권한이 없어 내부 DB 디렉터리는 확인하지 않았다.

### 적용 전 접속 및 기존 객체

- `slate-admin` login path를 통한 관리자 읽기 전용 접속에 성공했다.
- `slate` DB는 존재하지 않는다.
- 과거 이름인 `slate_prototype2` DB가 존재한다.
- `slate_app@localhost`, `slate_app@127.0.0.1` 계정이 존재하며 잠금 및 비밀번호 만료 상태가 아니다.
- 두 `slate_app` 계정 모두 `slate_prototype2.*`에 대한 권한만 있고 `slate` DB 권한은 없다.
- 현재 shell에 `SLATE_DB_URL`, `SLATE_DB_USERNAME`, `SLATE_DB_PASSWORD`는 주입되어 있지 않다.
- 사용자 홈에 관리자 login path가 안전하게 등록되었다. 실제 비밀번호는 조회하거나 기록하지 않았다.
- 로컬 backend 설정 파일은 존재하고 Git 제외 대상이지만, datasource password는 placeholder 상태다.

### SQL 정적 점검

의도된 적용 순서는 다음과 일치한다.

1. `sql/00_create_database.sql`
2. `sql/01_schema.sql`
3. `sql/02_seed_reference.sql`
4. `sql/03_seed_sample_data.sql`
5. `sql/04_youtube_metadata_schema.sql`
6. `sql/05_seed_ai_matching_dummy_data.sql`

`sql/99_reset.sql`은 실행하지 않았다.

- `01_schema.sql`은 50개 table을 생성하고 마지막에 foreign key 검사를 다시 활성화한다.
- MySQL 8에서 사용할 pending 지원/초대 generated column과 unique key가 정의되어 있다. 실제 동작 검증은 서버 적용 후 필요하다.
- YouTube metadata column은 기본 schema에 이미 포함되어 있고 `04_youtube_metadata_schema.sql`은 column 존재 여부를 검사하므로 신규 schema에서는 중복 변경 없이 종료될 것으로 예상된다.
- `02_seed_reference.sql`과 `03_seed_sample_data.sql`은 일반 INSERT 중심으로 재실행 안전성이 없다. 반드시 빈 schema 또는 reset 후 1회 적용해야 한다.
- `05_seed_ai_matching_dummy_data.sql`은 일부 upsert와 대상 dummy 관계 삭제를 포함한다. 운영 데이터가 아닌 demo/test DB에서만 적용해야 한다.
- `99_reset.sql`은 전체 table을 DROP하므로 이후 명시적인 재적용 검증 단계 외에는 실행하면 안 된다.

## 적용 전 차단 요소와 권장 조치

1. 문서 기준 `slate` DB가 없고 과거 `slate_prototype2` DB만 존재한다. 기존 DB를 삭제하지 않고 새 `slate` DB를 생성해야 한다.
2. 기존 `slate_app` 계정 권한은 `slate_prototype2.*`에만 있다. 비밀번호를 변경할 필요 없이 `slate.*` 권한을 추가해야 한다.
3. `00_create_database.sql`의 애플리케이션 계정 비밀번호는 placeholder다. 기존 계정이 있으므로 파일을 그대로 실행하지 말고 DB 생성과 권한 부여만 안전하게 수행해야 한다.
4. backend 로컬 datasource password도 placeholder다. 기존 `slate_app` 비밀번호를 로컬 전용 설정 또는 OS 환경변수에 주입해야 한다.
5. `slate_prototype2`의 보존 여부와 무관하게 이번 단계에서는 삭제하거나 reset하지 않는다.

## 다음 단계 판단

MySQL 실행, 관리자 접속, 기존 객체 확인을 완료한 뒤 기존 `slate_prototype2`를 보존한 채 다음 작업을 수행했다.

- `slate` DB 생성 완료
- 기존 `slate_app@localhost`, `slate_app@127.0.0.1`에 `slate.*` 권한 추가 완료
- `01_schema.sql`부터 `05_seed_ai_matching_dummy_data.sql`까지 순차 적용 완료
- `99_reset.sql` 미실행

## 적용 후 검증

- `slate` base table: 50개
- 기존 `slate_prototype2` base table: 50개로 보존
- 공통 코드 그룹: 38건
- 공통 코드: 133건
- 역할: 28건
- 장르: 19건
- 전체 sample 사용자: 21건
- AI dummy 사용자: 8건
- YouTube metadata column: 대상 2개 table에 총 10개
- pending 중복 방지 stored generated column: 2개
- pending unique index: 2개
- migration용 임시 procedure: 적용 후 제거 확인

DB schema/seed 적용 후 다음 연결 검증까지 완료했다.

- `slate_app` 계정으로 `slate` 직접 접속 성공
- `SHOW TABLES`에서 50개 table 확인
- backend 기동 후 `GET /api/references/genres` 호출 성공
- API 응답 `success: true` 및 장르 19건 반환 확인

실제 `slate_app` 비밀번호는 작업 로그와 채팅에 기록하지 않았다.

## 최초 적용 단계 변경 및 미실행 사항

- 변경 파일: 이 작업 로그 1개
- SQL 및 애플리케이션 코드 변경 없음
- `slate` DB 생성 및 `slate_app` 권한 부여 실행
- `01_schema.sql`~`05_seed_ai_matching_dummy_data.sql` 적용
- `99_reset.sql` 미실행
- 기존 `slate_prototype2` DB/데이터 삭제 또는 변경 없음

## 2026-06-18 보완 통합 검증

이 섹션은 위 초기 preflight와 최초 적용 이후 수행한 보안, reset, 제약조건, 회귀 검증의 최종 결과다.

### 보안 조치

- `backend/src/main/resources/application-local.yml`이 Git 추적 대상임을 확인했다.
- datasource password에 기록된 실제 기본값을 제거했다.
- 설정을 `password: ${SLATE_DB_PASSWORD}`로 변경해 문서 기준 변수명과 일치시키고 파일 기본값을 없앴다.
- 노출된 기존 `slate_app` 비밀번호를 폐기하고 `localhost`, `127.0.0.1` 계정 모두 무작위 새 비밀번호로 교체했다.
- 새 비밀번호는 파일, 명령 출력, 작업 로그에 기록하지 않았다.
- 새 비밀번호는 macOS 사용자 환경변수 `SLATE_DB_PASSWORD`에만 주입했다.
- 오래된 비밀번호를 담고 있던 `slate-app` MySQL login path는 제거했다.
- `.gitignore`에는 이미 `application-local.yml` ignore 규칙이 있다. 다만 파일이 기존 Git 추적 대상이라 ignore 규칙만으로는 보호되지 않는다.
- `.gitignore`의 `/.metadata/` 추가는 이번 DB 작업과 무관한 기존 사용자 변경으로 판단해 수정하거나 되돌리지 않았다.

### Reset 및 재적용

- `slate`의 사용자 21건 모두 `.test` 이메일과 `{noop}` password hash를 사용하는 sample/demo 데이터임을 확인한 뒤 reset을 수행했다.
- 최초 `99_reset.sql` 실행 후 `company_application_document` table 1개가 남는 결함을 발견했다.
- `sql/99_reset.sql`에 누락된 `DROP TABLE IF EXISTS company_application_document`를 FK 순서에 맞게 추가했다.
- 수정 후 reset 재실행 결과 `slate` base table 0개를 확인했다.
- `01_schema.sql`부터 `05_seed_ai_matching_dummy_data.sql`까지 모두 오류 없이 재적용했다.
- 재적용 결과는 최초 적용과 동일했다.
  - table 50개
  - 공통 코드 그룹 38건
  - 공통 코드 133건
  - 역할 28건
  - 장르 19건
  - 사용자 21건
  - AI dummy 사용자 8건
  - YouTube metadata column 10개
  - pending generated column/index 각 2개
- `slate_prototype2`는 reset 전후 모두 table 50개, 사용자 23건으로 유지됐다.

### 실제 제약조건 검증

- 기존 sample에 없는 사용자/슬롯 조합과 구분 가능한 검증 message를 사용했다.
- 동일 pending 팀 지원의 첫 INSERT는 1건 성공하고 두 번째 INSERT는 `uq_application_pending`에서 `1062 Duplicate entry`로 차단됐다.
- 동일 pending 팀 초대의 첫 INSERT는 1건 성공하고 두 번째 INSERT는 `uq_invitation_pending`에서 `1062 Duplicate entry`로 차단됐다.
- 두 검증은 각각 transaction rollback 후 임시 행 0건을 확인했다.
- 슬롯 정원 조건부 UPDATE는 첫 증가 affected row 1, 정원 도달 후 두 번째 증가 affected row 0을 확인했다.
- 슬롯 검증은 rollback 후 `accepted_count`가 원래 값으로 복원됨을 확인했다.
- `TeamService`의 수락 경로는 `@Transactional`이며 mapper affected row가 0이면 정원 마감 예외를 발생시키는 구조임을 확인했다.
- 실제 HTTP 동시 수락 요청 E2E는 수행하지 않았고, mapper와 동일한 transaction 수준 조건부 UPDATE로 검증했다.

### 회귀 검증

- 새 OS 환경변수만 사용해 `slate_app`의 `slate` 접속 성공
- `slate_app`으로 table 50개, 장르 19건 조회 성공
- `mvn test`: 39 tests, failures 0, errors 0, skipped 0
- 새 환경변수로 backend를 별도 포트에 기동하고 Hikari 신규 연결 성공 확인
- `GET /api/references/genres`: `success=true`, 장르 19건 확인
- 검증용 backend 프로세스 정상 종료

### 최종 Git 상태

```text
 M .gitignore
 M backend/src/main/resources/application-local.yml
 M sql/99_reset.sql
?? docu/work_logs/2026-06-18_db_mysql_preflight.md
```

이번 작업에서 변경한 파일:

- `backend/src/main/resources/application-local.yml`
- `sql/99_reset.sql`
- `docu/work_logs/2026-06-18_db_mysql_preflight.md`

이번 작업에서 변경하지 않은 기존 사용자 변경:

- `.gitignore`

### 남은 문제

- `application-local.yml`은 ignore 규칙이 있지만 이미 Git 추적 중이다. 현재 파일에는 비밀값이 없지만, 향후 재노출 방지를 위해 별도 승인된 Git 변경에서 추적 해제를 검토해야 한다.
- `launchctl setenv`로 주입한 로컬 환경변수는 사용자 세션 범위다. 재로그인/재부팅 후에는 IDE Run Configuration 또는 승인된 로컬 secret 주입 절차가 필요하다.
- 슬롯 정원 초과 방어의 실제 동시 HTTP 요청 E2E는 미수행이다. 이번에는 transaction 수준 조건부 UPDATE와 서비스 코드 경로를 검증했다.
