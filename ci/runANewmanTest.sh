#!/usr/bin/env bash

PREFIX=
FILEEXTENSION=.postman_collection.json

TESTSUBDIR=/
TESTFILE=mainzelliste_prob381_externalID_EditPatient_addNewExternalID


FULLPATH=${TESTSUBDIR}${PREFIX}${TESTFILE}${FILEEXTENSION}

source ./executeTestFunctions.sh
#Creating an Testenvironment
for f in newman_tests/$FULLPATH;
  do
     if [ -d $f ]
      then
         echo "Execute Subdir $f"
        for subDir in ${f}/*;

          do
            echo "Execute Subdir File $subDir"
            executeNewmanTest $subDir
          done;
     else
        echo "Execute Dir File ${f}"
        executeNewmanTest ${f}
     fi
  done;
