## The maven-samply image needs to be build locally by using verbis.maven Dockerfile
FROM maven-samply AS build
COPY ./ /workingdir/
WORKDIR /workingdir
RUN mvn clean && \
    mvn install && \
    mkdir -p extracted && \
    cd extracted && \
    unzip /workingdir/target/mainzelliste-*.war

FROM tomcat:8-jre8-alpine
RUN rm -r /usr/local/tomcat/webapps/*
COPY --from=build /workingdir/extracted/ /usr/local/tomcat/webapps/ROOT/
COPY ./ml_entrypoint.sh /ml_entrypoint.sh
COPY ./config/mainzelliste.conf.default /mainzelliste.conf.default
RUN mkdir /etc/mainzelliste && touch /etc/mainzelliste/mainzelliste.conf
ENTRYPOINT [ "/ml_entrypoint.sh" ]