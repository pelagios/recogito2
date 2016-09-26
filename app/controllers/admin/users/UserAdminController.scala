package controllers.admin.users

import controllers.BaseAuthController
import javax.inject.Inject
import models.document.DocumentService
import models.user.Roles._
import models.user.UserService
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import controllers.WebJarAssets
import play.api.Configuration
import models.annotation.AnnotationService
import scala.concurrent.Await
import scala.concurrent.duration._

class UserAdminController @Inject() (
    val config: Configuration,
    
    val annotations: AnnotationService,
    
    val documents: DocumentService,
    val users: UserService,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseAuthController(config, documents, users) {

  def index = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    users.listUsers(0, 500).map(users => 
      Ok(views.html.admin.users.index(users)))
  }
  
  private def regenerateLinks(username: String) = {
    documents.findByOwner(username, false, 0, Int.MaxValue).map { page =>
      val docs = page.items
      docs.map { doc =>
        annotations.findByDocId(doc.getId).map { a =>
          a.foreach { annotation =>
            Await.result(annotations.insertOrUpdateGeoTagsForAnnotation(annotation._1), 1.minute)
          }
        }
      }
    }
  }

  def showDetails(username: String) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    users.findByUsername(username).flatMap(_ match {

      case Some(user) =>
        documents.findByOwner(username).map { documents => 
          regenerateLinks(user.user.getUsername)
          Ok(views.html.admin.users.details(user, documents)) 
        }

      case None => Future.successful(NotFoundPage)

    })
  }

}
