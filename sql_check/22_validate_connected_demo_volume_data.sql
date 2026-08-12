SET NAMES utf8mb4;
USE slate;

SELECT 'CDV_EXPECTED_COUNTS' AS section, 'user_account' AS item, COUNT(*) AS actual_count, 37 AS expected_count
FROM user_account
WHERE login_id LIKE 'cdv-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'company_application', COUNT(*), 4
FROM company_application ca
JOIN user_account ua ON ua.user_id = ca.user_id
WHERE ua.login_id LIKE 'cdv-company-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'admin_permission', COUNT(*), 9
FROM admin_permission ap
JOIN user_account ua ON ua.user_id = ap.user_id
WHERE ua.login_id = 'cdv-admin'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'member_profile', COUNT(*), 32
FROM member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id LIKE 'cdv-user-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'profile_role', COUNT(*), 40
FROM profile_role pr
JOIN member_profile mp ON mp.profile_id = pr.profile_id
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id LIKE 'cdv-user-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'profile_genre', COUNT(*), 64
FROM profile_genre pg
JOIN member_profile mp ON mp.profile_id = pg.profile_id
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id LIKE 'cdv-user-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'profile_collaboration_condition', COUNT(*), 64
FROM profile_collaboration_condition pcc
JOIN member_profile mp ON mp.profile_id = pcc.profile_id
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id LIKE 'cdv-user-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'user_follow', COUNT(*), 128
FROM user_follow uf
JOIN user_account ua ON ua.user_id = uf.follower_user_id
WHERE ua.login_id LIKE 'cdv-user-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team', COUNT(*), 12
FROM team
WHERE name LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_genre', COUNT(*), 24
FROM team_genre tg
JOIN team t ON t.team_id = tg.team_id
WHERE t.name LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_member', COUNT(*), 36
FROM team_member tm
JOIN team t ON t.team_id = tm.team_id
WHERE t.name LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_recruitment', COUNT(*), 24
FROM team_recruitment tr
JOIN team t ON t.team_id = tr.team_id
WHERE t.name LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_recruitment_slot', COUNT(*), 60
FROM team_recruitment_slot s
JOIN team_recruitment tr ON tr.recruitment_id = s.recruitment_id
JOIN team t ON t.team_id = tr.team_id
WHERE t.name LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_application', COUNT(*), 60
FROM team_application ta
JOIN team t ON t.team_id = ta.team_id
WHERE t.name LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_invitation', COUNT(*), 36
FROM team_invitation ti
JOIN team t ON t.team_id = ti.team_id
WHERE t.name LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_plan_item', COUNT(*), 60
FROM team_plan_item tp
JOIN team t ON t.team_id = tp.team_id
WHERE t.name LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_closure_snapshot', COUNT(*), 2
FROM team_closure_snapshot tcs
JOIN team t ON t.team_id = tcs.team_id
WHERE t.name LIKE '[CDV]%'
  AND JSON_UNQUOTE(JSON_EXTRACT(tcs.snapshot_json, '$.namespace')) = 'CDV'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'matching_bookmark', COUNT(*), 96
FROM matching_bookmark mb
JOIN user_account ua ON ua.user_id = mb.user_id
WHERE ua.login_id LIKE 'cdv-user-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'matching_action_log', COUNT(*), 96
FROM matching_action_log mal
JOIN user_account ua ON ua.user_id = mal.actor_user_id
WHERE ua.login_id LIKE 'cdv-user-%'
  AND mal.action_type LIKE 'CDV_%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'board_post', COUNT(*), 60
FROM board_post
WHERE title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'board_review', COUNT(*), 180
FROM board_review br
JOIN board_post bp ON bp.post_id = br.post_id
WHERE bp.title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'board_like', COUNT(*), 300
FROM board_like bl
JOIN board_post bp ON bp.post_id = bl.post_id
WHERE bp.title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'board_view_log', COUNT(*), 480
FROM board_view_log bvl
JOIN board_post bp ON bp.post_id = bvl.post_id
WHERE bp.title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'work_item', COUNT(*), 36
FROM work_item
WHERE title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'work_genre', COUNT(*), 72
FROM work_genre wg
JOIN work_item wi ON wi.work_id = wg.work_id
WHERE wi.title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_work_approval_request', COUNT(*), 24
FROM team_work_approval_request
WHERE title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'team_work_approval_genre', COUNT(*), 48
FROM team_work_approval_genre twag
JOIN team_work_approval_request twr ON twr.request_id = twag.request_id
WHERE twr.title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'portfolio_item', COUNT(*), 64
FROM portfolio_item
WHERE external_source_name = 'SLATE_CDV'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'contest_open_request', COUNT(*), 6
FROM contest_open_request
WHERE title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'contest', COUNT(*), 24
FROM contest
WHERE title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'contest_save', COUNT(*), 120
FROM contest_save cs
JOIN contest c ON c.contest_id = cs.contest_id
WHERE c.title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'contest_submission_prepare', COUNT(*), 48
FROM contest_submission_prepare csp
JOIN contest c ON c.contest_id = csp.contest_id
WHERE c.title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'contest_fit_cache', COUNT(*), 48
FROM contest_fit_cache cfc
JOIN contest c ON c.contest_id = cfc.contest_id
WHERE c.title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'content_report', COUNT(*), 12
FROM content_report cr
JOIN user_account reporter ON reporter.user_id = cr.reporter_user_id
WHERE reporter.login_id LIKE 'cdv-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'user_sanction', COUNT(*), 4
FROM user_sanction us
JOIN user_account ua ON ua.user_id = us.user_id
WHERE ua.login_id LIKE 'cdv-%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'notification', COUNT(*), 180
FROM notification
WHERE title LIKE '[CDV]%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'audit_log', COUNT(*), 28
FROM audit_log
WHERE action_type LIKE 'CDV_%'
UNION ALL
SELECT 'CDV_EXPECTED_COUNTS', 'operation_log', COUNT(*), 4
FROM operation_log
WHERE event_code LIKE 'CDV_%';

SELECT 'CDV_STATUS_COUNTS' AS section, 'team' AS table_name, status, COUNT(*) AS row_count
FROM team
WHERE name LIKE '[CDV]%'
GROUP BY status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'team_application', ta.status, COUNT(*)
FROM team_application ta
JOIN team t ON t.team_id = ta.team_id
WHERE t.name LIKE '[CDV]%'
GROUP BY ta.status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'team_invitation', ti.status, COUNT(*)
FROM team_invitation ti
JOIN team t ON t.team_id = ti.team_id
WHERE t.name LIKE '[CDV]%'
GROUP BY ti.status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'team_plan_item', tp.status, COUNT(*)
FROM team_plan_item tp
JOIN team t ON t.team_id = tp.team_id
WHERE t.name LIKE '[CDV]%'
GROUP BY tp.status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'team_work_approval_request', status, COUNT(*)
FROM team_work_approval_request
WHERE title LIKE '[CDV]%'
GROUP BY status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'board_post', status, COUNT(*)
FROM board_post
WHERE title LIKE '[CDV]%'
GROUP BY status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'board_review', br.status, COUNT(*)
FROM board_review br
JOIN board_post bp ON bp.post_id = br.post_id
WHERE bp.title LIKE '[CDV]%'
GROUP BY br.status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'contest_open_request', status, COUNT(*)
FROM contest_open_request
WHERE title LIKE '[CDV]%'
GROUP BY status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'contest', status, COUNT(*)
FROM contest
WHERE title LIKE '[CDV]%'
GROUP BY status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'content_report', cr.status, COUNT(*)
FROM content_report cr
JOIN user_account reporter ON reporter.user_id = cr.reporter_user_id
WHERE reporter.login_id LIKE 'cdv-%'
GROUP BY cr.status
UNION ALL
SELECT 'CDV_STATUS_COUNTS', 'user_sanction', us.status, COUNT(*)
FROM user_sanction us
JOIN user_account ua ON ua.user_id = us.user_id
WHERE ua.login_id LIKE 'cdv-%'
GROUP BY us.status
ORDER BY table_name, status;

SELECT 'CDV_ZERO_ERROR_CHECKS' AS section, 'missing_required_cdv_ids' AS check_name,
  CASE WHEN
    (SELECT COUNT(*) FROM user_account WHERE login_id LIKE 'cdv-%') <> 37
    OR (SELECT COUNT(*) FROM team WHERE name LIKE '[CDV]%') <> 12
    OR (SELECT COUNT(*) FROM board_post WHERE title LIKE '[CDV]%') <> 60
    OR (SELECT COUNT(*) FROM work_item WHERE title LIKE '[CDV]%') <> 36
    OR (SELECT COUNT(*) FROM contest WHERE title LIKE '[CDV]%') <> 24
  THEN 1 ELSE 0 END AS issue_count
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cdv_admin_non_catalog_permission', COUNT(*)
FROM admin_permission ap
JOIN user_account ua ON ua.user_id = ap.user_id
WHERE ua.login_id = 'cdv-admin'
  AND ap.permission_code NOT IN (
    'COMPANY_APPROVAL',
    'USER_SANCTION',
    'CONTENT_MODERATION',
    'SCORE_POLICY',
    'CONTEST_MANAGE',
    'NOTIFICATION_SEND',
    'LOG_VIEW',
    'ADMIN_PERMISSION_MANAGE',
    'REGION_MANAGE'
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cdv_admin_missing_catalog_permission',
  9 - COUNT(DISTINCT CASE
    WHEN ap.active_yn = 'Y'
     AND ap.permission_code IN (
       'COMPANY_APPROVAL',
       'USER_SANCTION',
       'CONTENT_MODERATION',
       'SCORE_POLICY',
       'CONTEST_MANAGE',
       'NOTIFICATION_SEND',
       'LOG_VIEW',
       'ADMIN_PERMISSION_MANAGE',
       'REGION_MANAGE'
     )
    THEN ap.permission_code
  END)
FROM user_account ua
LEFT JOIN admin_permission ap ON ap.user_id = ua.user_id
WHERE ua.login_id = 'cdv-admin'
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cdv_admin_inactive_permission', COUNT(*)
FROM admin_permission ap
JOIN user_account ua ON ua.user_id = ap.user_id
WHERE ua.login_id = 'cdv-admin'
  AND ap.active_yn <> 'Y'
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'team_leader_active_mismatch', COUNT(*)
FROM team t
LEFT JOIN team_member tm
  ON tm.team_id = t.team_id
 AND tm.user_id = t.leader_user_id
 AND tm.team_role = 'LEADER'
 AND tm.status = 'ACTIVE'
WHERE t.name LIKE '[CDV]%'
  AND tm.team_member_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'active_leader_count_not_one', COUNT(*)
FROM (
  SELECT t.team_id
  FROM team t
  LEFT JOIN team_member tm
    ON tm.team_id = t.team_id
   AND tm.team_role = 'LEADER'
   AND tm.status = 'ACTIVE'
  WHERE t.name LIKE '[CDV]%'
  GROUP BY t.team_id
  HAVING COUNT(tm.team_member_id) <> 1
) issue
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'team_current_member_count_mismatch', COUNT(*)
FROM team t
WHERE t.name LIKE '[CDV]%'
  AND t.current_member_count <> (
    SELECT COUNT(*)
    FROM team_member tm
    WHERE tm.team_id = t.team_id
      AND tm.status = 'ACTIVE'
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'slot_accepted_count_mismatch', COUNT(*)
FROM team_recruitment_slot s
JOIN team_recruitment tr ON tr.recruitment_id = s.recruitment_id
JOIN team t ON t.team_id = tr.team_id
WHERE t.name LIKE '[CDV]%'
  AND s.accepted_count <> (
    SELECT COUNT(*)
    FROM team_application ta
    WHERE ta.slot_id = s.slot_id
      AND ta.status = 'ACCEPTED'
  ) + (
    SELECT COUNT(*)
    FROM team_invitation ti
    WHERE ti.slot_id = s.slot_id
      AND ti.status = 'ACCEPTED'
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'slot_accepted_count_over_required', COUNT(*)
FROM team_recruitment_slot s
JOIN team_recruitment tr ON tr.recruitment_id = s.recruitment_id
JOIN team t ON t.team_id = tr.team_id
WHERE t.name LIKE '[CDV]%'
  AND s.accepted_count > s.required_count
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'accepted_application_missing_active_member', COUNT(*)
FROM team_application ta
JOIN team t ON t.team_id = ta.team_id
LEFT JOIN team_member tm
  ON tm.team_id = ta.team_id
 AND tm.user_id = ta.applicant_user_id
 AND tm.status = 'ACTIVE'
WHERE t.name LIKE '[CDV]%'
  AND ta.status = 'ACCEPTED'
  AND tm.team_member_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'accepted_invitation_missing_active_member', COUNT(*)
FROM team_invitation ti
JOIN team t ON t.team_id = ti.team_id
LEFT JOIN team_member tm
  ON tm.team_id = ti.team_id
 AND tm.user_id = ti.target_user_id
 AND tm.status = 'ACTIVE'
WHERE t.name LIKE '[CDV]%'
  AND ti.status = 'ACCEPTED'
  AND tm.team_member_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'pending_application_already_active_member', COUNT(*)
FROM team_application ta
JOIN team t ON t.team_id = ta.team_id
JOIN team_member tm
  ON tm.team_id = ta.team_id
 AND tm.user_id = ta.applicant_user_id
 AND tm.status = 'ACTIVE'
WHERE t.name LIKE '[CDV]%'
  AND ta.status = 'PENDING'
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'pending_invitation_already_active_member', COUNT(*)
FROM team_invitation ti
JOIN team t ON t.team_id = ti.team_id
JOIN team_member tm
  ON tm.team_id = ti.team_id
 AND tm.user_id = ti.target_user_id
 AND tm.status = 'ACTIVE'
WHERE t.name LIKE '[CDV]%'
  AND ti.status = 'PENDING'
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'ended_team_open_recruitment_or_slot', COUNT(*)
FROM team t
WHERE t.name LIKE '[CDV]%'
  AND t.status = 'ENDED'
  AND (
    EXISTS (
      SELECT 1 FROM team_recruitment tr
      WHERE tr.team_id = t.team_id
        AND tr.status = 'OPEN'
    )
    OR EXISTS (
      SELECT 1
      FROM team_recruitment tr
      JOIN team_recruitment_slot s ON s.recruitment_id = tr.recruitment_id
      WHERE tr.team_id = t.team_id
        AND s.status = 'OPEN'
    )
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'ended_team_missing_snapshot', COUNT(*)
FROM team t
LEFT JOIN team_closure_snapshot tcs
  ON tcs.team_id = t.team_id
 AND JSON_UNQUOTE(JSON_EXTRACT(tcs.snapshot_json, '$.namespace')) = 'CDV'
WHERE t.name LIKE '[CDV]%'
  AND t.status = 'ENDED'
  AND tcs.closure_snapshot_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'board_like_count_mismatch', COUNT(*)
FROM board_post bp
WHERE bp.title LIKE '[CDV]%'
  AND bp.like_count <> (
    SELECT COUNT(*)
    FROM board_like bl
    WHERE bl.post_id = bp.post_id
      AND bl.active_yn = 'Y'
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'board_review_count_mismatch', COUNT(*)
FROM board_post bp
WHERE bp.title LIKE '[CDV]%'
  AND bp.review_count <> (
    SELECT COUNT(*)
    FROM board_review br
    WHERE br.post_id = bp.post_id
      AND br.status = 'PUBLISHED'
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'board_view_count_mismatch', COUNT(*)
FROM board_post bp
WHERE bp.title LIKE '[CDV]%'
  AND bp.view_count <> (
    SELECT COUNT(*)
    FROM board_view_log bvl
    WHERE bvl.post_id = bp.post_id
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'work_post_link_mismatch', COUNT(*)
FROM work_item wi
LEFT JOIN board_post bp ON bp.post_id = wi.board_post_id
WHERE wi.title LIKE '[CDV]%'
  AND wi.board_post_id IS NOT NULL
  AND (bp.post_id IS NULL OR bp.category <> 'WORK' OR bp.title <> wi.title)
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'approved_work_request_unlinked', COUNT(*)
FROM team_work_approval_request twr
LEFT JOIN board_post bp ON bp.post_id = twr.board_post_id
LEFT JOIN work_item wi ON wi.work_id = twr.work_id
WHERE twr.title LIKE '[CDV]%'
  AND twr.status = 'APPROVED'
  AND (
    twr.board_post_id IS NULL
    OR twr.work_id IS NULL
    OR bp.post_id IS NULL
    OR wi.work_id IS NULL
    OR wi.board_post_id <> bp.post_id
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'team_work_request_requester_not_active_member', COUNT(*)
FROM team_work_approval_request twr
LEFT JOIN team_member tm
  ON tm.team_id = twr.team_id
 AND tm.user_id = twr.requester_user_id
 AND tm.status = 'ACTIVE'
WHERE twr.title LIKE '[CDV]%'
  AND tm.team_member_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'contest_save_count_mismatch', COUNT(*)
FROM contest c
WHERE c.title LIKE '[CDV]%'
  AND c.save_count <> (
    SELECT COUNT(*)
    FROM contest_save cs
    WHERE cs.contest_id = c.contest_id
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'contest_request_link_mismatch', COUNT(*)
FROM contest_open_request cor
LEFT JOIN contest approved ON approved.contest_id = cor.approved_contest_id
WHERE cor.title LIKE '[CDV]%'
  AND (
    (cor.status = 'APPROVED' AND (
      cor.approved_contest_id IS NULL
      OR approved.source_request_id <> cor.request_id
      OR approved.requester_company_user_id <> cor.requester_user_id
    ))
    OR (cor.status <> 'APPROVED' AND cor.approved_contest_id IS NOT NULL)
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'self_follow', COUNT(*)
FROM user_follow uf
JOIN user_account ua ON ua.user_id = uf.follower_user_id
WHERE ua.login_id LIKE 'cdv-%'
  AND uf.follower_user_id = uf.following_user_id
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'duplicate_pending_application', COUNT(*)
FROM (
  SELECT ta.team_id, ta.slot_id, ta.applicant_user_id
  FROM team_application ta
  JOIN team t ON t.team_id = ta.team_id
  WHERE t.name LIKE '[CDV]%'
    AND ta.status = 'PENDING'
  GROUP BY ta.team_id, ta.slot_id, ta.applicant_user_id
  HAVING COUNT(*) > 1
) issue
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'duplicate_pending_invitation', COUNT(*)
FROM (
  SELECT ti.team_id, ti.slot_id, ti.target_user_id
  FROM team_invitation ti
  JOIN team t ON t.team_id = ti.team_id
  WHERE t.name LIKE '[CDV]%'
    AND ti.status = 'PENDING'
  GROUP BY ti.team_id, ti.slot_id, ti.target_user_id
  HAVING COUNT(*) > 1
) issue
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cdv_generated_image_path_missing_or_invalid', COUNT(*)
FROM (
  SELECT mp.profile_image_path AS image_path
  FROM member_profile mp
  JOIN user_account ua ON ua.user_id = mp.user_id
  WHERE ua.login_id LIKE 'cdv-user-%'
  UNION ALL
  SELECT representative_image_path FROM team WHERE name LIKE '[CDV]%'
  UNION ALL
  SELECT representative_image_path FROM work_item WHERE title LIKE '[CDV]%'
  UNION ALL
  SELECT thumbnail_image_path FROM portfolio_item WHERE external_source_name = 'SLATE_CDV'
  UNION ALL
  SELECT representative_image_path FROM contest WHERE title LIKE '[CDV]%'
  UNION ALL
  SELECT representative_image_path FROM contest_open_request WHERE title LIKE '[CDV]%'
) paths
WHERE image_path IS NULL
   OR image_path NOT LIKE 'images/seed/%.png'
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cdv_external_media_or_verification_should_be_empty', COUNT(*)
FROM (
  SELECT pi.portfolio_item_id AS entity_id
  FROM portfolio_item pi
  WHERE pi.external_source_name = 'SLATE_CDV'
    AND (pi.url IS NOT NULL OR pi.thumbnail_url IS NOT NULL)
  UNION ALL
  SELECT pv.verification_id
  FROM portfolio_verification pv
  JOIN portfolio_item pi ON pi.portfolio_item_id = pv.portfolio_item_id
  WHERE pi.external_source_name = 'SLATE_CDV'
  UNION ALL
  SELECT c.contest_id
  FROM contest c
  WHERE c.title LIKE '[CDV]%'
    AND (c.external_url IS NOT NULL OR c.representative_image_url IS NOT NULL)
) issue
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'notification_bad_profile_target', COUNT(*)
FROM notification n
LEFT JOIN member_profile mp ON mp.profile_id = n.target_id
WHERE n.title LIKE '[CDV]%'
  AND n.target_type = 'PROFILE'
  AND mp.profile_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'notification_bad_team_target', COUNT(*)
FROM notification n
LEFT JOIN team t ON t.team_id = n.target_id
WHERE n.title LIKE '[CDV]%'
  AND n.target_type = 'TEAM'
  AND t.team_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'notification_bad_contest_target', COUNT(*)
FROM notification n
LEFT JOIN contest c ON c.contest_id = n.target_id
WHERE n.title LIKE '[CDV]%'
  AND n.target_type = 'CONTEST'
  AND c.contest_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'notification_bad_board_target', COUNT(*)
FROM notification n
LEFT JOIN board_post bp ON bp.post_id = n.target_id
WHERE n.title LIKE '[CDV]%'
  AND n.target_type = 'BOARD_POST'
  AND bp.post_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'accepted_report_target_not_moderated', COUNT(*)
FROM content_report cr
JOIN user_account reporter ON reporter.user_id = cr.reporter_user_id
LEFT JOIN board_post bp
  ON cr.target_type = 'BOARD_POST'
 AND bp.post_id = cr.target_id
LEFT JOIN board_review br
  ON cr.target_type = 'BOARD_REVIEW'
 AND br.review_id = cr.target_id
WHERE reporter.login_id LIKE 'cdv-%'
  AND cr.status = 'ACCEPTED'
  AND (
    (cr.moderation_action = 'BLIND_POST' AND COALESCE(bp.status, '') <> 'BLINDED')
    OR (cr.moderation_action = 'BLIND_REVIEW' AND COALESCE(br.status, '') <> 'BLINDED')
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'accepted_report_missing_sanction', COUNT(*)
FROM content_report cr
JOIN user_account reporter ON reporter.user_id = cr.reporter_user_id
LEFT JOIN board_post bp
  ON cr.target_type = 'BOARD_POST'
 AND bp.post_id = cr.target_id
LEFT JOIN board_review br
  ON cr.target_type = 'BOARD_REVIEW'
 AND br.review_id = cr.target_id
LEFT JOIN user_sanction us
  ON us.user_id = COALESCE(bp.author_user_id, br.author_user_id)
 AND us.reason LIKE '[CDV]%'
WHERE reporter.login_id LIKE 'cdv-%'
  AND cr.status = 'ACCEPTED'
  AND us.sanction_id IS NULL
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'sanction_account_status_mismatch', COUNT(*)
FROM user_sanction us
JOIN user_account ua ON ua.user_id = us.user_id
WHERE ua.login_id LIKE 'cdv-%'
  AND (
    (us.status = 'ACTIVE' AND us.sanction_type = 'TEMP_SUSPENDED' AND ua.account_status <> 'TEMP_SUSPENDED')
    OR (us.status = 'ACTIVE' AND us.sanction_type = 'PERM_SUSPENDED' AND ua.account_status <> 'PERM_SUSPENDED')
    OR (us.status IN ('REVOKED', 'EXPIRED') AND ua.account_status <> 'ACTIVE')
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cross_namespace_matching_target', COUNT(*)
FROM matching_bookmark mb
JOIN user_account ua ON ua.user_id = mb.user_id
LEFT JOIN team t
  ON mb.target_type = 'TEAM'
 AND t.team_id = mb.target_id
LEFT JOIN member_profile mp
  ON mb.target_type = 'PROFILE'
 AND mp.profile_id = mb.target_id
LEFT JOIN user_account target_user ON target_user.user_id = mp.user_id
WHERE ua.login_id LIKE 'cdv-%'
  AND (
    (mb.target_type = 'TEAM' AND (t.team_id IS NULL OR t.name NOT LIKE '[CDV]%'))
    OR (mb.target_type = 'PROFILE' AND (target_user.user_id IS NULL OR target_user.login_id NOT LIKE 'cdv-%'))
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cross_namespace_team_user', COUNT(*)
FROM team_member tm
JOIN team t ON t.team_id = tm.team_id
JOIN user_account ua ON ua.user_id = tm.user_id
WHERE t.name LIKE '[CDV]%'
  AND ua.login_id NOT LIKE 'cdv-%'
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cross_namespace_contest_relation', COUNT(*)
FROM contest_submission_prepare csp
JOIN contest c ON c.contest_id = csp.contest_id
JOIN user_account ua ON ua.user_id = csp.user_id
LEFT JOIN team t
  ON csp.basis_type = 'TEAM'
 AND t.team_id = csp.basis_id
LEFT JOIN member_profile mp
  ON csp.basis_type = 'PROFILE'
 AND mp.profile_id = csp.basis_id
LEFT JOIN user_account profile_user ON profile_user.user_id = mp.user_id
WHERE c.title LIKE '[CDV]%'
  AND (
    ua.login_id NOT LIKE 'cdv-%'
    OR (csp.basis_type = 'TEAM' AND (t.team_id IS NULL OR t.name NOT LIKE '[CDV]%'))
    OR (csp.basis_type = 'PROFILE' AND (profile_user.user_id IS NULL OR profile_user.login_id NOT LIKE 'cdv-%'))
  )
UNION ALL
SELECT 'CDV_ZERO_ERROR_CHECKS', 'cdd_baseline_count_mismatch',
  CASE WHEN
    (SELECT COUNT(*) FROM user_account WHERE login_id LIKE 'cdd-%') <> 9
    OR (
      SELECT COUNT(*)
      FROM member_profile mp
      JOIN user_account ua ON ua.user_id = mp.user_id
      WHERE ua.login_id LIKE 'cdd-%'
    ) <> 8
    OR (SELECT COUNT(*) FROM team WHERE name LIKE '[CDD]%') <> 2
    OR (
      SELECT COUNT(*)
      FROM team_application ta
      JOIN team t ON t.team_id = ta.team_id
      WHERE t.name LIKE '[CDD]%'
    ) <> 5
    OR (
      SELECT COUNT(*)
      FROM team_invitation ti
      JOIN team t ON t.team_id = ti.team_id
      WHERE t.name LIKE '[CDD]%'
    ) <> 4
    OR (SELECT COUNT(*) FROM board_post WHERE title LIKE '[CDD]%') <> 3
    OR (SELECT COUNT(*) FROM work_item WHERE title LIKE '[CDD]%') <> 1
    OR (SELECT COUNT(*) FROM portfolio_item WHERE external_source_name = 'SLATE_CDD') <> 2
    OR (SELECT COUNT(*) FROM contest WHERE title LIKE '[CDD]%') <> 1
    OR (SELECT COUNT(*) FROM notification WHERE title LIKE '[CDD]%') <> 7
  THEN 1 ELSE 0 END;

SELECT 'CDD_GUARD_COUNTS' AS section, 'user_account' AS item, COUNT(*) AS actual_count, 9 AS expected_count
FROM user_account
WHERE login_id LIKE 'cdd-%'
UNION ALL
SELECT 'CDD_GUARD_COUNTS', 'member_profile', COUNT(*), 8
FROM member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id LIKE 'cdd-%'
UNION ALL
SELECT 'CDD_GUARD_COUNTS', 'team', COUNT(*), 2
FROM team
WHERE name LIKE '[CDD]%'
UNION ALL
SELECT 'CDD_GUARD_COUNTS', 'board_post', COUNT(*), 3
FROM board_post
WHERE title LIKE '[CDD]%'
UNION ALL
SELECT 'CDD_GUARD_COUNTS', 'work_item', COUNT(*), 1
FROM work_item
WHERE title LIKE '[CDD]%'
UNION ALL
SELECT 'CDD_GUARD_COUNTS', 'portfolio_item', COUNT(*), 2
FROM portfolio_item
WHERE external_source_name = 'SLATE_CDD'
UNION ALL
SELECT 'CDD_GUARD_COUNTS', 'contest', COUNT(*), 1
FROM contest
WHERE title LIKE '[CDD]%'
UNION ALL
SELECT 'CDD_GUARD_COUNTS', 'notification', COUNT(*), 7
FROM notification
WHERE title LIKE '[CDD]%';
