# Slate Generated Image Application Plan

## Purpose

Web ChatGPT가 생성한 이미지 파일을 CDD/CDV seed 데이터의 실제 엔티티 이미지로 반영하는 절차를 정의한다. 대상 엔티티는 `member_profile`, `team`, `work_item`, `portfolio_item`, `contest`, `contest_open_request`다.

이 문서는 `docu/image_maker/slate_cdd_cdv_image_generation_prompts.md`의 numbered item 순서와 생성 파일명 시간 순서가 같다는 전제를 사용한다.

## Storage Model

생성 원본과 앱 서빙 경로를 분리한다.

| 단계 | 위치 | 역할 |
| --- | --- | --- |
| Raw input | `assets/images/raw/prompt_XX_*` 또는 현재 임시 폴더 | Web ChatGPT 다운로드 원본 보관 |
| Canonical image pack | `assets/images/{profile,team,work,portfolio,contest,contest_request}` | 프롬프트 target stem 기반으로 정리한 더미 이미지 원본 |
| Runtime upload copy | `uploads/images/seed/{profile,team,work,portfolio,contest,contest_request}` 또는 현재 `SLATE_UPLOAD_DIR` 아래 같은 상대 경로 | 백엔드 `GET /api/media/images/{type}/{id}`가 실제로 읽는 파일 |

백엔드의 DB 경로는 `SLATE_UPLOAD_DIR` 기준 상대 경로다. 기본 설정은 `uploads`이며, 실제 사용자 업로드도 같은 root를 사용한다.

따라서 더미 이미지 적용을 위해 전역 `SLATE_UPLOAD_DIR=Slate/assets`를 설정하지 않는다. 이 설정은 `MediaImageService.upload()`의 실제 사용자 업로드 저장 위치까지 `Slate/assets/images/{entityType}/{year}/{month}/{uuid}`로 바꾸고, 이미지 교체/삭제 시 `assets` 아래 파일을 정리 대상으로 만들 수 있다. 더미 데이터만 대상으로 하는 작업에서는 기존 업로드 저장 정책을 침해한다.

권장 방식은 `SLATE_UPLOAD_DIR`를 기존값인 `uploads`로 유지하고, `assets/images/...`에 정리한 더미 이미지를 적용 시점에 `uploads/images/seed/...`로 복사한 뒤 DB에는 그 상대 경로를 저장하는 것이다. 예를 들어 DB에는 `images/seed/profile/profile_cdd_hyunseo_pd.png`를 저장하고, 실제 파일은 `<SLATE_UPLOAD_DIR>/images/seed/profile/profile_cdd_hyunseo_pd.png`에 둔다.

주의: Web ChatGPT 다운로드 파일은 현재 PNG다. 실제 변환 없이 `.webp`로 확장자만 바꾸면 백엔드가 `image/webp`로 응답할 수 있으므로, 변환하지 않는 경우 최종 파일명과 DB 경로는 `.png`를 사용한다. `.webp`가 필요하면 실제 WebP 변환을 먼저 수행한다.

## Applied Files

2026-06-26 기준 생성 파일은 prompt target stem 순서대로 정리했다.

| Type | Canonical path | Runtime path | Count |
| --- | --- | --- | ---: |
| Profile | `assets/images/profile` | `uploads/images/seed/profile` | 40 |
| Team | `assets/images/team` | `uploads/images/seed/team` | 14 |
| Work | `assets/images/work` | `uploads/images/seed/work` | 37 |
| Portfolio | `assets/images/portfolio` | `uploads/images/seed/portfolio` | 66 |
| Contest request | `assets/images/contest_request` | `uploads/images/seed/contest_request` | 7 |
| Contest | `assets/images/contest` | `uploads/images/seed/contest` | 25 |

DB 적용 SQL은 `sql/27_apply_generated_dummy_images.sql`이다. CDD/CDV seed 적용 후 실행한다.

```bash
mysql --login-path=slate-admin --batch --raw slate < sql/18_seed_connected_demo_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/21_seed_connected_demo_volume_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/27_apply_generated_dummy_images.sql
mysql --login-path=slate-admin --batch --raw slate < sql/19_validate_connected_demo_data.sql
mysql --login-path=slate-admin --batch --raw slate < sql/22_validate_connected_demo_volume_data.sql
```

## Mapping Rule

1. 한 prompt block의 생성 파일을 파일명 시간 오름차순으로 정렬한다. 현재 샘플처럼 파일명에 생성 시간이 들어 있으면 `Name` 오름차순을 기준으로 한다. 파일명이 보존되지 않은 경우 `LastWriteTime, Name` 순서를 보조로 사용한다.
2. 같은 prompt block의 numbered item target filename을 위에서 아래 순서로 읽는다.
3. source index와 target index를 1:1로 매칭한다.
4. target filename의 stem은 유지하고, 확장자는 실제 파일 형식에 맞춘다.
5. canonical image pack에 복사한 뒤 manifest를 남긴다.
6. manifest에서 DB update SQL을 생성하거나 수동 실행한다.

권장 manifest 컬럼:

```text
prompt_no	order	source_name	target_stem	final_relative_path	entity_type	entity_key	db_table	db_column
```

## Entity Keys

숫자 ID는 seed 재실행 시 달라질 수 있으므로 사용하지 않는다. stable key로만 update한다.

| Entity | DB column | Stable update key |
| --- | --- | --- |
| Profile | `member_profile.profile_image_path` | CDD는 `user_account.login_id`, CDV는 `cdv-user-NN` |
| Team | `team.representative_image_path` | CDD는 정확한 `team.name`, CDV는 `[CDV] NN %` |
| Work | `work_item.representative_image_path` | CDD는 `work_item.title`, CDV 01-30은 `[CDV] 작업물 NN 제작 기록`, CDV 31-36은 `[CDV] 독립 작업물 NN` |
| Portfolio | `portfolio_item.thumbnail_image_path` | CDD는 `external_source_name='SLATE_CDD'` + `external_reference_id`, CDV는 `external_reference_id='CDV-PORT-NN-1/2'` |
| Contest request | `contest_open_request.representative_image_path` | CDD/CDV request `title` |
| Contest | `contest.representative_image_path` | CDD/CDV contest `title` |

## Profile Prompt 01 Sample

현재 `assets/user_profile_images`의 10개 샘플은 Prompt 01 순서로 다음처럼 매핑한다. 최종 확장자는 현재 원본 기준 `.png`다.

| Order | Source file | Target path | Entity key |
| --- | --- | --- | --- |
| 1 | `ChatGPT Image 2026년 6월 25일 오후 09_19_31.png` | `images/seed/profile/profile_cdd_hyunseo_pd.png` | `cdd-leader` |
| 2 | `ChatGPT Image 2026년 6월 25일 오후 09_19_39.png` | `images/seed/profile/profile_cdd_minjae_cinematographer.png` | `cdd-camera` |
| 3 | `ChatGPT Image 2026년 6월 25일 오후 09_19_41.png` | `images/seed/profile/profile_cdd_soridam_sound.png` | `cdd-sound` |
| 4 | `ChatGPT Image 2026년 6월 25일 오후 09_19_42.png` | `images/seed/profile/profile_cdd_yoon_editor.png` | `cdd-editor` |
| 5 | `ChatGPT Image 2026년 6월 25일 오후 09_19_44.png` | `images/seed/profile/profile_cdd_rin_writer.png` | `cdd-writer` |
| 6 | `ChatGPT Image 2026년 6월 25일 오후 09_19_46.png` | `images/seed/profile/profile_cdd_jun_actor.png` | `cdd-actor` |
| 7 | `ChatGPT Image 2026년 6월 25일 오후 09_19_47.png` | `images/seed/profile/profile_cdd_reviewer.png` | `cdd-reporter` |
| 8 | `ChatGPT Image 2026년 6월 25일 오후 09_19_50.png` | `images/seed/profile/profile_cdd_moderation_test.png` | `cdd-moderated` |
| 9 | `ChatGPT Image 2026년 6월 25일 오후 09_19_52.png` | `images/seed/profile/profile_cdv_01_planning_creator.png` | `cdv-user-01` |
| 10 | `ChatGPT Image 2026년 6월 25일 오후 09_19_54.png` | `images/seed/profile/profile_cdv_02_directing_creator.png` | `cdv-user-02` |

Sample update SQL:

```sql
UPDATE member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
SET mp.profile_image_path = 'images/seed/profile/profile_cdd_hyunseo_pd.png',
    mp.updated_at = NOW()
WHERE ua.login_id = 'cdd-leader';

UPDATE member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
SET mp.profile_image_path = 'images/seed/profile/profile_cdv_01_planning_creator.png',
    mp.updated_at = NOW()
WHERE ua.login_id = 'cdv-user-01';
```

## SQL Patterns

Team:

```sql
UPDATE team
SET representative_image_path = 'images/seed/team/team_cdv_01_dawn_market_short.png',
    updated_at = NOW()
WHERE name LIKE '[CDV] 01 %';
```

Work:

```sql
UPDATE work_item
SET representative_image_path = 'images/seed/work/work_cdv_01_short_film_record.png',
    updated_at = NOW()
WHERE title = '[CDV] 작업물 01 제작 기록';
```

Portfolio:

```sql
UPDATE portfolio_item
SET thumbnail_image_path = 'images/seed/portfolio/portfolio_cdv_01_producer_a.png',
    updated_at = NOW()
WHERE external_source_name = 'SLATE_CDV'
  AND external_reference_id = 'CDV-PORT-01-1';
```

Contest request:

```sql
UPDATE contest_open_request
SET representative_image_path = 'images/seed/contest_request/contest_request_cdv_01_local_short.png',
    updated_at = NOW()
WHERE title = '[CDV] 공모전 개설 요청 01';
```

Contest:

```sql
UPDATE contest
SET representative_image_path = 'images/seed/contest/contest_cdv_approved_01_local_short.png',
    updated_at = NOW()
WHERE title = '[CDV] 승인 공모전 01';
```

## Validation

Before DB update:

- Source count equals retained numbered item count for the prompt block.
- No source file is larger than 5 MB because `MediaImageService` upload policy uses the same limit.
- Final extension matches actual file content.
- Profile images are visually 1:1 and contain exactly one person.
- Non-profile representative images are visually 16:9 unless the generated prompt intentionally differs.
- Manifest has no duplicated `final_relative_path`.
- Every `entity_key` resolves to exactly one DB row.

After DB update:

```sql
SELECT 'profile', COUNT(*)
FROM member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ((ua.login_id LIKE 'cdd-%' AND ua.account_type = 'USER') OR ua.login_id LIKE 'cdv-user-%')
  AND mp.profile_image_path IS NOT NULL
UNION ALL
SELECT 'team', COUNT(*) FROM team WHERE (name LIKE '[CDD]%' OR name LIKE '[CDV]%') AND representative_image_path IS NOT NULL
UNION ALL
SELECT 'work', COUNT(*) FROM work_item WHERE (title LIKE '[CDD]%' OR title LIKE '[CDV]%') AND representative_image_path IS NOT NULL
UNION ALL
SELECT 'portfolio', COUNT(*) FROM portfolio_item WHERE external_source_name IN ('SLATE_CDD', 'SLATE_CDV') AND thumbnail_image_path IS NOT NULL
UNION ALL
SELECT 'contest', COUNT(*) FROM contest WHERE (title LIKE '[CDD]%' OR title LIKE '[CDV]%') AND representative_image_path IS NOT NULL
UNION ALL
SELECT 'contest_request', COUNT(*) FROM contest_open_request WHERE (title LIKE '[CDD]%' OR title LIKE '[CDV]%') AND representative_image_path IS NOT NULL;
```

`sql/19_validate_connected_demo_data.sql`와 `sql/22_validate_connected_demo_volume_data.sql`는 이미지 적용 후 기준으로 갱신되어 `*_generated_image_path_missing_or_invalid` 체크가 0인지 확인한다.
