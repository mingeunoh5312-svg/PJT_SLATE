# Generated Image Application Plan Log

## Scope

- Web ChatGPT 생성 이미지 파일을 CDD/CDV seed 데이터의 엔티티 이미지로 반영하는 절차 문서화
- 기준 문서: `docu/image_maker/slate_cdd_cdv_image_generation_prompts.md`
- 샘플 폴더: `assets/user_profile_images`
- 결과 문서: `docu/image_maker/slate_generated_image_application_plan.md`

## References Checked

- `backend/src/main/java/com/slate/media/MediaImageService.java`
- `backend/src/main/resources/mappers/MediaImageMapper.xml`
- `sql/18_seed_connected_demo_data.sql`
- `sql/21_seed_connected_demo_volume_data.sql`
- `sql/19_validate_connected_demo_data.sql`
- `sql/22_validate_connected_demo_volume_data.sql`
- `docu/work_logs/2026-06-22_fixer_profile_media_and_portfolio_display.md`
- `docu/work_logs/2026-06-23_fixer_contest_data_ui_image_fit.md`

## Findings

- DB image paths are relative to `SLATE_UPLOAD_DIR`, not directly to the repository root.
- `SLATE_UPLOAD_DIR=Slate/assets` would change the storage root for real user uploads as well as dummy images. The application plan now keeps `SLATE_UPLOAD_DIR` unchanged and copies dummy images into the configured upload root under `images/seed/...`.
- Existing image columns are:
  - `member_profile.profile_image_path`
  - `team.representative_image_path`
  - `work_item.representative_image_path`
  - `portfolio_item.thumbnail_image_path`
  - `contest.representative_image_path`
  - `contest_open_request.representative_image_path`
- Current validation SQL includes pre-image assertions that CDD/CDV upload paths should be null, so those checks must be adjusted or skipped after image path application.
- Web ChatGPT sample files are PNG files even though prompt target filenames use `.webp`; extension-only renaming is unsafe.
- 2026-06-26 implementation copied generated PNGs to `assets/images/{profile,team,work,portfolio,contest_request,contest}` and `uploads/images/seed/{profile,team,work,portfolio,contest_request,contest}`.
- `sql/27_apply_generated_dummy_images.sql` now applies image paths to CDD/CDV dummy rows with stable natural keys.
- `sql/19_validate_connected_demo_data.sql` and `sql/22_validate_connected_demo_volume_data.sql` now validate generated image paths instead of requiring null upload paths.

## Commands Run

```powershell
Get-ChildItem -Path .\Slate\assets -Recurse -File
rg -n "profile.*image|image.*url|thumbnail_url|representative_image|avatar|asset|assets|webp|profile_image" .\Slate
rg -n "CREATE TABLE.*member_profile|CREATE TABLE.*team|CREATE TABLE.*portfolio_item|CREATE TABLE.*work_item|CREATE TABLE.*contest|profile_image|representative_image_url|thumbnail_url|image_url" .\Slate\sql .\Slate\backend .\Slate\frontend
Get-ChildItem -Path .\Slate\assets\user_profile_images -File | Sort-Object Name
rg -n "^[0-9]+\. .*\.(webp|png|jpg|jpeg)" .\Slate\docu\image_maker\slate_cdd_cdv_image_generation_prompts.md
```

## Remaining Work

- CDV seed reset이 필요한 경우 사용자 승인 후 재실행한다. 현재 이미지 경로 적용과 이미지 경로 검증은 완료됐다.
