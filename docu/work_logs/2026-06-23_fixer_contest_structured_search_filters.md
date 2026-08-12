# 공모전 구조화 검색 필터 작업 로그

- 작업일: 2026-06-23
- 범위: DB migration, 공모전 backend 검색/입력 계약, 목록·기업·관리자 frontend, 테스트와 브라우저 검증

## 전제

- 아직 공모전 외부 데이터 크롤링을 수행하지 않았다.
- 기존 `target_text`, `prize_text`, `organizer` 문자열을 분석하거나 임의 분류하지 않았다.
- 기존 공모전 2건은 구조화 검색 값 미등록 상태를 유지한다.
- 향후 크롤러는 이번 구조화 필드와 허용 코드 계약을 사용하되 이번 작업에는 크롤러·HTML 파싱·외부 수집을 포함하지 않는다.

## 구현

1. `contest`, `contest_open_request`에 대상·지역 JSON 코드, 주최 유형, 총상금·1등 상금 숫자를 추가했다.
2. `GET /api/contests`에 검색어, 대상, 지역, 주최 유형, 두 상금 범위 조건을 추가했다.
3. 같은 필터 그룹은 OR, 서로 다른 그룹은 AND로 조회하고 `ANYONE`, 지역 `ALL` 범위는 해당 그룹 전체 선택과 호환한다.
4. service에서 허용 코드, 음수 금액, 역전 범위, 1등 상금이 총상금보다 큰 입력을 거부한다.
5. 회사 개설 요청·승인 공모전 수정·관리자 등록/수정 폼에 같은 구조화 입력을 추가하고 승인 시 요청 값을 공모전으로 승계한다.
6. 목록 필터는 URL query로 새로고침 복원하며 초기화 시 query와 선택 상태를 함께 제거한다.
7. 필터 내부에 크롤링 전이며 직접 등록된 구조화 정보만 검색된다는 안내를 표시한다.

## 검증

- `sql/13_contest_search_filter_schema.sql` 실제 MySQL 2회 적용 성공
- 최초 마이그레이션 적용 당시 기존 행 추정 구조화 데이터 0건 확인
- `mvn clean test` 및 후속 `mvn test`: 96 tests, failures 0, errors 0
- `npm run build`: 통과
- `git diff --check`: 통과
- 브라우저: 구조화 필터와 회사 입력 control 확인
- `대학생 + 서울` 적용: URL `target=UNIVERSITY&region=SEOUL`, 기존 미분류 데이터 0건 노출
- 새로고침 시 선택 복원, 초기화 시 URL 제거 및 실제 2건 복원
- desktop/390x844 수평 overflow 0, console error/warning 0

## 제한 및 후속 데이터 계약

- 권한 실행 한도로 임시 구조화 공모전의 실제 API 생성 smoke는 수행되지 않았다. 대신 service 기준 객체·검증 테스트, mapper SQL 계약 테스트, 실제 DB의 0건 조건 조회를 검증했다.
- 크롤링 또는 운영 입력이 시작되기 전까지 세부 필터 결과가 적거나 0건인 것은 정상이다.

## 후속 범위 조정

- 요청에 따라 `특전`과 `추가 정보` 필터·입력·API 계약을 제거했다.
- 신규 스키마와 `13_contest_search_filter_schema.sql`에서는 관련 컬럼을 생성하지 않는다.
- 이미 13번 마이그레이션의 이전 버전을 적용한 DB를 위해 `14_remove_contest_benefit_extra_schema.sql`을 추가했다.
- 범위 조정 후 `mvn clean test`, `npm run build`, `git diff --check`를 다시 통과했다.
