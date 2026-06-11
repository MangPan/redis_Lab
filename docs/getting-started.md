# 시작하기

## 요구사항

- Java 21
- Docker 또는 Docker Desktop
- Gradle Wrapper 사용 가능 환경

## Redis 실행

프로젝트 루트의 `docker-compose.yml`은 Redis 컨테이너 하나를 실행합니다.

```yml
services:
  redis:
    image: redis:7.2-alpine
    container_name: redis-lab
    ports:
      - "6379:6379"
    restart: unless-stopped
```

실행:

```bash
docker compose up -d
```

상태 확인:

```bash
docker compose ps
```

Redis CLI 접속:

```bash
docker exec -it redis-lab redis-cli
```

연결 확인:

```redis
PING
```

정상 응답:

```redis
PONG
```

## Spring Boot 실행

macOS/Linux:

```bash
./gradlew bootRun
```

Windows PowerShell:

```bash
.\gradlew bootRun
```

## Redis 연결 확인 API

```bash
curl -i http://localhost:8080/health/redis
```

응답 예시:

```json
{
  "status": "UP",
  "message": "Redis connection successful"
}
```

## Redis 설정

`src/main/resources/application.properties`:

```properties
spring.application.name=redis-lab

spring.data.redis.host=localhost
spring.data.redis.port=6379
```

현재 구조에서는 Spring Boot는 로컬에서 실행하고 Redis만 Docker 컨테이너로 실행합니다.

Spring Boot도 Docker Compose 내부에서 함께 실행하는 구조로 바꾸면 Redis host는 `localhost`가 아니라 Compose 서비스 이름인 `redis`를 사용해야 합니다.

## 자주 쓰는 로컬 명령

Redis 컨테이너 중지:

```bash
docker compose down
```

Redis CLI에서 현재 DB 초기화:

```redis
FLUSHDB
```

주의:

```txt
FLUSHDB는 현재 Redis DB의 데이터를 모두 삭제합니다.
로컬 실습 환경에서만 사용하세요.
```
