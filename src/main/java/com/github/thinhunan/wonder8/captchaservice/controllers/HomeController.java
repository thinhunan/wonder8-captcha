package com.github.thinhunan.wonder8.captchaservice.controllers;

import com.github.thinhunan.wonder8.captchaservice.CaptchaConfig;
import com.github.thinhunan.wonder8.captchaservice.modules.Captcha;
import com.github.thinhunan.wonder8.captchaservice.modules.CaptchaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.stream.Collectors;

@Deprecated
@RestController
@RequestMapping(value = "/api/v1")
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger("HomeController");
    
    private CaptchaRepository captchaRepository;

    //private final HttpServletRequest request;

    public HomeController(CaptchaRepository captchaRepository, CaptchaConfig captchaConfig) {
        this.captchaRepository = captchaRepository;
    }

    @GetMapping(value="register")
    public String register(String code){
        Captcha captcha = captchaRepository.linkCodeToRandomCaptcha(code);
        return captcha.getInfo().stream().map(i-> i.getText()).collect(Collectors.joining(","));
    }

    @GetMapping(value = "captcha", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] captcha(String code) throws IOException {
        Captcha c = captchaRepository.getLinkedCaptcha(code);
        if(c != null){
            return c.getImage();
        }
        return null;
    }

    @GetMapping(value="verify")
    public boolean verify(String code,int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4){
        return captchaRepository.verify(code,x1,y1,x2,y2,x3,y3,x4,y4);
    }

    @GetMapping(value="check")
    public boolean check(String code, String key){
        return captchaRepository.check(code,key);
    }

}
