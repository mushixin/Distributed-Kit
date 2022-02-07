package com.distributed.lock.redis;

import com.distributed.lock.Callback;
import com.distributed.lock.zk.ZkDistributedLockTemplate;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by sunyujia@aliyun.com on 2016/2/24.
 */
public class RedisReentrantLockTemplateTest {

    private static CountDownLatch a = new CountDownLatch(1);
    private static CountDownLatch b = new CountDownLatch(1);
    private static CountDownLatch c = new CountDownLatch(1);
    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(3);


    /**
     * 先唤醒第一个线程，再唤醒第二个线程，再唤醒第三个线程。
     * 创建很多个线程，依次打印ABC，然后去进入阻塞队列，依次唤醒。
     *
     * 公平锁，第一个线程释放锁，交给后面的线程，当前线程会挂载到最后。
     * 非公平锁，第一个线程释放锁，交给后面的线程，当前线程会尝试抢占锁。会乱序。
     */
    public Thread getPrintThread(String ABC,ReentrantLock lock){
        return new Thread(()->{
            while (true) {
                lock.lock();
                System.out.print(ABC);
                lock.unlock();
            }
        });
    }
    @Test
    public void testPrintABCFairLock() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock(false);
        lock.lock();
        for (int i = 0; i < 10; i++) {
            getPrintThread("A",lock).start();
            Thread.sleep(10);
            getPrintThread("B",lock).start();
            Thread.sleep(10);
            getPrintThread("C",lock).start();
            Thread.sleep(10);
        }
        lock.unlock();
        Thread.sleep(10_000);
    }


    /**
     * 1.依靠reentrantlock 的公平锁，是不是也可以实现依次打印abc呢？依次唤醒。
     * 2.普通实现依次打印abc
     * @throws InterruptedException
     */
    @Test
    public void testPrintABC() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition conditionA = lock.newCondition();
        Condition conditionB = lock.newCondition();
        Condition conditionC = lock.newCondition();


        new Thread(()->{
            while (true) {
                lock.lock();
                try {
                    conditionA.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.print("A");
                conditionB.signalAll();
                lock.unlock();
            }
        }).start();

        new Thread(()->{
            while (true) {
                lock.lock();
                try {
                    conditionB.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.print("B");
                conditionC.signalAll();
                lock.unlock();
            }
        }).start();

        new Thread(()->{
            while (true) {
                lock.lock();
                try {
                    conditionC.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.print("C");
                conditionA.signalAll();
                lock.unlock();
            }
        }).start();

        Thread.sleep(1000);
        lock.lock();
        conditionA.signal();
        lock.unlock();

        Thread.sleep(10000);
    }




    @Test
    public void countDownLatchTest() throws BrokenBarrierException, InterruptedException {

        //学习下countDownLatch  我记得这个可以用来阻塞线程，和唤醒线程。交替打印ABC
        new Thread(){
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        System.out.print("A");
                        a = new CountDownLatch(1);
                        b.countDown();
                        a.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();


        new Thread(){
            @Override
            public void run() {
                while (true){
                    try {
                        b.await();
                        b = new CountDownLatch(1);
                        System.out.print("B");
                        c.countDown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();


        new Thread(){
            @Override
            public void run() {
                while (true){
                    try {
                        c.await();
                        System.out.print("C");
                        c = new CountDownLatch(1);
                        a.countDown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

//        System.out.println("runbef");
        cyclicBarrier.await();
        System.out.println("runaft");
    }


    @Test
    public void testRedis() {
        String key = "ke";
        final JedisPool jp=new JedisPool("127.0.0.1",6379);
        final Jedis jd = jp.getResource();
//        Map<String,String> map = new HashMap<>();
//        map.put("mapKey","1");
//        jd.hmset(key,map);
//        Map<String,String> map2 = (Map)jd.hgetAll(key);
//        System.out.println( map2.get("mapKey") );
//
//        Set<String> set = new HashSet<>();
//        set.add("setKey1");
//        set.add("setKey2");
//        jd.hset("k1","f1","v1");
//        System.out.println(jd.hget("k1","f1"));
//        System.out.println(jd.hgetAll("k1"));
//
//        jd.sadd("setK","setV","setV2");
//        System.out.println(jd.keys("setK"));
//        System.out.println(jd.smembers("setK"));

        //linked list
        jd.lpush("l1","l1v1");
        jd.lpush("l1","l1v3");
        jd.lpush("l2","l2v2");
        jd.lpush("l2","l2v3");
        System.out.println(jd.lpop("l1"));

    }



    @Test
    public void testTry() throws InterruptedException {
        System.setProperty("sun.net.client.defaultConnectTimeout", String
                .valueOf(100_000));// （单位：毫秒）
        System.setProperty("sun.net.client.defaultReadTimeout", String
                .valueOf(100_000)); // （单位：毫秒）


        final JedisPool jp=new JedisPool("127.0.0.1",6379);
        final Jedis jd = jp.getResource();

        int size=100;
        final CountDownLatch startCountDownLatch = new CountDownLatch(1);
        final CountDownLatch endDownLatch=new CountDownLatch(size);
        for (int i =0;i<size;i++){
            new Thread() {
                public void run() {
                    try {
                        startCountDownLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    final int sleepTime=ThreadLocalRandom.current().nextInt(2)*1000;
                    HHRedisDistributedLockTemplate template=new HHRedisDistributedLockTemplate(jd);
                    template.execute("test",5000, new Callback() {
                        public Object onGetLock() throws InterruptedException {
                            System.out.println(Thread.currentThread().getName() + ":getLock");
                            Thread.currentThread().sleep(sleepTime);
                            System.out.println(Thread.currentThread().getName() + ":sleeped:"+sleepTime);
                            endDownLatch.countDown();
                            return null;
                        }
                        public Object onTimeout() throws InterruptedException {
                            System.out.println(Thread.currentThread().getName() + ":timeout");
                            endDownLatch.countDown();
                            return null;
                        }
                    });
                }
            }.start();
        }
        startCountDownLatch.countDown();
        endDownLatch.await();
    }
}
