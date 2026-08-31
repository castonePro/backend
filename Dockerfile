# ==========================================
# 1. Build Stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper 및 빌드 스크립트 복사 (의존성 캐싱)
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# 실행 권한 부여 및 의존성 다운로드 (캐시 활용)
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 애플리케이션 빌드 (테스트 제외)
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ==========================================
# 2. Runtime Stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 타임존 설정 및 필요한 유틸리티(curl 등) 설치
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 보안을 위한 Non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# 빌드 산출물(JAR) 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 소유권 변경 및 Non-root 사용자로 전환
RUN chown spring:spring app.jar
USER spring:spring

# 포트 노출
EXPOSE 8080

# JVM 실행 옵션 및 엔트리포인트 설정
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
