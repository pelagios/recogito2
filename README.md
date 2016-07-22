# Recogito 2

Home of Recogito v2.0 - a Linked Data annotation tool for texts and images, developed by
[Pelagios](http://commons.pelagios.org). Track our progress on
[Waffle.io](http://waffle.io/pelagios/recogito2).

## Prerequisites

* Java 8 JDK
* [Play Framework v2.5.0](https://www.playframework.com/download)
* PostgreSQL DB
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
* Type `activator run` to start the application in development mode.
* Point your browser to [http://localhost:9000](http://localhost:9000)
* Recogito automatically creates a single user with administrator privileges with username
  'recogito' and password 'recogito'. Be sure to remove this user - or at least change the
  password - for production use!
* To generate an Eclipse project, type `activator eclipse`.

## Importing gazetteers

In order to use geo-tagging, you need to manually import one or several gazetteers. A dump file of
the gazetteer from the [Digital Atlas of the Roman Empire](http://dare.ht.lu.se/) (in [Pelagios
Gazetteer Interconnection Format](http://github.com/pelagios/pelagios-cookbook/wiki/Pelagios-Gazetteer-Interconnection-Format))
is included in the `/gazetteers` folder.

You can import the gazetteer by going to the gazetteer administration page at
[http://localhost:9000/admin/gazetteers](http://localhost:9000/admin/gazetteers). Note that you
need to be logged in with a user that has administrator privileges (e.g. the default 'recogito').

__Note:__ the admin pages are work in progress. Don't expect any fancy functionality or styling
there yet! Importing gazetteers can take a while, and there is no progress display yet. Bear with
us. Meanwhile, you can check progress through Recogito's JSON Place API:

[http://localhost:9000/api/places/search?q=*&pretty=true](http://localhost:9000/api/places/search?q=*&pretty=true)

## Running in production

* To test production mode before deploying, type `activator testProd`
* For full production deployment, refer to the current [Play Framework
  docs](https://www.playframework.com/documentation/2.5.x/Production)
* Be sure to set a random application secret in `conf/application.conf`. Play includes a utility
  to generate one for you - type `activator playGenerateSecret`.
* Production deployment requires and ElasticSearch installation. (Recogito will automatically
  create an embedded ElasticSearch index if cannot find a running cluster. However we strictly
  recommend this only for development purposes.)
* Last but not least: another reminder to remove the default 'recogito' admin user - or at least
  change its password!

## License

Recogito v2.0 is licensed under the terms of the
[Apache 2.0 license](https://github.com/pelagios/recogito2/blob/master/LICENSE).
