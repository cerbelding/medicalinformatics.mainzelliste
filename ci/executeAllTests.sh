#!/usr/bin/env bash

source ./testHelpers.sh

iterateDirAndExecuteFunction  newman_tests/ executeNewmanTest
exit $EXIST_CODE
