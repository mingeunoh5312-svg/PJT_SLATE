# 프로필 미디어 및 포트폴리오 표시 수정 작업 로그

## 작업 범위

- `/profile` 프로필 이미지 업로드·교체·삭제
- 참여 팀, 작업물, 포트폴리오 대표 이미지 표시
- 팀·작업물·포트폴리오 대표 이미지 업로드 UI
- 포트폴리오 직접 업로드 이미지와 YouTube 썸네일 선택 복원
- 포트폴리오 검증 배지와 출처 표시 정리
- 이미지 저장용 DB 스키마, API, 권한 및 파일 검증 추가

## 원인과 최종 동작

기존 화면은 프로필, 팀, 작업물 카드에 고정 또는 순환 placeholder를 사용했고, 포트폴리오는 직접 업로드 이미지를 영속 저장할 필드와 API가 없었다. 포트폴리오 카드에는 검증되지 않은 상태 배지와 내부 `sourceType` 값도 노출될 수 있었다.

수정 후 이미지는 서버 파일 저장소에 보관하고 DB에는 서버 내부 상대 경로만 저장한다. 브라우저의 blob URL은 저장 전 미리보기에만 사용하며 base64 및 localStorage에는 이미지를 저장하지 않는다. 화면은 실제 이미지 URL을 우선 사용하고, 없거나 로드에 실패하면 기존 기본 이미지를 표시한다.

## 저장 구조와 스키마

- 서버 저장 위치: `uploads/images/{entityType}/{year}/{month}/{uuid}.{extension}`
- DB 경로 필드:
  - `member_profile.profile_image_path`
  - `team.representative_image_path`
  - `work_item.representative_image_path`
  - `portfolio_item.thumbnail_image_path`
- 신규 설치 스키마: `sql/01_schema.sql`
- 기존 DB 멱등 마이그레이션: `sql/09_entity_image_schema.sql`

기존 `file_metadata`는 게시글 첨부 및 영상 메타데이터 중심 계약이므로 엔티티 대표 이미지의 1:1 소유권과 교체 의미를 직접 표현하기 어렵다. 이번 작업에서는 각 엔티티에 nullable 경로 컬럼을 두어 조회와 교체 책임을 명확히 했다.

## API와 응답 필드

- `POST /api/media/images/{entityType}/{entityId}`: 이미지 등록 또는 교체
- `DELETE /api/media/images/{entityType}/{entityId}`: 이미지 삭제
- `GET /api/media/images/{entityType}/{entityId}`: 이미지 스트리밍
- 지원 타입: `PROFILE`, `TEAM`, `WORK`, `PORTFOLIO`
- 응답 nullable 필드:
  - 프로필 `profileImageUrl`
  - 팀 `imageUrl`
  - 작업물 `representativeImageUrl`
  - 포트폴리오 `uploadedThumbnailUrl`

조회 응답은 이미지가 없을 때도 해당 키를 유지한다. 프로필, 팀, 게시판, 매칭, 팔로우 mapper와 service 응답을 같은 계약으로 맞췄다.

## 검증과 권한

- 최대 크기 5 MB
- 확장자 및 MIME: JPEG, PNG, WebP만 허용
- 확장자와 MIME 일치 확인
- JPEG, PNG, WebP 파일 시그니처 확인
- 서버가 UUID 파일명을 생성하며 사용자 파일명을 저장 경로에 사용하지 않음
- 프로필은 본인, 팀은 팀장, 작업물은 작성자, 포트폴리오는 프로필 소유자만 변경 가능
- 공개 엔티티만 비로그인 이미지 조회를 허용하고 비공개 엔티티는 소유권 또는 공개 범위를 확인

교체는 DB 갱신 성공 후 기존 파일을 삭제하고, 트랜잭션 롤백 시 새 파일을 삭제한다. 삭제도 DB 반영 성공 후 실제 파일을 제거한다. 감사 로그에는 경로 대신 성공 여부만 남긴다.

## 프런트 표시 규칙

- 프로필: 실제 프로필 이미지, 없으면 이름 이니셜
- 참여 팀: `imageUrl`, 없으면 기존 팀 placeholder
- 작업물: 업로드 대표 이미지 > YouTube 썸네일 > 기존 작업물 placeholder
- 포트폴리오: 업로드 이미지 > YouTube 썸네일 > 기존 포트폴리오 placeholder
- 이미지 로드 실패 시 각 영역의 기본 이미지로 복구
- object URL은 파일 재선택, 폼 초기화, 컴포넌트 해제 시 revoke

포트폴리오 폼은 직접 업로드와 YouTube 썸네일을 구분한다. YouTube URL은 기존 미리보기 API로 확인하며 문자열을 프런트에서 임의 변환하지 않는다. 업로드 이미지에서 YouTube 또는 미설정으로 변경하면 기존 업로드 이미지를 삭제한다.

검증 배지는 `verified === true`일 때만 `Verified`로 표시한다. 미검증 상태 배지는 제거했고 `PUBLIC_DATA_MANUAL` 같은 내부 `sourceType` 원문은 화면에 노출하지 않는다.

## 변경 파일

- `backend/src/main/java/com/slate/media/*`
- `backend/src/main/resources/mappers/MediaImageMapper.xml`
- `backend/src/main/java/com/slate/security/SecurityConfig.java`
- 프로필·팀·게시판 service 및 관련 mapper
- `frontend/src/services/api.js`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/TeamsView.vue`
- `frontend/src/views/BoardView.vue`
- `frontend/src/components/follows/FollowListDialog.vue`
- `frontend/src/styles/slate.css`
- `sql/01_schema.sql`
- `sql/09_entity_image_schema.sql`
- `docu/07_database/database_baseline.md`

## 검증 결과

| 항목 | 결과 |
|---|---|
| 백엔드 테스트 | `mvn test`: 74개 통과, failures/errors/skipped 0 |
| 프런트 빌드 | `npm run build` 통과 |
| SQL 마이그레이션 | 실제 MySQL에 2회 실행, 대상 컬럼 4개 및 임시 procedure 제거 확인 |
| 실제 API 업로드/조회/삭제 | PNG 업로드, `200 image/png` 조회, 삭제 후 DB NULL 및 파일 0건 확인 |
| 권한 | 비소유 사용자의 팀 이미지 업로드가 403으로 차단됨을 확인 |
| API nullable 계약 | 프로필·팀·작업물·포트폴리오 이미지 키 존재 확인 |
| 파일 검증 테스트 | JPEG/PNG/WebP 허용, 위장 파일·초과 크기·비소유자 차단 확인 |
| 정합성 테스트 | 이미지 교체 및 삭제 시 DB 경로와 실제 파일 정리 확인 |

## 미검증 및 제약

- 브라우저 도구 초기화가 `codex/sandbox-state-meta missing sandboxPolicy` 오류로 실패하여 desktop, 390x844, 콘솔 오류 및 실제 YouTube 선택 흐름은 브라우저에서 재검증하지 못했다. 컴파일과 API 단위 검증은 완료했다.
- 팀원이 작성한 팀 작업물은 승인 전에는 아직 게시글 ID가 없어 대표 이미지를 연결할 수 없다. 현재 승인 대기 폼에서는 이미지 선택을 숨기며, 승인 완료 후 게시글 수정에서 등록할 수 있다. 승인 요청 단계부터 이미지가 필요하면 별도 임시 업로드 계약이 필요하다.
- 보호된 이미지 GET은 인증을 확인하지만 일반 `<img>` 요청에는 Bearer 헤더가 붙지 않는다. 비공개 엔티티의 소유자 화면까지 항상 실제 이미지를 보여주려면 인증 blob loader 또는 서명 URL 방식이 추가로 필요하다.
- 소프트 삭제된 엔티티는 복구 가능성을 위해 이미지 경로와 파일을 즉시 제거하지 않는다. 영구 삭제 정책이 생기면 별도 정리 작업을 연결해야 한다.
