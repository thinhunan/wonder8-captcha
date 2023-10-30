package com.github.thinhunan.wonder8.captchaservice.modules;

import com.github.thinhunan.wonder8.captchaservice.CaptchaConfig;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

import java.time.LocalDateTime;
import java.util.List;

@Deprecated
@Repository
public class CaptchaRepository {

    private static final Logger logger = LoggerFactory.getLogger("CaptchaRepository");

    private RedisClient redisClient;
    private CaptchaConfig captchaConfig;
    private CaptchaUtils captchaUtils;

    public CaptchaRepository(RedisClient redisClient, CaptchaConfig captchaConfig, CaptchaUtils captchaUtils) {
        this.redisClient = redisClient;
        this.captchaConfig = captchaConfig;
        this.captchaUtils = captchaUtils;
    }

    // region get captcha
    public Captcha getCaptcha(double captchaId){
        Captcha captcha = null;
        try(Jedis jedis = redisClient.getClient()){
            List<byte[]> info = jedis.zrangeByScore(Constants.INFO_KEY,captchaId,captchaId);
            if(info != null && info.size() > 0){
                byte[] inf = info.get(0);
                byte[] image = jedis.zrangeByScore(Constants.IMAGE_KEY,captchaId,captchaId).get(0);
                captcha = Captcha.fromRedis(captchaId,inf,image);
            }
            else {
                List<byte[]> infos = jedis.zrangeByScore(Constants.OLD_INFO_KEY,captchaId,captchaId);
                if(infos != null && infos.size() > 0) {
                    byte[] inf = infos.get(0);
                    byte[] image = jedis.zrangeByScore(Constants.OLD_IMAGE_KEY,captchaId,captchaId).get(0);
                    captcha = Captcha.fromRedis(captchaId, inf, image);
                }
            }
        }
        return captcha;
    }

    private Captcha getRandomCaptchaWithoutImage(){
        try(Jedis jedis = redisClient.getClient()){
            Tuple randCaptcha =jedis.zrandmemberWithScores(Constants.INFO_KEY,1).get(0);
            double id = randCaptcha.getScore();
            byte[] info = randCaptcha.getBinaryElement();
            return Captcha.fromRedis(id,info);
        }
    }

    public Captcha linkCodeToRandomCaptcha(String code){
        Captcha c = getRandomCaptchaWithoutImage();
        try(Jedis jedis = redisClient.getClient()){
            String key = "captcha:link:"+code;
            long seconds = 5*60; // exists 5 minutes
            jedis.setex(key,seconds,Double.toString(c.id));
        }
        return c;
    }

    public Captcha getLinkedCaptcha(String code){
        try(Jedis jedis = redisClient.getClient()){
            String key = "captcha:link:"+code;
            String id = jedis.get(key);
            if(Strings.isNotEmpty(id)){
                return getCaptcha(Double.parseDouble(id));
            }
        }
        return null;
    }
    // endregion

    // region verify
    public boolean verify(String code,int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4){
        int distanceThreshold = this.captchaConfig.verify_distance;
        try(Jedis jedis = redisClient.getClient()){
            String key = "captcha:link:"+code;
            String id = jedis.get(key);
            jedis.del(key);//only once attempt
            if(!Strings.isEmpty(id)){
                double captchaId = Double.parseDouble(id);
                Captcha c = getCaptcha(captchaId);
                boolean verified = isNear(x1,y1,c.getInfo().get(0),distanceThreshold) &&
                        isNear(x2,y2,c.getInfo().get(1),distanceThreshold)&&
                        isNear(x3,y3,c.getInfo().get(2),distanceThreshold)&&
                        isNear(x4,y4,c.getInfo().get(3),distanceThreshold);
                if(verified){
                    String v_key = "captcha:verify:"+code;
                    long seconds = 5*60; // exists 5 minutes
                    jedis.setex(v_key,seconds,"1");
                }
                return verified;
            }
        }
        return false;
    }

    static boolean isNear(int x1, int y1, CaptchaText info, int distanceThreshold){
        double threshold = distanceThreshold + info.fontSize/2.0;
        double distance = Math.sqrt(Math.pow(x1-info.x,2) + Math.pow(y1 - info.y,2));
        logger.info(String.format("(x1:%d,y1:%d),(x2:%d,y2:%d,font:%d),dist:%.2f,thresh:%.2f %s",
                x1,y1,info.getX(),info.getY(),info.getFontSize(),distance,threshold,distance<threshold?"pass":"no pass"));
        return distance < threshold;
    }
    // endregion

    // region check
    public boolean check(String code,String key){
        if(!this.captchaConfig.checkKey.equals(key)){
            throw new IllegalArgumentException("Illegal check key.");
        }
        try(Jedis jedis = redisClient.getClient()){
            String id = "captcha:verify:"+code;
            String result = jedis.get(id);
            jedis.del(id);
            if(!Strings.isEmpty(result) && "1".equals(result)){
                return true;
            }
        }
        return false;
    }
    // endregion

    //#region prepare data

    public void prepareCaptcha(int count) {
        CaptchaUtils utils = this.captchaUtils;
        try(Jedis jedis = this.redisClient.getClient()){
//            Long startTime = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                double captchaId = utils.getCaptchaId(jedis);
                Captcha captcha = utils.generateCaptchaImage();
                jedis.zadd(Constants.IMAGE_KEY,captchaId,captcha.getImage());
                jedis.zadd(Constants.INFO_KEY,captchaId,captcha.toRedisInfoValue());
            }
//            long endTime = System.currentTimeMillis();
//            long elapsedTime = endTime - startTime;
//            logger.debug(count + " captcha generated! cost "+ elapsedTime + "ms");
        }
    }

    public void refreshCaptcha(int count) {
        prepareCaptcha(count);
        try (Jedis jedis = redisClient.getClient()) {
            long idBeforeHalfHour = captchaUtils.getEpochSeconds(LocalDateTime.now().minusMinutes(30));
            long idBeforeAnHour = captchaUtils.getEpochSeconds(LocalDateTime.now().minusMinutes(60));

            List<Tuple> oldCaptchaImages = jedis.zrangeByScoreWithScores(Constants.IMAGE_KEY, 0, idBeforeHalfHour);
            List<Tuple> oldCaptchaInfo = jedis.zrangeByScoreWithScores(Constants.INFO_KEY, 0, idBeforeHalfHour);

            for (Tuple oldImage : oldCaptchaImages) {
                jedis.zadd(Constants.OLD_IMAGE_KEY, oldImage.getScore(), oldImage.getBinaryElement());
            }
            for (Tuple oldInfo : oldCaptchaInfo) {
                jedis.zadd(Constants.OLD_INFO_KEY, oldInfo.getScore(), oldInfo.getBinaryElement());
            }
            jedis.zremrangeByScore(Constants.IMAGE_KEY, 0, idBeforeHalfHour);
            jedis.zremrangeByScore(Constants.INFO_KEY, 0, idBeforeHalfHour);
            jedis.zremrangeByScore(Constants.OLD_INFO_KEY, 0, idBeforeAnHour);
            jedis.zremrangeByScore(Constants.OLD_IMAGE_KEY, 0, idBeforeAnHour);
        }
    }

    //#endregion
}
