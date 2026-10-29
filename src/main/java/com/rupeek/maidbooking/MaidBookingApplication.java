package com.rupeek.maidbooking;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MaidBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaidBookingApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Asia/Kolkata"));
    }
}
