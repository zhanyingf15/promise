package com.wjj.promise.test;

import com.wjj.promise.Promise;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangjiajun
 */
public class ThenTest {
    public Integer then(int a,int b){
        System.out.println(Thread.currentThread().getName());
        return a+b;
    }
    public static void main(String[] args){
        System.out.println(Thread.currentThread().getName());
        List arg = new ArrayList<>();
        arg.add(1);
        arg.add(2);
        Promise.resolve(new ThenTest(),arg).then(resolvedData -> {
            System.out.println(resolvedData);
            return resolvedData;
        }).pCatch(e->{
            e.printStackTrace();
            return 1;
        });
    }
}
