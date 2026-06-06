# Redis CLI 기본 메모

## 0. 목표

이 메모는 Redis를 Spring Boot에 붙이기 전에 `redis-cli`에서 Redis의 기본 자료형과 명령어를 직접 확인하기 위한 정리 문서다.

Redis는 단순한 key-value 저장소처럼 보이지만, 실제로는 여러 자료구조를 제공한다.

핵심 자료형:

- String
- Hash
- List
- Set
- Sorted Set
- Bitmap
- HyperLogLog
- Stream

Redis Lab 초반에는 String, Hash, List, Set, Sorted Set까지만 제대로 익혀도 충분하다.

---

## 1. Redis CLI 접속

Docker Compose로 Redis를 실행한 상태에서 접속한다.

```bash
docker exec -it redis-lab redis-cli
```

접속 확인:

```redis
PING
```

응답:

```redis
PONG
```

종료:

```redis
exit
```

---

## 2. 공통 Key 명령어

Redis의 모든 데이터는 key를 기준으로 저장된다.

### key 존재 확인

```redis
EXISTS name
```

- 존재하면 `1`
- 없으면 `0`

### key 삭제

```redis
DEL name
```

### key 이름 변경

```redis
RENAME old-key new-key
```

### key 목록 조회

```redis
KEYS *
```

주의:

```txt
KEYS * 는 운영 환경에서 사용하면 위험하다.
데이터가 많으면 Redis를 오래 붙잡을 수 있다.
운영에서는 SCAN을 사용한다.
```

### 운영 친화적인 key 탐색

```redis
SCAN 0
SCAN 0 MATCH user:*
SCAN 0 MATCH post:* COUNT 10
```

### key 타입 확인

```redis
TYPE name
```

### 전체 삭제

현재 DB 전체 삭제:

```redis
FLUSHDB
```

모든 DB 전체 삭제:

```redis
FLUSHALL
```

주의:

```txt
FLUSHDB / FLUSHALL은 실수하면 데이터가 전부 날아간다.
로컬 실습에서만 사용한다.
```

---

## 3. TTL / Expire

TTL은 Redis를 쓰는 가장 큰 이유 중 하나다.

### 만료 시간 설정

```redis
SET auth:code:test@example.com 123456
EXPIRE auth:code:test@example.com 300
```

300초 뒤 삭제된다.

### 저장과 동시에 만료 설정

```redis
SET auth:code:test@example.com 123456 EX 300
```

### TTL 확인

```redis
TTL auth:code:test@example.com
```

응답 의미:

```txt
양수: 만료까지 남은 초
-1: key는 있지만 만료 시간이 없음
-2: key가 없음
```

### 만료 제거

```redis
PERSIST auth:code:test@example.com
```

### 밀리초 단위 TTL

```redis
SET temp:value hello PX 5000
PTTL temp:value
```

---

## 4. String

String은 Redis의 가장 기본 자료형이다.

사용처:

- 단순 key-value
- 인증번호
- access token / refresh token
- 카운터
- feature flag
- 임시 상태값

### 저장 / 조회

```redis
SET name minjun
GET name
```

### 여러 개 저장 / 조회

```redis
MSET user:1:name minjun user:1:role USER
MGET user:1:name user:1:role
```

### 없을 때만 저장

```redis
SETNX lock:job:1 running
```

또는:

```redis
SET lock:job:1 running NX
```

TTL까지 같이 설정:

```redis
SET lock:job:1 running NX EX 30
```

이 패턴은 중복 요청 방지, 락, 조회수 중복 방지에 자주 쓰인다.

### 있을 때만 수정

```redis
SET name changed XX
```

### 카운터 증가

```redis
SET post:1:view-count 0
INCR post:1:view-count
INCRBY post:1:view-count 5
GET post:1:view-count
```

### 감소

```redis
DECR post:1:view-count
DECRBY post:1:view-count 2
```

### 실수형 증가

```redis
SET score 1.5
INCRBYFLOAT score 0.7
```

---

## 5. Hash

Hash는 하나의 key 안에 field-value를 저장하는 자료형이다.

사용처:

- 사용자 요약 정보
- 게시글 요약 정보
- 상품 캐시
- 객체 형태 데이터 캐시

예시 key:

```txt
user:1
post:10
combo:15
```

### field 저장

```redis
HSET user:1 name minjun
HSET user:1 role USER
HSET user:1 age 25
```

여러 field 한 번에 저장:

```redis
HSET user:1 name minjun role USER age 25
```

### field 조회

```redis
HGET user:1 name
```

### 전체 조회

```redis
HGETALL user:1
```

### 여러 field 조회

```redis
HMGET user:1 name role
```

### field 존재 확인

```redis
HEXISTS user:1 name
```

### field 삭제

```redis
HDEL user:1 age
```

### field 수 확인

```redis
HLEN user:1
```

### 숫자 field 증가

```redis
HINCRBY user:1 loginCount 1
HINCRBY user:1 loginCount 5
```

실수형 증가:

```redis
HINCRBYFLOAT user:1 score 1.5
```

주의:

```txt
Hash는 객체 캐시에 좋지만, JPA Entity를 통째로 Redis Hash에 저장하려는 습관은 좋지 않다.
캐시할 DTO를 명확히 정해서 저장하는 편이 안전하다.
```

---

## 6. List

List는 순서가 있는 문자열 목록이다.

사용처:

- 최근 검색어
- 최근 본 게시글
- 간단한 큐
- 로그성 데이터
- 타임라인 일부

Redis List는 양쪽 끝에서 데이터를 넣고 뺄 수 있다.

### 왼쪽에 추가

```redis
LPUSH recent:searches:1 redis
LPUSH recent:searches:1 spring
LPUSH recent:searches:1 docker
```

### 오른쪽에 추가

```redis
RPUSH queue:jobs job1
RPUSH queue:jobs job2
```

### 범위 조회

```redis
LRANGE recent:searches:1 0 -1
LRANGE recent:searches:1 0 9
```

### 길이 확인

```redis
LLEN recent:searches:1
```

### 왼쪽/오른쪽에서 꺼내기

```redis
LPOP recent:searches:1
RPOP recent:searches:1
```

### 목록 길이 제한

최근 검색어 10개만 유지:

```redis
LPUSH recent:searches:1 redis
LTRIM recent:searches:1 0 9
```

### 특정 값 제거

```redis
LREM recent:searches:1 0 redis
```

주의:

```txt
List는 중복을 허용한다.
최근 검색어에서 중복 제거를 원하면 LREM 후 LPUSH 하는 방식이 자주 쓰인다.
```

패턴:

```redis
LREM recent:searches:1 0 redis
LPUSH recent:searches:1 redis
LTRIM recent:searches:1 0 9
```

---

## 7. Set

Set은 중복 없는 집합이다.

사용처:

- 좋아요 누른 사용자 목록
- 게시글 조회한 사용자 목록
- 팔로워 id 집합
- 태그 집합
- 중복 체크

### 추가

```redis
SADD post:1:likes user:1
SADD post:1:likes user:2
SADD post:1:likes user:1
```

같은 값을 여러 번 넣어도 중복 저장되지 않는다.

### 전체 조회

```redis
SMEMBERS post:1:likes
```

### 포함 여부 확인

```redis
SISMEMBER post:1:likes user:1
```

### 개수 확인

```redis
SCARD post:1:likes
```

### 삭제

```redis
SREM post:1:likes user:1
```

### 랜덤 조회

```redis
SRANDMEMBER post:1:likes
```

### 집합 연산

```redis
SADD user:1:tags redis spring java
SADD user:2:tags redis docker aws
```

교집합:

```redis
SINTER user:1:tags user:2:tags
```

합집합:

```redis
SUNION user:1:tags user:2:tags
```

차집합:

```redis
SDIFF user:1:tags user:2:tags
```

주의:

```txt
Set은 중복 방지에 강하다.
하지만 순서나 랭킹이 필요하면 Sorted Set을 써야 한다.
```

---

## 8. Sorted Set

Sorted Set은 중복 없는 값(member)에 점수(score)를 붙인 자료형이다.

사용처:

- 인기 게시글 랭킹
- 실시간 검색어
- 게임 점수판
- 인기 태그
- 시간 가중 랭킹
- 우선순위 큐 비슷한 구조

### 점수 추가

```redis
ZADD ranking:posts 10 post:1
ZADD ranking:posts 30 post:2
ZADD ranking:posts 20 post:3
```

### 점수 증가

```redis
ZINCRBY ranking:posts 5 post:1
```

### 낮은 점수부터 조회

```redis
ZRANGE ranking:posts 0 -1 WITHSCORES
```

### 높은 점수부터 조회

```redis
ZREVRANGE ranking:posts 0 -1 WITHSCORES
```

상위 10개:

```redis
ZREVRANGE ranking:posts 0 9 WITHSCORES
```

### 특정 member 점수 확인

```redis
ZSCORE ranking:posts post:1
```

### 순위 확인

낮은 점수 기준:

```redis
ZRANK ranking:posts post:1
```

높은 점수 기준:

```redis
ZREVRANK ranking:posts post:1
```

주의:

```txt
Redis 순위는 0부터 시작한다.
사용자에게 보여줄 때는 +1 해서 보여주는 경우가 많다.
```

### 점수 범위 조회

```redis
ZRANGEBYSCORE ranking:posts 10 30 WITHSCORES
```

### 삭제

```redis
ZREM ranking:posts post:1
```

### 랭킹 초기화

```redis
DEL ranking:posts
```

HotTag에 연결하면:

```txt
ranking:combos:daily
ranking:combos:weekly
ranking:combos:all
```

점수 예시:

```txt
조회 +1
공유 +3
좋아요 +2
SNS 클릭 +5
```

Redis 명령 예시:

```redis
ZINCRBY ranking:combos:daily 1 combo:1
ZINCRBY ranking:combos:daily 3 combo:1
ZREVRANGE ranking:combos:daily 0 9 WITHSCORES
```

---

## 9. Bitmap

Bitmap은 String을 bit 단위로 다루는 기능이다.

사용처:

- 출석 체크
- 일별 방문 여부
- 기능 사용 여부
- 매우 많은 boolean 상태 저장

### bit 설정

```redis
SETBIT attendance:2026-06 userIndex 1
```

실제 예시:

```redis
SETBIT attendance:2026-06-06 1001 1
```

### bit 조회

```redis
GETBIT attendance:2026-06-06 1001
```

### 1인 bit 개수 확인

```redis
BITCOUNT attendance:2026-06-06
```

주의:

```txt
Bitmap은 userId가 너무 큰 숫자면 메모리 낭비가 생길 수 있다.
실서비스에서는 userId를 그대로 offset으로 쓰기보다 별도 index 매핑이 필요할 수 있다.
```

---

## 10. HyperLogLog

HyperLogLog는 대략적인 고유 개수를 세는 자료형이다.

사용처:

- 일일 고유 방문자 수
- 고유 조회자 수
- 고유 검색 사용자 수

정확한 값이 아니라 근사값이다.

### 추가

```redis
PFADD uv:2026-06-06 user:1
PFADD uv:2026-06-06 user:2
PFADD uv:2026-06-06 user:1
```

### 고유 개수 조회

```redis
PFCOUNT uv:2026-06-06
```

### 여러 날짜 합산

```redis
PFMERGE uv:2026-06-week uv:2026-06-01 uv:2026-06-02 uv:2026-06-03
PFCOUNT uv:2026-06-week
```

주의:

```txt
정확한 명단이 필요하면 Set을 사용한다.
고유 개수만 대략 알면 되면 HyperLogLog가 좋다.
```

---

## 11. Stream

Stream은 Redis의 로그/이벤트 스트림 자료형이다.

사용처:

- 이벤트 로그
- 비동기 작업 큐
- 알림 처리
- 주문 이벤트
- 조회 이벤트 수집

초반에는 깊게 하지 않아도 된다.

### 이벤트 추가

```redis
XADD events:views * postId 1 userId user:1
```

### 이벤트 조회

```redis
XRANGE events:views - +
```

최근 N개:

```redis
XREVRANGE events:views + - COUNT 10
```

주의:

```txt
Stream은 강력하지만 초반 Redis Lab에서는 뒤로 미룬다.
먼저 String, Hash, List, Set, Sorted Set을 익히는 게 우선이다.
```

---

## 12. Pub/Sub

Pub/Sub은 발행/구독 기능이다.

사용처:

- 실시간 알림
- 채팅
- 서버 간 간단한 메시지 전달

터미널 1:

```redis
SUBSCRIBE news
```

터미널 2:

```redis
PUBLISH news "hello redis"
```

주의:

```txt
Redis Pub/Sub은 메시지를 저장하지 않는다.
구독자가 없을 때 발행된 메시지는 사라진다.
이벤트 보존이 필요하면 Stream을 고려한다.
```

---

## 13. 트랜잭션

Redis 트랜잭션은 `MULTI`, `EXEC`로 처리한다.

```redis
MULTI
SET user:1:name minjun
INCR user:1:login-count
EXEC
```

취소:

```redis
DISCARD
```

주의:

```txt
Redis 트랜잭션은 관계형 DB 트랜잭션과 다르다.
복잡한 롤백 개념보다는 명령 묶음 실행에 가깝게 이해하는 편이 좋다.
```

---

## 14. 원자적 처리

Redis의 많은 단일 명령은 원자적으로 실행된다.

예:

```redis
INCR post:1:view-count
SADD post:1:likes user:1
ZINCRBY ranking:posts 1 post:1
```

이 덕분에 여러 요청이 동시에 들어와도 단일 명령 기준으로 값이 꼬일 가능성이 낮다.

조회수 중복 방지에서는 다음 패턴이 중요하다.

```redis
SET viewed:post:1:user:1 1 NX EX 600
```

성공하면 처음 조회:

```txt
OK
```

실패하면 이미 조회함:

```txt
nil
```

---

## 15. Key 네이밍 규칙

Redis key는 일관성이 중요하다.

추천 형식:

```txt
domain:id:purpose
domain:purpose:id
```

예시:

```txt
user:1:profile
post:1:view-count
post:1:likes
viewed:post:1:user:1
ranking:posts:daily
auth:refresh:user:1
auth:code:test@example.com
rate-limit:user:1
```

주의:

```txt
콜론(:)은 Redis에서 특별한 문법은 아니지만, 관례적으로 namespace 구분에 많이 쓴다.
```

---

## 16. Redis를 언제 쓰면 좋은가

좋은 사용처:

```txt
- TTL이 필요한 데이터
- 빠른 카운터
- 중복 방지
- 랭킹
- 캐시
- 인증번호
- refresh token 저장
- rate limiting
- 최근 검색어
- 좋아요 여부
- 실시간성 통계
```

애매하거나 피해야 하는 사용처:

```txt
- 영구 보존이 필요한 핵심 원본 데이터
- 복잡한 관계형 조회
- 금융/결제 원장
- 트랜잭션 정합성이 매우 중요한 데이터
- Redis만 믿고 DB 저장을 생략하는 구조
```

기본 원칙:

```txt
PostgreSQL = 원본 데이터
Redis = 빠른 임시 데이터 / 캐시 / 집계 / 제어
```

---

## 17. Redis Lab 추천 실습 순서

1. String API
   - SET / GET / DEL / TTL

2. 인증번호 API
   - TTL 기반 일회성 코드

3. 조회수 중복 방지
   - SET NX EX + INCR

4. 좋아요 토글
   - Set 자료구조

5. 인기 랭킹
   - Sorted Set

6. 최근 검색어
   - List

7. Rate Limiting
   - INCR + EXPIRE

8. Refresh Token 저장
   - String + TTL

---

## 18. CLI 미니 실습 세트

아래 명령어를 순서대로 직접 쳐보면 Redis 핵심 감각이 잡힌다.

```redis
FLUSHDB

SET name minjun
GET name
TYPE name

SET temp hello EX 10
TTL temp
GET temp

SET counter 0
INCR counter
INCRBY counter 5
GET counter

HSET user:1 name minjun role USER age 25
HGET user:1 name
HGETALL user:1
HINCRBY user:1 loginCount 1

LPUSH recent:1 redis
LPUSH recent:1 spring
LPUSH recent:1 docker
LRANGE recent:1 0 -1
LTRIM recent:1 0 1
LRANGE recent:1 0 -1

SADD post:1:likes user:1
SADD post:1:likes user:2
SADD post:1:likes user:1
SCARD post:1:likes
SISMEMBER post:1:likes user:1
SMEMBERS post:1:likes

ZADD ranking:posts 10 post:1
ZADD ranking:posts 30 post:2
ZINCRBY ranking:posts 5 post:1
ZREVRANGE ranking:posts 0 -1 WITHSCORES

SET viewed:post:1:user:1 1 NX EX 600
SET viewed:post:1:user:1 1 NX EX 600
TTL viewed:post:1:user:1
```

---

## 19. 자주 하는 실수

### 1. Redis를 영구 DB처럼 쓰기

Redis는 빠르지만 메모리 기반이다. 영구 저장이 가능하더라도 원본 DB 역할로 쓰는 것은 신중해야 한다.

### 2. key 네이밍을 대충 하기

나중에 key가 많아지면 혼란이 커진다. 처음부터 규칙을 잡는다.

### 3. KEYS *를 운영에서 쓰기

운영에서는 `SCAN`을 사용한다.

### 4. TTL을 빼먹기

임시 데이터인데 TTL을 안 걸면 메모리가 계속 쌓인다.

### 5. JSON 문자열을 아무렇게나 저장하기

가능은 하지만, 수정/조회 패턴이 복잡하면 Hash나 DB 저장을 고려한다.

### 6. Sorted Set 점수 설계를 대충 하기

랭킹은 점수 정책이 중요하다. 조회, 공유, 좋아요의 가중치를 명확히 잡아야 한다.

---

## 20. 핵심 요약

Redis 핵심은 다음 5개다.

```txt
1. String = 단순 값, 카운터, 토큰
2. TTL = 자동 만료
3. Set = 중복 없는 집합
4. Sorted Set = 랭킹
5. 원자 명령 = 동시성에 강한 단일 연산
```

Spring Boot에서 Redis를 쓸 때도 결국 이 명령어들이 코드로 바뀌는 것뿐이다.

예:

```txt
redis-cli SET name minjun
→ stringRedisTemplate.opsForValue().set("name", "minjun")

redis-cli INCR counter
→ stringRedisTemplate.opsForValue().increment("counter")

redis-cli SADD post:1:likes user:1
→ redisTemplate.opsForSet().add("post:1:likes", "user:1")

redis-cli ZINCRBY ranking:posts 1 post:1
→ redisTemplate.opsForZSet().incrementScore("ranking:posts", "post:1", 1)
```

따라서 Spring 코드로 넘어가기 전에 CLI에서 자료형 감각을 잡는 것이 매우 중요하다.
