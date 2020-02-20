#!/usr/bin/env bash
source ./testHelpers.sh


PREFIX=
FILEEXTENSION=.postman_collection.json

if [ -n "$1" ]; then

    TESTFILE="$1"
    FULLPATH=${TESTFILE}${FILEEXTENSION}

else
    TESTFILE=mainzelliste_session_authorization_is_invalid
    TESTSUBDIR=sessions/
    FULLPATH=${TESTSUBDIR}${PREFIX}${TESTFILE}${FILEEXTENSION}
fi

echo "Use testfile $TESTFILE"


iterateDirAndExecuteFunction  newman_tests/$FULLPATH executeNewmanTest

