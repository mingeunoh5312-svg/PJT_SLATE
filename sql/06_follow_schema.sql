SET NAMES utf8mb4;

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
