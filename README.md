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
* Type `activator run` to start the application in development mode.
* Point your browser to [http://localhost:9000](http://localhost:9000)
* To generate an Eclipse project, type `activator eclipse`.

## Running in production

* To test production mode before deploying, type `activator testProd`
* For full production deployment, refer to the current [Play Framework
  docs](https://www.playframework.com/documentation/2.5.x/Production)
* Be sure to set a random application secret in `conf/application.conf`. Play includes a utility
  to generate one for you - type `activator playGenerateSecret`.
* Production deployment requires and ElasticSearch installation. (Recogito will automatically
  create an embedded ElasticSearch index if cannot find a running cluster. However we strictly
  recommend this only for development purposes.)

## License

Recogito v2.0 is licensed under the terms of the
[Apache 2.0 license](https://github.com/pelagios/recogito2/blob/master/LICENSE).
