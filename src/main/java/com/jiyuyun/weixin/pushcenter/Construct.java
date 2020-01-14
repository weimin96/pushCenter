package com.jiyuyun.weixin.pushcenter;

import com.jiyuyun.weixin.pushcenter.thread.MessageListenerThread;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * 启动监听
 *
 * @author pwm
 * @date 2019/12/23
 */
@Component
public class Construct implements CommandLineRunner {

    private ThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern("listener-thread-%d").daemon(true).build();

    private ExecutorService executorService = new ThreadPoolExecutor(2,2,
            0L,TimeUnit.SECONDS,new LinkedBlockingDeque<>(1),threadFactory);

    @Autowired
    private MessageListenerThread messageListenerThread;

//    @Autowired
//    private MessageBackListenerThread messageBackListenerThread;

    @Override
    public void run(String... args) {
        executorService.execute(messageListenerThread);
//        executorService.execute(messageBackListenerThread);
    }
}
