package com.gitssie.openapi;

import com.gitssie.openapi.ebean.RedisIdWorkerGenerator;
import io.ebean.config.IdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author: Awesome
 * @create: 2024-03-25 14:38
 */
@SpringBootTest
@ActiveProfiles(value = "test")
@Import(TestAppConfig.class)
public class IdWorkTest {

    private RedisTemplate<String, String> redisTemplate;
    private RedisIdWorkerGenerator idWorkerGenerator;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    public IdWorkTest(RedisConnectionFactory connectionFactory) {
        redisTemplate = new StringRedisTemplate(connectionFactory);
        idWorkerGenerator = new RedisIdWorkerGenerator("idWork", "redis-id", 1024 << 5,redisTemplate);
    }

    @Test
    public void testIdGenerator(){
        long value = (long) idGenerator.nextValue();
        System.out.println(idGenerator.getClass().getSimpleName()+ "," + value);
    }

    @Test
    public void testGenId() {
        long delta = 1024;
        long max = 1 << 20; //1048575
        long i = 0, j = 0, k = 0;
        while (i < max * 10) {
            i += delta;
            j = (i - delta) % max;
            k = i % max;
            if (k <= j) {
                continue;
            }
            System.out.println(j + "," + k);
        }
    }

    @Test
    public void testRedisGenIdOne() {
        //long i = this.redisTemplate.opsForValue().increment("redis-id", 1024);
        //System.out.println(i);
        long v = (long) idWorkerGenerator.nextValue();
        System.out.println(v);

    }

    @Test
    public void testRedisGenId() {
        //long i = this.redisTemplate.opsForValue().increment("redis-id", 1024);
        //System.out.println(i);
        long max = 1 << 20;
        long i = 0;
        while (i < max * 5) {
            long v = (long) idWorkerGenerator.nextValue();
            System.out.println(v);
        }
    }

    @Test
    public void testRedisGenIdConcurrent() throws Exception {
        //long i = this.redisTemplate.opsForValue().increment("redis-id", 1024);
        //System.out.println(i);
        long max = 1 << 20;
        Runnable run = () -> {
            long i = 0;
            while (i < max * 1) {
                long v = (long) idWorkerGenerator.nextValue();
                System.out.println(v);
            }
        };
        new Thread(run).start();
        new Thread(run).start();
        new Thread(run).start();
        new Thread(run).start();
        Thread.currentThread().join();
    }

    @Configuration
    public static class TestConfig {

    }
}
