/**
 * BBD Service Inc
 * All Rights Reserved @2018
 */
package org.hr.dislock.locker;

import java.util.Collections;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

/**
 *
 * @author huangr
 * @version $Id: RedisDistributeLocker.java, v0.1 2018/12/6 16:10 huangr Exp $$
 */
public class RedisDistributeLocker implements IDistributeLocker {
    private static final Logger              LOG             = LoggerFactory.getLogger(RedisDistributeLocker.class);
    private static final String              success         = "OK";
    private static final String              lockScript      = "return redis.call('set', KEYS[1], ARGV[1], 'EX', 60, 'NX')";
    private static final String              unlockScript    = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    private static final ThreadLocal<String> valueHolder     = ThreadLocal.withInitial(() -> "redisDisLockKey");
    private final StringRedisTemplate        redis;
    private final String                     lockerKeyPrefix = "redis::distribute::locker::key";

    public RedisDistributeLocker(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryLock(String lockKey) {
        String result;
        try {
            String uuid = UUID.randomUUID().toString();
            valueHolder.set(uuid);
            result = (String) this.executeScript(lockScript, lockerKeyPrefix + lockKey, uuid);
        } catch (Exception e) {
            return false;
        }

        return success.equals(result);
    }

    @Override
    public boolean tryLock(String lockKey, long timeout) {
        String result;
        try {
            long start = System.currentTimeMillis();
            String uuid = UUID.randomUUID().toString();
            valueHolder.set(uuid);
            do {
                result = (String) this.executeScript(lockScript, lockerKeyPrefix + lockKey, uuid);
                if (success.equals(result)) {
                    return true;
                }

                if (timeout <= 0) {
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException iex) {
                    break;
                }
            } while (System.currentTimeMillis() - start < timeout);

        } catch (Exception e) {
            return false;
        }
        return success.equals(result);
    }

    @Override
    public void unlock(String lockKey) {
        try {
            String uuid = valueHolder.get();
            this.executeScript(unlockScript, lockerKeyPrefix + lockKey, uuid);
        } catch (Exception e) {
        } finally {
            valueHolder.remove();
        }

    }

    private Object executeScript(String script, String key, String value) {
        return redis.execute(new RedisCallback<Object>() {

            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                Object nativeConnection = connection.getNativeConnection();
                // 集群模式和单点模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
                // 集群
                if (nativeConnection instanceof JedisCluster) {
                    return ((JedisCluster) nativeConnection).eval(script, Collections.singletonList(key), Collections.singletonList(value));
                }
                // 单点
                else if (nativeConnection instanceof Jedis) {
                    return ((Jedis) nativeConnection).eval(script, Collections.singletonList(key), Collections.singletonList(value));
                }
                return null;
            }
        });
    }
}
