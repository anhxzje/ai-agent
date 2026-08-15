package prj.anhzxje.aiagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AgentConfig {

    @Bean(name = "agentThreadPoolExecutor")
    public Executor agentThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Chỉ cho phép chạy tối đa 5 agent đồng thời để tránh quá tải
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("AgentExecutor-");
        executor.initialize();
        return executor;
    }
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
