# 2차 볼륨 더미 데이터 테스트 계정

작성일: 2026-06-25
대상 DB: `slate`

## 공통 비밀번호

모든 CDV 테스트 계정의 비밀번호:

`slate1234`

## 계정

| 구분 | 로그인 ID | 수량 | 상태 |
|---|---|---:|---|
| 일반 사용자 | `cdv-user-01` ~ `cdv-user-32` | 32 | 대부분 `ACTIVE` |
| 회사 | `cdv-company-01` ~ `cdv-company-04` | 4 | `ACTIVE` |
| 관리자 | `cdv-admin` | 1 | `ACTIVE` |

운영·제재 화면 검증을 위해 다음 계정은 임시 정지 상태다.

- `cdv-user-25`
- `cdv-user-26`

로그인과 일반 화면 확인에는 `cdv-user-01`부터 `cdv-user-24`, 또는 `cdv-user-27`부터 `cdv-user-32`를 사용하는 것이 편하다.

## 추천 확인 계정

- 일반 사용자·팀장: `cdv-user-01`
- 일반 사용자·다른 팀 상태: `cdv-user-05`, `cdv-user-11`
- 회사 화면: `cdv-company-01`
- 관리자 화면: `cdv-admin`
- 정지 계정 동작: `cdv-user-25`
