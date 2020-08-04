package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.RequestHeader
import services.HasNullableSeq
import services.document.ExtendedDocumentMetadata
import services.entity.EntityType
import services.user.User

case class GazetteerPreferences(useAll: Boolean, includes: Seq[String])

object GazetteerPreferences extends HasNullableSeq {
  
  implicit val gazetteerPreferencesFormat: Format[GazetteerPreferences] = (
    (JsPath \ "use_all").format[Boolean] and
    (JsPath \ "includes").formatNullable[Seq[String]]
      .inmap(fromOptSeq[String], toOptSeq[String])
  )(GazetteerPreferences.apply, unlift(GazetteerPreferences.unapply))
  
  val DEFAULTS = GazetteerPreferences(true, Seq.empty[String])

}

trait PreferencesActions { self: SettingsController =>
  
  def showAnnotationPreferences(doc: ExtendedDocumentMetadata, user: User)(implicit request: RequestHeader) = {
    val fGazetteers= self.authorities.listAll(Some(EntityType.PLACE))
    val fCurrentPrefs = self.documents.getDocumentPreferences(doc.id)
    
    val f = for {
      gazetteers <- fGazetteers
      currentPrefs <- fCurrentPrefs
    } yield (gazetteers, currentPrefs)
    
    f.map { case (gazetteers, allPrefs) =>
      val gazetteerPrefs = allPrefs
        .find(_.getPreferenceName == "authorities.gazetteers")
        .flatMap(str => Json.fromJson[GazetteerPreferences](Json.parse(str.getPreferenceValue)).asOpt)
        .getOrElse(GazetteerPreferences.DEFAULTS)
        
      Ok(views.html.document.settings.preferences(doc, user, gazetteers, gazetteerPrefs))
    }
  }
    
  def setGazetteerPreferences(docId: String) = self.silhouette.SecuredAction.async { implicit request =>
    // JSON is parsed to case class and instantly re-serialized as a security/sanitization measure!
    jsonDocumentAdminAction[GazetteerPreferences](docId, request.identity.username, { case (document, prefs) =>      
      self.documents.upsertPreferences(docId, "authorities.gazetteers", Json.stringify(Json.toJson(prefs))).map { success =>
        if (success) Ok else InternalServerError
      }
    })
  }

  def setTagVocabulary(docId: String) = self.silhouette.SecuredAction.async { implicit request => 
    jsonDocumentAdminAction[Seq[String]](docId, request.identity.username, { case (document, vocabulary) => 
      self.documents.upsertPreferences(docId, "tag.vocabulary", Json.stringify(Json.toJson(vocabulary))).map { success => 
        if (success) Ok else InternalServerError
      }
    }) 
  }
  
}