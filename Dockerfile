# Java 21 Temurin JRE (실행 전용 경량화 버전) 사용
FROM eclipse-temurin:21-jre

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY build/libs/*.jar app.jar

# 타임존 서울로 설정
ENV TZ=Asia/Seoul

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]