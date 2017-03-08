name := """recogito2"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature")

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  filters,

  "com.nrinaudo" %% "kantan.csv" % "0.1.15",
  "com.sksamuel.elastic4s" %% "elastic4s-streams" % "2.4.0",
  "com.typesafe.akka" %% "akka-contrib" % "2.4.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.2",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "com.vividsolutions" % "jts" % "1.13",
  "commons-io" % "commons-io" % "2.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2" classifier "models",
  "eu.bitwalker" % "UserAgentUtils" % "1.20",
  "jp.t2v" %% "play2-auth" % "0.14.1",
  "org.apache.jena" % "jena-arq" % "3.1.0",
  "org.geotools" % "gt-geojson" % "14.3",
  "org.jooq" % "jooq" % "3.7.2",
  "org.jooq" % "jooq-codegen-maven" % "3.7.2",
  "org.jooq" % "jooq-meta" % "3.7.2",
  "org.postgresql" % "postgresql" % "9.4.1208.jre7",
  "org.webjars" %% "webjars-play" % "2.5.0",

  // Scalagios core + transient dependencies
  "org.pelagios" % "scalagios-core" % "2.0.1" from "https://github.com/pelagios/scalagios/releases/download/v2.0.1/scalagios-core.jar",
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",

  // Recogito plugin API
  "org.pelagios" % "recogito-plugin-sdk" % "0.0.2" from "https://github.com/pelagios/recogito2-plugin-sdk/releases/download/v0.0.2/recogito-plugin-sdk-0.0.2.jar",

  "org.webjars" % "dropzone" % "4.2.0",
  "org.webjars" % "jquery" % "1.12.0",
  "org.webjars" % "jquery-ui" % "1.11.4",
  "org.webjars" % "leaflet" % "0.7.7",
  "org.webjars" % "numeral-js" % "1.5.3-1",
  "org.webjars" % "openlayers" % "3.13.0",
  "org.webjars" % "papa-parse" % "4.1.0-1",
  "org.webjars" % "requirejs" % "2.1.22",
  "org.webjars" % "typeaheadjs" % "0.11.1",
  "org.webjars" % "velocity" % "1.1.0",
  "org.webjars.bower" % "js-grid" % "1.4.1",
  "org.webjars.bower" % "plotly.js" % "1.12.0",
  "org.webjars.bower" % "rangy" % "1.3.0",
  "org.webjars.bower" % "timeago" % "1.4.1",
  "org.webjars.npm" % "chartist" % "0.9.8",

  specs2 % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.2" % "test"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

pipelineStages := Seq(rjs, digest, gzip)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

unmanagedClasspath in Runtime <++= (baseDirectory) map { base =>
  Attributed.blankSeq((file("plugins/") ** "*.jar").get)
}

val generateJOOQ = taskKey[Seq[File]]("Generate JooQ classes")

val generateJOOQTask = (sourceManaged, fullClasspath in Compile, runner in Compile, streams) map { (src, cp, r, s) =>
  toError(r.run("org.jooq.util.GenerationTool", cp.files, Array("conf/db.conf.xml"), s.log))
  (src ** "*.scala").get
}

generateJOOQ <<= generateJOOQTask
