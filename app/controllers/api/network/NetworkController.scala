package controllers.api.network

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, Security, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import services.HasDate
import services.document.DocumentService
import services.document.network.{AncestryTree, AncestryTreeNode}
import services.user.UserService

@Singleton
class NetworkAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON with HasDate {

  implicit val ancestryTreeNodeWrites = new Writes[AncestryTreeNode] {

    def writes(node: AncestryTreeNode) = { 
      val obj = node.clonedAt match {
        case Some(timestamp) =>
          val dt = new DateTime(timestamp.getTime)
          Json.obj("id" -> node.id, "owner" -> node.owner, "cloned_at" -> formatDate(dt))

        case None => 
          Json.obj("id" -> node.id, "owner" -> node.owner)
      }

      if (node.children.isEmpty) obj
      else obj ++ Json.obj("children" -> node.children.map(writes(_)))
    }

  }

  def getNetwork(docId: String) = silhouette.UserAwareAction.async { implicit request => 
    documentResponse(docId, request.identity, { case (doc, accesslevel) => 
      if (accesslevel.canReadData) {
        documents.getNetwork(docId).map { _ match { 
          case Some(tree) => 
            val thisNode =
              if (doc.id == tree.rootNode.id) // Network for the root node
                Json.obj(
                  "id" -> doc.id,
                  "title" -> doc.title,
                  "owner" -> doc.ownerName)

              else // Network for a node inside the tree 
                Json.obj(
                  "id" -> doc.id,
                  "title" -> doc.title,
                  "owner" -> doc.ownerName,
                  "cloned_from" -> doc.clonedFrom,
                  "cloned_at" -> formatDate(new DateTime(doc.uploadedAt.getTime)))

            jsonOk(Json.obj("network_for" -> thisNode, "root" -> tree.rootNode))

          case None => NotFound
        }}
      } else {
        Future.successful(Forbidden)
      }
    })
  }

}
