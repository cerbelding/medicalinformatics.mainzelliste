#!/bin/bash -e

: "${ML_DB_DRIVER:=org.postgresql.Driver}"
: "${ML_DB_TYPE:=postgresql}"
: "${ML_DB_HOST:=db}"
: "${ML_DB_PORT:=5432}"
: "${ML_DB_NAME:=mainzelliste}"
: "${ML_DB_USER:=mainzelliste}"
: "${ML_DB_PASS:=mainzelliste}"

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

if [ -e "$ML_CONFIG_FILE" ]; then \
    CONFIG_FILE=$ML_CONFIG_FILE
else
    CONFIG_FILE=/mainzelliste.conf.default
fi

if [ -e /mainzelliste.conf.default ]; then
	echo "Generating new mainzelliste.conf from environment variables."
	sed -e "s|# db.driver = org.postgresql.Driver|db.driver = $ML_DB_DRIVER|g ; \
		s|# db.url = jdbc:postgresql://localhost:5432/mainzelliste|db.url = jdbc:$ML_DB_TYPE://$ML_DB_HOST:$ML_DB_PORT/$ML_DB_NAME|g ;\
		s|db.username = mainzelliste|db.username = $ML_DB_USER|g ;\
		s|db.password = mainzelliste|db.password = $ML_DB_PASS|g ;
		s|servers.0.apiKey =|servers.0.apiKey = $ML_API_KEY|g" \
		/mainzelliste.conf.default \
		> /etc/mainzelliste/mainzelliste.conf
else
	echo "Applying user-provided mainzelliste.conf."
fi

exec catalina.sh run