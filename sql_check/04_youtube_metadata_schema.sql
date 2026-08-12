SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_youtube_metadata_column;

DELIMITER $$
CREATE PROCEDURE add_youtube_metadata_column(
  IN p_table_name varchar(64),
  IN p_column_name varchar(64),
  IN p_column_definition varchar(255),
  IN p_after_column varchar(64)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name
  ) THEN
    SET @ddl = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ',
      p_column_definition,
      ' AFTER `', p_after_column, '`'
    );
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL add_youtube_metadata_column('work_item', 'youtube_video_id', 'varchar(50) NULL', 'youtube_url');
CALL add_youtube_metadata_column('work_item', 'youtube_title', 'varchar(255) NULL', 'youtube_video_id');
CALL add_youtube_metadata_column('work_item', 'youtube_channel_title', 'varchar(255) NULL', 'youtube_title');
CALL add_youtube_metadata_column('work_item', 'youtube_thumbnail_url', 'varchar(500) NULL', 'youtube_channel_title');
CALL add_youtube_metadata_column('work_item', 'youtube_duration_seconds', 'int NULL', 'youtube_thumbnail_url');

CALL add_youtube_metadata_column('team_work_approval_request', 'youtube_video_id', 'varchar(50) NULL', 'youtube_url');
CALL add_youtube_metadata_column('team_work_approval_request', 'youtube_title', 'varchar(255) NULL', 'youtube_video_id');
CALL add_youtube_metadata_column('team_work_approval_request', 'youtube_channel_title', 'varchar(255) NULL', 'youtube_title');
CALL add_youtube_metadata_column('team_work_approval_request', 'youtube_thumbnail_url', 'varchar(500) NULL', 'youtube_channel_title');
CALL add_youtube_metadata_column('team_work_approval_request', 'youtube_duration_seconds', 'int NULL', 'youtube_thumbnail_url');

DROP PROCEDURE IF EXISTS add_youtube_metadata_column;
