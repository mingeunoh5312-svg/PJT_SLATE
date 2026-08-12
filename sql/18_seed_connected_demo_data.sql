SET NAMES utf8mb4;
USE slate;

START TRANSACTION;

SET @admin_user_id := (SELECT user_id FROM user_account WHERE login_id = 'admin' LIMIT 1);

SET @region_jongno := (SELECT region_id FROM region WHERE region_code = '1111000000');
SET @region_junggu := (SELECT region_id FROM region WHERE region_code = '1114000000');
SET @region_mapo := (SELECT region_id FROM region WHERE region_code = '1144000000');
SET @region_bundang := (SELECT region_id FROM region WHERE region_code = '4113500000');
SET @region_goyang := (SELECT region_id FROM region WHERE region_code = '4128100000');

SET @role_producer := (SELECT role_id FROM `role` WHERE name = '프로듀서' ORDER BY role_id LIMIT 1);
SET @role_line_producer := (SELECT role_id FROM `role` WHERE name = '라인프로듀서' ORDER BY role_id LIMIT 1);
SET @role_director := (SELECT role_id FROM `role` WHERE name = '감독' ORDER BY role_id LIMIT 1);
SET @role_writer := (SELECT role_id FROM `role` WHERE name = '시나리오 작가' ORDER BY role_id LIMIT 1);
SET @role_camera := (SELECT role_id FROM `role` WHERE name = '촬영감독' ORDER BY role_id LIMIT 1);
SET @role_sound := (SELECT role_id FROM `role` WHERE name = '동시녹음' ORDER BY role_id LIMIT 1);
SET @role_actor := (SELECT role_id FROM `role` WHERE name = '배우' ORDER BY role_id LIMIT 1);
SET @role_editor := (SELECT role_id FROM `role` WHERE name = '영상 편집' ORDER BY role_id LIMIT 1);
SET @role_color := (SELECT role_id FROM `role` WHERE name = '색보정' ORDER BY role_id LIMIT 1);

SET @genre_drama := (SELECT genre_id FROM genre WHERE name = '드라마' ORDER BY genre_id LIMIT 1);
SET @genre_thriller := (SELECT genre_id FROM genre WHERE name = '스릴러' ORDER BY genre_id LIMIT 1);
SET @genre_mystery := (SELECT genre_id FROM genre WHERE name = '미스터리' ORDER BY genre_id LIMIT 1);
SET @genre_documentary := (SELECT genre_id FROM genre WHERE name = '다큐멘터리' ORDER BY genre_id LIMIT 1);
SET @genre_youth := (SELECT genre_id FROM genre WHERE name = '청춘/학원' ORDER BY genre_id LIMIT 1);

SET @cdd_leader_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-leader' LIMIT 1);
SET @cdd_camera_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-camera' LIMIT 1);
SET @cdd_sound_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-sound' LIMIT 1);
SET @cdd_editor_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-editor' LIMIT 1);
SET @cdd_writer_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-writer' LIMIT 1);
SET @cdd_actor_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-actor' LIMIT 1);
SET @cdd_reporter_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-reporter' LIMIT 1);
SET @cdd_moderated_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-moderated' LIMIT 1);
SET @cdd_company_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-company' LIMIT 1);
SET @cdd_river_team := (SELECT team_id FROM team WHERE name = '[CDD] 한강 야간 단편팀' ORDER BY team_id LIMIT 1);
SET @cdd_closed_team := (SELECT team_id FROM team WHERE name = '[CDD] 완료된 포트폴리오팀' ORDER BY team_id LIMIT 1);
SET @cdd_work_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 한강 야간 리허설 컷' ORDER BY post_id LIMIT 1);
SET @cdd_info_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 야간 촬영 체크리스트 공유' ORDER BY post_id LIMIT 1);
SET @cdd_moderation_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 운영 정책 검토용 숨김 게시글' ORDER BY post_id LIMIT 1);
SET @cdd_work_item := (SELECT work_id FROM work_item WHERE board_post_id = @cdd_work_post ORDER BY work_id LIMIT 1);
SET @cdd_contest := (SELECT contest_id FROM contest WHERE title = '[CDD] 도시 단편 제작지원 공모' ORDER BY contest_id LIMIT 1);
SET @cdd_contest_request := (SELECT request_id FROM contest_open_request WHERE title = '[CDD] 도시 단편 제작지원 요청' ORDER BY request_id LIMIT 1);

DELETE FROM operation_log
WHERE event_code LIKE 'CDD_%';

DELETE FROM audit_log
WHERE action_type LIKE 'CDD_%'
   OR ip_hash = 'cdd-seed-ip-hash';

DELETE FROM notification
WHERE title LIKE '[CDD]%';

DELETE FROM user_sanction
WHERE user_id = @cdd_moderated_user
  AND reason = '[CDD] 신고 승인에 따른 임시 이용 제한 데모입니다.';

DELETE FROM content_report
WHERE detail = '[CDD] 운영 정책 검증을 위한 신고 상세입니다.'
   OR (target_type = 'BOARD_POST' AND target_id = @cdd_moderation_post);

DELETE FROM contest_submission_prepare
WHERE contest_id = @cdd_contest
   OR user_id IN (SELECT user_id FROM user_account WHERE login_id LIKE 'cdd-%');

DELETE FROM contest_fit_cache
WHERE contest_id = @cdd_contest
   OR (basis_type = 'TEAM' AND basis_id IN (@cdd_river_team, @cdd_closed_team))
   OR (basis_type = 'PROFILE' AND basis_id IN (
      SELECT profile_id
      FROM member_profile mp
      JOIN user_account ua ON ua.user_id = mp.user_id
      WHERE ua.login_id LIKE 'cdd-%'
   ));

DELETE FROM contest_save
WHERE contest_id = @cdd_contest
   OR user_id IN (SELECT user_id FROM user_account WHERE login_id LIKE 'cdd-%');

DELETE FROM contest_open_request
WHERE request_id = @cdd_contest_request
   OR (requester_user_id = @cdd_company_user AND title LIKE '[CDD]%');

DELETE FROM contest
WHERE contest_id = @cdd_contest
   OR title LIKE '[CDD]%';

DELETE FROM team_work_approval_genre
WHERE request_id IN (
  SELECT request_id
  FROM team_work_approval_request
  WHERE team_id = @cdd_river_team
    AND title LIKE '[CDD]%'
);

DELETE FROM team_work_approval_request
WHERE team_id = @cdd_river_team
  AND title LIKE '[CDD]%';

DELETE FROM work_genre
WHERE work_id = @cdd_work_item;

DELETE FROM work_item
WHERE work_id = @cdd_work_item
   OR title LIKE '[CDD]%';

DELETE FROM board_view_log
WHERE post_id IN (@cdd_work_post, @cdd_info_post, @cdd_moderation_post)
   OR ip_hash LIKE 'cdd-view-hash-%';

DELETE FROM board_like
WHERE post_id IN (@cdd_work_post, @cdd_info_post, @cdd_moderation_post);

DELETE br
FROM board_review br
JOIN board_post bp ON bp.post_id = br.post_id
WHERE bp.title LIKE '[CDD]%'
  AND br.parent_review_id IS NOT NULL;

DELETE br
FROM board_review br
JOIN board_post bp ON bp.post_id = br.post_id
WHERE bp.title LIKE '[CDD]%';

DELETE FROM board_post
WHERE post_id IN (@cdd_work_post, @cdd_info_post, @cdd_moderation_post)
   OR title LIKE '[CDD]%';

DELETE FROM portfolio_verification
WHERE portfolio_item_id IN (
  SELECT portfolio_item_id
  FROM portfolio_item
  WHERE external_source_name = 'SLATE_CDD'
);

DELETE FROM portfolio_item
WHERE external_source_name = 'SLATE_CDD'
   OR external_reference_id LIKE 'CDD-%';

DELETE FROM matching_action_log
WHERE action_type LIKE 'CDD_%';

DELETE FROM matching_bookmark
WHERE user_id IN (
  SELECT user_id FROM user_account WHERE login_id LIKE 'cdd-%'
)
   OR (target_type = 'TEAM' AND target_id IN (@cdd_river_team, @cdd_closed_team))
   OR (target_type = 'PROFILE' AND target_id IN (
      SELECT profile_id
      FROM member_profile mp
      JOIN user_account ua ON ua.user_id = mp.user_id
      WHERE ua.login_id LIKE 'cdd-%'
   ));

DELETE FROM team_application
WHERE team_id IN (@cdd_river_team, @cdd_closed_team);

DELETE FROM team_invitation
WHERE team_id IN (@cdd_river_team, @cdd_closed_team);

DELETE FROM team_closure_snapshot
WHERE team_id IN (@cdd_river_team, @cdd_closed_team)
  AND JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.namespace')) = 'CDD';

DELETE FROM team_plan_item
WHERE team_id IN (@cdd_river_team, @cdd_closed_team);

DELETE s
FROM team_recruitment_slot s
JOIN team_recruitment r ON r.recruitment_id = s.recruitment_id
WHERE r.team_id IN (@cdd_river_team, @cdd_closed_team);

DELETE FROM team_recruitment
WHERE team_id IN (@cdd_river_team, @cdd_closed_team);

DELETE FROM team_member
WHERE team_id IN (@cdd_river_team, @cdd_closed_team);

DELETE FROM team_genre
WHERE team_id IN (@cdd_river_team, @cdd_closed_team);

DELETE FROM team
WHERE team_id IN (@cdd_river_team, @cdd_closed_team)
   OR name LIKE '[CDD]%';

DELETE FROM user_follow
WHERE follower_user_id IN (SELECT user_id FROM user_account WHERE login_id LIKE 'cdd-%')
   OR following_user_id IN (SELECT user_id FROM user_account WHERE login_id LIKE 'cdd-%');

DELETE FROM profile_collaboration_condition
WHERE profile_id IN (
  SELECT profile_id
  FROM member_profile mp
  JOIN user_account ua ON ua.user_id = mp.user_id
  WHERE ua.login_id LIKE 'cdd-%'
);

DELETE FROM profile_genre
WHERE profile_id IN (
  SELECT profile_id
  FROM member_profile mp
  JOIN user_account ua ON ua.user_id = mp.user_id
  WHERE ua.login_id LIKE 'cdd-%'
);

DELETE FROM profile_role
WHERE profile_id IN (
  SELECT profile_id
  FROM member_profile mp
  JOIN user_account ua ON ua.user_id = mp.user_id
  WHERE ua.login_id LIKE 'cdd-%'
);

DELETE FROM member_profile
WHERE user_id IN (SELECT user_id FROM user_account WHERE login_id LIKE 'cdd-%');

DELETE FROM company_application_document
WHERE uploader_user_id = @cdd_company_user
   OR company_application_id IN (
      SELECT company_application_id
      FROM company_application
      WHERE user_id = @cdd_company_user
   );

DELETE FROM company_application
WHERE user_id = @cdd_company_user;

DELETE FROM user_account
WHERE login_id IN (
  'cdd-leader',
  'cdd-camera',
  'cdd-sound',
  'cdd-editor',
  'cdd-writer',
  'cdd-actor',
  'cdd-reporter',
  'cdd-moderated',
  'cdd-company'
);

INSERT INTO user_account (login_id, email, password_hash, nickname, phone, account_type, account_status, last_login_at, created_at)
VALUES
('cdd-leader', 'cdd-leader@slate.test', '{noop}slate1234', 'CDD 유현서', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 18 DAY),
('cdd-camera', 'cdd-camera@slate.test', '{noop}slate1234', 'CDD 민재촬영', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 17 DAY),
('cdd-sound', 'cdd-sound@slate.test', '{noop}slate1234', 'CDD 소리담', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 16 DAY),
('cdd-editor', 'cdd-editor@slate.test', '{noop}slate1234', 'CDD 편집윤', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 15 DAY),
('cdd-writer', 'cdd-writer@slate.test', '{noop}slate1234', 'CDD 작가린', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 8 HOUR, NOW() - INTERVAL 14 DAY),
('cdd-actor', 'cdd-actor@slate.test', '{noop}slate1234', 'CDD 배우준', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 13 DAY),
('cdd-reporter', 'cdd-reporter@slate.test', '{noop}slate1234', 'CDD 신고자', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 12 DAY),
('cdd-moderated', 'cdd-moderated@slate.test', '{noop}slate1234', 'CDD 제재대상', NULL, 'USER', 'TEMP_SUSPENDED', NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 11 DAY),
('cdd-company', 'cdd-company@slate.test', '{noop}slate1234', 'CDD 도시필름랩', NULL, 'COMPANY', 'ACTIVE', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 10 DAY)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  account_type = VALUES(account_type),
  account_status = VALUES(account_status),
  deactivated_at = NULL,
  updated_at = NOW();

SET @cdd_leader_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-leader');
SET @cdd_camera_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-camera');
SET @cdd_sound_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-sound');
SET @cdd_editor_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-editor');
SET @cdd_writer_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-writer');
SET @cdd_actor_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-actor');
SET @cdd_reporter_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-reporter');
SET @cdd_moderated_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-moderated');
SET @cdd_company_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-company');

INSERT INTO company_application
(user_id, company_name, business_registration_no, manager_name, manager_phone, company_intro, public_data_company_name, status, review_reason, reviewed_by, reviewed_at, created_at)
VALUES
(@cdd_company_user, 'CDD 도시필름랩', 'CDD-00-00000', 'CDD 담당자', '000-0000-0000', '도시 기반 단편 제작 지원과 신진 창작자 매칭을 운영하는 데모 회사입니다.', 'CDD 도시필름랩', 'APPROVED', '연관형 더미 데이터용 승인 회사입니다.', @admin_user_id, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 10 DAY)
ON DUPLICATE KEY UPDATE
  company_name = VALUES(company_name),
  business_registration_no = VALUES(business_registration_no),
  manager_name = VALUES(manager_name),
  manager_phone = VALUES(manager_phone),
  company_intro = VALUES(company_intro),
  public_data_company_name = VALUES(public_data_company_name),
  status = 'APPROVED',
  review_reason = VALUES(review_reason),
  reviewed_by = VALUES(reviewed_by),
  reviewed_at = VALUES(reviewed_at),
  updated_at = NOW();

INSERT INTO member_profile
(user_id, display_name, short_intro, detail_intro, visibility, activity_status, region_id, experience_level, join_availability, collaboration_status, travel_range, preferred_duration, equipment_status, age_band, participation_mode, profile_completed_yn, status, last_active_at, created_at)
VALUES
(@cdd_leader_user, 'CDD 현서 PD', '야간 로케이션과 소규모 단편 제작 진행을 맡습니다.', '예산, 일정, 팀 커뮤니케이션을 정리해 촬영 전후 흐름을 안정적으로 만드는 프로듀서입니다.', 'PUBLIC', 'VISIBLE', @region_mapo, 'Y3_10', 'WITHIN_1W', 'AVAILABLE', 'KM_30', 'WITHIN_3M', 'NOT_ENTERED', 'THIRTIES', 'HYBRID', 'Y', 'ACTIVE', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 18 DAY),
(@cdd_camera_user, 'CDD 민재 촬영감독', '도시 야간 촬영과 자연광 테스트에 강합니다.', '한강과 골목 로케이션에서 핸드헬드 동선과 저조도 촬영을 안정적으로 구성합니다.', 'PUBLIC', 'VISIBLE', @region_junggu, 'Y3_10', 'IMMEDIATE', 'AVAILABLE', 'KM_30', 'WITHIN_3M', 'HAS_EQUIPMENT', 'TWENTIES', 'OFFLINE', 'Y', 'ACTIVE', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 17 DAY),
(@cdd_sound_user, 'CDD 소리담 동시녹음', '현장 동시녹음과 후반 사운드 정리를 함께 봅니다.', '야외 소음이 많은 현장에서도 대사 명료도를 살리는 녹음 계획을 세웁니다.', 'PUBLIC', 'VISIBLE', @region_goyang, 'Y3_10', 'WITHIN_2W', 'AVAILABLE', 'KM_100', 'WITHIN_3M', 'HAS_EQUIPMENT', 'THIRTIES', 'HYBRID', 'Y', 'ACTIVE', NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 16 DAY),
(@cdd_editor_user, 'CDD 윤 편집감독', '감정선 중심 편집과 색감 정리에 익숙합니다.', '리허설 컷을 빠르게 조립해 팀이 촬영 방향을 조정할 수 있도록 돕습니다.', 'PUBLIC', 'VISIBLE', @region_bundang, 'Y3_10', 'WITHIN_1W', 'AVAILABLE', 'ANYWHERE', 'ANY', 'HAS_EQUIPMENT', 'THIRTIES', 'REMOTE', 'Y', 'ACTIVE', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 15 DAY),
(@cdd_writer_user, 'CDD 린 작가', '도시 미스터리와 관계 중심 단편을 씁니다.', '짧은 러닝타임 안에서 인물의 선택과 장소의 분위기를 함께 살리는 각본을 선호합니다.', 'PUBLIC', 'VISIBLE', @region_jongno, 'Y0_3', 'WITHIN_2W', 'AVAILABLE', 'KM_100', 'WITHIN_3M', 'NO_EQUIPMENT', 'TWENTIES', 'REMOTE', 'Y', 'ACTIVE', NOW() - INTERVAL 8 HOUR, NOW() - INTERVAL 14 DAY),
(@cdd_actor_user, 'CDD 준 배우', '생활 연기와 즉흥 리허설에 적극적입니다.', '소규모 단편에서 리딩과 동선 리허설을 함께 맞추는 방식에 익숙합니다.', 'PUBLIC', 'VISIBLE', @region_mapo, 'Y0_3', 'WITHIN_1W', 'AVAILABLE', 'KM_30', 'WITHIN_1M', 'NOT_ENTERED', 'TWENTIES', 'OFFLINE', 'Y', 'ACTIVE', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 13 DAY),
(@cdd_reporter_user, 'CDD 리뷰어', '작업물 리뷰와 신고 흐름 검증용 공개 프로필입니다.', '게시판 상호작용과 운영 신고 시나리오를 확인하기 위한 데모 사용자입니다.', 'PUBLIC', 'VISIBLE', @region_junggu, 'Y0_3', 'NEGOTIABLE', 'CONSIDERING', 'KM_30', 'WITHIN_3M', 'NOT_ENTERED', 'PRIVATE', 'REMOTE', 'Y', 'ACTIVE', NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 12 DAY),
(@cdd_moderated_user, 'CDD 제재 대상', '운영 제재 화면 검증용 프로필입니다.', '신고 처리와 사용자 제재 화면을 확인하기 위한 데모 사용자입니다.', 'PUBLIC', 'VISIBLE', @region_goyang, 'Y0_3', 'AFTER_1M', 'UNAVAILABLE', 'KM_30', 'WITHIN_1M', 'NOT_ENTERED', 'PRIVATE', 'REMOTE', 'Y', 'ACTIVE', NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 11 DAY)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  short_intro = VALUES(short_intro),
  detail_intro = VALUES(detail_intro),
  visibility = VALUES(visibility),
  activity_status = VALUES(activity_status),
  region_id = VALUES(region_id),
  experience_level = VALUES(experience_level),
  join_availability = VALUES(join_availability),
  collaboration_status = VALUES(collaboration_status),
  travel_range = VALUES(travel_range),
  preferred_duration = VALUES(preferred_duration),
  equipment_status = VALUES(equipment_status),
  age_band = VALUES(age_band),
  participation_mode = VALUES(participation_mode),
  profile_completed_yn = 'Y',
  status = 'ACTIVE',
  deleted_at = NULL,
  last_active_at = VALUES(last_active_at),
  updated_at = NOW();

SET @cdd_leader_profile := (SELECT profile_id FROM member_profile WHERE user_id = @cdd_leader_user);
SET @cdd_camera_profile := (SELECT profile_id FROM member_profile WHERE user_id = @cdd_camera_user);
SET @cdd_sound_profile := (SELECT profile_id FROM member_profile WHERE user_id = @cdd_sound_user);
SET @cdd_editor_profile := (SELECT profile_id FROM member_profile WHERE user_id = @cdd_editor_user);
SET @cdd_writer_profile := (SELECT profile_id FROM member_profile WHERE user_id = @cdd_writer_user);
SET @cdd_actor_profile := (SELECT profile_id FROM member_profile WHERE user_id = @cdd_actor_user);
SET @cdd_reporter_profile := (SELECT profile_id FROM member_profile WHERE user_id = @cdd_reporter_user);
SET @cdd_moderated_profile := (SELECT profile_id FROM member_profile WHERE user_id = @cdd_moderated_user);

DELETE FROM profile_role
WHERE profile_id IN (@cdd_leader_profile, @cdd_camera_profile, @cdd_sound_profile, @cdd_editor_profile, @cdd_writer_profile, @cdd_actor_profile, @cdd_reporter_profile, @cdd_moderated_profile);
INSERT INTO profile_role (profile_id, role_id, sort_order) VALUES
(@cdd_leader_profile, @role_producer, 0),
(@cdd_leader_profile, @role_line_producer, 1),
(@cdd_camera_profile, @role_camera, 0),
(@cdd_sound_profile, @role_sound, 0),
(@cdd_editor_profile, @role_editor, 0),
(@cdd_editor_profile, @role_color, 1),
(@cdd_writer_profile, @role_writer, 0),
(@cdd_actor_profile, @role_actor, 0),
(@cdd_reporter_profile, @role_line_producer, 0),
(@cdd_moderated_profile, @role_writer, 0);

DELETE FROM profile_genre
WHERE profile_id IN (@cdd_leader_profile, @cdd_camera_profile, @cdd_sound_profile, @cdd_editor_profile, @cdd_writer_profile, @cdd_actor_profile, @cdd_reporter_profile, @cdd_moderated_profile);
INSERT INTO profile_genre (profile_id, genre_id) VALUES
(@cdd_leader_profile, @genre_drama),
(@cdd_leader_profile, @genre_youth),
(@cdd_camera_profile, @genre_drama),
(@cdd_camera_profile, @genre_thriller),
(@cdd_sound_profile, @genre_thriller),
(@cdd_sound_profile, @genre_documentary),
(@cdd_editor_profile, @genre_drama),
(@cdd_editor_profile, @genre_mystery),
(@cdd_writer_profile, @genre_thriller),
(@cdd_writer_profile, @genre_mystery),
(@cdd_actor_profile, @genre_drama),
(@cdd_actor_profile, @genre_youth),
(@cdd_reporter_profile, @genre_documentary),
(@cdd_moderated_profile, @genre_mystery);

DELETE FROM profile_collaboration_condition
WHERE profile_id IN (@cdd_leader_profile, @cdd_camera_profile, @cdd_sound_profile, @cdd_editor_profile, @cdd_writer_profile, @cdd_actor_profile, @cdd_reporter_profile, @cdd_moderated_profile);
INSERT INTO profile_collaboration_condition (profile_id, condition_code) VALUES
(@cdd_leader_profile, 'NEGOTIABLE'),
(@cdd_leader_profile, 'PAID'),
(@cdd_camera_profile, 'NEGOTIABLE'),
(@cdd_camera_profile, 'PAID'),
(@cdd_sound_profile, 'NEGOTIABLE'),
(@cdd_sound_profile, 'PAID'),
(@cdd_editor_profile, 'REVENUE_SHARE'),
(@cdd_editor_profile, 'NEGOTIABLE'),
(@cdd_writer_profile, 'NEGOTIABLE'),
(@cdd_actor_profile, 'NEGOTIABLE'),
(@cdd_actor_profile, 'UNPAID'),
(@cdd_reporter_profile, 'ANY'),
(@cdd_moderated_profile, 'ANY');

INSERT IGNORE INTO user_follow (follower_user_id, following_user_id, created_at) VALUES
(@cdd_camera_user, @cdd_leader_user, NOW() - INTERVAL 8 DAY),
(@cdd_writer_user, @cdd_leader_user, NOW() - INTERVAL 7 DAY),
(@cdd_leader_user, @cdd_camera_user, NOW() - INTERVAL 6 DAY),
(@cdd_editor_user, @cdd_camera_user, NOW() - INTERVAL 5 DAY),
(@cdd_reporter_user, @cdd_editor_user, NOW() - INTERVAL 4 DAY);

INSERT INTO team (leader_user_id, name, description, status, end_type, region_id, region_any_yn, expected_duration, max_member_count, current_member_count, last_active_at, created_at)
SELECT @cdd_leader_user, '[CDD] 한강 야간 단편팀', '한강 야간 산책로를 배경으로 관계 드라마 단편을 제작하는 연관형 데모 팀입니다.', 'RECRUITING', NULL, @region_mapo, 'N', 'WITHIN_3M', 6, 4, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 14 DAY
WHERE NOT EXISTS (SELECT 1 FROM team WHERE name = '[CDD] 한강 야간 단편팀');
INSERT INTO team (leader_user_id, name, description, status, end_type, region_id, region_any_yn, expected_duration, max_member_count, current_member_count, last_active_at, created_at)
SELECT @cdd_editor_user, '[CDD] 완료된 포트폴리오팀', '원격 후반 협업으로 짧은 포트폴리오 컷을 완성하고 정상 종료한 데모 팀입니다.', 'ENDED', 'NORMAL', NULL, 'Y', 'WITHIN_1M', 5, 3, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 30 DAY
WHERE NOT EXISTS (SELECT 1 FROM team WHERE name = '[CDD] 완료된 포트폴리오팀');

UPDATE team
SET leader_user_id = @cdd_leader_user,
    description = '한강 야간 산책로를 배경으로 관계 드라마 단편을 제작하는 연관형 데모 팀입니다.',
    status = 'RECRUITING',
    end_type = NULL,
    region_id = @region_mapo,
    region_any_yn = 'N',
    expected_duration = 'WITHIN_3M',
    max_member_count = 6,
    last_active_at = NOW() - INTERVAL 2 HOUR,
    updated_at = NOW()
WHERE name = '[CDD] 한강 야간 단편팀';

UPDATE team
SET leader_user_id = @cdd_editor_user,
    description = '원격 후반 협업으로 짧은 포트폴리오 컷을 완성하고 정상 종료한 데모 팀입니다.',
    status = 'ENDED',
    end_type = 'NORMAL',
    region_id = NULL,
    region_any_yn = 'Y',
    expected_duration = 'WITHIN_1M',
    max_member_count = 5,
    last_active_at = NOW() - INTERVAL 5 DAY,
    updated_at = NOW() - INTERVAL 5 DAY
WHERE name = '[CDD] 완료된 포트폴리오팀';

SET @cdd_river_team := (SELECT team_id FROM team WHERE name = '[CDD] 한강 야간 단편팀' ORDER BY team_id LIMIT 1);
SET @cdd_closed_team := (SELECT team_id FROM team WHERE name = '[CDD] 완료된 포트폴리오팀' ORDER BY team_id LIMIT 1);

DELETE FROM team_genre WHERE team_id IN (@cdd_river_team, @cdd_closed_team);
INSERT INTO team_genre (team_id, genre_id) VALUES
(@cdd_river_team, @genre_drama),
(@cdd_river_team, @genre_thriller),
(@cdd_river_team, @genre_youth),
(@cdd_closed_team, @genre_drama),
(@cdd_closed_team, @genre_documentary);

INSERT INTO team_member (team_id, user_id, team_role, status, joined_at, left_at) VALUES
(@cdd_river_team, @cdd_leader_user, 'LEADER', 'ACTIVE', NOW() - INTERVAL 14 DAY, NULL),
(@cdd_river_team, @cdd_actor_user, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 11 DAY, NULL),
(@cdd_river_team, @cdd_camera_user, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 9 DAY, NULL),
(@cdd_river_team, @cdd_editor_user, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 8 DAY, NULL),
(@cdd_closed_team, @cdd_editor_user, 'LEADER', 'ACTIVE', NOW() - INTERVAL 30 DAY, NULL),
(@cdd_closed_team, @cdd_sound_user, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 28 DAY, NULL),
(@cdd_closed_team, @cdd_writer_user, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 27 DAY, NULL)
ON DUPLICATE KEY UPDATE
  team_role = VALUES(team_role),
  status = 'ACTIVE',
  left_at = NULL;

UPDATE team t
SET current_member_count = (
  SELECT COUNT(*)
  FROM team_member tm
  WHERE tm.team_id = t.team_id
    AND tm.status = 'ACTIVE'
)
WHERE t.team_id IN (@cdd_river_team, @cdd_closed_team);

INSERT INTO team_recruitment (team_id, title, status, deadline_at, work_start_at, created_by, created_at)
SELECT @cdd_river_team, '[CDD] 한강 야간 단편 촬영/후반 모집', 'OPEN', NOW() + INTERVAL 24 DAY, NOW() + INTERVAL 14 DAY, @cdd_leader_user, NOW() - INTERVAL 12 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment WHERE team_id = @cdd_river_team AND title = '[CDD] 한강 야간 단편 촬영/후반 모집');
INSERT INTO team_recruitment (team_id, title, status, deadline_at, work_start_at, created_by, created_at)
SELECT @cdd_closed_team, '[CDD] 완료팀 후반 기록 모집', 'CLOSED', NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 25 DAY, @cdd_editor_user, NOW() - INTERVAL 29 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment WHERE team_id = @cdd_closed_team AND title = '[CDD] 완료팀 후반 기록 모집');

UPDATE team_recruitment
SET status = 'OPEN',
    deadline_at = NOW() + INTERVAL 24 DAY,
    work_start_at = NOW() + INTERVAL 14 DAY,
    created_by = @cdd_leader_user,
    updated_at = NOW()
WHERE team_id = @cdd_river_team
  AND title = '[CDD] 한강 야간 단편 촬영/후반 모집';
UPDATE team_recruitment
SET status = 'CLOSED',
    deadline_at = NOW() - INTERVAL 18 DAY,
    work_start_at = NOW() - INTERVAL 25 DAY,
    created_by = @cdd_editor_user,
    updated_at = NOW() - INTERVAL 5 DAY
WHERE team_id = @cdd_closed_team
  AND title = '[CDD] 완료팀 후반 기록 모집';

SET @cdd_river_recruitment := (SELECT recruitment_id FROM team_recruitment WHERE team_id = @cdd_river_team AND title = '[CDD] 한강 야간 단편 촬영/후반 모집' ORDER BY recruitment_id LIMIT 1);
SET @cdd_closed_recruitment := (SELECT recruitment_id FROM team_recruitment WHERE team_id = @cdd_closed_team AND title = '[CDD] 완료팀 후반 기록 모집' ORDER BY recruitment_id LIMIT 1);

INSERT INTO team_recruitment_slot (recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status)
SELECT @cdd_river_recruitment, @role_camera, 1, 1, 'Y3_10', 'PAID', 'Y', 'WITHIN_3M', 'Y', 'CLOSED'
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment_slot WHERE recruitment_id = @cdd_river_recruitment AND role_id = @role_camera);
INSERT INTO team_recruitment_slot (recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status)
SELECT @cdd_river_recruitment, @role_sound, 1, 0, 'Y3_10', 'NEGOTIABLE', 'Y', 'WITHIN_3M', 'Y', 'OPEN'
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment_slot WHERE recruitment_id = @cdd_river_recruitment AND role_id = @role_sound);
INSERT INTO team_recruitment_slot (recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status)
SELECT @cdd_river_recruitment, @role_editor, 1, 1, 'Y3_10', 'REVENUE_SHARE', 'N', 'WITHIN_3M', 'Y', 'CLOSED'
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment_slot WHERE recruitment_id = @cdd_river_recruitment AND role_id = @role_editor);
INSERT INTO team_recruitment_slot (recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status)
SELECT @cdd_river_recruitment, @role_writer, 1, 0, 'Y0_3', 'NEGOTIABLE', 'N', 'WITHIN_3M', 'N', 'OPEN'
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment_slot WHERE recruitment_id = @cdd_river_recruitment AND role_id = @role_writer);
INSERT INTO team_recruitment_slot (recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status)
SELECT @cdd_closed_recruitment, @role_editor, 1, 0, 'Y3_10', 'REVENUE_SHARE', 'Y', 'WITHIN_1M', 'Y', 'CLOSED'
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment_slot WHERE recruitment_id = @cdd_closed_recruitment AND role_id = @role_editor);
INSERT INTO team_recruitment_slot (recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status)
SELECT @cdd_closed_recruitment, @role_writer, 1, 0, 'Y0_3', 'NEGOTIABLE', 'N', 'WITHIN_1M', 'N', 'CLOSED'
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment_slot WHERE recruitment_id = @cdd_closed_recruitment AND role_id = @role_writer);

UPDATE team_recruitment_slot SET required_count = 1, accepted_count = 1, status = 'CLOSED' WHERE recruitment_id = @cdd_river_recruitment AND role_id IN (@role_camera, @role_editor);
UPDATE team_recruitment_slot SET required_count = 1, accepted_count = 0, status = 'OPEN' WHERE recruitment_id = @cdd_river_recruitment AND role_id IN (@role_sound, @role_writer);
UPDATE team_recruitment_slot SET required_count = 1, accepted_count = 0, status = 'CLOSED' WHERE recruitment_id = @cdd_closed_recruitment AND role_id IN (@role_editor, @role_writer);

SET @slot_river_camera := (SELECT slot_id FROM team_recruitment_slot WHERE recruitment_id = @cdd_river_recruitment AND role_id = @role_camera ORDER BY slot_id LIMIT 1);
SET @slot_river_sound := (SELECT slot_id FROM team_recruitment_slot WHERE recruitment_id = @cdd_river_recruitment AND role_id = @role_sound ORDER BY slot_id LIMIT 1);
SET @slot_river_editor := (SELECT slot_id FROM team_recruitment_slot WHERE recruitment_id = @cdd_river_recruitment AND role_id = @role_editor ORDER BY slot_id LIMIT 1);
SET @slot_river_writer := (SELECT slot_id FROM team_recruitment_slot WHERE recruitment_id = @cdd_river_recruitment AND role_id = @role_writer ORDER BY slot_id LIMIT 1);
SET @slot_closed_editor := (SELECT slot_id FROM team_recruitment_slot WHERE recruitment_id = @cdd_closed_recruitment AND role_id = @role_editor ORDER BY slot_id LIMIT 1);
SET @slot_closed_writer := (SELECT slot_id FROM team_recruitment_slot WHERE recruitment_id = @cdd_closed_recruitment AND role_id = @role_writer ORDER BY slot_id LIMIT 1);

INSERT INTO team_application (team_id, recruitment_id, slot_id, applicant_user_id, message, status, reject_reason, decided_by, decided_at, created_at)
SELECT @cdd_river_team, @cdd_river_recruitment, @slot_river_camera, @cdd_camera_user, '[CDD] 야간 촬영 경험과 장비를 가지고 합류하고 싶습니다.', 'ACCEPTED', NULL, @cdd_leader_user, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 10 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_application WHERE team_id = @cdd_river_team AND slot_id = @slot_river_camera AND applicant_user_id = @cdd_camera_user AND status = 'ACCEPTED');
INSERT INTO team_application (team_id, recruitment_id, slot_id, applicant_user_id, message, status, created_at)
SELECT @cdd_river_team, @cdd_river_recruitment, @slot_river_writer, @cdd_writer_user, '[CDD] 장소의 분위기를 살리는 짧은 각색안을 제안하고 싶습니다.', 'PENDING', NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_application WHERE team_id = @cdd_river_team AND slot_id = @slot_river_writer AND applicant_user_id = @cdd_writer_user AND status = 'PENDING');
INSERT INTO team_application (team_id, recruitment_id, slot_id, applicant_user_id, message, status, reject_reason, decided_by, decided_at, created_at)
SELECT @cdd_river_team, @cdd_river_recruitment, @slot_river_writer, @cdd_reporter_user, '[CDD] 이번에는 제작 보조로 지원합니다.', 'REJECTED', '역할 요구 조건과 맞지 않아 이번 모집에서는 제외했습니다.', @cdd_leader_user, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_application WHERE team_id = @cdd_river_team AND slot_id = @slot_river_writer AND applicant_user_id = @cdd_reporter_user AND status = 'REJECTED');
INSERT INTO team_application (team_id, recruitment_id, slot_id, applicant_user_id, message, status, reject_reason, decided_by, decided_at, created_at, updated_at)
SELECT @cdd_closed_team, @cdd_closed_recruitment, @slot_closed_editor, @cdd_reporter_user, '[CDD] 일정 조율 전 지원했다가 직접 취소한 요청입니다.', 'CANCELED', NULL, NULL, NULL, NOW() - INTERVAL 24 DAY, NOW() - INTERVAL 23 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_application WHERE team_id = @cdd_closed_team AND slot_id = @slot_closed_editor AND applicant_user_id = @cdd_reporter_user AND status = 'CANCELED');
INSERT INTO team_application (team_id, recruitment_id, slot_id, applicant_user_id, message, status, reject_reason, decided_by, decided_at, created_at, updated_at)
SELECT @cdd_closed_team, @cdd_closed_recruitment, @slot_closed_writer, @cdd_actor_user, '[CDD] 모집 마감 전 확인하지 못해 만료된 지원입니다.', 'EXPIRED', NULL, NULL, NULL, NOW() - INTERVAL 26 DAY, NOW() - INTERVAL 18 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_application WHERE team_id = @cdd_closed_team AND slot_id = @slot_closed_writer AND applicant_user_id = @cdd_actor_user AND status = 'EXPIRED');

INSERT INTO team_invitation (team_id, recruitment_id, slot_id, target_user_id, inviter_user_id, message, status, decided_at, created_at)
SELECT @cdd_river_team, @cdd_river_recruitment, @slot_river_editor, @cdd_editor_user, @cdd_leader_user, '[CDD] 리허설 컷 편집 담당으로 함께해 주세요.', 'ACCEPTED', NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 9 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_invitation WHERE team_id = @cdd_river_team AND slot_id = @slot_river_editor AND target_user_id = @cdd_editor_user AND status = 'ACCEPTED');
INSERT INTO team_invitation (team_id, recruitment_id, slot_id, target_user_id, inviter_user_id, message, status, created_at)
SELECT @cdd_river_team, @cdd_river_recruitment, @slot_river_sound, @cdd_sound_user, @cdd_leader_user, '[CDD] 야외 동시녹음 담당으로 합류 제안을 드립니다.', 'PENDING', NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_invitation WHERE team_id = @cdd_river_team AND slot_id = @slot_river_sound AND target_user_id = @cdd_sound_user AND status = 'PENDING');
INSERT INTO team_invitation (team_id, recruitment_id, slot_id, target_user_id, inviter_user_id, message, status, decided_at, created_at, updated_at)
SELECT @cdd_river_team, @cdd_river_recruitment, @slot_river_sound, @cdd_reporter_user, @cdd_leader_user, '[CDD] 일정이 맞지 않아 팀장이 취소한 초대입니다.', 'CANCELED', NULL, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_invitation WHERE team_id = @cdd_river_team AND slot_id = @slot_river_sound AND target_user_id = @cdd_reporter_user AND status = 'CANCELED');
INSERT INTO team_invitation (team_id, recruitment_id, slot_id, target_user_id, inviter_user_id, message, status, decided_at, created_at, updated_at)
SELECT @cdd_closed_team, @cdd_closed_recruitment, @slot_closed_writer, @cdd_writer_user, @cdd_editor_user, '[CDD] 종료된 팀의 과거 초대가 응답 없이 만료되었습니다.', 'EXPIRED', NULL, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 18 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_invitation WHERE team_id = @cdd_closed_team AND slot_id = @slot_closed_writer AND target_user_id = @cdd_writer_user AND status = 'EXPIRED');

INSERT INTO team_plan_item (team_id, title, description, assignee_user_id, role_id, due_at, status, created_by, created_at)
SELECT @cdd_river_team, '[CDD] 촬영 콘티 1차 확정', '주요 동선과 컷 전환을 촬영 전 확정합니다.', @cdd_leader_user, @role_producer, NOW() - INTERVAL 2 DAY, 'DONE', @cdd_leader_user, NOW() - INTERVAL 11 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_plan_item WHERE team_id = @cdd_river_team AND title = '[CDD] 촬영 콘티 1차 확정');
INSERT INTO team_plan_item (team_id, title, description, assignee_user_id, role_id, due_at, status, created_by, created_at)
SELECT @cdd_river_team, '[CDD] 야간 로케이션 리허설', '카메라 동선과 조도, 주변 소음을 같은 시간대에 확인합니다.', @cdd_camera_user, @role_camera, NOW() + INTERVAL 2 DAY, 'IN_PROGRESS', @cdd_leader_user, NOW() - INTERVAL 10 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_plan_item WHERE team_id = @cdd_river_team AND title = '[CDD] 야간 로케이션 리허설');
INSERT INTO team_plan_item (team_id, title, description, assignee_user_id, role_id, due_at, status, created_by, created_at)
SELECT @cdd_river_team, '[CDD] 사운드 체크리스트 공유', '야외 녹음 리스크와 예비 장비 목록을 공유합니다.', @cdd_sound_user, @role_sound, NOW() + INTERVAL 7 DAY, 'TODO', @cdd_leader_user, NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_plan_item WHERE team_id = @cdd_river_team AND title = '[CDD] 사운드 체크리스트 공유');
INSERT INTO team_plan_item (team_id, title, description, assignee_user_id, role_id, due_at, status, created_by, created_at)
SELECT @cdd_river_team, '[CDD] 비 예보 대응 플랜', '우천 시 실내 대체 컷과 촬영 순서를 검토합니다.', NULL, NULL, NOW() + INTERVAL 12 DAY, 'HOLD', @cdd_leader_user, NOW() - INTERVAL 7 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_plan_item WHERE team_id = @cdd_river_team AND title = '[CDD] 비 예보 대응 플랜');
INSERT INTO team_plan_item (team_id, title, description, assignee_user_id, role_id, due_at, status, created_by, created_at)
SELECT @cdd_closed_team, '[CDD] 후반 믹스 마감', '완료팀의 마지막 후반 믹스 작업을 종료했습니다.', @cdd_sound_user, @role_sound, NOW() - INTERVAL 12 DAY, 'DONE', @cdd_editor_user, NOW() - INTERVAL 25 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_plan_item WHERE team_id = @cdd_closed_team AND title = '[CDD] 후반 믹스 마감');
INSERT INTO team_plan_item (team_id, title, description, assignee_user_id, role_id, due_at, status, created_by, created_at)
SELECT @cdd_closed_team, '[CDD] 추가 촬영 검토', '완성본 기준으로 추가 촬영은 진행하지 않기로 했습니다.', NULL, NULL, NOW() - INTERVAL 10 DAY, 'CANCELED', @cdd_editor_user, NOW() - INTERVAL 24 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_plan_item WHERE team_id = @cdd_closed_team AND title = '[CDD] 추가 촬영 검토');

INSERT INTO team_closure_snapshot (team_id, end_type, snapshot_json, created_by, created_at)
SELECT
  @cdd_closed_team,
  'NORMAL',
  JSON_OBJECT(
    'namespace', 'CDD',
    'teamName', '[CDD] 완료된 포트폴리오팀',
    'status', 'ENDED',
    'endType', 'NORMAL',
    'members', JSON_ARRAY(
      JSON_OBJECT('loginId', 'cdd-editor', 'teamRole', 'LEADER', 'status', 'ACTIVE'),
      JSON_OBJECT('loginId', 'cdd-sound', 'teamRole', 'MEMBER', 'status', 'ACTIVE'),
      JSON_OBJECT('loginId', 'cdd-writer', 'teamRole', 'MEMBER', 'status', 'ACTIVE')
    ),
    'recruitments', JSON_ARRAY(
      JSON_OBJECT('title', '[CDD] 완료팀 후반 기록 모집', 'status', 'CLOSED')
    ),
    'plans', JSON_ARRAY(
      JSON_OBJECT('title', '[CDD] 후반 믹스 마감', 'status', 'DONE'),
      JSON_OBJECT('title', '[CDD] 추가 촬영 검토', 'status', 'CANCELED')
    )
  ),
  @cdd_editor_user,
  NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_closure_snapshot WHERE team_id = @cdd_closed_team AND end_type = 'NORMAL');

INSERT IGNORE INTO matching_bookmark (user_id, target_type, target_id, created_at) VALUES
(@cdd_leader_user, 'PROFILE', @cdd_camera_profile, NOW() - INTERVAL 8 DAY),
(@cdd_leader_user, 'PROFILE', @cdd_sound_profile, NOW() - INTERVAL 7 DAY),
(@cdd_camera_user, 'TEAM', @cdd_river_team, NOW() - INTERVAL 6 DAY),
(@cdd_writer_user, 'TEAM', @cdd_river_team, NOW() - INTERVAL 5 DAY),
(@cdd_sound_user, 'TEAM', @cdd_closed_team, NOW() - INTERVAL 20 DAY);

INSERT INTO matching_action_log (actor_user_id, action_type, target_type, target_id, team_id, role_id, created_at)
SELECT @cdd_leader_user, 'CDD_PROFILE_BOOKMARK', 'PROFILE', @cdd_camera_profile, @cdd_river_team, @role_camera, NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (SELECT 1 FROM matching_action_log WHERE action_type = 'CDD_PROFILE_BOOKMARK' AND actor_user_id = @cdd_leader_user AND target_id = @cdd_camera_profile);
INSERT INTO matching_action_log (actor_user_id, action_type, target_type, target_id, team_id, role_id, created_at)
SELECT @cdd_camera_user, 'CDD_TEAM_BOOKMARK', 'TEAM', @cdd_river_team, @cdd_river_team, @role_camera, NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM matching_action_log WHERE action_type = 'CDD_TEAM_BOOKMARK' AND actor_user_id = @cdd_camera_user AND target_id = @cdd_river_team);
INSERT INTO matching_action_log (actor_user_id, action_type, target_type, target_id, team_id, role_id, created_at)
SELECT @cdd_writer_user, 'CDD_TEAM_APPLICATION_SENT', 'APPLICATION', @slot_river_writer, @cdd_river_team, @role_writer, NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM matching_action_log WHERE action_type = 'CDD_TEAM_APPLICATION_SENT' AND actor_user_id = @cdd_writer_user AND team_id = @cdd_river_team);

INSERT INTO board_post (author_user_id, category, free_category, title, content, status, visibility, like_count, review_count, view_count, created_at)
SELECT @cdd_editor_user, 'WORK', NULL, '[CDD] 한강 야간 리허설 컷', '한강 야간 로케이션 리허설에서 촬영 동선과 감정선을 확인한 데모 작업물입니다.', 'PUBLISHED', 'PUBLIC', 0, 0, 42, NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM board_post WHERE title = '[CDD] 한강 야간 리허설 컷');
INSERT INTO board_post (author_user_id, category, free_category, title, content, status, visibility, like_count, review_count, view_count, created_at)
SELECT @cdd_leader_user, 'FREE', 'INFO', '[CDD] 야간 촬영 체크리스트 공유', '야간 촬영 전 소음, 조도, 통행 동선, 예비 배터리를 함께 확인하는 체크리스트입니다.', 'PUBLISHED', 'PUBLIC', 0, 0, 18, NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM board_post WHERE title = '[CDD] 야간 촬영 체크리스트 공유');
INSERT INTO board_post (author_user_id, category, free_category, title, content, status, visibility, like_count, review_count, view_count, created_at)
SELECT @cdd_moderated_user, 'FREE', 'FREE', '[CDD] 운영 정책 검토용 숨김 게시글', '신고와 운영 숨김 처리, 제재 알림 연결을 확인하기 위한 데모 게시글입니다.', 'BLINDED', 'PUBLIC', 0, 0, 3, NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM board_post WHERE title = '[CDD] 운영 정책 검토용 숨김 게시글');

SET @cdd_work_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 한강 야간 리허설 컷' ORDER BY post_id LIMIT 1);
SET @cdd_info_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 야간 촬영 체크리스트 공유' ORDER BY post_id LIMIT 1);
SET @cdd_moderation_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 운영 정책 검토용 숨김 게시글' ORDER BY post_id LIMIT 1);

INSERT INTO work_item (owner_user_id, team_id, board_post_id, title, description, media_type, work_type, visibility, status, created_at)
SELECT @cdd_editor_user, @cdd_river_team, @cdd_work_post, '[CDD] 한강 야간 리허설 컷', '팀 승인 흐름을 거쳐 게시판과 포트폴리오에 연결되는 수동 등록 작업물입니다.', 'MANUAL', 'SHORT_FILM', 'PUBLIC', 'PUBLISHED', NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM work_item WHERE board_post_id = @cdd_work_post);

UPDATE work_item
SET owner_user_id = @cdd_editor_user,
    team_id = @cdd_river_team,
    title = '[CDD] 한강 야간 리허설 컷',
    description = '팀 승인 흐름을 거쳐 게시판과 포트폴리오에 연결되는 수동 등록 작업물입니다.',
    media_type = 'MANUAL',
    work_type = 'SHORT_FILM',
    visibility = 'PUBLIC',
    status = 'PUBLISHED',
    updated_at = NOW()
WHERE board_post_id = @cdd_work_post;

SET @cdd_work_item := (SELECT work_id FROM work_item WHERE board_post_id = @cdd_work_post ORDER BY work_id LIMIT 1);

DELETE FROM work_genre WHERE work_id = @cdd_work_item;
INSERT INTO work_genre (work_id, genre_id, sort_order) VALUES
(@cdd_work_item, @genre_drama, 0),
(@cdd_work_item, @genre_thriller, 1);

INSERT INTO board_review (post_id, author_user_id, parent_review_id, content, status, created_at)
SELECT @cdd_work_post, @cdd_camera_user, NULL, '[CDD] 촬영 동선이 명확해서 본 촬영 때 바로 참고할 수 있습니다.', 'PUBLISHED', NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM board_review WHERE post_id = @cdd_work_post AND author_user_id = @cdd_camera_user AND content = '[CDD] 촬영 동선이 명확해서 본 촬영 때 바로 참고할 수 있습니다.');
SET @cdd_work_review := (SELECT review_id FROM board_review WHERE post_id = @cdd_work_post AND author_user_id = @cdd_camera_user AND content = '[CDD] 촬영 동선이 명확해서 본 촬영 때 바로 참고할 수 있습니다.' ORDER BY review_id LIMIT 1);
INSERT INTO board_review (post_id, author_user_id, parent_review_id, content, status, created_at)
SELECT @cdd_work_post, @cdd_leader_user, @cdd_work_review, '[CDD] 다음 리허설에서는 사운드 체크도 함께 붙이겠습니다.', 'PUBLISHED', NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM board_review WHERE post_id = @cdd_work_post AND author_user_id = @cdd_leader_user AND content = '[CDD] 다음 리허설에서는 사운드 체크도 함께 붙이겠습니다.');
INSERT INTO board_review (post_id, author_user_id, parent_review_id, content, status, created_at)
SELECT @cdd_info_post, @cdd_sound_user, NULL, '[CDD] 야외 녹음 항목도 체크리스트에 추가하면 좋겠습니다.', 'PUBLISHED', NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM board_review WHERE post_id = @cdd_info_post AND author_user_id = @cdd_sound_user AND content = '[CDD] 야외 녹음 항목도 체크리스트에 추가하면 좋겠습니다.');

INSERT INTO board_like (post_id, user_id, active_yn, created_at) VALUES
(@cdd_work_post, @cdd_leader_user, 'Y', NOW() - INTERVAL 4 DAY),
(@cdd_work_post, @cdd_camera_user, 'Y', NOW() - INTERVAL 4 DAY),
(@cdd_work_post, @cdd_sound_user, 'Y', NOW() - INTERVAL 3 DAY),
(@cdd_work_post, @cdd_writer_user, 'Y', NOW() - INTERVAL 3 DAY),
(@cdd_info_post, @cdd_editor_user, 'Y', NOW() - INTERVAL 3 DAY),
(@cdd_info_post, @cdd_actor_user, 'Y', NOW() - INTERVAL 2 DAY)
ON DUPLICATE KEY UPDATE
  active_yn = 'Y',
  updated_at = NOW();

INSERT INTO board_view_log (post_id, viewer_user_id, ip_hash, view_window_start, created_at)
SELECT @cdd_work_post, @cdd_leader_user, 'cdd-view-hash-leader', DATE_FORMAT(NOW() - INTERVAL 4 DAY, '%Y-%m-%d %H:00:00'), NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM board_view_log WHERE post_id = @cdd_work_post AND viewer_user_id = @cdd_leader_user AND ip_hash = 'cdd-view-hash-leader');
INSERT INTO board_view_log (post_id, viewer_user_id, ip_hash, view_window_start, created_at)
SELECT @cdd_work_post, @cdd_camera_user, 'cdd-view-hash-camera', DATE_FORMAT(NOW() - INTERVAL 3 DAY, '%Y-%m-%d %H:00:00'), NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM board_view_log WHERE post_id = @cdd_work_post AND viewer_user_id = @cdd_camera_user AND ip_hash = 'cdd-view-hash-camera');

UPDATE board_post p
SET p.like_count = (
  SELECT COUNT(*) FROM board_like bl WHERE bl.post_id = p.post_id AND bl.active_yn = 'Y'
),
p.review_count = (
  SELECT COUNT(*) FROM board_review br WHERE br.post_id = p.post_id AND br.status = 'PUBLISHED'
)
WHERE p.post_id IN (@cdd_work_post, @cdd_info_post, @cdd_moderation_post);

INSERT INTO team_work_approval_request (team_id, requester_user_id, board_post_id, work_id, title, content, media_type, work_type, visibility, status, decided_by, decided_at, created_at)
SELECT @cdd_river_team, @cdd_editor_user, @cdd_work_post, @cdd_work_item, '[CDD] 한강 야간 컷 공개 승인', '팀 작업물을 게시판 작업물로 공개하는 승인 완료 흐름입니다.', 'MANUAL', 'SHORT_FILM', 'PUBLIC', 'APPROVED', @cdd_leader_user, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_work_approval_request WHERE team_id = @cdd_river_team AND title = '[CDD] 한강 야간 컷 공개 승인');
INSERT INTO team_work_approval_request (team_id, requester_user_id, title, content, media_type, work_type, visibility, status, created_at)
SELECT @cdd_river_team, @cdd_camera_user, '[CDD] 한강 현장음 믹스 승인 대기', '촬영감독이 정리한 현장음 테스트 파일 공개 여부를 팀장이 검토 중인 상태입니다.', 'MANUAL', 'OTHER', 'PUBLIC', 'PENDING', NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_work_approval_request WHERE team_id = @cdd_river_team AND title = '[CDD] 한강 현장음 믹스 승인 대기');
INSERT INTO team_work_approval_request (team_id, requester_user_id, title, content, media_type, work_type, visibility, status, reject_reason, decided_by, decided_at, created_at)
SELECT @cdd_river_team, @cdd_editor_user, '[CDD] 미완성 러프컷 반려', '편집 테스트용 러프컷이라 공개가 반려된 상태입니다.', 'MANUAL', 'SHORT_FILM', 'PUBLIC', 'REJECTED', '완성도가 낮아 팀 포트폴리오 공개 전 보완이 필요합니다.', @cdd_leader_user, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_work_approval_request WHERE team_id = @cdd_river_team AND title = '[CDD] 미완성 러프컷 반려');
INSERT INTO team_work_approval_request (team_id, requester_user_id, title, content, media_type, work_type, visibility, status, decided_at, created_at, updated_at)
SELECT @cdd_river_team, @cdd_actor_user, '[CDD] 배우 리허설 요청 취소', '업로드 전 요청자가 직접 취소한 팀 작업물 승인 요청입니다.', 'MANUAL', 'SHORT_FILM', 'PUBLIC', 'CANCELED', NULL, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_work_approval_request WHERE team_id = @cdd_river_team AND title = '[CDD] 배우 리허설 요청 취소');

SET @cdd_approval_approved := (SELECT request_id FROM team_work_approval_request WHERE team_id = @cdd_river_team AND title = '[CDD] 한강 야간 컷 공개 승인' ORDER BY request_id LIMIT 1);
SET @cdd_approval_pending := (SELECT request_id FROM team_work_approval_request WHERE team_id = @cdd_river_team AND title = '[CDD] 한강 현장음 믹스 승인 대기' ORDER BY request_id LIMIT 1);
SET @cdd_approval_rejected := (SELECT request_id FROM team_work_approval_request WHERE team_id = @cdd_river_team AND title = '[CDD] 미완성 러프컷 반려' ORDER BY request_id LIMIT 1);

UPDATE team_work_approval_request
SET board_post_id = @cdd_work_post,
    work_id = @cdd_work_item,
    status = 'APPROVED',
    decided_by = @cdd_leader_user,
    decided_at = NOW() - INTERVAL 4 DAY,
    updated_at = NOW()
WHERE request_id = @cdd_approval_approved;

DELETE FROM team_work_approval_genre WHERE request_id IN (@cdd_approval_approved, @cdd_approval_pending, @cdd_approval_rejected);
INSERT INTO team_work_approval_genre (request_id, genre_id, sort_order) VALUES
(@cdd_approval_approved, @genre_drama, 0),
(@cdd_approval_approved, @genre_thriller, 1),
(@cdd_approval_pending, @genre_documentary, 0),
(@cdd_approval_rejected, @genre_drama, 0);

INSERT INTO portfolio_item (profile_id, title, role_name, credit_name, description, source_type, external_source_name, external_reference_id, url, thumbnail_url, sort_order, status, created_at)
SELECT @cdd_editor_profile, '[CDD] 한강 야간 리허설 컷', '영상 편집', 'CDD 윤 편집감독', '팀 승인 작업물과 같은 작품을 포트폴리오에 연결한 데모 항목입니다.', 'MANUAL', 'SLATE_CDD', 'CDD-RIVER-WORK-001', NULL, NULL, 0, 'ACTIVE', NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (
  SELECT 1 FROM portfolio_item
  WHERE profile_id = @cdd_editor_profile
    AND external_source_name = 'SLATE_CDD'
    AND external_reference_id = 'CDD-RIVER-WORK-001'
);
INSERT INTO portfolio_item (profile_id, title, role_name, credit_name, description, source_type, external_source_name, external_reference_id, url, thumbnail_url, sort_order, status, created_at)
SELECT @cdd_camera_profile, '[CDD] 한강 저조도 카메라 테스트', '촬영감독', 'CDD 민재 촬영감독', '지원 수락 전 촬영감독 역량을 보여주는 데모 포트폴리오 항목입니다.', 'MANUAL', 'SLATE_CDD', 'CDD-CAMERA-TEST-001', NULL, NULL, 1, 'ACTIVE', NOW() - INTERVAL 9 DAY
WHERE NOT EXISTS (
  SELECT 1 FROM portfolio_item
  WHERE profile_id = @cdd_camera_profile
    AND external_source_name = 'SLATE_CDD'
    AND external_reference_id = 'CDD-CAMERA-TEST-001'
);

INSERT INTO contest_open_request
(requester_user_id, contest_type, title, summary, theme, prize_text, total_prize_amount, first_prize_amount, organizer, organizer_type, representative_image_url, submission_email, external_url, target_text, target_codes_json, region_codes_json, required_roles_text, related_genres_text, start_at, deadline_at, status, review_reason, reviewed_by, reviewed_at, created_at)
SELECT @cdd_company_user, 'INTERNAL', '[CDD] 도시 단편 제작지원 요청', '도시 야간 단편 제작팀을 찾는 회사 공모전 개설 요청입니다.', '도시 야간 단편', '총 500만원 / 대상 300만원', 5000000, 3000000, 'CDD 도시필름랩', 'COMPANY', NULL, 'cdd-contest@slate.test', NULL, '성인 및 대학생 영상 제작팀', JSON_ARRAY('ADULT', 'UNIVERSITY'), JSON_ARRAY('SEOUL', 'GYEONGGI'), '프로듀서, 촬영감독, 편집', '드라마, 스릴러, 청춘/학원', NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 45 DAY, 'APPROVED', '데모 회사 승인 공모전 요청입니다.', @admin_user_id, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM contest_open_request WHERE requester_user_id = @cdd_company_user AND title = '[CDD] 도시 단편 제작지원 요청');

SET @cdd_contest_request := (SELECT request_id FROM contest_open_request WHERE requester_user_id = @cdd_company_user AND title = '[CDD] 도시 단편 제작지원 요청' ORDER BY request_id LIMIT 1);

INSERT INTO contest
(contest_type, title, summary, theme, prize_text, total_prize_amount, first_prize_amount, organizer, organizer_type, representative_image_url, submission_email, external_url, target_text, target_codes_json, region_codes_json, required_roles_text, related_genres_text, start_at, deadline_at, status, save_count, created_by, requester_company_user_id, source_request_id, created_at)
SELECT 'INTERNAL', '[CDD] 도시 단편 제작지원 공모', '도시 야간 단편 제작팀과 창작자를 연결하는 승인 완료 데모 공모전입니다.', '도시 야간 단편', '총 500만원 / 대상 300만원', 5000000, 3000000, 'CDD 도시필름랩', 'COMPANY', NULL, 'cdd-contest@slate.test', NULL, '성인 및 대학생 영상 제작팀', JSON_ARRAY('ADULT', 'UNIVERSITY'), JSON_ARRAY('SEOUL', 'GYEONGGI'), '프로듀서, 촬영감독, 편집', '드라마, 스릴러, 청춘/학원', NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 45 DAY, 'OPEN', 0, @admin_user_id, @cdd_company_user, @cdd_contest_request, NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM contest WHERE title = '[CDD] 도시 단편 제작지원 공모');

SET @cdd_contest := (SELECT contest_id FROM contest WHERE title = '[CDD] 도시 단편 제작지원 공모' ORDER BY contest_id LIMIT 1);

UPDATE contest_open_request
SET status = 'APPROVED',
    review_reason = '데모 회사 승인 공모전 요청입니다.',
    reviewed_by = @admin_user_id,
    reviewed_at = NOW() - INTERVAL 2 DAY,
    approved_contest_id = @cdd_contest,
    updated_at = NOW()
WHERE request_id = @cdd_contest_request;

UPDATE contest
SET requester_company_user_id = @cdd_company_user,
    source_request_id = @cdd_contest_request,
    created_by = @admin_user_id,
    status = 'OPEN',
    deadline_at = NOW() + INTERVAL 45 DAY,
    updated_at = NOW()
WHERE contest_id = @cdd_contest;

INSERT IGNORE INTO contest_save (contest_id, user_id, created_at) VALUES
(@cdd_contest, @cdd_leader_user, NOW() - INTERVAL 2 DAY),
(@cdd_contest, @cdd_camera_user, NOW() - INTERVAL 1 DAY),
(@cdd_contest, @cdd_editor_user, NOW() - INTERVAL 12 HOUR);

UPDATE contest c
SET c.save_count = (
  SELECT COUNT(*) FROM contest_save cs WHERE cs.contest_id = c.contest_id
)
WHERE c.contest_id = @cdd_contest;

INSERT INTO contest_submission_prepare (contest_id, user_id, basis_type, basis_id, checklist_json, memo, click_count, created_at)
VALUES
(@cdd_contest, @cdd_leader_user, 'TEAM', @cdd_river_team, JSON_ARRAY('기획안 1p 정리', '촬영 일정표 첨부', '팀원 역할표 확인'), '[CDD] 한강 야간 단편팀 기준으로 제출 준비 중입니다.', 2, NOW() - INTERVAL 1 DAY),
(@cdd_contest, @cdd_camera_user, 'PROFILE', @cdd_camera_profile, JSON_ARRAY('촬영 포트폴리오 링크 확인', '장비 목록 정리'), '[CDD] 개인 촬영감독 기준 제출 준비 메모입니다.', 1, NOW() - INTERVAL 12 HOUR)
ON DUPLICATE KEY UPDATE
  checklist_json = VALUES(checklist_json),
  memo = VALUES(memo),
  click_count = VALUES(click_count),
  updated_at = NOW();

INSERT INTO contest_fit_cache (contest_id, basis_type, basis_id, fit_score, reason_json, status, calculated_at, expires_at)
VALUES
(@cdd_contest, 'TEAM', @cdd_river_team, 88.50, JSON_ARRAY('서울/경기권 도시 야간 단편', '드라마·스릴러 장르 일치', '프로듀서·촬영·편집 구성 보유'), 'READY', NOW() - INTERVAL 20 MINUTE, NOW() + INTERVAL 30 DAY),
(@cdd_contest, 'PROFILE', @cdd_camera_profile, 82.00, JSON_ARRAY('촬영감독 역할 일치', '저조도 로케이션 경험', '서울권 즉시 합류 가능'), 'READY', NOW() - INTERVAL 15 MINUTE, NOW() + INTERVAL 30 DAY)
ON DUPLICATE KEY UPDATE
  fit_score = VALUES(fit_score),
  reason_json = VALUES(reason_json),
  status = 'READY',
  calculated_at = VALUES(calculated_at),
  expires_at = VALUES(expires_at);

INSERT INTO content_report (reporter_user_id, target_type, target_id, reason_code, detail, status, moderation_action, resolution_note, reviewed_by, reviewed_at, created_at)
SELECT @cdd_reporter_user, 'BOARD_POST', @cdd_moderation_post, 'SPAM', '[CDD] 운영 정책 검증을 위한 신고 상세입니다.', 'ACCEPTED', 'BLIND_POST', '데모 신고를 승인하고 게시글을 숨김 처리했습니다.', @admin_user_id, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (
  SELECT 1 FROM content_report
  WHERE reporter_user_id = @cdd_reporter_user
    AND target_type = 'BOARD_POST'
    AND target_id = @cdd_moderation_post
    AND detail = '[CDD] 운영 정책 검증을 위한 신고 상세입니다.'
);

SET @cdd_report := (SELECT report_id FROM content_report WHERE reporter_user_id = @cdd_reporter_user AND target_type = 'BOARD_POST' AND target_id = @cdd_moderation_post AND detail = '[CDD] 운영 정책 검증을 위한 신고 상세입니다.' ORDER BY report_id LIMIT 1);

UPDATE board_post
SET status = 'BLINDED',
    updated_at = NOW() - INTERVAL 1 DAY
WHERE post_id = @cdd_moderation_post;

INSERT INTO user_sanction (user_id, sanction_type, status, reason, sanction_until, created_by, created_at)
SELECT @cdd_moderated_user, 'TEMP_SUSPENDED', 'ACTIVE', '[CDD] 신고 승인에 따른 임시 이용 제한 데모입니다.', NOW() + INTERVAL 10 DAY, @admin_user_id, NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (
  SELECT 1 FROM user_sanction
  WHERE user_id = @cdd_moderated_user
    AND reason = '[CDD] 신고 승인에 따른 임시 이용 제한 데모입니다.'
);

SET @cdd_sanction := (SELECT sanction_id FROM user_sanction WHERE user_id = @cdd_moderated_user AND reason = '[CDD] 신고 승인에 따른 임시 이용 제한 데모입니다.' ORDER BY sanction_id LIMIT 1);

UPDATE user_sanction
SET sanction_type = 'TEMP_SUSPENDED',
    status = 'ACTIVE',
    sanction_until = NOW() + INTERVAL 10 DAY,
    created_by = @admin_user_id,
    revoked_by = NULL,
    revoked_at = NULL,
    revoke_reason = NULL,
    updated_at = NOW()
WHERE sanction_id = @cdd_sanction;

UPDATE user_account
SET account_status = 'TEMP_SUSPENDED',
    updated_at = NOW()
WHERE user_id = @cdd_moderated_user;

INSERT INTO notification (recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, hidden_yn, created_at, expires_at)
SELECT @cdd_leader_user, @cdd_camera_user, 'SOCIAL', '[CDD] 새 팔로워가 생겼습니다.', 'CDD 민재 촬영감독이 CDD 현서 PD를 팔로우했습니다.', 'PROFILE', @cdd_leader_profile, 'N', 'N', NOW() - INTERVAL 8 DAY, NOW() + INTERVAL 22 DAY
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '[CDD] 새 팔로워가 생겼습니다.' AND recipient_user_id = @cdd_leader_user AND sender_user_id = @cdd_camera_user);
INSERT INTO notification (recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, hidden_yn, created_at, expires_at)
SELECT @cdd_leader_user, @cdd_writer_user, 'TEAM', '[CDD] 새 팀 지원이 도착했습니다.', 'CDD 린 작가가 한강 야간 단편팀 시나리오 작가 슬롯에 지원했습니다.', 'TEAM', @cdd_river_team, 'N', 'N', NOW() - INTERVAL 2 DAY, NOW() + INTERVAL 28 DAY
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '[CDD] 새 팀 지원이 도착했습니다.' AND recipient_user_id = @cdd_leader_user AND target_id = @cdd_river_team);
INSERT INTO notification (recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, hidden_yn, created_at, expires_at)
SELECT @cdd_camera_user, @cdd_leader_user, 'TEAM', '[CDD] 팀 지원이 수락되었습니다.', '한강 야간 단편팀 촬영감독 지원이 수락되었습니다.', 'TEAM', @cdd_river_team, 'Y', 'N', NOW() - INTERVAL 9 DAY, NOW() + INTERVAL 21 DAY
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '[CDD] 팀 지원이 수락되었습니다.' AND recipient_user_id = @cdd_camera_user AND target_id = @cdd_river_team);
INSERT INTO notification (recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, hidden_yn, created_at, expires_at)
SELECT @cdd_sound_user, @cdd_leader_user, 'TEAM', '[CDD] 팀 초대가 도착했습니다.', '한강 야간 단편팀 동시녹음 담당 초대가 도착했습니다.', 'TEAM', @cdd_river_team, 'N', 'N', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 29 DAY
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '[CDD] 팀 초대가 도착했습니다.' AND recipient_user_id = @cdd_sound_user AND target_id = @cdd_river_team);
INSERT INTO notification (recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, hidden_yn, created_at, expires_at)
SELECT @cdd_company_user, @admin_user_id, 'ADMIN', '[CDD] 공모전 요청이 승인되었습니다.', '도시 단편 제작지원 요청이 승인되어 공모전이 생성되었습니다.', 'CONTEST', @cdd_contest, 'N', 'N', NOW() - INTERVAL 2 DAY, NOW() + INTERVAL 28 DAY
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '[CDD] 공모전 요청이 승인되었습니다.' AND recipient_user_id = @cdd_company_user AND target_id = @cdd_contest);
INSERT INTO notification (recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, hidden_yn, created_at, expires_at)
SELECT @cdd_moderated_user, @admin_user_id, 'ADMIN', '[CDD] 계정 이용이 제한되었습니다.', '신고 승인에 따라 임시 이용 제한이 적용되었습니다.', 'USER_SANCTION', @cdd_sanction, 'N', 'N', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 29 DAY
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '[CDD] 계정 이용이 제한되었습니다.' AND recipient_user_id = @cdd_moderated_user AND target_id = @cdd_sanction);
INSERT INTO notification (recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, hidden_yn, created_at, expires_at)
SELECT @cdd_sound_user, @cdd_editor_user, 'TEAM', '[CDD] 팀 작업이 정상 종료되었습니다.', '완료된 포트폴리오팀이 정상 종료되어 closure snapshot이 생성되었습니다.', 'TEAM', @cdd_closed_team, 'Y', 'N', NOW() - INTERVAL 5 DAY, NOW() + INTERVAL 25 DAY
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '[CDD] 팀 작업이 정상 종료되었습니다.' AND recipient_user_id = @cdd_sound_user AND target_id = @cdd_closed_team);

INSERT INTO audit_log (actor_user_id, action_type, target_type, target_id, ip_hash, before_json, after_json, created_at)
SELECT @cdd_leader_user, 'CDD_TEAM_WORK_REQUEST_APPROVED', 'TEAM_WORK_REQUEST', @cdd_approval_approved, 'cdd-seed-ip-hash', NULL, JSON_OBJECT('namespace', 'CDD', 'boardPostId', @cdd_work_post, 'workId', @cdd_work_item), NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE action_type = 'CDD_TEAM_WORK_REQUEST_APPROVED' AND target_id = @cdd_approval_approved);
INSERT INTO audit_log (actor_user_id, action_type, target_type, target_id, ip_hash, before_json, after_json, created_at)
SELECT @admin_user_id, 'CDD_CONTEST_REQUEST_APPROVED', 'CONTEST', @cdd_contest, 'cdd-seed-ip-hash', NULL, JSON_OBJECT('namespace', 'CDD', 'requestId', @cdd_contest_request), NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE action_type = 'CDD_CONTEST_REQUEST_APPROVED' AND target_id = @cdd_contest);
INSERT INTO audit_log (actor_user_id, action_type, target_type, target_id, ip_hash, before_json, after_json, created_at)
SELECT @admin_user_id, 'CDD_USER_SANCTION_CREATED', 'USER_SANCTION', @cdd_sanction, 'cdd-seed-ip-hash', NULL, JSON_OBJECT('namespace', 'CDD', 'reportId', @cdd_report), NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE action_type = 'CDD_USER_SANCTION_CREATED' AND target_id = @cdd_sanction);

INSERT INTO operation_log (log_level, event_code, message, context_json, created_at)
SELECT 'INFO', 'CDD_CONNECTED_DEMO_READY', '연관형 더미 데이터 시나리오 초안 seed가 적용되었습니다.', JSON_OBJECT('namespace', 'CDD', 'riverTeamId', @cdd_river_team, 'contestId', @cdd_contest), NOW()
WHERE NOT EXISTS (SELECT 1 FROM operation_log WHERE event_code = 'CDD_CONNECTED_DEMO_READY');

COMMIT;
