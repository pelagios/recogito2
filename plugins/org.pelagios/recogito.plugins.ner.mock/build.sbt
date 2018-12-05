name := "recogito-plugin-ner-mock"

organization := "org.pelagios"

version := "0.1"

scalaVersion := "2.11.11"

// Do not append Scala versions to the generated artifacts
crossPaths := false

mappings in (Compile, packageBin) += {
  (baseDirectory.value / "provider.conf") -> "META-INF/services/org.pelagios.recogito.sdk.ner.NERPlugin"
}

/** Runtime dependencies **/
libraryDependencies ++= Seq(
  "org.pelagios" % "recogito-plugin-sdk" % "0.3" from "https://github.com/pelagios/recogito2-plugin-sdk/releases/download/v0.3/recogito-plugin-sdk-0.3.jar"
)

/** Test dependencies **/
libraryDependencies ++= Seq(
  "junit" % "junit" % "4.11" % "test"
)
