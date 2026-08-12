# Generated Dummy Image Application

## Scope

- 생성 완료된 Web ChatGPT PNG 이미지를 CDD/CDV 더미 데이터용 파일 구조로 정리
- DB에는 백엔드 업로드 루트 기준 상대 경로 `images/seed/.../*.png`를 저장
- `SLATE_UPLOAD_DIR=uploads` 설정은 유지하고 실제 사용자 업로드 경로를 변경하지 않음

## Applied File Counts

| Type | Canonical assets | Runtime uploads | Count |
| --- | --- | --- | ---: |
| Profile | `assets/images/profile` | `uploads/images/seed/profile` | 40 |
| Team | `assets/images/team` | `uploads/images/seed/team` | 14 |
| Work | `assets/images/work` | `uploads/images/seed/work` | 37 |
| Portfolio | `assets/images/portfolio` | `uploads/images/seed/portfolio` | 66 |
| Contest request | `assets/images/contest_request` | `uploads/images/seed/contest_request` | 7 |
| Contest | `assets/images/contest` | `uploads/images/seed/contest` | 25 |

## Files Changed

- `sql/27_apply_generated_dummy_images.sql`
- `sql/19_validate_connected_demo_data.sql`
- `sql/22_validate_connected_demo_volume_data.sql`
- `docu/image_maker/slate_generated_image_application_plan.md`
- `docu/dummy_data/restore_guide.md`
- `docu/dummy_data/validation_result.md`
- `docu/dummy_data/volume_restore_guide.md`
- `docu/dummy_data/volume_validation_result.md`
- `docu/07_database/database_baseline.md`
- `docu/work_logs/2026-06-25_documenter_generated_image_application_plan.md`
- `docu/work_logs/2026-06-26_applier_generated_dummy_images.md`

## Verification

- Prompt target count and generated PNG count match for all image types.
- Canonical assets and runtime upload copies have matching counts.
- Validation SQL now checks missing or invalid `images/seed/.../*.png` paths instead of expecting null image paths.
- `sql/27_apply_generated_dummy_images.sql` was executed against local `slate` with `slate_app`; all image path counts matched existing target row counts.
- `sql/19_validate_connected_demo_data.sql` passed CDD zero-error checks, including `cdd_generated_image_path_missing_or_invalid = 0`.
- `sql/22_validate_connected_demo_volume_data.sql` passed `cdv_generated_image_path_missing_or_invalid = 0`. Existing local CDV interaction counts still show `board_like` 302/300 and `board_view_log` 483/480; this is outside the image-application scope and would require an explicit CDV seed reset to normalize.
