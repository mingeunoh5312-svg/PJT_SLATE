# EC2 서버 재가동 가이드

이 문서는 Slate EC2 서버가 중지되었거나 재부팅된 뒤 서비스를 다시 올릴 때 사용하는 절차다. 실제 IP, key 경로, DB 비밀번호, API key 같은 비밀 값은 문서에 적지 않는다. 아래의 `<...>` 값은 각자 실제 값으로 바꿔서 실행한다.

## 1. EC2 인스턴스가 Stop 상태인 경우

SSH는 EC2가 꺼져 있으면 사용할 수 없다. 먼저 AWS 콘솔에서 인스턴스를 시작한다.

1. AWS Console 접속
2. EC2 > Instances 이동
3. Slate 배포 인스턴스 선택
4. Instance state > Start instance 클릭
5. Instance state가 `Running`이 될 때까지 대기
6. Public IPv4 주소 확인

주의: Elastic IP를 연결하지 않았다면 EC2를 Stop/Start 할 때 Public IP가 바뀔 수 있다. IP가 바뀌면 브라우저 접속 주소, SSH 명령, 프론트 설정, CORS 설정을 다시 확인해야 한다.

AWS CLI를 쓰는 경우:

```bash
aws ec2 start-instances --instance-ids <EC2_INSTANCE_ID> --region <AWS_REGION>
aws ec2 wait instance-running --instance-ids <EC2_INSTANCE_ID> --region <AWS_REGION>
```

## 2. 로컬 PC에서 SSH 접속

Windows PowerShell:

```powershell
ssh -i "<LOCAL_PEM_KEY_PATH>" ubuntu@<EC2_PUBLIC_IP_OR_DOMAIN>
```

예시 형식:

```powershell
ssh -i "C:\Users\<WINDOWS_USER>\.ssh\<KEY_FILE>.pem" ubuntu@<EC2_PUBLIC_IP_OR_DOMAIN>
```

접속이 안 될 때 확인할 것:

- EC2 상태가 `Running`인지 확인
- 보안 그룹 inbound에 TCP 22가 현재 접속 위치에서 허용되어 있는지 확인
- `.pem` 파일 경로가 맞는지 확인
- EC2 Public IP가 바뀌지 않았는지 확인

## 3. 서버 기본 상태 확인

EC2에 접속한 뒤 실행한다.

```bash
hostname
date
df -h
free -h
systemctl is-active mysql
systemctl is-active nginx
systemctl is-active slate.service
```

정상 기대값:

- `mysql`: `active`
- `nginx`: `active`
- `slate.service`: `active`

## 4. 서비스 수동 시작/재시작

서버는 보통 재부팅 후 자동 시작되도록 설정되어 있어야 한다. 그래도 서비스가 내려가 있으면 아래 순서로 올린다.

```bash
sudo systemctl start mysql
sudo systemctl start nginx
sudo systemctl start slate.service
```

이미 실행 중인데 반영이 안 되거나 오류 복구가 필요하면 재시작한다.

```bash
sudo systemctl restart mysql
sudo systemctl restart nginx
sudo systemctl restart slate.service
```

자동 시작 설정 확인:

```bash
systemctl is-enabled mysql
systemctl is-enabled nginx
systemctl is-enabled slate.service
```

필요 시 자동 시작 활성화:

```bash
sudo systemctl enable mysql
sudo systemctl enable nginx
sudo systemctl enable slate.service
```

## 5. 백엔드 로그 확인

```bash
journalctl -u slate.service -n 100 --no-pager
```

실시간 로그:

```bash
journalctl -u slate.service -f
```

서비스 상세 상태:

```bash
systemctl status slate.service --no-pager
```

## 6. Nginx 상태 확인

```bash
sudo nginx -t
systemctl status nginx --no-pager
sudo journalctl -u nginx -n 80 --no-pager
```

Nginx 설정을 바꾼 뒤에는:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 7. MySQL 상태 확인

```bash
systemctl status mysql --no-pager
sudo journalctl -u mysql -n 80 --no-pager
```

DB 접속 확인:

```bash
mysql -u slate_app -p slate -e "SELECT COUNT(*) AS user_count FROM user_account;"
```

비밀번호는 터미널에서 직접 입력하고 문서나 채팅에 남기지 않는다.

## 8. 애플리케이션 동작 확인

서버 내부에서 API 확인:

```bash
curl -i http://localhost:8080/api/references/genres
```

Nginx 경유 확인:

```bash
curl -i http://localhost/api/references/genres
```

외부 PC 브라우저에서 확인:

```text
http://<EC2_PUBLIC_IP_OR_DOMAIN>/
```

데모 접속 코드 gate가 켜져 있으면 첫 화면에서 접속 코드 입력 화면이 보여야 한다.

## 9. 서버가 켜졌지만 이미지가 안 보일 때

업로드 루트와 seed 이미지 파일을 확인한다.

```bash
sudo grep '^SLATE_UPLOAD_DIR=' /etc/slate/slate.env
find <SLATE_UPLOAD_DIR>/images/seed -type f | wc -l
test -f <SLATE_UPLOAD_DIR>/images/seed/profile/profile_cdd_hyunseo_pd.png && echo "seed image ok"
```

DB 이미지 경로를 다시 적용해야 하면 Slate SQL 폴더에서 실행한다.

```bash
cd /opt/slate
mysql -u slate_app -p slate < sql/27_apply_generated_dummy_images.sql
```

## 10. 콘테스트코리아 크롤러가 비활성화된 경우

현재 값 확인:

```bash
sudo grep '^CONTESTKOREA_CRAWLER_ENABLED=' /etc/slate/slate.env
```

활성화:

```bash
sudo cp /etc/slate/slate.env /etc/slate/slate.env.bak
sudo sed -i 's/^CONTESTKOREA_CRAWLER_ENABLED=.*/CONTESTKOREA_CRAWLER_ENABLED=true/' /etc/slate/slate.env
sudo systemctl restart slate.service
```

확인:

```bash
systemctl is-active slate.service
sudo grep '^CONTESTKOREA_CRAWLER_ENABLED=' /etc/slate/slate.env
```

## 11. 빠른 복구 체크리스트

```bash
systemctl is-active mysql
systemctl is-active nginx
systemctl is-active slate.service
curl -i http://localhost/api/references/genres
journalctl -u slate.service -n 50 --no-pager
```

위 명령에서 `slate.service`가 `active`이고 `/api/references/genres`가 응답하면 백엔드와 프록시는 기본적으로 살아 있는 상태다.
