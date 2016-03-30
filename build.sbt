name := """recogito2"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature")

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,

  "com.sksamuel.elastic4s" %% "elastic4s-streams" % "1.7.4",
  "com.typesafe.akka" %% "akka-contrib" % "2.4.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.2",
  "com.vividsolutions" % "jts" % "1.13",
  "commons-io" % "commons-io" % "2.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models",
  "jp.t2v" %% "play2-auth" % "0.14.1",
  "org.geotools" % "gt-geojson" % "14.3",
  "org.jooq" % "jooq" % "3.7.2",
  "org.jooq" % "jooq-codegen-maven" % "3.7.2",
  "org.jooq" % "jooq-meta" % "3.7.2",
  "org.xerial" % "sqlite-jdbc" % "3.8.11.2",

  // Scalagios core + transient dependencies
  "org.pelagios" % "scalagios-core" % "2.0.0" from "https://github.com/pelagios/scalagios/releases/download/v2.0/scalagios-core.jar",
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",

  "org.webjars" % "dropzone" % "4.2.0",
  "org.webjars" % "jquery" % "1.12.0",
  "org.webjars" % "jquery-ui" % "1.11.4",
  "org.webjars" % "openlayers" % "3.13.0",
  "org.webjars" % "requirejs" % "2.1.22",
  "org.webjars.bower" % "rangy" % "1.3.0",

  specs2 % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.2" % "test"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

val generateJOOQ = taskKey[Seq[File]]("Generate JooQ classes")

val generateJOOQTask = (sourceManaged, fullClasspath in Compile, runner in Compile, streams) map { (src, cp, r, s) =>
  toError(r.run("org.jooq.util.GenerationTool", cp.files, Array("conf/db.conf.xml"), s.log))
  (src ** "*.scala").get
}

generateJOOQ <<= generateJOOQTask
