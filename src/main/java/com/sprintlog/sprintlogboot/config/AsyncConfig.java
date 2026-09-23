package com.sprintlog.sprintlogboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // 알림 전용 풀
    @Bean("notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);          // 평소 대기 인원
        executor.setMaxPoolSize(4);           // 바빠지면 여기까지
        executor.setQueueCapacity(4);         // 줄은 4칸 — 일부러 작게 잡아 포화를 눈으로 본다
        executor.setKeepAliveSeconds(60);     // core 를 넘겨 뽑은 일꾼이 놀면 60초 뒤 정리
        executor.setAllowCoreThreadTimeOut(false);   // core 2명은 항상 대기시킨다
        executor.setThreadNamePrefix("noti-");       // 로그에서 바로 알아보려고
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("dashboardExecutor")
    public ThreadPoolTaskExecutor dashboardExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);          // 평소 대기 인원
        executor.setMaxPoolSize(2);           // 바빠지면 여기까지
        executor.setQueueCapacity(50);         // 줄은 4칸 — 일부러 작게 잡아 포화를 눈으로 본다
        executor.setThreadNamePrefix("dash-");       // 로그에서 바로 알아보려고
        executor.initialize();
        return executor;
    }


}















