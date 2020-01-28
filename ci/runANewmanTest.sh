#!/usr/bin/env bash
source ./executeTestFunctions.sh



if [ -n "$1" ]; then

    TESTFILE="$1"
else
    TESTFILE=mainzelliste_session_authorization_is_invalid
fi

echo "Use testfile $TESTFILE"


PREFIX=
FILEEXTENSION=.postman_collection.json

TESTSUBDIR=sessions/





FULLPATH=${TESTSUBDIR}${PREFIX}${TESTFILE}${FILEEXTENSION}



iterateDirAndExecuteFunction  newman_tests/$FULLPATH executeNewmanTest

