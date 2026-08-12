USE slate;

DROP PROCEDURE IF EXISTS add_portfolio_credit_name_column;
DELIMITER $$
CREATE PROCEDURE add_portfolio_credit_name_column()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolio_item'
      AND column_name = 'credit_name'
  ) THEN
    ALTER TABLE portfolio_item
      ADD COLUMN credit_name varchar(120) NULL AFTER role_name;
  END IF;
END$$
DELIMITER ;

CALL add_portfolio_credit_name_column();
DROP PROCEDURE IF EXISTS add_portfolio_credit_name_column;
