-- Validation queries for 27_seed_korea_regions.sql.

SELECT COUNT(*) AS active_region_count
FROM region
WHERE active_yn = 'Y';

SELECT sido_name, COUNT(*) AS region_count
FROM region
WHERE active_yn = 'Y'
GROUP BY sido_name
ORDER BY sido_name;

SELECT region_code, public_display_name, center_lat, center_lng
FROM region
WHERE active_yn = 'Y'
  AND (
    center_lat NOT BETWEEN 33.0 AND 39.5
    OR center_lng NOT BETWEEN 124.0 AND 132.5
  )
ORDER BY region_code;

SELECT region_code, public_display_name, COUNT(*) AS duplicate_count
FROM region
WHERE active_yn = 'Y'
GROUP BY region_code, public_display_name
HAVING COUNT(*) > 1;

SELECT public_display_name, COUNT(*) AS duplicate_count
FROM region
WHERE active_yn = 'Y'
GROUP BY public_display_name
HAVING COUNT(*) > 1;
