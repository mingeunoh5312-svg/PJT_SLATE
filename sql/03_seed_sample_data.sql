SET NAMES utf8mb4;

INSERT INTO user_account (user_id, login_id, email, password_hash, nickname, phone, account_type, account_status, last_login_at, created_at) VALUES
(1, 'leader', 'leader@slate.test', '{noop}slate1234', '김도윤', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 60 DAY),
(2, 'director', 'director@slate.test', '{noop}slate1234', '이서연', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 58 DAY),
(3, 'camera', 'camera@slate.test', '{noop}slate1234', '이지은', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 45 DAY),
(4, 'writer', 'writer@slate.test', '{noop}slate1234', '박재훈', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 42 DAY),
(5, 'actor', 'actor@slate.test', '{noop}slate1234', '최민재', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 39 DAY),
(6, 'editor', 'editor@slate.test', '{noop}slate1234', '한유나', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 34 DAY),
(7, 'sound', 'sound@slate.test', '{noop}slate1234', '서태호', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 8 HOUR, NOW() - INTERVAL 31 DAY),
(8, 'art', 'art@slate.test', '{noop}slate1234', '강소민', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 29 DAY),
(9, 'light', 'light@slate.test', '{noop}slate1234', '윤준서', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 20 DAY),
(10, 'vfx', 'vfx@slate.test', '{noop}slate1234', '배나리', NULL, 'USER', 'ACTIVE', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 18 DAY),
(11, 'company', 'company@slate.test', '{noop}slate1234', '슬레이트랩', '010-0000-0000', 'COMPANY', 'PENDING_APPROVAL', NULL, NOW() - INTERVAL 5 DAY),
(12, 'approved-company', 'approved-company@slate.test', '{noop}slate1234', '프레임스튜디오', '010-1111-1111', 'COMPANY', 'ACTIVE', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 30 DAY),
(99, 'admin', 'admin@slate.test', '{noop}slate1234', '운영관리자', NULL, 'ADMIN', 'ACTIVE', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 90 DAY);

INSERT INTO company_application
(company_application_id, user_id, company_name, business_registration_no, manager_name, manager_phone, company_intro, public_data_company_name, status, review_reason, reviewed_by, reviewed_at, created_at) VALUES
(1, 11, '슬레이트랩', '000-00-00000', '홍담당', '010-0000-0000', '신진 창작자와 브랜드 필름을 연결하는 제작사입니다.', '슬레이트랩', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 5 DAY),
(2, 12, '프레임스튜디오', '111-11-11111', '김담당', '010-1111-1111', '독립영화 후반 작업과 배급 협업을 지원합니다.', '프레임스튜디오', 'APPROVED', '샘플 승인 회사 계정입니다.', 99, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 30 DAY);

INSERT INTO admin_permission (user_id, permission_code, active_yn, granted_by) VALUES
(99, 'COMPANY_APPROVAL', 'Y', 99),
(99, 'USER_SANCTION', 'Y', 99),
(99, 'CONTENT_MODERATION', 'Y', 99),
(99, 'SCORE_POLICY', 'Y', 99),
(99, 'CONTEST_MANAGE', 'Y', 99),
(99, 'DEMO_ACCESS_MANAGE', 'Y', 99),
(99, 'NOTIFICATION_SEND', 'Y', 99),
(99, 'LOG_VIEW', 'Y', 99),
(99, 'ADMIN_PERMISSION_MANAGE', 'Y', 99),
(99, 'REGION_MANAGE', 'Y', 99);

SET @region_jongno := (SELECT region_id FROM region WHERE region_code = '1111000000');
SET @region_junggu := (SELECT region_id FROM region WHERE region_code = '1114000000');
SET @region_gangnam := (SELECT region_id FROM region WHERE region_code = '1168000000');
SET @region_mapo := (SELECT region_id FROM region WHERE region_code = '1144000000');
SET @region_bundang := (SELECT region_id FROM region WHERE region_code = '4113500000');
SET @region_goyang := (SELECT region_id FROM region WHERE region_code = '4128100000');
SET @region_busan := (SELECT region_id FROM region WHERE region_code = '2611000000');
SET @region_daegu := (SELECT region_id FROM region WHERE region_code = '2711000000');
SET @region_gwangju := (SELECT region_id FROM region WHERE region_code = '2915500000');

INSERT INTO member_profile
(profile_id, user_id, display_name, short_intro, detail_intro, visibility, activity_status, region_id, experience_level, join_availability, collaboration_status, travel_range, preferred_duration, equipment_status, age_band, participation_mode, profile_completed_yn, last_active_at, created_at) VALUES
(1, 1, '도윤 PD', '현장 진행과 예산 관리에 강한 프로듀서입니다.', '단편과 브랜드 필름 제작 진행 경험이 있습니다.', 'PUBLIC', 'VISIBLE', @region_jongno, 'Y3_10', 'WITHIN_1W', 'AVAILABLE', 'KM_30', 'WITHIN_3M', 'NOT_ENTERED', 'THIRTIES', 'HYBRID', 'Y', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 58 DAY),
(2, 2, '서연 감독', '인물 중심 드라마 연출을 선호합니다.', '청춘 드라마와 다큐멘터리 단편을 연출했습니다.', 'PUBLIC', 'VISIBLE', @region_gangnam, 'Y3_10', 'IMMEDIATE', 'CONSIDERING', 'KM_100', 'WITHIN_6M', 'NOT_ENTERED', 'TWENTIES', 'OFFLINE', 'Y', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 55 DAY),
(3, 3, '이지은', '로케이션 촬영과 핸드헬드에 익숙합니다.', '소규모 팀에서 촬영감독과 카메라 오퍼레이터를 맡았습니다.', 'PUBLIC', 'VISIBLE', @region_mapo, 'Y0_3', 'IMMEDIATE', 'AVAILABLE', 'KM_30', 'WITHIN_3M', 'HAS_EQUIPMENT', 'TWENTIES', 'OFFLINE', 'Y', NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 44 DAY),
(4, 4, '재훈 작가', '장르 단편 시나리오와 구성에 집중합니다.', '스릴러와 미스터리 단편 각본 경험이 있습니다.', 'PUBLIC', 'VISIBLE', @region_bundang, 'Y0_3', 'WITHIN_2W', 'AVAILABLE', 'KM_100', 'WITHIN_3M', 'NO_EQUIPMENT', 'THIRTIES', 'REMOTE', 'Y', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 41 DAY),
(5, 5, '최민재', '자연스러운 생활 연기를 지향합니다.', '독립 단편과 웹드라마 출연 경험이 있습니다.', 'PUBLIC', 'VISIBLE', @region_junggu, 'Y0_3', 'WITHIN_1W', 'AVAILABLE', 'KM_30', 'WITHIN_1M', 'NOT_ENTERED', 'TWENTIES', 'OFFLINE', 'Y', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 38 DAY),
(6, 6, '유나 편집', '호흡이 좋은 편집과 색감 정리에 관심이 많습니다.', '뮤직비디오와 단편 후반 작업을 진행했습니다.', 'PUBLIC', 'VISIBLE', @region_gangnam, 'Y3_10', 'NEGOTIABLE', 'CONSIDERING', 'ANYWHERE', 'ANY', 'HAS_EQUIPMENT', 'THIRTIES', 'REMOTE', 'Y', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 33 DAY),
(7, 7, '태호 사운드', '동시녹음과 후반 사운드를 모두 다룹니다.', '현장 녹음과 사운드 디자인을 함께 맡을 수 있습니다.', 'PUBLIC', 'VISIBLE', @region_goyang, 'Y3_10', 'WITHIN_1M', 'AVAILABLE', 'KM_100', 'WITHIN_6M', 'HAS_EQUIPMENT', 'THIRTIES', 'HYBRID', 'Y', NOW() - INTERVAL 8 HOUR, NOW() - INTERVAL 30 DAY),
(8, 8, '소민 미술', '저예산 세트와 소품 설계에 강합니다.', '청춘/학원물과 판타지 소품을 작업했습니다.', 'PUBLIC', 'VISIBLE', @region_gwangju, 'Y0_3', 'WITHIN_2W', 'CONSIDERING', 'ANYWHERE', 'WITHIN_3M', 'NOT_ENTERED', 'TWENTIES', 'HYBRID', 'Y', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 28 DAY),
(9, 9, '준서 조명', '소규모 조명 세팅과 야간 촬영을 지원합니다.', '단편 조명부와 조명감독 경험이 있습니다.', 'PUBLIC', 'VISIBLE', @region_busan, 'Y3_10', 'AFTER_1M', 'CONSIDERING', 'ANYWHERE', 'WITHIN_6M', 'HAS_EQUIPMENT', 'THIRTIES', 'OFFLINE', 'Y', NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 19 DAY),
(10, 10, '나리 VFX', '색보정과 간단한 합성을 담당합니다.', '후반/VFX 파이프라인을 단순하게 정리하는 데 익숙합니다.', 'PUBLIC', 'VISIBLE', @region_daegu, 'Y0_3', 'NEGOTIABLE', 'AVAILABLE', 'ANYWHERE', 'ANY', 'HAS_EQUIPMENT', 'TWENTIES', 'REMOTE', 'Y', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 17 DAY);

INSERT INTO profile_role (profile_id, role_id) VALUES
(1, 1), (1, 2), (2, 4), (2, 5), (3, 9), (3, 10), (4, 7), (4, 8), (5, 20), (5, 21),
(6, 22), (6, 23), (7, 14), (7, 15), (8, 16), (8, 17), (9, 12), (9, 13), (10, 23), (10, 24);

UPDATE profile_role
SET sort_order = CASE role_id
  WHEN 1 THEN 0 WHEN 4 THEN 0 WHEN 9 THEN 0 WHEN 7 THEN 0 WHEN 20 THEN 0
  WHEN 22 THEN 0 WHEN 14 THEN 0 WHEN 16 THEN 0 WHEN 12 THEN 0 WHEN 23 THEN 0
  ELSE 1
END;

INSERT INTO profile_genre (profile_id, genre_id) VALUES
(1, 1), (1, 11), (2, 1), (2, 16), (3, 1), (3, 5), (4, 5), (4, 7), (5, 2), (5, 16),
(6, 1), (6, 14), (7, 5), (7, 11), (8, 10), (8, 16), (9, 4), (9, 6), (10, 9), (10, 13);

INSERT INTO profile_collaboration_condition (profile_id, condition_code) VALUES
(1, 'NEGOTIABLE'), (1, 'PAID'), (2, 'NEGOTIABLE'), (2, 'REVENUE_SHARE'), (3, 'UNPAID'), (3, 'NEGOTIABLE'),
(4, 'NEGOTIABLE'), (4, 'PAID'), (5, 'NEGOTIABLE'), (5, 'PAID'), (6, 'PAID'), (6, 'REVENUE_SHARE'),
(7, 'PAID'), (7, 'NEGOTIABLE'), (8, 'UNPAID'), (8, 'PRIZE_SHARE'), (9, 'PAID'), (10, 'NEGOTIABLE'), (10, 'REVENUE_SHARE');

INSERT INTO portfolio_item
(profile_id, title, role_name, description, source_type, external_source_name, external_reference_id, url, sort_order, status) VALUES
(3, '남산 새벽 로케이션 테스트', '촬영감독', '새벽 자연광과 핸드헬드 동선을 테스트한 개인 작업입니다.', 'MANUAL', NULL, NULL, 'https://example.test/work/namsan', 1, 'ACTIVE'),
(6, '공연 영상 편집 리듬 메모', '영상 편집', '공연 컷 전환과 리듬 편집을 정리한 후반 작업 기록입니다.', 'MANUAL', NULL, NULL, 'https://example.test/work/live-edit', 1, 'ACTIVE'),
(10, '간단한 합성 테스트 기록', 'VFX', '마스크 합성과 색 보정을 짧게 검증한 테스트입니다.', 'MANUAL', NULL, NULL, 'https://example.test/work/vfx-test', 1, 'ACTIVE');

INSERT INTO public_data_sync_item
(source_name, item_type, external_id, title, description, provider_url, display_year, creator_name, raw_json) VALUES
('SAMPLE_KOFIC', 'MOVIE', 'SAMPLE-001', '청춘의 새벽', '서울 새벽 로케이션을 중심으로 한 단편 영화 샘플입니다.', 'https://example.test/public/movie/sample-001', '2025', '슬레이트 샘플팀', JSON_OBJECT('year', 2025, 'source', 'fallback')),
('SAMPLE_KOFIC', 'MOVIE', 'SAMPLE-002', '분당 미스터리 랩', '오피스 공간을 활용한 미스터리 단편 제작 샘플입니다.', 'https://example.test/public/movie/sample-002', '2024', '서연 감독', JSON_OBJECT('year', 2024, 'source', 'fallback')),
('SAMPLE_KOFIC', 'MOVIE', 'SAMPLE-003', '라이브 컷 편집 노트', '공연 영상 편집 리듬과 후반 색 보정을 다룬 샘플입니다.', 'https://example.test/public/movie/sample-003', '2026', '원격 후반 스튜디오', JSON_OBJECT('year', 2026, 'source', 'fallback')),
('SAMPLE_KOFIC', 'COMPANY', 'SAMPLE-C001', '프레임스튜디오', '독립영화 후반 작업과 배급 협업을 지원하는 승인 회사 샘플입니다.', 'https://example.test/public/company/sample-c001', NULL, '프레임스튜디오', JSON_OBJECT('status', 'sample')),
('SAMPLE_PUBLIC', 'PERSON', 'SAMPLE-P001', '이지은 촬영 포트폴리오', '로케이션 촬영과 카메라 오퍼레이팅 중심의 공개 포트폴리오 샘플입니다.', 'https://example.test/public/person/sample-p001', '2025', '이지은', JSON_OBJECT('role', 'camera'));

INSERT INTO team (team_id, leader_user_id, name, description, status, region_id, region_any_yn, expected_duration, max_member_count, current_member_count, last_active_at, created_at) VALUES
(1, 1, '남산 새벽팀', '서울 도심의 밤과 새벽을 배경으로 한 단편 드라마 제작팀입니다.', 'RECRUITING', @region_junggu, 'N', 'WITHIN_3M', 6, 2, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 20 DAY),
(2, 2, '분당 미스터리 랩', '판교 오피스와 주변 공간을 활용한 미스터리 단편을 준비합니다.', 'RECRUITING', @region_bundang, 'N', 'WITHIN_6M', 7, 2, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 17 DAY),
(3, 6, '원격 후반 스튜디오', '원격 협업 중심으로 음악 공연 영상과 후반 작업 포트폴리오를 만듭니다.', 'IN_PROGRESS', NULL, 'Y', 'ANY', 5, 2, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 12 DAY);

INSERT INTO team_genre (team_id, genre_id) VALUES
(1, 1), (1, 16), (2, 5), (2, 7), (3, 14), (3, 13);

INSERT INTO team_member (team_id, user_id, team_role, status, joined_at) VALUES
(1, 1, 'LEADER', 'ACTIVE', NOW() - INTERVAL 20 DAY),
(1, 5, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 10 DAY),
(2, 2, 'LEADER', 'ACTIVE', NOW() - INTERVAL 17 DAY),
(2, 4, 'SUB_LEADER', 'ACTIVE', NOW() - INTERVAL 12 DAY),
(3, 6, 'LEADER', 'ACTIVE', NOW() - INTERVAL 12 DAY),
(3, 10, 'MEMBER', 'ACTIVE', NOW() - INTERVAL 9 DAY);

INSERT INTO team_recruitment (recruitment_id, team_id, title, status, deadline_at, work_start_at, created_by, created_at) VALUES
(1, 1, '남산 새벽팀 1차 스태프 모집', 'OPEN', NOW() + INTERVAL 21 DAY, NOW() + INTERVAL 14 DAY, 1, NOW() - INTERVAL 15 DAY),
(2, 2, '분당 미스터리 단편 핵심 역할 모집', 'OPEN', NOW() + INTERVAL 30 DAY, NOW() + INTERVAL 21 DAY, 2, NOW() - INTERVAL 12 DAY),
(3, 3, '원격 후반 포트폴리오 협업 모집', 'OPEN', NOW() + INTERVAL 18 DAY, NOW() + INTERVAL 10 DAY, 6, NOW() - INTERVAL 8 DAY);

INSERT INTO team_recruitment_slot
(slot_id, recruitment_id, role_id, required_count, accepted_count, required_experience_level, collaboration_condition, required_yn, role_duration, equipment_required_yn, status) VALUES
(1, 1, 9, 1, 0, 'Y0_3', 'NEGOTIABLE', 'Y', 'WITHIN_3M', 'Y', 'OPEN'),
(2, 1, 14, 1, 0, 'Y0_3', 'NEGOTIABLE', 'Y', 'WITHIN_3M', 'Y', 'OPEN'),
(3, 1, 22, 1, 0, 'Y0_3', 'REVENUE_SHARE', 'N', 'WITHIN_3M', 'N', 'OPEN'),
(4, 2, 9, 1, 0, 'Y3_10', 'PAID', 'Y', 'WITHIN_6M', 'Y', 'OPEN'),
(5, 2, 12, 1, 0, 'Y3_10', 'PAID', 'Y', 'WITHIN_6M', 'Y', 'OPEN'),
(6, 2, 16, 1, 0, 'Y0_3', 'NEGOTIABLE', 'N', 'WITHIN_3M', 'N', 'OPEN'),
(7, 3, 23, 1, 0, 'Y0_3', 'REVENUE_SHARE', 'Y', 'ANY', 'Y', 'OPEN'),
(8, 3, 25, 1, 0, 'Y0_3', 'NEGOTIABLE', 'N', 'WITHIN_1M', 'N', 'OPEN');

INSERT INTO team_plan_item (team_id, title, description, assignee_user_id, role_id, due_at, status, created_by) VALUES
(1, '제작진 모집', '125125125125', NULL, NULL, '2026-06-10 01:32:00', 'DONE', 1),
(1, '1차 로케이션 리스트 확정', '서울 중구와 종로구 야간 촬영 가능 장소 후보를 정리합니다.', 1, NULL, '2026-07-01 01:06:40', 'IN_PROGRESS', 1),
(1, '촬영 장비 대여', '', NULL, NULL, '2026-07-03 01:33:00', 'TODO', 1),
(1, '테스트 촬영 컷 정리', '핸드헬드 테스트 컷과 조도 기록을 정리합니다.', NULL, 9, '2026-07-08 01:06:40', 'TODO', 1),
(1, '1차 촬영 진행', '', NULL, NULL, '2026-07-09 01:19:00', 'TODO', 1),
(1, '1차 촬영 파일 검수', '12512512512', NULL, NULL, '2026-07-31 01:33:00', 'TODO', 1),
(1, '2차 로케이션 선정', '', NULL, NULL, '2026-08-20 01:32:00', 'TODO', 1),
(1, '2차 로케이션 리스트 확정', '', NULL, NULL, NULL, 'TODO', 1),
(1, '2차 촬영 진행', '', NULL, NULL, NULL, 'TODO', 1),
(2, '미스터리 단서 소품 목록', '오피스 공간에 배치할 단서 소품을 정리합니다.', 4, 7, NOW() + INTERVAL 10 DAY, 'TODO', 2);

INSERT INTO matching_score_policy (policy_id, policy_name, status, version, description, created_by, updated_by, created_at) VALUES
(1, '기본 점수 정책 v1', 'ACTIVE', 1, '1차 필터 80, 내부 점수 20 기준의 기본 정책', 99, 99, NOW() - INTERVAL 5 DAY);

INSERT INTO matching_score_policy_item (policy_id, score_group, element_code, display_name, weight, sort_order) VALUES
(1, 'FINAL_RATIO', 'first_filter', '1차 필터 반영 비율', 80.00, 1),
(1, 'FINAL_RATIO', 'internal', '내부 점수 반영 비율', 20.00, 2),
(1, 'FIRST_FILTER', 'role', '역할', 35.00, 1),
(1, 'FIRST_FILTER', 'region_distance', '지역/거리', 20.00, 2),
(1, 'FIRST_FILTER', 'join_time', '합류 가능 시점', 15.00, 3),
(1, 'FIRST_FILTER', 'collaboration_condition', '협업 조건', 15.00, 4),
(1, 'FIRST_FILTER', 'genre', '장르', 10.00, 5),
(1, 'FIRST_FILTER', 'experience', '경력', 5.00, 6),
(1, 'INTERNAL', 'collaboration_status', '협업 가능 상태', 40.00, 1),
(1, 'INTERNAL', 'travel_range', '이동 가능 범위', 35.00, 2),
(1, 'INTERNAL', 'duration_fit', '작업 기간 적합도', 25.00, 3);

INSERT INTO matching_bookmark (user_id, target_type, target_id, created_at) VALUES
(1, 'PROFILE', 3, NOW() - INTERVAL 2 DAY),
(1, 'PROFILE', 7, NOW() - INTERVAL 1 DAY),
(3, 'TEAM', 1, NOW() - INTERVAL 1 DAY);

INSERT INTO matching_action_log (actor_user_id, action_type, target_type, target_id, team_id, role_id, created_at) VALUES
(1, 'PROFILE_BOOKMARK', 'PROFILE', 3, 1, 9, NOW() - INTERVAL 2 DAY),
(1, 'PROFILE_VIEW', 'PROFILE', 7, 1, 14, NOW() - INTERVAL 1 DAY),
(3, 'TEAM_BOOKMARK', 'TEAM', 1, 1, 9, NOW() - INTERVAL 1 DAY);

INSERT INTO board_post (post_id, author_user_id, category, title, content, status, visibility, view_count, created_at) VALUES
(1, 1, 'WORK', '남산 새벽 로케이션 테스트', '도심 새벽 톤을 확인하기 위한 로케이션 테스트 기록입니다.', 'PUBLISHED', 'PUBLIC', 180, NOW() - INTERVAL 8 DAY),
(2, 2, 'WORK', '청춘 단편 리딩 영상 기록', '배우 리딩과 장면 호흡을 확인한 텍스트 작업물입니다.', 'PUBLISHED', 'PUBLIC', 132, NOW() - INTERVAL 7 DAY),
(3, 3, 'WORK', '핸드헬드 촬영 테스트 노트', '작은 공간에서 움직임을 따라가는 촬영 구성을 정리했습니다.', 'PUBLISHED', 'PUBLIC', 95, NOW() - INTERVAL 6 DAY),
(4, 6, 'WORK', '공연 영상 편집 리듬 메모', '음악 공연 편집 컷 포인트와 색감 방향을 기록했습니다.', 'PUBLISHED', 'PUBLIC', 156, NOW() - INTERVAL 5 DAY),
(5, 4, 'FREE', '저예산 미스터리 단편 회의 팁', '팀 빌딩 초기에 유용했던 회의 방식과 기록 습관을 공유합니다.', 'PUBLISHED', 'PUBLIC', 74, NOW() - INTERVAL 4 DAY),
(6, 8, 'FREE', '소품 제작비 줄이는 방법', '소규모 팀에서 소품을 준비할 때 확인할 항목을 정리했습니다.', 'PUBLISHED', 'PUBLIC', 88, NOW() - INTERVAL 3 DAY);

INSERT INTO board_review (post_id, author_user_id, content, status, created_at) VALUES
(1, 2, '도심 새벽 분위기가 명확하게 느껴집니다.', 'PUBLISHED', NOW() - INTERVAL 7 DAY),
(1, 3, '촬영 동선까지 같이 적혀 있어 좋습니다.', 'PUBLISHED', NOW() - INTERVAL 6 DAY),
(2, 5, '배우 입장에서 리딩 흐름을 참고하기 좋네요.', 'PUBLISHED', NOW() - INTERVAL 6 DAY),
(3, 2, '장면별 렌즈 선택도 궁금합니다.', 'PUBLISHED', NOW() - INTERVAL 5 DAY),
(4, 7, '사운드 컷 포인트와 잘 맞을 것 같습니다.', 'PUBLISHED', NOW() - INTERVAL 4 DAY),
(5, 1, '회의록 양식도 공유되면 좋겠습니다.', 'PUBLISHED', NOW() - INTERVAL 3 DAY);

INSERT INTO board_like (post_id, user_id, active_yn, created_at) VALUES
(1, 2, 'Y', NOW() - INTERVAL 7 DAY), (1, 3, 'Y', NOW() - INTERVAL 7 DAY), (1, 4, 'Y', NOW() - INTERVAL 6 DAY),
(1, 5, 'Y', NOW() - INTERVAL 6 DAY), (1, 6, 'Y', NOW() - INTERVAL 5 DAY), (1, 7, 'Y', NOW() - INTERVAL 5 DAY),
(2, 1, 'Y', NOW() - INTERVAL 7 DAY), (2, 3, 'Y', NOW() - INTERVAL 6 DAY), (2, 5, 'Y', NOW() - INTERVAL 5 DAY),
(2, 8, 'Y', NOW() - INTERVAL 4 DAY), (3, 1, 'Y', NOW() - INTERVAL 5 DAY), (3, 2, 'Y', NOW() - INTERVAL 5 DAY),
(4, 1, 'Y', NOW() - INTERVAL 4 DAY), (4, 7, 'Y', NOW() - INTERVAL 3 DAY), (4, 10, 'Y', NOW() - INTERVAL 3 DAY),
(5, 2, 'Y', NOW() - INTERVAL 3 DAY), (5, 6, 'Y', NOW() - INTERVAL 2 DAY);

UPDATE board_post p
SET p.review_count = (
  SELECT COUNT(*) FROM board_review r WHERE r.post_id = p.post_id AND r.status = 'PUBLISHED'
),
p.like_count = (
  SELECT COUNT(*) FROM board_like l WHERE l.post_id = p.post_id AND l.active_yn = 'Y'
);

INSERT INTO work_item (work_id, owner_user_id, team_id, board_post_id, title, description, media_type, youtube_url, visibility, status) VALUES
(1, 1, 1, 1, '남산 새벽 로케이션 테스트', '팀 모집용 로케이션 테스트 영상', 'YOUTUBE', 'https://www.youtube.com/embed/dQw4w9WgXcQ', 'PUBLIC', 'PUBLISHED'),
(2, 6, 3, 4, '공연 영상 편집 리듬 메모', '원격 후반 포트폴리오 샘플', 'YOUTUBE', 'https://www.youtube.com/embed/dQw4w9WgXcQ', 'PUBLIC', 'PUBLISHED');

INSERT INTO team_work_approval_request
(request_id, team_id, requester_user_id, file_id, board_post_id, work_id, title, content, media_type, youtube_url, visibility, status, reject_reason, decided_by, decided_at, created_at) VALUES
(1, 1, 5, NULL, NULL, NULL, '남산 새벽팀 리허설 컷', '팀 포트폴리오로 공개하고 싶은 리허설 컷입니다.', 'YOUTUBE', 'https://www.youtube.com/embed/dQw4w9WgXcQ', 'PUBLIC', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 2 DAY);

INSERT INTO contest (contest_id, contest_type, title, summary, theme, prize_text, organizer, representative_image_url, submission_email, external_url, start_at, deadline_at, status, save_count) VALUES
(1, 'INTERNAL', 'Slate 신진 영화인 매칭 공모', '팀과 회사가 함께 볼 수 있는 단편 기획을 모집합니다.', '신진 창작자 협업', '총 300만원', 'Slate 운영팀', NULL, 'contest@slate.test', NULL, NOW() - INTERVAL 5 DAY, NOW() + INTERVAL 25 DAY, 'OPEN', 18),
(2, 'EXTERNAL', '지역 영상 제작 지원사업', '지역 기반 영상 제작팀을 위한 외부 지원사업 안내입니다.', '지역 문화데이터', '팀별 제작지원', '문화기관 샘플', NULL, NULL, 'https://example.test/contest', NOW() - INTERVAL 10 DAY, NOW() + INTERVAL 9 DAY, 'OPEN', 11);

INSERT INTO contest_open_request
(request_id, requester_user_id, contest_type, title, summary, theme, prize_text, organizer, representative_image_url, submission_email, external_url, target_text, required_roles_text, related_genres_text, start_at, deadline_at, status, created_at) VALUES
(1, 12, 'INTERNAL', '브랜드 숏필름 제작 파트너 공모', '프레임스튜디오의 신규 브랜드 숏필름을 함께 만들 팀을 모집합니다.', '브랜드 필름', '제작비 일부 지원', '프레임스튜디오', NULL, 'brand@frame.test', NULL, '2~5인 영상 제작팀', '감독, 촬영, 편집', '드라마, 광고/홍보', NOW() + INTERVAL 7 DAY, NOW() + INTERVAL 40 DAY, 'PENDING', NOW() - INTERVAL 1 DAY);

INSERT INTO contest_fit_cache (contest_id, basis_type, basis_id, fit_score, reason_json, status, calculated_at, expires_at) VALUES
(1, 'PROFILE', 1, 86.00, JSON_ARRAY('제작 진행 경험', '드라마 장르 관심', '즉시 협업 가능'), 'READY', NOW() - INTERVAL 5 MINUTE, NOW() + INTERVAL 25 MINUTE),
(1, 'TEAM', 1, 91.00, JSON_ARRAY('단편 드라마 제작팀', '모집 중 상태', '서울 지역 활동'), 'READY', NOW() - INTERVAL 5 MINUTE, NOW() + INTERVAL 25 MINUTE);

INSERT INTO notification_template
(template_code, display_name, notification_type, target_type, title_template, body_template, active_yn, created_by) VALUES
('ADMIN_NOTICE_DEFAULT', '기본 운영 안내', 'ADMIN', 'ADMIN_NOTICE', 'Slate 운영 안내', '서비스 운영 안내입니다. 필요한 내용을 확인해주세요.', 'Y', 99),
('CONTEST_NOTICE', '공모전 안내', 'ADMIN', 'CONTEST', '공모전 안내', '새 공모전 또는 마감 일정이 업데이트되었습니다. 공모전 화면에서 자세히 확인해주세요.', 'Y', 99),
('MAINTENANCE_NOTICE', '점검 안내', 'SYSTEM', 'ADMIN_NOTICE', '서비스 점검 안내', '서비스 안정화를 위한 점검이 예정되어 있습니다. 작업 전 저장이 필요한 내용을 확인해주세요.', 'Y', 99);

INSERT INTO notification (recipient_user_id, sender_user_id, notification_type, title, body, target_type, target_id, read_yn, expires_at) VALUES
(1, 99, 'ADMIN', '회사 승인 검토 요청', '새 회사 계정 신청이 검토 대기 중입니다.', 'COMPANY_APPLICATION', 1, 'N', NOW() + INTERVAL 30 DAY),
(3, 1, 'MATCHING', '남산 새벽팀 추천 후보', '촬영감독 역할과 높은 적합도를 보입니다.', 'TEAM', 1, 'N', NOW() + INTERVAL 30 DAY);

INSERT INTO audit_log (actor_user_id, action_type, target_type, target_id, ip_hash, after_json) VALUES
(99, 'SEED_ADMIN_READY', 'SYSTEM', NULL, 'sample-ip-hash', JSON_OBJECT('status', 'ready')),
(1, 'MATCHING_BOOKMARK_CREATED', 'PROFILE', 3, 'sample-ip-hash', JSON_OBJECT('targetType', 'PROFILE', 'targetId', 3));

INSERT INTO operation_log (log_level, event_code, message, context_json) VALUES
('INFO', 'SEED_COMPLETED', 'Slate sample data inserted', JSON_OBJECT('unit', 'core_matching_slice'));
