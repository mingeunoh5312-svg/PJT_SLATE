SET NAMES utf8mb4;
USE slate;

START TRANSACTION;

SET @seed_profile_dir := 'images/seed/profile/';
SET @seed_team_dir := 'images/seed/team/';
SET @seed_work_dir := 'images/seed/work/';
SET @seed_portfolio_dir := 'images/seed/portfolio/';
SET @seed_contest_request_dir := 'images/seed/contest_request/';
SET @seed_contest_dir := 'images/seed/contest/';

-- CDD profiles.
UPDATE member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
JOIN (
  SELECT 'cdd-leader' AS login_id, 'profile_cdd_hyunseo_pd' AS image_stem
  UNION ALL SELECT 'cdd-camera', 'profile_cdd_minjae_cinematographer'
  UNION ALL SELECT 'cdd-sound', 'profile_cdd_soridam_sound'
  UNION ALL SELECT 'cdd-editor', 'profile_cdd_yoon_editor'
  UNION ALL SELECT 'cdd-writer', 'profile_cdd_rin_writer'
  UNION ALL SELECT 'cdd-actor', 'profile_cdd_jun_actor'
  UNION ALL SELECT 'cdd-reporter', 'profile_cdd_reviewer'
  UNION ALL SELECT 'cdd-moderated', 'profile_cdd_moderation_test'
) image_map ON image_map.login_id = ua.login_id
SET mp.profile_image_path = CONCAT(@seed_profile_dir, image_map.image_stem, '.png'),
    mp.updated_at = NOW();

-- CDV profiles.
UPDATE member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
JOIN JSON_TABLE(
  JSON_ARRAY(
    'planning_creator', 'directing_creator', 'screenwriting_creator', 'cinematography_creator',
    'sound_creator', 'art_creator', 'acting_creator', 'post_creator',
    'planning_creator', 'directing_creator', 'screenwriting_creator', 'cinematography_creator',
    'planning_creator', 'directing_creator', 'screenwriting_creator', 'cinematography_creator',
    'planning_creator', 'directing_creator', 'screenwriting_creator', 'cinematography_creator',
    'sound_creator', 'art_creator', 'acting_creator', 'post_creator',
    'planning_creator', 'directing_creator', 'screenwriting_creator', 'cinematography_creator',
    'sound_creator', 'art_creator', 'acting_creator', 'post_creator'
  ),
  '$[*]' COLUMNS (
    n FOR ORDINALITY,
    suffix varchar(80) PATH '$'
  )
) image_map ON ua.login_id = CONCAT('cdv-user-', LPAD(image_map.n, 2, '0'))
SET mp.profile_image_path = CONCAT(@seed_profile_dir, 'profile_cdv_', LPAD(image_map.n, 2, '0'), '_', image_map.suffix, '.png'),
    mp.updated_at = NOW();

-- CDD teams.
UPDATE team t
JOIN (
  SELECT '[CDD] 한강 야간 단편팀' AS team_name, 'team_cdd_hangang_night_short' AS image_stem
  UNION ALL SELECT '[CDD] 완료된 포트폴리오팀', 'team_cdd_completed_portfolio'
) image_map ON image_map.team_name = t.name
SET t.representative_image_path = CONCAT(@seed_team_dir, image_map.image_stem, '.png'),
    t.updated_at = NOW();

-- CDV teams.
UPDATE team t
JOIN JSON_TABLE(
  JSON_ARRAY(
    'dawn_market_short', 'rooftop_romance', 'alley_mystery', 'youth_music_film',
    'jeju_observational_doc', 'busan_harbor_thriller', 'urban_sf_webcontent', 'family_animation',
    'period_previs', 'live_performance_post', 'completed_brand_film', 'ended_local_record'
  ),
  '$[*]' COLUMNS (
    n FOR ORDINALITY,
    suffix varchar(80) PATH '$'
  )
) image_map ON t.name LIKE CONCAT('[CDV] ', LPAD(image_map.n, 2, '0'), ' %')
SET t.representative_image_path = CONCAT(@seed_team_dir, 'team_cdv_', LPAD(image_map.n, 2, '0'), '_', image_map.suffix, '.png'),
    t.updated_at = NOW();

-- CDD works.
UPDATE work_item
SET representative_image_path = CONCAT(@seed_work_dir, 'work_cdd_hangang_night_rehearsal.png'),
    updated_at = NOW()
WHERE title = '[CDD] 한강 야간 리허설 컷';

-- CDV works linked to board posts.
UPDATE work_item wi
JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30),
  '$[*]' COLUMNS (n INT PATH '$')
) seq ON wi.title = CONCAT('[CDV] 작업물 ', LPAD(seq.n, 2, '0'), ' 제작 기록')
SET wi.representative_image_path = CONCAT(
      @seed_work_dir,
      'work_cdv_',
      LPAD(seq.n, 2, '0'),
      '_',
      CASE MOD(seq.n - 1, 7)
        WHEN 0 THEN 'short_film'
        WHEN 1 THEN 'feature_film'
        WHEN 2 THEN 'music_video'
        WHEN 3 THEN 'advertisement'
        WHEN 4 THEN 'documentary'
        WHEN 5 THEN 'web_content'
        ELSE 'other'
      END,
      '_record.png'
    ),
    wi.updated_at = NOW();

-- CDV independent works.
UPDATE work_item wi
JOIN JSON_TABLE(
  JSON_ARRAY(31,32,33,34,35,36),
  '$[*]' COLUMNS (n INT PATH '$')
) seq ON wi.title = CONCAT('[CDV] 독립 작업물 ', LPAD(seq.n, 2, '0'))
SET wi.representative_image_path = CONCAT(
      @seed_work_dir,
      'work_cdv_',
      LPAD(seq.n, 2, '0'),
      '_independent_',
      CASE MOD(seq.n, 3)
        WHEN 0 THEN 'short_film'
        WHEN 1 THEN 'music_video'
        ELSE 'web_content'
      END,
      '.png'
    ),
    wi.updated_at = NOW();

-- CDD portfolio items.
UPDATE portfolio_item pi
JOIN (
  SELECT 'CDD-RIVER-WORK-001' AS external_reference_id, 'portfolio_cdd_yoon_hangang_editor' AS image_stem
  UNION ALL SELECT 'CDD-CAMERA-TEST-001', 'portfolio_cdd_minjae_lowlight_camera'
) image_map ON image_map.external_reference_id = pi.external_reference_id
SET pi.thumbnail_image_path = CONCAT(@seed_portfolio_dir, image_map.image_stem, '.png'),
    pi.updated_at = NOW()
WHERE pi.external_source_name = 'SLATE_CDD';

-- CDV portfolio items.
UPDATE portfolio_item pi
JOIN (
  SELECT seq.n AS profile_no, item_no.n AS item_no
  FROM JSON_TABLE(
    JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
    '$[*]' COLUMNS (n INT PATH '$')
  ) seq
  CROSS JOIN JSON_TABLE(
    JSON_ARRAY(1,2),
    '$[*]' COLUMNS (n INT PATH '$')
  ) item_no
) image_map ON pi.external_reference_id = CONCAT('CDV-PORT-', LPAD(image_map.profile_no, 2, '0'), '-', image_map.item_no)
SET pi.thumbnail_image_path = CONCAT(
      @seed_portfolio_dir,
      'portfolio_cdv_',
      LPAD(image_map.profile_no, 2, '0'),
      '_',
      CASE MOD(image_map.profile_no - 1, 16)
        WHEN 0 THEN 'producer'
        WHEN 1 THEN 'line_producer'
        WHEN 2 THEN 'assistant_director'
        WHEN 3 THEN 'director'
        WHEN 4 THEN 'writer'
        WHEN 5 THEN 'cinematographer'
        WHEN 6 THEN 'lighting'
        WHEN 7 THEN 'sound'
        WHEN 8 THEN 'art_director'
        WHEN 9 THEN 'costume'
        WHEN 10 THEN 'actor'
        WHEN 11 THEN 'editor'
        WHEN 12 THEN 'colorist'
        WHEN 13 THEN 'vfx'
        WHEN 14 THEN 'music_director'
        ELSE 'marketing'
      END,
      '_',
      CASE image_map.item_no WHEN 1 THEN 'a' ELSE 'b' END,
      '.png'
    ),
    pi.updated_at = NOW()
WHERE pi.external_source_name = 'SLATE_CDV';

-- CDD contest request and approved contest.
UPDATE contest_open_request
SET representative_image_path = CONCAT(@seed_contest_request_dir, 'contest_request_cdd_city_night_short.png'),
    updated_at = NOW()
WHERE title = '[CDD] 도시 단편 제작지원 요청';

UPDATE contest
SET representative_image_path = CONCAT(@seed_contest_dir, 'contest_cdd_city_night_short.png'),
    updated_at = NOW()
WHERE title = '[CDD] 도시 단편 제작지원 공모';

-- CDV contest requests.
UPDATE contest_open_request cor
JOIN JSON_TABLE(
  JSON_ARRAY('local_short', 'emerging_webcontent', 'music_performance', 'local_short', 'pending_webcontent', 'rejected_music_performance'),
  '$[*]' COLUMNS (
    n FOR ORDINALITY,
    suffix varchar(80) PATH '$'
  )
) image_map ON cor.title = CONCAT('[CDV] 공모전 개설 요청 ', LPAD(image_map.n, 2, '0'))
SET cor.representative_image_path = CONCAT(@seed_contest_request_dir, 'contest_request_cdv_', LPAD(image_map.n, 2, '0'), '_', image_map.suffix, '.png'),
    cor.updated_at = NOW();

-- CDV approved internal contests.
UPDATE contest c
JOIN JSON_TABLE(
  JSON_ARRAY('local_short', 'webcontent', 'music_performance', 'local_short'),
  '$[*]' COLUMNS (
    n FOR ORDINALITY,
    suffix varchar(80) PATH '$'
  )
) image_map ON c.title = CONCAT('[CDV] 승인 공모전 ', LPAD(image_map.n, 2, '0'))
SET c.representative_image_path = CONCAT(@seed_contest_dir, 'contest_cdv_approved_', LPAD(image_map.n, 2, '0'), '_', image_map.suffix, '.png'),
    c.updated_at = NOW();

-- CDV external contests.
UPDATE contest c
JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20),
  '$[*]' COLUMNS (n INT PATH '$')
) seq ON c.title = CONCAT('[CDV] 외부 공모전 ', LPAD(seq.n, 2, '0'))
SET c.representative_image_path = CONCAT(
      @seed_contest_dir,
      'contest_cdv_external_',
      LPAD(seq.n, 2, '0'),
      '_',
      CASE MOD(seq.n - 1, 5)
        WHEN 0 THEN 'city_story'
        WHEN 1 THEN 'environment_record'
        WHEN 2 THEN 'youth_music'
        WHEN 3 THEN 'tech_future'
        ELSE 'family_local'
      END,
      '.png'
    ),
    c.updated_at = NOW();

COMMIT;

SELECT 'GENERATED_DUMMY_IMAGE_COUNTS' AS section, 'cdd_profile' AS entity_name, COUNT(*) AS image_path_count, (
  SELECT COUNT(*)
  FROM member_profile mp_expected
  JOIN user_account ua_expected ON ua_expected.user_id = mp_expected.user_id
  WHERE ua_expected.login_id IN ('cdd-leader', 'cdd-camera', 'cdd-sound', 'cdd-editor', 'cdd-writer', 'cdd-actor', 'cdd-reporter', 'cdd-moderated')
) AS expected_count
FROM member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id IN ('cdd-leader', 'cdd-camera', 'cdd-sound', 'cdd-editor', 'cdd-writer', 'cdd-actor', 'cdd-reporter', 'cdd-moderated')
  AND mp.profile_image_path LIKE 'images/seed/profile/%.png'
UNION ALL
SELECT 'GENERATED_DUMMY_IMAGE_COUNTS', 'cdv_profile', COUNT(*), (
  SELECT COUNT(*)
  FROM member_profile mp_expected
  JOIN user_account ua_expected ON ua_expected.user_id = mp_expected.user_id
  WHERE ua_expected.login_id LIKE 'cdv-user-%'
)
FROM member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id LIKE 'cdv-user-%'
  AND mp.profile_image_path LIKE 'images/seed/profile/%.png'
UNION ALL
SELECT 'GENERATED_DUMMY_IMAGE_COUNTS', 'team', COUNT(*), (
  SELECT COUNT(*) FROM team WHERE name LIKE '[CDD]%' OR name LIKE '[CDV]%'
)
FROM team
WHERE (name LIKE '[CDD]%' OR name LIKE '[CDV]%')
  AND representative_image_path LIKE 'images/seed/team/%.png'
UNION ALL
SELECT 'GENERATED_DUMMY_IMAGE_COUNTS', 'work', COUNT(*), (
  SELECT COUNT(*) FROM work_item WHERE title LIKE '[CDD]%' OR title LIKE '[CDV]%'
)
FROM work_item
WHERE (title LIKE '[CDD]%' OR title LIKE '[CDV]%')
  AND representative_image_path LIKE 'images/seed/work/%.png'
UNION ALL
SELECT 'GENERATED_DUMMY_IMAGE_COUNTS', 'portfolio', COUNT(*), (
  SELECT COUNT(*) FROM portfolio_item WHERE external_source_name IN ('SLATE_CDD', 'SLATE_CDV')
)
FROM portfolio_item
WHERE external_source_name IN ('SLATE_CDD', 'SLATE_CDV')
  AND thumbnail_image_path LIKE 'images/seed/portfolio/%.png'
UNION ALL
SELECT 'GENERATED_DUMMY_IMAGE_COUNTS', 'contest_request', COUNT(*), (
  SELECT COUNT(*) FROM contest_open_request WHERE title LIKE '[CDD]%' OR title LIKE '[CDV]%'
)
FROM contest_open_request
WHERE (title LIKE '[CDD]%' OR title LIKE '[CDV]%')
  AND representative_image_path LIKE 'images/seed/contest_request/%.png'
UNION ALL
SELECT 'GENERATED_DUMMY_IMAGE_COUNTS', 'contest', COUNT(*), (
  SELECT COUNT(*) FROM contest WHERE title LIKE '[CDD]%' OR title LIKE '[CDV]%'
)
FROM contest
WHERE (title LIKE '[CDD]%' OR title LIKE '[CDV]%')
  AND representative_image_path LIKE 'images/seed/contest/%.png';
