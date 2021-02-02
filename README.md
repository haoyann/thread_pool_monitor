# thread_pool_monitor
通过agnet监控和修改线程池

## 思路
来源思路来自https://tech.meituan.com/2020/04/02/java-pooling-pratice-in-meituan.html
项目主要通过agent采集线程池数据，和通过spring的Endpoint暴露接口查询和修改线程池.

## 使用
```
// 打包
mvn clean package

// 使用agent启动
-javaagent:path/agent/threadpool-agent.jar

// 暴露接口
management.endpoints.web.exposure.include=threadPool
```

查询 : GET http://ip:port/actuator/threadPool
```
{
    "data": [
        {
            "hashCode": 2068897588,
            "name": "RMI Scheduler(0)",
            "status": null,
            "corePoolSize": 1,
            "maximumPoolSize": 2147483647,
            "poolSize": 1,
            "activeThreadCount": 0,
            "queueType": "DelayedWorkQueue",
            "queueCapacityNum": -1,
            "remainingCapacity": null,
            "totalTaskCount": null,
            "completedTaskCount": null,
            "queueTaskCount": null,
            "rejectedExecutionHandler": "AbortPolicy",
            "rejectedCount": 0
        }
     ]

```
修改 ：POST http://ip:port/actuator/threadPool

```
{
    "hashCode": 1487424018,
    "coreSize": 1,
    "maxSize": 100
}
```