SET NAMES utf8mb4;

INSERT INTO common_code_group (code_group, group_name, description)
VALUES
  ('FREE_POST_CATEGORY', '자유게시판 세부 분류', '공지, 질문, 정보, 후기, 자유'),
  ('WORK_TYPE', '작업물 종류', '작업물 형식 분류')
ON DUPLICATE KEY UPDATE
  group_name = VALUES(group_name),
  description = VALUES(description),
  active_yn = 'Y';

INSERT INTO common_code (code_group, code, display_name, description, sort_order, active_yn)
VALUES
  ('FREE_POST_CATEGORY', 'NOTICE', '공지', NULL, 1, 'Y'),
  ('FREE_POST_CATEGORY', 'QUESTION', '질문', NULL, 2, 'Y'),
  ('FREE_POST_CATEGORY', 'INFO', '정보', NULL, 3, 'Y'),
  ('FREE_POST_CATEGORY', 'REVIEW', '후기', NULL, 4, 'Y'),
  ('FREE_POST_CATEGORY', 'FREE', '자유', NULL, 5, 'Y'),
  ('WORK_TYPE', 'SHORT_FILM', '단편영화', NULL, 1, 'Y'),
  ('WORK_TYPE', 'FEATURE_FILM', '장편영화', NULL, 2, 'Y'),
  ('WORK_TYPE', 'MUSIC_VIDEO', '뮤직비디오', NULL, 3, 'Y'),
  ('WORK_TYPE', 'ADVERTISEMENT', '광고', NULL, 4, 'Y'),
  ('WORK_TYPE', 'DOCUMENTARY', '다큐멘터리', NULL, 5, 'Y'),
  ('WORK_TYPE', 'WEB_CONTENT', '웹 콘텐츠', NULL, 6, 'Y'),
  ('WORK_TYPE', 'OTHER', '기타', NULL, 7, 'Y')
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  active_yn = 'Y';

DROP PROCEDURE IF EXISTS migrate_board_full_integration;
DELIMITER $$
CREATE PROCEDURE migrate_board_full_integration()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'board_post' AND column_name = 'free_category'
  ) THEN
    ALTER TABLE board_post ADD COLUMN free_category varchar(30) NULL AFTER category;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'work_item' AND column_name = 'work_type'
  ) THEN
    ALTER TABLE work_item ADD COLUMN work_type varchar(30) NULL AFTER media_type;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'team_work_approval_request' AND column_name = 'work_type'
  ) THEN
    ALTER TABLE team_work_approval_request ADD COLUMN work_type varchar(30) NULL AFTER media_type;
  END IF;

  UPDATE board_post SET free_category = 'FREE' WHERE category = 'FREE' AND free_category IS NULL;
  UPDATE work_item SET work_type = 'OTHER' WHERE work_type IS NULL;
  UPDATE team_work_approval_request SET work_type = 'OTHER' WHERE work_type IS NULL;
  UPDATE board_post p
  SET p.like_count = (
    SELECT COUNT(*) FROM board_like bl
    WHERE bl.post_id = p.post_id AND bl.active_yn = 'Y'
  );

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'board_post' AND index_name = 'idx_board_post_free_filter'
  ) THEN
    CREATE INDEX idx_board_post_free_filter
      ON board_post (category, free_category, status, visibility, created_at, post_id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'work_item' AND index_name = 'idx_work_type_status'
  ) THEN
    CREATE INDEX idx_work_type_status ON work_item (work_type, status, board_post_id);
  END IF;
END$$
DELIMITER ;

CALL migrate_board_full_integration();
DROP PROCEDURE IF EXISTS migrate_board_full_integration;
