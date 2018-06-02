package com.wjj.promise.test;

import com.wjj.promise.*;
import com.wjj.promise.then.OnFulfilledExecutor;
import com.wjj.promise.then.OnRejectedExecutor;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 * @author wangjiajun
 * @date 2018/5/28 11:17
 */
public class App {
    public static void main(String[] args){
        test3();
    }
    public static void test7(){
        ExecutorService pool = Promise.pool(1);
        IPromise promiseA = new Promise.Builder().pool(pool).promiseHanler(executor -> {
            Random random = new Random();
            int i=0;
            while (i<3){
                i++;
                System.out.println(random.nextInt(100));
                Thread.sleep(100);
            }
            return random.nextInt(100);
        }).build();
        promiseA.then(resultA -> {
            IPromise promiseB = new Promise.Builder().pool(pool).externalInput(resultA)
                    .promiseHanler(executor -> {
                        String bResult = "b:"+executor.getExternalInput();
                        return bResult;
                    }).build();
            return promiseB;
        }).then(resultB -> {
            System.out.println(resultB);
            return resultB;
        }).pCatch(e->{
            e.printStackTrace();
            return null;
        });
        pool.shutdown();
        System.out.println("主程序结束了");
    }
    public static void test6(){
        Map<String,Boolean> p1Flag = new HashMap<>();
        p1Flag.put("flag",true);
        IPromise p1 = new Promise.Builder().externalInput(p1Flag).promiseHanler(executor -> {
            while (((Map<String,Boolean>)executor.getExternalInput()).get("flag")){
                //do something
                System.out.println("p1 正在执行任务");
            }
            System.out.println("p1任务完成，正常结束");
            return 1;
        }).build();
        IPromise p2 = new Promise.Builder().promiseHanler(executor -> {
            while (!Thread.currentThread().isInterrupted()){
                System.out.println("执行p2正常逻辑");
            }
            System.err.println("p2线程被取消");
            return 2;
        }).build();
        IPromise p3 = new Promise.Builder().promiseHanler(executor -> {
            Thread.sleep(10);
            throw new RuntimeException("p3抛出异常");
        }).build();
        IPromise p4 = new Promise.Builder().finalPromise("4",true).build();
        long s = System.currentTimeMillis();
        Promise.all(p1,p2,p3,p4).then(resolvedData -> {
            Object[] datas = (Object[])resolvedData;
            for(Object d:datas){
                System.out.println(d);
            }
            return null;
        },e->e.printStackTrace());
        System.out.println("耗时："+(System.currentTimeMillis()-s));
        p1Flag.put("flag",false);
    }
    public static void test5(){
        IPromise p1 = new Promise.Builder().promiseHanler(executor -> {
            Thread.sleep(1000);
            return 1;
        }).build();
        IPromise p2 = new Promise.Builder().promiseHanler(executor -> {
            Thread.sleep(4000);
            return 2;
        }).build();
        IPromise p3 = new Promise.Builder().promiseHanler(executor -> {
            Thread.sleep(2000);
            return 3;
        }).build();
        long s = System.currentTimeMillis();
        Promise.all(p1,p2,p3).then(resolvedData -> {
            Object[] datas = (Object[])resolvedData;
            for(Object d:datas){
                System.out.println(d);
            }
            return null;
        },e->e.printStackTrace());
        System.out.println("耗时："+(System.currentTimeMillis()-s));
    }
    public static void test4(){
        new Promise.Builder().promiseHanler(executor -> 0).build()
          .then(res0->{
            System.out.println("a:"+res0);
            Thread.sleep(100);
            return 1;
        }).then(res1 -> {
            throw new RuntimeException("throw error");
        }).then(res2->{
            Thread.sleep(100);
            System.out.println("b:"+res2);
            return 2;
        }).pCatch(e->{
            Thread.sleep(100);
            System.out.println("c:");
            e.printStackTrace();
            return 3;
        }).then(res3->{
            Thread.sleep(100);
            System.out.println("d:"+res3);
            return 4;
        });
    }
    public static void test3(){
        new Promise.Builder().promiseHanler(executor -> 3).build().then(resolvedData->{
            System.out.println("a:"+resolvedData);
            return new Promise.Builder().promiseHanler(executor -> {
                executor.reject(new RuntimeException("err"));
                return null;
            }).build();
        }).then(resolvedData1 -> {
            System.out.println("b:"+resolvedData1);
            return resolvedData1;
        },rejectReason -> {
            System.err.println("c:"+rejectReason);
        });
    }
    public static void test2(){
        ExecutorService fixedPool = Promise.pool(1);
        IPromise promise1 = new Promise.Builder().pool(fixedPool).promiseHanler(executor->3).build();
        IPromise promise2 = new Promise.Builder().pool(fixedPool).promiseHanler(executor->4+(Integer) executor.getPromiseInput())
                .promise(promise1)
                .build()
                .then(resolvedData->{
            System.out.println(resolvedData);
            return resolvedData;
        }, rejectedReason-> rejectedReason.printStackTrace());
        System.out.println("end");
        fixedPool.shutdown();
    }
    public static void test1(){
        new Promise.Builder().promiseHanler(new PromiseHandler(){
            @Override
            public Object run(PromiseExecutor executor) {
                executor.resolve(3);
                return null;
            }
        }).build().then(new OnFulfilledExecutor() {
            @Override
            public Object onFulfilled(Object resolvedData) {
                Integer i = ((Integer)resolvedData)+1;
                System.out.println(i);
                IPromise p = new Promise.Builder().externalInput(i).promiseHanler(new PromiseHandler() {
                    @Override
                    public Object run(PromiseExecutor executor) {
                        Integer args = (Integer) executor.getExternalInput();
                        executor.resolve(args*2);
                        return null;
                    }
                }).build();
                return p;
            }
        })
        .then(new OnFulfilledExecutor() {
            @Override
            public Object onFulfilled(Object args) {
                System.out.println(args);
                return args;
            }
        }, new OnRejectedExecutor() {
            @Override
            public void onRejected(Throwable rejectedReason) throws Exception {
                rejectedReason.printStackTrace();
            }
        });
    }
}
