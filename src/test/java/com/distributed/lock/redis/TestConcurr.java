package com.distributed.lock.redis;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1。同时只能有一个线程调用A或者B
 * 2。每个线程，只能调用一次A或者B，线程调用完A后，不可以再调用A或者B每个线程调用的时候，都加一个永不失效的锁。
 */
public class TestConcurr {

    Object obj = new Object();

    public void printA() {
        synchronized (obj) {
            System.out.println("A");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            TimeUtils.stop();
        }
    }

    public void printB() {
        synchronized (obj) {

            System.out.println("B");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            TimeUtils.stop();
        }
    }

    public static void main(String[] args) {
        TestConcurr testC = new TestConcurr();

        TimeUtils.start();

        new Thread(() -> {
            testC.printA();
            testC.printA();
            testC.printB();
        }).start();
//        TimeUtils.stop();

        new Thread(() -> {
            testC.printA();
        }).start();
//        TimeUtils.stop();

        new Thread(() -> {
            testC.printB();
        }).start();
//        TimeUtils.stop();

    }

}
