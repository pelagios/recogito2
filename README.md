# Recogito 2

Future home of Recogito v2.0

## Prerequisites

* Java 8 JDK
* [Play Framework v2.5.0](https://www.playframework.com/download) installed on your machine
* Image upload requires that the [vips](http://www.vips.ecs.soton.ac.uk/) image processing system
  is installed and available through the command-line. If vips is not available on your system,
  Recogito will reject uploaded images as 'unsupported content'.

## Installation


* Create a copy of the file `conf/application.conf.template` named `conf/application.conf` and
  make any environment-specific changes there. (Normally, the defaults should be fine.)
* Type `activator run` to start the application in development mode.
* Point your browser to [http://localhost:9000](http://localhost:9000)
* To generate an Eclipse project, type `activator eclipse`.

## Database Configuration

If you stick with the default settings, Recogito will automatically create an SQLite database and
an embedded ElasticSearch index, so there's nothing you need to do. However, for production use
we recommend a PostgreSQL DB and a separate ElasticSearch installation.

## Running in production

* To test production mode before deploying, type `activator testProd`
* For full production deployment, refer to the current [Play Framework
  docs](https://www.playframework.com/documentation/2.5.x/Production)
* Be sure to set a random application secret in `conf/application.conf`. Play includes a utility
  to generate one for you - type `activator playGenerateSecret`.

## Current TODOs

* In UserService, we should use Play's cache to buffer user lookups to the DB.
