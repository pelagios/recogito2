name := "recogito-plugin-ner-stanford"

organization := "org.pelagios"

version := "0.0.2"

// Do not append Scala versions to the generated artifacts
crossPaths := false

/** Runtime dependencies **/
libraryDependencies ++= Seq(
  "org.pelagios" % "recogito-plugin-sdk" % "0.0.2" from "https://github.com/pelagios/recogito2-plugin-sdk/releases/download/v0.0.2/recogito-plugin-sdk-0.0.2.jar",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2" classifier "models"
)

/** Test dependencies **/
libraryDependencies ++= Seq(
  "junit" % "junit" % "4.11" % "test"
)
