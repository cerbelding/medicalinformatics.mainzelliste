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

### Using user preconfiguration

```shell
docker-compose -f docker-compose.yml -f docker-compose.user.yml up
```

### Environment Variables

Here is a list of all currently supported environment variables:

|Variable Name|Default Value|Description|
|-------------|-------------|-----------|
|`ML_DB_DRIVER`|`org.postgresql.Driver`|The driver used for db connection. Can be changed if you want to use mysql|
|`ML_DB_TYPE`|`postgresql`|Can be changed to mysql|
|`ML_DB_HOST`|`db`|Host address where the database is deployed, e.g. localhost|
|`ML_DB_PORT`|`5432`|Port of the database|
|`ML_DB_NAME`|`mainzelliste`|Name of the database|
|`ML_DB_USER`|`mainzelliste`|Username for a user with permissions on the database|
|`ML_DB_PASS`|(none, please define)|Password for a user who has permissions on the database. Can also be defined as Docker Secret `ML_DB_PASS_FILE`|
|`ML_API_KEY`|(none, please define)|The API Key for Mainzelliste API (MDAT server 0). Also also be defined as Docker Secret `ML_API_KEY_FILE`|
|`ML_ALLOWEDREMOTEADDRESSES`|`0.0.0.0/0`|Accepted origin addresses (IPv4 and/or IPv6) or address ranges in CIDR notation (IPv4 only) for MDAT server 0|
|`ML_REVERSEPROXY_FQDN`|(none, please define)|Fully-qualified domain name to be used for access to this Mainzelliste, e.g. `patientlist.example.org`|
|`ML_REVERSEPROXY_PORT`|80 or 443 according to `ML_REVERSEPROXY_SSL`|The corresponding port number|
|`ML_REVERSEPROXY_SSL`|`false`|Set to `true` if Mainzelliste is accessed via SSL/TLS; `false` otherwise|
|`DEBUG`|`false`|Set to `true` if you want to open a port for remote debugging. You will need to forward the port 1099 with dockers port configuration.|

Please note that Mainzelliste 1.9 will receive a generic Docker configuration interface. Variable names will change.

### Supported Secrets

|Secret Name|Environment Variable|
|-----------|--------------------|
|mainzellisteDbName|ML_DB_NAME|
|mainzellisteDbUser|ML_DB_USER|
|mainzellisteDbPassword|ML_DB_PASS|
|mainzellisteApiKey|ML_API_KEY|

Then using docker-compose you will need to pass the secrets from a file on the filesystem using the syntax from [*docker-compose.user.yml*](./docker-compose.user.yml).  
Using docker in swarm mode will allow you to pass secrets with ```external: true``` and then creating the corresponding secret with following command:
```shell
echo "<your_secret>" | docker secret create <secret_name> -
```

### Passing a mainzellist config file

It is also possible to pass an own mainzelliste configuration file to the container. In this case  *ML_DB_NAME*, *ML_DB_USER*, *ML_DB_PASS*, *ML_DB_TYPE*, *ML_DB_DRIVER*, *ML_DB_HOST*, *ML_DB_PORT*, *ML_API_KEY* and *ML_ALLOWEDREMOTEADDRESSES* are ignored.  
The config needs to be passed with a docker secret named ***mainzellisteConfig***. An example for this is available in [*docker-compose.user.yml*](./docker-compose.user.yml).  
Then using docker swarm, it is also possible to pass this without actually relying on a local file on your system. You can pass the entire file to a docker secret with:
```shell
cat "</path/to/your/mainzelliste.conf>" | docker secret create <secret_name> -
```
After creating the secret, you can remove the file from filesystem.

## For Developers
### Building the Mainzelliste Image
All Necessary files to build the mainzelliste container are included in mainzelliste repository. You can build your own container from repository.
For this the only command needed is:
```shell
docker-compose -f docker-compose.yml -f docker-compose.dev.yml build
```
If you don't have access to docker-compose the command docker build can also be used. e.g.:
```shell
docker build -t medicalinformatics/mainzelliste:rc-1.8 .
```
You may need to specify a proxy then building the image. We support the build parameter ***$http_proxy***.

Then using docker-compose you will need to overwrite the default docker-compose with following *docker-compose.override.yml*:
```yml
version: "3"
services:
  mainzelliste:
    build:
      context: .
      args:
        - http_proxy=<your_proxy_address>
```
Then using docker build you will need the *--build-arg* parameter:
```shell
docker build --build-arg http_proxy=<your_proxy_address> -t medicalinformatics/mainzelliste:rc-1.8 .
```
### Use Docker to deploy test data base
It is also possible to deploy a database for testing mainzelliste by using:
```shell
docker-compose up db
```
To access the database from your host system you will need to add a *docker-compose.override.yml* or modify the existing one:
```yml
version: "3"
services:
  db:
    ports:
      - 5432:5432
```
With the default configuration the database should now be available on port 5432 on your docker host system and it should be possible to  
connect using the Values from the *ML_DB_NAME*, *ML_DB_USER* and *ML_DB_PASSWORD* environment variables.

### Execute Mainzelliste tests

```docker-compose -f docker-compose.test.yml up```
