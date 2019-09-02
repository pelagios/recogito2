name := """recogito2"""

version := "2.2"

scalaVersion := "2.11.11"

scalaVersion in ThisBuild := "2.11.11"

publishMavenStyle in ThisBuild := false

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions ++= Seq("-feature")

updateOptions := updateOptions.value.withGigahorse(false)

PlayKeys.devSettings += "play.server.akka.requestTimeout" -> "600s"

resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "GeoTools" at "http://download.osgeo.org/webdav/geotools/",
  "Boundless" at "http://repo.boundlessgeo.com/main/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/",
  "Atlassian Maven" at "https://maven.atlassian.com/content/repositories/atlassian-public/",
  Resolver.jcenterRepo 
)

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  filters,
  guice,

  "com.iheart" %% "ficus" % "1.4.3",

  "com.mohiva" %% "play-silhouette" % "5.0.7",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.7",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.7",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.7",
  "com.mohiva" %% "play-silhouette-testkit" % "5.0.7" % Test,
  "com.mohiva" %% "play-silhouette-cas" % "5.0.7",

  "com.nrinaudo" %% "kantan.csv" % "0.3.1",
  "com.nrinaudo" %% "kantan.csv-commons" % "0.3.1",

  "com.sksamuel.elastic4s" %% "elastic4s-core" % "5.6.1",
  "com.sksamuel.elastic4s" %% "elastic4s-tcp" % "5.6.1",

  "com.typesafe.akka" %% "akka-testkit" % "2.5.8" % Test,

  "com.typesafe.play" %% "play-iteratees" % "2.6.1",
  "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1",
  "com.typesafe.play" %% "play-mailer" % "6.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.1",

  "com.vividsolutions" % "jts" % "1.13",

  "commons-io" % "commons-io" % "2.4",

  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.1" classifier "models",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.1" classifier "models-english",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.1" classifier "models-french",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.1" classifier "models-german",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.1" classifier "models-spanish",

  "eu.bitwalker" % "UserAgentUtils" % "1.20",

  "net.codingwell" %% "scala-guice" % "4.1.1",

  "org.apache.jena" % "jena-arq" % "3.1.0",

  "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.8.2",

  "org.geotools" % "gt-geojson" % "14.3",

  "org.jooq" % "jooq" % "3.7.2",
  "org.jooq" % "jooq-codegen-maven" % "3.7.2",
  "org.jooq" % "jooq-meta" % "3.7.2",

  "org.jooq" % "joox" % "1.5.0",

  "org.postgresql" % "postgresql" % "9.4.1208.jre7",

  "org.webjars" %% "webjars-play" % "2.6.2",

  // Scalagios core + transient dependencies
  "org.pelagios" % "scalagios-core" % "2.0.1" from "https://github.com/pelagios/scalagios/releases/download/v2.0.1/scalagios-core.jar",
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",

  // Recogito plugin API
  "org.pelagios" % "recogito-plugin-sdk" % "1.0" from "https://github.com/pelagios/recogito2-plugin-sdk/releases/download/v1.0/recogito-plugin-sdk-1.0.jar",

  "org.webjars" % "dropzone" % "4.2.0",
  "org.webjars" % "jquery" % "1.12.0",
  "org.webjars" % "jquery-ui" % "1.11.4",
  "org.webjars" % "leaflet" % "1.3.1",
  "org.webjars" % "numeral-js" % "1.5.3-1",
  "org.webjars" % "papa-parse" % "4.1.0-1",
  "org.webjars" % "requirejs" % "2.1.22",
  "org.webjars" % "slick" % "1.6.0",
  "org.webjars" % "swagger-ui" % "2.2.0",
  "org.webjars" % "typeaheadjs" % "0.11.1",
  "org.webjars" % "velocity" % "1.1.0",
  "org.webjars.npm" % "bootstrap-colorpicker" % "2.5.2" intransitive(),
  "org.webjars.npm" % "openlayers" % "4.6.4" intransitive(),
  "org.webjars.bower" % "blazy" % "1.8.0",
  "org.webjars.bower" % "cookieconsent" % "3.0.6",
  "org.webjars.bower" % "marked" % "0.3.6",
  "org.webjars.bower" % "js-grid" % "1.4.1",
  "org.webjars.bower" % "plotly.js" % "1.12.0",
  "org.webjars.bower" % "rangy" % "1.3.0",
  "org.webjars.bower" % "timeago" % "1.4.1",
  "org.webjars.npm" % "chartist" % "0.9.8",

  specs2 % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "xmlunit" % "xmlunit" % "1.6" % Test
)

PlayKeys.playRunHooks += Webpack(baseDirectory.value)

routesGenerator := InjectedRoutesGenerator

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

excludeFilter in (Assets, JshintKeys.jshint) := "*.js"

pipelineStages := Seq(rjs, digest, gzip)

unmanagedJars in Runtime ++= Attributed.blankSeq((file("plugins/") ** "*.jar").get)

val generateJOOQ = taskKey[Seq[File]]("Generate JooQ classes")
generateJOOQ := {
  val src = sourceManaged.value
  val cp = (fullClasspath in Compile).value
  val r = (runner in Compile).value
  val s = streams.value
  r.run("org.jooq.util.GenerationTool", cp.files, Array("conf/db.conf.xml"), s.log).failed foreach (sys error _.getMessage)
  (src ** "*.scala").get
}
