# 23-5-team8-server
와플스튜디오 23.5기 8조 server

## 로컬 개발 환경 설정
아래 절차에 따라 로컬 환경을 구성하세요.

### 1. 로컬 설정 파일 생성
.example 파일(application-local.yaml.example, .env.example)을 복사합니다.

```bash
cp src/main/resources/application-local.yaml.example \
   src/main/resources/application-local.yaml

cp .env.example .env
```

- application-local.yaml
    - Spring Boot local profile 전용 값 포함
- .env
    - Docker Compose에서 사용하는 환경변수
    - MySQL 계정 및 비밀번호 포함

복사한 파일에서 change-me를 수정해 주세요.

두 파일은 .gitignore에 포함되어 있습니다. 커밋하지 않도록 주의해 주세요.

### 2. MySQL 실행

로컬 MySQL 컨테이너를 실행합니다.

```bash
docker compose up -d mysql
```

중지하려면:

```bash
docker compose down
```

### 3. IntelliJ 설정

로컬 실행 시 local 프로파일을 활성화해야 합니다.

1. Run / Debug Configurations
2. Spring Boot 선택
3. Active profiles에 "local" 입력 후 적용

### 4. Swagger UI 접속 방법

애플리케이션을 실행한 후:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
