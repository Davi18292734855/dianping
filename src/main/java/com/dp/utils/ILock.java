package com.dp.utils;

public interface ILock {

    /**
     *尝试获取锁
     * @param time 锁持有时间，过期自动释放
     * @return
     */
    boolean tryLock(long time);

    /**
     *释放锁
     */
    void unlock();
}
