package controllers.api.network

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, Security, HasPrettyPrintJSON}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import services.HasDate
import services.annotation.AnnotationService
import services.document.{DocumentService, ExtendedDocumentMetadata}
import services.document.network.{AncestryTree, AncestryTreeNode}
import services.generated.tables.records.DocumentFilepartRecord
import services.user.UserService
import storage.uploads.Uploads

@Singleton
class NetworkAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val annotations: AnnotationService,
  val documents: DocumentService,
  val users: UserService,
  val uploads: Uploads,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON with HasDate {

  implicit val ancestryTreeNodeWrites = new Writes[AncestryTreeNode] {

    def writes(node: AncestryTreeNode) = { 
      val obj = node.clonedAt match {
        case Some(timestamp) =>
          val dt = new DateTime(timestamp.getTime)
          Json.obj(
            "id" -> node.id, 
            "owner" -> node.owner, 
            "cloned_from" -> node.clonedFrom.get,
            "cloned_at" -> formatDate(dt))

        case None => 
          Json.obj("id" -> node.id, "owner" -> node.owner)
      }

      if (node.children.isEmpty) obj
      else obj ++ Json.obj("children" -> node.children.map(writes(_)))
    }

  }

  /** Returns the network/tree of clones for the given document ID 
    *
    * Requires at least data-read privileges on the document.
    */
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

  private def matchFileparts(toDoc: ExtendedDocumentMetadata, fromDoc: ExtendedDocumentMetadata): Future[Map[UUID, UUID]] = {
    // Helper
    def readSize(f: DocumentFilepartRecord): Future[(DocumentFilepartRecord, Long)] =
      uploads.getFilesize(toDoc.ownerName, toDoc.id, f.getFile).map(size => (f, size))

    // Step 1: read the filesize for all fileparts, so we have some criterion to compare on
    val f = for {
      toDocFileSizes <- Future.sequence { toDoc.fileparts.map(readSize) }
      fromDocFileSizes <- Future.sequence { toDoc.fileparts.map(readSize) }
    } yield (toDocFileSizes, fromDocFileSizes)

    f.map { case (toDocFileSizes, fromDocFileSizes) =>
      toDocFileSizes.zipWithIndex.foldLeft(Seq.empty[(UUID, UUID)]) { case (matches, ((filepart, size), idx)) => 
        val isMatch = // If there is a corresponding filepart in 'from', check if its the same size
          if (fromDocFileSizes.size > idx)
            fromDocFileSizes(idx)._2 == size
          else
            false

        // TODO find a match based on file hash           

        if (isMatch)
          matches :+ (filepart.getId, fromDocFileSizes(idx)._1.getId)
        else 
          matches
      }.toMap
    }
  }

  /** Merges the annotations from the given document into the given document. 
    * 
    * Requires data-read privileges on fromDoc, and admin privileges on toDoc.
    */
  def mergeAnnotations(toDocId: String, fromDocId: String) = silhouette.SecuredAction.async { implicit request => 
    val fFromDoc = documents.getExtendedMeta(fromDocId, Some(request.identity.username))
    val fToDoc = documents.getExtendedMeta(toDocId, Some(request.identity.username))

    val f = for {
      from <- fFromDoc
      to <- fToDoc
    } yield (from, to)

    f.flatMap { _ match {
      case (Some((fromDoc, fromAccesslevel)), Some((toDoc, toAccesslevel))) => 

        if (fromAccesslevel.canReadData && toAccesslevel.isAdmin) {

          // TODO build UUID->UUID correspondence table
          
          annotations.mergeAnnotations(
            toDoc.id, 
            fromDoc.id, 
            Map.empty[java.util.UUID, java.util.UUID], // TODO
            new DateTime() // TODO
          ).map { success => 
            if (success) Ok else InternalServerError
          }
        } else {
          Future.successful(Forbidden)
        }

      // At least one of the docs wasn't found
      case _ => Future.successful(NotFound)
    }}
  }

}
