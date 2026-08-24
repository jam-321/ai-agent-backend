package com.jam.agent.agent.runtime;

import java.time.Duration;
import java.util.Collections;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class ConversationLock {

    private static final String PREFIX = "agent:conversation:lock:";
    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] "
                    + "then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public ConversationLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean tryLock(long conversationId, String traceId, Duration ttl) {
        Boolean result = redis.opsForValue().setIfAbsent(key(conversationId), traceId, ttl);
        return Boolean.TRUE.equals(result);
    }

    public void unlock(long conversationId, String traceId) {
        // Compare-and-delete prevents an expired run from removing a newer run's lock.
        redis.execute(UNLOCK, Collections.singletonList(key(conversationId)), traceId);
    }

    private String key(long conversationId) {
        return PREFIX + conversationId;
    }
}
