package com.wjj.promise.executor;

/**
 * @author wangjiajun
 */
public interface RejectExecutor {
    /**
     * 将Promise对象的状态从“未完成”变为“失败”（即从pending变为fulfilled）
     * @param args 异常对象
     */
    public void reject(Throwable args);
}
