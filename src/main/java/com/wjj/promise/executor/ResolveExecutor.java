package com.wjj.promise.executor;

/**
 * @author wangjiajun
 */
public interface ResolveExecutor{
    /**
     *将Promise对象的状态从“未完成”变为“成功”（即从pending变为fulfilled）
     * @param args
     */
    public void resolve(Object args);
}
