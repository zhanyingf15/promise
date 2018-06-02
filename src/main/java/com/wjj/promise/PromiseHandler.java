package com.wjj.promise;

/**
 * @author wangjiajun
 * @date 2018/5/28 13:53
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
