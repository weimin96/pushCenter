package com.jiyuyun.weixin.pushcenter.thread;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * @author pwm
 * @date 2019/12/26
 */
@Component
public class TaskExecutorService {

    private ThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern("thread-%d").daemon(true).build();

    private ExecutorService executorService = new ThreadPoolExecutor(10, Integer.MAX_VALUE,
            0L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(1), threadFactory);

    @Bean(name = "taskExecutor")
    public ExecutorService taskExecutorService() {
        return executorService;
    }
}
