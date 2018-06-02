package com.wjj.promise;

import com.wjj.promise.executor.RejectExecutor;
import com.wjj.promise.executor.ResolveExecutor;

/**
 *
 * @author wangjiajun
 */
public class PromiseExecutor implements ResolveExecutor,RejectExecutor{
    private Promise promise;
    protected PromiseExecutor(Promise promise){
        this.promise = promise;
    }

    @Override
    public void resolve(final Object args) {
        promise.transferStatus(Status.FULFILLED,args);
    }

    @Override
    public void reject(final Throwable args) {
        promise.transferStatus(Status.REJECTED,args);
    }

    /**
     * 获取通过new Promise.Builder().externalInput(Object externalInput)方法注入的参数。<br/>
     * 具体参考：{@link com.wjj.promise.Promise.Builder#externalInput(Object externalInput)}
     * @return
     */
    public Object getExternalInput(){
        return this.promise.getExternalInput();
    }

    /**
     * 获内部promise的执行结果。通过new Promise.Builder().promise(promise1)指定的promise1的执行结果<br/>
     * 具体参考：{@link com.wjj.promise.Promise.Builder#promise(IPromise promise)}
     * @return
     */
    public Object getPromiseInput(){
        return this.promise.getPromiseInput();
    }
}
