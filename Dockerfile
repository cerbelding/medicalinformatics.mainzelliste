FROM maven:alpine AS build
COPY ./ /workingdir/
RUN cd /workingdir && \
    mvn clean && \
    mvn install && \
    mvn package && \
    mkdir -p /workingdir/extracted && \ 
    cd /workingdir/extracted && \
    unzip /workingdir/target/mainzelliste-*.war

FROM tomcat:8-jre8-alpine
RUN rm -r /usr/local/tomcat/webapps/*
COPY --from=build /workingdir/extracted/ /usr/local/tomcat/webapps/ROOT/
COPY /config/mainzelliste.conf.docker /etc/mainzelliste/mainzelliste.conf.docker
COPY /ml_entrypoint.sh /ml_entrypoint.sh
RUN dos2unix /ml_entrypoint.sh
ENTRYPOINT [ "/ml_entrypoint.sh" ]