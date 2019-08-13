#!/bin/bash -e

MAND_VARS="ML_REVERSEPROXY_FQDN ML_DB_PASS ML_API_KEY"
MAND_FILES=""

MISSING_VARS=""

## process docker secrets
if [ -e "/run/secrets/mainzellisteDbName" ]; then \
	ML_DB_NAME=$(cat /run/secrets/mainzellisteDbName) \
;fi && if [ -e "/run/secrets/mainzellisteDbUser" ]; then \
	ML_DB_USER=$(cat /run/secrets/mainzellisteDbUser) \
;fi && if [ -e "/run/secrets/mainzellisteDbPassword" ]; then \
	ML_DB_PASS=$(cat /run/secrets/mainzellisteDbPassword) \
;fi && if [ -e "/run/secrets/mainzellisteApiKey" ]; then \
	ML_API_KEY=$(cat /run/secrets/mainzellisteApiKey) \
;fi

: "${ML_DB_DRIVER:=org.postgresql.Driver}"
: "${ML_DB_TYPE:=postgresql}"
: "${ML_DB_HOST:=db}"
: "${ML_DB_PORT:=5432}"
: "${ML_DB_NAME:=mainzelliste}"
: "${ML_DB_USER:=mainzelliste}"
: "${ML_ALLOWEDREMOTEADDRESSES:=0.0.0.0/0}"
: "${ML_REVERSEPROXY_SSL:=false}"

if [ -z "$ML_REVERSEPROXY_PORT" ]; then
	case "$ML_REVERSEPROXY_SSL" in
		true)
			ML_REVERSEPROXY_PORT=443
			;;
		false)
			ML_REVERSEPROXY_PORT=80
			;;
		*)
			echo "Please set ML_REVERSEPROXY_SSL to either true or false."
			exit 1
	esac
fi

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

if [ -e "/run/secrets/mainzellisteConfig" ]; then
	echo "mainzelliste docker entrypoint - Applying user-provided mainzelliste.conf."
	cp /run/secrets/mainzellisteConfig /etc/mainzelliste/mainzelliste.conf
else
	echo "mainzelliste docker entrypoint - Generating new mainzelliste.conf from environment variables"
	sed -e "s|# db.driver = org.postgresql.Driver|db.driver = $ML_DB_DRIVER|g ; \
		s|# db.url = jdbc:postgresql://localhost:5432/mainzelliste|db.url = jdbc:$ML_DB_TYPE://$ML_DB_HOST:$ML_DB_PORT/$ML_DB_NAME|g ; \
		s|db.username = mainzelliste|db.username = $ML_DB_USER|g ; \
		s|db.password = mainzelliste|db.password = $ML_DB_PASS|g ; \
		s|^\(servers.0.apiKey =\).*$|\1 $ML_API_KEY|g ; \
		s|\(# Accepted origin addresses.*\)|\1\nservers.0.allowedRemoteAdresses = $ML_ALLOWEDREMOTEADDRESSES|g" \
		/mainzelliste.conf.default \
		> /etc/mainzelliste/mainzelliste.conf
fi

sed -e "s|ML_REVERSEPROXY_FQDN|$ML_REVERSEPROXY_FQDN|g ; \
	s|ML_REVERSEPROXY_PORT|$ML_REVERSEPROXY_PORT|g ; \
	s|ML_REVERSEPROXY_SSL|$ML_REVERSEPROXY_SSL|g" \
	/usr/local/tomcat/conf/server.xml.ori > /usr/local/tomcat/conf/server.xml

if [ "$DEBUG" = 'true' ]; then
	echo "mainzelliste docker entrypoint - Tomcat starting with debug true"
	export JPDA_ADDRESS=1099
	echo "mainzelliste docker entrypoint - Set JPDA_ADRESS to: 1099"
	export JPDA_TRANSPORT=dt_socket
	echo "mainzelliste docker entrypoint - Set JPDA_TRANSPORT to: " $JPDA_TRANSPORT
	exec catalina.sh jpda run
else
	echo "mainzelliste docker entrypoint - Tomcat starting with debug false"
	exec catalina.sh run
fi
