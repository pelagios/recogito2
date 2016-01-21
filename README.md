# Recogito 2

Future home of Recogito v2.0

## Installation

* Prerequisites: Java 8 JDK and [Play Framework v2.4.6](https://www.playframework.com/download)
* Create a copy of the file `conf/application.conf.template` named `conf/application.conf` and
  make any environment-specific there
* Type `activator run` to start the application in development mode.
* Point your browser to [http://localhost:9000](http://localhost:9000)
* To generate an Eclipse project, type `activator eclipse`

## Database Configuration

If you stick with the default settings, Recogito will automatically create an SQLite database, so there's
nothing you need to do. However, we recommend a PostgreSQL DB for production use. [TODO]
