USE slate;

DROP PROCEDURE IF EXISTS add_contest_crawl_source_schema;
DELIMITER $$
CREATE PROCEDURE add_contest_crawl_source_schema()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'poster_source_type') THEN
    ALTER TABLE contest ADD COLUMN poster_source_type varchar(50) NULL AFTER representative_image_path;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'poster_original_url') THEN
    ALTER TABLE contest ADD COLUMN poster_original_url varchar(500) NULL AFTER poster_source_type;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'poster_collected_at') THEN
    ALTER TABLE contest ADD COLUMN poster_collected_at datetime NULL AFTER poster_original_url;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'source_name') THEN
    ALTER TABLE contest ADD COLUMN source_name varchar(80) NULL AFTER source_request_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'source_external_id') THEN
    ALTER TABLE contest ADD COLUMN source_external_id varchar(100) NULL AFTER source_name;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'source_url') THEN
    ALTER TABLE contest ADD COLUMN source_url varchar(500) NULL AFTER source_external_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'source_category_code') THEN
    ALTER TABLE contest ADD COLUMN source_category_code varchar(50) NULL AFTER source_url;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'source_collected_at') THEN
    ALTER TABLE contest ADD COLUMN source_collected_at datetime NULL AFTER source_category_code;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'source_updated_at') THEN
    ALTER TABLE contest ADD COLUMN source_updated_at datetime NULL AFTER source_collected_at;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'source_permission_text') THEN
    ALTER TABLE contest ADD COLUMN source_permission_text varchar(1000) NULL AFTER source_updated_at;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'contest' AND column_name = 'source_attribution') THEN
    ALTER TABLE contest ADD COLUMN source_attribution varchar(120) NULL AFTER source_permission_text;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'contest' AND index_name = 'uk_contest_source_external') THEN
    ALTER TABLE contest ADD UNIQUE KEY uk_contest_source_external (source_name, source_external_id);
  END IF;
END$$
DELIMITER ;

CALL add_contest_crawl_source_schema();
DROP PROCEDURE IF EXISTS add_contest_crawl_source_schema;
