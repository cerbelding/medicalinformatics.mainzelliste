#!/bin/bash -e

: "${ML_DB_DRIVER:=org.postgresql.Driver}"
: "${ML_DB_TYPE:=postgresql}"
: "${ML_DB_HOST:=db}"
: "${ML_DB_PORT:=5432}"
: "${ML_DB_NAME:=mainzelliste}"
: "${ML_DB_USER:=mainzelliste}"
: "${ML_DB_PASS:=mainzelliste}"

if [ -e /etc/mainzelliste/mainzelliste.conf.docker ]; then
	echo "Generating new mainzelliste.conf from environment variables."
	sed -e "s/ML_DB_DRIVER/$ML_DB_DRIVER/g ; \
		s/ML_DB_TYPE/$ML_DB_TYPE/g ;\
		s/ML_DB_HOST/$ML_DB_HOST/g ;\
		s/ML_DB_PORT/$ML_DB_PORT/g ;\
		s/ML_DB_NAME/$ML_DB_NAME/g ;\
		s/ML_DB_USER/$ML_DB_USER/g ;\
		s/ML_DB_PASS/$ML_DB_PASS/g" \
		/etc/mainzelliste/mainzelliste.conf.docker \
		> /etc/mainzelliste/mainzelliste.conf
else
	echo "Applying user-provided mainzelliste.conf."
fi

exec catalina.sh run