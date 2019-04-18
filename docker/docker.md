# Mainzelliste Docker

This is a version of Mainzelliste which can be started using docker-compose command. 

## Start Mainzelliste Docker

To start the docker compose just switch to the root of this repository and type:
```
docker-compose up
```

## Stop Mainzelliste Docker

Stopping the docker-compose works with a similiar command:
```
docker-compose down
```

## Information

Currently docker starts a postgres database on which user mainzelliste and the mainzelliste db are created. We didn't use the environment variables provided by postgres docker container because mainzelliste user shouldn't be a super user on postgres.

The data stored into the Mainzelliste Docker at the moment, will be deleted after the docker compose is shut down. To change this it is possible to create a volume which copies a directory inside a docker container to the host system.