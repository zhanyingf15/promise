package com.wjj.promise;

/**
 * @author wangjiajun
 */
@FunctionalInterface
public interface PromiseHandler {
    /**
     * 具体的业务逻辑
     * @param executor
     * @return 执行结果
     * @throws Exception
     */
    public Object run(PromiseExecutor executor)throws Exception;
}
