SET NAMES utf8mb4;

SET @verified_badge_demo_login_id = COALESCE(@verified_badge_demo_login_id, 'leader');

-- UI/API fixture only. This does not represent a successful live KOBIS verification.
INSERT INTO portfolio_item (
  profile_id,
  title,
  role_name,
  credit_name,
  description,
  source_type,
  external_source_name,
  external_reference_id,
  url,
  thumbnail_url,
  sort_order,
  status
)
SELECT
  mp.profile_id,
  'Verified 배지 UI 검증용 더미 작품',
  '프로듀서',
  '도윤 PD',
  'KOBIS API 키 없이 Verified 배지의 API 응답과 화면 표시를 확인하기 위한 로컬 데모 데이터입니다.',
  'PUBLIC_DATA_MANUAL',
  'KOBIS_UI_FIXTURE',
  'SLATE-DEMO-VERIFIED-001',
  NULL,
  NULL,
  0,
  'ACTIVE'
FROM member_profile mp
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id = @verified_badge_demo_login_id
  AND NOT EXISTS (
    SELECT 1
    FROM portfolio_item pi
    WHERE pi.profile_id = mp.profile_id
      AND pi.external_source_name = 'KOBIS_UI_FIXTURE'
      AND pi.external_reference_id = 'SLATE-DEMO-VERIFIED-001'
      AND pi.status = 'ACTIVE'
  );

INSERT INTO portfolio_verification (
  portfolio_item_id,
  provider,
  provider_movie_code,
  provider_movie_title,
  provider_movie_year,
  provider_person_name,
  provider_role_name,
  matched_role_group,
  matched_source,
  verification_status,
  raw_response_json,
  checked_at
)
SELECT
  pi.portfolio_item_id,
  'KOBIS_UI_FIXTURE',
  'SLATE-DEMO-VERIFIED-001',
  pi.title,
  '2026',
  '도윤 PD',
  pi.role_name,
  'PRODUCER',
  'UI_FIXTURE',
  'VERIFIED',
  JSON_OBJECT('fixture', TRUE, 'purpose', 'verified badge UI verification'),
  NOW()
FROM portfolio_item pi
JOIN member_profile mp ON mp.profile_id = pi.profile_id
JOIN user_account ua ON ua.user_id = mp.user_id
WHERE ua.login_id = @verified_badge_demo_login_id
  AND pi.external_source_name = 'KOBIS_UI_FIXTURE'
  AND pi.external_reference_id = 'SLATE-DEMO-VERIFIED-001'
  AND pi.status = 'ACTIVE'
ON DUPLICATE KEY UPDATE
  provider = VALUES(provider),
  provider_movie_code = VALUES(provider_movie_code),
  provider_movie_title = VALUES(provider_movie_title),
  provider_movie_year = VALUES(provider_movie_year),
  provider_person_name = VALUES(provider_person_name),
  provider_role_name = VALUES(provider_role_name),
  matched_role_group = VALUES(matched_role_group),
  matched_source = VALUES(matched_source),
  verification_status = VALUES(verification_status),
  raw_response_json = VALUES(raw_response_json),
  checked_at = VALUES(checked_at),
  updated_at = NOW();
