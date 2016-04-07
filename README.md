# Recogito 2

Future home of Recogito v2.0.

## Prerequisites

* Java 8 JDK
* [Play Framework v2.5.0](https://www.playframework.com/download)
* To use image annotation, you need to have the [vips](http://www.vips.ecs.soton.ac.uk/) image
  processing system installed. If vips is not available on the command line, Recogito is set to
  reject uploaded images as 'unsupported content'. (Note: on Ubuntu, 'libvips-tools' is the
  package you need.)

## Installation

* Create a copy of the file `conf/application.conf.template` and name it `conf/application.conf`.
  Make any environment-specific changes there. (For the most part, the defaults should be fine.)
* Type `activator run` to start the application in development mode.
* Point your browser to [http://localhost:9000](http://localhost:9000)
* To generate an Eclipse project, type `activator eclipse`.

## Database Configuration

If you stick with the default settings, Recogito will automatically create an SQLite database and
an embedded ElasticSearch index, so there's nothing you need to do. However, for production use
we recommend a PostgreSQL DB and a separate ElasticSearch installation. Modify your settings in
`conf/application.conf` accordingly.

## Running in production

* To test production mode before deploying, type `activator testProd`
* For full production deployment, refer to the current [Play Framework
  docs](https://www.playframework.com/documentation/2.5.x/Production)
* Be sure to set a random application secret in `conf/application.conf`. Play includes a utility
  to generate one for you - type `activator playGenerateSecret`.

## Upcoming TODOs

* Clean up & refactor the `models` package
* Clean up & organize the `test/resources` folder
* Refactor some of the bulkier unit tests in the `models.place` package
* ElasticSearch gazetteer framework
  * Max number of SHOULD clauses is currently not checked - we should do this in the PlaceStore
    class to be sure weird gazetteer records don't break the import
  * Implement PlaceLink rewriting after gazetteer updates (using optimistic locking)
  * The place schema doesn't yet include is_part_of relations. Q: how do we deal with
    incoherent hierarchy relations reported by different gazetteers?
* Check the build file: do we need 'ws' dependency? Remove Groovy dependency in case we don't use
  ElasticSearch scripting after all
* Accordingly, clean up elasticsearch.yml in case we don't use scripting
* Fix the various compiler warnings introduced with the Play 2.5 upgrade
* Upgrade to latest ElasticSearch version
* Deleting a doc currently doesn't delete its annotations
