FROM eclipse-temurin:17
RUN apt-get update && \
    apt-get install -y vim && \
    apt-get install -y procps coreutils && \
    apt-get clean

RUN echo "Asia/Shanghai" > /etc/timezone
ENV TZ=Asia/Shanghai

ARG app_home=/opt/app
WORKDIR ${app_home}
ARG jar_file=target/*.jar
COPY ${jar_file} lib/app.jar

ARG server_port=9000
ENV SERVER_PORT_ENV=${server_port}
ARG java_opts="-XX:+UseZGC -Xms256M -Dsun.net.inetaddr.ttl=30 -Dsun.net.inetaddr.negative.ttl=10 -Djava.security.egd=file:/dev/urandom"
ARG spring_opts="-Dspring.profiles.active=prod -Dserver.port=${SERVER_PORT_ENV}"
ENV JAVA_OPTS_ENV="${java_opts} ${spring_opts}"
ENV USER_DIR=${app_home}

EXPOSE ${SERVER_PORT_ENV}

CMD java -Duser.dir=${USER_DIR} ${JAVA_OPTS_ENV} ${JAVA_OPTS} -jar ${USER_DIR}/lib/app.jar