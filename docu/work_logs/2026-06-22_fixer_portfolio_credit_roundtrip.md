# 포트폴리오 크레딧 저장·조회·KOBIS 검증 수정

## 원인

- 프런트 요청의 `creditName`이 `portfolio_item`에 저장되지 않았다.
- 검증 준비 데이터가 사용자 입력 이름을 `portfolio_verification.provider_person_name`에 임시 저장했다.
- KOBIS 이름 불일치 결과가 provider 이름을 null로 덮어써 사용자 입력까지 유실된 것처럼 응답됐다.
- 포트폴리오 목록·단건·소유자 조회가 `creditName`과 provider 매칭 이름을 분리해 반환하지 않았다.

## 최종 데이터 계약

- `portfolio_item.credit_name`: 사용자가 입력한 크레딧 이름. nullable `varchar(120)`이며 검증 결과와 무관하게 생성·수정·조회 왕복 시 보존한다.
- `portfolio_verification.provider_person_name`: KOBIS에서 실제 이름이 일치한 참여자 이름. 미일치·오류 시 null일 수 있다.
- `portfolio_item.role_name`: 사용자 입력 역할.
- `portfolio_verification.provider_role_name`: KOBIS에서 실제 매칭된 역할.
- `verificationStatus`: `VERIFIED`, `NOT_VERIFIED`, `AMBIGUOUS`, `ERROR`.
- `verified`: `verificationStatus == VERIFIED`를 Boolean으로 반환한다.

## 구현 내용

- `sql/01_schema.sql`에 `credit_name`을 추가했다.
- 기존 DB용 멱등 마이그레이션 `sql/08_portfolio_credit_name_schema.sql`을 추가했다.
- 생성·수정 mapper에 `creditName` 저장을 연결했다.
- 목록·단건·소유자 조회 모두 `creditName`, provider 영화/인물/역할/매칭 필드, 검증 상태를 동일하게 반환한다.
- KOBIS 검증 기본 행에서 사용자 입력 이름·역할을 provider 필드에 복사하지 않도록 수정했다.
- KOBIS 예외 시 저장을 막지 않고 `ERROR` 검증 행 기록을 시도한다.
- KOBIS 선택을 해제하고 수동 포트폴리오로 저장하면 검증 행을 삭제하되 `creditName`은 본문 컬럼에 유지한다.
- 프런트 수정 폼은 `creditName`만 복원하며 `providerPersonName` fallback을 사용하지 않는다.
- 대시보드·목록·상세에 검증 상태를 표시하고 `verified === true`에만 Verified 배지를 표시한다.
- 상세 화면에서 사용자 크레딧과 KOBIS 매칭 이름·역할을 별도 항목으로 표시한다.
- UI 검증 fixture의 사용자 입력 크레딧을 `credit_name`에 명시했다.

## 상태별 보존 결과

| 상태 | creditName | providerPersonName | 배지 |
|---|---|---|---|
| `VERIFIED` | 보존 | 실제 매칭 이름 | 표시 |
| `NOT_VERIFIED` | 보존 | null 가능 | 미표시, `검증되지 않음` |
| `AMBIGUOUS` | 보존 | 이름 일치 시 실제 이름 | 미표시, `확인 필요` |
| `ERROR` | 보존 | null | 미표시, `검증 오류` |

## 변경 파일

- `backend/src/main/java/com/slate/profiles/ProfileService.java`
- `backend/src/main/java/com/slate/profiles/PortfolioVerificationService.java`
- `backend/src/main/resources/mappers/ProfileMapper.xml`
- `backend/src/test/java/com/slate/profiles/ProfileMapperPortfolioVerificationContractTest.java`
- `backend/src/test/java/com/slate/profiles/ProfileServicePortfolioCreditTest.java`
- `backend/src/test/java/com/slate/profiles/PortfolioVerificationServiceTest.java`
- `backend/src/test/java/com/slate/profiles/PortfolioCreditSchemaContractTest.java`
- `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/styles/slate.css`
- `sql/01_schema.sql`
- `sql/07_seed_verified_portfolio_ui_demo.sql`
- `sql/08_portfolio_credit_name_schema.sql`
- `docu/07_database/database_baseline.md`

## 검증 결과

- 관련 테스트 12개: 통과
- 전체 `mvn test`: 69개 통과, failures/errors/skipped 0
- `npm run build`: 통과
- `xmllint --noout backend/src/main/resources/mappers/ProfileMapper.xml`: 통과
- `git diff --check`: 통과
- schema/migration 정적 계약 테스트: 통과
- 로컬 MySQL에 `08_portfolio_credit_name_schema.sql` 2회 적용: 통과
- 적용 후 `portfolio_item.credit_name` 컬럼 수 1, 임시 migration procedure 수 0 확인
- Mockito는 현재 JVM의 attach 제한을 피하도록 테스트 전용 `mock-maker-subclass`를 사용했다. 신규 라이브러리는 추가하지 않았다.

## 미수행 검증과 남은 이슈

- 최초 검증 때는 로컬 MySQL 연결 정보와 실행 상태를 확인하지 못했으나, 이후 저장된 `slate-admin` login-path로 migration 실제 적용·재실행을 완료했다.
- 인증된 실제 POST/PUT API와 저장 후 응답 왕복 검증은 미수행이다.
- 내장 브라우저 연결이 실행 환경 메타데이터 오류로 시작되지 않아 desktop/390x844 UI, Network payload, console 검증은 미수행이다.
- 현재 KOBIS API key 유무와 관계없이 단위 테스트로 네 상태를 검증했으며, 실제 참여자가 아닌 기존 항목을 강제로 Verified 처리하지 않았다.
- 기존 유실 데이터는 추측하거나 backfill하지 않았다.

## 후속 실제 KOBIS 확인

- 2026-06-22 16:05 기준 portfolio item 5 `역린`의 사용자 입력은 `creditName=이재규`, `roleName=감독`으로 보존됐다.
- 저장된 KOBIS 원본 응답의 감독 목록은 `이재규`이며 matcher 결과도 `providerPersonName=이재규`, `providerRoleName=감독`, `matchedSource=DIRECTOR`, `verificationStatus=VERIFIED`로 확인됐다.
- 사용자가 제공한 `검증되지 않음` 화면은 이 재검증 이전 또는 화면 갱신 이전 상태다. 최신 데이터 조회 후에는 Verified 대상이다.

## 실행 명령

```text
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home mvn test
npm run build
xmllint --noout backend/src/main/resources/mappers/ProfileMapper.xml
git diff --check
/usr/local/mysql/bin/mysql --login-path=slate-admin slate < sql/08_portfolio_credit_name_schema.sql
```
