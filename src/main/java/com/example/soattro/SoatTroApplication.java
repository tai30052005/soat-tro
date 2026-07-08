package com.example.soattro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của ứng dụng Soát Trọ.
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan:
 * Spring tự quét các class trong package com.example.soattro và cấu hình mọi thứ.
 */
@SpringBootApplication
public class SoatTroApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoatTroApplication.class, args);
    }
}
