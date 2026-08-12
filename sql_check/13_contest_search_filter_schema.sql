USE slate;

DROP PROCEDURE IF EXISTS add_contest_filter_schema;
DELIMITER $$
CREATE PROCEDURE add_contest_filter_schema()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'total_prize_amount') THEN
    ALTER TABLE contest ADD COLUMN total_prize_amount bigint NULL AFTER prize_text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'first_prize_amount') THEN
    ALTER TABLE contest ADD COLUMN first_prize_amount bigint NULL AFTER total_prize_amount;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'organizer_type') THEN
    ALTER TABLE contest ADD COLUMN organizer_type varchar(50) NULL AFTER organizer;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'target_codes_json') THEN
    ALTER TABLE contest ADD COLUMN target_codes_json json NULL AFTER target_text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'region_codes_json') THEN
    ALTER TABLE contest ADD COLUMN region_codes_json json NULL AFTER target_codes_json;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'total_prize_amount') THEN
    ALTER TABLE contest_open_request ADD COLUMN total_prize_amount bigint NULL AFTER prize_text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'first_prize_amount') THEN
    ALTER TABLE contest_open_request ADD COLUMN first_prize_amount bigint NULL AFTER total_prize_amount;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'organizer_type') THEN
    ALTER TABLE contest_open_request ADD COLUMN organizer_type varchar(50) NULL AFTER organizer;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'target_codes_json') THEN
    ALTER TABLE contest_open_request ADD COLUMN target_codes_json json NULL AFTER target_text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'region_codes_json') THEN
    ALTER TABLE contest_open_request ADD COLUMN region_codes_json json NULL AFTER target_codes_json;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'contest' AND index_name = 'idx_contest_organizer_type') THEN
    ALTER TABLE contest ADD INDEX idx_contest_organizer_type (organizer_type, status, deadline_at);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'contest' AND index_name = 'idx_contest_total_prize') THEN
    ALTER TABLE contest ADD INDEX idx_contest_total_prize (total_prize_amount, status);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'contest' AND index_name = 'idx_contest_first_prize') THEN
    ALTER TABLE contest ADD INDEX idx_contest_first_prize (first_prize_amount, status);
  END IF;
END$$
DELIMITER ;

CALL add_contest_filter_schema();
DROP PROCEDURE IF EXISTS add_contest_filter_schema;
