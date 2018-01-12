# Recogito

[![Project Stats](https://www.openhub.net/p/recogito2/widgets/project_thin_badge.gif)](https://www.openhub.net/p/recogito2)

Home of [Recogito](http://recogito.pelagios.org) - a Linked Data annotation tool for texts and
images, developed by [Pelagios](http://commons.pelagios.org). Track our progress on
[Waffle.io](http://waffle.io/pelagios/recogito2).

## Prerequisites

* Java 8 JDK
* [SBT](http://www.scala-sbt.org/) (version 1.0.x)
* PostgreSQL DB (tested with version 9.3)
* __Important: as of version 2.2, Recogito requires an installation of
  [ElasticSearch v5.6.5](https://www.elastic.co/downloads/past-releases/elasticsearch-5-6-5).
  Automatic fallback to an embedded ElasticSearch index is no longer supported.__
* To use image annotation, you need to have the [vips](http://www.vips.ecs.soton.ac.uk/) image
  processing system installed. If vips is not available on the command line, Recogito is set to
  reject uploaded images as 'unsupported content'. (Note: on Ubuntu, 'libvips-tools' is the
  package you need.)

## Installation

* Clone this repository
* Create a copy of the file `conf/application.conf.template` and name it `conf/application.conf`.
  Make any environment-specific changes there. (For the most part, the defaults should be fine.)
* Create a database named 'recogito' on your Postgres DB server. (If you want a different name, adjust
  the settings in your `conf/application.conf` accordingly.)
* Type `sbt run` to start the application in development mode.
* Point your browser to [http://localhost:9000](http://localhost:9000)
* Recogito automatically creates a single user with administrator privileges with username
  'recogito' and password 'recogito'. Be sure to remove this user - or at least change the
  password - for production use!
* To generate an Eclipse project, type `sbt eclipse`.

## Importing gazetteers

* A gazetteer dump file from the [Digital Atlas of the Roman Empire](http://dare.ht.lu.se/) (in [Pelagios
  Gazetteer Interconnection Format](http://github.com/pelagios/pelagios-cookbook/wiki/Pelagios-Gazetteer-Interconnection-Format))
  is included in the `/gazetteers` folder.
* You can import this gazetteer to Recogito on the Gazetteer Administration page at
  [http://localhost:9000/admin/gazetteers](http://localhost:9000/admin/gazetteers). Note that you
  need to be logged in with a user that has administrator privileges (e.g. the default 'recogito').
* __WARNING__ the admin pages are work in progress. Don't expect any fancy functionality or styling
  there yet! Importing gazetteers can take a while, and there is no progress display yet. Bear with
  us. You can track progress through Recogito's JSON Place API at [http://localhost:9000/api/place/search?q=*&pretty=true](http://localhost:9000/api/place/search?q=*&pretty=true)
* It's not possible to upload controlled vocabularies for persons, periods or events yet.

## Running in production

* To test production mode before deploying, type `sbt runProd`
* To change to a different port (than default 9000), type `sbt "runProd -Dhttp.port=9876"`
* For full production deployment, refer to the current [Play Framework
  docs](https://www.playframework.com/documentation/2.6.x/Production)
* Be sure to set a random application secret in `conf/application.conf`. Play includes a utility
  to generate one for you - type `sbt playGenerateSecret`.
* Last but not least: another reminder to remove the default 'recogito' admin user - or at least
  change its password!

## License

Recogito is licensed under the terms of the
[Apache 2.0 license](https://github.com/pelagios/recogito2/blob/master/LICENSE).
