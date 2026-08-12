# 공모전 실제 데이터·이미지·적합도 UI 수정 로그

- 작업일: 2026-06-23
- 기준 프롬프트: `docu/prompt/contest_data_ui_image_fit_fixer_prompt.md`
- 범위: 공모전 DB migration, backend, frontend, 테스트, 실제 브라우저 검증

## 변경 내용

1. `ContestView.vue`의 샘플 공모전, 가짜 수치, 추천 hero, 목록 fit gauge와 샘플 상세 분기를 제거했다.
2. `/contests`를 실제 OPEN 공모전 전체의 마감일 오름차순 목록으로 바꾸고 별도 `GET /api/contests/urgent?limit=4` 결과를 하단에 표시한다.
3. 상태·유형·장르·정렬·기준 filter DOM과 query watcher를 목록 route에서 제거하고 간단 검색만 유지했다.
4. 목록/긴급 목록 이미지는 업로드 이미지, 외부 URL, 공통 default 순으로 해석하며 실패 시 default로 복구한다.
5. `contest`와 `contest_open_request`에 `representative_image_path`를 추가하고 media entity에 `CONTEST`, `CONTEST_REQUEST`를 연결했다.
6. 회사 개설 요청과 승인 공모전 편집에 JPEG/PNG/WebP 파일 선택·미리보기·교체·삭제 UI를 추가했다. object URL은 교체·취소·unmount 시 해제한다.
7. 요청 승인 시 저장 경로를 새 공모전으로 이전하고 요청 참조만 해제한다. 거절 시 DB 참조를 지우고 commit 이후 실제 파일을 정리한다.
8. 상세 GET과 목록 SQL에서 fit cache 자동 노출을 제거했다. 상세는 `적합도 분석` POST 성공 후만 결과를 표시하고 기준 변경/새로고침 시 초기화한다.
9. 목록 우측의 `공모전 개설 요청`은 COMPANY에만 표시하고 목록 자기 링크와 중복 진입점을 제거했다.

## 보안·데이터 계약

- 직접 업로드는 최대 5MB JPEG/PNG/WebP만 허용하며 MIME, 확장자, 실제 signature를 모두 검증한다.
- 서버 생성 파일명과 정규화된 upload root를 사용하고 대상 소유자가 아닌 사용자의 교체·삭제를 거부한다.
- `representative_image_url`은 향후 관리자/크롤러의 외부 URL용으로 보존한다. 이번 작업에는 크롤러, HTML 파싱, 외부 이미지 다운로드, 스케줄러를 구현하지 않았다.
- 공개 목록과 긴급 목록은 fit 계산/캐시와 분리한다.

## 검증 결과

- `cd frontend && npm run build`: 통과
- `cd backend && mvn test`: 96 tests, failures 0, errors 0
- 관련 contest/media 회귀 테스트 재실행: 통과
- `sql/12_contest_image_schema.sql` 실제 MySQL 2회 연속 적용: 통과
- 컬럼 확인: 두 대상 테이블에 nullable `varchar(500)` 컬럼 각 1개
- 게스트: 목록 2건, 긴급 2건, 개설 요청 미노출, fit 미노출
- 일반 USER: 개설 요청/기업 관리 nav 미노출, 실제 목록 정상
- COMPANY: 목록 우측 개설 요청 1개 노출, 요청 작성의 직접 이미지 control 확인
- 실제 COMPANY 요청 생성·PNG 업로드·API 재조회·브라우저 재진입에서 인증 blob 이미지 표시 확인. 내부 저장 경로는 응답에 없으며 테스트 이미지/요청 레코드는 검증 후 삭제했다.
- 상세: 최초 fit 미노출, POST 성공 후 점수/이유 표시, 기준 변경과 새로고침 시 초기화
- desktop: 수평 overflow 없음, console error/warning 0건
- 390x844 목록/상세: 수평 overflow 없음, console error/warning 0건

## 남은 위험

- 외부 URL의 실제 가용성은 제공처에 의존하며 로드 실패 시 공통 default 이미지로 표시한다.
