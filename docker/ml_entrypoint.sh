#!/bin/bash -e

MAND_VARS="ML_PUBLICURL_FQDN ML_DB_PASS ML_API_KEY"
MAND_FILES=""

MISSING_VARS=""

## process docker secrets
if [ -e "$ML_DB_NAME_FILE" ]; then \
	ML_DB_NAME=$(cat $ML_DB_NAME_FILE) \
;fi && if [ -e "$ML_DB_USER_FILE" ]; then \
	ML_DB_USER=$(cat $ML_DB_USER_FILE) \
;fi && if [ -e "$ML_DB_PASS_FILE" ]; then \
	ML_DB_PASS=$(cat $ML_DB_PASS_FILE) \
;fi && if [ -e "$ML_API_KEY_FILE" ]; then \
	ML_API_KEY=$(cat $ML_API_KEY_FILE) \
;fi

: "${ML_DB_DRIVER:=org.postgresql.Driver}"
: "${ML_DB_TYPE:=postgresql}"
: "${ML_DB_HOST:=db}"
: "${ML_DB_PORT:=5432}"
: "${ML_DB_NAME:=mainzelliste}"
: "${ML_DB_USER:=mainzelliste}"
: "${ML_PUBLICURL_PORT:=443}"

for VAR in $MAND_VARS; do
	if [ -z "${!VAR}" ]; then
		MISSING_VARS+="$VAR "
	fi
done

if [ -n "$MISSING_VARS" ]; then
	echo "Error: Mandatory variables not defined (see documentation): $MISSING_VARS"
	exit 1
fi

MISSING_FILES=""

for VAR in $MAND_FILES; do
	if [ ! -e /docker/conf/$VAR ]; then
		MISSING_FILES+="$VAR "
	fi
done

if [ -n "$MISSING_FILES" ]; then
	echo "Error: Mandatory files not present in conf/ directory (see documentation): $MISSING_FILES"
	exit 1
fi

if [ -e "$ML_CONFIG_FILE" ]; then
	echo "mainzelliste docker entrypoint - Applying user-provided mainzelliste.conf."
	cp $ML_CONFIG_FILE /etc/mainzelliste/mainzelliste.conf
else
	echo "mainzelliste docker entrypoint - Generating new mainzelliste.conf from environment variables"
	sed -e "s|# db.driver = org.postgresql.Driver|db.driver = $ML_DB_DRIVER|g ; \
		s|# db.url = jdbc:postgresql://localhost:5432/mainzelliste|db.url = jdbc:$ML_DB_TYPE://$ML_DB_HOST:$ML_DB_PORT/$ML_DB_NAME|g ;\
		s|db.username = mainzelliste|db.username = $ML_DB_USER|g ;\
		s|db.password = mainzelliste|db.password = $ML_DB_PASS|g ;
		s|servers.0.apiKey =|servers.0.apiKey = $ML_API_KEY|g ; \
		s|\(# Accepted origin addresses.*\)|\1\nservers.0.allowedRemoteAdresses = $ML_ALLOWEDREMOTEADDRESSES|g" \
		/mainzelliste.conf.default \
		> /etc/mainzelliste/mainzelliste.conf
fi

echo "Configuring Tomcat for public address https://$ML_PUBLICURL_FQDN on port $ML_PUBLICURL_PORT"

sed -e "s|ML_PUBLICURL_FQDN|$ML_PUBLICURL_FQDN|g ; \
	s|ML_PUBLICURL_PORT|$ML_PUBLICURL_PORT|g" \
	/usr/local/tomcat/conf/server.xml.ori > /usr/local/tomcat/conf/server.xml

if [ "$DEBUG" = 'TRUE' ]; then
	echo "mainzelliste docker entrypoint - Tomcat starting with debug true"
	export JPDA_ADDRESS=$DEBUG_PORT
	echo "mainzelliste docker entrypoint - Set JPDA_ADRESS to: " $JPDA_ADDRESS
	export JPDA_TRANSPORT=dt_socket
	echo "mainzelliste docker entrypoint - Set JPDA_TRANSPORT to: " $JPDA_TRANSPORT
	exec catalina.sh jpda run
else
	echo "mainzelliste docker entrypoint - Tomcat starting with debug false"
	exec catalina.sh run
fi
