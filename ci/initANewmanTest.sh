#!/usr/bin/env bash
source ./executeTestFunctions.sh
PREFIX=
FILEEXTENSION=.postman_collection.json

TESTSUBDIR=
TESTFILE=mainzelliste_getAllPatiens_with_idType


FULLPATH=${TESTSUBDIR}${PREFIX}${TESTFILE}${FILEEXTENSION}


#Creating an Testenvironment
iterateDirAndExecuteFunction  newman_tests/$FULLPATH initTestEnvironment

