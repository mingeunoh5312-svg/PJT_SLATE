USE slate;

UPDATE contest
SET external_url = NULL
WHERE source_name = 'CONTESTKOREA'
  AND external_url IS NOT NULL
  AND (
    (source_url IS NOT NULL AND TRIM(external_url) = TRIM(source_url))
    OR LOWER(SUBSTRING_INDEX(SUBSTRING_INDEX(REGEXP_REPLACE(TRIM(external_url), '^https?://', ''), '/', 1), ':', 1)) IN ('contestkorea.com', 'www.contestkorea.com')
    OR LOWER(SUBSTRING_INDEX(SUBSTRING_INDEX(REGEXP_REPLACE(TRIM(external_url), '^https?://', ''), '/', 1), ':', 1)) LIKE '%.contestkorea.com'
  );
