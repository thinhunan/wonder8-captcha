package com.github.thinhunan.wonder8.captchaservice.modules;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGEncodeParam;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;
//import sun.font.FontDesignMetrics;

import com.github.thinhunan.wonder8.captchaservice.CaptchaConfig;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import redis.clients.jedis.Jedis;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
public final class CaptchaUtils {

    private static final Logger logger = LoggerFactory.getLogger("CaptchaUtils");

    private static final Random rnd = new Random();

    private CaptchaConfig captchaConfig;
    private BackgroundImageService backgroundImageService;

    public CaptchaUtils(CaptchaConfig captchaConfig, BackgroundImageService backgroundImageService) {
        this.captchaConfig = captchaConfig;
        this.backgroundImageService = backgroundImageService;
    }

// region generate

    /**
     * 生成captcha图像与信息对象
     * @return 已经过压缩的图片
     */
    public Captcha generateCaptchaImage(){
        int width = captchaConfig.width, height = captchaConfig.height;
        Graphics2D g2d = null;
        ImageWriter writer = null;
        ImageOutputStream imageOutput = null;

        try {
            BufferedImage background = this.backgroundImageService.getRandomBackgroundImage();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            g2d = image.createGraphics();
            g2d.drawImage(background.getScaledInstance(width, height, java.awt.Image.SCALE_DEFAULT), 0, 0, null);
            List<CaptchaText> captchaTexts = drawRandomChinese(g2d, 4, width, height);

            // 使用jpeg 0.4 quality压缩,压缩后相当于png的1/20大小
            ByteArrayOutputStream output = new ByteArrayOutputStream();
//            JPEGEncodeParam param = JPEGCodec.getDefaultJPEGEncodeParam(image);
//            param.setQuality(this.quality,false);
//            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(output,param);
//            encoder.encode(image);
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            writer = writers.next();
            imageOutput = ImageIO.createImageOutputStream(output);
            writer.setOutput(imageOutput);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(this.captchaConfig.image_quality);
            writer.write(null, new IIOImage(image, null, null), param);
            Captcha c = new Captcha();
            c.setInfo(captchaTexts);
            c.setImage(output.toByteArray());
            return c;
        }
        catch (IOException e){
            logger.error("Generating captcha image.",e);
        }
        finally {
            if(g2d != null) {
                g2d.dispose();
            }
            if(imageOutput != null) {
                try {
                    imageOutput.close();
                } catch (IOException e) {
                    logger.error("Generating captcha image.",e);
                }
            }
            if(writer != null) {
                writer.dispose();
            }
        }
        return null;
    }

    static String getRandomChinese(int count){
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < count ; i++){
            int highCode;
            int lowCode;

            highCode = (176 + Math.abs(rnd.nextInt(39))); //B0 + 0~39(16~55) 一级汉字所占区
            lowCode = (161 + Math.abs(rnd.nextInt(93))); //A1 + 0~93 每区有94个汉字

            byte[] b = new byte[2];
            b[0] = (Integer.valueOf(highCode)).byteValue();
            b[1] = (Integer.valueOf(lowCode)).byteValue();

            try {
                sb.append(new String(b, "GBK"));
            } catch (UnsupportedEncodingException e) {
                logger.error("Generating captcha text.",e);
            }
        }
        return sb.toString();
    }

    static Color getRandomColor(){
        return new Color(rnd.nextInt(255),
                rnd.nextInt(255),
                rnd.nextInt(255));
    }

    static List<CaptchaText> drawRandomChinese(Graphics2D g, int count, int width, int height) {
        //随机常用汉字
        String text = getRandomChinese(count);
        List<CaptchaText> results = new ArrayList<>(count);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int i = 0; i < count; i++) {
            int fontSize = height/7+ rnd.nextInt(height/21);
            // 这里给四周预留了1/10的空间，使得后期可以再增加一层随机性：
            //  验证过程中，生成一个随机小于1/5图片大小的偏移值
            //  前端展示图片时应用这个偏移值，所以用户点击的坐标在后端验证时，需先加上偏移值
            int x = width/10 + fontSize/2 + width/5*i + rnd.nextInt(width/5-fontSize);
            int y = height/10 + fontSize/2 + rnd.nextInt(height/5*4-fontSize);

            String ch = text.substring(i,i+1);
            Font song = new Font("宋体", Font.PLAIN, fontSize);
            g.setFont(song);

            // 设置字体旋转角度
            int degree = rnd.nextInt() % 30;
            g.rotate(degree * Math.PI / 180, x, y);

            //blend to background, but this method causes 'black text'
            //g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_ATOP,0.8f));
            g.setColor(Color.white);
            g.drawString(ch,x-1,y-1);

            g.setColor(Color.black);
            g.drawString(ch,x+1,y+1);
            //g.setComposite(AlphaComposite.Clear);
            g.setColor(getRandomColor());
            g.drawString(ch, x, y);

            g.rotate(-degree * Math.PI / 180, x, y);

            //center after rotation, notice screen's reversed coordinate
            //FontDesignMetrics metrics = FontDesignMetrics.getMetrics(song); //stupid sun company
            //double _x = metrics.stringWidth(ch) /2;
            //double _y = metrics.getHeight() /-2;
            FontRenderContext ctx = g.getFontRenderContext();
            Rectangle rectangle = song.getStringBounds(ch,ctx).getBounds();
            double _x = rectangle.getWidth()/2.4;
            double _y = rectangle.getHeight()/-2.4;

            double angle = Math.atan2(_y,_x);//-45 * Math.PI /180;//如果每个字长宽固定不用每次算arc tangent
            double length = Math.sqrt(Math.pow(_x,2) + Math.pow(_y,2));
            angle += degree * Math.PI / 180;
            _x = length * Math.cos(angle);
            _y = length * Math.sin(angle);
            int tx = x + (int)_x;
            int ty = y + (int)_y;

            results.add(new CaptchaText(ch, tx, ty,fontSize));

            // for debug
//            g.setStroke(new BasicStroke(3f));
//            g.setColor(Color.red);
//            g.drawOval(tx,ty,10,10);
        }
        return results.stream().sorted(Comparator.comparing(CaptchaText::getText)).collect(Collectors.toList());
    }

    // endregion

    //#region id utils
    public double getCaptchaId(Jedis jedis){
        return getEpochSeconds(LocalDateTime.now()) + jedis.incr(Constants.ID_KEY) % 1000;
    }

    public Mono<Long> getCaptchaIdRx(RedisReactiveCommands<byte[], byte[]> cmd){
        return cmd.incr(Constants.ID_KEY).map(n-> getEpochSeconds(LocalDateTime.now()) + n % 1000);
    }

    public long getEpochSeconds(LocalDateTime dateTime){
        return dateTime.toEpochSecond(ZoneOffset.ofHours(8)) * 1000;
    }

    //#endregion
}
