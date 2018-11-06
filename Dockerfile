## Add here maven support
FROM tomcat:8-jre8-alpine
RUN rm -r /usr/local/tomcat/webapps/*
## This needs to go to root or some specific folder
COPY /target/mainzelliste-1.7.0.war /usr/local/tomcat/webapps/
COPY /config/ /etc/mainzelliste/
CMD ["catalina.sh", "run"]