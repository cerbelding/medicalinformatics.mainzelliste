#!/usr/bin/env bash

source ./executeTestFunctions.sh
# Iterates all Test Dir and Subdirectories and execute the newman Test





iterateDirAndExecuteFunction  newman_tests/ executeNewmanTest
