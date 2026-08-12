USE slate;

DROP PROCEDURE IF EXISTS add_entity_image_columns;
DELIMITER $$
CREATE PROCEDURE add_entity_image_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'member_profile' AND column_name = 'profile_image_path') THEN
    ALTER TABLE member_profile ADD COLUMN profile_image_path varchar(500) NULL AFTER detail_intro;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'team' AND column_name = 'representative_image_path') THEN
    ALTER TABLE team ADD COLUMN representative_image_path varchar(500) NULL AFTER description;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'work_item' AND column_name = 'representative_image_path') THEN
    ALTER TABLE work_item ADD COLUMN representative_image_path varchar(500) NULL AFTER youtube_thumbnail_url;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portfolio_item' AND column_name = 'thumbnail_image_path') THEN
    ALTER TABLE portfolio_item ADD COLUMN thumbnail_image_path varchar(500) NULL AFTER thumbnail_url;
  END IF;
END$$
DELIMITER ;

CALL add_entity_image_columns();
DROP PROCEDURE IF EXISTS add_entity_image_columns;
