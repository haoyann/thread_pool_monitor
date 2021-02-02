package com.threadpool.monitor.agent;

import com.thread.pool.manager.ThreadPoolMonitorManager;
import net.bytebuddy.asm.Advice;

import java.util.concurrent.atomic.AtomicLong;


/**
 * @Date 2021/1/30 下午2:48
 * @Created by haoyann
 */
public class ThreadPoolRejectInterceptor {
    
    
    @Advice.OnMethodEnter
    public static void intercept(@Advice.This Object obj) throws Throwable {
        
        AtomicLong rejectCount = ThreadPoolMonitorManager.getRejectCount(obj.hashCode());
        if (rejectCount != null) {
            rejectCount.incrementAndGet();
        }
        
    }
    
    
}
