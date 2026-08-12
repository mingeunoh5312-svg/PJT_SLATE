# 프로필 작품 검색 버튼 제거 프롬프트

```text
Slate 프로필 메인 화면(`/profile`) 오른쪽 상단 action 영역의 `작품 검색으로 추가` 버튼을 제거하세요.

작업 루트:
- `/Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate`

수정 대상:
- `frontend/src/views/ProfileView.vue`
- 필요한 경우 `frontend/src/styles/slate.css`

요구사항:
- 프로필 hero 오른쪽 상단의 `작품 검색으로 추가` 버튼만 화면에서 제거
- `프로필 수정`, `포트폴리오 추가` 버튼은 기존대로 유지
- `/profile/public-data` route와 작품 검색 기능 자체는 삭제하거나 변경하지 않기
- 버튼 제거 후 남는 여백과 정렬을 desktop 및 mobile에서 확인
- 관련 없는 코드, 기존 사용자 변경, backend와 DB는 수정하지 않기
- commit/push 금지

검증:
- `cd frontend && npm run build`
- `/profile`에서 해당 버튼이 보이지 않는지 확인
- 다른 프로필 action과 작품 검색 route가 정상 동작하는지 확인
- `git diff --check`
```
