package com.thread.pool.example;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Date 2021/2/2 上午11:20
 * @Created by haoyann
 */
@RequestMapping("/thread/pool/test")
@RestController
@SpringBootApplication
public class ExampleBootstrap {
    
    ExecutorService executorService = new ThreadPoolExecutor(1, 5, 1L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(5),new NamedThreadFactory("thread_pool_test"));
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(ExampleBootstrap.class).run(args);
    }
    
    @PostMapping("/{num}")
    public void execute(@PathVariable("num") Integer num) {
        
        for (int i = 0; i < num; i++) {
            executorService.execute(() -> {
                System.out.println("execute task -----");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            });
        }
    }
}
