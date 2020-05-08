#Mainzeliste Tests
In order to guarantee the quality of the Mainzelliste, integration tests are provided to developers and testers.

##Quickstart
To test the Mainzelliste the following commands can be used.
###Run all Tests
To execute all tests in the newman_tests folder execute following line:

```shell
/executeAllTests.sh
```

###Run one Test or several Subtests
To execute a single test or subtests located in a root folder . e.g. to test specific changes execute following line:

```shell
./executeSubTests.sh <pathToFolderOrFile>
```

###Create a Test Environment
To develop tests, a test environment can be created in which you can test your test cases. 

If a new test environment is created which is not yet covered in the test cases, a new empty Postman Collection must be created.
If a special configuration is required, it must be created with the given naming conventions

To create a test environment execute following line:

```shell
./initTestEnvironment.sh <pathToFile>
```
##How to write a Test
###Best Practices
Every Testcase is an isolated Postman Collection with its own Mainzelliste configuration and Test data.

###Writing new Testcases
1. Create Collection in a appropriated folder or subfolder of **newman_tests/**
    1. If necessary add custom config file otherwise the default config will be used
1. Write Test case (e.g with the help of init test environment)
   1. If the request already exists use the  [request from request-Collection](./test_data/mainzelliste_z-test-requests_Collection.postman_collection.json)
   1. If the request does not exist
        1. Write new request
        2. Add the request to [request-Collection](./test_data/mainzelliste_z-test-requests_Collection.postman_collection.json)
1. If necessary add custom data file otherwise the default data will be used
##File structure
[**/newman_tests/:**](./newman_tests)
> All Testcases are stored here.

[**/newman_mainzelliste_configs/:**](./newman_mainzelliste_configs)
> Every Test can be started with its own Mainzzelliste configuration.  <br/>
>To link a configuration to its test, the following naming conventions must be followed:
>   * The name of the configuration file must be the same as the test name (the extension of the test is included)
>   * The extension of the configuration file is **.conf**
>
>  _Example_: 
>   * Config: `mainzelliste_AddPatient_ReadPatient.postman_collection.json.conf` refers to 
>  * Testcasae: `mainzelliste_AddPatient_ReadPatient.postman_collection.json`
>  
>
> _Default Data:_ [**newman_mainzelliste_configs/default/mainzelliste_default.conf**](./newman_mainzelliste_configs/default/mainzelliste_default.conf)
> 
>If no test-specific configuration exists, the default configuration is used



[**/test_data/:**](./test_data)
> Each test case can be fed with specific test data. <br/>
>To link a Dataset to a Testcase following naming conventions must be followed:
>   * The name of the test file must be the same as the test name (the extension of the test is included)
>   * The extension of the configuration file is **.json or .csv**
>   
>  _Example_: 
>   * Data: `mainzelliste_getAllPatiens_with_idType.postman_collection.json.json` refers to 
>   * Testcase: `mainzelliste_getAllPatiens_with_idType.postman_collection.json`
>
> _Default Data:_ [**test_data/5000Idat.json**](./test_data/5000Idat.json)
>
>If no test-specific data exists, the default data is used


[**/test_data/mainzelliste_z-test-requests_Collection.postman_collection.json:**](./test_data/mainzelliste_z-test-requests_Collection.postman_collection.json)
> The collection contains predefined requests. Due to standardization, please use the requests from this collection. 
>If new requests are created, they should be added to the collection.
>Keep this Collection up to date.


[**/newman_environment_variables/:** ](./newman_environment_variables)
>All Newman environment variables are stored here. Currently only one file is supported.
>Variables should be store in postman's collectionVariables API, therefore its not necessary to add environment variables.


[**/test_results/:**](./test_results)
> After executing one or more tests, the output of the tests will be generated here.
>
>Two types of documents are stored:
>   * **.xml**: A xml-format makes possible to interprate the tests as junit tests
>   * **.html**: For visual inspection
 

