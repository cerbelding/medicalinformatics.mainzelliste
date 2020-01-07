#!/usr/bin/env bash
#for f in newman_tests/*;
TESTFILE=regex_Operations/mainzelliste_editPatient_regexError_name.postman_collection.json

source ./executeTestFunctions.sh
# Iterates all Test Dir and Subdirectories and execute the newman Test
for f in newman_tests/$TESTFILE;
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
     fi;
  done;