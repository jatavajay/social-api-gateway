# Social API Gateway with Redis Guardrails

A high-performance Spring Boot microservice that implements a social media API gateway with Redis-based concurrency controls and smart notification batching.

## Tech Stack
- Java 17+
- Spring Boot 3.1.x
- PostgreSQL (Data persistence)
- Redis (Distributed state, counters, rate limiting)

## Architecture Overview

### Phase 1: Core API
- REST endpoints for posts, comments, and likes
- JPA entities with proper relationships
- PostgreSQL as source of truth

### Phase 2: Redis Guardrails (Thread-Safe)
- **Virality Score**: Real-time scoring system (Bot reply=1, Human like=20, Human comment=50)
- **Horizontal Cap**: Max 100 bot replies per post using atomic Lua scripts
- **Vertical Cap**: Max 20 comment depth level
- **Cooldown Cap**: Bot-Human interaction limited to once per 10 minutes

### Thread Safety Implementation
The horizontal cap uses a custom Lua script that atomically checks and increments the counter:
```lua
local current = redis.call('INCR', KEYS[1])
if current > tonumber(ARGV[1]) then
    redis.call('DECR', KEYS[1])
    return 0
else
    return current
end