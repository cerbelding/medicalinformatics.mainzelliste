FROM maven:3.6-alpine AS build

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

RUN mvn clean && \
    mvn install && \
    mkdir -p extracted && \
    cd extracted && \
    unzip /workingdir/target/mainzelliste-*.war

FROM tomcat:8-jre8-alpine

ENV ML_CONFIG_FILE ""

## Create mainzelliste user with www-data group
RUN set -x ; \
    addgroup -g 82 -S www-data && \
    adduser -u 82 -D -S -G www-data mainzelliste

RUN rm -r /usr/local/tomcat/webapps/*
COPY --from=build --chown=mainzelliste:www-data /workingdir/extracted/ /usr/local/tomcat/webapps/ROOT/
COPY --chown=mainzelliste:www-data ./docker/ml_entrypoint.sh ./config/mainzelliste.conf.default /
RUN mkdir /etc/mainzelliste && touch /etc/mainzelliste/mainzelliste.conf && chown -R mainzelliste /etc/mainzelliste/mainzelliste.conf
RUN chmod u+x /ml_entrypoint.sh && chmod u+r /mainzelliste.conf.default && chmod u+rw /etc/mainzelliste/mainzelliste.conf
COPY ./docker/tomcat.*.patch /usr/local/tomcat/conf

RUN cd /usr/local/tomcat/conf && \
	patch -i *.patch && \
	rm *.patch && \
	mv server.xml server.xml.ori

ENTRYPOINT [ "/ml_entrypoint.sh" ]
