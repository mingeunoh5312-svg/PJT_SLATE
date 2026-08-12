# 포트폴리오 크레딧 저장·조회·KOBIS 검증 수정 프롬프트

```text
Slate 포트폴리오에서 사용자가 입력한 크레딧 이름이 저장 후 사라지고 상세 화면에 `-`로 표시되는 문제를 직접 수정하세요. 설명만 하지 말고 구현, DB 반영, 테스트, API·브라우저 검증, 작업로그까지 완료하세요.

작업 루트:
- `/Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate`

먼저 확인:
- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/07_database/database_baseline.md`
- `docu/work_logs/2026-06-22_fixer_verified_portfolio_badge.md`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/services/api.js`
- `backend/src/main/java/com/slate/profiles/ProfileController.java`
- `backend/src/main/java/com/slate/profiles/ProfileService.java`
- `backend/src/main/java/com/slate/profiles/PortfolioVerificationService.java`
- `backend/src/main/java/com/slate/profiles/KobisRoleMatcher.java`
- `backend/src/main/resources/mappers/ProfileMapper.xml`
- `sql/01_schema.sql`

## 확인된 원인

1. 프런트는 `creditName`을 payload에 포함하지만 `portfolio_item`에는 사용자 입력 크레딧 이름을 저장할 컬럼이 없다.
2. 백엔드는 검증 직전 `baseVerification()`에서 사용자 입력 이름을 `providerPersonName`에 임시로 넣는다.
3. KOBIS에서 이름이 일치하지 않으면 `KobisRoleMatcher`가 `providerPersonName=null`인 `NOT_VERIFIED`를 반환한다.
4. `PortfolioVerificationService.verifyAfterSave()`가 기존 사용자 입력값을 이 null 매칭 결과로 덮어써 이름이 유실된다.
5. 포트폴리오 목록·단건·소유자 조회는 `provider_person_name`이나 별도 `creditName`을 반환하지 않는다.
6. 프런트 상세는 `selectedPortfolioItem.creditName`, 수정 폼 복원은 `item.creditName || item.providerPersonName`을 사용하므로 현재 응답에서는 이름이 항상 비거나 재수정 시 다시 유실될 수 있다.
7. 로컬 portfolio item ID 5의 `raw_response_json`이 존재하므로 크레딧·역할 입력 후 KOBIS 상세 호출까지 진행됐지만, 불일치 결과가 입력 이름을 null로 덮어쓴 사례가 확인됐다.

## 목표 데이터 계약

사용자 입력과 외부 검증 결과를 분리하세요.

- `creditName`: 사용자가 포트폴리오에 입력한 크레딧 이름. 검증 성공·실패와 관계없이 저장하고 등록→조회→수정 왕복 시 보존한다.
- `providerPersonName`: KOBIS 응답에서 실제로 매칭된 참여자 이름. 매칭되지 않으면 null일 수 있다.
- `roleName`: 사용자가 입력한 역할.
- `matchedRoleName` 또는 현재 동등 필드: KOBIS에서 매칭된 역할.
- `verificationStatus`: `VERIFIED`, `NOT_VERIFIED`, `AMBIGUOUS`, `ERROR` 중 현재 검증 결과.
- `verified`: `verificationStatus == VERIFIED`의 명시적 Boolean.

`providerPersonName`에 사용자 입력을 대신 저장하거나, KOBIS 불일치 시 사용자 입력을 null로 덮어쓰지 마세요.

## 구현 요구사항

### DB

- 사용자 입력 `creditName`을 `portfolio_item`에 nullable 컬럼으로 저장하는 방향을 우선 적용하세요.
- `sql/01_schema.sql`의 신규 설치 schema와 기존 로컬 DB에 적용할 멱등 migration SQL을 함께 작성하세요.
- 컬럼명, 길이, mapper property를 한 계약으로 통일하세요.
- 기존 유실 데이터의 이름을 추측해 backfill하지 마세요.
- `portfolio_verification.provider_person_name`은 KOBIS 매칭 결과 용도로 유지하세요.
- fixture SQL은 실제 KOBIS 검증 데이터로 오해되지 않도록 기존 표기를 유지하고 필요한 컬럼만 계약에 맞게 조정하세요.

### Backend

- 생성·수정 시 `PortfolioItemRequest.creditName`을 포트폴리오 본문 데이터와 함께 저장하세요.
- 목록, 단건, 소유자 조회 모두 사용자 입력 `creditName`을 반환하세요.
- `providerPersonName`, `providerRoleName` 등 외부 매칭 필드는 별도 응답 필드로 유지하세요.
- KOBIS 매칭 실패, 모호, API 오류가 발생해도 `creditName`은 보존하세요.
- KOBIS 영화 선택 해제나 수동 포트폴리오 전환 시 검증 행 처리와 사용자 입력 보존/초기화 정책을 명확히 하고 테스트하세요.
- `verified`는 JSON Boolean으로 유지하세요.
- 검증 실패를 포트폴리오 저장 실패로 바꾸지 말고 기존 비차단 정책을 유지하세요.

### Frontend

- 상세 화면의 `크레딧`에는 사용자 입력 `creditName`을 표시하세요.
- 수정 화면 진입 시 저장된 `creditName`을 정확히 복원하세요.
- KOBIS 매칭 이름과 사용자 입력 이름을 같은 값으로 오인하지 마세요.
- 검증 상태를 사용자에게 구분해 표시하세요. 최소한 미검증 항목이 단순 누락처럼 보이지 않도록 `검증되지 않음`, `확인 필요`, `검증 오류` 등 현재 상태에 맞는 문구를 제공하세요.
- `Verified` 배지는 `verified === true`인 항목에만 표시하세요.
- 기존 포트폴리오 등록·수정·삭제, KOBIS 영화 검색/선택, 대시보드·목록·상세 route를 보존하세요.

## 필수 테스트

1. 사용자 입력 이름과 역할이 KOBIS 크레딧과 일치: `creditName` 보존, provider 이름·역할 저장, `VERIFIED`, `verified=true`.
2. 이름 불일치: `creditName` 보존, provider 이름 null 가능, `NOT_VERIFIED`, `verified=false`.
3. 이름 일치·역할 불일치: 입력 보존, 현재 matcher 계약에 따른 `AMBIGUOUS`, 배지 미표시.
4. KOBIS API 오류: 입력 보존, `ERROR`, 포트폴리오 저장 자체는 성공.
5. 수정 화면 재진입: 기존 `creditName` 복원 후 다른 필드만 수정해도 이름 유지.
6. 목록·단건·소유자 조회 세 경로가 같은 credit/verification 필드를 반환.
7. migration SQL 재실행 시 오류·중복 변경 없음.

## 실제 검증

- 테스트용 계정에서 KOBIS 영화 선택 → 역할·크레딧 이름 입력 → 저장 → 상세 확인 → 수정 재진입 → 재저장 흐름을 확인하세요.
- 브라우저 Network에서 PUT/POST payload에 `creditName`이 포함되고 응답에도 동일 값이 존재하는지 확인하세요.
- DB에서 `portfolio_item`의 사용자 입력 이름과 `portfolio_verification`의 provider 매칭 이름·상태를 각각 확인하세요.
- 검증 성공과 실패 항목 모두 확인하되, 실제 참여자가 아닌 사용자를 Verified로 강제 변경하지 마세요.
- desktop과 390x844에서 상세·수정 화면 overflow 및 console error를 확인하세요.

## 실행 명령

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run build
```

- mapper XML 문법 검사
- migration SQL 신규 적용 및 재실행 검사
- `git diff --check`

## 금지사항

- 사용자 입력 이름을 KOBIS 매칭 이름으로 간주하는 임시 fallback 금지
- 검증 실패 항목을 강제로 `VERIFIED` 처리 금지
- 기존 portfolio item ID 5의 유실된 이름 추측·직접 복원 금지
- 실제 API key, DB 비밀번호, JWT 또는 `.env` 값 출력·문서화 금지
- 관련 없는 리팩터링, 기존 사용자 변경 되돌리기, 새 라이브러리 설치, commit/push 금지
- `../prototype*` 원본 수정 금지

## 완료 보고

`docu/work_logs/YYYY-MM-DD_fixer_portfolio_credit_roundtrip.md`에 다음을 기록하세요.

- 원인과 최종 데이터 계약
- schema/migration 변경 내용
- API 요청·응답 필드
- VERIFIED/NOT_VERIFIED/AMBIGUOUS/ERROR별 보존 결과
- 변경 파일
- 전체 test/build/SQL/API/브라우저 검증 결과
- 미수행 검증과 남은 이슈
```
