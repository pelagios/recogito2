package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq

case class GazetteerPreferences(useAll: Boolean, includes: Seq[String])

object GazetteerPreferences extends HasNullableSeq {
  
  implicit val gazetteerPreferencesFormat: Format[GazetteerPreferences] = (
    (JsPath \ "use_all").format[Boolean] and
    (JsPath \ "includes").formatNullable[Seq[String]]
      .inmap(fromOptSeq[String], toOptSeq[String])
  )(GazetteerPreferences.apply, unlift(GazetteerPreferences.unapply))

}

trait PreferencesActions { self: SettingsController =>
    
  def setGazetteerPreferences(docId: String) = self.silhouette.SecuredAction.async { implicit request =>
    // JSON is parsed to case class and instantly re-serialized as a security/sanitization measure!
    jsonDocumentAdminAction[GazetteerPreferences](docId, request.identity.username, { case (document, prefs) =>      
      self.documents.upsertPreferences(docId, "GAZETTEERS", Json.stringify(Json.toJson(prefs))).map { success =>
        if (success) Ok
        else InternalServerError
      }
    })
  }
  
}