#!/usr/bin/env bash

PREFIX=
FILEEXTENSION=.postman_collection.json

TESTSUBDIR=ids/egk/
TESTFILE=mainzelliste_id_egk_prob381__EditPatient_addNewExternalID


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
            initTestEnvironment $subDir
          done;
     else
        echo "Execute Dir File ${f}"
        initTestEnvironment ${f}
     fi
  done;
