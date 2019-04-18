FROM maven:3.6.0-alpine AS build
ARG PROXY_HOST
ARG PROXY_PORT
RUN apk update && apk add xmlstarlet
COPY ./settings-template.xml /
RUN sed -e "s_<host/>_<host>$PROXY_HOST</host>_g; \
		s_<port/>_<port>$PROXY_PORT</port>_g; \
		s_<active>false</active>_<active>true</active>_g" \
		/settings-template.xml > /usr/share/maven/conf/settings.xml