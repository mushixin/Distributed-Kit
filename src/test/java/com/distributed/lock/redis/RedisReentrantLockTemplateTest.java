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

import java.util.concurrent.*;

/**
 * Created by sunyujia@aliyun.com on 2016/2/24.
 */
public class RedisReentrantLockTemplateTest {

    private static CountDownLatch a = new CountDownLatch(1);
    private static CountDownLatch b = new CountDownLatch(1);
    private static CountDownLatch c = new CountDownLatch(1);
    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(3);


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
