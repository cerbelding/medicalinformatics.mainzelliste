#!/usr/bin/env bash

source ./testHelpers.sh

iterateDirAndExecuteFunction  newman_tests/ executeNewmanTest
exit $EXIT_CODE
