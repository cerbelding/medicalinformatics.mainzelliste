#!/usr/bin/env bash
source ./executeTestFunctions.sh


PREFIX=
FILEEXTENSION=.postman_collection.json

TESTSUBDIR=sessions/
TESTFILE=mainzelliste_session_authorization_is_invalid


FULLPATH=${TESTSUBDIR}${PREFIX}${TESTFILE}${FILEEXTENSION}




iterateDirAndExecuteFuntion  newman_tests/$FULLPATH executeNewmanTest

