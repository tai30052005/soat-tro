package com.example.soattro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Bật xử lý bất đồng bộ (chặng 6). Soát 1 hợp đồng gọi Gemini ~20–30s;
 * nếu chạy đồng bộ thì request HTTP bị treo cả nửa phút -> dễ timeout.
 *
 * Giải pháp: POST trả về ngay (status PROCESSING), pipeline chạy trên pool riêng,
 * frontend poll GET để lấy kết quả khi xong.
 */
// Tắt trong profile "test": không @EnableAsync -> @Async bị bỏ qua, pipeline chạy
// đồng bộ ngay trên luồng gọi để test tất định (không phải chờ/poll luồng nền).
@Configuration
@EnableAsync
@Profile("!test")
public class AsyncConfig {

    /** Pool riêng cho việc soát hợp đồng — đặt tên để @Async trỏ đúng executor này. */
    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("analysis-");
        // Quá tải thì chạy ngay trên luồng gọi (không vứt bỏ lượt soát của người dùng).
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Khi tắt app, chờ các lượt đang chạy hoàn tất để không mất dữ liệu dở.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
