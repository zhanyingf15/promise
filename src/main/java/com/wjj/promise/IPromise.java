package com.wjj.promise;

import com.wjj.promise.then.OnCatchedExecutor;
import com.wjj.promise.then.OnCompleteListener;
import com.wjj.promise.then.OnFulfilledExecutor;
import com.wjj.promise.then.OnRejectedExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author wangjiajun
 */
public interface IPromise extends Callable {
    /**
     * 同then {@link com.wjj.promise.IPromise#then(OnFulfilledExecutor onFulfilledExecutor, OnRejectedExecutor onRejectedExecutor)}
     * @param onFulfilledExecutor
     * @return
     */
    public IPromise then(OnFulfilledExecutor onFulfilledExecutor);
    /**
     * <li>如果当前promise处于pending状态，阻塞当前线程，等待promise状态转变为fulfilled或rejected
     * <li>如果处于fulfilled状态，执行onFulfilledExecutor.onFulfilled(resolvedData)回调。
     * 如果回调返回一个Promise对象a，以a作为then方法的返回值，如果回调返回一个普通对象obj，以obj作为终值、状态为fulfilled包装一个新Promise作为then方法的返回值</li>
     * 如果执行回调过程中产生异常e,返回一个以e作为拒因、状态为rejected的新Promise，并拒绝执行接下来的所有Promise直到遇到pCatch。
     * <li>如果处于rejected状态，执行onRejectedExecutor.onRejected(rejectReason)回调，
     * 返回一个以当前promise的异常作为拒因、状态为rejected的新Promise，并拒绝执行接下来的所有Promise直到遇到pCatch</li>
     * @param onFulfilledExecutor
     * @param onRejectedExecutor
     * @return
     */
    public IPromise then(OnFulfilledExecutor onFulfilledExecutor, OnRejectedExecutor onRejectedExecutor);
    /**
     * then(null,onRejectedExecutor)的别名，但返回不同于then，出现异常时可以选择不拒绝接下来Promise的执行，可用于异常修正，类似于try{}catch{}<br/>
     * 尝试捕获当前promise的异常,最终返回一个Promise,当被捕获Promise处于不同的状态时有不同的行为
     * <pre>
     *     <ul>
     *         <li>pending：阻塞当前线程，等待pending转变为fulfilled或rejected，行为同then</li>
     *         <li>fulfilled：不执行回调，以当前Promise终值和状态返回一个全新的Promise</li>
     *         <li>rejected：执行onCatched(Throwable catchReason)回调，如果onCatched方法返回一个Promise，以这个Promise作为最终返回。
     *             如果onCatched方法返回一个非Promise对象obj，以obj作为终值、fulfilled状态返回一个全新的对象。
     *             如果执行回调过程中产生异常e，以e为拒因、状态为rejected返回一个新的Promise，并拒绝执行接下来的所有Promise直到遇到pCatch
     *         </li>
     *     </ul>
     * </pre>
     * @param onCatchedExecutor
     * @return
     */
    public IPromise pCatch(OnCatchedExecutor onCatchedExecutor);

    /**
     * 指定一个监听器，在promise状态转为fulfilled或rejected调用，该方法不会阻塞线程执行，可以多次调用指定多个监听器
     * @param onCompleteListener
     */
    public void listen(OnCompleteListener onCompleteListener);

    /**
     * listen的别名，行为同listen
     * @param onCompleteListener
     */
    public void pFinally(OnCompleteListener onCompleteListener);

    /**
     * 转变promise状态，只能由pending转变为fulfilled或rejected，如果promise已为fulfilled或rejected状态，操作将被忽略
     * @param status 目标状态
     * @param data  状态对应数据
     * @return
     */
    public boolean transferStatus(Status status,Object data);

    /**
     * 获取promise的当前状态
     * @return
     */
    public Status getStatus();

    /**
     * 获取promise fulfilled状态下的终值，其余状态下时为null
     * @return
     */
    public Object getResolvedData();

    /**
     * 获取promise rejected状态下的拒因，其余状态下为null
     * @return
     */
    public Throwable getRejectedData();

    /**
     * 获取promise对应异步任务的future
     * @return
     */
    public Future getFuture();

    /**
     * 尝试取消promise对应的异步任务，底层调用future.cancel(true)。fulfilled或rejected状态下无效。
     * @return
     */
    public boolean cancel();
}
