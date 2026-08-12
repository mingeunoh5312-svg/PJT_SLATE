SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS work_genre (
  work_id bigint NOT NULL,
  genre_id bigint NOT NULL,
  sort_order int NOT NULL DEFAULT 0,
  PRIMARY KEY (work_id, genre_id),
  KEY idx_work_genre_genre (genre_id, work_id),
  KEY idx_work_genre_order (work_id, sort_order, genre_id),
  CONSTRAINT fk_work_genre_work FOREIGN KEY (work_id) REFERENCES work_item (work_id),
  CONSTRAINT fk_work_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS team_work_approval_genre (
  request_id bigint NOT NULL,
  genre_id bigint NOT NULL,
  sort_order int NOT NULL DEFAULT 0,
  PRIMARY KEY (request_id, genre_id),
  KEY idx_team_work_approval_genre (genre_id, request_id),
  CONSTRAINT fk_team_work_approval_genre_request FOREIGN KEY (request_id) REFERENCES team_work_approval_request (request_id),
  CONSTRAINT fk_team_work_approval_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
