#!/usr/bin/env bash
source ./testHelpers.sh

ROOTFOLDER=newman_tests/


if [ -n "$1" ]; then
    getPath  "${1}" ${ROOTFOLDER}
    echo $FULLPATH
    iterateDirAndExecuteFunction  $FULLPATH initTestEnvironment
else
    echo "Error Code 2 Please subnmit a valid Path"
fi


