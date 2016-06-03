package models.contribution

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object ContributionAction extends Enumeration {
  
  val CREATE_DOCUMENT        = Value("CREATE_DOCUMENT")
  val EDIT_DOCUMENT_METADATA = Value("UPDATE_DOCUMENT")
  val DELETE_DOCUMENT        = Value("DELETE_DOCUMENT")
  val SHARE_DOCUMENT         = Value("SHARE_DOCUMENT")

  val CREATE_BODY            = Value("CREATE_BODY")
  val EDIT_BODY              = Value("EDIT_BODY")
  val CONFIRM_BODY           = Value("CONFIRM_BODY")
  val DELETE_BODY            = Value("DELETE_BODY")
  
  // TODO forum posts
  
  /** JSON conversion **/
  implicit val contributionActionFormat: Format[ContributionAction.Value] =
    Format(
      JsPath.read[String].map(ContributionAction.withName(_)),
      Writes[ContributionAction.Value](s => JsString(s.toString))
    )
   
}