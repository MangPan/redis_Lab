# Redis 적용 패턴

이 문서는 Redis Lab에서 구현한 기능을 Redis 자료구조와 운영 관점으로 다시 정리합니다.

## 자료구조별 구현 기능

| Redis 자료구조 | Redis 명령어 | 구현 기능 |
|---|---|---|
| String | `SET`, `GET`, `DEL` | 단순 key-value API |
| String + TTL | `SET EX`, `TTL`, `EXPIRE` | 인증번호, Refresh Token |
| String Counter | `INCR`, `INCRBY` | 조회수, Rate Limiting |
| SET NX EX | `SET NX EX` | 중복 조회 방지 |
| Set | `SADD`, `SREM`, `SCARD`, `SISMEMBER` | 좋아요 처리 |
| Sorted Set | `ZINCRBY`, `ZREVRANGE`, `ZSCORE`, `ZREVRANK` | 인기 게시글 랭킹 |
| List | `LPUSH`, `LREM`, `LTRIM`, `LRANGE` | 최근 검색어 |
| TTL | `TTL`, `EXPIRE`, `PERSIST` | 자동 만료 관리 |

## Redis Key 네이밍

이 프로젝트에서는 콜론(`:`) 기반 네임스페이스를 사용합니다.

```txt
auth:code:{email}
auth:refresh:user:{userId}
post:{postId}:view-count
viewed:post:{postId}:viewer:{viewerId}
post:{postId}:likes
ranking:posts
user:{userId}:recent-searches
rate-limit:user:{userId}
```

좋은 key 설계 기준:

```txt
1. 도메인 구분이 명확해야 한다.
2. 어떤 자료구조인지 추측 가능해야 한다.
3. userId, postId 같은 식별자가 포함되어야 한다.
4. TTL이 필요한 key와 유지되는 key를 구분해야 한다.
5. 운영에서 SCAN으로 찾기 쉬운 prefix를 사용한다.
```

## 실습용 Redis CLI 명령어

```redis
PING

SET name minjun
GET name
DEL name

SET temp hello EX 30
TTL temp
EXPIRE name 60

INCR counter
INCRBY counter 5

LPUSH recent:1 redis
LRANGE recent:1 0 -1
LREM recent:1 0 redis
LTRIM recent:1 0 9

SADD post:1:likes user-1
SREM post:1:likes user-1
SCARD post:1:likes
SISMEMBER post:1:likes user-1

ZINCRBY ranking:posts 3 post:1
ZREVRANGE ranking:posts 0 9 WITHSCORES
ZSCORE ranking:posts post:1
ZREVRANK ranking:posts post:1

SCAN 0 MATCH user:*
```

주의:

```txt
KEYS * 와 FLUSHDB는 운영 환경에서 위험합니다.
로컬 실습에서만 사용합니다.
```

## 운영 관점 주의점

### Redis는 원본 DB가 아니다

Redis는 빠르지만 메모리 기반 저장소입니다. 영구 저장이 가능하더라도 원본 데이터의 유일한 저장소로 쓰는 것은 신중해야 합니다.

권장 역할:

```txt
캐시
임시 상태
중복 방지
카운터
랭킹
토큰 저장
Rate Limiting
```

원본 데이터, 최종 정산값, 이벤트 로그처럼 보존과 정합성이 중요한 데이터는 별도 영구 저장소를 기준으로 설계하는 편이 안전합니다.

### TTL을 명확히 관리해야 한다

임시 데이터인데 TTL을 걸지 않으면 메모리에 계속 쌓입니다.

TTL이 필요한 예:

```txt
인증번호
Refresh Token
Rate Limit key
중복 조회 방지 key
임시 잠금 key
```

### 클라이언트 식별자를 그대로 믿으면 안 된다

이 프로젝트는 Redis 학습용이므로 `userId`, `viewerId`를 요청으로 받습니다.

실제 서비스에서는 보통 인증 후 서버에서 현재 사용자 ID를 가져와야 합니다.

```txt
Authorization: Bearer <accessToken>
-> SecurityContext
-> currentUserId
```

### 좋아요, 조회수, 랭킹은 역할 분리가 필요하다

학습용 구현:

```txt
Redis에 모든 값 저장
```

운영 설계 예:

```txt
영구 저장소:
원본 데이터, 최종 카운트, 이벤트 로그

Redis:
중복 방지, 빠른 카운터, 랭킹 캐시
```

### 대량 조회 명령을 조심해야 한다

`KEYS *`, 큰 Set의 `SMEMBERS`, 큰 List의 전체 `LRANGE`는 데이터가 많을 때 Redis에 부담을 줄 수 있습니다.

운영에서는 다음을 우선 고려합니다.

```txt
KEYS 대신 SCAN
전체 목록 대신 페이징
큰 집합은 DB 조회와 함께 사용
필요한 데이터만 짧은 TTL로 캐시
```

## 구현별 핵심 포인트

### 조회수 중복 방지

```txt
SET viewed:post:{postId}:viewer:{viewerId} 1 NX EX 600
```

성공하면 최초 조회로 보고 조회수를 증가시킵니다. 실패하면 TTL 안에 이미 조회한 사용자로 보고 증가시키지 않습니다.

### 최근 검색어

```txt
LREM key 0 keyword
LPUSH key keyword
LTRIM key 0 9
```

중복 검색어를 제거한 뒤 맨 앞에 다시 넣고, 최근 10개만 유지합니다.

### Rate Limiting

현재 구현은 Fixed Window 방식입니다.

```txt
INCR rate-limit:user:{userId}
첫 요청이면 EXPIRE
카운트가 제한값을 넘으면 차단
```

단순하고 이해하기 쉽지만 윈도우 경계에 요청이 몰릴 수 있습니다. 더 정교한 제한이 필요하면 Sliding Window, Token Bucket, Lua Script를 고려할 수 있습니다.

### Refresh Token

학습용 구현은 Refresh Token 원문을 Redis에 저장합니다.

운영에서는 다음을 검토합니다.

```txt
토큰 해시 저장
deviceId 기반 다중 기기 관리
rotation 시 이전 토큰 무효화
재사용 감지
로그아웃/전체 로그아웃 정책
```

## 기술 질문 대비

```txt
1. Redis를 왜 사용하는가?
2. Redis와 관계형 DB의 역할 차이는 무엇인가?
3. TTL은 어떤 상황에서 필요한가?
4. SET NX EX는 어디에 쓰는가?
5. Redis Set과 Sorted Set의 차이는 무엇인가?
6. Rate Limiting을 Redis로 어떻게 구현했는가?
7. Fixed Window 방식의 한계는 무엇인가?
8. Refresh Token을 Redis에 저장하는 이유는 무엇인가?
9. Redis에 좋아요를 전부 저장해도 되는가?
10. KEYS *를 운영에서 쓰면 왜 위험한가?
```

## 다음 개선 방향

```txt
1. GlobalExceptionHandler 추가
2. 429 Too Many Requests 응답 처리
3. Redis Lua Script로 원자적 Rate Limiting 구현
4. Refresh Token 해시 저장
5. deviceId 기반 다중 기기 토큰 관리
6. Testcontainers로 Redis 통합 테스트 작성
7. Docker Compose에 Spring Boot까지 포함
8. API 응답 예시 보강
9. Redis key 만료 정책 문서화
10. 대량 데이터 조회 시 페이징/SCAN 예제 추가
```
