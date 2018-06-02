package com.wjj.promise.util;

import com.wjj.promise.AbstractPromise;
import com.wjj.promise.IPromise;
import com.wjj.promise.Promise;
import com.wjj.promise.Status;
import com.wjj.promise.then.OnCatchedExecutor;
import com.wjj.promise.then.OnCompleteListener;
import com.wjj.promise.then.OnFulfilledExecutor;
import com.wjj.promise.then.OnRejectedExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一些工具
 * @author wangjiajun
 */
public class PromiseUtil {
    /**
     * 判断是否所有Promise都处于fulfilled状态
     * @param promises
     * @return
     */
    public static boolean isAllDone(IPromise ...promises){
        for(IPromise p:promises){
            if(p.getStatus().equals(Status.PENDING)){
                return false;
            }
        }
        return true;
    }

    /**
     * 取出第一个rejected状态的Promise
     * @param promises
     * @return
     */
    public static IPromise getFirstRejected(IPromise ...promises){
        for(IPromise p:promises){
            if(p.getStatus().equals(Status.REJECTED)){
                return p;
            }
        }
        return null;
    }

    /**
     * 获取第一个处于fulfilled或rejected状态的Promise
     * @param promises
     * @return
     */
    public static IPromise getFinal(IPromise ...promises){
        for(IPromise p:promises){
            if(!p.getStatus().equals(Status.PENDING)){
                return p;
            }
        }
        return null;
    }

    /**
     * 取消执行所有Promise
     * @param promises
     */
    public static void cancel(IPromise ...promises){
        for(IPromise p:promises){
            Future f = p.getFuture();
            if(f!=null&&!f.isDone()){
                p.cancel();
            }
        }
    }

    /**
     * 复制一个处于非pending状态的Promise，如果Promise处于pending状态，返回一个终值为null、状态为fulfilled的Promise
     * @param promise
     * @return
     */
    public static IPromise cloneFinal(IPromise promise){
        if(promise.getStatus().equals(Status.PENDING)){
            return new Promise.Builder().finalPromise(null,true).build();
        }else if(promise.getStatus().equals(Status.FULFILLED)){
            return new Promise.Builder().finalPromise(promise.getResolvedData(),true).build();
        }else{
            return new Promise.Builder().finalPromise(promise.getRejectedData(),false).build();
        }
    }
}
