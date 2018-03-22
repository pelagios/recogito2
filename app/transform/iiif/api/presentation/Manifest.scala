package transform.iiif.api.presentation

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Manifest(thumbnail: Option[String], sequences: Seq[Sequence])

object Manifest {
  
  implicit val manifestReads: Reads[Manifest] = //(
    (JsPath \ "sequences").read[Seq[Sequence]].map(s => Manifest(None, s)) // and
    /*(JsPath \ "sequences").read[Seq[Sequence]]
  )(Manifest.apply _)*/
  
}
