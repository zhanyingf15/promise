package com.wjj.promise;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wjj.promise.then.OnCatchedExecutor;
import com.wjj.promise.then.OnCompleteListener;
import com.wjj.promise.then.OnFulfilledExecutor;
import com.wjj.promise.then.OnRejectedExecutor;
import com.wjj.promise.util.PromiseCountDownLatch;
import com.wjj.promise.util.PromiseUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wangjiajun
 */
public class Promise extends AbstractPromise {
    private PromiseHandler promiseHandler;
    private PromiseExecutor promiseExecutor;

    private Set<OnCompleteListener> onCompleteListenerSet = new HashSet<>();

    private ExecutorService threadPool;

    private Object externalInput;
    private Object promiseInput;

    private Future future;

    protected Promise(){}
    private Future startThread(Callable callable){
        if(this.threadPool !=null){
            this.future = this.threadPool.submit(callable);
        }else{
            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("promise-thread-%d").build();
            FutureTask futureTask = new FutureTask<Object>(callable);
            namedThreadFactory.newThread(futureTask).start();
            this.future = futureTask;
        }
        return this.future;
    }

    @Override
    public boolean cancel(){
        return this.future!=null&&this.future.cancel(true);
    }

    /**
     * 启动线程
     */
    private void executePromise(IPromise otherPromise){
        if(otherPromise!=null){
            //如果otherPromise处于等待态，promise需保持为等待态直至otherPromise被执行或拒绝
            if(otherPromise.getStatus().equals(Status.PENDING)){
                otherPromise.then(null);
            }
            if(otherPromise.getStatus().equals(Status.FULFILLED)){
                this.promiseInput = otherPromise.getResolvedData();
                startThread(this);
            }else{
                this.transferStatus(Status.REJECTED,otherPromise.getRejectedData());
            }
        }else{
            startThread(this);
        }
    }
    @Override
     public Object call(){
        if(this.getStatus().equals(Status.PENDING)){
            try{
                //执行具体的方法
                final Object result = this.promiseHandler.run(this.promiseExecutor);
                //直接返回数据，没有调用resolve
                if(this.getStatus().equals(Status.PENDING)){
                    this.transferStatus(Status.FULFILLED,result);
                }
            }catch (Exception e){
                this.transferStatus(Status.REJECTED,e);
            }
        }
        //调用监听
        if(this.onCompleteListenerSet !=null){
            this.onCompleteListenerSet.forEach(listener->listener.listen(this.getResolvedData(),this.getRejectedData()));
        }
        return this.getStatus().equals(Status.FULFILLED)?this.getResolvedData():this.getRejectedData();
    }
    @Override
    public IPromise then(OnFulfilledExecutor onFulfilledExecutor){
        return then(onFulfilledExecutor,null);
    }
    @Override
    public IPromise then(OnFulfilledExecutor onFulfilledExecutor, OnRejectedExecutor onRejectedExecutor){
        try {
            if(this.getStatus().equals(Status.PENDING)){
                this.future.get();
            }
            if(this.getStatus().equals(Status.FULFILLED)){
                if(onFulfilledExecutor!=null){
                    //调用回调
                    Object r = onFulfilledExecutor.onFulfilled(this.getResolvedData());
                    if(r instanceof IPromise){
                        return (IPromise)r;
                    }else{
                        return finalPromise(r,true);
                    }
                }else{
                    return finalPromise(this.getResolvedData(),true);
                }
            }else{
                if(onRejectedExecutor != null){
                    onRejectedExecutor.onRejected(this.getRejectedData());
                }
                return finalPromise(this.getRejectedData(),false);
            }
        }catch (Exception e){
            return finalPromise(e,false);
        }
    }
    @Override
    public void listen(OnCompleteListener onCompleteListener){
        this.onCompleteListenerSet.add(onCompleteListener);
        //如果是非pending状态，立即调用监听
        if(this.getStatus().equals(Status.FULFILLED)){
            onCompleteListener.listen(this.getResolvedData(),null);
        }else if(this.getStatus().equals(Status.REJECTED)){
            onCompleteListener.listen(this.getResolvedData(),this.getRejectedData());
        }
    }
    @Override
    public void pFinally(OnCompleteListener onCompleteListener){
        listen(onCompleteListener);
    }
    @Override
    public IPromise pCatch(OnCatchedExecutor onCatchedExecutor){
        try {
            if(this.getStatus().equals(Status.PENDING)){
                this.future.get();
            }
            if(this.getStatus().equals(Status.REJECTED)){
                if(onCatchedExecutor!=null){
                    Object r = onCatchedExecutor.onCatched(this.getRejectedData());
                    if(r instanceof IPromise){
                        return (IPromise)r;
                    }else{
                        return finalPromise(r,true);
                    }
                }
                return finalPromise(this.getRejectedData(),false);
            }
            return finalPromise(this.getResolvedData(),true);
        }catch (Exception e){
            return finalPromise(e,false);
        }
    }

    /**
     * 创建一个不可变的promise
     * @param data
     * @param fulfilled
     * @return
     */
    private IPromise finalPromise(Object data,boolean fulfilled){
        return new Builder().finalPromise(data,fulfilled).build();
    }

    protected Object getExternalInput(){
        return this.externalInput;
    }
    protected Object getPromiseInput(){
        return this.promiseInput;
    }
    @Override
    public Future getFuture(){
        return this.future;
    }

    /**
     * 创建一个固定容量的线程池
     * @param size
     * @return
     */
    public static ExecutorService pool(int size){
        return pool(size,size);
    }

    /**
     * 创建一个线程池
     * @param minSize 最小线程数
     * @param maxSize 最大线程数
     * @return
     */
    public static ExecutorService pool(int minSize,int maxSize){
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("promise-pool-%d").build();
        ExecutorService fixedThreadPool = new ThreadPoolExecutor(minSize,maxSize,
                10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),namedThreadFactory);
        return fixedThreadPool;
    }
    public static class Builder{
        private Promise promise = new Promise();
        private IPromise otherPromise;
        public Builder(){
        }

        /**
         * 指定一个线程池用于执行promise任务，如果不指定，每一个promise都将启动一个线程
         * @param threadPool
         * @return
         */
        public Builder pool(ExecutorService threadPool){
            if(threadPool!=null){
                this.promise.threadPool = threadPool;
            }
            return this;
        }

        /**
         * 向Promise注入一个外部参数，可以在指定PromiseHandler时通过PromiseExecutor.getExternalInput()获取<br/>
         * <pre>
         *   int i = 3;
         *   IPromise p = new Promise.Builder().externalInput(i).promiseHandler(new PromiseHandler() {
         *       public Object run(PromiseExecutor executor) {
         *           Integer args = (Integer) executor.getExternalInput();
         *           executor.resolve(args*2);
         *           return null;
         *       }
         *   }).build();
         * </pre>
         * @param externalInput
         * @return
         */
        public Builder externalInput(Object externalInput){
            this.promise.externalInput = externalInput;
            return this;
        }

        /**
         * 指定promise处理器
         * @param promiseExecutor
         * @return
         */
        public Builder promiseHandler(PromiseHandler promiseExecutor){
            this.promise.promiseHandler = promiseExecutor;
            this.promise.promiseExecutor = new PromiseExecutor(this.promise);
            return this;
        }

        /**
         * 创建一个不可变的promise
         * @param finalData
         * @param fulfilled
         * @return
         */
        public Builder finalPromise(final Object finalData,boolean fulfilled){
            if(fulfilled){
                this.promise.transferStatus(Status.FULFILLED,finalData);
            }else{
                this.promise.transferStatus(Status.REJECTED,finalData);
            }
            return this;
        }

        /**
         * 指定一个promise x，使当前promise接受 x 的状态
         * <li>如果 x 处于pending， 当前promise 需保持为pending直至 x 转为fulfilled或rejected</li>
         * <li>如果 x 处于fulfilled，用x的终值值执行当前promise，可以在指定PromiseHandler时通过PromiseExecutor.getPromiseInput()获取</li>
         * <li>如果 x 处于拒绝态，用相同的据因拒绝当前promise执行</li>
         * <pre>
         *ExecutorService fixedPool = Promise.pool(1);
          IPromise promise1 = new Promise.Builder().pool(fixedPool).promiseHandler(executor->3).build();
          IPromise promise2 = new Promise.Builder().pool(fixedPool).promise(promise1).promiseHandler(executor->4+(Integer) executor.getPromiseInput())
            .build()
            .then(resolvedData->{
                System.out.println(resolvedData);
                return resolvedData;
            }, rejectedReason-> rejectedReason.printStackTrace());
         * </pre>
         * 执行结果为7
         * @param promise
         * @return
         */
        public Builder promise(IPromise promise){
            this.otherPromise = promise;
            return this;
        }
        public IPromise build(){
            if(this.promise.getStatus().equals(Status.PENDING)){
                this.promise.executePromise(this.otherPromise);
            }
            return this.promise;
        }
    }

    /**
     * 参考 waitAll {@link com.wjj.promise.Promise#all(ExecutorService threadPool, IPromise[] promises)}
     * @return IPromise
     */
    public static IPromise all(final IPromise ...promises){
        return all(null,promises);
    }
    /**
     * 将多个 Promise 实例p1,...pn，包装成一个新的 Promise 实例 p,只有当p1-pn的状态都转为fulfilled时，p的状态才为fulfilled，将
     * p1-pn的终值包装为一个数组Object[r1,...rn]作为p的终值。<br/>
     * 只要p1-pn中任意一个被rejected，p的状态就转为rejected，将第一个被rejected的promise的拒因作为p的拒因，并尝试取消其余promise的执行(内部调用future.cancel(true))
     * @param threadPool 线程池,指定新的实例 p的运行环境，为null时会为实例 p开启一个新线程。
     *                   线程池的容量最好比promises长度大1，避免实例p处于队列等待
     * @param promises promise数组
     * @return IPromise
     */
    public static IPromise all(ExecutorService threadPool,final IPromise ...promises){
        if(promises==null){
            return new Promise.Builder().finalPromise(null,true).build();
        }
        if(promises.length==0){
            return new Promise.Builder().finalPromise(new Object[0],true).build();
        }
        IPromise rejectedPromise = PromiseUtil.getFirstRejected(promises);
        if(rejectedPromise!=null){
            PromiseUtil.cancel(promises);
            return new Promise.Builder().finalPromise(rejectedPromise.getRejectedData(),false).build();
        }
        return new Promise.Builder().promiseHandler(handler->{
            final PromiseCountDownLatch counter = new PromiseCountDownLatch(promises.length);
            for(IPromise p:promises){
                p.listen((resolvedData, throwable) -> {
                    if(throwable!=null){
                        counter.countDownAll();
                    }else{
                        counter.countDown();
                    }
                });
            }
            counter.await();
            IPromise rejectedPromise2 = PromiseUtil.getFirstRejected(promises);
            if(rejectedPromise2!=null){
                PromiseUtil.cancel(promises);
                handler.reject(rejectedPromise2.getRejectedData());
            }else{
                final Object[] datas = new Object[promises.length];
                for(int i=0;i<promises.length;i++){
                    IPromise p = promises[i];
                    datas[i] = p.getResolvedData();
                }
                handler.resolve(datas);
            }
            return null;
        }).pool(threadPool).build();
    }

    /**
     * 参考 waitAll {@link com.wjj.promise.Promise#waitAll(ExecutorService threadPool, IPromise[] promises)}
     * @return IPromise
     */
    public static IPromise waitAll(final IPromise ...promises){
        return waitAll(null,promises);
    }
    /**
     * 将多个 Promise 实例p1,...pn，包装成一个新的 Promise 实例 p,等待p1-pn的状态全部转为fulfilled或rejected，p的状态转为fulfilled，将
     * p1-pn的终值包装为一个数组Object[r1,...rn]作为p的终值。r1...rn的值取决于p1...pn的最终状态<br/>
     * 假如p2异常结束，那么r2为一个throwable实例<br/>
     * ，不同于all，即便只要p1-pn中任意一个被rejected，p都会等待全部的promise执行完成
     * @param threadPool 线程池,指定新的实例 p的运行环境，为null时会为实例 p开启一个新线程。
     *                   线程池的容量最好比promises长度大1，避免实例p处于队列等待
     * @param promises promise数组
     * @return IPromise
     */
    public static IPromise waitAll(ExecutorService threadPool,final IPromise ...promises){
        if(promises==null){
            return new Promise.Builder().finalPromise(null,true).build();
        }
        if(promises.length==0){
            return new Promise.Builder().finalPromise(new Object[0],true).build();
        }
        return new Promise.Builder().promiseHandler(handler->{
            final PromiseCountDownLatch counter = new PromiseCountDownLatch(promises.length);
            for(IPromise p:promises){
                p.listen((resolvedData, throwable) -> {
                    counter.countDown();
                });
            }
            counter.await();
            final Object[] datas = new Object[promises.length];
            for(int i=0;i<promises.length;i++){
                IPromise p = promises[i];
                datas[i] = Status.FULFILLED.equals(p.getStatus())?p.getResolvedData():p.getRejectedData();
            }
            return datas;
        }).pool(threadPool).build();

    }


    /**
     * 参考 waitAll {@link com.wjj.promise.Promise#race(ExecutorService threadPool, IPromise[] promises)}
     * @return
     */
    public static IPromise race(IPromise ...promises){
        return race(null,promises);
    }
    /**
     * 将多个 Promise p1,...pn实例，包装成一个新的 Promise 实例 p，只要p1-pn有一个状态发生改变，p的状态立即改变
     * 并尝试取消其余promise的执行(内部调用future.cancel(true))<br/>
     * 第一个改变的promise的状态和终值作为p的状态和终值
     * @param threadPool 线程池,指定新的实例 p的运行环境，为null时会为实例 p开启一个新线程。
     *                   线程池的容量最好比promises长度大1，避免实例p处于队列等待
     * @param promises
     * @return
     */
    public static IPromise race(ExecutorService threadPool,IPromise ...promises){
        if(promises==null){
            return new Promise.Builder().finalPromise(null,true).build();
        }
        if(promises.length==0){
            return new Promise.Builder().finalPromise(new Object[0],true).build();
        }
        IPromise finalPromise = PromiseUtil.getFinal(promises);
        if(finalPromise!=null){
            PromiseUtil.cancel(promises);
            return PromiseUtil.cloneFinal(finalPromise);
        }
        return new Promise.Builder().promiseHandler(handler->{
            final PromiseCountDownLatch counter = new PromiseCountDownLatch(promises.length);
            for(IPromise p:promises){
                p.listen((resolvedData, throwable) -> counter.countDownAll());
            }
            counter.await();
            IPromise finalPromise2 = PromiseUtil.getFinal(promises);
            PromiseUtil.cancel(promises);
            if(Status.FULFILLED.equals(finalPromise2.getStatus())){
                handler.resolve(finalPromise2.getResolvedData());
            }else{
                handler.reject(finalPromise2.getRejectedData());
            }
            return null;
        }).pool(threadPool).build();
    }

    /**
     * 创建一个终值为null、fulfilled状态的promise
     * @return
     */
    public static IPromise resolve(){
        return new Promise.Builder().finalPromise(null,true).build();
    }
    /**
     * 创建一个终值为object、fulfilled状态的promise
     * @return
     */
    public static IPromise resolve(Object object){
        return new Promise.Builder().finalPromise(object,true).build();
    }

    /**
     * 将object的then方法以异步方式执行，then方法的执行结果作为Promise的终值
     * @param object
     * @param args then方法的参数，必须按顺序包含在List中
     * @return
     */
    public static IPromise resolve(Object object,List<Object> args){
        return resolve(object,"then",args);
    }

    /**
     * 将object的指定方法以异步方式执行，该方法的执行结果作为Promise的终值
     * @param object
     * @param methodName 方法名
     * @param args 参数 必须按顺序包含在List中
     * @return
     */
    public static IPromise resolve(Object object,String methodName,List<Object> args){
        if(null==object){
            return new Promise.Builder().finalPromise(null,true).build();
        }
        Class[] cls = new Class[args.size()];
        for(int i=0;i<args.size();i++){
            cls[i] = args.get(i).getClass();
        }
        try {
            Method method = object.getClass().getMethod(methodName,cls);
            return new Promise.Builder().promiseHandler(executor -> method.invoke(object,args.toArray())).build();
        } catch (NoSuchMethodException e) {
            return new Promise.Builder().finalPromise(e,false).build();
        }
    }

    /**
     * 创建一个拒因为reason、rejected状态的promise
     * @param reason
     * @return
     */
    public static IPromise reject(Throwable reason){
        return new Promise.Builder().finalPromise(reason,false).build();
    }

    /**
     * 将object的指定方法以同步方式执行，该方法的执行结果作为Promise的终值，如果object为IPromise实例，将忽略methodName和args参数，异步执行该实例。<br/>
     * 该方法是以Promise统一处理同步和异步方法，不管object是同步操作还是异步操作，都可以使用then指定下一步流程,用pCatch方法捕获异常,避免开发中出现以下情况
     * <pre>
     *     try{
     *         object.doSomething(args1,args2);//可能会抛出异常
     *         promise.then(resolvedData->{
     *             //一些逻辑
     *         }).then(resolvedData->{
     *             //一些逻辑
     *         }).pCatch(e->{
     *             //异常处理逻辑
     *         })
     *     }catch(Exception e){
     *         //异常处理逻辑
     *     }
     * </pre>
     * 使用pTry，可以简化异常处理
     * <pre>
     *     List args = new ArrayList(){args1,args2};
     *     Promise.pTry(object,"doSomething",args)
     *      .then(resolvedData->{
     *             //一些逻辑
 *          }).then(resolvedData->{
 *             //一些逻辑
 *          }).pCatch(e->{
 *             //异常处理逻辑
 *          })
     * </pre>
     * @param object
     * @param methodName
     * @param args 指定方法的参数，必须按顺序包含在List中
     * @return
     */
    public static IPromise pTry(Object object,String methodName,List<Object> args){
        if(object instanceof IPromise){
            return (IPromise)object;
        }
        try {
            Class[] cls = new Class[args.size()];
            for(int i=0;i<args.size();i++){
                cls[i] = args.get(i).getClass();
            }
            Method method = object.getClass().getMethod(methodName,cls);
            Object result = method.invoke(object,args.toArray());
            return new Promise.Builder().finalPromise(result,true).build();
        }catch (Exception e){
            return new Promise.Builder().finalPromise(e,false).build();
        }
    }

}
