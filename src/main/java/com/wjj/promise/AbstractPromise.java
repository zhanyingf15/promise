package com.wjj.promise;

import com.wjj.promise.then.OnCatchedExecutor;
import com.wjj.promise.then.OnCompleteListener;
import com.wjj.promise.then.OnFulfilledExecutor;
import com.wjj.promise.then.OnRejectedExecutor;

import java.util.concurrent.Callable;

/**
 * @author wangjiajun
 */
public abstract class AbstractPromise implements IPromise{
    private Status status = Status.PENDING;
    private Object resolvedData;
    private Throwable rejectedData;

    @Override
    final public boolean transferStatus(Status status,Object data){
        if(this.getStatus().equals(Status.PENDING)){
            this.status = status;
            if(status.equals(Status.FULFILLED)){
                this.resolvedData = data;
            }else if(status.equals(Status.REJECTED)){
                this.rejectedData = (Throwable) data;
            }
            return true;
        }
        return false;
    }
    @Override
    public void listen(OnCompleteListener onCompleteListener) {

    }
    @Override
    public Status getStatus(){
        return this.status;
    }
    @Override
    public Object getResolvedData(){
        return this.resolvedData;
    }
    @Override
    public Throwable getRejectedData(){
        return this.rejectedData;
    }
}
