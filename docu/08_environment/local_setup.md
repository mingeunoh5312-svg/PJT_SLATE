# 로컬 설정

## 기준

| 항목 | 기준 |
|---|---|
| 작업 루트 | `<SLATE_ROOT>` |
| Backend | `backend` |
| Frontend | `frontend` |
| SQL | `sql` |
| Backend URL | `http://localhost:8080` |
| Frontend URL | `http://127.0.0.1:5174/` |
| DB | `slate` |

## 준비

1. JDK 17을 사용한다.
2. Maven을 사용할 수 있게 한다.
3. Node.js `>=20.19.0`을 사용한다.
4. MySQL 8을 실행한다.
5. `backend/src/main/resources/application-local.yml`은 example에서 복사하되 실제값은 커밋하지 않는다.
6. `uploads` 디렉터리는 로컬에서 생성한다.

## DB 적용

```powershell
mysql -u root -p < sql/00_create_database.sql
mysql -u slate_app -p slate < sql/01_schema.sql
mysql -u slate_app -p slate < sql/02_seed_reference.sql
mysql -u slate_app -p slate < sql/03_seed_sample_data.sql
```

## 백엔드 실행

```powershell
cd backend
mvn -DskipTests compile
mvn spring-boot:run
```

기본 확인:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/references/genres
```

## 프론트 실행

```powershell
cd frontend
npm install
npm run build
npm run dev
```

브라우저:

```text
http://127.0.0.1:5174/
```

## 데모 접속 코드 로컬 확인

로컬에서 배포 데모 gate를 확인하려면 backend와 frontend 설정을 함께 켠다.

```dotenv
SLATE_DEMO_ACCESS_ENABLED=true
SLATE_DEMO_ACCESS_CODE=CHANGE_ME
VITE_DEMO_ACCESS_GATE=true
```

## 샘플 계정

모든 샘플 계정 비밀번호는 `slate1234`로 문서에 기록되어 있다. 배포 데모에는 샘플 계정을 포함하되, Slate 웹 페이지 접근 전에 접속 코드 gate를 둔다. 운영 전환 시에는 데모 seed와 운영 seed를 분리한다.

| 용도 | 아이디 |
|---|---|
| 일반/팀장 | `leader` |
| 팀원 | `camera` |
| 관리자 | `admin` |
| 승인 대기 회사 | `company` |
| 승인 완료 회사 | `approved-company` |

## 참조 경로

- `backend/src/main/resources/application-local.yml.example`
- `backend/.env.example`
- `frontend/.env.example`
- `frontend/package.json`
- `frontend/vite.config.js`
- `sql`
- `docu/03_mvp_scope/mvp_decisions.md`
