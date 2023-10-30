package com.github.thinhunan.wonder8.captchaservice.modules;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public final class RedisClient {

    private static final Logger logger = LoggerFactory.getLogger("RedisClient");
    
    @Value("${redis.host}")
    String redisHost;

    @Value("${redis.port}")
    int redisPort;

    private JedisPool pool;

    private io.lettuce.core.RedisClient reactiveClient;
    private StatefulRedisConnection<byte[], byte[]> reactiveByteArrayConnection;
    private StatefulRedisConnection<String, String> reactiveStringConnection;

    @PostConstruct
    public void prepareClientPool(){
        logger.info("connect redis @"+this.redisHost+this.redisPort);
        prepareJedisPool();
        prepareLettuceClient();
    }

    @PreDestroy
    public void destoryResource(){
        if(reactiveByteArrayConnection != null && reactiveByteArrayConnection.isOpen()){
            reactiveByteArrayConnection.close();
            logger.info("reactiveByteArrayConnection is closed");
        }
        if(reactiveStringConnection != null && reactiveStringConnection.isOpen()){
            reactiveStringConnection.close();
            logger.info("reactiveStringConnection is closed");
        }
    }

    private void prepareJedisPool(){
        this.pool = new JedisPool(this.redisHost,this.redisPort);
    }

    private void prepareLettuceClient(){
        reactiveClient = io.lettuce.core.RedisClient.create("redis://"+redisHost+":"+redisPort);
        reactiveByteArrayConnection = reactiveClient.connect(RedisCodec.of(new ByteArrayCodec(), new ByteArrayCodec()));
        reactiveStringConnection = reactiveClient.connect();
    }

    public Jedis getClient(){
        return pool.getResource();
    }

    public RedisReactiveCommands<byte[], byte[]> getReactiveByteArrayCommands(){
        return reactiveByteArrayConnection.reactive();
    }

    public RedisReactiveCommands<String, String> getReactiveStringCommands(){
        return reactiveStringConnection.reactive();
    }
}
