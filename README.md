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

* ElasticSearch gazetteer framework
  * While updating, even unchanged places are re-written to the index. Might not have such a
    huge performance impact, but investigate.
  * Place updates currently don't make use of optimistic locking. Do we need that? Concurrency-
    related conflicts during parallel import are possible. But should normally be either
    recognizable by failed clean-up deletes; or wouldn't matter since the later update will
    clean everything up. Scenario is different for PlaceLinks however!
  * Places currently don't include added_at and last_updated timestamps
  * Gazetteer records currently don't include added_at and last_updated timestamps
  * The place schema doesn't yet include hierarchical relations. Q: how do we deal with
    incoherent hierarchy relations reported by different gazetteers?
