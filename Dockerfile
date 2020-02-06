FROM maven:3.6-alpine AS build

ARG http_proxy=""

COPY ./ /workingdir/
WORKDIR /workingdir
RUN apk add dos2unix
RUN cp docker/maven_proxy_parser.sh /usr/local/bin/ && \
    chmod +x /usr/local/bin/maven_proxy_parser.sh && \
    dos2unix /usr/local/bin/maven_proxy_parser.sh
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

FROM tomcat:8-jdk8-openjdk-slim

ENV ML_CONFIG_FILE ""

RUN apt-get update && \
	apt-get -y install patch && \
	rm -rf /var/lib/apt/lists/*

## Create mainzelliste user with www-data group
RUN set -x ; \
    adduser --disabled-password --system --ingroup www-data mainzelliste

COPY --from=build --chown=mainzelliste:www-data /workingdir/extracted/ /usr/local/tomcat/webapps/ROOT/
COPY --chown=mainzelliste:www-data ./docker/ml_entrypoint.sh ./config/mainzelliste.conf.default /
RUN mkdir /etc/mainzelliste && touch /etc/mainzelliste/mainzelliste.conf && chown -R mainzelliste /etc/mainzelliste/mainzelliste.conf
RUN chmod u+x /ml_entrypoint.sh && chmod u+r /mainzelliste.conf.default && chmod u+rw /etc/mainzelliste/mainzelliste.conf
COPY ./docker/tomcat.*.patch /usr/local/tomcat/conf
RUN dos2unix /ml_entrypoint.sh /usr/local/tomcat/conf/tomcat.*.patch && apk del dos2unix

RUN cd /usr/local/tomcat/conf && \
	patch -i *.patch && \
	rm *.patch && \
	mv server.xml server.xml.ori

ENTRYPOINT [ "/ml_entrypoint.sh" ]
