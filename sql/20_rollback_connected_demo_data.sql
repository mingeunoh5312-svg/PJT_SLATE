SET NAMES utf8mb4;
USE slate;

START TRANSACTION;

SET @cdd_river_team := (SELECT team_id FROM team WHERE name = '[CDD] 한강 야간 단편팀' ORDER BY team_id LIMIT 1);
SET @cdd_closed_team := (SELECT team_id FROM team WHERE name = '[CDD] 완료된 포트폴리오팀' ORDER BY team_id LIMIT 1);
SET @cdd_work_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 한강 야간 리허설 컷' ORDER BY post_id LIMIT 1);
SET @cdd_info_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 야간 촬영 체크리스트 공유' ORDER BY post_id LIMIT 1);
SET @cdd_moderation_post := (SELECT post_id FROM board_post WHERE title = '[CDD] 운영 정책 검토용 숨김 게시글' ORDER BY post_id LIMIT 1);
SET @cdd_work_item := (SELECT work_id FROM work_item WHERE board_post_id = @cdd_work_post ORDER BY work_id LIMIT 1);
SET @cdd_contest := (SELECT contest_id FROM contest WHERE title = '[CDD] 도시 단편 제작지원 공모' ORDER BY contest_id LIMIT 1);
SET @cdd_contest_request := (SELECT request_id FROM contest_open_request WHERE title = '[CDD] 도시 단편 제작지원 요청' ORDER BY request_id LIMIT 1);
SET @cdd_company_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-company' LIMIT 1);
SET @cdd_moderated_user := (SELECT user_id FROM user_account WHERE login_id = 'cdd-moderated' LIMIT 1);

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
WHERE contest_id = @cdd_contest;

DELETE FROM contest_fit_cache
WHERE contest_id = @cdd_contest;

DELETE FROM contest_save
WHERE contest_id = @cdd_contest;

DELETE FROM contest_open_request
WHERE request_id = @cdd_contest_request;

DELETE FROM contest
WHERE contest_id = @cdd_contest;

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
WHERE work_id = @cdd_work_item;

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

COMMIT;
