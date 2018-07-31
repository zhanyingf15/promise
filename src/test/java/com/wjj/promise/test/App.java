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
 */
public class App {
    public static void main(String[] args){
        test3();
    }
    public static void test9(){
        IPromise p1 = new Promise.Builder().promiseHandler(executor -> {
            Thread.sleep(2000);
            System.out.println("p1执行完成");
            return 1;
        }).build();
        IPromise p2 = new Promise.Builder().promiseHandler(executor -> {
            int a = 0;
            while (!Thread.currentThread().isInterrupted()){
                Thread.sleep(3000);
            }
            System.err.println("p2任务被取消");
            return 2;
        }).build();
        IPromise p3 = new Promise.Builder().promiseHandler(executor -> {
            Thread.sleep(1000);
            System.out.println("p3执行完成");
            return 3;
        }).build();
        long s = System.currentTimeMillis();
        Promise.race(p1,p2,p3).then(resolvedData -> {
            System.out.println("resolvedData:"+resolvedData);
            return null;
        },e->e.printStackTrace());
        System.out.println("耗时："+(System.currentTimeMillis()-s));
    }
    public static void test8(){
        IPromise promise = new Promise.Builder().promiseHandler(executor -> {
            return 2*3;
        }).build().then(resolvedData -> {
            System.out.println(resolvedData);
            return (Integer)resolvedData+1;
        }).then(res2->{
            System.out.println(res2);
            return new Promise.Builder().externalInput(res2).promiseHandler(executor -> {
                return (Integer)executor.getExternalInput()+2;
            }).build();
        }).then(res3->{
            System.out.println(res3);
            return res3;
        });
    }
    /**测试两个线程A、B先后执行*/
    public static void test7(){
        ExecutorService pool = Promise.pool(1);
        IPromise promiseA = new Promise.Builder().pool(pool).promiseHandler(executor -> {
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
                    .promiseHandler(executor -> {
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
    /**all测试某个promise发生异常,取消线程*/
    public static void test6(){
        Map<String,Boolean> p1Flag = new HashMap<>();
        p1Flag.put("flag",true);
        IPromise p1 = new Promise.Builder().externalInput(p1Flag).promiseHandler(executor -> {
            while (((Map<String,Boolean>)executor.getExternalInput()).get("flag")){
                //do something
                System.out.println("p1 正在执行任务");
            }
            System.out.println("p1任务完成，正常结束");
            return 1;
        }).build();
        IPromise p2 = new Promise.Builder().promiseHandler(executor -> {
            while (!Thread.currentThread().isInterrupted()){
                System.out.println("执行p2正常逻辑");
            }
            System.err.println("p2线程被取消");
            return 2;
        }).build();
        IPromise p3 = new Promise.Builder().promiseHandler(executor -> {
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
    /**测试all*/
    public static void test5(){
        IPromise p1 = new Promise.Builder().promiseHandler(executor -> {
            Thread.sleep(1000);
            return 1;
        }).build();
        IPromise p2 = new Promise.Builder().promiseHandler(executor -> {
            Thread.sleep(4000);
            return 2;
        }).build();
        IPromise p3 = new Promise.Builder().promiseHandler(executor -> {
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
    /**测试异常处理*/
    public static void test4(){
        new Promise.Builder().promiseHandler(executor -> 0).build()
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
        new Promise.Builder().promiseHandler(executor -> 3).build().then(resolvedData->{
            System.out.println("a:"+resolvedData);
            return new Promise.Builder().promiseHandler(executor -> {
                executor.reject(new RuntimeException("err"));
                return null;
            }).build();
        }).pCatch(e->{
            System.out.println("捕获到异常");
            return 3;
        }).then(resolvedData1 -> {
            System.out.println("b:"+resolvedData1);
            return "b:"+resolvedData1;
        },rejectReason -> {
            System.err.println("c:"+rejectReason);
        }).then(resolvedData2 -> {
            System.out.println("d:"+resolvedData2);
            return "d:"+resolvedData2;
        },rejectReason -> {
            System.err.println("e:"+rejectReason);
        });
    }
    public static void test2(){
        ExecutorService fixedPool = Promise.pool(1);
        IPromise promise1 = new Promise.Builder().pool(fixedPool).promiseHandler(executor->3).build();
        IPromise promise2 = new Promise.Builder().pool(fixedPool).promiseHandler(executor->4+(Integer) executor.getPromiseInput())
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
        new Promise.Builder().promiseHandler(new PromiseHandler(){
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
                IPromise p = new Promise.Builder().externalInput(i).promiseHandler(new PromiseHandler() {
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
    public static void test0(){
        IPromise p = new Promise.Builder().promiseHandler(handler->{
            int a = 0;
            while (a<=5){
                System.out.println("a:"+a);
                Thread.sleep(1000);
                a++;
            }
            return a;
        }).build();
        System.out.println("promise已创建");
        p.then(resolvedData -> {
            System.out.println("resolvedData:"+resolvedData);
            return null;
        });
        System.out.println("运行结束");
    }
}
