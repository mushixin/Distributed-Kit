package com.distributed.lock.redis;

import com.distributed.lock.Callback;
import com.distributed.lock.DistributedLockTemplate;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by sunyujia@aliyun.com on 2016/2/26.
 */
public class HHRedisDistributedLockTemplate implements DistributedLockTemplate {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HHRedisDistributedLockTemplate.class);

    private JedisPool jedisPool;
    private Jedis jedis ;

    public HHRedisDistributedLockTemplate(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.jedis = jedisPool.getResource();
    }


    public boolean tryLock(String key, String ran, int timout){
        System.out.println("tryLock key:"+key+"ran:"+ran);
        Long val = jedis.setnx(key, ran);
        System.out.println("tryLock key:"+key+"ran:"+ran+"val:"+val);
        jedis.pexpire(key,timout);

        return  jedis.get(key).equals(ran);
    }

    public boolean unLock(String key, String value){
        if (value.equals(jedis.get(key))){
            jedis.del(key);
            System.out.println("unLock key:"+key+"val:"+value);
        }else{
            jedis.close();
            System.out.println("unlockERROR:"+"key:"+key+"expectVal:"+value+"val:"+jedis.get(key));
            return false;
        }
        jedis.close();
        return true;
    }

    public Object execute(String lockId, int timeout, Callback callback) {
        String ran = Thread.currentThread().getName();

        boolean getLock=false;
        try {
            if(tryLock(lockId,ran, timeout)){
                getLock=true;
                return callback.onGetLock();
            }else{
                return callback.onTimeout();
            }
        }catch(InterruptedException ex){
            log.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }catch (Exception e) {
            log.error(e.getMessage(), e);
        }finally {
            if(getLock) {
                unLock(lockId,ran);
            }else{
                jedis.close();
            }
        }
        return null;
    }
}
