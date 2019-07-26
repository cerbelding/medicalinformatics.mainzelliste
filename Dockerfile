## The samply maven image needs to be build locally by using following command:
## docker-compose -f ./docker/samply-maven/docker-compose.maven.yml build --build-arg PROXY_HOST=<yourProxyHost> --build-arg PROXY_PORT=<yourProxyPort>
FROM maven:3.6.1-alpine AS build

ARG http_proxy=""

COPY ./ /workingdir/
WORKDIR /workingdir
RUN cp docker/maven_proxy_parser.sh /usr/local/bin/ && \
    chmod +x /usr/local/bin/maven_proxy_parser.sh
RUN if [ "$http_proxy" != "" ]; then \
        apk add --no-cache xmlstarlet && \
        ## TODO: Split Proxy Parsing from Maven Proxy Setting
        maven_proxy_parser.sh parse && \
        apk del xmlstarlet \
    ;fi

RUN cat /usr/share/maven/conf/settings.xml
RUN mvn clean && \
    mvn install && \
    mkdir -p extracted && \
    cd extracted && \
    unzip /workingdir/target/mainzelliste-*.war

FROM tomcat:8-jre8-alpine

ENV ML_CONFIG_FILE ""

RUN rm -r /usr/local/tomcat/webapps/*
COPY --from=build /workingdir/extracted/ /usr/local/tomcat/webapps/ROOT/
COPY ./docker/ml_entrypoint.sh /ml_entrypoint.sh
COPY ./config/mainzelliste.conf.default /mainzelliste.conf.default
RUN mkdir /etc/mainzelliste && touch /etc/mainzelliste/mainzelliste.conf
## Create Mainzelliste User and run tomcat with it
RUN set -x ; \
    addgroup -g 82 -S www-data && \
    adduser -u 82 -D -S -G www-data mainzelliste && \
    chown -R mainzelliste /usr/local/tomcat/ /etc/mainzelliste/mainzelliste.conf
USER mainzelliste
ENTRYPOINT [ "/ml_entrypoint.sh" ]