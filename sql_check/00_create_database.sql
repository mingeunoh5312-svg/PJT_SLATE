CREATE DATABASE IF NOT EXISTS slate
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'slate_app'@'localhost' IDENTIFIED BY 'slate_omg_chachi_java_05';
GRANT ALL PRIVILEGES ON slate.* TO 'slate_app'@'localhost';
FLUSH PRIVILEGES;

USE slate;
