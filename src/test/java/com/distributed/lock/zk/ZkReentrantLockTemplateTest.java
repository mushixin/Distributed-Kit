package com.distributed.lock.zk;

import com.distributed.lock.Callback;
import com.distributed.lock.redis.HHRedisDistributedLockTemplate;
import com.distributed.lock.redis.RedisDistributedLockTemplate;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.*;

/**
 * Created by sunyujia@aliyun.com on 2016/2/24.
 */


public class ZkReentrantLockTemplateTest {

    @Test
    public void testTry() throws InterruptedException {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", retryPolicy);
        client.start();

        final ZkDistributedLockTemplate template=new ZkDistributedLockTemplate(client);
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
                    final int sleepTime=ThreadLocalRandom.current().nextInt(5)*1000;
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

    public static void main(String[] args) throws InterruptedException {
//        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
//        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", retryPolicy);
//        client.start();
//
//        final ZkDistributedLockTemplate template=new ZkDistributedLockTemplate(client);//本类多线程安全,可通过spring注入
//        template.execute("订单流水号", 5000, new Callback() {
//            @Override
//            public Object onGetLock() throws InterruptedException {
//                //TODO 获得锁后要做的事
//                return null;
//            }
//
//            @Override
//            public Object onTimeout() throws InterruptedException {
//                //TODO 获得锁超时后要做的事
//                return null;
//            }
//        });

        /**
         * 创建十个客户端，然后去创建分布式锁。
         * 获得锁后，会打印当前线程。持有锁1s
         * 若是获得锁失败了，也会打印。然后自旋等待锁释放。
         */
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

                    ZooKeeperSession session = new ZooKeeperSession();
                    boolean res  = session.acquireDistributedLock("resid");
                    System.out.println(Thread.currentThread().getName() + res);
                    if(res){
                        res = session.releaseDistributedLock("resid");
                        if(res){
                            System.out.println(Thread.currentThread().getName() + res + " 释放锁");
                        }
                    }
//                    HHRedisDistributedLockTemplate template=new HHRedisDistributedLockTemplate(jd);
//                    template.execute("test",5000, new Callback() {
//                        public Object onGetLock() throws InterruptedException {
//                            System.out.println(Thread.currentThread().getName() + ":getLock");
//                            Thread.currentThread().sleep(sleepTime);
//                            System.out.println(Thread.currentThread().getName() + ":sleeped:"+sleepTime);
//                            endDownLatch.countDown();
//                            return null;
//                        }
//                        public Object onTimeout() throws InterruptedException {
//                            System.out.println(Thread.currentThread().getName() + ":timeout");
//                            endDownLatch.countDown();
//                            return null;
//                        }
//                    });
                }
            }.start();
        }
        startCountDownLatch.countDown();
        endDownLatch.await();

    }
}
