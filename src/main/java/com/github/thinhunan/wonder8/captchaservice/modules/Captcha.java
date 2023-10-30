package com.github.thinhunan.wonder8.captchaservice.modules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Captcha {
    public double getId() {
        return id;
    }

    public void setId(double id) {
        this.id = id;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public List<CaptchaText> getInfo() {
        return info;
    }

    public void setInfo(List<CaptchaText> info) {
        this.info = info;
    }

    public byte[] toRedisInfoValue(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.info.size(); i++) {
            if(i > 0){
                sb.append('|');
            }
            CaptchaText ct = this.info.get(i);
            sb.append(String.format("%s,%d,%d,%d",ct.text,ct.x,ct.y,ct.fontSize));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static List<CaptchaText> fromRedisInfo(byte[] value){
        String s = new String(value,StandardCharsets.UTF_8);
        String[] captchas = s.split("\\|");
        List<CaptchaText> results = new ArrayList<>(captchas.length);
        for (String c : captchas) {
            String[] parts = c.split(",");
            results.add(new CaptchaText(
                    parts[0],
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            ));
        }
        return results;
    }

    public static Captcha fromRedis(double id, byte[] info,byte[] image){
        Captcha c = new Captcha();
        c.setId(id);
        c.setImage(image);
        c.setInfo(Captcha.fromRedisInfo(info));
        return c;
    }

    public static Captcha fromRedis(double id, byte[] info){
        Captcha c = new Captcha();
        c.setId(id);
        c.setInfo(Captcha.fromRedisInfo(info));
        return c;
    }

    double id;
    List<CaptchaText> info;
    byte[] image;
}
