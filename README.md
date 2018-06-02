# promise
Promise使用commonJs规范中的一中异步编程解决方案，比传统的解决方案—回调函数和事件—更合理和更强大。   
在java中，多线程编程相对来说是一件比较麻烦的事情，虽然在java `concurrent`包中提供了一系列工具，但是我们想知道线程何时结束、获取线程执行结果、异常处理一直是件比较麻烦的事。`future.get()`会阻塞当前线程。Goolge-Guava Concurrent中的Service和ServiceManager很好地解决了这一问题，但是使用繁琐。某些时候我们需要线程a结束后，拿到线程a的结果立即执行线程b，可能会使用guava的ListenableFuture添加监听，可能得逻辑如下
```java
public static void main(String[] args) throws Exception{
    ExecutorService pool = Executors.newFixedThreadPool(1);
    ListeningExecutorService service = MoreExecutors.listeningDecorator(pool);
    MoreExecutors.addDelayedShutdownHook(service,3,TimeUnit.SECONDS);
    ListenableFuture<Integer> listenableFuture = service.submit(()->{
        Random random = new Random();
        int i=0;
        while (i<3){
            i++;
            Console.log(random.nextInt(100));
            Thread.sleep(100);
        }
        return random.nextInt(100);
    });
    Futures.addCallback(listenableFuture, new FutureCallback<Integer>() {
        @Override
        public void onSuccess(@Nullable Integer result) {
            Console.log("执行结果："+result);
            //执行线程b
            .....
            Console.log("执行线程b");    
            .....
        }
        @Override
        public void onFailure(Throwable t) {
            Console.log("线程执行发生错误");
            t.printStackTrace();
        }
    });
    Console.log("主程序结束了");
}
```
输出结果
```
主程序结束了
47
62
48
执行结果：33
执行线程b
```
可以看到线程b的执行是嵌套在线程a的成功回调中的，然后线程b又是一个回调，做前端的知道ajax的回调地狱是多么痛苦和复杂
```javascript
ajax(xxx,function(r1){
    ajax(xxx,function(r2)){
        ajax(xxx,function(r3){
            
        })
    }
})
```
在上面java例子中，“主程序结束了”最先被打印，假如需要在springMVC的controller中返回线程b的执行结果，意味着需要在线程b执行结束前阻塞当前线程，这又该怎么做?如果以后需求变化，需要线程a和线程a1共同的执行结果去执行线程b，那改动会相当麻烦。   
在前端开发中，也经常会遇到同样的问题，commonJs提出的Promise A+规范就很好地解决了这一个问题，ES6已经实现了这个规范。
* [Promise A+规范](http://malcolmyu.github.io/malnote/2015/06/12/Promises-A-Plus/#note-4)
* [ES6 Promise对象](http://es6.ruanyifeng.com/#docs/promise)  
### java Promise
java promise是一个开源项目，是Promise A+规范的java实现版本，使用Promise可以很好地解决上面例子的监听回调问题，以第一个例子为例，用java Promise实现如下
```java
ExecutorService pool = Promise.pool(1);
IPromise promiseA = new Promise.Builder().pool(pool).promiseHanler(executor -> {
    //PromiseA的业务逻辑
    Random random = new Random();
    int i=0;
    while (i<3){
        i++;
        System.out.println(random.nextInt(100));
        Thread.sleep(100);
    }
    return random.nextInt(100);
}).build();
promiseA.then(resultA -> {//PromiseB的成功回调
    //在promiseA的回调中创建PromiseB
    IPromise promiseB = new Promise.Builder().pool(pool).externalInput(resultA)
            .promiseHanler(executor -> {
                //promiseB的业务逻辑
                String bResult = "b:"+executor.getExternalInput();
                return bResult;
            }).build();
    //返回PromiseB
    return promiseB;
}).then(resultB -> {//PromiseB的成功回调
    System.out.println(resultB);
    return resultB;
}).pCatch(e->{//捕获PromiseA和PromiseB的异常
    e.printStackTrace();
    return null;
});
pool.shutdown();
System.out.println("主程序结束了");
```
打印结果如下
```
65
97
85
b:24
主程序结束了
```
从上面可以看到PromiseA和PromiseB是链式调用的，在promiseA的回调中创建并返回了promiseB，但是promiseB的回调是在外层调用的，假如需要顺序执行a->b-c->d四个线程，调用顺序如下
```
new PromiseA()
.then(dataA->new PromiseB())//A的回调
.then(dataB->new PromiseC())//B的回调
.then(dataC->new PromiseD())//C的回调
.then(dataD->xxx)//D的回调
.pCatch(error->xxxx)//捕获中间可能产生的异常
```
### Docs
#### promise规范
promise规范可以参考 [Promise A+规范](http://malcolmyu.github.io/malnote/2015/06/12/Promises-A-Plus/#note-4)。其中[ES6 Promise对象](http://es6.ruanyifeng.com/#docs/promise) 在Promise A+规范上做了一些补充。java promise在使用上基本与ES6 Promise对象保持一致，部分地方有些许不同，后面会做出说明。
Promise的三个状态
* pending:等待态，对应线程未执行或执行中
* fulfilled:完成态，对应线程正常执行完毕，其执行结果称为**终值**
* rejected:拒绝态，对应线程异常结束，其异常原因称为**拒因**   
  状态转移只能由pending->fulfilled或pending->rejected，状态一旦发生转移无法再次改变。
#### Promise
Promise是Ipromise的实现，部分接口如下
##### IPromise then(OnFulfilledExecutor onFulfilledExecutor)
* 如果当前promise处于pending状态，阻塞当前线程，等待promise状态转变为fulfilled或rejected
* 如果处于fulfilled状态，执行onFulfilledExecutor.onFulfilled(resolvedData)回调。
     * 如果回调返回一个Promise对象a，以a作为then方法的返回值，如果回调返回一个普通对象obj，以obj作为终值、状态为fulfilled包装一个新Promise作为then方法的返回值
     * 如果执行回调过程中产生异常e,返回一个以e作为拒因、状态为rejected的新Promise，并拒绝执行接下来的所有Promise直到遇到pCatch。
* 如果处于rejected状态，执行onRejectedExecutor.onRejected(rejectReason)回调，返回一个以当前promise的异常作为拒因、状态为rejected的新Promise，并拒绝执行接下来的所有Promise直到遇到pCatch或pFinally   
     参数：
#### IPromise pCatch(OnCatchedExecutor onCatchedExecutor);
then(null,onRejectedExecutor)的别名，但返回不同于then，出现异常时可以选择不拒绝接下来Promise的执行，可用于异常修正，类似于try{}catch{}   
该方法会尝试捕获当前promise的异常,最终返回一个新Promise,当被捕获Promise处于不同的状态时有不同的行为
* pending：阻塞当前线程，等待pending转变为fulfilled或rejected，行为同then
* fulfilled：不执行回调，以当前Promise终值和状态返回一个全新的Promise
* rejected：执行onCatched(Throwable catchReason)回调。
     * 如果onCatched方法返回一个Promise，以这个Promise作为最终返回。
     * 如果onCatched方法返回一个非Promise对象obj，以obj作为终值、fulfilled状态返回一个全新的对象。
     * 如果执行回调过程中产生异常e，以e为拒因、状态为rejected返回一个新的Promise，并拒绝执行接下来的所有Promise直到再次遇到pCatch
##### void listen(OnCompleteListener onCompleteListener);
指定一个监听器，在promise状态转为fulfilled或rejected调用，该方法不会阻塞线程执行，可以多次调用指定多个监听器
##### void pFinally(OnCompleteListener onCompleteListener);
listen的别名，行为同listen  
##### Status getStatus()
获取promise的当前状态
##### Object getResolvedData()
获取promise fulfilled状态下的终值，其余状态下时为null
##### Throwable getRejectedData()
获取promise rejected状态下的拒因，其余状态下为null
##### Future getFuture()
获取promise对应异步任务的future
##### boolean cancel()
尝试取消promise对应的异步任务，底层调用future.cancel(true)。fulfilled或rejected状态下无效。
#### Promise.Builder
Promise对象生成器
##### Builder pool(ExecutorService threadPool)
指定一个线程池用于执行promise任务,如果不指定，每一个promise都将启动一个线程
##### Builder promiseHanler(PromiseHandler promiseExecutor)
指定promise处理器,在promiseHanler的run方法中实现线程的具体业务逻辑
##### Builder externalInput(Object externalInput)
向Promise注入一个外部参数，可以在指定PromiseHandler时通过PromiseExecutor.getExternalInput()获取
```java
int i = 3;
IPromise p = new Promise.Builder()
.externalInput(i).promiseHanler(new PromiseHandler() {
    public Object run(PromiseExecutor executor) {
        Integer args = (Integer) executor.getExternalInput();
        return args*2;
    }
}).build();
```
##### Builder promise(IPromise promise)
指定一个promise x，使当前promise接受 x 的状态
* 如果 x 处于pending， 当前promise 需保持为pending直至 x 转为fulfilled或rejected
* 如果 x 处于fulfilled，用x的终值值执行当前promise，可以在指定PromiseHandler时通过PromiseExecutor.getPromiseInput()获取
* 如果 x 处于拒绝态，用相同的据因拒绝当前promise执行
```java
ExecutorService fixedPool = Promise.pool(1);
IPromise promise1 = new Promise.Builder().pool(fixedPool).promiseHanler(executor->3).build();
IPromise promise2 = new Promise.Builder().pool(fixedPool)
    .promise(promise1)
    .promiseHanler(executor->4+(Integer) executor.getPromiseInput())
.build()
.then(resolvedData->{
    System.out.println(resolvedData);
    return resolvedData;
}, rejectedReason-> rejectedReason.printStackTrace());
```
最终结果返回7,。如果promise1在执行过程中抛出异常e，promise2将被拒绝执行，将会以e作为拒因，状态为rejected返回一个新的Promise，最终会执行`rejectedReason-> rejectedReason.printStackTrace()`回调。
##### IPromise build()
创建一个Promise实例
#### Promise的静态方法
##### static IPromise all(IPromise ...promises)
将多个 Promise 实例p1,...pn，包装成一个新的 Promise 实例 p,只有当p1-pn的状态都转为fulfilled时，p的状态才为fulfilled，此时p1-pn的返回值包装为一个数组Object[r1,...rn]作为p的终值。   
只要p1-pn中任意一个被rejected，p的状态就转为rejected，将第一个被rejected的promise的拒因作为p的拒因，并尝试取消其余promise的执行(内部调用future.cancel(true))
##### static IPromise race(IPromise ...promises)
将多个 Promise p1,...pn实例，包装成一个新的 Promise 实例 p，只要p1-pn有一个状态发生改变，p的状态立即改变。并尝试取消其余promise的执行(内部调用future.cancel(true))   
第一个改变的promise的状态和数据作为p的状态和数据
##### static IPromise resolve()
创建一个终值为null、fulfilled状态的promise
##### static IPromise resolve(Object object)
创建一个终值为object、fulfilled状态的promise
##### static IPromise resolve(Object object,List<Object> args)
将object的then方法以异步方式执行，then方法的执行结果作为Promise的终值
##### static IPromise resolve(Object object,String methodName,List<Object> args)
将object的指定方法以异步方式执行，该方法的执行结果作为Promise的终值，目标方法的参数必须按顺序包含在List中，如object.doSomething(int a,Map b)，用resolve执行为
```java
List args = new ArrayList()
args.add(1);
args.add(map)
Promise.resolve(object,"doSomething",args);
```
##### static IPromise reject(Object reason)
创建一个拒因为reason、rejected状态的promise
##### static IPromise pTry(Object object,String methodName,List<Object> args)
将object的指定方法以同步方式执行，该方法的执行结果作为Promise的终值，如果object为IPromise实例，将忽略methodName和args参数，异步执行该实例。   
该方法是以Promise统一处理同步和异步方法，不管object是同步操作还是异步操作，都可以使用then指定下一步流程,用pCatch方法捕获异常,避免开发中出现以下情况
```
try{
  object.doSomething(args1,args2);//可能会抛出异常
  promise.then(resolvedData->{
      //一些逻辑
  }).then(resolvedData->{
      //一些逻辑
  }).pCatch(e->{
      //异常处理逻辑
  })
}catch(Exception e){
  //异常处理逻辑
}
```
使用pTry，可以简化异常处理
```java
List args = new ArrayList(){args1,args2};
Promise.pTry(object,"doSomething",args)
.then(resolvedData->{
      //一些逻辑
}).then(resolvedData->{
  //一些逻辑
}).pCatch(e->{
  //异常处理逻辑
})
```
#### PromiseHandler
定义异步逻辑的接口
##### Object run(PromiseExecutor executor)throws Exception;
run方法中实现具体的业务逻辑,最终run方式是在线程的call方法执行，如果run方法中含有wait、sleep...等锁操作，可能需要自行处理`InterruptedException`。因为该线程可能被外部调用cancel()或interrupt()方法
#### PromiseExecutor
promise状态处理
##### void resolve(final Object args)
将Promise对象的状态从“未完成”变为“成功”（即从pending变为fulfilled）。注意该方法一经调用，promise状态将不可改变，如下例，在调用executor.resolve(3);后，return之前抛出一个异常，promise的状态依旧是fulfilled，终值为3。
```java
new Promise.Builder().promiseHanler(new PromiseHandler(){
    @Override
    public Object run(PromiseExecutor executor) {
        executor.resolve(3);
        throw new RuntimeException("error");
        return null;
    }
}).build()
```
在run方法中executor.resolve(3)等同于return 3
```java
@Override
public Object run(PromiseExecutor executor) {
    return 3;
}
```
大多数情况下建议直接使用return返回promise的终值。
##### void reject(final Throwable args)
将Promise对象的状态从“未完成”变为“失败”（即从pending变为fulfilled）
##### Object getExternalInput()
获取通过`new Promise.Builder().externalInput(Object externalInput)`方法注入的参数，具体参考`Promise.Builder#externalInput(Object externalInput)`
##### Object getPromiseInput()
获内部promise的执行结果。通过new Promise.Builder().promise(promise1)指定的promise1的执行结果。具体参考
`Promise.Builder#promise(IPromise promise)`
#### OnFulfilledExecutor
fulfilled回调接口
##### Object onFulfilled(Object resolvedData)throws Exception;
状态转为fulfilled时的回调，返回值可以是IPromise实例或普通对象。如果object是IPromise实例，object作为then方法的返回值，如果object是个普通对象，以object作为终值、状态为fulfilled包装一个新Promise作为then方法的返回值
#### OnRejectedExecutor
rejected回调接口
##### void onRejected(Throwable rejectReason)throws Exception;
当Promise转变为rejected状态时的回调
##### OnCatchedExecutor
rejected回调接口
##### Object onCatched(Throwable catchReason)throws Exception;
当发生异常时的回调,最终返回一个Promise或普通对象，如果是一个普通对象，这个对象将作为下一个Promise的终值
#### OnCompleteListener
##### void listen(Object resolvedData,Throwable e);
当Promise执行结束时的回调(无论是fulfilled还是rejected)
* resolvedData fulfilled状态时的终值，rejected状态时为null
* e rejected状态时的异常信息,fulfilled状态时为null
#### 示例
##### 示例1：基本使用
```java
new Promise.Builder().promiseHanler(new PromiseHandler(){
    @Override
    public Object run(PromiseExecutor executor) {
        executor.resolve(3);//返回异步执行结果3
        return null;
    }
}).build().then(new OnFulfilledExecutor() {
    @Override
    public Object onFulfilled(Object resolvedData) {
        Integer i = ((Integer)resolvedData)+1;//获取上一个promsie执行结果3,执行+1
        System.out.println(i);//输出执行结果4
        //创建一个新的promise，将3作为该promise的输入
        IPromise p = new Promise.Builder().externalInput(i).promiseHanler(new PromiseHandler() {
            @Override
            public Object run(PromiseExecutor executor) {
                //获取外部输入4
                Integer args = (Integer) executor.getExternalInput();
                executor.resolve(args*2);//执行 4x2
                return null;
            }
        }).build();
        return p;//返回该promise p
    }
})
.then(new OnFulfilledExecutor() {//执行p的回调
    @Override
    public Object onFulfilled(Object args) {
        System.out.println(args);//输出p的执行结果
        return args;
    }
}, new OnRejectedExecutor() {//捕获可能出现的异常
    @Override
    public void onRejected(Throwable rejectedReason) throws Exception {
        rejectedReason.printStackTrace();
    }
});
```
结果
```
4
8
```
##### 示例2
```java
ExecutorService fixedPool = Promise.pool(1);//创建一个线程池
//创建promise1
IPromise promise1 = new Promise.Builder().pool(fixedPool).promiseHanler(executor->3).build();
//创建promise2
IPromise promise2 = new Promise.Builder().pool(fixedPool)
    .promise(promise1)//让promise2接受promise1的状态，优先执行promise1
    .promiseHanler(executor->{
        //获取promise1的执行结果，执行promise2的逻辑
        return 4+(Integer) executor.getPromiseInput();
    })
    .build()
    .then(resolvedData->{
        System.out.println(resolvedData);//打印promise2的执行结果 
        return resolvedData;
    }, rejectedReason-> rejectedReason.printStackTrace());
System.out.println("end");
fixedPool.shutdown();
```
结果
```
7
end
```
##### 示例3：错误处理
```
new Promise.Builder().promiseHanler(executor -> 3).build().then(resolvedData->{
    System.out.println("a:"+resolvedData);
    return new Promise.Builder().promiseHanler(executor -> {
        executor.reject(new RuntimeException("err"));//抛出异常
        return null;
    }).build();
}).then(resolvedData1 -> {//fulfilled回调
    System.out.println("b:"+resolvedData1);
    return resolvedData1;
},rejectReason -> {//rejected回调
    System.err.println("c:"+rejectReason);
});
```
结果
```
a:3
c:java.lang.RuntimeException: err
```

