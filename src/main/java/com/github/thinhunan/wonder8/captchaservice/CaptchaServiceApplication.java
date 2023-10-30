package com.github.thinhunan.wonder8.captchaservice;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CaptchaServiceApplication {

    public static void main(String[] args) {
        //SpringApplication.run(CaptchaServiceApplication.class, args);
        new SpringApplicationBuilder(CaptchaServiceApplication.class).web(WebApplicationType.REACTIVE).run(args);
    }

}
