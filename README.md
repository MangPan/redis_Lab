# Redis Lab

Redis Lab은 Redis의 핵심 자료구조와 운영성 기능을 Spring Boot API로 직접 구현해보는 학습 프로젝트입니다.

Redis를 단순한 캐시 서버로만 보지 않고, TTL, 원자적 카운터, 중복 방지, 집합, 랭킹, 최근 목록 같은 패턴을 직접 API로 만들어보는 데 초점을 둡니다.

## 목표

- Redis String 기반 key-value 저장 흐름 이해
- TTL을 활용한 임시 데이터 관리
- 인증번호와 Refresh Token 저장 패턴 학습
- `SET NX EX` 기반 중복 요청 방지
- Set 기반 좋아요 처리
- Sorted Set 기반 랭킹 구현
- List 기반 최근 검색어 구현
- `INCR` + `EXPIRE` 기반 Rate Limiting 구현
- Redis key 네이밍과 운영 시 주의점 정리

## 기술 스택

```txt
Java 21
Spring Boot 3.5.x
Gradle
Spring Web
Spring Data Redis
Validation
Docker Compose
Redis 7.2 Alpine
```

## 빠른 실행

Redis 실행:

```bash
docker compose up -d
```

Redis 연결 확인:

```bash
docker exec -it redis-lab redis-cli
PING
```

Spring Boot 실행:

```bash
./gradlew bootRun
```

Windows PowerShell:

```bash
.\gradlew bootRun
```

애플리케이션에서 Redis 연결 확인:

```bash
curl -i http://localhost:8080/health/redis
```

## 문서

| 문서 | 내용 |
|---|---|
| [시작하기](docs/getting-started.md) | 실행 방법, Redis 접속, 설정값 |
| [API 레퍼런스](docs/api-reference.md) | 구현된 API 목록, Redis key, curl 예시 |
| [Redis 적용 패턴](docs/redis-patterns.md) | 자료구조 매핑, key 네이밍, 운영 주의점, 개선 방향 |
| [Redis CLI 기본 메모](docs/redis_cli_basic_memo.md) | `redis-cli`로 직접 확인하는 Redis 기본 명령어 |

## 구현 기능 요약

| 영역 | Redis 자료구조/명령 | 핵심 학습 포인트 |
|---|---|---|
| Redis Health | `PING` | 애플리케이션과 Redis 연결 확인 |
| String API | `SET`, `GET`, `DEL` | 단순 key-value 저장 |
| TTL String | `SET EX`, `TTL`, `EXPIRE` | 자동 만료 데이터 |
| 인증번호 | String + TTL | 일회성 코드 저장과 검증 |
| 조회수 중복 방지 | `SET NX EX`, `INCR` | 일정 시간 내 중복 증가 차단 |
| 좋아요 | Set | 중복 없는 사용자 집합 |
| 인기 랭킹 | Sorted Set | 점수 기반 정렬과 순위 조회 |
| 최근 검색어 | List | 최신순 목록, 중복 제거, 길이 제한 |
| Rate Limiting | `INCR`, `EXPIRE`, `TTL` | Fixed Window 방식 제한 |
| Refresh Token | String + TTL | 토큰 저장, 검증, 삭제, 교체 |

## 패키지 구조

```txt
com.redislab
├── health
├── string
├── code
├── view
├── like
├── ranking
├── search
├── ratelimit
└── refreshtoken
```

각 패키지는 Controller, Service, dto 중심으로 구성되어 있습니다.

## 학습 순서

1. `redis-cli`에서 String, TTL, Set, Sorted Set, List 명령어를 먼저 실행해봅니다.
2. `/health/redis`로 Spring Boot와 Redis 연결을 확인합니다.
3. String API로 `SET`, `GET`, `DEL`, `TTL` 흐름을 확인합니다.
4. 인증번호, 조회수 중복 방지, 좋아요, 랭킹, 최근 검색어 순서로 자료구조별 API를 실습합니다.
5. Rate Limiting과 Refresh Token 예제로 운영성 기능을 확인합니다.
6. [Redis 적용 패턴](docs/redis-patterns.md)을 보면서 실제 서비스에 적용할 때의 주의점을 정리합니다.

## 주의

이 프로젝트는 학습용입니다. 일부 API는 실습 편의를 위해 인증번호나 Refresh Token 원문을 응답하거나 저장합니다.

실제 서비스에서는 인증번호를 응답에 포함하지 않고, Refresh Token은 해시 저장, 기기 식별자 분리, 재사용 감지, 로그아웃 정책 등을 함께 고려해야 합니다.
