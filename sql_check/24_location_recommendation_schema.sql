SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS shooting_location (
  location_id bigint NOT NULL AUTO_INCREMENT,
  source_location_id varchar(20) NOT NULL,
  source_variant_no int NOT NULL DEFAULT 1,
  place_name varchar(120) NOT NULL,
  sido varchar(50) NOT NULL,
  sigungu varchar(80) NULL,
  lot_address varchar(255) NULL,
  road_address varchar(255) NULL,
  latitude decimal(10,7) NOT NULL,
  longitude decimal(10,7) NOT NULL,
  source_conflict_yn char(1) NOT NULL DEFAULT 'N',
  quality_flags_json json NULL,
  search_text text NULL,
  active_yn char(1) NOT NULL DEFAULT 'Y',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (location_id),
  UNIQUE KEY uk_shooting_location_source_variant (source_location_id, source_variant_no),
  KEY idx_shooting_location_source_location (source_location_id),
  KEY idx_shooting_location_region (sido, sigungu),
  KEY idx_shooting_location_coord (latitude, longitude),
  KEY idx_shooting_location_place_name (place_name),
  KEY idx_shooting_location_active (active_yn),
  KEY idx_shooting_location_source_coord (source_location_id, latitude, longitude),
  CONSTRAINT chk_shooting_location_variant CHECK (source_variant_no >= 1),
  CONSTRAINT chk_shooting_location_latitude CHECK (latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_shooting_location_longitude CHECK (longitude BETWEEN -180 AND 180),
  CONSTRAINT chk_shooting_location_conflict_yn CHECK (source_conflict_yn IN ('Y', 'N')),
  CONSTRAINT chk_shooting_location_active_yn CHECK (active_yn IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS shooting_location_history (
  history_id bigint NOT NULL AUTO_INCREMENT,
  location_id bigint NOT NULL,
  source_event_id varchar(20) NOT NULL,
  movie_code varchar(20) NOT NULL,
  movie_title varchar(120) NOT NULL,
  production_year smallint NULL,
  scene_description varchar(1000) NULL,
  characters varchar(300) NULL,
  source_name varchar(100) NOT NULL,
  source_url varchar(1000) NULL,
  raw_row_json json NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (history_id),
  UNIQUE KEY uk_shooting_location_history_event (source_event_id),
  KEY idx_shooting_location_history_location (location_id),
  KEY idx_shooting_location_history_movie_code (movie_code),
  KEY idx_shooting_location_history_movie_title (movie_title),
  KEY idx_shooting_location_history_production_year (production_year),
  CONSTRAINT fk_shooting_location_history_location FOREIGN KEY (location_id) REFERENCES shooting_location (location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS location_search_session (
  session_id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  team_id bigint NULL,
  prompt text NOT NULL,
  context_type varchar(30) NOT NULL,
  parsed_conditions_json json NULL,
  candidate_count int NOT NULL DEFAULT 0,
  recommendation_count int NOT NULL DEFAULT 0,
  status varchar(30) NOT NULL DEFAULT 'COMPLETED',
  failure_reason varchar(500) NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id),
  KEY idx_location_search_session_user_created (user_id, created_at),
  KEY idx_location_search_session_team_created (team_id, created_at),
  KEY idx_location_search_session_status_created (status, created_at),
  CONSTRAINT fk_location_search_session_user FOREIGN KEY (user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_location_search_session_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT chk_location_search_session_candidate_count CHECK (candidate_count >= 0),
  CONSTRAINT chk_location_search_session_recommendation_count CHECK (recommendation_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS location_recommendation_result (
  recommendation_id bigint NOT NULL AUTO_INCREMENT,
  session_id bigint NOT NULL,
  location_id bigint NOT NULL,
  rank_no int NOT NULL,
  score decimal(5,2) NULL,
  ai_summary varchar(500) NULL,
  match_reason varchar(1000) NULL,
  usage_idea varchar(1000) NULL,
  recommendation_basis varchar(1000) NULL,
  check_points_json json NULL,
  model_name varchar(100) NULL,
  fallback_yn char(1) NOT NULL DEFAULT 'N',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (recommendation_id),
  UNIQUE KEY uk_location_recommendation_result_session_location (session_id, location_id),
  UNIQUE KEY uk_location_recommendation_result_session_rank (session_id, rank_no),
  KEY idx_location_recommendation_result_location (location_id),
  CONSTRAINT fk_location_recommendation_result_session FOREIGN KEY (session_id) REFERENCES location_search_session (session_id),
  CONSTRAINT fk_location_recommendation_result_location FOREIGN KEY (location_id) REFERENCES shooting_location (location_id),
  CONSTRAINT chk_location_recommendation_result_rank CHECK (rank_no >= 1),
  CONSTRAINT chk_location_recommendation_result_score CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
  CONSTRAINT chk_location_recommendation_result_fallback_yn CHECK (fallback_yn IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS saved_location_candidate (
  candidate_id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  team_id bigint NULL,
  location_id bigint NOT NULL,
  session_id bigint NULL,
  recommendation_id bigint NULL,
  title varchar(150) NOT NULL,
  memo varchar(1000) NULL,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  source_type varchar(30) NOT NULL DEFAULT 'AI_RECOMMENDATION',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  deleted_at datetime NULL,
  active_unique_key varchar(191) GENERATED ALWAYS AS (
    CASE
      WHEN status = 'ACTIVE' AND deleted_at IS NULL
      THEN CONCAT_WS(':', user_id, COALESCE(team_id, 0), location_id)
      ELSE NULL
    END
  ) STORED,
  PRIMARY KEY (candidate_id),
  UNIQUE KEY uq_saved_location_candidate_active (active_unique_key),
  KEY idx_saved_location_candidate_user_status_created (user_id, status, created_at),
  KEY idx_saved_location_candidate_team_status_created (team_id, status, created_at),
  KEY idx_saved_location_candidate_location (location_id),
  KEY idx_saved_location_candidate_session (session_id),
  KEY idx_saved_location_candidate_recommendation (recommendation_id),
  CONSTRAINT fk_saved_location_candidate_user FOREIGN KEY (user_id) REFERENCES user_account (user_id),
  CONSTRAINT fk_saved_location_candidate_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_saved_location_candidate_location FOREIGN KEY (location_id) REFERENCES shooting_location (location_id),
  CONSTRAINT fk_saved_location_candidate_session FOREIGN KEY (session_id) REFERENCES location_search_session (session_id),
  CONSTRAINT fk_saved_location_candidate_recommendation FOREIGN KEY (recommendation_id) REFERENCES location_recommendation_result (recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
