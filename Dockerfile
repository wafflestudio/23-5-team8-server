FROM eclipse-temurin:21-jre

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        chromium chromium-driver fonts-noto-cjk && \
    rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_BIN=/usr/bin/chromedriver

WORKDIR /app
COPY build/libs/*.jar app.jar
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "app.jar"]
