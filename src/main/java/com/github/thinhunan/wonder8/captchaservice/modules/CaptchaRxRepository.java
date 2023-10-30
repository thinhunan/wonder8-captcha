package com.github.thinhunan.wonder8.captchaservice.modules;

import com.github.thinhunan.wonder8.captchaservice.CaptchaConfig;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public class CaptchaRxRepository {

    private static final Logger logger = LoggerFactory.getLogger("CaptchaRxRepository");
    private RedisClient redisClient;
    private CaptchaConfig captchaConfig;
    private CaptchaUtils captchaUtils;

    public CaptchaRxRepository(CaptchaConfig captchaConfig, RedisClient redisClient, CaptchaUtils captchaUtils){
        this.captchaConfig = captchaConfig;
        this.redisClient = redisClient;
        this.captchaUtils = captchaUtils;
    }

    // region get captcha
    public Mono<Captcha> getCaptcha(double captchaId){
        RedisReactiveCommands<byte[], byte[]> commands = redisClient.getReactiveByteArrayCommands();
        Mono<byte[]> info = commands.zrangebyscore(Constants.INFO_KEY,Range.create(captchaId,captchaId)).singleOrEmpty();
        Mono<byte[]> image = commands.zrangebyscore(Constants.IMAGE_KEY,Range.create(captchaId,captchaId)).singleOrEmpty();
        return Mono.zip(info,image,(i1,i2)->Captcha.fromRedis(captchaId,i1,i2));
    }

    private Mono<Captcha> getRandomCaptchaWithoutImage(){
        RedisReactiveCommands<byte[], byte[]> commands = redisClient.getReactiveByteArrayCommands();
        return commands.zrandmemberWithScores(Constants.INFO_KEY,1).singleOrEmpty()
                .map(v->Captcha.fromRedis(v.getScore(),v.getValue()));
    }

    public Mono<Captcha> linkCodeToRandomCaptcha(String code){
        return getRandomCaptchaWithoutImage().doOnNext(captcha -> {
            String key = "captcha:link:"+code;
            long seconds = 5*60; // exists 5 minutes
            redisClient.getReactiveStringCommands()
                    .setex(key,seconds,Double.toString(captcha.id))
                    .subscribe();
            if(this.captchaConfig.logEnabled) {
                logger.info("register:" + code + " with " + (long) captcha.id);
            }
        });
    }

    public Mono<Captcha> getLinkedCaptcha(String code){
        String key = "captcha:link:"+code;
        return redisClient.getReactiveStringCommands().get(key).flatMap(id->getCaptcha(Double.parseDouble(id)));
    }

    //#endregion

    // region verify
    public Mono<Boolean> verify(String code,int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4){
        int distanceThreshold = this.captchaConfig.verify_distance;
        String key = "captcha:link:"+code;
        RedisReactiveCommands<String, String> cmd = redisClient.getReactiveStringCommands();
        return cmd.get(key).defaultIfEmpty("").flatMap(id->{
            if(Strings.isEmpty(id)){
                return Mono.just(false);
            }
            cmd.del(key).subscribe();
            double captchaId = Double.parseDouble(id);
            return getCaptcha(captchaId).map(c->{
                if(c == null || c.getInfo() == null){
                    return false;
                }
                boolean verified = isNear(x1,y1,c.getInfo().get(0),distanceThreshold) &&
                        isNear(x2,y2,c.getInfo().get(1),distanceThreshold)&&
                        isNear(x3,y3,c.getInfo().get(2),distanceThreshold)&&
                        isNear(x4,y4,c.getInfo().get(3),distanceThreshold);
                if(verified){
                    String v_key = "captcha:verify:"+code;
                    long seconds = 5*60; // exists 5 minutes
                    cmd.setex(v_key,seconds,"1").subscribe();
                }
                if(this.captchaConfig.logEnabled) {
                    logger.info(String.format("verify %s %s", code, verified));
                }
                return verified;
            });
        });
    }

    boolean isNear(int x1, int y1, CaptchaText info, int distanceThreshold){
        double threshold = distanceThreshold + info.fontSize/2.0;
        double distance = Math.sqrt(Math.pow(x1-info.x,2) + Math.pow(y1 - info.y,2));
        if(this.captchaConfig.logEnabled) {
            logger.info(String.format("(x1:%d,y1:%d),(x2:%d,y2:%d,font:%d),dist:%.2f,thresh:%.2f %s",
                    x1,y1,info.getX(),info.getY(),info.getFontSize(),distance,threshold,distance<threshold?"pass":"no pass"));
        }
        return distance < threshold;
    }
    // endregion

    // region check
    public Mono<Boolean> check(String code,String key){
        if(!this.captchaConfig.checkKey.equals(key)){
            throw new IllegalArgumentException("Illegal check key.");
        }
        String id = "captcha:verify:"+code;
        RedisReactiveCommands<String, String> cmd = redisClient.getReactiveStringCommands();
        return cmd.get(id).defaultIfEmpty("0").map(v->{
            cmd.del(id).subscribe();
            boolean verified = "1".equals(v);
            if(this.captchaConfig.logEnabled) {
                logger.info(String.format("check verify %s %s", code, verified));
            }
            return verified;
        });
    }
    // endregion

    //#region prepare data

    public void prepareCaptcha(int count) {
        CaptchaUtils utils = this.captchaUtils;
        RedisReactiveCommands<byte[], byte[]> cmd = redisClient.getReactiveByteArrayCommands();
        Flux.range(0,count).subscribe(integer -> utils.getCaptchaIdRx(cmd).subscribe(id->{
            Captcha captcha = utils.generateCaptchaImage();
            cmd.zadd(Constants.IMAGE_KEY,id,captcha.getImage()).subscribe();
            cmd.zadd(Constants.INFO_KEY,id,captcha.toRedisInfoValue()).subscribe();
        }));
    }

    public void refreshCaptcha(int count){
        this.prepareCaptcha(count);

        RedisReactiveCommands<byte[], byte[]> cmd = redisClient.getReactiveByteArrayCommands();
        long idBeforeHalfHour = captchaUtils.getEpochSeconds(LocalDateTime.now().minusMinutes(30));
        long idBeforeAnHour = captchaUtils.getEpochSeconds(LocalDateTime.now().minusMinutes(60));
        cmd.zrangebyscoreWithScores(
                        Constants.IMAGE_KEY,
                        Range.from(Range.Boundary.including(0), Range.Boundary.including(idBeforeHalfHour)),
                        Limit.unlimited())
                .doOnComplete(() -> cmd.zremrangebyscore(
                                Constants.IMAGE_KEY,
                                Range.from(Range.Boundary.including(0), Range.Boundary.including(idBeforeHalfHour)))
                        .subscribe())
                .subscribe(i -> cmd.zadd(Constants.OLD_IMAGE_KEY, i.getScore(), i.getValue()).subscribe());

        cmd.zrangebyscoreWithScores(
                        Constants.INFO_KEY,
                        Range.from(Range.Boundary.including(0), Range.Boundary.including(idBeforeHalfHour)),
                        Limit.unlimited())
                .doOnComplete(() -> cmd.zremrangebyscore(
                                Constants.INFO_KEY,
                                Range.from(Range.Boundary.including(0), Range.Boundary.including(idBeforeHalfHour)))
                        .subscribe())
                .subscribe(i -> cmd.zadd(Constants.OLD_INFO_KEY, i.getScore(), i.getValue()).subscribe());

        cmd.zremrangebyscore(
                        Constants.OLD_INFO_KEY,
                        Range.from(Range.Boundary.including(0), Range.Boundary.including(idBeforeAnHour)))
                .subscribe();
        cmd.zremrangebyscore(
                        Constants.OLD_IMAGE_KEY,
                        Range.from(Range.Boundary.including(0), Range.Boundary.including(idBeforeAnHour)))
                .subscribe();
    }

    //#endregion
}
