SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO user_account (login_id, email, password_hash, nickname, phone, account_type, account_status, last_login_at, created_at)
VALUES
('ai-camera-a', 'ai-camera-a@slate.test', '{noop}slate1234', 'AI 서하늘', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 16 DAY),
('ai-editor-a', 'ai-editor-a@slate.test', '{noop}slate1234', 'AI 민채원', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 15 DAY),
('ai-sound-a', 'ai-sound-a@slate.test', '{noop}slate1234', 'AI 강태민', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 14 DAY),
('ai-art-a', 'ai-art-a@slate.test', '{noop}slate1234', 'AI 오수아', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 13 DAY),
('ai-light-a', 'ai-light-a@slate.test', '{noop}slate1234', 'AI 문준혁', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 7 HOUR, NOW() - INTERVAL 12 DAY),
('ai-vfx-a', 'ai-vfx-a@slate.test', '{noop}slate1234', 'AI 정하린', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 9 HOUR, NOW() - INTERVAL 11 DAY),
('ai-leader-a', 'ai-leader-a@slate.test', '{noop}slate1234', 'AI 한도윤', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 10 DAY),
('ai-leader-b', 'ai-leader-b@slate.test', '{noop}slate1234', 'AI 윤서진', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 9 DAY)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  account_status = 'ACTIVE',
  deactivated_at = NULL;

SET @ai_camera_user := (SELECT user_id FROM user_account WHERE login_id = 'ai-camera-a');
SET @ai_editor_user := (SELECT user_id FROM user_account WHERE login_id = 'ai-editor-a');
SET @ai_sound_user := (SELECT user_id FROM user_account WHERE login_id = 'ai-sound-a');
SET @ai_art_user := (SELECT user_id FROM user_account WHERE login_id = 'ai-art-a');
SET @ai_light_user := (SELECT user_id FROM user_account WHERE login_id = 'ai-light-a');
SET @ai_vfx_user := (SELECT user_id FROM user_account WHERE login_id = 'ai-vfx-a');
SET @ai_leader_a_user := (SELECT user_id FROM user_account WHERE login_id = 'ai-leader-a');
SET @ai_leader_b_user := (SELECT user_id FROM user_account WHERE login_id = 'ai-leader-b');

SET @region_jongno := (SELECT region_id FROM region WHERE region_code = '1111000000');
SET @region_junggu := (SELECT region_id FROM region WHERE region_code = '1114000000');
SET @region_gangnam := (SELECT region_id FROM region WHERE region_code = '1168000000');
SET @region_mapo := (SELECT region_id FROM region WHERE region_code = '1144000000');
SET @region_bundang := (SELECT region_id FROM region WHERE region_code = '4113500000');

INSERT INTO member_profile
(user_id, display_name, short_intro, detail_intro, visibility, activity_status, region_id, experience_level, join_availability, collaboration_status, travel_range, preferred_duration, equipment_status, age_band, participation_mode, profile_completed_yn, status, last_active_at, created_at)
VALUES
(@ai_camera_user, '서하늘 촬영감독', '자연광과 핸드헬드 촬영에 강한 촬영감독입니다.', '서울 도심 로케이션과 저예산 단편 촬영 경험이 있어 드라마/청춘 장르 팀과 잘 맞습니다.', 'PUBLIC', 'VISIBLE', @region_junggu, 'Y3_10', 'IMMEDIATE', 'AVAILABLE', 'KM_30', 'WITHIN_3M', 'HAS_EQUIPMENT', 'TWENTIES', 'OFFLINE', 'Y', 'ACTIVE', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 16 DAY),
(@ai_editor_user, '민채원 편집감독', '감정선 중심의 컷 구성과 색감 정리에 강합니다.', '뮤직비디오와 단편 드라마 후반 작업 경험이 있으며 원격 협업에 익숙합니다.', 'PUBLIC', 'VISIBLE', @region_gangnam, 'Y3_10', 'WITHIN_1W', 'AVAILABLE', 'ANYWHERE', 'ANY', 'HAS_EQUIPMENT', 'THIRTIES', 'REMOTE', 'Y', 'ACTIVE', NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 15 DAY),
(@ai_sound_user, '강태민 사운드', '현장 동시녹음과 후반 사운드 디자인을 함께 맡을 수 있습니다.', '스릴러와 다큐멘터리 프로젝트에서 공간감 있는 사운드 작업을 진행했습니다.', 'PUBLIC', 'VISIBLE', @region_bundang, 'Y3_10', 'WITHIN_2W', 'AVAILABLE', 'KM_100', 'WITHIN_6M', 'HAS_EQUIPMENT', 'THIRTIES', 'HYBRID', 'Y', 'ACTIVE', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 14 DAY),
(@ai_art_user, '오수아 미술감독', '소품과 색채 톤을 활용한 저예산 미술 설계에 강합니다.', '판타지/미스터리 단편에서 소품 제작과 공간 스타일링을 담당했습니다.', 'PUBLIC', 'VISIBLE', @region_mapo, 'Y0_3', 'WITHIN_1W', 'CONSIDERING', 'KM_30', 'WITHIN_3M', 'NOT_ENTERED', 'TWENTIES', 'HYBRID', 'Y', 'ACTIVE', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 13 DAY),
(@ai_light_user, '문준혁 조명감독', '야간 촬영과 작은 공간 조명 세팅에 강합니다.', '서울/경기권 단편 촬영장에서 조명감독과 조명부 경험을 쌓았습니다.', 'PUBLIC', 'VISIBLE', @region_jongno, 'Y3_10', 'IMMEDIATE', 'AVAILABLE', 'KM_100', 'WITHIN_3M', 'HAS_EQUIPMENT', 'THIRTIES', 'OFFLINE', 'Y', 'ACTIVE', NOW() - INTERVAL 7 HOUR, NOW() - INTERVAL 12 DAY),
(@ai_vfx_user, '정하린 VFX', '짧은 숏 합성과 색보정 파이프라인 정리에 익숙합니다.', 'SF와 실험 영상의 간단한 합성, 색보정, 타이틀 작업을 지원할 수 있습니다.', 'PUBLIC', 'VISIBLE', @region_gangnam, 'Y0_3', 'NEGOTIABLE', 'AVAILABLE', 'ANYWHERE', 'ANY', 'HAS_EQUIPMENT', 'TWENTIES', 'REMOTE', 'Y', 'ACTIVE', NOW() - INTERVAL 9 HOUR, NOW() - INTERVAL 11 DAY),
(@ai_leader_a_user, '한도윤 프로듀서', '일정 관리와 팀 커뮤니케이션을 정리하는 프로듀서입니다.', '청춘 드라마와 브랜드 필름 프로젝트에서 제작 진행을 맡았습니다.', 'PUBLIC', 'VISIBLE', @region_junggu, 'Y3_10', 'WITHIN_1W', 'AVAILABLE', 'KM_30', 'WITHIN_3M', 'NOT_ENTERED', 'THIRTIES', 'HYBRID', 'Y', 'ACTIVE', NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 10 DAY),
(@ai_leader_b_user, '윤서진 감독', '장르 단편과 인물 중심 미스터리 연출을 선호합니다.', '미스터리/스릴러 기반의 단편 시나리오 개발과 현장 연출 경험이 있습니다.', 'PUBLIC', 'VISIBLE', @region_bundang, 'Y3_10', 'WITHIN_2W', 'AVAILABLE', 'KM_100', 'WITHIN_6M', 'NOT_ENTERED', 'THIRTIES', 'OFFLINE', 'Y', 'ACTIVE', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 9 DAY)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  short_intro = VALUES(short_intro),
  detail_intro = VALUES(detail_intro),
  visibility = 'PUBLIC',
  activity_status = 'VISIBLE',
  status = 'ACTIVE',
  deleted_at = NULL,
  last_active_at = VALUES(last_active_at);

SET @ai_camera_profile := (SELECT profile_id FROM member_profile WHERE user_id = @ai_camera_user);
SET @ai_editor_profile := (SELECT profile_id FROM member_profile WHERE user_id = @ai_editor_user);
SET @ai_sound_profile := (SELECT profile_id FROM member_profile WHERE user_id = @ai_sound_user);
SET @ai_art_profile := (SELECT profile_id FROM member_profile WHERE user_id = @ai_art_user);
SET @ai_light_profile := (SELECT profile_id FROM member_profile WHERE user_id = @ai_light_user);
SET @ai_vfx_profile := (SELECT profile_id FROM member_profile WHERE user_id = @ai_vfx_user);
SET @ai_leader_a_profile := (SELECT profile_id FROM member_profile WHERE user_id = @ai_leader_a_user);
SET @ai_leader_b_profile := (SELECT profile_id FROM member_profile WHERE user_id = @ai_leader_b_user);

DELETE FROM profile_role WHERE profile_id IN (@ai_camera_profile, @ai_editor_profile, @ai_sound_profile, @ai_art_profile, @ai_light_profile, @ai_vfx_profile, @ai_leader_a_profile, @ai_leader_b_profile);
INSERT INTO profile_role (profile_id, role_id, sort_order) VALUES
(@ai_camera_profile, 9, 0), (@ai_camera_profile, 10, 1),
(@ai_editor_profile, 22, 0), (@ai_editor_profile, 23, 1),
(@ai_sound_profile, 14, 0), (@ai_sound_profile, 15, 1),
(@ai_art_profile, 16, 0), (@ai_art_profile, 17, 1),
(@ai_light_profile, 12, 0), (@ai_light_profile, 13, 1),
(@ai_vfx_profile, 24, 0), (@ai_vfx_profile, 23, 1),
(@ai_leader_a_profile, 1, 0), (@ai_leader_a_profile, 2, 1),
(@ai_leader_b_profile, 4, 0), (@ai_leader_b_profile, 7, 1);

DELETE FROM profile_genre WHERE profile_id IN (@ai_camera_profile, @ai_editor_profile, @ai_sound_profile, @ai_art_profile, @ai_light_profile, @ai_vfx_profile, @ai_leader_a_profile, @ai_leader_b_profile);
INSERT INTO profile_genre (profile_id, genre_id) VALUES
(@ai_camera_profile, 1), (@ai_camera_profile, 16),
(@ai_editor_profile, 1), (@ai_editor_profile, 14),
(@ai_sound_profile, 5), (@ai_sound_profile, 11),
(@ai_art_profile, 7), (@ai_art_profile, 10),
(@ai_light_profile, 1), (@ai_light_profile, 5),
(@ai_vfx_profile, 9), (@ai_vfx_profile, 13),
(@ai_leader_a_profile, 1), (@ai_leader_a_profile, 16),
(@ai_leader_b_profile, 5), (@ai_leader_b_profile, 7);

DELETE FROM profile_collaboration_condition WHERE profile_id IN (@ai_camera_profile, @ai_editor_profile, @ai_sound_profile, @ai_art_profile, @ai_light_profile, @ai_vfx_profile, @ai_leader_a_profile, @ai_leader_b_profile);
INSERT INTO profile_collaboration_condition (profile_id, condition_code) VALUES
(@ai_camera_profile, 'NEGOTIABLE'), (@ai_camera_profile, 'PAID'),
(@ai_editor_profile, 'PAID'), (@ai_editor_profile, 'REVENUE_SHARE'),
(@ai_sound_profile, 'NEGOTIABLE'), (@ai_sound_profile, 'PAID'),
(@ai_art_profile, 'UNPAID'), (@ai_art_profile, 'NEGOTIABLE'),
(@ai_light_profile, 'PAID'), (@ai_light_profile, 'NEGOTIABLE'),
(@ai_vfx_profile, 'REVENUE_SHARE'), (@ai_vfx_profile, 'NEGOTIABLE'),
(@ai_leader_a_profile, 'NEGOTIABLE'), (@ai_leader_a_profile, 'PAID'),
(@ai_leader_b_profile, 'PAID'), (@ai_leader_b_profile, 'REVENUE_SHARE');

INSERT INTO team (leader_user_id, name, description, status, region_id, region_any_yn, expected_duration, max_member_count, current_member_count, last_active_at, created_at)
SELECT @ai_leader_a_user, 'AI 청춘 드라마 제작팀', '서울 중구와 종로구를 배경으로 청춘 드라마 단편을 제작하는 AI 추천 테스트 팀입니다.', 'RECRUITING', @region_junggu, 'N', 'WITHIN_3M', 6, 2, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (SELECT 1 FROM team WHERE name = 'AI 청춘 드라마 제작팀');

INSERT INTO team (leader_user_id, name, description, status, region_id, region_any_yn, expected_duration, max_member_count, current_member_count, last_active_at, created_at)
SELECT @ai_leader_b_user, 'AI 분당 미스터리 팀', '경기 분당 오피스 공간을 활용해 미스터리/스릴러 단편을 준비하는 테스트 팀입니다.', 'RECRUITING', @region_bundang, 'N', 'WITHIN_6M', 7, 2, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 7 DAY
WHERE NOT EXISTS (SELECT 1 FROM team WHERE name = 'AI 분당 미스터리 팀');

INSERT INTO team (leader_user_id, name, description, status, region_id, region_any_yn, expected_duration, max_member_count, current_member_count, last_active_at, created_at)
SELECT @ai_editor_user, 'AI 원격 후반 협업팀', '원격 기반으로 음악 공연 영상의 편집, 색보정, VFX를 함께 진행하는 테스트 팀입니다.', 'RECRUITING', NULL, 'Y', 'ANY', 5, 2, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM team WHERE name = 'AI 원격 후반 협업팀');

INSERT INTO team (leader_user_id, name, description, status, region_id, region_any_yn, expected_duration, max_member_count, current_member_count, last_active_at, created_at)
SELECT @ai_camera_user, 'AI 로케이션 다큐팀', '서울과 경기권의 공간 기록을 중심으로 다큐멘터리 포트폴리오를 제작하는 테스트 팀입니다.', 'RECRUITING', @region_jongno, 'N', 'WITHIN_3M', 5, 1, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM team WHERE name = 'AI 로케이션 다큐팀');

SET @ai_team_drama := (SELECT team_id FROM team WHERE name = 'AI 청춘 드라마 제작팀' ORDER BY team_id LIMIT 1);
SET @ai_team_mystery := (SELECT team_id FROM team WHERE name = 'AI 분당 미스터리 팀' ORDER BY team_id LIMIT 1);
SET @ai_team_post := (SELECT team_id FROM team WHERE name = 'AI 원격 후반 협업팀' ORDER BY team_id LIMIT 1);
SET @ai_team_docu := (SELECT team_id FROM team WHERE name = 'AI 로케이션 다큐팀' ORDER BY team_id LIMIT 1);

DELETE FROM team_genre WHERE team_id IN (@ai_team_drama, @ai_team_mystery, @ai_team_post, @ai_team_docu);
INSERT INTO team_genre (team_id, genre_id) VALUES
(@ai_team_drama, 1), (@ai_team_drama, 16),
(@ai_team_mystery, 5), (@ai_team_mystery, 7),
(@ai_team_post, 14), (@ai_team_post, 13),
(@ai_team_docu, 11), (@ai_team_docu, 1);

INSERT IGNORE INTO team_member (team_id, user_id, team_role, status, joined_at) VALUES
(@ai_team_drama, @ai_leader_a_user, 'LEADER', 'ACTIVE', NOW() - INTERVAL 8 DAY),
(@ai_team_drama, @ai_art_user, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 4 DAY),
(@ai_team_mystery, @ai_leader_b_user, 'LEADER', 'ACTIVE', NOW() - INTERVAL 7 DAY),
(@ai_team_mystery, @ai_sound_user, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 3 DAY),
(@ai_team_post, @ai_editor_user, 'LEADER', 'ACTIVE', NOW() - INTERVAL 6 DAY),
(@ai_team_post, @ai_vfx_user, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 2 DAY),
(@ai_team_docu, @ai_camera_user, 'LEADER', 'ACTIVE', NOW() - INTERVAL 5 DAY);

INSERT INTO team_recruitment (team_id, title, status, deadline_at, work_start_at, created_by, created_at)
SELECT @ai_team_drama, 'AI 청춘 드라마 촬영/사운드 모집', 'OPEN', NOW() + INTERVAL 24 DAY, NOW() + INTERVAL 10 DAY, @ai_leader_a_user, NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment WHERE team_id = @ai_team_drama AND title = 'AI 청춘 드라마 촬영/사운드 모집');

INSERT INTO team_recruitment (team_id, title, status, deadline_at, work_start_at, created_by, created_at)
SELECT @ai_team_mystery, 'AI 미스터리 핵심 스태프 모집', 'OPEN', NOW() + INTERVAL 28 DAY, NOW() + INTERVAL 14 DAY, @ai_leader_b_user, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment WHERE team_id = @ai_team_mystery AND title = 'AI 미스터리 핵심 스태프 모집');

INSERT INTO team_recruitment (team_id, title, status, deadline_at, work_start_at, created_by, created_at)
SELECT @ai_team_post, 'AI 원격 후반 색보정/VFX 모집', 'OPEN', NOW() + INTERVAL 20 DAY, NOW() + INTERVAL 7 DAY, @ai_editor_user, NOW() - INTERVAL 4 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment WHERE team_id = @ai_team_post AND title = 'AI 원격 후반 색보정/VFX 모집');

INSERT INTO team_recruitment (team_id, title, status, deadline_at, work_start_at, created_by, created_at)
SELECT @ai_team_docu, 'AI 다큐 사운드/편집 모집', 'OPEN', NOW() + INTERVAL 18 DAY, NOW() + INTERVAL 9 DAY, @ai_camera_user, NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM team_recruitment WHERE team_id = @ai_team_docu AND title = 'AI 다큐 사운드/편집 모집');

SET @ai_recruit_drama := (SELECT recruitment_id FROM team_recruitment WHERE team_id = @ai_team_drama AND title = 'AI 청춘 드라마 촬영/사운드 모집' ORDER BY recruitment_id LIMIT 1);
SET @ai_recruit_mystery := (SELECT recruitment_id FROM team_recruitment WHERE team_id = @ai_team_mystery AND title = 'AI 미스터리 핵심 스태프 모집' ORDER BY recruitment_id LIMIT 1);
SET @ai_recruit_post := (SELECT recruitment_id FROM team_recruitment WHERE team_id = @ai_team_post AND title = 'AI 원격 후반 색보정/VFX 모집' ORDER BY recruitment_id LIMIT 1);
SET @ai_recruit_docu := (SELECT recruitment_id FROM team_recruitment WHERE team_id = @ai_team_docu AND title = 'AI 다큐 사운드/편집 모집' ORDER BY recruitment_id LIMIT 1);

DELETE FROM team_recruitment_slot WHERE recruitment_id IN (@ai_recruit_drama, @ai_recruit_mystery, @ai_recruit_post, @ai_recruit_docu);
INSERT INTO team_recruitment_slot
(recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status)
VALUES
(@ai_recruit_drama, 9, 1, 0, 'Y0_3', 'NEGOTIABLE', 'Y', 'WITHIN_3M', 'Y', 'OPEN'),
(@ai_recruit_drama, 14, 1, 0, 'Y0_3', 'NEGOTIABLE', 'N', 'WITHIN_3M', 'Y', 'OPEN'),
(@ai_recruit_mystery, 9, 1, 0, 'Y3_10', 'PAID', 'Y', 'WITHIN_6M', 'Y', 'OPEN'),
(@ai_recruit_mystery, 12, 1, 0, 'Y3_10', 'PAID', 'Y', 'WITHIN_6M', 'Y', 'OPEN'),
(@ai_recruit_mystery, 16, 1, 0, 'Y0_3', 'NEGOTIABLE', 'N', 'WITHIN_3M', 'N', 'OPEN'),
(@ai_recruit_post, 23, 1, 0, 'Y0_3', 'REVENUE_SHARE', 'Y', 'ANY', 'Y', 'OPEN'),
(@ai_recruit_post, 24, 1, 0, 'Y0_3', 'NEGOTIABLE', 'N', 'ANY', 'Y', 'OPEN'),
(@ai_recruit_docu, 14, 1, 0, 'Y3_10', 'NEGOTIABLE', 'Y', 'WITHIN_3M', 'Y', 'OPEN'),
(@ai_recruit_docu, 22, 1, 0, 'Y0_3', 'REVENUE_SHARE', 'N', 'WITHIN_3M', 'Y', 'OPEN');

UPDATE team t
SET current_member_count = (
  SELECT COUNT(*)
  FROM team_member tm
  WHERE tm.team_id = t.team_id
    AND tm.status = 'ACTIVE'
)
WHERE t.team_id IN (@ai_team_drama, @ai_team_mystery, @ai_team_post, @ai_team_docu);

INSERT IGNORE INTO matching_bookmark (user_id, target_type, target_id, created_at) VALUES
(@ai_leader_a_user, 'PROFILE', @ai_camera_profile, NOW() - INTERVAL 2 DAY),
(@ai_leader_a_user, 'PROFILE', @ai_sound_profile, NOW() - INTERVAL 1 DAY),
(@ai_camera_user, 'TEAM', @ai_team_drama, NOW() - INTERVAL 1 DAY),
(@ai_vfx_user, 'TEAM', @ai_team_post, NOW() - INTERVAL 3 HOUR);

COMMIT;
