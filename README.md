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

## Upcoming TODOs & Questions

* ElasticSearch gazetteer framework
  * Max number of SHOULD clauses is currently not checked - we should do this in the PlaceStore
    class to be sure weird gazetteer records don't break the import
  * Implement PlaceLink rewriting after gazetteer updates (using optimistic locking)
  * The place schema doesn't yet include is_part_of relations. Q: how do we deal with
    incoherent hierarchy relations reported by different gazetteers?
* Fix the various compiler warnings introduced with the Play 2.5 upgrade
* Deleting a doc currently doesn't delete its annotations
* Q: do we want people to be able to upload their own gazetteers, for exclusive use within a team.
  How would we model in ES? One separate index for user gazetteers + owner field + multi-index
  query? No conflation with "normal" gazetteers?
* Should we support ingesting URI correspondence lists into the gazetteer? Since those would be
  close/exactMatches not linked to any gazetteer record, it would mean moving matches to the level
  of the place itself. (May make sense in terms of query performance anyway)
* What about documents consisting of a mix of text and image "layers". E.g. scanned image in latin,
  scanned image in other language, transcription (in differnt languages) - with each layer
  "linking through" to the others.
  * General Q about "linking through": should this be modeled as a relation between annotations?
    Or as another type of annotation body ("hyperlink")? (In OA, a link would be modeled as an
    additional annotation that has two annotations as target I guess? Prob. more complicated
    than it needs to be. In our case: e.g. a body of type HYPERLINK with a URI? (Disadvantage:
    we don't know if its an internal link in the same doc or not, unless we parse the URI.)
