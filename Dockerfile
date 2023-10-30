FROM openjdk:8-jdk-alpine

ARG JAVA_OPTS
ENV JAVA_OPTS=$JAVA_OPTS

#RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories
    # chinese font
RUN apk add ttf-dejavu \
  # supervisor
  && apk add supervisor \
  && rm -rf /var/cache/apk/* \
  && mkdir /log \
  && mkdir /data

# support chinese font
COPY containerize/SIMSUN.ttf /usr/lib/jvm/java-1.8-openjdk/jre/lib/fonts/SIMSUN.ttf
COPY ["containerize/redis.conf","containerize/supervisord.conf","/etc/"]
COPY src/main/resources/logback-docker.xml /etc/logback.xml
# install redis
#COPY redis-6.2.7.tar.gz /redis-6.2.7.tar.gz
#RUN apk add make gcc g++ linux-headers tar
#RUN tar zxvf redis-6.2.7.tar.gz && cd redis-6.2.7 && make && make install
#RUN apk del make gcc g++ linux-headers tar
#RUN rm -rfv /redis-6.2.7 /redis-6.2.7.tar.gz
COPY containerize/redis-server.6.2.7_alpine /usr/local/bin/redis-server
COPY containerize/redis-cli.6.2.7_alpine /usr/local/bin/redis-cli

COPY src/main/resources/images/* /images/

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} captchaservice.jar
#COPY target/captcha-service-0.0.1-SNAPSHOT.jar captchaservice.jar
EXPOSE 8080

## For Spring-Boot project, use the entrypoint below to reduce Tomcat startup time.
#ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar captchaservice.jar --spring.profiles.active=docker
ENTRYPOINT ["supervisord","-c","/etc/supervisord.conf"]

