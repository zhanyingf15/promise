package com.wjj.promise.then;

/**
 * @author wangjiajun
 */
@FunctionalInterface
public interface OnCompleteListener {
    /**
     * 当Promise执行结束时的回调(无论是fulfilled还是rejected)
     * @param resolvedData fulfilled状态时的终值
     * @param e rejected状态时的异常信息
     */
    public void listen(Object resolvedData,Throwable e);
}
