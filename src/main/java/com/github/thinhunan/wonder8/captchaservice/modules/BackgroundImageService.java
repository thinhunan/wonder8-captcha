package com.github.thinhunan.wonder8.captchaservice.modules;

import com.github.thinhunan.wonder8.captchaservice.CaptchaConfig;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

@Component
public class BackgroundImageService  {

    private static final Logger logger = LoggerFactory.getLogger("BackgroundImageService");

    private CaptchaConfig captchaConfig;
    static Random rnd = new Random();
    Hashtable<String,BufferedImage> images;
    String imageDir;

    public BackgroundImageService(CaptchaConfig captchaConfig) {
        this.captchaConfig = captchaConfig;
        String backgroundImageDIR = captchaConfig.backgroundImage_dir;
        if(backgroundImageDIR.startsWith("classpath:")){
            try {
                backgroundImageDIR = ResourceUtils.getFile(backgroundImageDIR).getAbsolutePath();
            } catch (FileNotFoundException e) {
                logger.error("wrong format of classpath in background_image_dir",e);
            }
        }
        if(!backgroundImageDIR.endsWith("/")){
            this.imageDir = backgroundImageDIR + "/";
        }
        else{
            this.imageDir = backgroundImageDIR;
        }

        FilenameFilter fileFilter = (dir, name) -> {
            name = name.toLowerCase();
            return  name.endsWith(".jpeg") || name.endsWith(".jpg")
                    || name.endsWith(".png") || name.endsWith(".bmp");
        };
        File dir = new File(this.imageDir);
        File[] files = dir.listFiles(fileFilter);
        if(files != null) {
            this.images = new Hashtable<>(files.length);

            for (File file : files) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    this.images.put(file.getName(), image);
                }
                catch (IOException e) {
                    logger.error("load background image:",e);
                }
            }
        }
        if(this.images == null || this.images.size()<1){
            this.images = new Hashtable<>(1);
            this.images.put("empty.jpeg",emptyBufferedImage());
            logger.info(this.imageDir+" has no picture!");
        }

        monitorImageFiles();
    }

    void monitorImageFiles(){
        long interval = 1000;
        IOFileFilter filter = FileFilterUtils.or(
                FileFilterUtils.suffixFileFilter(".jpg", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".jpeg", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".png", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".bmp", IOCase.INSENSITIVE));
        FileAlterationObserver imageObserver = new FileAlterationObserver(this.imageDir,filter);
        imageObserver.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileDelete(File file) {
                images.remove(file.getName());
                if(images.size()<1){
                    images.put("empty.jpeg",emptyBufferedImage());
                }
            }

            @Override
            public void onFileChange(File file) {
                images.remove(file.getName());
                try {
                    BufferedImage image = ImageIO.read(file);
                    images.put(file.getName(),image);
                    images.remove("empty.jpeg");
                } catch (IOException e) {
                    logger.error("while background image has changed: ",e);
                }
            }

            @Override
            public void onFileCreate(File file) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    images.put(file.getName(),image);
                    images.remove("empty.jpeg");
                } catch (IOException e) {
                    logger.error("while background image has added: ",e);
                }
            }
        });
        FileAlterationMonitor monitor = new FileAlterationMonitor(interval);
        monitor.addObserver(imageObserver);
        try {
            monitor.start();
        } catch (Exception e) {
            logger.error("file change monitor starting: ",e);
        }
    }

    private BufferedImage emptyBufferedImage(){
        BufferedImage empty = new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = empty.createGraphics();
        g.setColor(Color.white);
        g.drawRect(0,0,10,10);
        g.dispose();
        return empty;
    }

    public BufferedImage getRandomBackgroundImage(){
        int index = rnd.nextInt(this.images.size());
        int i = 0;
        for (String fileName : this.images.keySet()) {
            if(i == index){
                return this.images.get(fileName);
            }
            i++;
        }
        return null;
    }
}
