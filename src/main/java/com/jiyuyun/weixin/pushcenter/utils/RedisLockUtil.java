package com.jiyuyun.weixin.pushcenter.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * redis 分布式锁
 *
 * @author pwm
 * @date 2020/1/9
 */
@Component
@Slf4j
public class RedisLockUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String UNLOCK_LUA = "if redis.call(\"get\",KEYS[1]) == ARGV[1] " +
            "then " +
            "    return redis.call(\"del\",KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end ";


    public boolean setLock(String key, long expire) {
        try {
            RedisCallback<String> callback = (connection -> {
                JedisCommands jedisCommands = (JedisCommands) connection.getNativeConnection();
                String uuid = UUID.randomUUID().toString();
                return jedisCommands.set(key, uuid, "NX", "PX", expire);
            });
            // 已经存在会返回false，成功设置返回true
            String result = redisTemplate.execute(callback);
            return StringUtils.isNotBlank(result);
        } catch (Exception e) {
            log.error("加锁异常", e);
        }
        return false;
    }

    public String getLock(String key) {
        try {
            RedisCallback<String> callback = (connection -> {
                JedisCommands jedisCommands = (JedisCommands) connection.getNativeConnection();
                String uuid = UUID.randomUUID().toString();
                return jedisCommands.get(key);
            });
            return redisTemplate.execute(callback);
        } catch (Exception e) {
            log.error("获取锁异常", e);
        }
        return "";
    }

    public boolean releaseLock(String key, String requestId) {
        // 释放锁的时候，有可能因为持锁之后方法执行时间大于锁的有效期，此时有可能已经被另外一个线程持有锁，所以不能直接删除
        try {
            List<String> keys = new ArrayList<>();
            keys.add(key);
            List<String> args = new ArrayList<>();
            args.add(requestId);

            // 使用lua脚本删除redis中匹配value的key，可以避免由于方法执行时间过长而redis锁自动过期失效的时候误删其他线程的锁
            // spring自带的执行脚本方法中，集群模式直接抛出不支持执行脚本的异常，所以只能拿到原redis的connection来执行脚本
            RedisCallback<Long> callback = (connection) -> {
                Object nativeConnection = connection.getNativeConnection();
                // 集群模式和单机模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
                // 集群模式
                if (nativeConnection instanceof JedisCluster) {
                    return (Long) ((JedisCluster) nativeConnection).eval(UNLOCK_LUA, keys, args);
                }// 单机模式
                else if (nativeConnection instanceof Jedis) {
                    return (Long) ((Jedis) nativeConnection).eval(UNLOCK_LUA, keys, args);
                }
                return 0L;
            };
            Long result = redisTemplate.execute(callback);

            return result != null && result > 0;
        } catch (Exception e) {
            log.error("释放锁异常", e);
        }
        return false;
    }
}
