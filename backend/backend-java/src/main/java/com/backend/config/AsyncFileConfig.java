package com.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAsync
@Slf4j
public class AsyncFileConfig {

    @Bean(name = "fileUploadExecutor")
    public Executor fileUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("FileUpload-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setTaskDecorator(runnable -> {
            return () -> {
                String threadName = Thread.currentThread().getName();
                try {
                    log.debug("File upload task starting on thread: {}", threadName);
                    long startTime = System.currentTimeMillis();
                    runnable.run();
                    long endTime = System.currentTimeMillis();
                    log.debug("File upload task completed on thread: {} in {} ms", 
                            threadName, (endTime - startTime));
                } catch (Exception e) {
                    log.error("File upload task failed on thread {}: {}", threadName, e.getMessage());
                    throw e;
                }
            };
        });
        
        executor.initialize();
        log.info("File upload thread pool initialized with {} core threads, {} max threads",
                executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }
    
    @Bean(name = "batchFileUploadExecutor")
    public Executor batchFileUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("BatchUpload-");
        executor.setKeepAliveSeconds(300);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("Batch file upload thread pool initialized");
        return executor;
    }
}
