## Docker images for Recogito

The Dockerfile is a specification to set up a static Docker image, which after
building will contain everything needed to run the core Recogito system.
There are two main ways to use the Dockerfile provided here:

### The dev image

By following the instructions in the Dockerfile, you can stop the build
prematurely and have a clean and reproducible dev environment to apply and
test minimal change sets. Here, we do setup and a clean git clone to
`/usr/share/recogito` and compile it for running immediately.

The `application.conf` file is a "reasonable" default configuration, though
certain settings are _highly_ recommended to be changed - in particular the
"application secret". When running the docker image in any sort of
production-like environment, you would thus likely use the Docker
functionality to make `/usr/share/recogito/conf/application.conf` in the
image refer to a local configuration file, as in 

```docker run -v application.conf:/usr/share/recogito/conf/application.conf:ro recogito-image```

or the appropriate options in a `docker-compose.yml` file.

The directory used for persistent storage (`/usr/share/recogito/uploads` in the
dev config) should also be made persistent, either using volumes or bind
mounts.

### The prod image

Building with the Dockerfile as-is will first go through a build in the dev
environment, and then proceed to the next stage, starting over from a clean
OpenJDK Docker image, adding libvips and its dependencies for image
processing, and then finally copying the finished distribution from the
build environment, placing it in `/opt/recogito`. The configuration and data
directories obviously are placed in the corresponding paths under `/opt` as
well.


### Complete Docker stack

In `docker-compose.yml`, we define the services involved, and set up, as far as
possible, the requisite settings and persistent storage. We assume an external
database server (PostgreSQL), but run the Recogito and Elasticsearch images in
Docker, with persistent storage using Docker volumes.







