package com.github.thinhunan.wonder8.captchaservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CaptchaConfig {
    @Value("${captcha.prepareSize}")
    public int prepareSize;
    @Value("${captcha.renewSize}")
    public int renewSize;
    @Value("${captcha.width}")
    public int width;
    @Value("${captcha.height}")
    public int height;
    @Value("${captcha.image.quality}")
    public float image_quality;
    @Value("${captcha.verify.distance}")
    public int verify_distance;
    @Value("${captcha.checkKey}")
    public String checkKey;
    @Value("${captcha.backgroundImage.dir}")
    public String backgroundImage_dir;
    @Value("${captcha.log.enabled}")
    public boolean logEnabled;
}
