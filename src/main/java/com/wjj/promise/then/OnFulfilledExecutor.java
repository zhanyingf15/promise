package com.wjj.promise.then;

/**
 * @author wangjiajun
 */
@FunctionalInterface
public interface OnFulfilledExecutor {
    /**
     * 当Promise转变为fulfilled状态时的回调,具体参考{@link com.wjj.promise.IPromise#then(OnFulfilledExecutor onFulfilledExecutor, OnRejectedExecutor onRejectedExecutor)}
     * @param resolvedData 终值
     * @return object，如果object是IPromise实例，object作为then方法的返回值，如果object是个普通对象，以object作为终值、状态为fulfilled包装一个新Promise作为then方法的返回值<br/>
     * @throws Exception
     */
    public Object onFulfilled(Object resolvedData)throws Exception;
}
