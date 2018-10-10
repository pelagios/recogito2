package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import services.document.RuntimeAccessLevel
import services.generated.tables.records.DocumentRecord
import services.user.Roles._
import play.api.mvc.{ AnyContent, Request }
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.Try

trait DeleteActions { self: SettingsController =>
  
  protected def documentOwnerAction(docId: String, username: String, action: DocumentRecord => Future[Boolean]) = {
    documents.getDocumentRecord(docId, Some(username)).flatMap(_ match {
      case Some((document, accesslevel)) => {
        if (accesslevel == RuntimeAccessLevel.OWNER) // We allow only the owner to delete a document
          action(document).map { success =>
            if (success) Ok else InternalServerError
          }
        else
          Future.successful(ForbiddenPage)
      }

      case None =>
        Future.successful(NotFoundPage) // No document with that ID found in DB
    }).recover { case t =>
      t.printStackTrace()
      InternalServerError(t.getMessage)
    }
  }

  /** Deletes all annotations on the document with the given ID **/
  def deleteAnnotations(docId: String) = self.silhouette.SecuredAction.async { implicit request =>
    documentOwnerAction(docId, request.identity.username, { document =>
      val deleteAnnotations = annotations.deleteByDocId(docId)
      val deleteContributions = contributions.deleteHistory(docId) 
      for {
        s1 <- deleteAnnotations
        s2 <- deleteContributions
      } yield (s1 && s2)
    })
  }

  /** Deletes one document. 
    * 
    * WARNING: this method DOES NOT CHECK ACCESS PERMISSONS. Ensure that whoever triggered 
    * it is allowed to delete.
    */
  private def deleteOneDocument(doc: DocumentRecord): Future[Boolean] = {
    val deleteDocument = documents.delete(doc)
    val deleteAnnotations = annotations.deleteByDocId(doc.getId)
    val deleteContributions = contributions.deleteHistory(doc.getId) 
    for {
      _ <- documents.delete(doc)
      s1 <- deleteAnnotations
      s2 <- deleteContributions
    } yield (s1 && s2)
  }

  /** Deletes the document with the given ID, along with all annotations and files **/
  def deleteDocument(docId: String) = self.silhouette.SecuredAction.async { implicit request =>
    documentOwnerAction(docId, request.identity.username, { document =>
      deleteOneDocument(document)
    })
  }

  def bulkDeleteDocuments() = self.silhouette.SecuredAction.async { implicit request => 
    val docIds = request.body.asJson match {
      case Some(json) => 
        Try(Json.fromJson[Seq[String]](json).get)
          .toOption.getOrElse(Seq.empty[String])

      case None => Seq.empty[String]
    }

    // All documents this user can - and is allowed to - delete
    val fDeleteableDocuments = Future.sequence {
      docIds.map { docId =>
        documents.getDocumentRecord(docId, Some(request.identity.username)).map(_ match {
          case Some((document, accesslevel)) =>
            if (accesslevel == RuntimeAccessLevel.OWNER) 
              Some(document)  
            else 
              None

          case _ => None
        })
      }
    } map { _.flatten }

    val fSuccess = fDeleteableDocuments.flatMap { toDelete =>
      Future.sequence(toDelete.map(deleteOneDocument))
    } map { !_.exists(!_) } // "No false exists in the list"

    fSuccess.map { success => 
      if (success) Ok else InternalServerError
    }
  }

}
