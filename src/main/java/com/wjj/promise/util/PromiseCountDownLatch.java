package com.wjj.promise.util;

import java.util.concurrent.CountDownLatch;

/**
 * @author wangjiajun
 */
public class PromiseCountDownLatch extends CountDownLatch {
    /**
     * Constructs a {@code CountDownLatch} initialized with the given count.
     *
     * @param count the number of times {@link #countDown} must be invoked
     *              before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public PromiseCountDownLatch(int count) {
        super(count);
    }
    public void countDownAll() {
        while (this.getCount()>0){
            this.countDown();
        }
    }
}
