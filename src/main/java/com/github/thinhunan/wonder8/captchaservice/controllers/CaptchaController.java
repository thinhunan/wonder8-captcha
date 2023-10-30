package com.github.thinhunan.wonder8.captchaservice.controllers;

import com.github.thinhunan.wonder8.captchaservice.CaptchaConfig;
import com.github.thinhunan.wonder8.captchaservice.modules.CaptchaRxRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.stream.Collectors;

@RequestMapping(value = "/api/v2")
@RestController
public class CaptchaController {

    private CaptchaRxRepository captchaRxRepository;
    private CaptchaConfig captchaConfig;

    public CaptchaController(CaptchaConfig captchaConfig, CaptchaRxRepository captchaRxRepository) {
        this.captchaConfig = captchaConfig;
        this.captchaRxRepository = captchaRxRepository;
    }

    @GetMapping(value="register")
    public Mono<String> register(String code){
        return captchaRxRepository.linkCodeToRandomCaptcha(code)
                .map(captcha -> captcha.getInfo().stream().map(i-> i.getText()).collect(Collectors.joining(",")));
    }
//
//    @GetMapping(value = "captcha", produces = MediaType.IMAGE_JPEG_VALUE)
//    public Mono<byte[]> captcha(String code) throws IOException {
//        return captchaRxRepository.getLinkedCaptcha(code).map(c->c.getImage());
//    }
//
     @GetMapping(value = "captcha", produces = MediaType.IMAGE_JPEG_VALUE)
     public Mono<ResponseEntity<byte[]>> captcha(String code) throws IOException {
         return captchaRxRepository.getLinkedCaptcha(code).map(c -> ResponseEntity.ok()
                 .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                 .header(HttpHeaders.PRAGMA, "no-cache")
                 .body(c.getImage()));
     }


    @GetMapping(value="verify")
    public Mono<Boolean> verify(String code,int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4){
        return captchaRxRepository.verify(code,x1,y1,x2,y2,x3,y3,x4,y4);
    }

    @GetMapping(value="check")
    public Mono<Boolean> check(String code, String key){
        return captchaRxRepository.check(code,key);
    }


    // region for test ,need delete

    @GetMapping(value="hack")
    public Mono<String> getVerifyUrl(String code, String key) {
        if (!this.captchaConfig.checkKey.equals(key)) {
            throw new IllegalArgumentException("Illegal check key.");
        }
        return captchaRxRepository.getLinkedCaptcha(code)
                .map(c -> {
                    if (c != null) {
                        return verifyUrl(code,
                                c.getInfo().get(0).getX(),
                                c.getInfo().get(0).getY(),
                                c.getInfo().get(1).getX(),
                                c.getInfo().get(1).getY(),
                                c.getInfo().get(2).getX(),
                                c.getInfo().get(2).getY(),
                                c.getInfo().get(3).getX(),
                                c.getInfo().get(3).getY());
                    }
                    return "";
                });
    }

    String verifyUrl(String code,int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4){
        return String.format("http://localhost:%d/api/v2/verify?code=%s&x1=%d&y1=%d&x2=%d&y2=%d&x3=%d&y3=%d&x4=%d&y4=%d",
                8080,code,x1,y1,x2,y2,x3,y3,x4,y4);
    }

    // endregion
}
