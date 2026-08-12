# 홈 대시보드 3단계: 통합 검증·수정 프롬프트

## 사용 목적

홈 대시보드 1·2단계 결과를 실제 계정과 브라우저에서 검증하고, 발견한 문제를 범위 안에서 수정한다.

## 프롬프트

```text
당신은 Slate 홈 대시보드 통합 검증 및 수정 담당자입니다.

선행 조건:
- `home_dashboard_01_data_structure_prompt.md` 완료
- `home_dashboard_02_ui_responsive_prompt.md` 완료

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

먼저 두 단계의 작업 로그와 실제 diff를 읽으세요. 문서에 적힌 완료 상태를 그대로 믿지 말고 코드와 화면으로 재검증하세요.

## 검증 순서

### 1. 정적 검토

- HomeView와 home components에서 하드코딩 사용자·수치·추천 데이터 검색
- 비로그인 분기에서 인증 API 호출 여부 확인
- USER 외 계정에서 USER API 호출 여부 확인
- route와 실제 router name/path 일치 확인
- 날짜 및 D-day 계산 경계 확인
- 이전 요청이 사용자 전환 후 상태를 덮을 가능성 확인
- 카드 링크와 내부 버튼 중첩 여부 확인

### 2. 빌드

```bash
cd frontend
npm run build
```

### 3. 실제 브라우저 시나리오

비로그인:

- 랜딩 히어로와 가입·로그인 CTA
- 공개 공모전과 작업물
- 인증 API 401 요청이 발생하지 않음

USER:

- nickname 개인화 문구
- 참여 팀·초대·알림·일정 수치가 API와 일치
- 지금 확인할 활동의 route
- 마감 공모전 정렬과 저장 토글
- 최근 작업물 상세 이동
- 로그아웃 후 개인 데이터 제거

COMPANY/ADMIN:

- USER 활동 수치가 노출되지 않음
- 계정별 CTA 유지
- 공개 섹션 정상 표시

오류/빈 상태:

- 한 API 실패가 다른 섹션을 가리지 않음
- 팀, 초대, 일정, 공모전, 작업물 0건 상태
- 재시도 동작

반응형:

- 데스크톱
- 태블릿
- 390x844 모바일
- document 가로 overflow 없음
- dialog, notification panel, sidebar와 홈이 충돌하지 않음

### 4. 회귀 검증

- 팔로우/팔로잉 UI가 유지됨
- 매칭 저장·초대·지원 유지
- 공모전 저장·상세 유지
- 게시판 상세 유지
- 상단 알림 패널 유지
- 로그인 redirect 유지

## 수정 원칙

검증 중 발견한 홈 관련 문제는 직접 수정하고 다시 검증하세요.
관련 없는 기존 문제는 임의로 넓혀 고치지 말고 남은 이슈로 기록하세요.
테스트를 위해 생성한 저장 관계나 데이터는 가능한 범위에서 원상 복구하세요.

## 금지 사항

- 새 홈 기능 추가
- 팔로우 활동 피드 추가
- 추천 매칭 홈 노출
- 검증 편의를 위한 운영 코드 목업 삽입
- DB reset 또는 사용자 데이터 파괴
- 브라우저 미검증을 검증 완료로 기록

## 완료 보고

- 발견한 문제와 수정 내용
- 계정별 검증 결과
- 실제 API 수치 대조 결과
- desktop/tablet/mobile 결과
- build 결과
- console/network 오류
- 수행하지 못한 항목과 이유
- 남은 리스크

결과는 `docu/work_logs/YYYY-MM-DD_home_dashboard_validation.md`에 기록하세요.
```

