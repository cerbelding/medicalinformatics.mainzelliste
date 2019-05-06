# Mainzelliste Docker

## Introduction
Mainzelliste is a widely used component for pseudonimization and record linkage. In order to set up a Mainzelliste yourself, you will need to configure a tomcat server and supply a database. To make this steps easier, we offer a docker image which devilers a preconfigured tomcat with mainzelliste installed.

## Quick start
To deploy Mainzelliste, a database is needed. You can either provide this on your docker host machine or as a docker container. For the approach with a docker container we provide a [docker-compose file](./docker-compose.yml) which can be used to either deploy with [_docker-compose_](https://docs.docker.com/compose/overview/) or with an ochestrator like [_docker stack_](https://docs.docker.com/engine/swarm/stack-deploy/).

### Deploy with docker compose

To deploy with docker-compose just navigate to the directory there _docker-compose file_ file lies and execute following line:
```shell
docker-compose up
```
To stop the deployed containers use:
```shell
docker-compose down
```

### Deploy with docker stack

To initialize a stack, your docker engine must be set to swarm mode. This can be done with following command:
```shell
docker swarm init
```
After this you should receive a notification that your machine is now a node of a docker swarm. Now you can start the mainzelliste stack with the following command:
```shell
docker stack deploy --compose-file /path/to/docker-compose.yml mainzelliste
```

## Configuration

Here a list of all Environment Variables:

|Variable Name|Default Value|Description|
|-------------|-------------|-----------|
|ML_DB_DRIVER|org.postgresql.Driver|The driver used for db connection. Can be changed if you want to use mysql|
|ML_DB_TYPE|postgresql|Can be changed to mysql|
|ML_DB_HOST|db|Host address there the database is deployed, e.g. localhost|
|ML_DB_PORT|5432|Port of the database|
|ML_DB_NAME|mainzelliste|Name of the database|
|ML_DB_USER|mainzelliste|Username for a user which has login rights on mainzelliste db|
|ML_DB_PASS|mainzelliste|Password for a user which has login rights on mainzelliste db. Also avaiable as Secret: ML_DB_PASS_FILE|
|ML_API_KEY|1234Test|The Api Key for Mainzelliste Api.|

It is also possible to pass own mainzelliste configuration to the container. In this case the environment variables are not useable.  
In this case you need to initialize a [docker config](https://docs.docker.com/engine/reference/commandline/config/). The path to the config file needs to be passed to mainzelliste with the Environment Variable ML_CONFIG_FILE. Config Files are available in the container at root. The files have the name of the docker config on host.  

**e.g.**
```shell
cat ./config/mainzelliste.conf | docker config create mainzellisteConfig - 
```
ML_CONFIG_FILE=/mainzellisteConfig

## For Developers

All Necessary files to build the mainzelliste container are included in mainzelliste repository. You can build your own container from repository.
It is also possible to deploy a database for testing mainzelliste by using:
```shell
docker-compose up db
```