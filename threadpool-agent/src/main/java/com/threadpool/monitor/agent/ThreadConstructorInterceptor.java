package com.threadpool.monitor.agent;

import com.thread.pool.manager.ThreadPoolMonitorManager;
import net.bytebuddy.asm.Advice;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * @Date 2021/1/30 下午2:44
 * @Created by haoyann
 */
public class ThreadConstructorInterceptor {
    
    @Advice.OnMethodExit
    public static void intercept(@Advice.This Object obj, @Advice.AllArguments Object[] allArguments) {
        if (obj instanceof ThreadPoolExecutor) {
            ThreadPoolMonitorManager.putThreadPoolExecutor(obj.hashCode(), (ThreadPoolExecutor) obj);
            
        }
    }
}
