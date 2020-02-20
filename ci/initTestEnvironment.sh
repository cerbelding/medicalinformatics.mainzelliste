#!/usr/bin/env bash
source ./testHelpers.sh
PREFIX=
FILEEXTENSION=.postman_collection.json

TESTSUBDIR=tokens/validate/
TESTFILE=mainzelliste_validate_token_addPatient


FULLPATH=${TESTSUBDIR}${PREFIX}${TESTFILE}${FILEEXTENSION}


#Creating an Testenvironment
iterateDirAndExecuteFunction  newman_tests/$FULLPATH initTestEnvironment

