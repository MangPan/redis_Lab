# API 레퍼런스

모든 예시는 애플리케이션이 `localhost:8080`에서 실행 중이고 Redis가 `localhost:6379`에서 실행 중인 상태를 기준으로 합니다.

## Redis Health

| Method | Path | Description |
|---|---|---|
| GET | `/health/redis` | Redis 연결 확인 |

```bash
curl -i http://localhost:8080/health/redis
```

## String API

| Method | Path | Description |
|---|---|---|
| POST | `/api/strings` | key-value 저장 |
| GET | `/api/strings/{key}` | value 조회 |
| DELETE | `/api/strings/{key}` | key 삭제 |
| POST | `/api/strings/ttl` | TTL 포함 key-value 저장 |
| GET | `/api/strings/{key}/ttl` | TTL 조회 |
| POST | `/api/strings/{key}/expire` | 기존 key에 TTL 설정 |

저장:

```bash
curl -i -X POST http://localhost:8080/api/strings \
  -H "Content-Type: application/json" \
  -d '{
    "key": "name",
    "value": "minjun"
  }'
```

TTL 포함 저장:

```bash
curl -i -X POST http://localhost:8080/api/strings/ttl \
  -H "Content-Type: application/json" \
  -d '{
    "key": "code:test@example.com",
    "value": "123456",
    "ttlSeconds": 60
  }'
```

## 인증번호 API

| Method | Path | Description |
|---|---|---|
| POST | `/api/codes` | 6자리 인증번호 생성 |
| POST | `/api/codes/verify` | 인증번호 검증 |
| GET | `/api/codes/{email}` | 저장된 인증번호 확인 |
| GET | `/api/codes/{email}/ttl` | 인증번호 TTL 확인 |
| DELETE | `/api/codes/{email}` | 인증번호 삭제 |

Redis key:

```txt
auth:code:{email}
```

생성:

```bash
curl -i -X POST http://localhost:8080/api/codes \
  -H "Content-Type: application/json" \
  -d '{
    "email": "minjun@example.com"
  }'
```

검증:

```bash
curl -i -X POST http://localhost:8080/api/codes/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "minjun@example.com",
    "code": "123456"
  }'
```

학습용이므로 응답에 인증번호가 포함됩니다. 실제 서비스에서는 인증번호를 응답에 포함하면 안 됩니다.

## 조회수 중복 방지 API

| Method | Path | Description |
|---|---|---|
| POST | `/api/posts/{postId}/views` | 조회수 증가 시도 |
| GET | `/api/posts/{postId}/views` | 현재 조회수 조회 |
| GET | `/api/posts/{postId}/views/viewers/{viewerId}/ttl` | viewer 중복 방지 TTL 조회 |
| DELETE | `/api/posts/{postId}/views` | 조회수 초기화 |

Redis key:

```txt
post:{postId}:view-count
viewed:post:{postId}:viewer:{viewerId}
```

조회수 증가:

```bash
curl -i -X POST http://localhost:8080/api/posts/1/views \
  -H "Content-Type: application/json" \
  -d '{
    "viewerId": "user-1"
  }'
```

핵심 동작:

```txt
SET viewed:post:1:viewer:user-1 1 NX EX 600
성공하면 INCR post:1:view-count
실패하면 중복 조회로 판단하고 조회수를 증가시키지 않음
```

## 좋아요 API

| Method | Path | Description |
|---|---|---|
| POST | `/api/posts/{postId}/likes` | 좋아요 추가 |
| DELETE | `/api/posts/{postId}/likes` | 좋아요 취소 |
| GET | `/api/posts/{postId}/likes/count` | 좋아요 수 조회 |
| GET | `/api/posts/{postId}/likes/me?userId={userId}` | 내 좋아요 여부 확인 |
| GET | `/api/posts/{postId}/likes` | 좋아요한 사용자 목록 조회 |

Redis key:

```txt
post:{postId}:likes
```

좋아요 추가:

```bash
curl -i -X POST http://localhost:8080/api/posts/1/likes \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1"
  }'
```

Redis Set 명령어 매핑:

```txt
SADD      좋아요 추가
SREM      좋아요 취소
SCARD     좋아요 수 조회
SISMEMBER 내 좋아요 여부 확인
SMEMBERS  좋아요 사용자 목록 조회
```

## 인기 랭킹 API

| Method | Path | Description |
|---|---|---|
| POST | `/api/posts/{postId}/score` | 게시글 점수 증가 |
| GET | `/api/rankings/posts` | 게시글 랭킹 조회 |
| GET | `/api/rankings/posts/{postId}` | 특정 게시글 랭킹 상태 조회 |
| DELETE | `/api/rankings/posts` | 랭킹 초기화 |

Redis key:

```txt
ranking:posts
```

점수 증가:

```bash
curl -i -X POST http://localhost:8080/api/posts/1/score \
  -H "Content-Type: application/json" \
  -d '{
    "score": 3
  }'
```

랭킹 조회:

```bash
curl -i "http://localhost:8080/api/rankings/posts?limit=10"
```

Redis Sorted Set 명령어 매핑:

```txt
ZINCRBY   점수 증가
ZREVRANGE 높은 점수순 조회
ZSCORE    특정 member 점수 조회
ZREVRANK  높은 점수순 순위 조회
```

## 최근 검색어 API

| Method | Path | Description |
|---|---|---|
| POST | `/api/users/{userId}/searches` | 최근 검색어 추가 |
| GET | `/api/users/{userId}/searches` | 최근 검색어 조회 |
| DELETE | `/api/users/{userId}/searches/{keyword}` | 특정 검색어 삭제 |
| DELETE | `/api/users/{userId}/searches` | 최근 검색어 전체 삭제 |

Redis key:

```txt
user:{userId}:recent-searches
```

검색어 추가:

```bash
curl -i -X POST http://localhost:8080/api/users/user-1/searches \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "redis"
  }'
```

핵심 패턴:

```txt
LREM key 0 keyword
LPUSH key keyword
LTRIM key 0 9
```

## Rate Limiting API

| Method | Path | Description |
|---|---|---|
| POST | `/api/limited-actions` | 제한 대상 행동 시도 |
| GET | `/api/limited-actions/{userId}` | 현재 제한 상태 조회 |
| DELETE | `/api/limited-actions/{userId}` | 제한 상태 초기화 |

Redis key:

```txt
rate-limit:user:{userId}
```

요청:

```bash
curl -i -X POST http://localhost:8080/api/limited-actions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1"
  }'
```

현재 정책:

```txt
1분에 10회까지 허용
11번째부터 allowed=false
```

## Refresh Token API

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/refresh-tokens` | Refresh Token 생성 및 저장 |
| POST | `/api/auth/refresh-tokens/verify` | Refresh Token 검증 |
| GET | `/api/auth/refresh-tokens/{userId}` | 저장된 Refresh Token 확인 |
| DELETE | `/api/auth/refresh-tokens/{userId}` | Refresh Token 삭제 |
| POST | `/api/auth/refresh-tokens/{userId}/rotate` | Refresh Token 교체 |

Redis key:

```txt
auth:refresh:user:{userId}
```

생성:

```bash
curl -i -X POST http://localhost:8080/api/auth/refresh-tokens \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1"
  }'
```

검증:

```bash
curl -i -X POST http://localhost:8080/api/auth/refresh-tokens/verify \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1",
    "refreshToken": "..."
  }'
```

학습용이므로 refresh token 원문을 Redis에 저장합니다. 실제 서비스에서는 해시 저장, deviceId 분리, rotation 검증, 재사용 감지 등을 고려해야 합니다.
