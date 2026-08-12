SET NAMES utf8mb4;
USE slate;

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
SET @cdd_sanction := (SELECT sanction_id FROM user_sanction WHERE user_id = @cdd_moderated_user AND reason = '[CDD] 신고 승인에 따른 임시 이용 제한 데모입니다.' ORDER BY sanction_id LIMIT 1);

SELECT 'CDD_EXPECTED_COUNTS' AS section, 'user_account' AS item, COUNT(*) AS actual_count, 9 AS expected_count
FROM user_account
WHERE login_id IN ('cdd-leader', 'cdd-camera', 'cdd-sound', 'cdd-editor', 'cdd-writer', 'cdd-actor', 'cdd-reporter', 'cdd-moderated', 'cdd-company')
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'member_profile', COUNT(*), 8
FROM member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id LIKE 'cdd-%' AND ua.account_type = 'USER'
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'team', COUNT(*), 2
FROM team
WHERE name IN ('[CDD] 한강 야간 단편팀', '[CDD] 완료된 포트폴리오팀')
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'team_application', COUNT(*), 5
FROM team_application
WHERE team_id IN (@cdd_river_team, @cdd_closed_team)
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'team_invitation', COUNT(*), 4
FROM team_invitation
WHERE team_id IN (@cdd_river_team, @cdd_closed_team)
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'team_work_approval_request', COUNT(*), 4
FROM team_work_approval_request
WHERE team_id = @cdd_river_team
  AND title LIKE '[CDD]%'
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'contest_open_request_APPROVED', COUNT(*), 1
FROM contest_open_request
WHERE request_id = @cdd_contest_request
  AND status = 'APPROVED'
  AND approved_contest_id = @cdd_contest
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'contest_save', COUNT(*), 3
FROM contest_save
WHERE contest_id = @cdd_contest
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'contest_submission_prepare', COUNT(*), 2
FROM contest_submission_prepare
WHERE contest_id = @cdd_contest
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'content_report_ACCEPTED', COUNT(*), 1
FROM content_report
WHERE target_type = 'BOARD_POST'
  AND target_id = @cdd_moderation_post
  AND status = 'ACCEPTED'
UNION ALL
SELECT 'CDD_EXPECTED_COUNTS', 'active_sanction', COUNT(*), 1
FROM user_sanction
WHERE sanction_id = @cdd_sanction
  AND status = 'ACTIVE';

SELECT 'CDD_STATUS_COUNTS' AS section, 'team_application' AS table_name, status, COUNT(*) AS row_count
FROM team_application
WHERE team_id IN (@cdd_river_team, @cdd_closed_team)
GROUP BY status
UNION ALL
SELECT 'CDD_STATUS_COUNTS', 'team_invitation', status, COUNT(*)
FROM team_invitation
WHERE team_id IN (@cdd_river_team, @cdd_closed_team)
GROUP BY status
UNION ALL
SELECT 'CDD_STATUS_COUNTS', 'team_plan_item', status, COUNT(*)
FROM team_plan_item
WHERE team_id IN (@cdd_river_team, @cdd_closed_team)
GROUP BY status
UNION ALL
SELECT 'CDD_STATUS_COUNTS', 'team_work_approval_request', status, COUNT(*)
FROM team_work_approval_request
WHERE team_id = @cdd_river_team
  AND title LIKE '[CDD]%'
GROUP BY status
ORDER BY table_name, status;

SELECT 'CDD_ZERO_ERROR_CHECKS' AS section, 'missing_required_cdd_ids' AS check_name,
  CASE
    WHEN @cdd_river_team IS NULL
      OR @cdd_closed_team IS NULL
      OR @cdd_work_post IS NULL
      OR @cdd_work_item IS NULL
      OR @cdd_contest IS NULL
      OR @cdd_contest_request IS NULL
      OR @cdd_sanction IS NULL
    THEN 1 ELSE 0
  END AS issue_count
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'team_leader_active_mismatch', COUNT(*)
FROM team t
LEFT JOIN team_member tm
  ON tm.team_id = t.team_id
 AND tm.user_id = t.leader_user_id
 AND tm.team_role = 'LEADER'
 AND tm.status = 'ACTIVE'
WHERE t.team_id IN (@cdd_river_team, @cdd_closed_team)
  AND tm.team_member_id IS NULL
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'active_leader_count_not_one', COUNT(*)
FROM (
  SELECT t.team_id
  FROM team t
  LEFT JOIN team_member tm
    ON tm.team_id = t.team_id
   AND tm.team_role = 'LEADER'
   AND tm.status = 'ACTIVE'
  WHERE t.team_id IN (@cdd_river_team, @cdd_closed_team)
  GROUP BY t.team_id
  HAVING COUNT(tm.team_member_id) <> 1
) x
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'team_current_member_count_mismatch', COUNT(*)
FROM team t
WHERE t.team_id IN (@cdd_river_team, @cdd_closed_team)
  AND t.current_member_count <> (
    SELECT COUNT(*)
    FROM team_member tm
    WHERE tm.team_id = t.team_id
      AND tm.status = 'ACTIVE'
  )
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'slot_accepted_count_mismatch', COUNT(*)
FROM team_recruitment_slot s
JOIN team_recruitment tr ON tr.recruitment_id = s.recruitment_id
WHERE tr.team_id IN (@cdd_river_team, @cdd_closed_team)
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
SELECT 'CDD_ZERO_ERROR_CHECKS', 'slot_accepted_count_over_required', COUNT(*)
FROM team_recruitment_slot s
JOIN team_recruitment tr ON tr.recruitment_id = s.recruitment_id
WHERE tr.team_id IN (@cdd_river_team, @cdd_closed_team)
  AND s.accepted_count > s.required_count
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'accepted_application_missing_active_member', COUNT(*)
FROM team_application ta
LEFT JOIN team_member tm
  ON tm.team_id = ta.team_id
 AND tm.user_id = ta.applicant_user_id
 AND tm.status = 'ACTIVE'
WHERE ta.team_id IN (@cdd_river_team, @cdd_closed_team)
  AND ta.status = 'ACCEPTED'
  AND tm.team_member_id IS NULL
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'accepted_invitation_missing_active_member', COUNT(*)
FROM team_invitation ti
LEFT JOIN team_member tm
  ON tm.team_id = ti.team_id
 AND tm.user_id = ti.target_user_id
 AND tm.status = 'ACTIVE'
WHERE ti.team_id IN (@cdd_river_team, @cdd_closed_team)
  AND ti.status = 'ACCEPTED'
  AND tm.team_member_id IS NULL
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'ended_team_missing_snapshot', COUNT(*)
FROM team t
LEFT JOIN team_closure_snapshot s ON s.team_id = t.team_id
WHERE t.team_id = @cdd_closed_team
  AND t.status = 'ENDED'
  AND s.closure_snapshot_id IS NULL
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'ended_team_open_recruitment_or_slot', COUNT(*)
FROM team t
LEFT JOIN team_recruitment tr
  ON tr.team_id = t.team_id
 AND tr.status = 'OPEN'
LEFT JOIN team_recruitment_slot s
  ON s.recruitment_id = tr.recruitment_id
 AND s.status = 'OPEN'
WHERE t.team_id = @cdd_closed_team
  AND t.status = 'ENDED'
  AND (tr.recruitment_id IS NOT NULL OR s.slot_id IS NOT NULL)
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'board_like_count_mismatch', COUNT(*)
FROM board_post p
WHERE p.post_id IN (@cdd_work_post, @cdd_info_post, @cdd_moderation_post)
  AND p.like_count <> (
    SELECT COUNT(*)
    FROM board_like bl
    WHERE bl.post_id = p.post_id
      AND bl.active_yn = 'Y'
  )
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'board_review_count_mismatch', COUNT(*)
FROM board_post p
WHERE p.post_id IN (@cdd_work_post, @cdd_info_post, @cdd_moderation_post)
  AND p.review_count <> (
    SELECT COUNT(*)
    FROM board_review br
    WHERE br.post_id = p.post_id
      AND br.status = 'PUBLISHED'
  )
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'work_post_link_mismatch', COUNT(*)
FROM work_item wi
JOIN board_post bp ON bp.post_id = wi.board_post_id
WHERE wi.work_id = @cdd_work_item
  AND (bp.category <> 'WORK' OR bp.post_id <> @cdd_work_post)
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'approved_work_request_unlinked', COUNT(*)
FROM team_work_approval_request twr
WHERE twr.team_id = @cdd_river_team
  AND twr.title = '[CDD] 한강 야간 컷 공개 승인'
  AND twr.status = 'APPROVED'
  AND (twr.board_post_id <> @cdd_work_post OR twr.work_id <> @cdd_work_item)
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'team_work_request_requester_not_active_member', COUNT(*)
FROM team_work_approval_request twr
LEFT JOIN team_member tm
  ON tm.team_id = twr.team_id
 AND tm.user_id = twr.requester_user_id
 AND tm.status = 'ACTIVE'
WHERE twr.team_id IN (@cdd_river_team, @cdd_closed_team)
  AND twr.title LIKE '[CDD]%'
  AND tm.team_member_id IS NULL
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'contest_save_count_mismatch', COUNT(*)
FROM contest c
WHERE c.contest_id = @cdd_contest
  AND c.save_count <> (
    SELECT COUNT(*)
    FROM contest_save cs
    WHERE cs.contest_id = c.contest_id
  )
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'approved_contest_request_link_mismatch', COUNT(*)
FROM contest_open_request cor
LEFT JOIN contest c ON c.contest_id = cor.approved_contest_id
WHERE cor.request_id = @cdd_contest_request
  AND (
    cor.status <> 'APPROVED'
    OR cor.approved_contest_id <> @cdd_contest
    OR c.source_request_id <> cor.request_id
  )
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'self_follow', COUNT(*)
FROM user_follow uf
JOIN user_account u ON u.user_id = uf.follower_user_id
WHERE u.login_id LIKE 'cdd-%'
  AND uf.follower_user_id = uf.following_user_id
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'cdd_generated_image_path_missing_or_invalid', COUNT(*)
FROM (
  SELECT profile_image_path AS image_path
  FROM member_profile mp
  JOIN user_account ua ON ua.user_id = mp.user_id
  WHERE ua.login_id IN ('cdd-leader', 'cdd-camera', 'cdd-sound', 'cdd-editor', 'cdd-writer', 'cdd-actor', 'cdd-reporter', 'cdd-moderated')
  UNION ALL
  SELECT representative_image_path FROM team WHERE team_id IN (@cdd_river_team, @cdd_closed_team)
  UNION ALL
  SELECT representative_image_path FROM work_item WHERE work_id = @cdd_work_item
  UNION ALL
  SELECT thumbnail_image_path FROM portfolio_item WHERE external_source_name = 'SLATE_CDD'
  UNION ALL
  SELECT representative_image_path FROM contest WHERE contest_id = @cdd_contest
  UNION ALL
  SELECT representative_image_path FROM contest_open_request WHERE request_id = @cdd_contest_request
) paths
WHERE image_path IS NULL
   OR image_path NOT LIKE 'images/seed/%.png'
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'notification_bad_team_target', COUNT(*)
FROM notification n
LEFT JOIN team t ON t.team_id = n.target_id
WHERE n.title LIKE '[CDD]%'
  AND n.target_type = 'TEAM'
  AND t.team_id IS NULL
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'notification_bad_profile_target', COUNT(*)
FROM notification n
LEFT JOIN member_profile p ON p.profile_id = n.target_id
WHERE n.title LIKE '[CDD]%'
  AND n.target_type = 'PROFILE'
  AND p.profile_id IS NULL
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'notification_bad_contest_target', COUNT(*)
FROM notification n
LEFT JOIN contest c ON c.contest_id = n.target_id
WHERE n.title LIKE '[CDD]%'
  AND n.target_type = 'CONTEST'
  AND c.contest_id IS NULL
UNION ALL
SELECT 'CDD_ZERO_ERROR_CHECKS', 'notification_bad_sanction_target', COUNT(*)
FROM notification n
LEFT JOIN user_sanction us ON us.sanction_id = n.target_id
WHERE n.title LIKE '[CDD]%'
  AND n.target_type = 'USER_SANCTION'
  AND us.sanction_id IS NULL;

SELECT 'GLOBAL_EXISTING_DATA_WARNINGS' AS section, 'existing_upload_paths_without_file_manifest_check' AS check_name, COUNT(*) AS row_count
FROM (
  SELECT profile_image_path AS image_path FROM member_profile WHERE profile_image_path IS NOT NULL
  UNION ALL SELECT representative_image_path FROM team WHERE representative_image_path IS NOT NULL
  UNION ALL SELECT representative_image_path FROM work_item WHERE representative_image_path IS NOT NULL
  UNION ALL SELECT thumbnail_image_path FROM portfolio_item WHERE thumbnail_image_path IS NOT NULL
  UNION ALL SELECT representative_image_path FROM contest WHERE representative_image_path IS NOT NULL
  UNION ALL SELECT representative_image_path FROM contest_open_request WHERE representative_image_path IS NOT NULL
  UNION ALL SELECT stored_path FROM file_metadata WHERE stored_path IS NOT NULL
  UNION ALL SELECT stored_path FROM company_application_document WHERE stored_path IS NOT NULL
) paths
UNION ALL
SELECT 'GLOBAL_EXISTING_DATA_WARNINGS', 'example_test_url_rows', COUNT(*)
FROM (
  SELECT url AS value FROM portfolio_item WHERE url LIKE '%example.test%'
  UNION ALL SELECT provider_url FROM public_data_sync_item WHERE provider_url LIKE '%example.test%'
  UNION ALL SELECT external_url FROM contest WHERE external_url LIKE '%example.test%'
  UNION ALL SELECT representative_image_url FROM contest WHERE representative_image_url LIKE '%example.test%'
) urls;
