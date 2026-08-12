# Slate

영화·창작 산업 종사자를 위한 포트폴리오·매칭·공모전 플랫폼.
배우·감독·스태프의 **경력(크레딧)** 을 KOBIS(영화진흥위원회) 데이터와 대조해 검증하고,
팀 매칭·공모전·커뮤니티·AI 촬영지 추천까지 한 곳에서 제공한다.

> SSAFY 공통(관통) 프로젝트 결과물. 실제 시크릿·운영 값은 저장소에 포함되지 않으며 모든 `.env`는 예시(`CHANGE_ME`)만 담는다.

## 주요 기능

| 도메인 | 설명 |
|---|---|
| 계정·인증 (`accounts`, `security`) | JWT 기반 인증/인가, 데모 접근 게이트 |
| 프로필·포트폴리오 (`profiles`) | 경력·크레딧 등록, **KOBIS 박스오피스 데이터 매칭으로 크레딧 검증(VERIFIED)** |
| 팀·매칭 (`teams`, `matching`) | 역할별 팀 슬롯, 지원/초대, 정원 조건부 슬롯 관리 |
| 공모전 (`contests`) | OPEN 목록·마감 임박, 구조화 검색 필터, 콘테스트코리아 크롤러(jsoup) |
| 게시판 (`boards`) | HOME/WORK/FREE/POPULAR, 주간·월간·전체 랭킹, 검색 |
| 팔로우 (`follows`) | 팔로우 시스템, 대시보드 |
| 미디어 (`media`) | 프로필·팀·작업물·포트폴리오 이미지 업로드/교체/삭제, 권한·파일 검증 |
| 촬영지 추천 (`locations`) | 영화 로케이션 촬영 이력(CSV) 임포트 + OpenAI 추천 + Kakao Map 시각화 |
| 운영·관리 (`admin`, `moderation`, `operations`, `notifications`) | 관리자 보드, 신고 처리, 알림 |

## 기술 스택

**Backend** — Spring Boot 4.0.6 · Java 17 · Spring MVC · Spring Security(JWT) · MyBatis 4 · MySQL 8 · Maven
외부 연동: KOBIS · YouTube Data API · OpenAI · jsoup(크롤러) · commons-csv(임포터)

**Frontend** — Vue 3.5 · Vite 8 · Vue Router 4 · Node ≥ 20.19

## 저장소 구조

```
.
├── backend/        Spring Boot API (com.slate.*)
├── frontend/       Vue 3 SPA
├── sql/            스키마 · 시드 SQL
├── assets/         데모 시드 데이터 (샘플만 포함 — assets/README.md 참고)
├── design/         화면 설계 자료
├── docu/           프로젝트 기준 문서 (요구사항·아키텍처·리뷰·배포)
├── tools/          보조 스크립트
├── Agent.md        작업 기준 문서
└── EC2_*.md        배포 운영 가이드 (민감정보 미포함, placeholder)
```

## 실행 방법

### 사전 준비
- JDK 17, Maven
- Node ≥ 20.19
- MySQL 8 (DB명 `slate`)

### Backend
```bash
cd backend
cp .env.example .env          # 값 채우기 (DB, JWT, 외부 API 키)
# .env 값을 환경변수로 주입 후:
mvn spring-boot:run           # profile: local
```
DB 스키마·시드는 `sql/` 참고. 로케이션 임포터는 `SLATE_LOCATION_IMPORT_ENABLED=true`로 활성화.

### Frontend
```bash
cd frontend
cp .env.example .env
npm install
npm run dev                   # http://localhost:5174
```

### 환경변수
서비스별 예시는 `backend/.env.example`, `frontend/.env.example` 참고.
실제 값(DB 비밀번호, JWT secret, KOBIS/YouTube/OpenAI/Kakao 키)은 로컬 환경변수 또는 배포 secret manager로만 주입한다.

## 참고

- **시드 데이터**: `assets/`의 데모 이미지는 저장소 용량을 위해 카테고리별 샘플만 포함한다. 전체 데이터셋 구성은 `assets/README.md` 참고.
- **GIS 원본 데이터**(vworld 시군구 경계 shapefile 등)는 용량·라이선스상 저장소에서 제외했다. 출처는 `assets/README.md` 참고.
