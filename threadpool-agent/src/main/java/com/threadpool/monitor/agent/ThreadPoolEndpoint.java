package com.threadpool.monitor.agent;

import com.thread.pool.manager.ThreadPoolMonitorManager;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.stereotype.Component;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @Date 2021/2/1 下午10:16
 * @Created by haoyann
 */
@Component
@WebEndpoint(id = "threadPool")
public class ThreadPoolEndpoint {
    
    
    private Map<Integer, String> threadNameMap = new HashMap<>();
    
    @ReadOperation
    public Map<String, Object> list() {
        
        Collection<ThreadPoolExecutor> values = ThreadPoolMonitorManager.getThreadPoolExecutorCollection();
        
        List<ThreadPoolMonitorModel> threadPoolMonitorModels = values.stream().map(tp -> {
            ThreadPoolMonitorModel threadPoolMonitorModel = new ThreadPoolMonitorModel();
            threadPoolMonitorModel.setHashCode(tp.hashCode());
            threadPoolMonitorModel.setName(getThreadName(tp));
            threadPoolMonitorModel.setCorePoolSize(tp.getCorePoolSize());
            threadPoolMonitorModel.setMaximumPoolSize(tp.getMaximumPoolSize());
            threadPoolMonitorModel.setPoolSize(tp.getPoolSize());
            threadPoolMonitorModel.setActiveThreadCount(tp.getActiveCount());
            threadPoolMonitorModel.setQueueType(tp.getQueue().getClass().getSimpleName());
            threadPoolMonitorModel.setQueueCapacityNum(getQueueCapacity(tp.getQueue()));
            threadPoolMonitorModel
                    .setRejectedExecutionHandler(tp.getRejectedExecutionHandler().getClass().getSimpleName());
            threadPoolMonitorModel.setRejectedCount(ThreadPoolMonitorManager.getRejectCount(tp.hashCode()).intValue());
            
            return threadPoolMonitorModel;
        }).collect(Collectors.toList());
        
        Map<String, Object> map = new HashMap<>();
        map.put("count", values.size());
        map.put("data", threadPoolMonitorModels);
        
        return map;
        
    }
    
    
    private String getThreadName(ThreadPoolExecutor threadPoolExecutor) {
        
        String threadName = threadNameMap.get(threadPoolExecutor.hashCode());
        if (threadName != null) {
            return threadName;
        }
        try {
            Field workersField = getWorkersField(threadPoolExecutor);
            workersField.setAccessible(true);
            HashSet workers = (HashSet) workersField.get(threadPoolExecutor);
            Iterator iterator = workers.iterator();
            
            if (iterator.hasNext()) {
                Object next = iterator.next();
                Field threadField = next.getClass().getDeclaredField("thread");
                threadField.setAccessible(true);
                Thread thread = (Thread) threadField.get(next);
                threadName = dealThreadName(thread.getName());
                threadNameMap.put(threadPoolExecutor.hashCode(), threadName);
                return threadName;
            }
        } catch (Exception e) {
            // ignore
        }
        
        return "";
    }
    
    private Field getWorkersField(ThreadPoolExecutor threadPoolExecutor) throws NoSuchFieldException {
        // must three times
        Class aClass = threadPoolExecutor.getClass();
        for (int i = 0; i < 3; i++) {
            try {
                return aClass.getDeclaredField("workers");
            } catch (Exception e) {
                aClass = aClass.getSuperclass();
                if (aClass == null) {
                    throw e;
                }
            }
            
        }
        throw new NoSuchFieldException();
    }
    
    private String dealThreadName(String name) {
        
        String[] split = name.split("-");
        
        if (split.length == 1) {
            return name;
        }
        String suffix = split[split.length - 1];
        
        try {
            Integer.parseInt(suffix);
            
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < split.length - 1; i++) {
                ret.append(split[i]);
                if (i < split.length - 2) {
                    ret.append("-");
                }
            }
            return ret.toString();
        } catch (Exception e) {
            // ignore
        }
        
        return name;
    }
    
    private Integer getQueueCapacity(BlockingQueue queue) {
        try {
            if (queue instanceof LinkedBlockingQueue) {
                Field capacityField = queue.getClass().getDeclaredField("capacity");
                capacityField.setAccessible(true);
                Object capacity = capacityField.get(queue);
                return (Integer) capacity;
            }
            
            if (queue instanceof ArrayBlockingQueue) {
                Field capacityField = queue.getClass().getDeclaredField("items");
                capacityField.setAccessible(true);
                Object capacity = capacityField.get(queue);
                Object[] items = (Object[]) capacity;
                return items.length;
            }
            
        } catch (Exception e) {
            // ignore
        }
        return -1;
        
    }
    
    
    @WriteOperation
    public String modify(Integer hashCode, Integer coreSize, Integer maxSize) {
        // 参数校验 判断
        
        if (coreSize == null || coreSize < 0) {
            return "参数不合法";
        }
        
        if (maxSize == null || maxSize < 0) {
            return "参数不合法";
        }
        
        if (maxSize < coreSize) {
            return "参数不合法";
        }
        ThreadPoolExecutor threadPoolExecutor = ThreadPoolMonitorManager.getThreadPoolExecutor(hashCode);
        
        if (threadPoolExecutor == null) {
            return "未找到需要修改的线程";
        }
        // 修改
        threadPoolExecutor.setCorePoolSize(coreSize);
        threadPoolExecutor.setMaximumPoolSize(maxSize);
        return "success";
    }
    
    
    public static class ThreadPoolMonitorModel {
        
        // hashCode
        private Integer hashCode;
        
        // 名称
        private String name;
        
        // 线程池状态
        private String status;
        
        // 核心线程数量
        private int corePoolSize;
        
        // 最大线程数
        private int maximumPoolSize;
        
        // 当前works线程
        private Integer poolSize;
        
        // 当前活跃线程线程
        private Integer activeThreadCount;
        
        // 队列类型
        private String queueType;
        
        // 队列初始化长度
        private Integer queueCapacityNum;
        
        // 队列空余长度
        private Integer remainingCapacity;
        
        // 总任务数量
        private Long totalTaskCount;
        
        // 完成任务数量
        private Long completedTaskCount;
        
        // 当前队列任务数量
        private Long queueTaskCount;
        
        // 拒绝策略
        private String rejectedExecutionHandler;
        
        //拒绝次数
        private Integer rejectedCount;
        
        public Integer getHashCode() {
            return hashCode;
        }
        
        public void setHashCode(Integer hashCode) {
            this.hashCode = hashCode;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public int getCorePoolSize() {
            return corePoolSize;
        }
        
        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }
        
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }
        
        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }
        
        public Integer getPoolSize() {
            return poolSize;
        }
        
        public void setPoolSize(Integer poolSize) {
            this.poolSize = poolSize;
        }
        
        public Integer getActiveThreadCount() {
            return activeThreadCount;
        }
        
        public void setActiveThreadCount(Integer activeThreadCount) {
            this.activeThreadCount = activeThreadCount;
        }
        
        public String getQueueType() {
            return queueType;
        }
        
        public void setQueueType(String queueType) {
            this.queueType = queueType;
        }
        
        public Integer getQueueCapacityNum() {
            return queueCapacityNum;
        }
        
        public void setQueueCapacityNum(Integer queueCapacityNum) {
            this.queueCapacityNum = queueCapacityNum;
        }
        
        public Integer getRemainingCapacity() {
            return remainingCapacity;
        }
        
        public void setRemainingCapacity(Integer remainingCapacity) {
            this.remainingCapacity = remainingCapacity;
        }
        
        public Long getTotalTaskCount() {
            return totalTaskCount;
        }
        
        public void setTotalTaskCount(Long totalTaskCount) {
            this.totalTaskCount = totalTaskCount;
        }
        
        public Long getCompletedTaskCount() {
            return completedTaskCount;
        }
        
        public void setCompletedTaskCount(Long completedTaskCount) {
            this.completedTaskCount = completedTaskCount;
        }
        
        public Long getQueueTaskCount() {
            return queueTaskCount;
        }
        
        public void setQueueTaskCount(Long queueTaskCount) {
            this.queueTaskCount = queueTaskCount;
        }
        
        public String getRejectedExecutionHandler() {
            return rejectedExecutionHandler;
        }
        
        public void setRejectedExecutionHandler(String rejectedExecutionHandler) {
            this.rejectedExecutionHandler = rejectedExecutionHandler;
        }
        
        public Integer getRejectedCount() {
            return rejectedCount;
        }
        
        public void setRejectedCount(Integer rejectedCount) {
            this.rejectedCount = rejectedCount;
        }
    }
}
