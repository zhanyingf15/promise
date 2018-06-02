package com.wjj.promise.then;

/**
 * @author wangjiajun
 */
@FunctionalInterface
public interface OnCatchedExecutor {
    /**
     * 当发生异常时的回调,最终返回一个Promise或普通对象，如果是一个普通对象，这个对象将作为下一个Promise的终值
     * @param catchReason
     * @return
     * @throws Exception
     */
    public Object onCatched(Throwable catchReason)throws Exception;
}
