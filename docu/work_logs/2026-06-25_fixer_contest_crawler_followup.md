# 2026-06-25 수정자 작업 로그 - 공모전 크롤링 후속 보완

작성일: 2026-06-25

## 문서 작성 기준

- `Agent.md`의 작업 로그 규칙에 따라 `docu/work_logs/YYYY-MM-DD_{role}_{summary}.md` 형식으로 작성한다.
- 이번 대화에서 처리한 공모전 크롤링 이식, 관리자 공모전 관리, 공모전 이미지, 크롤링 오류 대응 작업만 기록한다.
- 실제 API key, DB 비밀번호, 로컬 secret 값은 기록하지 않는다.
- 구현 완료, 검증 완료, 미검증 항목을 분리해 기록한다.

## 작업 목적

`dev_Slate_0625_user1_v1` 현재 코드 흐름을 기준으로 하되, 공모전 크롤링 관련 동작은 `dev_Slate_0624_user2` 작업물의 의도에 맞게 다시 보완했다. 이후 관리자 페이지와 공모전 목록에서 발견된 크롤링 결과 확인성, 이미지 표시, 사진 단일 주제 제외, 포스터 저장 실패 케이스를 순차적으로 수정했다.

## 반영 내용

| 영역 | 상태 | 내용 |
|---|---|---|
| 공모전 검색 필터 | 구현 | 공모전 목록 검색 필터를 크롤링 작업물 기준의 구조화 필터 흐름에 맞췄다. |
| 외부 크롤링 실행 | 구현 | 관리자 외부 크롤링 실행 시 체크박스 선택 상태와 활성화 조건을 점검하고, 페이지 2-10 수집 누락을 보정했다. |
| 크롤링 대상 제한 | 구현 | 영상 관련 공모전 중심 수집을 유지하고, 사진 단일 주제 공모전은 제외하되 사진과 영상/영화제 키워드가 함께 있는 공모전은 제외하지 않도록 조정했다. |
| 상금 정규화 | 구현 | `1등 상금은 총상금보다 클 수 없습니다` 케이스가 크롤링 전체 실패로 이어지지 않도록 크롤링 정규화/저장 흐름을 보정했다. |
| 중복 결과 최적화 | 구현 | 기존 크롤링 결과와 주요 내용 및 포스터 정보가 동일한 경우 DB 갱신과 포스터 재다운로드를 생략할 수 있도록 `ContestKoreaUpsertService`에 사전 skip 경로를 추가했다. |
| 크롤링 결과 목록 | 구현 | 관리자 직접 크롤링 결과를 최근 20건이 아닌 전체 item 결과로 표시하고, 유형 필터와 개수 필터, 상단/하단 페이지 이동을 추가했다. |
| 공모전 선택 삭제 | 구현 | 관리자 공모전 목록에서 여러 공모전을 선택하고 삭제할 수 있는 UI와 삭제 실행 흐름을 추가했다. |
| 공모전 목록 UI | 구현 | `전체 공모전/저장한 공모전` 하단 탭을 게시판 하단 탭 디자인에 맞춰 복구했다. |
| 접수 중인 공모전 블록 | 구현 | 공모전 목록의 접수 중인 공모전 블록에 건수 위치 조정, 상단/하단 페이지 버튼, 10/20/50개 출력 선택을 추가했다. |
| 기본 공모전 이미지 | 구현 | 공모전 목록과 홈 마감 임박 공모전 모두 `contest-camera.png` 기본 이미지를 fallback으로 사용하도록 맞췄다. |
| 크롤링 포스터 이미지 저장 | 구현 | 콘테스트코리아 포스터 저장 결과가 공모전 대표 이미지로 연결되도록 유지하고, 기존 포스터 URL이 동일한 경우 재다운로드를 생략하도록 했다. |
| 관리자 직접 공모전 이미지 | 구현 | 관리자 공모전 직접 등록/수정 폼에 대표 이미지 파일 업로드와 미리보기를 추가했다. 저장 후 `CONTEST` 이미지 업로드 API를 호출한다. |
| 관리자 생성 공모전 이미지 권한 | 구현 | 관리자 직접 생성 공모전은 `requester_company_user_id`가 없으므로 `created_by`를 이미지 소유자로 인정하도록 `MediaImageMapper`를 보정했다. |
| 포스터 Content-Type 불일치 | 구현 | 포스터 저장 시 응답 `Content-Type`이 아닌 실제 파일 시그니처로 `jpg/png/webp` 확장자를 판단한다. 이미지 본문이 아니면 포스터만 건너뛰고 공모전 upsert는 계속한다. |
| 서버 재시작 | 완료 | 백엔드 서버를 변경 코드 기준으로 재시작하고 `/api/contests` 응답을 확인했다. |

## 주요 변경 경로

### Backend

- `backend/src/main/java/com/slate/contests/ContestKoreaCrawlerItemResult.java`
- `backend/src/main/java/com/slate/contests/ContestKoreaCrawlerService.java`
- `backend/src/main/java/com/slate/contests/ContestKoreaPosterStorageService.java`
- `backend/src/main/java/com/slate/contests/ContestKoreaUpsertService.java`
- `backend/src/main/resources/mappers/ContestMapper.xml`
- `backend/src/main/resources/mappers/MediaImageMapper.xml`

### Backend Tests

- `backend/src/test/java/com/slate/contests/ContestKoreaCrawlerServiceTest.java`
- `backend/src/test/java/com/slate/contests/ContestKoreaPosterStorageServiceTest.java`
- `backend/src/test/java/com/slate/contests/ContestKoreaUpsertServiceTest.java`
- `backend/src/test/java/com/slate/media/MediaImageMapperContractTest.java`

### Frontend

- `frontend/src/views/AdminView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/HomeView.vue`
- `frontend/src/styles/slate.css`

## 검증 결과

| 검증 | 결과 | 비고 |
|---|---|---|
| `npm.cmd run build` in `frontend` | 통과 | Vite chunk size 경고만 있음. |
| `mvn -Dtest=MediaImageMapperContractTest,MediaImageServiceTest test` | 통과 | 관리자 생성 공모전 이미지 권한 보정 검증. |
| `mvn -Dtest=ContestKoreaPosterStorageServiceTest,ContestKoreaUpsertServiceTest,ContestKoreaCrawlerServiceTest test` | 통과 | 40 tests, Failures 0, Errors 0. |
| `git diff --check` 대상 파일 | 통과 | CRLF 변환 경고만 있음. |
| 백엔드 재시작 | 완료 | 최종 확인 PID `16112`, `http://127.0.0.1:8080`. |
| API smoke | 통과 | `GET /api/contests?status=OPEN&sort=deadline&limit=1` HTTP 200. |
| 실제 콘테스트코리아 10페이지/100건 live run | 미검증 | 외부 네트워크와 실제 운영 데이터 의존. 관리자 화면에서 재실행 확인 필요. |

## 이슈와 보정 메모

- 포스터 응답이 `image/png`로 오지만 실제 본문은 JPEG이거나, 반대로 Content-Type이 잘못된 외부 응답이 있을 수 있다. 현재 구현은 실제 파일 시그니처를 신뢰해 저장한다.
- 포스터 본문이 HTML 오류 페이지 등 이미지가 아닌 경우, 기존에는 `UPSERT` 실패로 크롤링 결과가 실패 처리됐다. 현재는 포스터만 미저장 처리하고 공모전 데이터 등록/갱신은 계속한다.
- 관리자 직접 생성 공모전의 대표 이미지 업로드는 `created_by`를 소유자 기준으로 사용한다. 회사가 요청한 공모전은 기존처럼 `requester_company_user_id`가 우선이다.
- 크롤링 원본 공모전 중 소유자가 없는 데이터는 일반 미디어 업로드 권한을 임의로 열지 않았다.
- 중복 결과 skip은 기존 DB 내용과 크롤링 row가 동일하고 기존 포스터가 이미 있는 경우에만 포스터 재다운로드까지 생략한다.

## 남은 확인 사항

1. 관리자 페이지에서 외부 크롤링 `페이지 10`, `건수 100`, `dry-run 해제` 조건으로 실제 수집 결과를 다시 확인한다.
2. `202606040040` 같은 포스터 Content-Type 불일치 케이스가 실패가 아닌 등록/갱신 또는 포스터 미저장 처리로 표시되는지 확인한다.
3. 관리자 공모전 직접 등록에서 대표 이미지를 업로드한 뒤 홈 마감 임박 공모전과 공모전 목록/상세에 동일하게 노출되는지 브라우저로 확인한다.
4. 사진 단일 주제 제외 규칙이 실제 목록에서 과도하게 배제하지 않는지, 사진+영상 혼합 공모전 샘플로 확인한다.
5. 관리자 공모전 다중 선택 삭제는 실제 운영 데이터가 아닌 테스트 데이터에서 먼저 확인한다.

## 참조 경로

- `Agent.md`
- `docu/00_common/document_structure.md`
- `docu/work_logs/2026-06-24_fixer_user2_crawler_demo_access_port.md`
- `docu/handoff/user2_crawler_and_filter_port_2026-06-24.md`
- `backend/src/main/java/com/slate/contests`
- `backend/src/main/resources/mappers/ContestMapper.xml`
- `backend/src/main/resources/mappers/MediaImageMapper.xml`
- `frontend/src/views/AdminView.vue`
- `frontend/src/views/ContestView.vue`
- `frontend/src/views/HomeView.vue`
- `frontend/src/styles/slate.css`
