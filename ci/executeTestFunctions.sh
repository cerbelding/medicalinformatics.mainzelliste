#Debug Options:
#&& ls -ll && ls -ll ./ && ls -ll ./newman_mainzelliste_configs && ls -ll ./newman_mainzelliste_configs/active_config \

initConfigs(){
   echo "Searching  newman_mainzelliste_configs/${1##*/}.conf .." \
    && if [ -f newman_mainzelliste_configs/${1##*/}.conf ]
      then
        echo "ci/newman_mainzelliste_configs/${1##*/}.conf exists"
        cp newman_mainzelliste_configs/${1##*/}.conf newman_mainzelliste_configs/active_config/mainzellisteConfig
    else
        echo "no ci/newman_mainzelliste_configs/${1##*/}.conf - use default config"
        cp newman_mainzelliste_configs/mainzelliste_default.conf newman_mainzelliste_configs/active_config/mainzellisteConfig
      fi \


    echo "Searching  test_data/${1##*/}.json .." \
    && if [ -f  test_data/${1##*/}.json ]
      then
        echo "ci/test_data/${1##*/}.json exists"
        NEWMANDATA=${1##*/}.json
    else
        echo "no ci/test_data/${1##*/}.json - use default data"
        NEWMANDATA=5000Idat.json
      fi \

}

executeNewmanTest(){

    initConfigs $1 \
    && docker-compose -f ../docker-compose.test.yml run \
    --service-ports newman run -d \
    test_data/${NEWMANDATA} -n 1 -e newman_environment_variables/newman_environmentVariables.json ${1} \
    --reporters="cli,htmlextra,junit" --reporter-htmlextra-export=test_results/"${1##*/}.html" --reporter-junit-export=test_results/"${1##*/}.xml" \
    || docker-compose down  \
    && docker-compose down;




}


# This functon creates an TestEnvironment for Testing the Mainzelliste with Postman.
initTestEnvironment(){

    initConfigs $1 \
    && docker-compose -f ../docker-compose.test.yml up
}


: "
This function iterates ofer all Subfolders, if an file is detected the $2 function ist called with the filename as parameter
 paramters: path to the folder/file
 last parameter : Function which should called
"
function iterateDirAndExecuteFuntion(){

  length=$(($#-1))
  SUBFILES=${@:1:$length}
  FUNCTION=${@: -1}

  for subFile in $SUBFILES
    do
      if [ -d ${subFile} ]
      then
        iterateDirAndExecuteFuntion $subFile/*  $FUNCTION
      else
         $FUNCTION ${subFile}
      fi;
    done;


}


