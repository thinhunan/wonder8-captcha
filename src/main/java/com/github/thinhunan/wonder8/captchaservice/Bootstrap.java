package com.github.thinhunan.wonder8.captchaservice;

import com.github.thinhunan.wonder8.captchaservice.modules.CaptchaRxRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Bootstrap implements CommandLineRunner {
    private CaptchaConfig captchaConfig;
    //private CaptchaRepository captchaRepository;
    private CaptchaRxRepository captchaRxRepository;

    public Bootstrap(CaptchaConfig captchaConfig, CaptchaRxRepository captchaRepository) {
        this.captchaConfig = captchaConfig;
        this.captchaRxRepository = captchaRepository;
    }


    @Override
    public void run(String... args) {
        //captchaRepository.prepareCaptcha(captchaConfig.prepareSize);
        captchaRxRepository.prepareCaptcha(captchaConfig.prepareSize);
    }
}
