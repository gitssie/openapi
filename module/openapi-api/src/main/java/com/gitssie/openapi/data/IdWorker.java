package com.gitssie.openapi.data;

import java.util.concurrent.atomic.AtomicLong;

public class IdWorker {
    private final long workerId;
    private final long epoch = 1403854494756L >> 7;   // 时间起始标记点，作为基准，一般取系统的最近时间
    private final long workerIdBits = 8L;      // 机器标识位数
    private final long maxWorkerId = -1L ^ -1L << this.workerIdBits;// 机器ID最大值: 255
    private final long sequenceBits = 12L;      //毫秒内自增位
    private final long workerIdShift = this.sequenceBits;
    private final long timestampLeftShift = this.sequenceBits + this.workerIdBits;// 20
    private final long sequenceMask = -1L ^ -1L << this.sequenceBits;
    private final AtomicLong lastTimestamp = new AtomicLong(0);
    private final AtomicLong sequence = new AtomicLong(0);

    public IdWorker(long workerId) {
        if (workerId > this.maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", this.maxWorkerId));
        }
        this.workerId = workerId;
    }

    public long nextId() {
        long current = this.timeGen();
        long seq = 0;
        long last;
        while (true) {
            last = lastTimestamp.get();
            if (last == current) {
                seq = sequence.incrementAndGet() & this.sequenceMask;
                if (seq == 0) {
                    current = this.tilNextMillis(this.lastTimestamp.get());
                    continue;
                } else {
                    break;
                }
            } else {
                if (lastTimestamp.compareAndSet(last, current)) {
                    sequence.set(0);
                    break;
                }
            }
        }
        if (current < last) {
            throw new IllegalStateException(String.format("clock moved backwards.Refusing to generate id for %d milliseconds", (last - current)));
        }
        return current - this.epoch << this.timestampLeftShift | this.workerId << this.workerIdShift | seq;
    }

    private static IdWorker flowIdWorker = new IdWorker(1);

    public static IdWorker getInstance() {
        return flowIdWorker;
    }


    /**
     * 等待下一个毫秒的到来, 保证返回的毫秒数在参数lastTimestamp之后
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = this.timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = this.timeGen();
        }
        return timestamp;
    }

    /**
     * 获得系统当前毫秒数
     */
    private static long timeGen() {
        return System.currentTimeMillis() >> 7;
    }
}