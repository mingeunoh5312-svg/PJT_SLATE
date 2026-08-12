SET NAMES utf8mb4;

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

INSERT INTO common_code (code_group, code, display_name, description, sort_order)
VALUES ('ADMIN_PERMISSION', 'DEMO_ACCESS_MANAGE', '접근 코드 관리', NULL, 6)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  sort_order = VALUES(sort_order),
  active_yn = 'Y';

INSERT INTO admin_permission (user_id, permission_code, active_yn, granted_by)
SELECT user_id, 'DEMO_ACCESS_MANAGE', 'Y', user_id
FROM user_account
WHERE account_type = 'ADMIN'
ON DUPLICATE KEY UPDATE
  active_yn = VALUES(active_yn),
  granted_by = VALUES(granted_by);
