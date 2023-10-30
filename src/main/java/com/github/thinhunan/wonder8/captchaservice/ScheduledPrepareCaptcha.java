package com.github.thinhunan.wonder8.captchaservice;

import com.github.thinhunan.wonder8.captchaservice.modules.CaptchaRxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledPrepareCaptcha {

    private CaptchaConfig captchaConfig;
    //private CaptchaRepository captchaRepository;
    private CaptchaRxRepository captchaRxRepository;

    public ScheduledPrepareCaptcha( CaptchaConfig captchaConfig,
                                   CaptchaRxRepository captchaRepository) {
        this.captchaConfig = captchaConfig;
        this.captchaRxRepository = captchaRepository;
    }

    /**
     * 定时产生新的captcha，并将半小时前的放入old集合中，同时将old集合中超过1小时前生成的丢弃
     **/
    @Scheduled(fixedRate = 1000)
    public void prepareCaptcha(){
        int renew = captchaConfig.renewSize;
        captchaRxRepository.refreshCaptcha(renew);
    }
}
