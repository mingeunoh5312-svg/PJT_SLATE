# 공통·메인·매칭·팀 TODO 이미지 준비 목록

작성일: 2026-06-23

## 기준

- 임의 기본 이미지는 코드에 추가하지 않는다.
- 아래 파일은 작업자가 추후 준비해 업로드하거나 저장한다.
- 실제 서비스 기본 이미지로 연결하기 전까지는 기존 텍스트/이니셜/무이미지 상태를 유지한다.

## 필요 이미지

| 용도 | 권장 저장 경로 | 권장 형식 | 비고 |
| --- | --- | --- | --- |
| 비로그인 메인 카드 배경 | `frontend/src/assets/home/main-card/guest-main.webp` | WebP, 1600x900 이상 | `Slate에서 제작을 시작하는 방법` 흐름과 어울리는 제작 현장 이미지 |
| 로그인 사용자 메인 카드 배경 | `frontend/src/assets/home/main-card/user-dashboard.webp` | WebP, 1600x900 이상 | 제작 일정과 팀 활동을 암시하는 이미지 |
| 회사 계정 메인 카드 배경 | `frontend/src/assets/home/main-card/company-dashboard.webp` | WebP, 1600x900 이상 | 공모전 운영 또는 작품 검토 맥락 |
| 관리자 계정 메인 카드 배경 | `frontend/src/assets/home/main-card/admin-dashboard.webp` | WebP, 1600x900 이상 | 운영 대시보드 맥락, 민감 정보 없는 이미지 |
| 공모전 기본 대표 이미지 | `frontend/src/assets/contests/default-contest.webp` | WebP, 1200x675 이상 | 업로드 이미지가 없는 공모전용. 등록 전까지 기본 이미지는 노출하지 않음 |
| 작업물 기본 썸네일 | `frontend/src/assets/boards/default-work.webp` | WebP, 1200x675 이상 | 업로드 이미지와 YouTube 썸네일이 모두 없을 때 사용할 후보 |
| 팀 기본 대표 이미지 | `frontend/src/assets/teams/default-team.webp` | WebP, 1200x675 이상 | 팀 대표 이미지가 없을 때 사용할 후보. 등록 전까지 이니셜 placeholder 유지 |

## 연결 시 확인

- 메인 카드 배경은 관리자 등록/수정 API가 확정된 뒤 URL 기반 설정으로 연결한다.
- 공모전/작업물/팀 기본 이미지는 업로드 이미지보다 낮은 우선순위로만 사용한다.
- 작업물 게시판 썸네일은 바로가기 성격이므로 이미지 확대 기능을 연결하지 않는다.
- 프로필 사진, 팀 대표 이미지, 공모전 이미지는 실제 이미지가 있을 때만 확대 기능을 노출한다.
