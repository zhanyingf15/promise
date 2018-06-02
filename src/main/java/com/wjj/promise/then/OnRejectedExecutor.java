package com.wjj.promise.then;

/**
 * @author wangjiajun
 * @date 2018/5/28 11:13
 */
@FunctionalInterface
public interface OnRejectedExecutor {
    /**
     * 当Promise转变为rejected状态时的回调
     * @param rejectReason
     * @throws Exception 异常信息
     */
    public void onRejected(Throwable rejectReason)throws Exception;
}
