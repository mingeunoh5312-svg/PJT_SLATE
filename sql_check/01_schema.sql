SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS common_code_group (
  code_group varchar(50) NOT NULL,
  group_name varchar(100) NOT NULL,
  description varchar(255) NULL,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (code_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS common_code (
  code_group varchar(50) NOT NULL,
  code varchar(50) NOT NULL,
  display_name varchar(100) NOT NULL,
  description varchar(255) NULL,
  sort_order int NOT NULL DEFAULT 0,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (code_group, code),
  CONSTRAINT fk_common_code_group FOREIGN KEY (code_group) REFERENCES common_code_group (code_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS region (
  region_id bigint NOT NULL AUTO_INCREMENT,
  region_code varchar(20) NOT NULL,
  sido_name varchar(50) NOT NULL,
  sigungu_name varchar(80) NOT NULL,
  dong_name varchar(80) NOT NULL,
  center_lat decimal(10,7) NOT NULL,
  center_lng decimal(10,7) NOT NULL,
  public_display_name varchar(150) NOT NULL,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (region_id),
  UNIQUE KEY uk_region_code (region_code),
  KEY idx_region_public (public_display_name),
  KEY idx_region_coord (center_lat, center_lng)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS role_category (
  role_category_id bigint NOT NULL AUTO_INCREMENT,
  name varchar(80) NOT NULL,
  sort_order int NOT NULL DEFAULT 0,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (role_category_id),
  UNIQUE KEY uk_role_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `role` (
  role_id bigint NOT NULL AUTO_INCREMENT,
  role_category_id bigint NOT NULL,
  name varchar(80) NOT NULL,
  sort_order int NOT NULL DEFAULT 0,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (role_id),
  KEY idx_role_category (role_category_id),
  KEY idx_role_name (name),
  CONSTRAINT fk_role_category FOREIGN KEY (role_category_id) REFERENCES role_category (role_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS genre (
  genre_id bigint NOT NULL AUTO_INCREMENT,
  name varchar(50) NOT NULL,
  description varchar(255) NULL,
  sort_order int NOT NULL DEFAULT 0,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (genre_id),
  UNIQUE KEY uk_genre_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_account (
  user_id bigint NOT NULL AUTO_INCREMENT,
  login_id varchar(50) NOT NULL,
  email varchar(255) NOT NULL,
  password_hash varchar(255) NOT NULL,
  nickname varchar(50) NOT NULL,
  phone varchar(30) NULL,
  account_type varchar(30) NOT NULL,
  account_status varchar(30) NOT NULL,
  last_login_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deactivated_at datetime NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_login_id (login_id),
  UNIQUE KEY uk_user_email (email),
  KEY idx_user_account_type_status (account_type, account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_follow (
  follower_user_id bigint NOT NULL,
  following_user_id bigint NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (follower_user_id, following_user_id),
  KEY idx_user_follow_follower_created (follower_user_id, created_at DESC, following_user_id),
  KEY idx_user_follow_following_created (following_user_id, created_at DESC, follower_user_id),
  CONSTRAINT chk_user_follow_not_self CHECK (follower_user_id <> following_user_id),
  CONSTRAINT fk_user_follow_follower FOREIGN KEY (follower_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_user_follow_following FOREIGN KEY (following_user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS company_application (
  company_application_id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  company_name varchar(120) NOT NULL,
  business_registration_no varchar(30) NOT NULL,
  manager_name varchar(50) NOT NULL,
  manager_phone varchar(30) NOT NULL,
  company_intro varchar(1000) NOT NULL,
  public_data_company_name varchar(120) NULL,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  review_reason varchar(500) NULL,
  reviewed_by bigint NULL,
  reviewed_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (company_application_id),
  UNIQUE KEY uk_company_application_user (user_id),
  KEY idx_company_application_status (status, created_at),
  CONSTRAINT fk_company_application_user FOREIGN KEY (user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_company_application_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS company_application_document (
  document_id bigint NOT NULL AUTO_INCREMENT,
  company_application_id bigint NOT NULL,
  uploader_user_id bigint NOT NULL,
  document_type varchar(50) NOT NULL,
  original_name varchar(255) NOT NULL,
  stored_path varchar(500) NOT NULL,
  content_type varchar(100) NOT NULL,
  size_bytes bigint NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  uploaded_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at datetime NULL,
  PRIMARY KEY (document_id),
  KEY idx_company_document_application (company_application_id, status, uploaded_at),
  KEY idx_company_document_uploader (uploader_user_id, uploaded_at),
  CONSTRAINT fk_company_document_application FOREIGN KEY (company_application_id) REFERENCES company_application (company_application_id),
  CONSTRAINT fk_company_document_uploader FOREIGN KEY (uploader_user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS admin_permission (
  admin_permission_id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  permission_code varchar(50) NOT NULL,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  granted_by bigint NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (admin_permission_id),
  UNIQUE KEY uk_admin_permission (user_id, permission_code),
  CONSTRAINT fk_admin_permission_user FOREIGN KEY (user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_admin_permission_granter FOREIGN KEY (granted_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS demo_access_code (
  code_id bigint NOT NULL AUTO_INCREMENT,
  label varchar(100) NOT NULL,
  code_hash varchar(255) NOT NULL,
  code_fingerprint varchar(128) NULL,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  starts_at datetime NULL,
  expires_at datetime NOT NULL,
  max_uses int NULL,
  used_count int NOT NULL DEFAULT 0,
  last_used_at datetime NULL,
  created_by bigint NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by bigint NULL,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  revoked_by bigint NULL,
  revoked_at datetime NULL,
  revoke_reason varchar(500) NULL,
  PRIMARY KEY (code_id),
  KEY idx_demo_access_code_status_dates (status, starts_at, expires_at),
  KEY idx_demo_access_code_created (created_at),
  KEY idx_demo_access_code_fingerprint (code_fingerprint),
  CONSTRAINT fk_demo_access_code_creator FOREIGN KEY (created_by) REFERENCES user_account (user_id),
  CONSTRAINT fk_demo_access_code_updater FOREIGN KEY (updated_by) REFERENCES user_account (user_id),
  CONSTRAINT fk_demo_access_code_revoker FOREIGN KEY (revoked_by) REFERENCES user_account (user_id),
  CONSTRAINT chk_demo_access_code_used_count CHECK (used_count >= 0),
  CONSTRAINT chk_demo_access_code_max_uses CHECK (max_uses IS NULL OR max_uses >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS member_profile (
  profile_id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  display_name varchar(50) NOT NULL,
  short_intro varchar(120) NOT NULL,
  detail_intro text NULL,
  profile_image_path varchar(500) NULL,
  visibility varchar(30) NOT NULL,
  activity_status varchar(30) NOT NULL,
  region_id bigint NOT NULL,
  experience_level varchar(30) NOT NULL,
  join_availability varchar(30) NOT NULL,
  collaboration_status varchar(30) NOT NULL,
  travel_range varchar(30) NOT NULL,
  preferred_duration varchar(30) NOT NULL,
  equipment_status varchar(30) NULL,
  age_band varchar(30) NULL,
  participation_mode varchar(30) NULL,
  profile_completed_yn char(1) NOT NULL DEFAULT 'N',
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  last_active_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted_at datetime NULL,
  PRIMARY KEY (profile_id),
  UNIQUE KEY uk_member_profile_user (user_id),
  KEY idx_profile_status (status, deleted_at),
  KEY idx_profile_matching (activity_status, profile_completed_yn, collaboration_status),
  KEY idx_profile_region (region_id, travel_range),
  KEY idx_profile_experience (experience_level),
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_profile_region FOREIGN KEY (region_id) REFERENCES region (region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS profile_role (
  profile_id bigint NOT NULL,
  role_id bigint NOT NULL,
  sort_order int NOT NULL DEFAULT 0,
  PRIMARY KEY (profile_id, role_id),
  KEY idx_profile_role_role (role_id, profile_id),
  CONSTRAINT fk_profile_role_profile FOREIGN KEY (profile_id) REFERENCES member_profile (profile_id),
  CONSTRAINT fk_profile_role_role FOREIGN KEY (role_id) REFERENCES `role` (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS profile_genre (
  profile_id bigint NOT NULL,
  genre_id bigint NOT NULL,
  PRIMARY KEY (profile_id, genre_id),
  KEY idx_profile_genre_genre (genre_id, profile_id),
  CONSTRAINT fk_profile_genre_profile FOREIGN KEY (profile_id) REFERENCES member_profile (profile_id),
  CONSTRAINT fk_profile_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS profile_collaboration_condition (
  profile_id bigint NOT NULL,
  condition_code varchar(30) NOT NULL,
  PRIMARY KEY (profile_id, condition_code),
  CONSTRAINT fk_profile_condition_profile FOREIGN KEY (profile_id) REFERENCES member_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS portfolio_item (
  portfolio_item_id bigint NOT NULL AUTO_INCREMENT,
  profile_id bigint NOT NULL,
  public_data_sync_item_id bigint NULL,
  title varchar(150) NOT NULL,
  role_name varchar(80) NULL,
  credit_name varchar(120) NULL,
  description varchar(1000) NULL,
  source_type varchar(30) NOT NULL DEFAULT 'MANUAL',
  external_source_name varchar(80) NULL,
  external_reference_id varchar(100) NULL,
  url varchar(500) NULL,
  thumbnail_url varchar(500) NULL,
  thumbnail_image_path varchar(500) NULL,
  sort_order int NOT NULL DEFAULT 0,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted_at datetime NULL,
  PRIMARY KEY (portfolio_item_id),
  KEY idx_portfolio_profile (profile_id, status, sort_order, created_at),
  KEY idx_portfolio_public_data (public_data_sync_item_id),
  CONSTRAINT fk_portfolio_profile FOREIGN KEY (profile_id) REFERENCES member_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS portfolio_verification (
  verification_id bigint NOT NULL AUTO_INCREMENT,
  portfolio_item_id bigint NOT NULL,
  provider varchar(30) NOT NULL DEFAULT 'KOBIS',
  provider_movie_code varchar(50) NOT NULL,
  provider_movie_title varchar(200) NULL,
  provider_movie_title_en varchar(200) NULL,
  provider_movie_year varchar(20) NULL,
  provider_open_date varchar(20) NULL,
  provider_genres varchar(300) NULL,
  provider_person_name varchar(120) NULL,
  provider_person_name_en varchar(120) NULL,
  provider_role_name varchar(120) NULL,
  matched_role_group varchar(80) NULL,
  matched_source varchar(30) NULL,
  verification_status varchar(30) NOT NULL,
  raw_response_json json NULL,
  checked_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (verification_id),
  UNIQUE KEY uk_portfolio_verification_item (portfolio_item_id),
  KEY idx_portfolio_verification_status (verification_status, checked_at),
  KEY idx_portfolio_verification_provider_movie (provider, provider_movie_code),
  CONSTRAINT fk_portfolio_verification_item FOREIGN KEY (portfolio_item_id) REFERENCES portfolio_item (portfolio_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS public_data_sync_item (
  public_data_sync_item_id bigint NOT NULL AUTO_INCREMENT,
  source_name varchar(80) NOT NULL,
  item_type varchar(30) NOT NULL,
  external_id varchar(100) NOT NULL,
  title varchar(200) NOT NULL,
  description varchar(1000) NULL,
  provider_url varchar(500) NULL,
  display_year varchar(20) NULL,
  creator_name varchar(120) NULL,
  raw_json json NULL,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  synced_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (public_data_sync_item_id),
  UNIQUE KEY uk_public_data_item (source_name, item_type, external_id),
  KEY idx_public_data_search (item_type, title),
  KEY idx_public_data_creator (creator_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team (
  team_id bigint NOT NULL AUTO_INCREMENT,
  leader_user_id bigint NOT NULL,
  name varchar(100) NOT NULL,
  description text NOT NULL,
  representative_image_path varchar(500) NULL,
  status varchar(30) NOT NULL,
  end_type varchar(30) NULL,
  region_id bigint NULL,
  region_any_yn char(1) NOT NULL DEFAULT 'N',
  expected_duration varchar(30) NOT NULL,
  max_member_count int NOT NULL DEFAULT 100,
  current_member_count int NOT NULL DEFAULT 1,
  recruitment_reopen_count int NOT NULL DEFAULT 0,
  last_active_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (team_id),
  KEY idx_team_status (status),
  KEY idx_team_region (region_id, region_any_yn),
  KEY idx_team_leader (leader_user_id),
  CONSTRAINT fk_team_leader FOREIGN KEY (leader_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_team_region FOREIGN KEY (region_id) REFERENCES region (region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_genre (
  team_id bigint NOT NULL,
  genre_id bigint NOT NULL,
  PRIMARY KEY (team_id, genre_id),
  KEY idx_team_genre_genre (genre_id, team_id),
  CONSTRAINT fk_team_genre_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_team_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_member (
  team_member_id bigint NOT NULL AUTO_INCREMENT,
  team_id bigint NOT NULL,
  user_id bigint NOT NULL,
  team_role varchar(30) NOT NULL,
  status varchar(30) NOT NULL,
  joined_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at datetime NULL,
  PRIMARY KEY (team_member_id),
  UNIQUE KEY uk_team_member_user (team_id, user_id),
  KEY idx_team_member_user (user_id, status),
  KEY idx_team_member_team (team_id, status),
  CONSTRAINT fk_team_member_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_team_member_user FOREIGN KEY (user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_recruitment (
  recruitment_id bigint NOT NULL AUTO_INCREMENT,
  team_id bigint NOT NULL,
  title varchar(120) NOT NULL,
  status varchar(30) NOT NULL,
  deadline_at datetime NULL,
  work_start_at datetime NULL,
  created_by bigint NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (recruitment_id),
  KEY idx_recruitment_team (team_id, status),
  KEY idx_recruitment_status (status, deadline_at),
  CONSTRAINT fk_recruitment_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_recruitment_created_by FOREIGN KEY (created_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_recruitment_slot (
  slot_id bigint NOT NULL AUTO_INCREMENT,
  recruitment_id bigint NOT NULL,
  role_id bigint NOT NULL,
  required_count int NOT NULL,
  accepted_count int NOT NULL DEFAULT 0,
  required_experience_level varchar(30) NOT NULL,
  collaboration_condition varchar(30) NOT NULL,
  required_yn char(1) NOT NULL DEFAULT 'Y',
  role_duration varchar(30) NULL,
  equipment_required_yn char(1) NULL,
  status varchar(30) NOT NULL,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (slot_id),
  KEY idx_recruit_slot_role (role_id, status),
  KEY idx_recruit_slot_exp (required_experience_level),
  KEY idx_recruit_slot_condition (collaboration_condition),
  KEY idx_recruit_slot_recruitment (recruitment_id, status),
  CONSTRAINT fk_recruit_slot_recruitment FOREIGN KEY (recruitment_id) REFERENCES team_recruitment (recruitment_id),
  CONSTRAINT fk_recruit_slot_role FOREIGN KEY (role_id) REFERENCES `role` (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_application (
  application_id bigint NOT NULL AUTO_INCREMENT,
  team_id bigint NOT NULL,
  recruitment_id bigint NOT NULL,
  slot_id bigint NOT NULL,
  applicant_user_id bigint NOT NULL,
  message varchar(300) NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  pending_unique_key varchar(191) GENERATED ALWAYS AS (
    CASE WHEN status = 'PENDING' THEN CONCAT_WS(':', team_id, slot_id, applicant_user_id) ELSE NULL END
  ) STORED,
  reject_reason varchar(300) NULL,
  decided_by bigint NULL,
  decided_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (application_id),
  KEY idx_application_applicant (applicant_user_id, status),
  KEY idx_application_team (team_id, status),
  KEY idx_application_slot (slot_id, status),
  UNIQUE KEY uq_application_pending (pending_unique_key),
  CONSTRAINT fk_application_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_application_recruitment FOREIGN KEY (recruitment_id) REFERENCES team_recruitment (recruitment_id),
  CONSTRAINT fk_application_slot FOREIGN KEY (slot_id) REFERENCES team_recruitment_slot (slot_id),
  CONSTRAINT fk_application_user FOREIGN KEY (applicant_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_application_decider FOREIGN KEY (decided_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_invitation (
  invitation_id bigint NOT NULL AUTO_INCREMENT,
  team_id bigint NOT NULL,
  recruitment_id bigint NOT NULL,
  slot_id bigint NOT NULL,
  target_user_id bigint NOT NULL,
  inviter_user_id bigint NOT NULL,
  message varchar(300) NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  pending_unique_key varchar(191) GENERATED ALWAYS AS (
    CASE WHEN status = 'PENDING' THEN CONCAT_WS(':', team_id, slot_id, target_user_id) ELSE NULL END
  ) STORED,
  decided_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (invitation_id),
  KEY idx_invitation_target (target_user_id, status, created_at),
  KEY idx_invitation_team (team_id, status),
  KEY idx_invitation_slot (slot_id, status),
  UNIQUE KEY uq_invitation_pending (pending_unique_key),
  CONSTRAINT fk_invitation_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_invitation_recruitment FOREIGN KEY (recruitment_id) REFERENCES team_recruitment (recruitment_id),
  CONSTRAINT fk_invitation_slot FOREIGN KEY (slot_id) REFERENCES team_recruitment_slot (slot_id),
  CONSTRAINT fk_invitation_target FOREIGN KEY (target_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_invitation_inviter FOREIGN KEY (inviter_user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_plan_item (
  plan_item_id bigint NOT NULL AUTO_INCREMENT,
  team_id bigint NOT NULL,
  title varchar(150) NOT NULL,
  description varchar(1000) NULL,
  assignee_user_id bigint NULL,
  role_id bigint NULL,
  due_at datetime NULL,
  status varchar(30) NOT NULL DEFAULT 'TODO',
  created_by bigint NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (plan_item_id),
  KEY idx_plan_team (team_id, status, due_at),
  CONSTRAINT fk_plan_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_plan_assignee FOREIGN KEY (assignee_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_plan_role FOREIGN KEY (role_id) REFERENCES `role` (role_id),
  CONSTRAINT fk_plan_created_by FOREIGN KEY (created_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_closure_snapshot (
  closure_snapshot_id bigint NOT NULL AUTO_INCREMENT,
  team_id bigint NOT NULL,
  end_type varchar(30) NOT NULL,
  snapshot_json json NOT NULL,
  created_by bigint NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (closure_snapshot_id),
  KEY idx_closure_team (team_id, created_at),
  CONSTRAINT fk_closure_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_closure_user FOREIGN KEY (created_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS matching_score_policy (
  policy_id bigint NOT NULL AUTO_INCREMENT,
  policy_name varchar(100) NOT NULL,
  status varchar(30) NOT NULL,
  version int NOT NULL,
  description varchar(255) NULL,
  created_by bigint NULL,
  updated_by bigint NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (policy_id),
  KEY idx_score_policy_status (status, version),
  CONSTRAINT fk_policy_created_by FOREIGN KEY (created_by) REFERENCES user_account (user_id),
  CONSTRAINT fk_policy_updated_by FOREIGN KEY (updated_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS matching_score_policy_item (
  policy_item_id bigint NOT NULL AUTO_INCREMENT,
  policy_id bigint NOT NULL,
  score_group varchar(30) NOT NULL,
  element_code varchar(50) NOT NULL,
  display_name varchar(100) NOT NULL,
  weight decimal(5,2) NOT NULL,
  sort_order int NOT NULL DEFAULT 0,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (policy_item_id),
  KEY idx_score_policy_item (policy_id, score_group, element_code),
  CONSTRAINT fk_score_policy_item_policy FOREIGN KEY (policy_id) REFERENCES matching_score_policy (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS matching_score_policy_history (
  history_id bigint NOT NULL AUTO_INCREMENT,
  policy_id bigint NOT NULL,
  changed_by bigint NOT NULL,
  before_json json NOT NULL,
  after_json json NOT NULL,
  change_reason varchar(500) NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (history_id),
  KEY idx_policy_history_policy (policy_id, created_at),
  CONSTRAINT fk_policy_history_policy FOREIGN KEY (policy_id) REFERENCES matching_score_policy (policy_id),
  CONSTRAINT fk_policy_history_user FOREIGN KEY (changed_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS matching_bookmark (
  bookmark_id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  target_type varchar(30) NOT NULL,
  target_id bigint NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (bookmark_id),
  UNIQUE KEY uk_matching_bookmark (user_id, target_type, target_id),
  KEY idx_bookmark_target (target_type, target_id),
  CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS matching_action_log (
  action_log_id bigint NOT NULL AUTO_INCREMENT,
  actor_user_id bigint NOT NULL,
  action_type varchar(50) NOT NULL,
  target_type varchar(30) NOT NULL,
  target_id bigint NOT NULL,
  team_id bigint NULL,
  role_id bigint NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (action_log_id),
  KEY idx_action_actor (actor_user_id, created_at),
  KEY idx_action_target (target_type, target_id, created_at),
  KEY idx_action_type (action_type, created_at),
  CONSTRAINT fk_action_actor FOREIGN KEY (actor_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_action_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_action_role FOREIGN KEY (role_id) REFERENCES `role` (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS board_post (
  post_id bigint NOT NULL AUTO_INCREMENT,
  author_user_id bigint NOT NULL,
  category varchar(30) NOT NULL,
  free_category varchar(30) NULL,
  title varchar(150) NOT NULL,
  content text NOT NULL,
  status varchar(30) NOT NULL,
  visibility varchar(30) NOT NULL,
  like_count int NOT NULL DEFAULT 0,
  review_count int NOT NULL DEFAULT 0,
  view_count int NOT NULL DEFAULT 0,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted_at datetime NULL,
  PRIMARY KEY (post_id),
  KEY idx_board_post_category_created (category, created_at),
  KEY idx_board_post_popular (category, like_count, review_count, view_count),
  KEY idx_board_post_free_filter (category, free_category, status, visibility, created_at, post_id),
  CONSTRAINT fk_board_post_author FOREIGN KEY (author_user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS board_review (
  review_id bigint NOT NULL AUTO_INCREMENT,
  post_id bigint NOT NULL,
  author_user_id bigint NOT NULL,
  parent_review_id bigint NULL,
  content varchar(300) NOT NULL,
  status varchar(30) NOT NULL,
  delete_display_text varchar(80) NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted_at datetime NULL,
  PRIMARY KEY (review_id),
  KEY idx_board_review_post (post_id, created_at),
  KEY idx_board_review_parent (parent_review_id),
  CONSTRAINT fk_board_review_post FOREIGN KEY (post_id) REFERENCES board_post (post_id),
  CONSTRAINT fk_board_review_author FOREIGN KEY (author_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_board_review_parent FOREIGN KEY (parent_review_id) REFERENCES board_review (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS board_like (
  post_id bigint NOT NULL,
  user_id bigint NOT NULL,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id),
  KEY idx_board_like_user (user_id, active_yn),
  CONSTRAINT fk_board_like_post FOREIGN KEY (post_id) REFERENCES board_post (post_id),
  CONSTRAINT fk_board_like_user FOREIGN KEY (user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS board_view_log (
  view_log_id bigint NOT NULL AUTO_INCREMENT,
  post_id bigint NOT NULL,
  viewer_user_id bigint NULL,
  ip_hash varchar(128) NOT NULL,
  view_window_start datetime NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (view_log_id),
  UNIQUE KEY uk_board_view_window (post_id, viewer_user_id, ip_hash, view_window_start),
  KEY idx_board_view_post (post_id, created_at),
  CONSTRAINT fk_board_view_post FOREIGN KEY (post_id) REFERENCES board_post (post_id),
  CONSTRAINT fk_board_view_user FOREIGN KEY (viewer_user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS content_report (
  report_id bigint NOT NULL AUTO_INCREMENT,
  reporter_user_id bigint NOT NULL,
  target_type varchar(30) NOT NULL,
  target_id bigint NOT NULL,
  reason_code varchar(50) NOT NULL,
  detail varchar(1000) NULL,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  moderation_action varchar(50) NULL,
  resolution_note varchar(1000) NULL,
  reviewed_by bigint NULL,
  reviewed_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (report_id),
  KEY idx_content_report_status (status, created_at),
  KEY idx_content_report_target (target_type, target_id, status),
  KEY idx_content_report_reporter (reporter_user_id, created_at),
  CONSTRAINT fk_content_report_reporter FOREIGN KEY (reporter_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_content_report_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS work_item (
  work_id bigint NOT NULL AUTO_INCREMENT,
  owner_user_id bigint NOT NULL,
  team_id bigint NULL,
  board_post_id bigint NULL,
  file_id bigint NULL,
  title varchar(150) NOT NULL,
  description text NULL,
  media_type varchar(30) NOT NULL,
  work_type varchar(30) NULL,
  youtube_url varchar(500) NULL,
  youtube_video_id varchar(50) NULL,
  youtube_title varchar(255) NULL,
  youtube_channel_title varchar(255) NULL,
  youtube_thumbnail_url varchar(500) NULL,
  representative_image_path varchar(500) NULL,
  youtube_duration_seconds int NULL,
  visibility varchar(30) NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'PUBLISHED',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (work_id),
  KEY idx_work_owner (owner_user_id, created_at),
  KEY idx_work_team (team_id, created_at),
  KEY idx_work_file (file_id),
  KEY idx_work_type_status (work_type, status, board_post_id),
  CONSTRAINT fk_work_owner FOREIGN KEY (owner_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_work_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_work_board_post FOREIGN KEY (board_post_id) REFERENCES board_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS work_genre (
  work_id bigint NOT NULL,
  genre_id bigint NOT NULL,
  sort_order int NOT NULL DEFAULT 0,
  PRIMARY KEY (work_id, genre_id),
  KEY idx_work_genre_genre (genre_id, work_id),
  KEY idx_work_genre_order (work_id, sort_order, genre_id),
  CONSTRAINT fk_work_genre_work FOREIGN KEY (work_id) REFERENCES work_item (work_id),
  CONSTRAINT fk_work_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS file_metadata (
  file_id bigint NOT NULL AUTO_INCREMENT,
  uploader_user_id bigint NOT NULL,
  team_id bigint NULL,
  original_name varchar(255) NOT NULL,
  stored_path varchar(500) NOT NULL,
  content_type varchar(100) NOT NULL,
  size_bytes bigint NOT NULL,
  duration_seconds int NULL,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  hold_reason varchar(500) NULL,
  deleted_at datetime NULL,
  physical_delete_due_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (file_id),
  KEY idx_file_uploader (uploader_user_id, status),
  KEY idx_file_team (team_id, status),
  KEY idx_file_delete_due (status, physical_delete_due_at),
  CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_file_team FOREIGN KEY (team_id) REFERENCES team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_work_approval_request (
  request_id bigint NOT NULL AUTO_INCREMENT,
  team_id bigint NOT NULL,
  requester_user_id bigint NOT NULL,
  file_id bigint NULL,
  board_post_id bigint NULL,
  work_id bigint NULL,
  title varchar(150) NOT NULL,
  content text NOT NULL,
  media_type varchar(30) NOT NULL,
  work_type varchar(30) NULL,
  youtube_url varchar(500) NULL,
  youtube_video_id varchar(50) NULL,
  youtube_title varchar(255) NULL,
  youtube_channel_title varchar(255) NULL,
  youtube_thumbnail_url varchar(500) NULL,
  youtube_duration_seconds int NULL,
  visibility varchar(30) NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  reject_reason varchar(500) NULL,
  decided_by bigint NULL,
  decided_at datetime NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (request_id),
  KEY idx_team_work_request_team (team_id, status, created_at),
  KEY idx_team_work_request_user (requester_user_id, status, created_at),
  KEY idx_team_work_request_file (file_id, status),
  KEY idx_team_work_request_post (board_post_id),
  CONSTRAINT fk_team_work_request_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_team_work_request_requester FOREIGN KEY (requester_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_team_work_request_file FOREIGN KEY (file_id) REFERENCES file_metadata (file_id),
  CONSTRAINT fk_team_work_request_post FOREIGN KEY (board_post_id) REFERENCES board_post (post_id),
  CONSTRAINT fk_team_work_request_work FOREIGN KEY (work_id) REFERENCES work_item (work_id),
  CONSTRAINT fk_team_work_request_decider FOREIGN KEY (decided_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_work_approval_genre (
  request_id bigint NOT NULL,
  genre_id bigint NOT NULL,
  sort_order int NOT NULL DEFAULT 0,
  PRIMARY KEY (request_id, genre_id),
  KEY idx_team_work_approval_genre (genre_id, request_id),
  CONSTRAINT fk_team_work_approval_genre_request FOREIGN KEY (request_id) REFERENCES team_work_approval_request (request_id),
  CONSTRAINT fk_team_work_approval_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS contest (
  contest_id bigint NOT NULL AUTO_INCREMENT,
  contest_type varchar(30) NOT NULL,
  title varchar(200) NOT NULL,
  summary varchar(500) NOT NULL,
  theme varchar(150) NULL,
  prize_text varchar(150) NULL,
  total_prize_amount bigint NULL,
  first_prize_amount bigint NULL,
  organizer varchar(120) NOT NULL,
  organizer_type varchar(50) NULL,
  representative_image_url varchar(500) NULL,
  representative_image_path varchar(500) NULL,
  poster_source_type varchar(50) NULL,
  poster_original_url varchar(500) NULL,
  poster_collected_at datetime NULL,
  submission_email varchar(255) NULL,
  external_url varchar(500) NULL,
  target_text varchar(500) NULL,
  target_codes_json json NULL,
  region_codes_json json NULL,
  required_roles_text varchar(500) NULL,
  related_genres_text varchar(500) NULL,
  start_at datetime NULL,
  deadline_at datetime NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'OPEN',
  save_count int NOT NULL DEFAULT 0,
  created_by bigint NULL,
  requester_company_user_id bigint NULL,
  source_request_id bigint NULL,
  source_name varchar(80) NULL,
  source_external_id varchar(100) NULL,
  source_url varchar(500) NULL,
  source_category_code varchar(50) NULL,
  source_collected_at datetime NULL,
  source_updated_at datetime NULL,
  source_permission_text varchar(1000) NULL,
  source_attribution varchar(120) NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (contest_id),
  KEY idx_contest_status_deadline (status, deadline_at),
  KEY idx_contest_type (contest_type, created_at),
  KEY idx_contest_organizer_type (organizer_type, status, deadline_at),
  KEY idx_contest_total_prize (total_prize_amount, status),
  KEY idx_contest_first_prize (first_prize_amount, status),
  KEY idx_contest_creator (created_by, created_at),
  KEY idx_contest_requester (requester_company_user_id, created_at),
  UNIQUE KEY uk_contest_source_external (source_name, source_external_id),
  CONSTRAINT fk_contest_creator FOREIGN KEY (created_by) REFERENCES user_account (user_id),
  CONSTRAINT fk_contest_requester_company FOREIGN KEY (requester_company_user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS contest_open_request (
  request_id bigint NOT NULL AUTO_INCREMENT,
  requester_user_id bigint NOT NULL,
  contest_type varchar(30) NOT NULL DEFAULT 'INTERNAL',
  title varchar(200) NOT NULL,
  summary varchar(500) NOT NULL,
  theme varchar(150) NULL,
  prize_text varchar(150) NULL,
  total_prize_amount bigint NULL,
  first_prize_amount bigint NULL,
  organizer varchar(120) NOT NULL,
  organizer_type varchar(50) NULL,
  representative_image_url varchar(500) NULL,
  representative_image_path varchar(500) NULL,
  submission_email varchar(255) NULL,
  external_url varchar(500) NULL,
  target_text varchar(500) NULL,
  target_codes_json json NULL,
  region_codes_json json NULL,
  required_roles_text varchar(500) NULL,
  related_genres_text varchar(500) NULL,
  start_at datetime NULL,
  deadline_at datetime NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  review_reason varchar(500) NULL,
  reviewed_by bigint NULL,
  reviewed_at datetime NULL,
  approved_contest_id bigint NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (request_id),
  KEY idx_contest_request_status (status, created_at),
  KEY idx_contest_request_user (requester_user_id, created_at),
  KEY idx_contest_request_contest (approved_contest_id),
  CONSTRAINT fk_contest_request_user FOREIGN KEY (requester_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_contest_request_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account (user_id),
  CONSTRAINT fk_contest_request_contest FOREIGN KEY (approved_contest_id) REFERENCES contest (contest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS contest_save (
  contest_id bigint NOT NULL,
  user_id bigint NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (contest_id, user_id),
  CONSTRAINT fk_contest_save_contest FOREIGN KEY (contest_id) REFERENCES contest (contest_id),
  CONSTRAINT fk_contest_save_user FOREIGN KEY (user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS contest_fit_cache (
  fit_cache_id bigint NOT NULL AUTO_INCREMENT,
  contest_id bigint NOT NULL,
  basis_type varchar(30) NOT NULL,
  basis_id bigint NOT NULL,
  fit_score decimal(5,2) NULL,
  reason_json json NULL,
  status varchar(30) NOT NULL DEFAULT 'READY',
  calculated_at datetime NULL,
  expires_at datetime NULL,
  PRIMARY KEY (fit_cache_id),
  UNIQUE KEY uk_contest_fit (contest_id, basis_type, basis_id),
  KEY idx_contest_fit_expires (expires_at),
  CONSTRAINT fk_contest_fit_contest FOREIGN KEY (contest_id) REFERENCES contest (contest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS contest_submission_prepare (
  prepare_id bigint NOT NULL AUTO_INCREMENT,
  contest_id bigint NOT NULL,
  user_id bigint NOT NULL,
  basis_type varchar(30) NOT NULL,
  basis_id bigint NOT NULL,
  checklist_json json NULL,
  memo varchar(1000) NULL,
  click_count int NOT NULL DEFAULT 1,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (prepare_id),
  UNIQUE KEY uk_contest_prepare_basis (contest_id, user_id, basis_type, basis_id),
  KEY idx_contest_prepare_user (user_id, updated_at),
  CONSTRAINT fk_contest_prepare_contest FOREIGN KEY (contest_id) REFERENCES contest (contest_id),
  CONSTRAINT fk_contest_prepare_user FOREIGN KEY (user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_sanction (
  sanction_id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  sanction_type varchar(30) NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  reason varchar(1000) NOT NULL,
  sanction_until datetime NULL,
  created_by bigint NOT NULL,
  revoked_by bigint NULL,
  revoked_at datetime NULL,
  revoke_reason varchar(1000) NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (sanction_id),
  KEY idx_user_sanction_user (user_id, status, sanction_until),
  KEY idx_user_sanction_status (status, created_at),
  CONSTRAINT fk_user_sanction_user FOREIGN KEY (user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_user_sanction_creator FOREIGN KEY (created_by) REFERENCES user_account (user_id),
  CONSTRAINT fk_user_sanction_revoker FOREIGN KEY (revoked_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS notification_template (
  template_id bigint NOT NULL AUTO_INCREMENT,
  template_code varchar(80) NOT NULL,
  display_name varchar(120) NOT NULL,
  notification_type varchar(50) NOT NULL DEFAULT 'ADMIN',
  target_type varchar(30) NULL,
  title_template varchar(150) NOT NULL,
  body_template varchar(500) NOT NULL,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  created_by bigint NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (template_id),
  UNIQUE KEY uk_notification_template_code (template_code),
  KEY idx_notification_template_active (active_yn, display_name),
  CONSTRAINT fk_notification_template_creator FOREIGN KEY (created_by) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS notification_delivery_batch (
  batch_id bigint NOT NULL AUTO_INCREMENT,
  sender_user_id bigint NOT NULL,
  template_id bigint NULL,
  target_scope varchar(30) NOT NULL,
  account_type varchar(30) NULL,
  team_id bigint NULL,
  recipient_count int NOT NULL DEFAULT 0,
  sent_count int NOT NULL DEFAULT 0,
  chunk_count int NOT NULL DEFAULT 0,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  title varchar(150) NOT NULL,
  body varchar(500) NOT NULL,
  notification_type varchar(50) NOT NULL,
  target_type varchar(30) NULL,
  target_id bigint NULL,
  context_json json NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at datetime NULL,
  PRIMARY KEY (batch_id),
  KEY idx_notification_batch_sender (sender_user_id, created_at),
  KEY idx_notification_batch_status (status, created_at),
  CONSTRAINT fk_notification_batch_sender FOREIGN KEY (sender_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_notification_batch_template FOREIGN KEY (template_id) REFERENCES notification_template (template_id),
  CONSTRAINT fk_notification_batch_team FOREIGN KEY (team_id) REFERENCES team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS notification (
  notification_id bigint NOT NULL AUTO_INCREMENT,
  batch_id bigint NULL,
  recipient_user_id bigint NOT NULL,
  sender_user_id bigint NULL,
  notification_type varchar(50) NOT NULL,
  title varchar(150) NOT NULL,
  body varchar(500) NOT NULL,
  target_type varchar(30) NULL,
  target_id bigint NULL,
  read_yn char(1) NOT NULL DEFAULT 'N',
  hidden_yn char(1) NOT NULL DEFAULT 'N',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at datetime NULL,
  expires_at datetime NULL,
  PRIMARY KEY (notification_id),
  KEY idx_notification_recipient (recipient_user_id, read_yn, created_at),
  KEY idx_notification_batch (batch_id, created_at),
  CONSTRAINT fk_notification_batch FOREIGN KEY (batch_id) REFERENCES notification_delivery_batch (batch_id),
  CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_notification_sender FOREIGN KEY (sender_user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_log (
  audit_log_id bigint NOT NULL AUTO_INCREMENT,
  actor_user_id bigint NULL,
  action_type varchar(80) NOT NULL,
  target_type varchar(50) NULL,
  target_id bigint NULL,
  ip_hash varchar(128) NULL,
  before_json json NULL,
  after_json json NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (audit_log_id),
  KEY idx_audit_actor (actor_user_id, created_at),
  KEY idx_audit_target (target_type, target_id, created_at),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES user_account (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS operation_log (
  operation_log_id bigint NOT NULL AUTO_INCREMENT,
  log_level varchar(20) NOT NULL,
  event_code varchar(80) NOT NULL,
  message varchar(1000) NOT NULL,
  context_json json NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (operation_log_id),
  KEY idx_operation_event (event_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
