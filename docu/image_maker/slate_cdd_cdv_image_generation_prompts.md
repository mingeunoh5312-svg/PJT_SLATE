# Slate CDD/CDV Image Generation Prompts

Source scope:

- `Slate/sql/18_seed_connected_demo_data.sql`
- `Slate/sql/21_seed_connected_demo_volume_data.sql`
- Image-enabled entities from `member_profile`, `team`, `work_item`, `portfolio_item`, `contest`, and `contest_open_request`

Target scope:

- User profile portraits: up to 40 images, 4 prompts
- Team representative images: up to 14 images, 2 prompts
- Work post/work thumbnails: up to 37 images, 4 prompts
- Portfolio thumbnails: up to 66 images, 7 prompts
- Contest and contest request representative images: up to 32 images, 4 prompts
- Total: up to 189 images, 21 prompts

Paste one prompt block at a time into Web ChatGPT image generation. Each prompt asks for no more than 10 images and includes the exact image count.

Critical output rule: a numbered item means one standalone image file/canvas. If Web ChatGPT combines multiple numbered items into one image, that output is invalid and must be regenerated as separate images.

Profile prompt rule: Prompt 01 through Prompt 04 are generation queues, not a request to render all listed people together. In Web ChatGPT, submit exactly one retained profile item per image-generation request unless the tool can return truly separate image files. A valid profile output contains exactly one person for exactly one filename.

## DB Count Guard

Before pasting any block, cap the requested image count by the current DB count for that entity group. Web ChatGPT cannot read the DB, so adjust the prompt text before pasting it.

Use this formula for each prompt block:

```text
actual_count = min(listed_count, max(0, current_db_count - block_start_index + 1))
```

- If `actual_count` is `0`, skip the prompt block.
- If `actual_count` is smaller than the listed count, keep only numbered items `1..actual_count` in that block.
- Rewrite both `Create exactly N...` and `Generate these N...` to the capped `actual_count`.
- Do not invent extra targets, duplicate earlier images, or create placeholder images when DB rows are fewer than the listed items.

Current CDD/CDV seed baseline expected counts:

```sql
SELECT 'user_profile_portraits' AS image_group, COUNT(*) AS db_count
FROM member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ((ua.login_id LIKE 'cdd-%' AND ua.account_type = 'USER') OR ua.login_id LIKE 'cdv-user-%')
UNION ALL
SELECT 'team_representative_images', COUNT(*)
FROM team
WHERE name LIKE '[CDD]%' OR name LIKE '[CDV]%'
UNION ALL
SELECT 'work_thumbnails', COUNT(*)
FROM work_item
WHERE title LIKE '[CDD]%' OR title LIKE '[CDV]%'
UNION ALL
SELECT 'portfolio_thumbnails', COUNT(*)
FROM portfolio_item
WHERE external_source_name IN ('SLATE_CDD', 'SLATE_CDV')
UNION ALL
SELECT 'contest_and_request_representative_images',
  (SELECT COUNT(*) FROM contest WHERE title LIKE '[CDD]%' OR title LIKE '[CDV]%')
  + (SELECT COUNT(*) FROM contest_open_request WHERE title LIKE '[CDD]%' OR title LIKE '[CDV]%');
```

Block start indexes:

| Entity group | Prompt blocks |
| --- | --- |
| User profile portraits | Prompt 01 starts at 1, Prompt 02 at 11, Prompt 03 at 21, Prompt 04 at 31 |
| Team representative images | Prompt 05 starts at 1, Prompt 06 at 11 |
| Work thumbnails | Prompt 07 starts at 1, Prompt 08 at 11, Prompt 09 at 21, Prompt 10 at 31 |
| Portfolio thumbnails | Prompt 11 starts at 1, Prompt 12 at 11, Prompt 13 at 21, Prompt 14 at 31, Prompt 15 at 41, Prompt 16 at 51, Prompt 17 at 61 |
| Contest and request images | Prompt 18 starts at 1, Prompt 19 at 11, Prompt 20 at 21, Prompt 21 at 31 |

## Profile Portrait Diversity Guard

Apply this guard to Prompt 01 through Prompt 04:

- Treat each numbered item as a different database user. Do not reuse the same face, haircut, outfit, pose, lighting setup, camera angle, or background composition.
- Every profile image must be a solo portrait of exactly one human subject. Do not include a second person, background crew, crowd, extra face, duplicated face, poster face, screen face, framed-photo face, mirror/reflection face, mannequin, statue, split-panel portrait, before/after comparison, or couple/group composition.
- Use the target gender and appearance brief in each item as mandatory. If the role cue, name, or background cue conflicts with the target gender or appearance brief, keep the target gender and appearance brief and adapt the prop/background.
- Target gender is a hard visual lock: female means an adult woman presentation, and male means an adult man presentation. Do not output the opposite gender or an ambiguous gender presentation for these seed portraits.
- Do not infer gender from Korean or English names. The explicit `Target gender` label controls the output.
- Vary age band, face shape, hair length, hair texture, glasses or no glasses, outfit silhouette, posture, expression, and accent color across the 10 images in each block.
- Keep all portraits fictional Korean media creators, but avoid making them look like siblings, clones, or the same base model in different clothes.
- Use Slate blue/navy/gray as a base only. Give each person a distinct secondary accent color or material so the set does not collapse into one palette.

## Prompt 01 - User Profiles 01-10

```text
Use this block as a generation queue for exactly 10 separate profile images, one standalone image per numbered item. If this block is capped by the DB Count Guard, use exactly the capped number of retained numbered items only. In Web ChatGPT, run one retained numbered item per image-generation request; never ask it to render all retained profile items together. The output for each request must be exactly one square image for exactly one filename. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas.

Use square 1:1 composition suitable for 800x800 profile images. Each image must be a solo head-and-shoulders or upper-body portrait of exactly one person. Style: polished cinematic editorial portrait illustration for Slate, a professional film and video production collaboration platform. All people are fictional Korean media creators. Avoid celebrity likenesses, real people, real brand logos, watermarks, UI, and text. Use clean studio or location-inspired backgrounds with subtle role-specific props, but do not include any other human figures or face-like images in the background. Use Slate-friendly colors as a base: cobalt blue, deep navy, cool gray, restrained warm accents. Keep faces natural, professional, and not overly cartoonish.

Identity diversity is mandatory: each numbered item must have a clearly different face shape, hairstyle, outfit silhouette, camera angle, expression, target gender, and background palette. Do not reuse the same portrait template. Use each target gender and appearance brief as required visual constraints.

Generate these 10 profile images as separate one-item requests, or only the retained first N items if the DB Count Guard capped this block:
1. profile_cdd_hyunseo_pd.webp - CDD Hyunseo PD, producer for small night-location short films. Target gender: female. Appearance brief: late-30s woman, oval face, shoulder-length straight black hair with side part, thin rectangular glasses, navy field jacket over cream knit, calm half-smile, three-quarter view. Background: city-night planning board, cobalt and amber accents.
2. profile_cdd_minjae_cinematographer.webp - CDD Minjae cinematographer, low-light urban camera specialist. Target gender: male. Appearance brief: early-30s man, angular jaw, close-cropped hair, faint stubble, black utility vest over gray hoodie, focused expression, low side-light. Background: soft camera-rig silhouette, night street bokeh, teal accent.
3. profile_cdd_soridam_sound.webp - CDD Soridam location sound recordist, outdoor dialogue clarity specialist. Target gender: female. Appearance brief: late-20s woman, round face, short wavy bob, no glasses, olive windbreaker, attentive listening pose with one hand near headphones. Background: subtle boom mic and portable mixer shapes, muted green accent.
4. profile_cdd_yoon_editor.webp - CDD Yoon editor, emotion-driven editing and color finishing. Target gender: male. Appearance brief: early-40s man, long narrow face, medium-length swept-back hair, round metal glasses, charcoal cardigan, thoughtful downward glance. Background: soft monitor glow and abstract timeline colors without readable UI, violet-gray accent.
5. profile_cdd_rin_writer.webp - CDD Rin writer, urban mystery and relationship short-film screenwriter. Target gender: female. Appearance brief: mid-20s woman, heart-shaped face, long tied-back hair with loose bangs, tan trench-style overshirt, serious direct gaze. Background: notebook and city alley mood, warm sodium-light accent.
6. profile_cdd_jun_actor.webp - CDD Jun actor, natural performance and rehearsal focused. Target gender: male. Appearance brief: mid-20s man, soft square face, longer layered hair, clean-shaven, burgundy rehearsal sweatshirt, open expressive posture. Background: warm rehearsal-room lighting and script pages without text, copper accent.
7. profile_cdd_reviewer.webp - CDD Reviewer, work reviewer and community moderation test profile. Target gender: female. Appearance brief: early-30s woman, broad cheekbones, neat short hair, clear acetate glasses, crisp light-blue shirt, composed neutral expression, front-facing crop. Background: neutral media-review workspace, calm objective tone, pale blue accent.
8. profile_cdd_moderation_test.webp - CDD moderation test user, subdued neutral test profile for account-status screens. Target gender: male. Appearance brief: late-30s man, rounder jaw, buzz cut, no facial hair, dark gray crewneck, reserved expression, slightly off-center pose. Background: simple remote-work room, not ominous, soft graphite accent.
9. profile_cdv_01_planning_creator.webp - CDV creator 01, planning/producer role, early-career collaboration. Target gender: female. Appearance brief: early-20s woman, slim face, chin-length blunt bob, small hoop earrings, cobalt overshirt, bright alert expression, high-key studio angle. Background: Seoul production notebook and shot-planning cards, coral accent.
10. profile_cdv_02_directing_creator.webp - CDV creator 02, directing/line producer role, structured crew coordination. Target gender: male. Appearance brief: late-20s man, rectangular face, side-parted hair, black-rim glasses, beige chore jacket, confident closed-mouth smile. Background: compact set-planning space, mustard accent.
```

## Prompt 02 - User Profiles 11-20

```text
Use this block as a generation queue for exactly 10 separate profile images, one standalone image per numbered item. If this block is capped by the DB Count Guard, use exactly the capped number of retained numbered items only. In Web ChatGPT, run one retained numbered item per image-generation request; never ask it to render all retained profile items together. The output for each request must be exactly one square image for exactly one filename. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas.

Use square 1:1 composition suitable for 800x800 profile images. Each image must be a solo head-and-shoulders or upper-body portrait of exactly one person. Style: polished cinematic editorial portrait illustration for Slate, a professional film and video production collaboration platform. All people are fictional Korean media creators. Avoid celebrity likenesses, real people, real brand logos, watermarks, UI, and text. Use clean studio or location-inspired backgrounds with subtle role-specific props, but do not include any other human figures or face-like images in the background. Use Slate-friendly colors as a base: cobalt blue, deep navy, cool gray, restrained warm accents.

Identity diversity is mandatory: each numbered item must have a clearly different face shape, hairstyle, outfit silhouette, camera angle, expression, target gender, and background palette. Do not reuse the same portrait template. Use each target gender and appearance brief as required visual constraints.

Generate these 10 profile images as separate one-item requests, or only the retained first N items if the DB Count Guard capped this block:
1. profile_cdv_03_screenwriting_creator.webp - CDV creator 03, screenwriter and assistant director. Target gender: female. Appearance brief: early-30s woman, long face, low ponytail, tortoise-shell glasses, dark green knit vest over white shirt, measured expression. Background: story structure and call-sheet organization, quiet desk, sage accent.
2. profile_cdv_04_cinematography_creator.webp - CDV creator 04, cinematography/directing role. Target gender: male. Appearance brief: late-20s man, sharp cheekbones, slightly messy medium hair, no glasses, black denim jacket, intense side glance. Background: handheld camera movement and thriller-lighting interest, red practical-light accent.
3. profile_cdv_05_sound_creator.webp - CDV creator 05, sound-focused creator. Target gender: female. Appearance brief: mid-30s woman, square face, short pixie cut, matte black headphones around neck, gray technical shirt, calm listening expression. Background: portable recorder, acoustic panels, mystery/documentary tone, muted plum accent.
4. profile_cdv_06_art_creator.webp - CDV creator 06, art direction and production design. Target gender: male. Appearance brief: mid-20s man, round face, soft curly hair, clear glasses, paint-speckled navy overshirt, curious slight smile. Background: color swatches and miniature set shapes, clay orange accent.
5. profile_cdv_07_acting_creator.webp - CDV creator 07, actor and documentary-friendly performer. Target gender: female. Appearance brief: early-20s woman, expressive eyes, long natural waves, white rehearsal tee under black blazer, animated but professional expression. Background: rehearsal-room energy, warm wood accent.
6. profile_cdv_08_post_creator.webp - CDV creator 08, post-production editor. Target gender: male. Appearance brief: late-30s man, broad face, shaved head, thick square glasses, dark turtleneck, quiet concentration. Background: remote collaboration, timeline color blocks and headphones, cool cyan accent.
7. profile_cdv_09_planning_creator.webp - CDV creator 09, planning and experimental-art production. Target gender: female. Appearance brief: early-30s woman, angular face, asymmetrical bob, no glasses, structured charcoal blazer with rust scarf, decisive posture. Background: organized mood boards and budget binder cues, rust accent.
8. profile_cdv_10_directing_creator.webp - CDV creator 10, director with music/performance interest. Target gender: male. Appearance brief: late-20s man, oval face, long hair tied in a low bun, thin mustache, black stagewear jacket, confident upright pose. Background: stage-light setup, indigo and magenta accents.
9. profile_cdv_11_screenwriting_creator.webp - CDV creator 11, screenwriter for youth stories. Target gender: female. Appearance brief: mid-20s woman, small round face, straight bangs and shoulder-length hair, soft yellow cardigan, gentle reflective expression. Background: clean writing desk and storyboard note cards without text, pale yellow accent.
10. profile_cdv_12_cinematography_creator.webp - CDV creator 12, cinematography and historical drama mood. Target gender: male. Appearance brief: early-40s man, strong brow, salt-and-pepper short hair, no glasses, brown work shirt under navy coat, composed gaze. Background: classic lens and muted period-color backdrop, sepia-blue accent.
```

## Prompt 03 - User Profiles 21-30

```text
Use this block as a generation queue for exactly 10 separate profile images, one standalone image per numbered item. If this block is capped by the DB Count Guard, use exactly the capped number of retained numbered items only. In Web ChatGPT, run one retained numbered item per image-generation request; never ask it to render all retained profile items together. The output for each request must be exactly one square image for exactly one filename. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas.

Use square 1:1 composition suitable for 800x800 profile images. Each image must be a solo head-and-shoulders or upper-body portrait of exactly one person. Style: polished cinematic editorial portrait illustration for Slate, a professional film and video production collaboration platform. All people are fictional Korean media creators. Avoid celebrity likenesses, real people, real brand logos, watermarks, UI, and text. Use clean studio or location-inspired backgrounds with subtle role-specific props, but do not include any other human figures or face-like images in the background. Use Slate-friendly colors as a base: cobalt blue, deep navy, cool gray, restrained warm accents.

Identity diversity is mandatory: each numbered item must have a clearly different face shape, hairstyle, outfit silhouette, camera angle, expression, target gender, and background palette. Do not reuse the same portrait template. Use each target gender and appearance brief as required visual constraints.

Generate these 10 profile images as separate one-item requests, or only the retained first N items if the DB Count Guard capped this block:
1. profile_cdv_13_planning_creator.webp - CDV creator 13, producer/planner for drama projects. Target gender: female. Appearance brief: mid-40s woman, defined jaw, shoulder-length layered hair, rimless glasses, navy suit jacket with silver pin, calm senior presence. Background: production board cues, deep blue and pearl accent.
2. profile_cdv_14_directing_creator.webp - CDV creator 14, director and VFX-aware collaborator. Target gender: male. Appearance brief: early-30s man, narrow face, high fade haircut, no glasses, cobalt tech jacket, analytical expression. Background: modern studio with abstract previs shapes, electric blue accent.
3. profile_cdv_15_screenwriting_creator.webp - CDV creator 15, writer with music-film interest. Target gender: female. Appearance brief: late-20s woman, round face, long braided hair, burgundy sweater, soft but focused expression, seated at slight profile. Background: script binder and audio mood references without text, burgundy accent.
4. profile_cdv_16_cinematography_creator.webp - CDV creator 16, cinematographer and marketing-minded web content creator. Target gender: male. Appearance brief: mid-20s man, triangular face, bleached-brown short hair, clear safety-style glasses, light gray studio hoodie, energetic expression. Background: bright compact studio setup, lime accent used sparingly.
5. profile_cdv_17_planning_creator.webp - CDV creator 17, producer for city drama. Target gender: female. Appearance brief: early-30s woman, oval face, sleek low bun, no glasses, dark navy travel jacket, practical alert expression. Background: urban production bag cues, blue-hour street, orange transit accent.
6. profile_cdv_18_directing_creator.webp - CDV creator 18, line producer/directing support. Target gender: male. Appearance brief: late-30s man, broad jaw, neatly combed hair, rectangular glasses, black collared shirt, precise composed gaze. Background: schedule and crew logistics mood, slate-gray accent.
7. profile_cdv_19_screenwriting_creator.webp - CDV creator 19, assistant director and story continuity role. Target gender: female. Appearance brief: early-20s woman, heart-shaped face, short shag haircut, denim overshirt, alert side-smile. Background: shot list cards with no readable text, dusty pink accent.
8. profile_cdv_20_cinematography_creator.webp - CDV creator 20, director/cinematography blend. Target gender: male. Appearance brief: late-20s man, square face, shoulder-length straight hair tucked behind ears, no glasses, black mock-neck, focused posture. Background: dramatic set lighting, narrow rim light, emerald accent.
9. profile_cdv_21_sound_creator.webp - CDV creator 21, writer and sound-aware creator. Target gender: female. Appearance brief: mid-30s woman, long oval face, short curly hair, round glasses, charcoal sweater with blue scarf, quiet observant gaze. Background: audio-writing studio, genre mystery tone, midnight blue accent.
10. profile_cdv_22_art_creator.webp - CDV creator 22, cinematographer with art/design sensitivity. Target gender: male. Appearance brief: early-40s man, soft round face, wavy side-parted hair, light stubble, canvas chore coat, reflective expression. Background: set texture and light-meter cues, warm linen accent.
```

## Prompt 04 - User Profiles 31-40

```text
Use this block as a generation queue for exactly 10 separate profile images, one standalone image per numbered item. If this block is capped by the DB Count Guard, use exactly the capped number of retained numbered items only. In Web ChatGPT, run one retained numbered item per image-generation request; never ask it to render all retained profile items together. The output for each request must be exactly one square image for exactly one filename. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas.

Use square 1:1 composition suitable for 800x800 profile images. Each image must be a solo head-and-shoulders or upper-body portrait of exactly one person. Style: polished cinematic editorial portrait illustration for Slate, a professional film and video production collaboration platform. All people are fictional Korean media creators. Avoid celebrity likenesses, real people, real brand logos, watermarks, UI, and text. Use clean studio or location-inspired backgrounds with subtle role-specific props, but do not include any other human figures or face-like images in the background. Use Slate-friendly colors as a base: cobalt blue, deep navy, cool gray, restrained warm accents.

Identity diversity is mandatory: each numbered item must have a clearly different face shape, hairstyle, outfit silhouette, camera angle, expression, target gender, and background palette. Do not reuse the same portrait template. Use each target gender and appearance brief as required visual constraints.

Generate these 10 profile images as separate one-item requests, or only the retained first N items if the DB Count Guard capped this block:
1. profile_cdv_23_acting_creator.webp - CDV creator 23, lighting director and performer-friendly collaborator. Target gender: female. Appearance brief: late-20s woman, angular face, high ponytail, no glasses, black utility shirt with small light-meter strap, poised expression. Background: rehearsal lights and compact fixture cues, amber accent.
2. profile_cdv_24_post_creator.webp - CDV creator 24, sound recordist and editor. Target gender: male. Appearance brief: mid-30s man, oval face, thick wavy hair, round black glasses, navy sweatshirt, relaxed concentration. Background: remote post-production desk with headphones and waveform-like abstract shapes, aquamarine accent.
3. profile_cdv_25_planning_creator.webp - CDV creator 25, art director for experimental projects. Target gender: female. Appearance brief: early-30s woman, square face, cropped bob with side-swept bangs, rust work apron over black top, creative focused stare. Background: tactile materials and production design, terracotta accent.
4. profile_cdv_26_directing_creator.webp - CDV creator 26, costume and directing-support creator. Target gender: male. Appearance brief: late-20s man, slim face, center-parted medium hair, no glasses, soft gray cardigan with measuring tape as subtle prop, reserved smile. Background: wardrobe textures and quiet fitting-room mood, lavender-gray accent.
5. profile_cdv_27_screenwriting_creator.webp - CDV creator 27, actor/writer energy. Target gender: female. Appearance brief: early-20s woman, round cheeks, long straight hair with half-up clip, cobalt rehearsal jacket, lively direct gaze. Background: rehearsal mirror and script pages without readable text, sky-blue accent.
6. profile_cdv_28_cinematography_creator.webp - CDV creator 28, editor plus producer support. Target gender: male. Appearance brief: late-30s man, rectangular face, short textured hair, thin silver glasses, dark green button-up, pragmatic expression. Background: color-coded post workflow without actual UI, forest green accent.
7. profile_cdv_29_sound_creator.webp - CDV creator 29, colorist and sound-sensitive collaborator. Target gender: female. Appearance brief: early-40s woman, long face, chin-length silver-streaked hair, no glasses, black turtleneck with muted cobalt shawl, precise gaze. Background: grading-suite glow and restrained palette, cool violet accent.
8. profile_cdv_30_art_creator.webp - CDV creator 30, VFX and art direction. Target gender: male. Appearance brief: mid-20s man, round face, tousled short hair, clear glasses, white technical jacket, curious forward lean. Background: abstract compositing layers and clean technical studio mood, ice-blue accent.
9. profile_cdv_31_acting_creator.webp - CDV creator 31, music director and actor-friendly production collaborator. Target gender: female. Appearance brief: early-30s woman, oval face, short natural waves, gold-rim glasses, deep red studio shirt, warm confident smile. Background: small recording studio mood, muted gold accent.
10. profile_cdv_32_post_creator.webp - CDV creator 32, marketing and post-production creator. Target gender: male. Appearance brief: late-20s man, heart-shaped face, neat undercut, no glasses, beige blazer over black tee, polished presentation posture. Background: campaign mood board and compact edit desk, cobalt and sand accent.
```

## Prompt 05 - Team Images 01-10

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 team representative images. Style: cinematic editorial illustration for a professional film/video collaboration service. Show teams through spaces, tools, storyboards, lighting, and atmosphere rather than identifiable faces. No real logos, no readable text, no UI, no watermarks. Keep the central subject clear for small cards.

Generate these 10 team images, or only the retained first N items if the DB Count Guard capped this block:
1. team_cdd_hangang_night_short.webp - CDD Han River night short-film team, night riverside rehearsal, drama/thriller tone, compact crew setup.
2. team_cdd_completed_portfolio.webp - CDD completed portfolio team, remote post-production collaboration, finished cut review, calm wrap-up mood.
3. team_cdv_01_dawn_market_short.webp - CDV 01 dawn market short-film team, early market light, small camera crew, human drama mood.
4. team_cdv_02_rooftop_romance.webp - CDV 02 rooftop romance production team, Seoul rooftop at dusk, warm relationship drama atmosphere.
5. team_cdv_03_alley_mystery.webp - CDV 03 alley mystery team, narrow alley, controlled shadows, suspenseful but not horror.
6. team_cdv_04_youth_music_film.webp - CDV 04 youth music film team, rehearsal room, instruments and camera setup, energetic but refined.
7. team_cdv_05_jeju_observational_doc.webp - CDV 05 Jeju observational documentary team, coastal landscape, portable documentary gear.
8. team_cdv_06_busan_harbor_thriller.webp - CDV 06 Busan harbor thriller team, harbor lights, noir-like planning table, restrained suspense.
9. team_cdv_07_urban_sf_webcontent.webp - CDV 07 urban SF web content team, modern city night, subtle futuristic lighting, web-series scale.
10. team_cdv_08_family_animation.webp - CDV 08 family animation team, animation storyboards, soft color models, friendly production space.
```

## Prompt 06 - Team Images 11-14

```text
Create exactly 4 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 team representative images. Style: cinematic editorial illustration for a professional film/video collaboration service. Show teams through spaces, tools, storyboards, lighting, and atmosphere rather than identifiable faces. No real logos, no readable text, no UI, no watermarks. Keep the central subject clear for small cards.

Generate these 4 team images, or only the retained first N items if the DB Count Guard capped this block:
1. team_cdv_09_period_previs.webp - CDV 09 period-drama previs team, miniature set, costume fabric, previs frames without text.
2. team_cdv_10_live_performance_post.webp - CDV 10 live performance post-production team, concert footage edit room, sound and color workflow.
3. team_cdv_11_completed_brand_film.webp - CDV 11 completed brand-film team, polished commercial production wrap, clean studio and product-lighting shapes without brands.
4. team_cdv_12_ended_local_record.webp - CDV 12 ended local-record documentary team, regional archive materials, travel case, closing-project mood.
```

## Prompt 07 - Work Images 01-10

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 work post thumbnails. Style: cinematic editorial still or refined illustration for film/video work cards. No real logos, no readable text, no UI, no watermarks, no copied film stills. The image should make the work type and mood understandable at thumbnail size.

Generate these 10 work images, or only the retained first N items if the DB Count Guard capped this block:
1. work_cdd_hangang_night_rehearsal.webp - CDD Han River night rehearsal cut, low-light riverside rehearsal, camera blocking marks, emotional drama mood.
2. work_cdv_01_short_film_record.webp - CDV work 01, short-film production record, dawn market drama, crew prep and natural light.
3. work_cdv_02_feature_film_record.webp - CDV work 02, feature-film style rooftop romance, dusk skyline and intimate blocking.
4. work_cdv_03_music_video_record.webp - CDV work 03, alley mystery music-video mood, rhythmic light streaks, no performers identifiable.
5. work_cdv_04_advertisement_record.webp - CDV work 04, youth music advertisement style, clean product-lighting shapes without brands.
6. work_cdv_05_documentary_record.webp - CDV work 05, Jeju observational documentary, coastline, handheld camera, quiet realism.
7. work_cdv_06_web_content_record.webp - CDV work 06, Busan harbor web-content thriller, compact digital crew and harbor lights.
8. work_cdv_07_other_record.webp - CDV work 07, urban SF experimental web piece, neon city reflections, subtle future tone.
9. work_cdv_08_short_film_record.webp - CDV work 08, family animation short-film production, storyboards and soft character shapes without text.
10. work_cdv_09_feature_film_record.webp - CDV work 09, period-drama feature production record, fabric, props, and previs table.
```

## Prompt 08 - Work Images 11-20

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 work post thumbnails. Style: cinematic editorial still or refined illustration for film/video work cards. No real logos, no readable text, no UI, no watermarks, no copied film stills. The image should make the work type and mood understandable at thumbnail size.

Generate these 10 work images, or only the retained first N items if the DB Count Guard capped this block:
1. work_cdv_10_music_video_record.webp - CDV work 10, live performance music-video edit, stage lighting, camera angle references.
2. work_cdv_11_advertisement_record.webp - CDV work 11, completed brand-film style commercial, clean studio lighting with abstract objects only.
3. work_cdv_12_documentary_record.webp - CDV work 12, local-record documentary, archive table, regional street detail, documentary calm.
4. work_cdv_13_web_content_record.webp - CDV work 13, city-drama web content, compact crew on urban sidewalk at blue hour.
5. work_cdv_14_other_record.webp - CDV work 14, rooftop romance experimental cut, warm light and abstract editing motif.
6. work_cdv_15_short_film_record.webp - CDV work 15, alley mystery short film, controlled shadows and practical lamp setup.
7. work_cdv_16_feature_film_record.webp - CDV work 16, youth music feature scene, rehearsal stage and production monitor silhouette.
8. work_cdv_17_music_video_record.webp - CDV work 17, Jeju music-video/documentary hybrid, ocean wind and portable audio gear.
9. work_cdv_18_advertisement_record.webp - CDV work 18, harbor-thriller promotional cut, moody pier lights and storyboard panels.
10. work_cdv_19_documentary_record.webp - CDV work 19, urban SF documentary-style test, city reflections and subtle augmented-light motif.
```

## Prompt 09 - Work Images 21-30

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 work post thumbnails. Style: cinematic editorial still or refined illustration for film/video work cards. No real logos, no readable text, no UI, no watermarks, no copied film stills. The image should make the work type and mood understandable at thumbnail size.

Generate these 10 work images, or only the retained first N items if the DB Count Guard capped this block:
1. work_cdv_20_web_content_record.webp - CDV work 20, family animation web-content record, clean animation desk and soft light.
2. work_cdv_21_other_record.webp - CDV work 21, period-drama experimental record, props table and previs framing.
3. work_cdv_22_short_film_record.webp - CDV work 22, live performance short film, backstage post-production mood.
4. work_cdv_23_feature_film_record.webp - CDV work 23, brand-film feature-style production, clean commercial lighting without brands.
5. work_cdv_24_music_video_record.webp - CDV work 24, local-record music video, regional street performance atmosphere.
6. work_cdv_25_advertisement_record.webp - CDV work 25, independent commercial test, reflective studio surface, no logo.
7. work_cdv_26_documentary_record.webp - CDV work 26, independent documentary work, field notes and portable camera.
8. work_cdv_27_web_content_record.webp - CDV work 27, independent web content, small creator studio and flexible setup.
9. work_cdv_28_other_record.webp - CDV work 28, experimental production record, abstract light and editing rhythm.
10. work_cdv_29_short_film_record.webp - CDV work 29, independent short film, clean location scout frame and slate-like color accents.
```

## Prompt 10 - Work Images 31-37

```text
Create exactly 7 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 work post thumbnails. Style: cinematic editorial still or refined illustration for film/video work cards. No real logos, no readable text, no UI, no watermarks, no copied film stills. The image should make the work type and mood understandable at thumbnail size.

Generate these 7 work images, or only the retained first N items if the DB Count Guard capped this block:
1. work_cdv_30_feature_film_record.webp - CDV work 30, independent feature-style production record, precise blocking and muted studio light.
2. work_cdv_31_independent_music_video.webp - CDV independent work 31, music-video thumbnail, small performance setup and color rhythm.
3. work_cdv_32_independent_web_content.webp - CDV independent work 32, web-content thumbnail, compact digital studio and clean creator desk.
4. work_cdv_33_independent_short_film.webp - CDV independent work 33, short-film thumbnail, intimate scene setup and natural light.
5. work_cdv_34_independent_music_video.webp - CDV independent work 34, music-video thumbnail, stage-like color accents and lens flare.
6. work_cdv_35_independent_web_content.webp - CDV independent work 35, web-content thumbnail, modular set and practical lighting.
7. work_cdv_36_independent_short_film.webp - CDV independent work 36, short-film thumbnail, quiet cinematic location and crew-ready atmosphere.
```

## Prompt 11 - Portfolio Images 01-10

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 portfolio thumbnails. Style: refined cinematic editorial illustration for individual creator portfolios. No real logos, no readable text, no UI, no watermarks, no copied film stills. Each thumbnail should look like a polished portfolio project, not a generic default image.

Generate these 10 portfolio images, or only the retained first N items if the DB Count Guard capped this block:
1. portfolio_cdd_yoon_hangang_editor.webp - CDD Yoon editor portfolio, Han River night rehearsal cut, emotional edit flow and subtle color finishing.
2. portfolio_cdd_minjae_lowlight_camera.webp - CDD Minjae cinematographer portfolio, low-light camera test, city night lens and stable handheld movement.
3. portfolio_cdv_01_producer_a.webp - CDV creator 01 portfolio A, producer planning package, organized shot cards and budget flow without text.
4. portfolio_cdv_01_producer_b.webp - CDV creator 01 portfolio B, finished short-film planning board, dawn market crew coordination.
5. portfolio_cdv_02_line_producer_a.webp - CDV creator 02 portfolio A, line production schedule mood, rooftop romance logistics.
6. portfolio_cdv_02_line_producer_b.webp - CDV creator 02 portfolio B, crew coordination result, clean call-time atmosphere without readable text.
7. portfolio_cdv_03_assistant_director_a.webp - CDV creator 03 portfolio A, assistant-director blocking plan, alley story continuity.
8. portfolio_cdv_03_assistant_director_b.webp - CDV creator 03 portfolio B, scene-order rehearsal board, compact production workflow.
9. portfolio_cdv_04_director_a.webp - CDV creator 04 portfolio A, director's thriller scene look, focused monitor silhouette.
10. portfolio_cdv_04_director_b.webp - CDV creator 04 portfolio B, low-light dramatic scene composition, precise staging.
```

## Prompt 12 - Portfolio Images 11-20

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 portfolio thumbnails. Style: refined cinematic editorial illustration for individual creator portfolios. No real logos, no readable text, no UI, no watermarks, no copied film stills. Each thumbnail should look like a polished portfolio project, not a generic default image.

Generate these 10 portfolio images, or only the retained first N items if the DB Count Guard capped this block:
1. portfolio_cdv_05_writer_a.webp - CDV creator 05 portfolio A, writer's urban mystery board, story beats shown as blank cards.
2. portfolio_cdv_05_writer_b.webp - CDV creator 05 portfolio B, final script-to-scene mood, rainy city table light.
3. portfolio_cdv_06_cinematographer_a.webp - CDV creator 06 portfolio A, camera test with natural light and lens references.
4. portfolio_cdv_06_cinematographer_b.webp - CDV creator 06 portfolio B, composed frame for SF or documentary mood.
5. portfolio_cdv_07_lighting_a.webp - CDV creator 07 portfolio A, compact lighting setup, controlled contrast and fixture silhouettes.
6. portfolio_cdv_07_lighting_b.webp - CDV creator 07 portfolio B, final lighting mood for youth film scene.
7. portfolio_cdv_08_sound_a.webp - CDV creator 08 portfolio A, location sound workflow, recorder and boom silhouettes.
8. portfolio_cdv_08_sound_b.webp - CDV creator 08 portfolio B, clean dialogue post setup, waveform-like abstract light without UI.
9. portfolio_cdv_09_art_director_a.webp - CDV creator 09 portfolio A, production design texture board, prop shapes and color swatches.
10. portfolio_cdv_09_art_director_b.webp - CDV creator 09 portfolio B, finished art direction scene, experimental set corner.
```

## Prompt 13 - Portfolio Images 21-30

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 portfolio thumbnails. Style: refined cinematic editorial illustration for individual creator portfolios. No real logos, no readable text, no UI, no watermarks, no copied film stills. Each thumbnail should look like a polished portfolio project, not a generic default image.

Generate these 10 portfolio images, or only the retained first N items if the DB Count Guard capped this block:
1. portfolio_cdv_10_costume_a.webp - CDV creator 10 portfolio A, costume concept table, fabric textures and neutral fitting-room light.
2. portfolio_cdv_10_costume_b.webp - CDV creator 10 portfolio B, final costume mood for music/performance project.
3. portfolio_cdv_11_actor_a.webp - CDV creator 11 portfolio A, rehearsal still mood, natural acting and empty stage space.
4. portfolio_cdv_11_actor_b.webp - CDV creator 11 portfolio B, performance close-up scene mood without recognizable face detail.
5. portfolio_cdv_12_editor_a.webp - CDV creator 12 portfolio A, editing rhythm board, abstract timeline colors without UI.
6. portfolio_cdv_12_editor_b.webp - CDV creator 12 portfolio B, finished historical-drama cut mood, color-managed review room.
7. portfolio_cdv_13_colorist_a.webp - CDV creator 13 portfolio A, grading suite, color palette chips and monitor glow without UI.
8. portfolio_cdv_13_colorist_b.webp - CDV creator 13 portfolio B, final color look for city drama, rich but restrained contrast.
9. portfolio_cdv_14_vfx_a.webp - CDV creator 14 portfolio A, VFX compositing layers as abstract translucent frames.
10. portfolio_cdv_14_vfx_b.webp - CDV creator 14 portfolio B, finished subtle sci-fi effect integrated into urban night.
```

## Prompt 14 - Portfolio Images 31-40

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 portfolio thumbnails. Style: refined cinematic editorial illustration for individual creator portfolios. No real logos, no readable text, no UI, no watermarks, no copied film stills. Each thumbnail should look like a polished portfolio project, not a generic default image.

Generate these 10 portfolio images, or only the retained first N items if the DB Count Guard capped this block:
1. portfolio_cdv_15_music_director_a.webp - CDV creator 15 portfolio A, music direction desk, headphones, keyboard, and score shapes without notes.
2. portfolio_cdv_15_music_director_b.webp - CDV creator 15 portfolio B, final music-performance film mood with stage light.
3. portfolio_cdv_16_marketing_a.webp - CDV creator 16 portfolio A, campaign planning board with blank visual cards, no readable text.
4. portfolio_cdv_16_marketing_b.webp - CDV creator 16 portfolio B, polished web-content launch visual, abstract social-card layout without UI.
5. portfolio_cdv_17_producer_a.webp - CDV creator 17 portfolio A, producer package for city drama, travel case and production binder.
6. portfolio_cdv_17_producer_b.webp - CDV creator 17 portfolio B, finished team pitch mood, urban blue-hour scene.
7. portfolio_cdv_18_line_producer_a.webp - CDV creator 18 portfolio A, line producer logistics, schedule board as blank blocks.
8. portfolio_cdv_18_line_producer_b.webp - CDV creator 18 portfolio B, crew transport and location plan mood, clean and professional.
9. portfolio_cdv_19_assistant_director_a.webp - CDV creator 19 portfolio A, assistant director continuity setup, shot order cards without text.
10. portfolio_cdv_19_assistant_director_b.webp - CDV creator 19 portfolio B, final rehearsal coordination image, precise blocking marks.
```

## Prompt 15 - Portfolio Images 41-50

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 portfolio thumbnails. Style: refined cinematic editorial illustration for individual creator portfolios. No real logos, no readable text, no UI, no watermarks, no copied film stills. Each thumbnail should look like a polished portfolio project, not a generic default image.

Generate these 10 portfolio images, or only the retained first N items if the DB Count Guard capped this block:
1. portfolio_cdv_20_director_a.webp - CDV creator 20 portfolio A, director's scene study, dramatic staging and controlled light.
2. portfolio_cdv_20_director_b.webp - CDV creator 20 portfolio B, finished cinematic frame for feature-style project.
3. portfolio_cdv_21_writer_a.webp - CDV creator 21 portfolio A, writer portfolio with mystery notes as blank cards and quiet audio room.
4. portfolio_cdv_21_writer_b.webp - CDV creator 21 portfolio B, final narrative mood, desk lamp and city reflection.
5. portfolio_cdv_22_cinematographer_a.webp - CDV creator 22 portfolio A, cinematography test with artful set textures.
6. portfolio_cdv_22_cinematographer_b.webp - CDV creator 22 portfolio B, finished observational frame, balanced natural light.
7. portfolio_cdv_23_lighting_a.webp - CDV creator 23 portfolio A, lighting design plan, fixtures and gels without labels.
8. portfolio_cdv_23_lighting_b.webp - CDV creator 23 portfolio B, final performance-lighting result, stage shadows.
9. portfolio_cdv_24_sound_a.webp - CDV creator 24 portfolio A, sound capture workflow, recorder and acoustic treatment.
10. portfolio_cdv_24_sound_b.webp - CDV creator 24 portfolio B, post sound mix mood, headphones and subtle waveform abstraction.
```

## Prompt 16 - Portfolio Images 51-60

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 portfolio thumbnails. Style: refined cinematic editorial illustration for individual creator portfolios. No real logos, no readable text, no UI, no watermarks, no copied film stills. Each thumbnail should look like a polished portfolio project, not a generic default image.

Generate these 10 portfolio images, or only the retained first N items if the DB Count Guard capped this block:
1. portfolio_cdv_25_art_director_a.webp - CDV creator 25 portfolio A, art direction material board, miniature set and color chips.
2. portfolio_cdv_25_art_director_b.webp - CDV creator 25 portfolio B, finished experimental art-film set mood.
3. portfolio_cdv_26_costume_a.webp - CDV creator 26 portfolio A, costume reference table, layered fabrics and fitting pins without labels.
4. portfolio_cdv_26_costume_b.webp - CDV creator 26 portfolio B, final costume mood for period or music project.
5. portfolio_cdv_27_actor_a.webp - CDV creator 27 portfolio A, rehearsal performance scene, expressive body language and neutral stage.
6. portfolio_cdv_27_actor_b.webp - CDV creator 27 portfolio B, final actor portfolio frame, warm natural performance mood.
7. portfolio_cdv_28_editor_a.webp - CDV creator 28 portfolio A, editing workflow, timeline-like abstract blocks without UI.
8. portfolio_cdv_28_editor_b.webp - CDV creator 28 portfolio B, final cut review room, restrained color and clean monitor light.
9. portfolio_cdv_29_colorist_a.webp - CDV creator 29 portfolio A, color grading palette, calibrated light and color chart shapes without text.
10. portfolio_cdv_29_colorist_b.webp - CDV creator 29 portfolio B, final color look for moody city project.
```

## Prompt 17 - Portfolio Images 61-66

```text
Create exactly 6 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 portfolio thumbnails. Style: refined cinematic editorial illustration for individual creator portfolios. No real logos, no readable text, no UI, no watermarks, no copied film stills. Each thumbnail should look like a polished portfolio project, not a generic default image.

Generate these 6 portfolio images, or only the retained first N items if the DB Count Guard capped this block:
1. portfolio_cdv_30_vfx_a.webp - CDV creator 30 portfolio A, VFX test plates, translucent layers and clean technical desk.
2. portfolio_cdv_30_vfx_b.webp - CDV creator 30 portfolio B, finished subtle future-city VFX integration, not flashy.
3. portfolio_cdv_31_music_director_a.webp - CDV creator 31 portfolio A, music direction setup, keyboard, headphones, and recording light without text.
4. portfolio_cdv_31_music_director_b.webp - CDV creator 31 portfolio B, final music/performance film mood, stage and soft haze.
5. portfolio_cdv_32_marketing_a.webp - CDV creator 32 portfolio A, marketing visual planning board with blank layout cards.
6. portfolio_cdv_32_marketing_b.webp - CDV creator 32 portfolio B, polished web-content campaign thumbnail, abstract launch mood without UI.
```

## Prompt 18 - Contest Images 01-10

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 contest or contest-request representative images. Style: premium cinematic editorial poster illustration for a film/video production opportunity platform. The UI will display the title separately, so do not include any text, numbers, logos, watermarks, award seals, or readable documents inside the image. Make each image feel like a production opportunity, not a finished movie poster.

Generate these 10 contest images, or only the retained first N items if the DB Count Guard capped this block:
1. contest_request_cdd_city_night_short.webp - CDD city night short-film production support request, adult and university creators, producer/cinematographer/editor focus.
2. contest_cdd_city_night_short.webp - CDD approved city night short-film contest, Seoul/Gyeonggi urban night, drama/thriller/youth tone.
3. contest_request_cdv_01_local_short.webp - CDV contest request 01, local short film, drama/thriller, producer/director/cinematographer roles.
4. contest_request_cdv_02_emerging_webcontent.webp - CDV contest request 02, emerging creator web content, SF and youth tone, writer/editor/VFX roles.
5. contest_request_cdv_03_music_performance.webp - CDV contest request 03, music and performance opportunity, music director/actor/marketing roles.
6. contest_request_cdv_04_local_short.webp - CDV contest request 04, regional short-film production, local team atmosphere and travel-ready gear.
7. contest_request_cdv_05_pending_webcontent.webp - CDV contest request 05, pending emerging web-content opportunity, clean review-ready poster mood.
8. contest_request_cdv_06_rejected_music_performance.webp - CDV contest request 06, music/performance proposal needing revision, neutral and professional.
9. contest_cdv_approved_01_local_short.webp - CDV approved contest 01, local short film with city and crew planning cues.
10. contest_cdv_approved_02_webcontent.webp - CDV approved contest 02, emerging web-content production, modern creator studio and subtle SF tone.
```

## Prompt 19 - Contest Images 11-20

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 contest representative images. Style: premium cinematic editorial poster illustration for a film/video production opportunity platform. The UI will display the title separately, so do not include any text, numbers, logos, watermarks, award seals, or readable documents inside the image. Make each image feel like a production opportunity, not a finished movie poster.

Generate these 10 contest images, or only the retained first N items if the DB Count Guard capped this block:
1. contest_cdv_approved_03_music_performance.webp - CDV approved contest 03, music/performance creators, stage and camera opportunity.
2. contest_cdv_approved_04_local_short.webp - CDV approved contest 04, regional short-film team opportunity, practical production planning.
3. contest_cdv_external_01_city_story.webp - CDV external contest 01, city story drama, Seoul urban collaboration opportunity.
4. contest_cdv_external_02_environment_record.webp - CDV external contest 02, environmental documentary record, Gyeonggi landscape and field crew.
5. contest_cdv_external_03_youth_music.webp - CDV external contest 03, youth and music, Busan energy, performers and camera setup without faces.
6. contest_cdv_external_04_tech_future.webp - CDV external contest 04, technology and future, Jeju regional production and subtle SF light.
7. contest_cdv_external_05_family_local.webp - CDV external contest 05, family and local community, nationwide accessible production mood.
8. contest_cdv_external_06_city_story.webp - CDV external contest 06, city story drama, urban night and writer/director opportunity.
9. contest_cdv_external_07_environment_record.webp - CDV external contest 07, environmental record, documentary crew kit and field notes without text.
10. contest_cdv_external_08_youth_music.webp - CDV external contest 08, youth music opportunity, rehearsal stage and camera movement.
```

## Prompt 20 - Contest Images 21-30

```text
Create exactly 10 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 contest representative images. Style: premium cinematic editorial poster illustration for a film/video production opportunity platform. The UI will display the title separately, so do not include any text, numbers, logos, watermarks, award seals, or readable documents inside the image. Make each image feel like a production opportunity, not a finished movie poster.

Generate these 10 contest images, or only the retained first N items if the DB Count Guard capped this block:
1. contest_cdv_external_09_tech_future.webp - CDV external contest 09, technology and future, clean futuristic production lab mood.
2. contest_cdv_external_10_family_local.webp - CDV external contest 10, family and regional story, warm community production setup.
3. contest_cdv_external_11_city_story.webp - CDV external contest 11, city story drama, director and writer opportunity in blue-hour city.
4. contest_cdv_external_12_environment_record.webp - CDV external contest 12, environmental documentary, coastal or forest field-production mood.
5. contest_cdv_external_13_youth_music.webp - CDV external contest 13, youth and music, small performance venue and creative crew.
6. contest_cdv_external_14_tech_future.webp - CDV external contest 14, SF and future technology, regional team opportunity, restrained visual effects.
7. contest_cdv_external_15_family_local.webp - CDV external contest 15, family/local storytelling, friendly production opportunity.
8. contest_cdv_external_16_city_story.webp - CDV external contest 16, city story drama, independent crew gathering at urban location.
9. contest_cdv_external_17_environment_record.webp - CDV external contest 17, environmental record, documentary field kit and natural light.
10. contest_cdv_external_18_youth_music.webp - CDV external contest 18, youth and music, stage rehearsal and film crew opportunity.
```

## Prompt 21 - Contest Images 31-32

```text
Create exactly 2 separate images, one image per numbered item. If this block is capped by the DB Count Guard, create exactly the capped number of retained numbered items only. Do not create a collage, contact sheet, grid, or combined image. Never place multiple numbered items in one generated image or canvas; output each numbered item as a separate standalone image.

Use horizontal 16:9 composition suitable for 1200x675 contest representative images. Style: premium cinematic editorial poster illustration for a film/video production opportunity platform. The UI will display the title separately, so do not include any text, numbers, logos, watermarks, award seals, or readable documents inside the image. Make each image feel like a production opportunity, not a finished movie poster.

Generate these 2 contest images, or only the retained first N items if the DB Count Guard capped this block:
1. contest_cdv_external_19_tech_future.webp - CDV external contest 19, technology and future, closed/ended opportunity mood, refined and not alarming.
2. contest_cdv_external_20_family_local.webp - CDV external contest 20, family and local story, closed/ended opportunity mood, warm archival production tone.
```
