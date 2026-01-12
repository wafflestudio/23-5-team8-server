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
(배포)15.164.49.159
- Swagger UI: http://15.164.49.159/swagger-ui/index.html
- OpenAPI JSON: http://15.164.49.159/v3/api-docs
(로컬)애플리케이션을 실행한 후:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 강의 데이터 import 절차

강의 데이터는 서울대학교 수강신청 사이트(https://sugang.snu.ac.kr)에서 제공하는 엑셀(.xls) 파일을 직접 업로드하여 적재합니다.
이는 매 학기 수동 작업으로 이루어지며, frontend와 연동되지 않습니다.

### 1. 준비
- 수강신청 사이트에서 검색 필터를 설정하지 않고 전체 강의 검색 결과를 얻어서 엑셀(.xls) 파일을 준비합니다.
- 엑셀 파일은 다음 조건을 만족하는 것을 전제로 합니다.
  - 파일 형식: .xls
  - column header가 3행에 존재

### 2. 서버 실행

### 3. Import API 호출
아래 API를 통해 강의 데이터를 적재합니다.
```bash
POST /api/courses/import
```
- Content type: multipart/form-data
- Parameters:
  - year: 연도
  - semester: 학기 (SPRING, SUMMER, FALL, WINTER)
  - file: 강의 엑셀 파일 (.xls)

적재 예시(curl):
```bash
curl -X POST "http://localhost:8080/api/courses/import?year=2026&semester=SPRING" \
  -F "file=@courses.xls"
```