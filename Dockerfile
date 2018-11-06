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
COPY /config/ /etc/mainzelliste/
CMD ["catalina.sh", "run"]