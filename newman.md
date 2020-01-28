# Mainzelliste Postman/Newman tests

Postman is a tool that can be used for HTTP API testing. The Mainzelliste project uses Postman collection to verify the correct HTTP API functionalities. Newman is a command-line Collection Runner for Postman. 

## Automated newman tests on bitbucket.org

On each commit, all the collections are executed automatically on bitbucket.org. 

[bitbucket-pipelines.yml](./bitbucket-pipelines.yml): The bitbucket-pipelines.yml file defines your pipelines builds configuration. Via [ci/executeAllNewmanContainerTests.sh](./ci/executeAllNewmanContainerTests.sh) it is iterating over each test case. For each test case a docker environment is started using the docker-compose.test.yml file.

## Execute newman tests on your local machine

* Execute all tests [ci/executeAllNewmanContainerTests.sh](.ci/executeAllNewmanContainerTests.sh): For each test in [ci/newman_tests](.ci/newman_tests) Mainzelliste, Postgres and Newman are started and executed.
* Execute a single test [ci/runANewmanTest.sh](.ci/runANewmanTest.sh) $testName, e.g. ./runANewmanTest.sh ./newman_tests/mainzelliste_AddPatient_ReadPatient: Mainzelliste, Postgres and Newmann are started and executed the specific test. 


## Further information

* docker-compose.newman.yml: The Docker environment is defined here.
*  ci/  The relevant test data is stored in this directory. Each test case is defined in a separate Postman Collection. A Mainzelliste configuration file is provided for each test case. The configuration file has the same file name as the Postman Collection (including the file extension of the collection).
*  newman_environment_variables/:  The Newman Environment variables are stored here. Currently there is one file for all test cases.
*  newman_mainzelliste_configs/:  This folder contains the Mainzelliste configuration files associated with the test cases. If there is no configuration file for a test case, a default configuration is chosen.
*  newman_tests/: The test cases are saved here. A collection contains exactly one test case.
*  test_data/: Here are some mockup test data. There is also a collection file that contains some relevant API requests. To ensure that the test cases are standardized, you should copy the required requests from this collection. If a request does not yet exist, you should add it to the collection.
*  test_results/: Here the Newman results are stored in HTML and XML format. This allows a pretty presentation of the tests.