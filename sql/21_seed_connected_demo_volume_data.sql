SET NAMES utf8mb4;
USE slate;

START TRANSACTION;

-- CDV namespace cleanup for idempotent local demo seeding.
DELETE FROM operation_log
WHERE event_code LIKE 'CDV_%';

DELETE FROM audit_log
WHERE action_type LIKE 'CDV_%'
   OR ip_hash = 'cdv-seed-ip-hash';

DELETE FROM notification
WHERE title LIKE '[CDV]%'
   OR recipient_user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   )
   OR sender_user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   );

DELETE FROM user_sanction
WHERE user_id IN (
  SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
);

DELETE FROM content_report
WHERE reporter_user_id IN (
  SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
)
   OR (target_type = 'BOARD_POST' AND target_id IN (
      SELECT post_id FROM board_post WHERE title LIKE '[CDV]%'
   ))
   OR (target_type = 'BOARD_REVIEW' AND target_id IN (
      SELECT br.review_id
      FROM board_review br
      JOIN board_post bp ON bp.post_id = br.post_id
      WHERE bp.title LIKE '[CDV]%'
   ));

DELETE FROM contest_submission_prepare
WHERE contest_id IN (
  SELECT contest_id FROM contest WHERE title LIKE '[CDV]%'
)
   OR user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   );

DELETE FROM contest_fit_cache
WHERE contest_id IN (
  SELECT contest_id FROM contest WHERE title LIKE '[CDV]%'
)
   OR (basis_type = 'TEAM' AND basis_id IN (
      SELECT team_id FROM team WHERE name LIKE '[CDV]%'
   ))
   OR (basis_type = 'PROFILE' AND basis_id IN (
      SELECT mp.profile_id
      FROM member_profile mp
      JOIN user_account ua ON ua.user_id = mp.user_id
      WHERE ua.login_id LIKE 'cdv-%'
   ));

DELETE FROM contest_save
WHERE contest_id IN (
  SELECT contest_id FROM contest WHERE title LIKE '[CDV]%'
)
   OR user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   );

DELETE FROM contest_open_request
WHERE title LIKE '[CDV]%'
   OR requester_user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-company-%'
   );

DELETE FROM contest
WHERE title LIKE '[CDV]%';

DELETE FROM team_work_approval_genre
WHERE request_id IN (
  SELECT request_id
  FROM team_work_approval_request
  WHERE title LIKE '[CDV]%'
);

DELETE FROM team_work_approval_request
WHERE title LIKE '[CDV]%'
   OR team_id IN (
      SELECT team_id FROM team WHERE name LIKE '[CDV]%'
   );

DELETE FROM work_genre
WHERE work_id IN (
  SELECT work_id FROM work_item WHERE title LIKE '[CDV]%'
);

DELETE FROM work_item
WHERE title LIKE '[CDV]%';

DELETE FROM board_view_log
WHERE post_id IN (
  SELECT post_id FROM board_post WHERE title LIKE '[CDV]%'
)
   OR ip_hash LIKE 'cdv-view-%';

DELETE FROM board_like
WHERE post_id IN (
  SELECT post_id FROM board_post WHERE title LIKE '[CDV]%'
);

DELETE br
FROM board_review br
JOIN board_post bp ON bp.post_id = br.post_id
WHERE bp.title LIKE '[CDV]%'
  AND br.parent_review_id IS NOT NULL;

DELETE br
FROM board_review br
JOIN board_post bp ON bp.post_id = br.post_id
WHERE bp.title LIKE '[CDV]%';

DELETE FROM board_post
WHERE title LIKE '[CDV]%';

DELETE FROM portfolio_verification
WHERE portfolio_item_id IN (
  SELECT portfolio_item_id
  FROM portfolio_item
  WHERE external_source_name = 'SLATE_CDV'
     OR external_reference_id LIKE 'CDV-%'
);

DELETE FROM portfolio_item
WHERE external_source_name = 'SLATE_CDV'
   OR external_reference_id LIKE 'CDV-%';

DELETE FROM matching_action_log
WHERE action_type LIKE 'CDV_%'
   OR actor_user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   );

DELETE FROM matching_bookmark
WHERE user_id IN (
  SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
)
   OR (target_type = 'TEAM' AND target_id IN (
      SELECT team_id FROM team WHERE name LIKE '[CDV]%'
   ))
   OR (target_type = 'PROFILE' AND target_id IN (
      SELECT mp.profile_id
      FROM member_profile mp
      JOIN user_account ua ON ua.user_id = mp.user_id
      WHERE ua.login_id LIKE 'cdv-%'
   ));

DELETE FROM team_application
WHERE team_id IN (
  SELECT team_id FROM team WHERE name LIKE '[CDV]%'
)
   OR applicant_user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   );

DELETE FROM team_invitation
WHERE team_id IN (
  SELECT team_id FROM team WHERE name LIKE '[CDV]%'
)
   OR target_user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   )
   OR inviter_user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   );

DELETE FROM team_closure_snapshot
WHERE team_id IN (
  SELECT team_id FROM team WHERE name LIKE '[CDV]%'
)
   OR JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.namespace')) = 'CDV';

DELETE FROM team_plan_item
WHERE team_id IN (
  SELECT team_id FROM team WHERE name LIKE '[CDV]%'
);

DELETE s
FROM team_recruitment_slot s
JOIN team_recruitment r ON r.recruitment_id = s.recruitment_id
JOIN team t ON t.team_id = r.team_id
WHERE t.name LIKE '[CDV]%';

DELETE r
FROM team_recruitment r
JOIN team t ON t.team_id = r.team_id
WHERE t.name LIKE '[CDV]%';

DELETE tm
FROM team_member tm
JOIN team t ON t.team_id = tm.team_id
WHERE t.name LIKE '[CDV]%';

DELETE tg
FROM team_genre tg
JOIN team t ON t.team_id = tg.team_id
WHERE t.name LIKE '[CDV]%';

DELETE FROM team
WHERE name LIKE '[CDV]%';

DELETE FROM user_follow
WHERE follower_user_id IN (
  SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
)
   OR following_user_id IN (
      SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
   );

DELETE FROM profile_collaboration_condition
WHERE profile_id IN (
  SELECT mp.profile_id
  FROM member_profile mp
  JOIN user_account ua ON ua.user_id = mp.user_id
  WHERE ua.login_id LIKE 'cdv-%'
);

DELETE FROM profile_genre
WHERE profile_id IN (
  SELECT mp.profile_id
  FROM member_profile mp
  JOIN user_account ua ON ua.user_id = mp.user_id
  WHERE ua.login_id LIKE 'cdv-%'
);

DELETE FROM profile_role
WHERE profile_id IN (
  SELECT mp.profile_id
  FROM member_profile mp
  JOIN user_account ua ON ua.user_id = mp.user_id
  WHERE ua.login_id LIKE 'cdv-%'
);

DELETE FROM member_profile
WHERE user_id IN (
  SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-%'
);

DELETE FROM company_application_document
WHERE uploader_user_id IN (
  SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-company-%'
)
   OR company_application_id IN (
      SELECT ca.company_application_id
      FROM company_application ca
      JOIN user_account ua ON ua.user_id = ca.user_id
      WHERE ua.login_id LIKE 'cdv-company-%'
   );

DELETE FROM company_application
WHERE user_id IN (
  SELECT user_id FROM user_account WHERE login_id LIKE 'cdv-company-%'
);

DELETE FROM admin_permission
WHERE user_id IN (
  SELECT user_id FROM user_account WHERE login_id = 'cdv-admin'
)
   OR granted_by IN (
      SELECT user_id FROM user_account WHERE login_id = 'cdv-admin'
   );

DELETE FROM user_account
WHERE login_id LIKE 'cdv-%';

-- Region IDs are resolved by stable region codes.
SET @region_jongno := (SELECT region_id FROM region WHERE region_code = '1111000000');
SET @region_junggu := (SELECT region_id FROM region WHERE region_code = '1114000000');
SET @region_gangnam := (SELECT region_id FROM region WHERE region_code = '1168000000');
SET @region_mapo := (SELECT region_id FROM region WHERE region_code = '1144000000');
SET @region_gwangjin := (SELECT region_id FROM region WHERE region_code = '1121500000');
SET @region_bundang := (SELECT region_id FROM region WHERE region_code = '4113500000');
SET @region_goyang := (SELECT region_id FROM region WHERE region_code = '4128100000');
SET @region_busan := (SELECT region_id FROM region WHERE region_code = '2611000000');
SET @region_daegu := (SELECT region_id FROM region WHERE region_code = '2711000000');
SET @region_gwangju := (SELECT region_id FROM region WHERE region_code = '2915500000');
SET @region_daejeon := (SELECT region_id FROM region WHERE region_code = '3011000000');
SET @region_jeju := (SELECT region_id FROM region WHERE region_code = '5011000000');

SET @role_producer := (SELECT role_id FROM `role` WHERE name = '프로듀서' ORDER BY role_id LIMIT 1);
SET @role_line_producer := (SELECT role_id FROM `role` WHERE name = '라인프로듀서' ORDER BY role_id LIMIT 1);
SET @role_director := (SELECT role_id FROM `role` WHERE name = '감독' ORDER BY role_id LIMIT 1);
SET @role_assistant_director := (SELECT role_id FROM `role` WHERE name = '조감독' ORDER BY role_id LIMIT 1);
SET @role_writer := (SELECT role_id FROM `role` WHERE name = '시나리오 작가' ORDER BY role_id LIMIT 1);
SET @role_camera := (SELECT role_id FROM `role` WHERE name = '촬영감독' ORDER BY role_id LIMIT 1);
SET @role_light := (SELECT role_id FROM `role` WHERE name = '조명감독' ORDER BY role_id LIMIT 1);
SET @role_sound := (SELECT role_id FROM `role` WHERE name = '동시녹음' ORDER BY role_id LIMIT 1);
SET @role_art := (SELECT role_id FROM `role` WHERE name = '미술감독' ORDER BY role_id LIMIT 1);
SET @role_costume := (SELECT role_id FROM `role` WHERE name = '의상' ORDER BY role_id LIMIT 1);
SET @role_actor := (SELECT role_id FROM `role` WHERE name = '배우' ORDER BY role_id LIMIT 1);
SET @role_editor := (SELECT role_id FROM `role` WHERE name = '영상 편집' ORDER BY role_id LIMIT 1);
SET @role_color := (SELECT role_id FROM `role` WHERE name = '색보정' ORDER BY role_id LIMIT 1);
SET @role_vfx := (SELECT role_id FROM `role` WHERE name = 'VFX' ORDER BY role_id LIMIT 1);
SET @role_music := (SELECT role_id FROM `role` WHERE name = '음악감독' ORDER BY role_id LIMIT 1);
SET @role_marketing := (SELECT role_id FROM `role` WHERE name = '마케팅' ORDER BY role_id LIMIT 1);

SET @genre_drama := (SELECT genre_id FROM genre WHERE name = '드라마' ORDER BY genre_id LIMIT 1);
SET @genre_romance := (SELECT genre_id FROM genre WHERE name = '로맨스' ORDER BY genre_id LIMIT 1);
SET @genre_comedy := (SELECT genre_id FROM genre WHERE name = '코미디' ORDER BY genre_id LIMIT 1);
SET @genre_thriller := (SELECT genre_id FROM genre WHERE name = '스릴러' ORDER BY genre_id LIMIT 1);
SET @genre_mystery := (SELECT genre_id FROM genre WHERE name = '미스터리' ORDER BY genre_id LIMIT 1);
SET @genre_sf := (SELECT genre_id FROM genre WHERE name = 'SF' ORDER BY genre_id LIMIT 1);
SET @genre_documentary := (SELECT genre_id FROM genre WHERE name = '다큐멘터리' ORDER BY genre_id LIMIT 1);
SET @genre_animation := (SELECT genre_id FROM genre WHERE name = '애니메이션' ORDER BY genre_id LIMIT 1);
SET @genre_art := (SELECT genre_id FROM genre WHERE name = '실험/예술' ORDER BY genre_id LIMIT 1);
SET @genre_music := (SELECT genre_id FROM genre WHERE name = '음악/공연' ORDER BY genre_id LIMIT 1);
SET @genre_youth := (SELECT genre_id FROM genre WHERE name = '청춘/학원' ORDER BY genre_id LIMIT 1);
SET @genre_history := (SELECT genre_id FROM genre WHERE name = '역사/시대극' ORDER BY genre_id LIMIT 1);

-- 32 users, 4 companies, and 1 CDV admin.
INSERT INTO user_account
(login_id, email, password_hash, nickname, phone, account_type, account_status, last_login_at, created_at)
SELECT
  CONCAT('cdv-user-', LPAD(seq.n, 2, '0')),
  CONCAT('cdv-user-', LPAD(seq.n, 2, '0'), '@slate.test'),
  '{noop}slate1234',
  CONCAT('CDV 창작자 ', LPAD(seq.n, 2, '0')),
  NULL,
  'USER',
  'ACTIVE',
  NOW() - INTERVAL MOD(seq.n * 3, 72) HOUR,
  NOW() - INTERVAL (50 - seq.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq;

INSERT INTO user_account
(login_id, email, password_hash, nickname, phone, account_type, account_status, last_login_at, created_at)
SELECT
  CONCAT('cdv-company-', LPAD(seq.n, 2, '0')),
  CONCAT('cdv-company-', LPAD(seq.n, 2, '0'), '@slate.test'),
  '{noop}slate1234',
  CASE seq.n
    WHEN 1 THEN 'CDV 프레임브릿지'
    WHEN 2 THEN 'CDV 로컬씬 스튜디오'
    WHEN 3 THEN 'CDV 뉴웨이브 콘텐츠'
    ELSE 'CDV 시네마루프'
  END,
  NULL,
  'COMPANY',
  'ACTIVE',
  NOW() - INTERVAL seq.n DAY,
  NOW() - INTERVAL (45 - seq.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4),
  '$[*]' COLUMNS (n INT PATH '$')
) seq;

INSERT INTO user_account
(login_id, email, password_hash, nickname, phone, account_type, account_status, last_login_at, created_at)
VALUES
('cdv-admin', 'cdv-admin@slate.test', '{noop}slate1234', 'CDV 운영 관리자', NULL, 'ADMIN', 'ACTIVE', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 60 DAY);

SET @cdv_admin_user := (SELECT user_id FROM user_account WHERE login_id = 'cdv-admin');

INSERT INTO admin_permission (user_id, permission_code, active_yn, granted_by, created_at)
SELECT @cdv_admin_user, cc.code, 'Y', @cdv_admin_user, NOW() - INTERVAL 60 DAY
FROM common_code cc
WHERE cc.code_group = 'ADMIN_PERMISSION'
  AND cc.active_yn = 'Y'
  AND cc.code IN (
    'COMPANY_APPROVAL',
    'USER_SANCTION',
    'CONTENT_MODERATION',
    'SCORE_POLICY',
    'CONTEST_MANAGE',
    'NOTIFICATION_SEND',
    'LOG_VIEW',
    'ADMIN_PERMISSION_MANAGE',
    'REGION_MANAGE'
  );

INSERT INTO company_application
(user_id, company_name, business_registration_no, manager_name, manager_phone, company_intro, public_data_company_name, status, review_reason, reviewed_by, reviewed_at, created_at)
SELECT
  ua.user_id,
  ua.nickname,
  CONCAT('CDV-', LPAD(seq.n, 2, '0'), '-00000'),
  CONCAT('CDV 담당자 ', seq.n),
  '000-0000-0000',
  CASE seq.n
    WHEN 1 THEN '독립영화 제작팀과 신진 창작자를 연결하는 데모 기업입니다.'
    WHEN 2 THEN '지역 로케이션 기반 콘텐츠 제작 지원을 운영하는 데모 기업입니다.'
    WHEN 3 THEN '웹 콘텐츠와 뮤직비디오 공모를 운영하는 데모 기업입니다.'
    ELSE '후반 제작과 배급 연계를 지원하는 데모 기업입니다.'
  END,
  ua.nickname,
  'APPROVED',
  '[CDV] 볼륨 데이터용 승인 회사입니다.',
  @cdv_admin_user,
  NOW() - INTERVAL (30 - seq.n) DAY,
  NOW() - INTERVAL (35 - seq.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN user_account ua
  ON ua.login_id = CONCAT('cdv-company-', LPAD(seq.n, 2, '0'));

-- Complete public profiles with varied matching attributes.
INSERT INTO member_profile
(user_id, display_name, short_intro, detail_intro, visibility, activity_status, region_id, experience_level, join_availability, collaboration_status, travel_range, preferred_duration, equipment_status, age_band, participation_mode, profile_completed_yn, status, last_active_at, created_at)
SELECT
  ua.user_id,
  CONCAT('CDV ', CASE MOD(seq.n - 1, 8)
    WHEN 0 THEN '기획'
    WHEN 1 THEN '연출'
    WHEN 2 THEN '각본'
    WHEN 3 THEN '촬영'
    WHEN 4 THEN '사운드'
    WHEN 5 THEN '미술'
    WHEN 6 THEN '연기'
    ELSE '후반'
  END, ' 창작자 ', LPAD(seq.n, 2, '0')),
  CONCAT('프로젝트 ', LPAD(seq.n, 2, '0'), '에서 역할과 일정이 분명한 협업을 선호합니다.'),
  CONCAT('CDV 볼륨 화면 검증을 위한 공개 프로필입니다. 지역, 경력, 합류 시점, 협업 조건을 조합한 창작자 ', LPAD(seq.n, 2, '0'), '의 소개입니다.'),
  'PUBLIC',
  'VISIBLE',
  CASE MOD(seq.n - 1, 12)
    WHEN 0 THEN @region_jongno
    WHEN 1 THEN @region_junggu
    WHEN 2 THEN @region_gangnam
    WHEN 3 THEN @region_mapo
    WHEN 4 THEN @region_gwangjin
    WHEN 5 THEN @region_bundang
    WHEN 6 THEN @region_goyang
    WHEN 7 THEN @region_busan
    WHEN 8 THEN @region_daegu
    WHEN 9 THEN @region_gwangju
    WHEN 10 THEN @region_daejeon
    ELSE @region_jeju
  END,
  CASE MOD(seq.n - 1, 3) WHEN 0 THEN 'Y0_3' WHEN 1 THEN 'Y3_10' ELSE 'Y10_PLUS' END,
  CASE MOD(seq.n - 1, 6)
    WHEN 0 THEN 'IMMEDIATE'
    WHEN 1 THEN 'WITHIN_1W'
    WHEN 2 THEN 'WITHIN_2W'
    WHEN 3 THEN 'WITHIN_1M'
    WHEN 4 THEN 'AFTER_1M'
    ELSE 'NEGOTIABLE'
  END,
  CASE MOD(seq.n - 1, 3) WHEN 0 THEN 'AVAILABLE' WHEN 1 THEN 'CONSIDERING' ELSE 'AVAILABLE' END,
  CASE MOD(seq.n - 1, 4) WHEN 0 THEN 'KM_10' WHEN 1 THEN 'KM_30' WHEN 2 THEN 'KM_100' ELSE 'ANYWHERE' END,
  CASE MOD(seq.n - 1, 4) WHEN 0 THEN 'WITHIN_1M' WHEN 1 THEN 'WITHIN_3M' WHEN 2 THEN 'WITHIN_6M' ELSE 'ANY' END,
  CASE MOD(seq.n - 1, 3) WHEN 0 THEN 'HAS_EQUIPMENT' WHEN 1 THEN 'NO_EQUIPMENT' ELSE 'NOT_ENTERED' END,
  CASE MOD(seq.n - 1, 5) WHEN 0 THEN 'TWENTIES' WHEN 1 THEN 'THIRTIES' WHEN 2 THEN 'FORTIES' WHEN 3 THEN 'TEENS' ELSE 'PRIVATE' END,
  CASE MOD(seq.n - 1, 3) WHEN 0 THEN 'OFFLINE' WHEN 1 THEN 'REMOTE' ELSE 'HYBRID' END,
  'Y',
  'ACTIVE',
  NOW() - INTERVAL MOD(seq.n * 3, 72) HOUR,
  NOW() - INTERVAL (50 - seq.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN user_account ua
  ON ua.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'));

INSERT INTO profile_role (profile_id, role_id, sort_order)
SELECT
  mp.profile_id,
  CASE MOD(seq.n - 1, 16)
    WHEN 0 THEN @role_producer
    WHEN 1 THEN @role_line_producer
    WHEN 2 THEN @role_assistant_director
    WHEN 3 THEN @role_director
    WHEN 4 THEN @role_writer
    WHEN 5 THEN @role_camera
    WHEN 6 THEN @role_light
    WHEN 7 THEN @role_sound
    WHEN 8 THEN @role_art
    WHEN 9 THEN @role_costume
    WHEN 10 THEN @role_actor
    WHEN 11 THEN @role_editor
    WHEN 12 THEN @role_color
    WHEN 13 THEN @role_vfx
    WHEN 14 THEN @role_music
    ELSE @role_marketing
  END,
  0
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN user_account ua
  ON ua.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'))
JOIN member_profile mp ON mp.user_id = ua.user_id;

INSERT INTO profile_role (profile_id, role_id, sort_order)
SELECT
  mp.profile_id,
  CASE MOD(seq.n, 16)
    WHEN 4 THEN @role_producer
    WHEN 8 THEN @role_editor
    WHEN 12 THEN @role_writer
    ELSE @role_camera
  END,
  1
FROM JSON_TABLE(
  JSON_ARRAY(4,8,12,16,20,24,28,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN user_account ua
  ON ua.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'))
JOIN member_profile mp ON mp.user_id = ua.user_id;

INSERT INTO profile_genre (profile_id, genre_id)
SELECT
  mp.profile_id,
  CASE MOD(seq.n + offsets.k - 2, 12)
    WHEN 0 THEN @genre_drama
    WHEN 1 THEN @genre_romance
    WHEN 2 THEN @genre_comedy
    WHEN 3 THEN @genre_thriller
    WHEN 4 THEN @genre_mystery
    WHEN 5 THEN @genre_sf
    WHEN 6 THEN @genre_documentary
    WHEN 7 THEN @genre_animation
    WHEN 8 THEN @genre_art
    WHEN 9 THEN @genre_music
    WHEN 10 THEN @genre_youth
    ELSE @genre_history
  END
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,5),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account ua
  ON ua.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'))
JOIN member_profile mp ON mp.user_id = ua.user_id;

INSERT INTO profile_collaboration_condition (profile_id, condition_code)
SELECT
  mp.profile_id,
  CASE MOD(seq.n + offsets.k - 2, 6)
    WHEN 0 THEN 'UNPAID'
    WHEN 1 THEN 'NEGOTIABLE'
    WHEN 2 THEN 'PAID'
    WHEN 3 THEN 'REVENUE_SHARE'
    WHEN 4 THEN 'PRIZE_SHARE'
    ELSE 'ANY'
  END
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,3),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account ua
  ON ua.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'))
JOIN member_profile mp ON mp.user_id = ua.user_id;

INSERT INTO user_follow (follower_user_id, following_user_id, created_at)
SELECT
  follower.user_id,
  following_user.user_id,
  NOW() - INTERVAL MOD(seq.n * offsets.k, 30) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account follower
  ON follower.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'))
JOIN user_account following_user
  ON following_user.login_id = CONCAT('cdv-user-', LPAD(MOD(seq.n - 1 + offsets.k, 32) + 1, 2, '0'));

-- Twelve teams cover all major lifecycle states.
INSERT INTO team
(leader_user_id, name, description, status, end_type, region_id, region_any_yn, expected_duration, max_member_count, current_member_count, recruitment_reopen_count, last_active_at, created_at)
SELECT
  leader.user_id,
  CONCAT('[CDV] ', LPAD(seq.n, 2, '0'), ' ', CASE seq.n
    WHEN 1 THEN '새벽시장 단편팀'
    WHEN 2 THEN '옥상 로맨스 제작팀'
    WHEN 3 THEN '골목 미스터리팀'
    WHEN 4 THEN '청춘 음악영화팀'
    WHEN 5 THEN '제주 관찰 다큐팀'
    WHEN 6 THEN '부산 항구 스릴러팀'
    WHEN 7 THEN '도심 SF 웹콘텐츠팀'
    WHEN 8 THEN '가족 애니메이션팀'
    WHEN 9 THEN '시대극 프리비즈팀'
    WHEN 10 THEN '공연 실황 후반팀'
    WHEN 11 THEN '완료된 브랜드필름팀'
    ELSE '종료된 지역기록팀'
  END),
  CONCAT('CDV 볼륨 검증용 팀 ', LPAD(seq.n, 2, '0'), '입니다. 역할별 모집, 일정, 작업물, 공모전 연결 상태를 확인합니다.'),
  CASE
    WHEN seq.n <= 4 THEN 'RECRUITING'
    WHEN seq.n <= 7 THEN 'IN_PROGRESS'
    WHEN seq.n <= 9 THEN 'RECRUITMENT_CLOSED'
    WHEN seq.n = 10 THEN 'CLOSING'
    ELSE 'ENDED'
  END,
  CASE WHEN seq.n = 11 THEN 'NORMAL' WHEN seq.n = 12 THEN 'DISSOLUTION' ELSE NULL END,
  CASE MOD(seq.n - 1, 12)
    WHEN 0 THEN @region_jongno
    WHEN 1 THEN @region_junggu
    WHEN 2 THEN @region_gangnam
    WHEN 3 THEN @region_mapo
    WHEN 4 THEN @region_gwangjin
    WHEN 5 THEN @region_bundang
    WHEN 6 THEN @region_goyang
    WHEN 7 THEN @region_busan
    WHEN 8 THEN @region_daegu
    WHEN 9 THEN @region_gwangju
    WHEN 10 THEN @region_daejeon
    ELSE @region_jeju
  END,
  CASE WHEN seq.n IN (5, 7, 10) THEN 'Y' ELSE 'N' END,
  CASE MOD(seq.n - 1, 4) WHEN 0 THEN 'WITHIN_1M' WHEN 1 THEN 'WITHIN_3M' WHEN 2 THEN 'WITHIN_6M' ELSE 'ANY' END,
  8,
  1,
  CASE WHEN seq.n IN (3, 6) THEN 1 ELSE 0 END,
  NOW() - INTERVAL seq.n HOUR,
  NOW() - INTERVAL (40 - seq.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN user_account leader
  ON leader.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'));

INSERT INTO team_genre (team_id, genre_id)
SELECT
  t.team_id,
  CASE MOD(seq.n + offsets.k - 2, 12)
    WHEN 0 THEN @genre_drama
    WHEN 1 THEN @genre_romance
    WHEN 2 THEN @genre_comedy
    WHEN 3 THEN @genre_thriller
    WHEN 4 THEN @genre_mystery
    WHEN 5 THEN @genre_sf
    WHEN 6 THEN @genre_documentary
    WHEN 7 THEN @genre_animation
    WHEN 8 THEN @genre_art
    WHEN 9 THEN @genre_music
    WHEN 10 THEN @genre_youth
    ELSE @genre_history
  END
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,5),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN team t
  ON t.name LIKE CONCAT('[CDV] ', LPAD(seq.n, 2, '0'), ' %');

INSERT INTO team_member (team_id, user_id, team_role, status, joined_at)
SELECT
  t.team_id,
  leader.user_id,
  'LEADER',
  'ACTIVE',
  t.created_at
FROM team t
JOIN user_account leader ON leader.user_id = t.leader_user_id
WHERE t.name LIKE '[CDV]%';

INSERT INTO team_recruitment
(team_id, title, status, deadline_at, work_start_at, created_by, created_at)
SELECT
  t.team_id,
  CONCAT('[CDV] T', LPAD(seq.n, 2, '0'), '-R', round_no.n, ' 역할 모집'),
  CASE WHEN seq.n <= 7 THEN 'OPEN' ELSE 'CLOSED' END,
  CASE WHEN seq.n <= 7 THEN NOW() + INTERVAL (10 + seq.n + round_no.n * 5) DAY ELSE NOW() - INTERVAL (seq.n + round_no.n) DAY END,
  CASE WHEN seq.n <= 10 THEN NOW() + INTERVAL (20 + seq.n) DAY ELSE NOW() - INTERVAL 10 DAY END,
  t.leader_user_id,
  NOW() - INTERVAL (35 - seq.n + round_no.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2),
  '$[*]' COLUMNS (n INT PATH '$')
) round_no
JOIN team t
  ON t.name LIKE CONCAT('[CDV] ', LPAD(seq.n, 2, '0'), ' %');

INSERT INTO team_recruitment_slot
(recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status)
SELECT
  tr.recruitment_id,
  CASE MOD(seq.n + slot_no.n - 2, 12)
    WHEN 0 THEN @role_producer
    WHEN 1 THEN @role_director
    WHEN 2 THEN @role_writer
    WHEN 3 THEN @role_camera
    WHEN 4 THEN @role_light
    WHEN 5 THEN @role_sound
    WHEN 6 THEN @role_art
    WHEN 7 THEN @role_costume
    WHEN 8 THEN @role_actor
    WHEN 9 THEN @role_editor
    WHEN 10 THEN @role_vfx
    ELSE @role_music
  END,
  1,
  0,
  CASE MOD(seq.n + slot_no.n, 3) WHEN 0 THEN 'Y0_3' WHEN 1 THEN 'Y3_10' ELSE 'Y10_PLUS' END,
  CASE MOD(seq.n + slot_no.n, 5) WHEN 0 THEN 'UNPAID' WHEN 1 THEN 'NEGOTIABLE' WHEN 2 THEN 'PAID' WHEN 3 THEN 'REVENUE_SHARE' ELSE 'PRIZE_SHARE' END,
  CASE WHEN slot_no.n <= 3 THEN 'Y' ELSE 'N' END,
  CASE MOD(seq.n - 1, 4) WHEN 0 THEN 'WITHIN_1M' WHEN 1 THEN 'WITHIN_3M' WHEN 2 THEN 'WITHIN_6M' ELSE 'ANY' END,
  CASE WHEN MOD(slot_no.n, 2) = 0 THEN 'Y' ELSE 'N' END,
  CASE WHEN seq.n <= 7 AND slot_no.n >= 3 THEN 'OPEN' ELSE 'CLOSED' END
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5),
  '$[*]' COLUMNS (n INT PATH '$')
) slot_no
JOIN team t
  ON t.name LIKE CONCAT('[CDV] ', LPAD(seq.n, 2, '0'), ' %')
JOIN team_recruitment tr
  ON tr.team_id = t.team_id
 AND tr.title = CONCAT('[CDV] T', LPAD(seq.n, 2, '0'), '-R', CASE WHEN slot_no.n <= 3 THEN 1 ELSE 2 END, ' 역할 모집');

-- Five applications and three invitations per team.
INSERT INTO team_application
(team_id, recruitment_id, slot_id, applicant_user_id, message, status, reject_reason, decided_by, decided_at, created_at, updated_at)
SELECT
  slots.team_id,
  slots.recruitment_id,
  slots.slot_id,
  applicant.user_id,
  CONCAT('[CDV] T', LPAD(slots.team_no, 2, '0'), ' ', status_no.status, ' 지원 시나리오입니다.'),
  status_no.status,
  CASE WHEN status_no.status = 'REJECTED' THEN '역할 요구 조건과 일정이 맞지 않아 반려했습니다.' ELSE NULL END,
  CASE WHEN status_no.status IN ('ACCEPTED', 'REJECTED') THEN leader.user_id ELSE NULL END,
  CASE WHEN status_no.status IN ('ACCEPTED', 'REJECTED') THEN NOW() - INTERVAL (12 - slots.team_no) DAY ELSE NULL END,
  NOW() - INTERVAL (20 - slots.team_no + status_no.slot_no) DAY,
  CASE WHEN status_no.status IN ('CANCELED', 'EXPIRED') THEN NOW() - INTERVAL (10 - MOD(slots.team_no, 5)) DAY ELSE NULL END
FROM (
  SELECT
    t.team_id,
    t.leader_user_id,
    CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(t.name, ' ', 2), ' ', -1) AS UNSIGNED) AS team_no,
    s.slot_id,
    s.recruitment_id,
    ROW_NUMBER() OVER (PARTITION BY t.team_id ORDER BY tr.title, s.slot_id) AS slot_no
  FROM team t
  JOIN team_recruitment tr ON tr.team_id = t.team_id
  JOIN team_recruitment_slot s ON s.recruitment_id = tr.recruitment_id
  WHERE t.name LIKE '[CDV]%'
) slots
JOIN JSON_TABLE(
  JSON_ARRAY(
    JSON_OBJECT('slot_no', 1, 'status', 'ACCEPTED'),
    JSON_OBJECT('slot_no', 3, 'status', 'PENDING'),
    JSON_OBJECT('slot_no', 4, 'status', 'REJECTED'),
    JSON_OBJECT('slot_no', 5, 'status', 'CANCELED'),
    JSON_OBJECT('slot_no', 3, 'status', 'EXPIRED')
  ),
  '$[*]' COLUMNS (
    slot_no INT PATH '$.slot_no',
    status VARCHAR(30) PATH '$.status'
  )
) status_no ON status_no.slot_no = slots.slot_no
JOIN user_account leader ON leader.user_id = slots.leader_user_id
JOIN user_account applicant
  ON applicant.login_id = CONCAT(
    'cdv-user-',
    LPAD(
      CASE status_no.status
        WHEN 'ACCEPTED' THEN slots.team_no * 2 + 1
        WHEN 'PENDING' THEN 27 + MOD(slots.team_no - 1, 6)
        WHEN 'REJECTED' THEN MOD(slots.team_no + 10, 32) + 1
        WHEN 'CANCELED' THEN MOD(slots.team_no + 16, 32) + 1
        ELSE MOD(slots.team_no + 22, 32) + 1
      END,
      2,
      '0'
    )
  );

INSERT INTO team_invitation
(team_id, recruitment_id, slot_id, target_user_id, inviter_user_id, message, status, decided_at, created_at, updated_at)
SELECT
  slots.team_id,
  slots.recruitment_id,
  slots.slot_id,
  target_user.user_id,
  leader.user_id,
  CONCAT(
    '[CDV] T',
    LPAD(slots.team_no, 2, '0'),
    ' ',
    CASE
      WHEN status_seed.status_key = 'MIXED' AND MOD(slots.team_no, 2) = 0 THEN 'EXPIRED'
      WHEN status_seed.status_key = 'MIXED' THEN 'CANCELED'
      ELSE status_seed.status_key
    END,
    ' 초대 시나리오입니다.'
  ),
  CASE
    WHEN status_seed.status_key = 'MIXED' AND MOD(slots.team_no, 2) = 0 THEN 'EXPIRED'
    WHEN status_seed.status_key = 'MIXED' THEN 'CANCELED'
    ELSE status_seed.status_key
  END,
  CASE WHEN status_seed.status_key = 'ACCEPTED' THEN NOW() - INTERVAL (10 - MOD(slots.team_no, 5)) DAY ELSE NULL END,
  NOW() - INTERVAL (18 - slots.team_no + status_seed.slot_no) DAY,
  CASE WHEN status_seed.status_key = 'MIXED' THEN NOW() - INTERVAL (8 - MOD(slots.team_no, 4)) DAY ELSE NULL END
FROM (
  SELECT
    t.team_id,
    t.leader_user_id,
    CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(t.name, ' ', 2), ' ', -1) AS UNSIGNED) AS team_no,
    s.slot_id,
    s.recruitment_id,
    ROW_NUMBER() OVER (PARTITION BY t.team_id ORDER BY tr.title, s.slot_id) AS slot_no
  FROM team t
  JOIN team_recruitment tr ON tr.team_id = t.team_id
  JOIN team_recruitment_slot s ON s.recruitment_id = tr.recruitment_id
  WHERE t.name LIKE '[CDV]%'
) slots
JOIN JSON_TABLE(
  JSON_ARRAY(
    JSON_OBJECT('slot_no', 2, 'status_key', 'ACCEPTED'),
    JSON_OBJECT('slot_no', 4, 'status_key', 'PENDING'),
    JSON_OBJECT('slot_no', 5, 'status_key', 'MIXED')
  ),
  '$[*]' COLUMNS (
    slot_no INT PATH '$.slot_no',
    status_key VARCHAR(30) PATH '$.status_key'
  )
) status_seed ON status_seed.slot_no = slots.slot_no
JOIN user_account leader ON leader.user_id = slots.leader_user_id
JOIN user_account target_user
  ON target_user.login_id = CONCAT(
    'cdv-user-',
    LPAD(
      CASE status_seed.status_key
        WHEN 'ACCEPTED' THEN slots.team_no * 2 + 2
        WHEN 'PENDING' THEN 27 + MOD(slots.team_no + 2, 6)
        ELSE MOD(slots.team_no + 5, 32) + 1
      END,
      2,
      '0'
    )
  );

INSERT INTO team_member (team_id, user_id, team_role, status, joined_at)
SELECT ta.team_id, ta.applicant_user_id, 'MEMBER', 'ACTIVE', ta.decided_at
FROM team_application ta
JOIN team t ON t.team_id = ta.team_id
WHERE t.name LIKE '[CDV]%'
  AND ta.status = 'ACCEPTED';

INSERT INTO team_member (team_id, user_id, team_role, status, joined_at)
SELECT ti.team_id, ti.target_user_id, 'MEMBER', 'ACTIVE', ti.decided_at
FROM team_invitation ti
JOIN team t ON t.team_id = ti.team_id
WHERE t.name LIKE '[CDV]%'
  AND ti.status = 'ACCEPTED';

UPDATE team t
SET t.current_member_count = (
  SELECT COUNT(*)
  FROM team_member tm
  WHERE tm.team_id = t.team_id
    AND tm.status = 'ACTIVE'
)
WHERE t.name LIKE '[CDV]%';

UPDATE team_recruitment_slot s
JOIN team_recruitment tr ON tr.recruitment_id = s.recruitment_id
JOIN team t ON t.team_id = tr.team_id
SET s.accepted_count = (
    SELECT COUNT(*)
    FROM team_application ta
    WHERE ta.slot_id = s.slot_id
      AND ta.status = 'ACCEPTED'
  ) + (
    SELECT COUNT(*)
    FROM team_invitation ti
    WHERE ti.slot_id = s.slot_id
      AND ti.status = 'ACCEPTED'
  ),
  s.status = CASE
    WHEN tr.status = 'CLOSED' THEN 'CLOSED'
    WHEN (
      SELECT COUNT(*)
      FROM team_application ta
      WHERE ta.slot_id = s.slot_id
        AND ta.status = 'ACCEPTED'
    ) + (
      SELECT COUNT(*)
      FROM team_invitation ti
      WHERE ti.slot_id = s.slot_id
        AND ti.status = 'ACCEPTED'
    ) >= s.required_count THEN 'CLOSED'
    ELSE 'OPEN'
  END,
  s.updated_at = NOW()
WHERE t.name LIKE '[CDV]%';

INSERT INTO team_plan_item
(team_id, title, description, assignee_user_id, role_id, due_at, status, created_by, created_at)
SELECT
  t.team_id,
  CONCAT('[CDV] T', LPAD(seq.n, 2, '0'), '-P', plan_no.n, ' ', CASE plan_no.n
    WHEN 1 THEN '기획안 잠금'
    WHEN 2 THEN '로케이션 점검'
    WHEN 3 THEN '촬영 준비'
    WHEN 4 THEN '후반 리뷰'
    ELSE '공개 일정 확인'
  END),
  CONCAT('팀 ', LPAD(seq.n, 2, '0'), '의 볼륨 검증용 일정 항목입니다.'),
  assignee.user_id,
  CASE plan_no.n
    WHEN 1 THEN @role_producer
    WHEN 2 THEN @role_director
    WHEN 3 THEN @role_camera
    WHEN 4 THEN @role_editor
    ELSE @role_marketing
  END,
  CASE WHEN seq.n >= 11 THEN NOW() - INTERVAL plan_no.n DAY ELSE NOW() + INTERVAL (seq.n + plan_no.n) DAY END,
  CASE
    WHEN seq.n >= 11 AND plan_no.n <= 4 THEN 'DONE'
    WHEN seq.n >= 11 THEN 'CANCELED'
    WHEN plan_no.n = 1 THEN 'DONE'
    WHEN plan_no.n = 2 THEN 'IN_PROGRESS'
    WHEN plan_no.n = 3 THEN 'TODO'
    WHEN plan_no.n = 4 THEN 'HOLD'
    ELSE 'TODO'
  END,
  t.leader_user_id,
  NOW() - INTERVAL (20 - seq.n + plan_no.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5),
  '$[*]' COLUMNS (n INT PATH '$')
) plan_no
JOIN team t
  ON t.name LIKE CONCAT('[CDV] ', LPAD(seq.n, 2, '0'), ' %')
JOIN user_account assignee
  ON assignee.login_id = CONCAT('cdv-user-', LPAD(seq.n * 2 + CASE WHEN plan_no.n = 1 THEN 1 ELSE 2 END, 2, '0'));

INSERT INTO team_closure_snapshot
(team_id, end_type, snapshot_json, created_by, created_at)
SELECT
  t.team_id,
  t.end_type,
  JSON_OBJECT(
    'namespace', 'CDV',
    'teamName', t.name,
    'status', t.status,
    'endType', t.end_type,
    'activeMemberCount', t.current_member_count,
    'recruitmentState', 'CLOSED',
    'scenario', CASE WHEN t.end_type = 'NORMAL' THEN 'volume-normal-close' ELSE 'volume-dissolution' END
  ),
  t.leader_user_id,
  NOW() - INTERVAL (13 - CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(t.name, ' ', 2), ' ', -1) AS UNSIGNED)) DAY
FROM team t
WHERE t.name LIKE '[CDV]%'
  AND t.status = 'ENDED';

-- Matching bookmarks and actions.
INSERT INTO matching_bookmark (user_id, target_type, target_id, created_at)
SELECT
  actor.user_id,
  CASE WHEN offsets.k <= 2 THEN 'PROFILE' ELSE 'TEAM' END,
  CASE
    WHEN offsets.k <= 2 THEN target_profile.profile_id
    ELSE target_team.team_id
  END,
  NOW() - INTERVAL MOD(seq.n + offsets.k, 25) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account actor
  ON actor.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'))
LEFT JOIN user_account target_user
  ON offsets.k <= 2
 AND target_user.login_id = CONCAT('cdv-user-', LPAD(MOD(seq.n - 1 + offsets.k * 5, 32) + 1, 2, '0'))
LEFT JOIN member_profile target_profile
  ON target_profile.user_id = target_user.user_id
LEFT JOIN team target_team
  ON offsets.k = 3
 AND target_team.name LIKE CONCAT('[CDV] ', LPAD(MOD(seq.n - 1, 12) + 1, 2, '0'), ' %');

INSERT INTO matching_action_log
(actor_user_id, action_type, target_type, target_id, team_id, role_id, created_at)
SELECT
  actor.user_id,
  CASE offsets.k
    WHEN 1 THEN 'CDV_PROFILE_VIEW'
    WHEN 2 THEN 'CDV_PROFILE_BOOKMARK'
    ELSE 'CDV_TEAM_BOOKMARK'
  END,
  CASE WHEN offsets.k <= 2 THEN 'PROFILE' ELSE 'TEAM' END,
  CASE WHEN offsets.k <= 2 THEN target_profile.profile_id ELSE target_team.team_id END,
  CASE WHEN offsets.k = 3 THEN target_team.team_id ELSE NULL END,
  CASE offsets.k WHEN 1 THEN @role_camera WHEN 2 THEN @role_editor ELSE @role_writer END,
  NOW() - INTERVAL MOD(seq.n * offsets.k, 20) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account actor
  ON actor.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'))
LEFT JOIN user_account target_user
  ON offsets.k <= 2
 AND target_user.login_id = CONCAT('cdv-user-', LPAD(MOD(seq.n - 1 + offsets.k * 5, 32) + 1, 2, '0'))
LEFT JOIN member_profile target_profile
  ON target_profile.user_id = target_user.user_id
LEFT JOIN team target_team
  ON offsets.k = 3
 AND target_team.name LIKE CONCAT('[CDV] ', LPAD(MOD(seq.n - 1, 12) + 1, 2, '0'), ' %');

-- Sixty posts: 30 work posts and 30 free posts.
INSERT INTO board_post
(author_user_id, category, free_category, title, content, status, visibility, like_count, review_count, view_count, created_at)
SELECT
  author.user_id,
  CASE WHEN seq.n <= 30 THEN 'WORK' ELSE 'FREE' END,
  CASE
    WHEN seq.n <= 30 THEN NULL
    WHEN MOD(seq.n, 5) = 0 THEN 'NOTICE'
    WHEN MOD(seq.n, 5) = 1 THEN 'QUESTION'
    WHEN MOD(seq.n, 5) = 2 THEN 'INFO'
    WHEN MOD(seq.n, 5) = 3 THEN 'REVIEW'
    ELSE 'FREE'
  END,
  CASE
    WHEN seq.n <= 30 THEN CONCAT('[CDV] 작업물 ', LPAD(seq.n, 2, '0'), ' 제작 기록')
    ELSE CONCAT('[CDV] 자유글 ', LPAD(seq.n, 2, '0'), ' 현장 이야기')
  END,
  CASE
    WHEN seq.n <= 30 THEN CONCAT('작업물 ', LPAD(seq.n, 2, '0'), '의 기획, 촬영, 후반 과정과 팀 협업 기록입니다.')
    ELSE CONCAT('창작 현장에서 얻은 질문과 정보, 후기, 자유로운 의견을 나누는 게시글 ', LPAD(seq.n, 2, '0'), '입니다.')
  END,
  'PUBLISHED',
  CASE WHEN MOD(seq.n, 6) = 0 THEN 'COMPANY' ELSE 'PUBLIC' END,
  0,
  0,
  0,
  NOW() - INTERVAL MOD(61 - seq.n, 30) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN user_account author
  ON author.login_id = CONCAT('cdv-user-', LPAD(MOD(seq.n - 1, 32) + 1, 2, '0'));

INSERT INTO work_item
(owner_user_id, team_id, board_post_id, title, description, media_type, work_type, visibility, status, created_at)
SELECT
  owner_user.user_id,
  CASE WHEN seq.n <= 24 THEN t.team_id ELSE NULL END,
  bp.post_id,
  CONCAT('[CDV] 작업물 ', LPAD(seq.n, 2, '0'), ' 제작 기록'),
  CONCAT('게시글과 연결된 CDV 작업물 ', LPAD(seq.n, 2, '0'), '입니다.'),
  'MANUAL',
  CASE MOD(seq.n - 1, 7)
    WHEN 0 THEN 'SHORT_FILM'
    WHEN 1 THEN 'FEATURE_FILM'
    WHEN 2 THEN 'MUSIC_VIDEO'
    WHEN 3 THEN 'ADVERTISEMENT'
    WHEN 4 THEN 'DOCUMENTARY'
    WHEN 5 THEN 'WEB_CONTENT'
    ELSE 'OTHER'
  END,
  CASE WHEN MOD(seq.n, 6) = 0 THEN 'COMPANY' ELSE 'PUBLIC' END,
  'PUBLISHED',
  bp.created_at
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN board_post bp
  ON bp.title = CONCAT('[CDV] 작업물 ', LPAD(seq.n, 2, '0'), ' 제작 기록')
JOIN user_account owner_user
  ON owner_user.login_id = CONCAT('cdv-user-', LPAD(MOD(seq.n - 1, 32) + 1, 2, '0'))
LEFT JOIN team t
  ON t.name LIKE CONCAT('[CDV] ', LPAD(MOD(seq.n - 1, 12) + 1, 2, '0'), ' %');

INSERT INTO work_item
(owner_user_id, team_id, board_post_id, title, description, media_type, work_type, visibility, status, created_at)
SELECT
  owner_user.user_id,
  NULL,
  NULL,
  CONCAT('[CDV] 독립 작업물 ', LPAD(seq.n, 2, '0')),
  CONCAT('게시글 없이 프로필과 홈 카드 볼륨을 보강하는 독립 작업물 ', LPAD(seq.n, 2, '0'), '입니다.'),
  'MANUAL',
  CASE MOD(seq.n, 3) WHEN 0 THEN 'SHORT_FILM' WHEN 1 THEN 'MUSIC_VIDEO' ELSE 'WEB_CONTENT' END,
  'PUBLIC',
  'PUBLISHED',
  NOW() - INTERVAL seq.n DAY
FROM JSON_TABLE(
  JSON_ARRAY(31,32,33,34,35,36),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN user_account owner_user
  ON owner_user.login_id = CONCAT('cdv-user-', LPAD(MOD(seq.n - 1, 32) + 1, 2, '0'));

INSERT INTO work_genre (work_id, genre_id, sort_order)
SELECT
  wi.work_id,
  CASE MOD(work_seq.n + offsets.k - 2, 12)
    WHEN 0 THEN @genre_drama
    WHEN 1 THEN @genre_romance
    WHEN 2 THEN @genre_comedy
    WHEN 3 THEN @genre_thriller
    WHEN 4 THEN @genre_mystery
    WHEN 5 THEN @genre_sf
    WHEN 6 THEN @genre_documentary
    WHEN 7 THEN @genre_animation
    WHEN 8 THEN @genre_art
    WHEN 9 THEN @genre_music
    WHEN 10 THEN @genre_youth
    ELSE @genre_history
  END,
  offsets.k - 1
FROM (
  SELECT
    wi.work_id,
    ROW_NUMBER() OVER (ORDER BY wi.work_id) AS n
  FROM work_item wi
  WHERE wi.title LIKE '[CDV]%'
) work_seq
JOIN work_item wi ON wi.work_id = work_seq.work_id
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,5),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets;

INSERT INTO board_review
(post_id, author_user_id, parent_review_id, content, status, created_at)
SELECT
  bp.post_id,
  author.user_id,
  NULL,
  CONCAT('[CDV] P', LPAD(post_seq.n, 2, '0'), '-R', review_no.n, ' ', CASE review_no.n
    WHEN 1 THEN '구성과 역할 분담이 명확해서 참고가 됩니다.'
    ELSE '다음 제작 과정과 후반 결과도 기대합니다.'
  END),
  'PUBLISHED',
  bp.created_at + INTERVAL review_no.n DAY
FROM (
  SELECT bp.post_id, ROW_NUMBER() OVER (ORDER BY bp.post_id) AS n
  FROM board_post bp
  WHERE bp.title LIKE '[CDV]%'
) post_seq
JOIN board_post bp ON bp.post_id = post_seq.post_id
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2),
  '$[*]' COLUMNS (n INT PATH '$')
) review_no
JOIN user_account author
  ON author.login_id = CONCAT('cdv-user-', LPAD(MOD(post_seq.n + review_no.n * 3 - 1, 32) + 1, 2, '0'));

INSERT INTO board_review
(post_id, author_user_id, parent_review_id, content, status, created_at)
SELECT
  bp.post_id,
  author.user_id,
  parent.review_id,
  CONCAT('[CDV] P', LPAD(post_seq.n, 2, '0'), '-R3 답변과 보충 의견을 남깁니다.'),
  'PUBLISHED',
  bp.created_at + INTERVAL 3 DAY
FROM (
  SELECT bp.post_id, ROW_NUMBER() OVER (ORDER BY bp.post_id) AS n
  FROM board_post bp
  WHERE bp.title LIKE '[CDV]%'
) post_seq
JOIN board_post bp ON bp.post_id = post_seq.post_id
JOIN board_review parent
  ON parent.post_id = bp.post_id
 AND parent.content LIKE CONCAT('[CDV] P', LPAD(post_seq.n, 2, '0'), '-R1 %')
JOIN user_account author
  ON author.login_id = CONCAT('cdv-user-', LPAD(MOD(post_seq.n + 8 - 1, 32) + 1, 2, '0'));

INSERT INTO board_like (post_id, user_id, active_yn, created_at)
SELECT
  bp.post_id,
  liker.user_id,
  'Y',
  bp.created_at + INTERVAL offsets.k HOUR
FROM (
  SELECT bp.post_id, ROW_NUMBER() OVER (ORDER BY bp.post_id) AS n
  FROM board_post bp
  WHERE bp.title LIKE '[CDV]%'
) post_seq
JOIN board_post bp ON bp.post_id = post_seq.post_id
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account liker
  ON liker.login_id = CONCAT('cdv-user-', LPAD(MOD(post_seq.n + offsets.k * 4 - 1, 32) + 1, 2, '0'));

INSERT INTO board_view_log
(post_id, viewer_user_id, ip_hash, view_window_start, created_at)
SELECT
  bp.post_id,
  viewer.user_id,
  CONCAT('cdv-view-', LPAD(post_seq.n, 2, '0'), '-', offsets.k),
  DATE_FORMAT(bp.created_at + INTERVAL offsets.k HOUR, '%Y-%m-%d %H:00:00'),
  bp.created_at + INTERVAL offsets.k HOUR
FROM (
  SELECT bp.post_id, ROW_NUMBER() OVER (ORDER BY bp.post_id) AS n
  FROM board_post bp
  WHERE bp.title LIKE '[CDV]%'
) post_seq
JOIN board_post bp ON bp.post_id = post_seq.post_id
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account viewer
  ON viewer.login_id = CONCAT('cdv-user-', LPAD(MOD(post_seq.n + offsets.k * 3 - 1, 32) + 1, 2, '0'));

UPDATE board_post bp
SET bp.like_count = (
    SELECT COUNT(*)
    FROM board_like bl
    WHERE bl.post_id = bp.post_id
      AND bl.active_yn = 'Y'
  ),
  bp.review_count = (
    SELECT COUNT(*)
    FROM board_review br
    WHERE br.post_id = bp.post_id
      AND br.status = 'PUBLISHED'
  ),
  bp.view_count = (
    SELECT COUNT(*)
    FROM board_view_log bvl
    WHERE bvl.post_id = bp.post_id
  )
WHERE bp.title LIKE '[CDV]%';

-- Two work approval requests per team; all requesters are active members.
INSERT INTO team_work_approval_request
(team_id, requester_user_id, board_post_id, work_id, title, content, media_type, work_type, visibility, status, reject_reason, decided_by, decided_at, created_at)
SELECT
  t.team_id,
  requester.user_id,
  CASE WHEN request_no.n = 1 THEN bp.post_id ELSE NULL END,
  CASE WHEN request_no.n = 1 THEN wi.work_id ELSE NULL END,
  CONCAT('[CDV] T', LPAD(seq.n, 2, '0'), '-W', request_no.n, ' 작업물 승인 요청'),
  CONCAT('팀 ', LPAD(seq.n, 2, '0'), '의 작업물 승인 상태 ', request_no.n, '번 시나리오입니다.'),
  'MANUAL',
  CASE MOD(seq.n - 1, 7)
    WHEN 0 THEN 'SHORT_FILM'
    WHEN 1 THEN 'FEATURE_FILM'
    WHEN 2 THEN 'MUSIC_VIDEO'
    WHEN 3 THEN 'ADVERTISEMENT'
    WHEN 4 THEN 'DOCUMENTARY'
    WHEN 5 THEN 'WEB_CONTENT'
    ELSE 'OTHER'
  END,
  'PUBLIC',
  CASE
    WHEN request_no.n = 1 THEN 'APPROVED'
    WHEN MOD(seq.n - 1, 3) = 0 THEN 'PENDING'
    WHEN MOD(seq.n - 1, 3) = 1 THEN 'REJECTED'
    ELSE 'CANCELED'
  END,
  CASE WHEN request_no.n = 2 AND MOD(seq.n - 1, 3) = 1 THEN '공개 전 후반 보완이 필요합니다.' ELSE NULL END,
  CASE WHEN request_no.n = 1 OR (request_no.n = 2 AND MOD(seq.n - 1, 3) = 1) THEN t.leader_user_id ELSE NULL END,
  CASE WHEN request_no.n = 1 OR (request_no.n = 2 AND MOD(seq.n - 1, 3) = 1) THEN NOW() - INTERVAL seq.n DAY ELSE NULL END,
  NOW() - INTERVAL (15 - seq.n + request_no.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2),
  '$[*]' COLUMNS (n INT PATH '$')
) request_no
JOIN team t
  ON t.name LIKE CONCAT('[CDV] ', LPAD(seq.n, 2, '0'), ' %')
JOIN user_account requester
  ON requester.login_id = CONCAT('cdv-user-', LPAD(seq.n * 2 + request_no.n, 2, '0'))
LEFT JOIN board_post bp
  ON request_no.n = 1
 AND bp.title = CONCAT('[CDV] 작업물 ', LPAD(seq.n, 2, '0'), ' 제작 기록')
LEFT JOIN work_item wi
  ON request_no.n = 1
 AND wi.board_post_id = bp.post_id;

INSERT INTO team_work_approval_genre (request_id, genre_id, sort_order)
SELECT
  requests.request_id,
  CASE MOD(requests.n + offsets.k - 2, 12)
    WHEN 0 THEN @genre_drama
    WHEN 1 THEN @genre_romance
    WHEN 2 THEN @genre_comedy
    WHEN 3 THEN @genre_thriller
    WHEN 4 THEN @genre_mystery
    WHEN 5 THEN @genre_sf
    WHEN 6 THEN @genre_documentary
    WHEN 7 THEN @genre_animation
    WHEN 8 THEN @genre_art
    WHEN 9 THEN @genre_music
    WHEN 10 THEN @genre_youth
    ELSE @genre_history
  END,
  offsets.k - 1
FROM (
  SELECT twr.request_id, ROW_NUMBER() OVER (ORDER BY twr.request_id) AS n
  FROM team_work_approval_request twr
  WHERE twr.title LIKE '[CDV]%'
) requests
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,5),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets;

INSERT INTO portfolio_item
(profile_id, title, role_name, credit_name, description, source_type, external_source_name, external_reference_id, url, thumbnail_url, sort_order, status, created_at)
SELECT
  mp.profile_id,
  CONCAT('[CDV] 포트폴리오 ', LPAD(seq.n, 2, '0'), '-', item_no.n),
  r.name,
  mp.display_name,
  CONCAT('창작자 ', LPAD(seq.n, 2, '0'), '의 역할과 장르를 보여주는 수동 포트폴리오 항목입니다.'),
  'MANUAL',
  'SLATE_CDV',
  CONCAT('CDV-PORT-', LPAD(seq.n, 2, '0'), '-', item_no.n),
  NULL,
  NULL,
  item_no.n - 1,
  'ACTIVE',
  NOW() - INTERVAL MOD(seq.n * item_no.n, 40) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2),
  '$[*]' COLUMNS (n INT PATH '$')
) item_no
JOIN user_account ua
  ON ua.login_id = CONCAT('cdv-user-', LPAD(seq.n, 2, '0'))
JOIN member_profile mp ON mp.user_id = ua.user_id
JOIN profile_role pr ON pr.profile_id = mp.profile_id AND pr.sort_order = 0
JOIN `role` r ON r.role_id = pr.role_id;

-- Six company requests, four linked internal contests, and twenty external contests.
INSERT INTO contest_open_request
(requester_user_id, contest_type, title, summary, theme, prize_text, total_prize_amount, first_prize_amount, organizer, organizer_type, representative_image_url, submission_email, external_url, target_text, target_codes_json, region_codes_json, required_roles_text, related_genres_text, start_at, deadline_at, status, review_reason, reviewed_by, reviewed_at, approved_contest_id, created_at)
SELECT
  company_user.user_id,
  'INTERNAL',
  CONCAT('[CDV] 공모전 개설 요청 ', LPAD(seq.n, 2, '0')),
  CONCAT('CDV 회사가 제안한 제작 지원 공모전 요청 ', LPAD(seq.n, 2, '0'), '입니다.'),
  CASE MOD(seq.n - 1, 3) WHEN 0 THEN '지역 단편' WHEN 1 THEN '신진 창작자 웹콘텐츠' ELSE '음악과 퍼포먼스' END,
  CONCAT('총 ', seq.n * 500, '만원 / 1등 ', seq.n * 250, '만원'),
  seq.n * 5000000,
  seq.n * 2500000,
  company_user.nickname,
  'COMPANY',
  NULL,
  CONCAT('cdv-contest-', LPAD(seq.n, 2, '0'), '@slate.test'),
  NULL,
  CASE MOD(seq.n - 1, 3) WHEN 0 THEN '성인 및 대학생' WHEN 1 THEN '신진 창작자' ELSE '지역 기반 제작팀' END,
  CASE MOD(seq.n - 1, 3)
    WHEN 0 THEN JSON_ARRAY('ADULT', 'UNIVERSITY')
    WHEN 1 THEN JSON_ARRAY('YOUTH', 'ADULT')
    ELSE JSON_ARRAY('ANY')
  END,
  CASE MOD(seq.n - 1, 4)
    WHEN 0 THEN JSON_ARRAY('SEOUL', 'GYEONGGI')
    WHEN 1 THEN JSON_ARRAY('BUSAN')
    WHEN 2 THEN JSON_ARRAY('JEJU')
    ELSE JSON_ARRAY('NATIONWIDE')
  END,
  CASE MOD(seq.n - 1, 3) WHEN 0 THEN '프로듀서, 감독, 촬영감독' WHEN 1 THEN '작가, 편집, VFX' ELSE '음악감독, 배우, 마케팅' END,
  CASE MOD(seq.n - 1, 3) WHEN 0 THEN '드라마, 스릴러' WHEN 1 THEN 'SF, 청춘/학원' ELSE '음악/공연, 실험/예술' END,
  NOW() + INTERVAL seq.n DAY,
  NOW() + INTERVAL (30 + seq.n * 4) DAY,
  CASE WHEN seq.n <= 4 THEN 'APPROVED' WHEN seq.n = 5 THEN 'PENDING' ELSE 'REJECTED' END,
  CASE WHEN seq.n <= 4 THEN '[CDV] 개설 승인 완료' WHEN seq.n = 5 THEN NULL ELSE '[CDV] 모집 요건 보완이 필요합니다.' END,
  CASE WHEN seq.n = 5 THEN NULL ELSE @cdv_admin_user END,
  CASE WHEN seq.n = 5 THEN NULL ELSE NOW() - INTERVAL (7 - seq.n) DAY END,
  NULL,
  NOW() - INTERVAL (12 - seq.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN user_account company_user
  ON company_user.login_id = CONCAT('cdv-company-', LPAD(MOD(seq.n - 1, 4) + 1, 2, '0'));

INSERT INTO contest
(contest_type, title, summary, theme, prize_text, total_prize_amount, first_prize_amount, organizer, organizer_type, representative_image_url, submission_email, external_url, target_text, target_codes_json, region_codes_json, required_roles_text, related_genres_text, start_at, deadline_at, status, save_count, created_by, requester_company_user_id, source_request_id, created_at)
SELECT
  'INTERNAL',
  CONCAT('[CDV] 승인 공모전 ', LPAD(seq.n, 2, '0')),
  cor.summary,
  cor.theme,
  cor.prize_text,
  cor.total_prize_amount,
  cor.first_prize_amount,
  cor.organizer,
  cor.organizer_type,
  NULL,
  cor.submission_email,
  NULL,
  cor.target_text,
  cor.target_codes_json,
  cor.region_codes_json,
  cor.required_roles_text,
  cor.related_genres_text,
  cor.start_at,
  cor.deadline_at,
  'OPEN',
  0,
  @cdv_admin_user,
  cor.requester_user_id,
  cor.request_id,
  cor.reviewed_at
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4),
  '$[*]' COLUMNS (n INT PATH '$')
) seq
JOIN contest_open_request cor
  ON cor.title = CONCAT('[CDV] 공모전 개설 요청 ', LPAD(seq.n, 2, '0'));

UPDATE contest_open_request cor
JOIN contest c ON c.source_request_id = cor.request_id
SET cor.approved_contest_id = c.contest_id,
    cor.updated_at = NOW()
WHERE cor.title LIKE '[CDV]%'
  AND cor.status = 'APPROVED';

INSERT INTO contest
(contest_type, title, summary, theme, prize_text, total_prize_amount, first_prize_amount, organizer, organizer_type, representative_image_url, submission_email, external_url, target_text, target_codes_json, region_codes_json, required_roles_text, related_genres_text, start_at, deadline_at, status, save_count, created_by, requester_company_user_id, source_request_id, created_at)
SELECT
  'EXTERNAL',
  CONCAT('[CDV] 외부 공모전 ', LPAD(seq.n, 2, '0')),
  CONCAT('장르와 지역, 역할 조건을 다양화한 외부 공모전 ', LPAD(seq.n, 2, '0'), '입니다.'),
  CASE MOD(seq.n - 1, 5) WHEN 0 THEN '도시 이야기' WHEN 1 THEN '환경 기록' WHEN 2 THEN '청춘과 음악' WHEN 3 THEN '기술과 미래' ELSE '가족과 지역' END,
  CONCAT('총 ', 1000 + seq.n * 100, '만원'),
  (1000 + seq.n * 100) * 10000,
  (400 + seq.n * 50) * 10000,
  CONCAT('CDV 외부 주최 ', LPAD(seq.n, 2, '0')),
  CASE MOD(seq.n - 1, 4) WHEN 0 THEN 'PUBLIC' WHEN 1 THEN 'COMPANY' WHEN 2 THEN 'UNIVERSITY' ELSE 'ASSOCIATION' END,
  NULL,
  CONCAT('cdv-external-', LPAD(seq.n, 2, '0'), '@slate.test'),
  NULL,
  CASE MOD(seq.n - 1, 4) WHEN 0 THEN '전 국민' WHEN 1 THEN '대학생' WHEN 2 THEN '신진 창작자' ELSE '지역 제작팀' END,
  CASE MOD(seq.n - 1, 4)
    WHEN 0 THEN JSON_ARRAY('ANY')
    WHEN 1 THEN JSON_ARRAY('UNIVERSITY')
    WHEN 2 THEN JSON_ARRAY('YOUTH', 'ADULT')
    ELSE JSON_ARRAY('ADULT')
  END,
  CASE MOD(seq.n - 1, 5)
    WHEN 0 THEN JSON_ARRAY('SEOUL')
    WHEN 1 THEN JSON_ARRAY('GYEONGGI')
    WHEN 2 THEN JSON_ARRAY('BUSAN')
    WHEN 3 THEN JSON_ARRAY('JEJU')
    ELSE JSON_ARRAY('NATIONWIDE')
  END,
  CASE MOD(seq.n - 1, 4) WHEN 0 THEN '감독, 작가' WHEN 1 THEN '촬영감독, 동시녹음' WHEN 2 THEN '배우, 음악감독' ELSE '편집, VFX' END,
  CASE MOD(seq.n - 1, 5) WHEN 0 THEN '드라마' WHEN 1 THEN '다큐멘터리' WHEN 2 THEN '음악/공연' WHEN 3 THEN 'SF' ELSE '가족/아동' END,
  NOW() - INTERVAL MOD(seq.n, 5) DAY,
  CASE WHEN seq.n <= 17 THEN NOW() + INTERVAL (12 + seq.n * 2) DAY ELSE NOW() - INTERVAL (seq.n - 17) DAY END,
  CASE WHEN seq.n <= 17 THEN 'OPEN' ELSE 'ENDED' END,
  0,
  @cdv_admin_user,
  NULL,
  NULL,
  NOW() - INTERVAL (25 - seq.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20),
  '$[*]' COLUMNS (n INT PATH '$')
) seq;

INSERT INTO contest_save (contest_id, user_id, created_at)
SELECT
  contests.contest_id,
  saver.user_id,
  NOW() - INTERVAL MOD(contests.n + offsets.k, 20) DAY
FROM (
  SELECT c.contest_id, ROW_NUMBER() OVER (ORDER BY c.contest_id) AS n
  FROM contest c
  WHERE c.title LIKE '[CDV]%'
) contests
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account saver
  ON saver.login_id = CONCAT('cdv-user-', LPAD(MOD(contests.n + offsets.k * 5 - 1, 32) + 1, 2, '0'));

UPDATE contest c
SET c.save_count = (
  SELECT COUNT(*)
  FROM contest_save cs
  WHERE cs.contest_id = c.contest_id
)
WHERE c.title LIKE '[CDV]%';

INSERT INTO contest_submission_prepare
(contest_id, user_id, basis_type, basis_id, checklist_json, memo, click_count, created_at)
SELECT
  contests.contest_id,
  prep_user.user_id,
  CASE offsets.k WHEN 1 THEN 'TEAM' ELSE 'PROFILE' END,
  CASE offsets.k WHEN 1 THEN t.team_id ELSE mp.profile_id END,
  CASE offsets.k
    WHEN 1 THEN JSON_ARRAY('기획안 확인', '팀원 역할표 확인', '일정표 첨부')
    ELSE JSON_ARRAY('프로필 공개 확인', '포트폴리오 순서 확인')
  END,
  CONCAT('[CDV] 공모전 ', LPAD(contests.n, 2, '0'), ' 제출 준비 메모 ', offsets.k, '입니다.'),
  offsets.k + MOD(contests.n, 3),
  NOW() - INTERVAL MOD(contests.n + offsets.k, 12) DAY
FROM (
  SELECT c.contest_id, ROW_NUMBER() OVER (ORDER BY c.contest_id) AS n
  FROM contest c
  WHERE c.title LIKE '[CDV]%'
) contests
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account prep_user
  ON prep_user.login_id = CONCAT('cdv-user-', LPAD(MOD(contests.n + offsets.k * 7 - 1, 32) + 1, 2, '0'))
LEFT JOIN team t
  ON offsets.k = 1
 AND t.name LIKE CONCAT('[CDV] ', LPAD(MOD(contests.n - 1, 12) + 1, 2, '0'), ' %')
LEFT JOIN member_profile mp
  ON offsets.k = 2
 AND mp.user_id = prep_user.user_id;

INSERT INTO contest_fit_cache
(contest_id, basis_type, basis_id, fit_score, reason_json, status, calculated_at, expires_at)
SELECT
  contests.contest_id,
  CASE offsets.k WHEN 1 THEN 'TEAM' ELSE 'PROFILE' END,
  CASE offsets.k WHEN 1 THEN t.team_id ELSE mp.profile_id END,
  62.00 + MOD(contests.n * 7 + offsets.k * 5, 34),
  CASE offsets.k
    WHEN 1 THEN JSON_ARRAY('장르 구성 일치', '팀 역할 구성 확인', '지역 이동 범위 적합')
    ELSE JSON_ARRAY('주요 역할 일치', '활동 지역 적합', '합류 가능 시점 확인')
  END,
  'READY',
  NOW() - INTERVAL MOD(contests.n + offsets.k, 60) MINUTE,
  NOW() + INTERVAL 30 DAY
FROM (
  SELECT c.contest_id, ROW_NUMBER() OVER (ORDER BY c.contest_id) AS n
  FROM contest c
  WHERE c.title LIKE '[CDV]%'
) contests
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2),
  '$[*]' COLUMNS (k INT PATH '$')
) offsets
JOIN user_account profile_user
  ON profile_user.login_id = CONCAT('cdv-user-', LPAD(MOD(contests.n + offsets.k * 4 - 1, 32) + 1, 2, '0'))
LEFT JOIN team t
  ON offsets.k = 1
 AND t.name LIKE CONCAT('[CDV] ', LPAD(MOD(contests.n - 1, 12) + 1, 2, '0'), ' %')
LEFT JOIN member_profile mp
  ON offsets.k = 2
 AND mp.user_id = profile_user.user_id;

-- Moderation states: 4 accepted, 4 pending, 4 rejected.
INSERT INTO content_report
(reporter_user_id, target_type, target_id, reason_code, detail, status, moderation_action, resolution_note, reviewed_by, reviewed_at, created_at)
SELECT
  reporter.user_id,
  report_seed.target_type,
  CASE WHEN report_seed.target_type = 'BOARD_POST' THEN bp.post_id ELSE br.review_id END,
  CASE MOD(report_seed.n - 1, 5) WHEN 0 THEN 'SPAM' WHEN 1 THEN 'ABUSE' WHEN 2 THEN 'ILLEGAL' WHEN 3 THEN 'PRIVACY' ELSE 'OTHER' END,
  CONCAT('[CDV] 신고 시나리오 ', LPAD(report_seed.n, 2, '0'), ' 상세입니다.'),
  CASE WHEN report_seed.n <= 4 THEN 'ACCEPTED' WHEN report_seed.n <= 8 THEN 'PENDING' ELSE 'REJECTED' END,
  CASE
    WHEN report_seed.n IN (1,2) THEN 'BLIND_POST'
    WHEN report_seed.n IN (3,4) THEN 'BLIND_REVIEW'
    WHEN report_seed.n >= 9 THEN 'NONE'
    ELSE NULL
  END,
  CASE WHEN report_seed.n <= 4 THEN '[CDV] 운영 정책에 따라 숨김 처리했습니다.' WHEN report_seed.n >= 9 THEN '[CDV] 위반 근거가 부족해 반려했습니다.' ELSE NULL END,
  CASE WHEN report_seed.n BETWEEN 5 AND 8 THEN NULL ELSE @cdv_admin_user END,
  CASE WHEN report_seed.n BETWEEN 5 AND 8 THEN NULL ELSE NOW() - INTERVAL MOD(report_seed.n, 4) DAY END,
  NOW() - INTERVAL (13 - report_seed.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(
    JSON_OBJECT('n', 1, 'target_type', 'BOARD_POST', 'post_no', 57),
    JSON_OBJECT('n', 2, 'target_type', 'BOARD_POST', 'post_no', 58),
    JSON_OBJECT('n', 3, 'target_type', 'BOARD_REVIEW', 'post_no', 59),
    JSON_OBJECT('n', 4, 'target_type', 'BOARD_REVIEW', 'post_no', 60),
    JSON_OBJECT('n', 5, 'target_type', 'BOARD_POST', 'post_no', 49),
    JSON_OBJECT('n', 6, 'target_type', 'BOARD_POST', 'post_no', 50),
    JSON_OBJECT('n', 7, 'target_type', 'BOARD_POST', 'post_no', 51),
    JSON_OBJECT('n', 8, 'target_type', 'BOARD_POST', 'post_no', 52),
    JSON_OBJECT('n', 9, 'target_type', 'BOARD_REVIEW', 'post_no', 53),
    JSON_OBJECT('n', 10, 'target_type', 'BOARD_REVIEW', 'post_no', 54),
    JSON_OBJECT('n', 11, 'target_type', 'BOARD_REVIEW', 'post_no', 55),
    JSON_OBJECT('n', 12, 'target_type', 'BOARD_REVIEW', 'post_no', 56)
  ),
  '$[*]' COLUMNS (
    n INT PATH '$.n',
    target_type VARCHAR(30) PATH '$.target_type',
    post_no INT PATH '$.post_no'
  )
) report_seed
JOIN user_account reporter
  ON reporter.login_id = CONCAT('cdv-user-', LPAD(MOD(report_seed.n + 3, 32) + 1, 2, '0'))
JOIN (
  SELECT bp.post_id, ROW_NUMBER() OVER (ORDER BY bp.post_id) AS n
  FROM board_post bp
  WHERE bp.title LIKE '[CDV]%'
) post_seq ON post_seq.n = report_seed.post_no
JOIN board_post bp ON bp.post_id = post_seq.post_id
LEFT JOIN board_review br
  ON report_seed.target_type = 'BOARD_REVIEW'
 AND br.post_id = bp.post_id
 AND br.parent_review_id IS NULL
 AND br.content LIKE CONCAT('[CDV] P', LPAD(report_seed.post_no, 2, '0'), '-R1 %');

UPDATE board_post bp
JOIN (
  SELECT cr.target_id
  FROM content_report cr
  JOIN user_account reporter ON reporter.user_id = cr.reporter_user_id
  WHERE reporter.login_id LIKE 'cdv-%'
    AND cr.status = 'ACCEPTED'
    AND cr.target_type = 'BOARD_POST'
    AND cr.moderation_action = 'BLIND_POST'
) accepted ON accepted.target_id = bp.post_id
SET bp.status = 'BLINDED',
    bp.updated_at = NOW();

UPDATE board_review br
JOIN (
  SELECT cr.target_id
  FROM content_report cr
  JOIN user_account reporter ON reporter.user_id = cr.reporter_user_id
  WHERE reporter.login_id LIKE 'cdv-%'
    AND cr.status = 'ACCEPTED'
    AND cr.target_type = 'BOARD_REVIEW'
    AND cr.moderation_action = 'BLIND_REVIEW'
) accepted ON accepted.target_id = br.review_id
SET br.status = 'BLINDED',
    br.updated_at = NOW();

UPDATE board_post bp
SET bp.review_count = (
  SELECT COUNT(*)
  FROM board_review br
  WHERE br.post_id = bp.post_id
    AND br.status = 'PUBLISHED'
)
WHERE bp.title LIKE '[CDV]%';

INSERT INTO user_sanction
(user_id, sanction_type, status, reason, sanction_until, created_by, revoked_by, revoked_at, revoke_reason, created_at)
SELECT
  target_user.user_id,
  CASE WHEN sanction_seed.n = 4 THEN 'PERM_SUSPENDED' ELSE 'TEMP_SUSPENDED' END,
  CASE WHEN sanction_seed.n <= 2 THEN 'ACTIVE' WHEN sanction_seed.n = 3 THEN 'REVOKED' ELSE 'EXPIRED' END,
  CONCAT('[CDV] 승인 신고 ', sanction_seed.n, '에 연결된 사용자 제재입니다.'),
  CASE WHEN sanction_seed.n <= 2 THEN NOW() + INTERVAL (7 + sanction_seed.n) DAY WHEN sanction_seed.n = 4 THEN NOW() - INTERVAL 1 DAY ELSE NULL END,
  @cdv_admin_user,
  CASE WHEN sanction_seed.n = 3 THEN @cdv_admin_user ELSE NULL END,
  CASE WHEN sanction_seed.n = 3 THEN NOW() - INTERVAL 1 DAY ELSE NULL END,
  CASE WHEN sanction_seed.n = 3 THEN '[CDV] 재검토 후 제재를 해제했습니다.' ELSE NULL END,
  NOW() - INTERVAL (6 - sanction_seed.n) DAY
FROM JSON_TABLE(
  JSON_ARRAY(1,2,3,4),
  '$[*]' COLUMNS (n INT PATH '$')
) sanction_seed
JOIN content_report cr
  ON cr.detail = CONCAT('[CDV] 신고 시나리오 ', LPAD(sanction_seed.n, 2, '0'), ' 상세입니다.')
LEFT JOIN board_post bp
  ON cr.target_type = 'BOARD_POST'
 AND bp.post_id = cr.target_id
LEFT JOIN board_review br
  ON cr.target_type = 'BOARD_REVIEW'
 AND br.review_id = cr.target_id
JOIN user_account target_user
  ON target_user.user_id = COALESCE(bp.author_user_id, br.author_user_id);

UPDATE user_account ua
LEFT JOIN user_sanction active_sanction
  ON active_sanction.user_id = ua.user_id
 AND active_sanction.status = 'ACTIVE'
SET ua.account_status = CASE
      WHEN active_sanction.sanction_type = 'PERM_SUSPENDED' THEN 'PERM_SUSPENDED'
      WHEN active_sanction.sanction_type = 'TEMP_SUSPENDED' THEN 'TEMP_SUSPENDED'
      ELSE 'ACTIVE'
    END,
    ua.updated_at = NOW()
WHERE ua.login_id LIKE 'cdv-user-%';

-- Five notifications for each non-admin CDV account.
INSERT INTO notification
(recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, hidden_yn, created_at, read_at, expires_at)
SELECT
  recipients.user_id,
  CASE WHEN notification_no.n IN (1,2) THEN sender_user.user_id ELSE @cdv_admin_user END,
  CASE notification_no.n WHEN 1 THEN 'MATCHING' WHEN 2 THEN 'TEAM' WHEN 3 THEN 'ADMIN' ELSE 'SYSTEM' END,
  CONCAT('[CDV] ', LPAD(recipients.n, 2, '0'), '-', notification_no.n, ' ', CASE notification_no.n
    WHEN 1 THEN '추천 프로필이 도착했습니다.'
    WHEN 2 THEN '팀 활동이 업데이트되었습니다.'
    WHEN 3 THEN '공모전 마감 정보를 확인하세요.'
    WHEN 4 THEN '새 게시글 반응이 있습니다.'
    ELSE '주간 활동 요약이 준비되었습니다.'
  END),
  CONCAT('CDV 볼륨 검증용 알림 ', recipients.n, '-', notification_no.n, '입니다.'),
  CASE notification_no.n WHEN 1 THEN 'PROFILE' WHEN 2 THEN 'TEAM' WHEN 3 THEN 'CONTEST' WHEN 4 THEN 'BOARD_POST' ELSE NULL END,
  CASE notification_no.n
    WHEN 1 THEN target_profile.profile_id
    WHEN 2 THEN target_team.team_id
    WHEN 3 THEN target_contest.contest_id
    WHEN 4 THEN target_post.post_id
    ELSE NULL
  END,
  CASE WHEN MOD(recipients.n + notification_no.n, 3) = 0 THEN 'Y' ELSE 'N' END,
  'N',
  NOW() - INTERVAL MOD(recipients.n * notification_no.n, 72) HOUR,
  CASE WHEN MOD(recipients.n + notification_no.n, 3) = 0 THEN NOW() - INTERVAL MOD(recipients.n, 24) HOUR ELSE NULL END,
  NOW() + INTERVAL 30 DAY
FROM (
  SELECT
    ua.user_id,
    ua.login_id,
    ROW_NUMBER() OVER (ORDER BY ua.login_id) AS n
  FROM user_account ua
  WHERE ua.login_id LIKE 'cdv-user-%'
     OR ua.login_id LIKE 'cdv-company-%'
) recipients
CROSS JOIN JSON_TABLE(
  JSON_ARRAY(1,2,3,4,5),
  '$[*]' COLUMNS (n INT PATH '$')
) notification_no
JOIN user_account sender_user
  ON sender_user.login_id = CONCAT('cdv-user-', LPAD(MOD(recipients.n + 4 - 1, 32) + 1, 2, '0'))
JOIN user_account profile_user
  ON profile_user.login_id = CONCAT('cdv-user-', LPAD(MOD(recipients.n + 7 - 1, 32) + 1, 2, '0'))
JOIN member_profile target_profile ON target_profile.user_id = profile_user.user_id
JOIN team target_team
  ON target_team.name LIKE CONCAT('[CDV] ', LPAD(MOD(recipients.n - 1, 12) + 1, 2, '0'), ' %')
JOIN (
  SELECT c.contest_id, ROW_NUMBER() OVER (ORDER BY c.contest_id) AS n
  FROM contest c
  WHERE c.title LIKE '[CDV]%'
) target_contest
  ON target_contest.n = MOD(recipients.n - 1, 24) + 1
JOIN (
  SELECT bp.post_id, ROW_NUMBER() OVER (ORDER BY bp.post_id) AS n
  FROM board_post bp
  WHERE bp.title LIKE '[CDV]%'
) target_post
  ON target_post.n = MOD(recipients.n - 1, 60) + 1;

INSERT INTO audit_log
(actor_user_id, action_type, target_type, target_id, ip_hash, before_json, after_json, created_at)
SELECT
  t.leader_user_id,
  'CDV_TEAM_WORK_APPROVED',
  'TEAM_WORK_REQUEST',
  twr.request_id,
  'cdv-seed-ip-hash',
  JSON_OBJECT('namespace', 'CDV', 'status', 'PENDING'),
  JSON_OBJECT('namespace', 'CDV', 'status', 'APPROVED', 'workId', twr.work_id, 'boardPostId', twr.board_post_id),
  twr.decided_at
FROM team_work_approval_request twr
JOIN team t ON t.team_id = twr.team_id
WHERE twr.title LIKE '[CDV]%'
  AND twr.status = 'APPROVED'
UNION ALL
SELECT
  @cdv_admin_user,
  'CDV_CONTEST_REQUEST_APPROVED',
  'CONTEST',
  c.contest_id,
  'cdv-seed-ip-hash',
  JSON_OBJECT('namespace', 'CDV', 'status', 'PENDING'),
  JSON_OBJECT('namespace', 'CDV', 'status', 'APPROVED', 'requestId', c.source_request_id),
  c.created_at
FROM contest c
WHERE c.title LIKE '[CDV] 승인 공모전%';

INSERT INTO audit_log
(actor_user_id, action_type, target_type, target_id, ip_hash, before_json, after_json, created_at)
SELECT
  @cdv_admin_user,
  'CDV_USER_SANCTION_RECORDED',
  'USER_SANCTION',
  us.sanction_id,
  'cdv-seed-ip-hash',
  NULL,
  JSON_OBJECT('namespace', 'CDV', 'status', us.status, 'userId', us.user_id),
  us.created_at
FROM user_sanction us
JOIN user_account ua ON ua.user_id = us.user_id
WHERE ua.login_id LIKE 'cdv-%'
UNION ALL
SELECT
  @cdv_admin_user,
  'CDV_CONTENT_REPORT_REVIEWED',
  'CONTENT_REPORT',
  cr.report_id,
  'cdv-seed-ip-hash',
  NULL,
  JSON_OBJECT('namespace', 'CDV', 'status', cr.status, 'action', cr.moderation_action),
  COALESCE(cr.reviewed_at, cr.created_at)
FROM content_report cr
JOIN user_account reporter ON reporter.user_id = cr.reporter_user_id
WHERE reporter.login_id LIKE 'cdv-%'
  AND cr.status IN ('ACCEPTED', 'REJECTED');

INSERT INTO operation_log (log_level, event_code, message, context_json, created_at)
VALUES
('INFO', 'CDV_VOLUME_ACCOUNTS_READY', 'CDV 계정과 프로필 볼륨 데이터가 준비되었습니다.', JSON_OBJECT('namespace', 'CDV', 'accountCount', 37, 'profileCount', 32), NOW()),
('INFO', 'CDV_VOLUME_TEAMS_READY', 'CDV 팀과 모집 관계 데이터가 준비되었습니다.', JSON_OBJECT('namespace', 'CDV', 'teamCount', 12, 'slotCount', 60), NOW()),
('INFO', 'CDV_VOLUME_CONTENT_READY', 'CDV 게시판과 작업물 데이터가 준비되었습니다.', JSON_OBJECT('namespace', 'CDV', 'postCount', 60, 'workCount', 36), NOW()),
('INFO', 'CDV_VOLUME_CONTESTS_READY', 'CDV 공모전과 운영 데이터가 준비되었습니다.', JSON_OBJECT('namespace', 'CDV', 'contestCount', 24, 'notificationCount', 180), NOW());

COMMIT;
