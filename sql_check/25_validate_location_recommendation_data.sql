SELECT 'shooting_location_count' AS check_name, COUNT(*) AS actual_value
FROM shooting_location
UNION ALL
SELECT 'shooting_location_history_count', COUNT(*)
FROM shooting_location_history
UNION ALL
SELECT 'distinct_source_location_id', COUNT(DISTINCT source_location_id)
FROM shooting_location
UNION ALL
SELECT 'distinct_source_event_id', COUNT(DISTINCT source_event_id)
FROM shooting_location_history
UNION ALL
SELECT 'duplicate_source_variant_keys', COUNT(*)
FROM (
  SELECT source_location_id, source_variant_no
  FROM shooting_location
  GROUP BY source_location_id, source_variant_no
  HAVING COUNT(*) > 1
) duplicate_source_variant
UNION ALL
SELECT 'duplicate_source_event_ids', COUNT(*)
FROM (
  SELECT source_event_id
  FROM shooting_location_history
  GROUP BY source_event_id
  HAVING COUNT(*) > 1
) duplicate_event
UNION ALL
SELECT 'orphan_history_count', COUNT(*)
FROM shooting_location_history h
LEFT JOIN shooting_location l ON l.location_id = h.location_id
WHERE l.location_id IS NULL
UNION ALL
SELECT 'invalid_coordinate_count', COUNT(*)
FROM shooting_location
WHERE latitude < -90
   OR latitude > 90
   OR longitude < -180
   OR longitude > 180
UNION ALL
SELECT 'conflict_flag_source_ids', COUNT(DISTINCT source_location_id)
FROM shooting_location
WHERE source_conflict_yn = 'Y'
UNION ALL
SELECT 'actual_multi_variant_source_ids', COUNT(*)
FROM (
  SELECT source_location_id
  FROM shooting_location
  GROUP BY source_location_id
  HAVING COUNT(DISTINCT CONCAT(latitude, ':', longitude)) > 1
) multi_variant
UNION ALL
SELECT 'multi_variant_source_missing_conflict_flag', COUNT(*)
FROM (
  SELECT source_location_id
  FROM shooting_location
  GROUP BY source_location_id
  HAVING COUNT(DISTINCT CONCAT(latitude, ':', longitude)) > 1
     AND SUM(source_conflict_yn = 'N') > 0
) missing_conflict
UNION ALL
SELECT 'single_variant_source_with_conflict_flag', COUNT(*)
FROM (
  SELECT source_location_id
  FROM shooting_location
  GROUP BY source_location_id
  HAVING COUNT(DISTINCT CONCAT(latitude, ':', longitude)) = 1
     AND SUM(source_conflict_yn = 'Y') > 0
) incorrect_conflict
UNION ALL
SELECT 'missing_required_location_fields', COUNT(*)
FROM shooting_location
WHERE source_location_id IS NULL
   OR TRIM(source_location_id) = ''
   OR place_name IS NULL
   OR TRIM(place_name) = ''
   OR sido IS NULL
   OR TRIM(sido) = ''
UNION ALL
SELECT 'missing_required_history_fields', COUNT(*)
FROM shooting_location_history
WHERE source_event_id IS NULL
   OR TRIM(source_event_id) = ''
   OR movie_code IS NULL
   OR TRIM(movie_code) = ''
   OR movie_title IS NULL
   OR TRIM(movie_title) = ''
   OR source_name IS NULL
   OR TRIM(source_name) = ''
UNION ALL
SELECT 'empty_search_text_count', COUNT(*)
FROM shooting_location
WHERE search_text IS NULL
   OR TRIM(search_text) = ''
UNION ALL
SELECT 'invalid_location_quality_json', COUNT(*)
FROM shooting_location
WHERE quality_flags_json IS NOT NULL
  AND JSON_VALID(quality_flags_json) = 0
UNION ALL
SELECT 'invalid_history_raw_json', COUNT(*)
FROM shooting_location_history
WHERE raw_row_json IS NULL
   OR JSON_VALID(raw_row_json) = 0;
