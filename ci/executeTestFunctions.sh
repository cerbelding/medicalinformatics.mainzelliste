#Debug Options:
#&& ls -ll && ls -ll ./ && ls -ll ./newman_mainzelliste_configs && ls -ll ./newman_mainzelliste_configs/active_config \



executeNewmanTest(){

    echo "Searching  newman_mainzelliste_configs/${1##*/}.conf .." \
    && if [ -f newman_mainzelliste_configs/${1##*/}.conf ]
      then
        echo "ci/newman_mainzelliste_configs/${1##*/}.conf exists"
        cp newman_mainzelliste_configs/${1##*/}.conf newman_mainzelliste_configs/active_config/mainzellisteConfig
    else
        echo "no ci/newman_mainzelliste_configs/${1##*/}.conf - use default config"
        cp newman_mainzelliste_configs/mainzelliste_default.conf newman_mainzelliste_configs/active_config/mainzellisteConfig
      fi \
    && docker-compose -f ../docker-compose.test.yml run \
    --service-ports newman run -d \
    test_data/5000Idat.json -n 1 -e newman_environment_variables/newman_environmentVariables.json ${1} \
    --reporters="cli,htmlextra,junit" --reporter-htmlextra-export=test_results/"${1##*/}.html" --reporter-junit-export=test_results/"${1##*/}.xml" \
    || docker-compose down  \
    && docker-compose down;




}


# This functon creates an TestEnvironment for Testing the Mainzelliste with Postman.
initTestEnvironment(){

    echo "Searching newman_mainzelliste_configs/${1##*/}.conf .." \
     && if [ -f newman_mainzelliste_configs/${1##*/}.conf ]
      then
        echo "ci/newman_mainzelliste_configs/${1##*/}.conf exists"
        cp newman_mainzelliste_configs/${1##*/}.conf newman_mainzelliste_configs/active_config/mainzellisteConfig
    else
        echo "no ci/newman_mainzelliste_configs/${1##*/}.conf - use default config"
        cp newman_mainzelliste_configs/mainzelliste_default.conf newman_mainzelliste_configs/active_config/mainzellisteConfig
      fi \
    && docker-compose -f ../docker-compose.test.yml up
}
