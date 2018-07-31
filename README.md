java promise（[GitHub](https://github.com/zhanyingf15/promise)）是Promise A+规范的java实现版本。Promise A+是commonJs规范提出的一种异步编程解决方案，比传统的解决方案—回调函数和事件—更合理和更强大。promise实现了Promise A+规范，包装了java中对多线程的操作，提供统一的接口，使得控制异步操作更加容易。实现过程中参考文档如下：
* [Promise A+规范](http://malcolmyu.github.io/malnote/2015/06/12/Promises-A-Plus/#note-4)
* [ES6 Promise对象](http://es6.ruanyifeng.com/#docs/promise)   

基本使用：修改pom.xml
```xml
<repositories>
    <repository>
      <id>wjj-maven-repo</id>
      <url>https://raw.github.com/zhanyingf15/maven-repo/master</url>
    </repository>
</repositories>
```
```xml
<dependency>
  <groupId>com.wjj</groupId>
  <artifactId>promise</artifactId>
  <version>1.0.0</version>
</dependency>
```
如果maven settings.xml使用了mirror配置，修改mirrorOf
```xml
<mirror>
  <id>nexus</id>
  <mirrorOf>*,!wjj-maven-repo</mirrorOf> 
  <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
</mirror>
```
```java
IPromise promise = new Promise.Builder().promiseHanler(new PromiseHandler() {
    @Override
    public Object run(PromiseExecutor executor) throws Exception {
        return 2*3;
    }
}).build();
```
上面的例子中创建了一个promise对象，指定PromiseHandler实现，在run方法中写具体的业务逻辑，类似于Runable的run方法。promise对象一经创建，将立即异步执行。推荐使用lambda表达式，更加简洁。
```java
IPromise promise = new Promise.Builder().promiseHanler(executor -> {
    return 2*3;
}).build();
```
获取promise的执行结果通常使用两个方法`then`和`listen`，前者是阻塞的后者是非阻塞的。then方法返回一个新的promise对象，因此支持链式调用。
```java
new Promise.Builder().promiseHanler(executor -> {//promise0
    return 2*3;
}).build().then(resolvedData -> {//返回一个新的promise1
    System.out.println(resolvedData);
    return (Integer)resolvedData+1;
}).then(res2->{
    System.out.println(res2);
    //创建一个新的promise2并返回
    return new Promise.Builder().externalInput(res2).promiseHanler(executor -> {
        return (Integer)executor.getExternalInput()+2;
    }).build();
}).then(res3->{
    System.out.println(res3);
    return res3;
});
```


从上面可以看到promise0、promise1和Promise2是链式调用的，每一次then方法都返回一个新的promise。在then方法的回调中，如果返回的是一个非promise对象，那么promise被认为是一个fulfilled状态的promise，如果返回的是一个promsie实例，那么该实例将会异步执行。   
假如需要异步顺序执行a->b-c->d四个线程，调用顺序如下
```
new PromiseA()
.then(dataA->new PromiseB())//A的回调
.then(dataB->new PromiseC())//B的回调
.then(dataC->new PromiseD())//C的回调
.then(dataD->xxx)//D的回调
.pCatch(error->xxxx)//捕获中间可能产生的异常
```
### DOCS
docs文档参考[promise wiki](https://github.com/zhanyingf15/promise/wiki)
#### promise规范
promise规范可以参考 [Promise A+规范](http://malcolmyu.github.io/malnote/2015/06/12/Promises-A-Plus/#note-4)。其中[ES6 Promise对象](http://es6.ruanyifeng.com/#docs/promise) 在Promise A+规范上做了一些补充。java promise在使用上基本与ES6 Promise对象保持一致，部分地方有些许不同。
Promise的三个状态
* pending:等待态，对应线程未执行或执行中
* fulfilled:完成态，对应线程正常执行完毕，其执行结果称为**终值**
* rejected:拒绝态，对应线程异常结束，其异常原因称为**拒因**   
状态转移只能由pending->fulfilled或pending->rejected，状态一旦发生转移无法再次改变。
### 使用
```java
IPromise promise = new Promise.Builder().promiseHandler(handler->2*3).build();//mark1
promise.then(resolvedData -> {
    System.out.println(resolvedData);
    return null;
});
```
创建一个线程非常简单，mark1标注的行创建一个IPromise实例promise，并指定异步逻辑，这里简单地做了个乘法操作。promise实例一经创建，异步逻辑将立即执行，执行结果或执行中抛出的异常将保存在promise实例中。可以在创建promise实例时指定一个线程池。
```java
ExecutorService pool = Promise.pool(5,10);
IPromise promise = new Promise.Builder().pool(pool).promiseHandler(handler->2*3).build();
```
上面创建了一个最小为5最大为10的线程池，promise实例对应的线程将被提交的线程池中执行。promise可以通过then或listen方法获取执行结果，then方法是阻塞的而listen是非阻塞的。   
通常情况下异步逻辑需要访问外部参数，而外部参数往往并不是final的，promise提供了输入外部参数到内部逻辑的方法`externalInput`。
```java
Map<String,String> map = ImmutableMap.of("name","张三");
IPromise promise = new Promise.Builder().externalInput(map).promiseHandler(handler->{
    Map<String,String> m = (Map<String,String>)handler.getExternalInput();
    return "你好："+m.get("name");
}).build();
```
#### resolve和reject
resolve和reject是PromiseExecutor类的方法，resolve方法将promise状态由pending->fulfilled，reject方法将promise状态由pending->rejected。如果promise已经是非pending状态，resolve和reject调用将无效。
```java
new Promise.Builder().promiseHandler(handler->{
    int a = 2*3;
    handler.resolve(a);
    return null;
}).build().listen(((resolvedData, e) -> {
    System.out.println(resolvedData);
}));
```
上面的例子中，计算出a的值，手动将promise状态转移为fulfilled，并将a的值作为promise的终值。同样也可以手动调用`handler.reject(e)`将promise状态转为rejected，e(*e为Throwable实例*)作为promise的据因。

在前面和后面的例子中，并没有显示调用handler.resolve(x)方法，而是return具体的结果。因为在 resolve方法之后return之前程序抛出异常，该异常不会更改promise的状态，异常会被内部吞掉，resolve方法已经将promise的状态修改为fulfilled了。
```java
new Promise.Builder().promiseHandler(handler->{
    int a = 2*3;
    handler.resolve(a);
    throw new RuntimeException("err");
}).build()
        .listen(((resolvedData, e) -> {
    System.out.println(resolvedData);
    System.out.println(e==null);
}));
```
打印结果
```
6
true
```
上面的例子中，手动调用resolve方法后，后续逻辑即便是抛出了异常，e仍然是null，因为promise状态已经转变为fulfilled，后续的所有逻辑(包括return的值)已经跟promise的最终状态无关，后续异常和返回结果将被忽略。因此非特殊情况下不建议直接调用resolve方法，而是直接return返回执行结果。这种方式是handler.resolve(x)的隐式做法，return的结果将作为promise的终值
#### 异常捕获
promise的rejected状态对应线程异常结束(运行时异常或手动调用executor.reject(e))，其据因保存了异常实例，这些异常被promise内部吞掉，并不会抛出到当前运行环境中，所有try...catch是无法捕获到promise内部逻辑抛出的异常的。promise提供了多种方式可以侦测到内部异常：
* `then(onFulfilledExecutor,onRejectedExecutor)`  [API 参考wiki-then](https://github.com/zhanyingf15/promise/wiki#ipromise-thenonfulfilledexecutor-onfulfilledexecutor-onrejectedexecutor-onrejectedexecutor)
* `listen(onCompleteListener),pFinally(onCompleteListener)`  [API 参考wiki-listen](https://github.com/zhanyingf15/promise/wiki#void-listenoncompletelistener-oncompletelistener)
* `pCatch(onCatchedExecutor)`  [API 参考wiki-pCatch](https://github.com/zhanyingf15/promise/wiki#ipromise-pcatchoncatchedexecutor-oncatchedexecutor)

then方法的第二个参数是可选参数，如果发生异常，第二个参数回调将被执行。  

listen和pFinally行为是一致的，onCompleteListener的listen(Object resolvedData,Throwable e)方法第二参数为异常对象，如果发生异常，e为异常实例，否则为null。   

pCatch是推荐使用的异常捕获方式，then和listen对于异常只能“观察”不能修正，在promise链式调用时，一旦发生异常，then方法只能观察到异常发生，但是异常仍会向调用链后方传递，并拒绝后面promise的执行。pCatch不同，它捕获到异常后，可以自行根据业务逻辑对异常处理，继续执行后面的promise链。
```java
new Promise.Builder().promiseHandler(executor -> 3).build().then(resolvedData->{//p1
    System.out.println("a:"+resolvedData);
    return new Promise.Builder().promiseHandler(executor -> {//p2
        executor.reject(new RuntimeException("err"));
        return resolvedData;
    }).build();
}).then(resolvedData1 -> {//p3
        System.out.println("b:"+resolvedData1);
        return "b:"+resolvedData1;
    },rejectReason -> {
        System.err.println("c:"+rejectReason);
    }
).then(resolvedData2 -> {//p4
        System.out.println("d:"+resolvedData2);
        return "d:"+resolvedData2;
    },rejectReason -> {
        System.err.println("e:"+rejectReason);
    }
);
```
执行结果
```java
a:3
c:java.lang.RuntimeException: err
e:java.lang.RuntimeException: err
```
在上面的例子中，p1,p2,p3链式调用，p1执行后在p2处手动抛出异常,p2的then侦测到异常，p3,p4的正常逻辑被取消了
```java
new Promise.Builder().promiseHandler(executor -> 3).build().then(resolvedData->{
    System.out.println("a:"+resolvedData);
    return new Promise.Builder().promiseHandler(executor -> {
        executor.reject(new RuntimeException("err"));
        return resolvedData;
    }).build();
}).pCatch(e->{
    System.out.println("捕获到异常");
    return 3;
}).then(resolvedData1 -> {
        System.out.println("b:"+resolvedData1);
        return "b:"+resolvedData1;
    },rejectReason -> {
        System.err.println("c:"+rejectReason);
    }
).then(resolvedData2 -> {
        System.out.println("d:"+resolvedData2);
        return "d:"+resolvedData2;
    },rejectReason -> {
        System.err.println("e:"+rejectReason);
    }
);
```
打印结果
```
a:3
捕获到异常
b:3
d:b:3
```
pCatch捕获到异常后，返回一个修正值3，这个值会传递个下一个promise处理，继续完成链式调用。pCatch也可以直接返回一个promise，promise的状态决定是否继续后续链的执行(*如果pCatch返回的promise是rejected状态仍然会拒绝后续promise的执行直到遇到下一个pCatch*)

> pCatch可以在promise链的任何位置出现，出现的次数不受限制，如果没有异常出现，将忽略pCatch逻辑。listen和pFinally只能在链的末尾出现，无论异常是否发生，它都将被调用(类似于try...catch...finally)。

#### promise 组合
由于开发中无法预计线程什么时候执行结束，有时需要拿到线程执行结果在进行下一步操作就比较麻烦。如果是单个promise，可以简单地使用then方法阻塞当前线程，等待promise线程执行完毕。如果是多个promise并行执行，需要等待所有的promise都执行完毕才能执行下一步，可以使用all或者waitAll方法。
```java
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
Promise.all(p1,p2,p3).then(resolvedData -> {
    Object[] datas = (Object[])resolvedData;
    for(Object d:datas){
        System.out.println(d);
    }
    return null;
},e->e.printStackTrace());
```
上面创建了三个promise，Promise.all将三个promise组装成一个新promise p，新的promise p的状态将由p1-p3的状态决定，如果p1-p3全部正常结束，p的状态是fulfilled，其终值是一个数组，按传入顺序保存p1-p3的执行结果。依据promise规范，如果p1-p3任意一个异常结束或手动调用executor.reject()方法将pn状态转为rejected，p的状态会转为rejected，并尝试取消其余promise。具体可以参考[wiki all](https://github.com/zhanyingf15/promise/wiki#static-ipromise-allipromise-promises)。
> 1.0.1版本all还有一个重载方法all(ExecutorService threadPool,final IPromise ...promises),可以指定p的执行环境，不指定线程池默认新开一个线程。

在有些情况下，当p1-p3的其中一个发生异常时，并不希望p的状态立即转变为rejected并尝试取消其余promise的执行，而是希望其余promise继续执行，可以使用waitAll()方法。Promise.waitAll将多个promise组装成一个新promise p，不同于all，p1-p3的状态不会影响p的状态，如果p自身未发生异常(*waitAll内部使用了CountDownLatch处理多个线程，可能会有异常*)，p的状态一直是fulfilled，其终值是一个数组，数组值是pn的终值或据因。具体可参考[wiki-waitAll](https://github.com/zhanyingf15/promise/wiki#static-ipromise-waitallipromise-promises),使用方式如下：
```java
IPromise p1 = new Promise.Builder().promiseHandler(handler->2*3).build();
IPromise p2 = new Promise.Builder().promiseHandler(handler->{
    throw new RuntimeException("手动抛出异常");
}).build();
IPromise p = Promise.waitAll(p1,p2).then(resolvedData -> {
    Object[] datas = (Object[]) resolvedData;
    for(Object d:datas){
        if(d instanceof Throwable){
            ((Throwable)d).printStackTrace();
        }else{
            System.out.println(d);
        }
    }
    return datas;
});
```
输出结果
```
6
java.lang.RuntimeException: 手动抛出异常
```
p1为正常执行完毕，其终值为6，p2手动抛出异常，使用waitAll后，p的终值为一个数组，遍历数组需要判断值的类型。

> 类似于all，Promise.race方法将多个 Promise p1,...pn实例，包装成一个新的 Promise 实例 p，只要p1-pn有一个状态发生改变（无论是转变为正常状态还是异常状态），p的状态立即改变，并尝试取消其余promise的执行。第一个改变的promise的状态和终值作为p的状态和终值

#### Promise.resolve和Promise.pTry
这两个方法都是Promise的静态方法。Promise.resolve方法有多个重载，最重要的一个是`resolve(Object object,String methodName,List<Object> args)`，该方法是将object的指定方法以异步方式执行，该方法的执行结果作为Promise的终值，具体可参考[wiki Promise.resolve](https://github.com/zhanyingf15/promise/wiki#static-ipromise-resolveobject-objectstring-methodnamelist--args)。

pTry方法将object的指定方法以同步方式执行，该方法的执行结果作为Promise的终值，如果object为IPromise实例，将忽略methodName和args参数，异步执行该实例。   
该方法是以Promise统一处理同步和异步方法，不管object是同步操作还是异步操作，都可以使用then指定下一步流程,用pCatch方法捕获异常,避免开发中出现以下情况
```java
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