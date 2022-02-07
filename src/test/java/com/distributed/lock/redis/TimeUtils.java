package com.distributed.lock.redis;

public class TimeUtils {
    public static volatile long startL = 0;
    public static void start(){
        startL = System.currentTimeMillis();
    }

    /**
     * 打印耗时
     * @return
     */
    public static long stop(){
        long cost = System.currentTimeMillis() - startL;
        System.out.println("操作耗时:"+cost+"ms");
        startL = System.currentTimeMillis();
        return cost;
    }

}
