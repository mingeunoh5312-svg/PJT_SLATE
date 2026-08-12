USE slate;

DROP PROCEDURE IF EXISTS remove_contest_benefit_extra_columns;
DELIMITER $$
CREATE PROCEDURE remove_contest_benefit_extra_columns()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'benefit_codes_json') THEN
    ALTER TABLE contest DROP COLUMN benefit_codes_json;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'manager_tip_yn') THEN
    ALTER TABLE contest DROP COLUMN manager_tip_yn;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'award_info_yn') THEN
    ALTER TABLE contest DROP COLUMN award_info_yn;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'benefit_codes_json') THEN
    ALTER TABLE contest_open_request DROP COLUMN benefit_codes_json;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'manager_tip_yn') THEN
    ALTER TABLE contest_open_request DROP COLUMN manager_tip_yn;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest_open_request' AND column_name = 'award_info_yn') THEN
    ALTER TABLE contest_open_request DROP COLUMN award_info_yn;
  END IF;
END$$
DELIMITER ;

CALL remove_contest_benefit_extra_columns();
DROP PROCEDURE IF EXISTS remove_contest_benefit_extra_columns;
