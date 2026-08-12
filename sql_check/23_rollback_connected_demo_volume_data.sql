SET NAMES utf8mb4;
USE slate;

START TRANSACTION;

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

COMMIT;
