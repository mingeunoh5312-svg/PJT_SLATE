# EC2 운영 정보

이 문서는 Slate EC2 배포 운영에 필요한 경로, 서비스, 환경변수, 점검 명령을 정리한다. 실제 secret, DB 비밀번호, API key, EC2 IP, 접속 코드, 개인 key 경로는 문서에 기록하지 않는다. `<...>`로 표시된 값은 운영자가 실제 값으로 교체해서 사용한다.

## 배포 구성

| 항목 | 값 |
|---|---|
| OS | Ubuntu Server |
| Web server | Nginx |
| Backend | Spring Boot jar |
| Backend service | `slate.service` |
| Backend port | `localhost:8080` |
| Frontend | Vite build 정적 파일 |
| DB | MySQL 8 |
| DB name | `slate` |
| DB user | `slate_app` |
| Upload storage | `<SLATE_UPLOAD_DIR>` |
| Public entry | `http://<EC2_PUBLIC_IP_OR_DOMAIN>/` |

권장 요청 흐름:

```text
Browser
  -> Nginx :80
    -> static frontend files
    -> /api/* proxy to localhost:8080
      -> Spring Boot
        -> MySQL localhost:3306
        -> Upload files under <SLATE_UPLOAD_DIR>
```

## 주요 서버 경로

| 용도 | 경로 |
|---|---|
| 애플리케이션 작업 경로 | `/opt/slate` |
| 백엔드 jar | `/opt/slate/slate-backend.jar` |
| SQL 파일 | `/opt/slate/sql` |
| 백엔드 환경파일 | `/etc/slate/slate.env` |
| systemd 서비스 | `/etc/systemd/system/slate.service` |
| 업로드 루트 | `<SLATE_UPLOAD_DIR>` |
| seed 이미지 경로 | `<SLATE_UPLOAD_DIR>/images/seed` |
| Nginx 설정 | `/etc/nginx/sites-available/<NGINX_SITE_NAME>` |
| Nginx enabled link | `/etc/nginx/sites-enabled/<NGINX_SITE_NAME>` |

현재 서버에서 실제 값 확인:

```bash
systemctl cat slate.service
sudo grep '^SLATE_UPLOAD_DIR=' /etc/slate/slate.env
sudo nginx -T | grep -E 'server_name|root|proxy_pass' -n
```

## 서비스 관리

상태 확인:

```bash
systemctl status slate.service --no-pager
systemctl status nginx --no-pager
systemctl status mysql --no-pager
```

시작:

```bash
sudo systemctl start mysql
sudo systemctl start nginx
sudo systemctl start slate.service
```

재시작:

```bash
sudo systemctl restart mysql
sudo systemctl restart nginx
sudo systemctl restart slate.service
```

자동 시작:

```bash
sudo systemctl enable mysql
sudo systemctl enable nginx
sudo systemctl enable slate.service
```

## 로그 확인

백엔드:

```bash
journalctl -u slate.service -n 100 --no-pager
journalctl -u slate.service -f
```

Nginx:

```bash
sudo journalctl -u nginx -n 100 --no-pager
sudo tail -n 100 /var/log/nginx/access.log
sudo tail -n 100 /var/log/nginx/error.log
```

MySQL:

```bash
sudo journalctl -u mysql -n 100 --no-pager
```

## 환경변수 정책

실제 값은 `/etc/slate/slate.env`에만 둔다. 이 파일은 Git에 커밋하지 않는다.

필수 또는 주요 변수:

```dotenv
SPRING_PROFILES_ACTIVE=prod
SLATE_DB_URL=jdbc:mysql://localhost:3306/slate?serverTimezone=Asia/Seoul&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
SLATE_DB_USERNAME=slate_app
SLATE_DB_PASSWORD=<DB_PASSWORD>
SLATE_JWT_SECRET=<LONG_RANDOM_JWT_SECRET>
SLATE_JWT_EXPIRATION_MINUTES=120
SLATE_UPLOAD_DIR=<SLATE_UPLOAD_DIR>
SLATE_FFPROBE_PATH=<FFPROBE_PATH_OR_ffprobe>
SLATE_AUDIT_IP_HASH_SALT=<LONG_RANDOM_AUDIT_SALT>
SLATE_CORS_ALLOWED_ORIGINS=http://<EC2_PUBLIC_IP_OR_DOMAIN>,https://<DOMAIN_IF_USED>
SLATE_DEMO_ACCESS_ENABLED=true
SLATE_DEMO_ACCESS_CODE=<DEMO_ACCESS_CODE>
KOBIS_API_KEY=<KOBIS_API_KEY>
KOBIS_BASE_URL=http://www.kobis.or.kr/kobisopenapi/webservice/rest
YOUTUBE_API_KEY=<YOUTUBE_API_KEY>
YOUTUBE_BASE_URL=https://www.googleapis.com/youtube/v3
OPENAI_API_KEY=<OPENAI_API_KEY>
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=<OPENAI_MODEL>
CONTESTKOREA_CRAWLER_ENABLED=true
CONTESTKOREA_BASE_URL=https://www.contestkorea.com
CONTESTKOREA_LIST_PATH=/sub/list.php
CONTESTKOREA_CATEGORY_CODE=031210001
CONTESTKOREA_INT_GBN=1
CONTESTKOREA_USER_AGENT=<CRAWLER_USER_AGENT_WITH_CONTACT_POLICY>
CONTESTKOREA_REQUEST_DELAY_MILLIS=1500
CONTESTKOREA_CONNECT_TIMEOUT_MILLIS=5000
CONTESTKOREA_READ_TIMEOUT_MILLIS=10000
CONTESTKOREA_MAX_PAGES=10
CONTESTKOREA_MAX_ITEMS_PER_RUN=100
CONTESTKOREA_POSTER_DOWNLOAD_ENABLED=true
CONTESTKOREA_REQUIRED_PERMISSION_TEXT=콘테스트코리아 출처 표기
CONTESTKOREA_SOURCE_NAME=CONTESTKOREA
CONTESTKOREA_SOURCE_ATTRIBUTION=출처: 콘테스트코리아
```

환경파일 수정 전 백업:

```bash
sudo cp /etc/slate/slate.env /etc/slate/slate.env.bak
```

수정 후 반영:

```bash
sudo systemctl restart slate.service
```

## 프론트엔드 운영 값

프론트 `.env.production`은 빌드 시점에 반영된다. 값을 바꿨으면 다시 빌드해서 Nginx 정적 파일 위치에 배포해야 한다.

```dotenv
VITE_API_BASE_URL=
VITE_DEMO_ACCESS_GATE=true
VITE_KAKAO_MAP_APP_KEY=<KAKAO_MAP_JAVASCRIPT_KEY>
```

단일 EC2 + Nginx `/api` 프록시 구성에서는 `VITE_API_BASE_URL`을 비워두는 것이 권장된다.

## DB 관리

DB 접속:

```bash
mysql -u slate_app -p slate
```

간단 확인:

```bash
mysql -u slate_app -p slate -e "SHOW TABLES;"
mysql -u slate_app -p slate -e "SELECT COUNT(*) AS user_count FROM user_account;"
```

백업:

```bash
mkdir -p ~/slate-db-backups
mysqldump -u slate_app -p slate > ~/slate-db-backups/slate_$(date +%Y%m%d_%H%M%S).sql
```

복구는 기존 DB 상태를 덮어쓸 수 있으므로 실행 전 백업을 먼저 만든다.

```bash
mysql -u slate_app -p slate < <BACKUP_FILE.sql>
```

## 초기 SQL 적용 순서

빈 DB에 초기 적용할 때 사용하는 순서다. 운영 DB에 반복 실행하면 중복 또는 삭제 문제가 생길 수 있으므로 주의한다.

```bash
cd /opt/slate
mysql -u slate_app -p slate < sql/01_schema.sql
mysql -u slate_app -p slate < sql/02_seed_reference.sql
mysql -u slate_app -p slate < sql/27_seed_korea_regions.sql
mysql -u slate_app -p slate < sql/03_seed_sample_data.sql
mysql -u slate_app -p slate < sql/04_youtube_metadata_schema.sql
mysql -u slate_app -p slate < sql/05_seed_ai_matching_dummy_data.sql
mysql -u slate_app -p slate < sql/06_follow_schema.sql
mysql -u slate_app -p slate < sql/07_seed_verified_portfolio_ui_demo.sql
mysql -u slate_app -p slate < sql/08_portfolio_credit_name_schema.sql
mysql -u slate_app -p slate < sql/09_entity_image_schema.sql
mysql -u slate_app -p slate < sql/10_board_full_integration_schema.sql
mysql -u slate_app -p slate < sql/11_board_search_genre_period_schema.sql
mysql -u slate_app -p slate < sql/12_contest_image_schema.sql
mysql -u slate_app -p slate < sql/13_contest_search_filter_schema.sql
mysql -u slate_app -p slate < sql/14_remove_contest_benefit_extra_schema.sql
mysql -u slate_app -p slate < sql/15_contest_crawl_source_schema.sql
mysql -u slate_app -p slate < sql/16_contest_official_link_cleanup.sql
mysql -u slate_app -p slate < sql/17_demo_access_code_management_schema.sql
mysql -u slate_app -p slate < sql/18_seed_connected_demo_data.sql
mysql -u slate_app -p slate < sql/21_seed_connected_demo_volume_data.sql
mysql -u slate_app -p slate < sql/24_location_recommendation_schema.sql
mysql -u slate_app -p slate < sql/26_remove_location_candidate_plan_link.sql
mysql -u slate_app -p slate < sql/27_apply_generated_dummy_images.sql
```

초기화 파일은 운영 DB에서 실수로 실행하지 않는다.

```text
sql/99_reset.sql
sql/20_rollback_connected_demo_data.sql
sql/23_rollback_connected_demo_volume_data.sql
```

## Seed 이미지 운영

DB에는 `images/seed/...` 형태의 상대경로가 저장된다. 실제 파일은 `<SLATE_UPLOAD_DIR>/images/seed/...` 아래에 있어야 한다.

확인:

```bash
sudo grep '^SLATE_UPLOAD_DIR=' /etc/slate/slate.env
find <SLATE_UPLOAD_DIR>/images/seed -type f | wc -l
test -f <SLATE_UPLOAD_DIR>/images/seed/profile/profile_cdd_hyunseo_pd.png && echo "seed image ok"
```

이미지 ZIP을 서버에 올린 뒤 압축 해제:

```bash
sudo mkdir -p <SLATE_UPLOAD_DIR>
sudo unzip -o /tmp/slate-seed-images.zip -d <SLATE_UPLOAD_DIR>
sudo find <SLATE_UPLOAD_DIR>/images/seed -type d -exec chmod 755 {} \;
sudo find <SLATE_UPLOAD_DIR>/images/seed -type f -exec chmod 644 {} \;
```

DB 이미지 경로 재적용:

```bash
cd /opt/slate
mysql -u slate_app -p slate < sql/27_apply_generated_dummy_images.sql
```

## 콘테스트코리아 크롤러

활성화 여부:

```bash
sudo grep '^CONTESTKOREA_CRAWLER_ENABLED=' /etc/slate/slate.env
```

활성화:

```bash
sudo cp /etc/slate/slate.env /etc/slate/slate.env.bak
sudo sed -i 's/^CONTESTKOREA_CRAWLER_ENABLED=.*/CONTESTKOREA_CRAWLER_ENABLED=true/' /etc/slate/slate.env
sudo systemctl restart slate.service
```

운영 전 확인할 것:

- 요청 간격이 과도하지 않은지 확인
- 출처 표기가 화면에 유지되는지 확인
- 관리자 권한 계정으로만 실행되는지 확인
- 외부 사이트 정책 변경 시 수집을 중단하고 재검토

## 배포 갱신 절차

백엔드 jar 교체 예시:

```bash
sudo systemctl stop slate.service
sudo cp /opt/slate/slate-backend.jar /opt/slate/slate-backend.jar.bak
sudo cp <NEW_BACKEND_JAR_PATH> /opt/slate/slate-backend.jar
sudo chown slate:slate /opt/slate/slate-backend.jar
sudo systemctl start slate.service
```

프론트 정적 파일 교체 예시:

```bash
sudo mkdir -p <FRONTEND_WEB_ROOT>
sudo rsync -av --delete <NEW_FRONTEND_DIST_DIR>/ <FRONTEND_WEB_ROOT>/
sudo nginx -t
sudo systemctl reload nginx
```

## 보안 그룹과 포트

권장 inbound:

| Port | 용도 | 공개 범위 |
|---:|---|---|
| 22 | SSH | 운영자 IP만 허용 권장 |
| 80 | HTTP | 공개 |
| 443 | HTTPS | HTTPS 적용 시 공개 |

외부에 직접 열지 않는 포트:

| Port | 용도 |
|---:|---|
| 8080 | Spring Boot 내부 포트 |
| 3306 | MySQL 내부 포트 |

## 운영 점검 명령 모음

```bash
df -h
free -h
systemctl is-active mysql
systemctl is-active nginx
systemctl is-active slate.service
curl -i http://localhost/api/references/genres
journalctl -u slate.service -n 50 --no-pager
sudo nginx -t
```

## 장애 대응 원칙

1. 먼저 현재 상태를 확인한다.
2. 로그를 본다.
3. 설정 파일을 수정하기 전 백업한다.
4. secret 값을 채팅, 문서, Git에 남기지 않는다.
5. DB 초기화/rollback SQL은 운영 DB에서 실행 전 반드시 백업한다.
6. EC2 Stop/Start 후 Public IP 변경 여부를 확인한다.
