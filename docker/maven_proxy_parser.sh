#!/bin/bash -e

parse(){
    read -a URL_TOKENS <<< $(echo $http_proxy | awk -v FS='(://|:|@)' -v OFS='\t' '{print $1,$2,$3,$4,$5}')
    declare -A RETURN_TOKENS;
    if [ -z "${URL_TOKENS[3]}" ]; then \
        RETURN_TOKENS["PROXY_SCHEMA"]="${URL_TOKENS[0]}"
        RETURN_TOKENS["PROXY_HOST"]="${URL_TOKENS[1]}"
        RETURN_TOKENS["PROXY_PORT"]="${URL_TOKENS[2]}"
    else
        RETURN_TOKENS["PROXY_SCHEMA"]="${URL_TOKENS[0]}"
        RETURN_TOKENS["PROXY_USER"]="${URL_TOKENS[1]}"
        RETURN_TOKENS["PROXY_USER_PASSWORD"]="${URL_TOKENS[2]}"
        RETURN_TOKENS["PROXY_HOST"]="${URL_TOKENS[3]}"
        RETURN_TOKENS["PROXY_PORT"]="${URL_TOKENS[4]}"
    fi
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings" -t elem -n proxies -v "" /usr/share/maven/ref/settings-docker.xml | \
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings/x:proxies" -t elem -n proxy -v "" | \
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings/x:proxies/x:proxy" -t elem -n id -v "mavenProxy" | \
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings/x:proxies/x:proxy" -t elem -n active -v "true" | \
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings/x:proxies/x:proxy" -t elem -n protocol -v "${RETURN_TOKENS[PROXY_SCHEMA]}" | \
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings/x:proxies/x:proxy" -t elem -n host -v "${RETURN_TOKENS[PROXY_HOST]}" | \
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings/x:proxies/x:proxy" -t elem -n port -v "${RETURN_TOKENS[PROXY_PORT]}" | \
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings/x:proxies/x:proxy" -t elem -n username -v "${RETURN_TOKENS[PROXY_USER]}" | \
    xmlstarlet ed -N x="http://maven.apache.org/SETTINGS/1.0.0" -s "x:settings/x:proxies/x:proxy" -t elem -n password -v "${RETURN_TOKENS[PROXY_USER_PASSWORD]}" > /usr/share/maven/conf/settings.xml
}

case $1 in
	parse)
		parse
		;;
	*)
		echo "Please supply command"
		exit 1
		;;
esac

exit 0
