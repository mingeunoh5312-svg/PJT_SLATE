USE slate;

DROP PROCEDURE IF EXISTS add_contest_image_columns;
DELIMITER $$
CREATE PROCEDURE add_contest_image_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'representative_image_path'
  ) THEN
    ALTER TABLE contest ADD COLUMN representative_image_path varchar(500) NULL AFTER representative_image_url;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'representative_image_path'
  ) THEN
    ALTER TABLE contest_open_request ADD COLUMN representative_image_path varchar(500) NULL AFTER representative_image_url;
  END IF;
END$$
DELIMITER ;

CALL add_contest_image_columns();
DROP PROCEDURE IF EXISTS add_contest_image_columns;
