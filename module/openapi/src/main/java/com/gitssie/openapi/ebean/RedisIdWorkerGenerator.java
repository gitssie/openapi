package com.gitssie.openapi.ebean;

import io.ebean.config.IdGenerator;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: Awesome
 * @create: 2024-03-25 14:52
 */
public class RedisIdWorkerGenerator implements IdGenerator {
    private final String name;
    private final String redisKey;

    private final RedisTemplate redisTemplate;
    private final AtomicLong start = new AtomicLong(0);
    private final AtomicLong end = new AtomicLong(0);
    private final long sequenceBits;// 20 毫秒内自增位
    private final long maxRange;// 1 << sequenceBits;
    private final long delta;
    private final int timeShift;//  >> 7 大概是1/10秒
    private final long epoch;  //时间起始标记点，作为基准

    public RedisIdWorkerGenerator(String name, String redisKey, RedisTemplate redisTemplate) {
        this(name, redisKey, 1024, redisTemplate);
    }

    public RedisIdWorkerGenerator(String name, String redisKey, long delta, RedisTemplate redisTemplate) {
        this(name, redisKey, new Date(1403854494756L), 7, 20, delta, redisTemplate);
    }

    public RedisIdWorkerGenerator(String name, String redisKey, Date epoch, int timeShift, int sequenceBits, RedisTemplate redisTemplate) {
        this(name, redisKey, epoch, timeShift, sequenceBits, 1024, redisTemplate);
    }

    public RedisIdWorkerGenerator(String name, String redisKey, Date epoch, int timeShift, int sequenceBits, long delta, RedisTemplate redisTemplate) {
        this.name = name;
        this.redisKey = redisKey;
        this.epoch = epoch.getTime() >> timeShift;
        this.timeShift = timeShift;
        this.sequenceBits = sequenceBits;
        this.maxRange = 1 << sequenceBits;
        this.delta = delta;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Object nextValue() {
        long current = this.timeGen();
        long seq = start.incrementAndGet();
        if (seq > end.get()) {
            incrementValue(start.get(), end.get());
        }
        return current - this.epoch << this.sequenceBits | seq;
    }

    /**
     * 获取同步锁之后会今日此方法，先判断start、end 值是否已变更过
     *
     * @param startValue
     * @param endValue
     * @return
     */
    private synchronized void incrementValue(long startValue, long endValue) {
        if (start.get() != startValue || end.get() != endValue) {
            return;
        }
        long i = redisTemplate.opsForValue().increment(redisKey, delta);
        long j = (i - delta) % maxRange;
        long k = i % maxRange;
        if (k <= j) {
            incrementValue(startValue, endValue);
            return;
        }
        start.set(j);
        end.set(k);
    }

    @Override
    public String getName() {
        return name;
    }

    private long timeGen() {
        return System.currentTimeMillis() >> timeShift;
    }
}
