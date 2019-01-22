package controllers.my.directory.create.types

import controllers.my.directory.create.CreateController
import play.api.Logger
import play.api.mvc.AnyContent
import scala.concurrent.Future
import scala.util.{Success, Failure}
import services.ContentType
import services.generated.tables.records.UploadRecord
import services.user.User
import transform.iiif.{IIIF, IIIFParser}

trait IIIFSource { self: CreateController =>

  protected def registerIIIFSource(pendingUpload: UploadRecord, owner: User, body: AnyContent) = {
    body.asMultipartFormData.flatMap(_.dataParts.get("url").flatMap(_.headOption)) match {
      case Some(url) =>
        // Identify type of IIIF URL - image or item manifest? 
        IIIFParser.identify(url).flatMap {  
          case Success(IIIF.IMAGE_INFO) =>     
            uploads.deleteFilePartsByUploadId(pendingUpload.getId).flatMap { _ =>
              uploads.insertRemoteFilepart(pendingUpload.getId, owner.username, ContentType.IMAGE_IIIF, url)
            }.map { success =>
              if (success) Ok else InternalServerError
            }
          
          case Success(IIIF.MANIFEST) =>
            IIIFParser.fetchManifest(url).flatMap { 
              case Success(manifest) =>
                // Update the upload title with the manifset label (if any)
                manifest.label.map { label => uploads.storePendingUpload(owner.username, label) }

                // The weirdness of IIIF canvases. In order to get a label for the images,
                // we zip the images with the label of the canvas they are on (images don't
                // seem to have labels).
                val imagesAndLabels = manifest.sequences.flatMap(_.canvases).flatMap { canvas =>
                  canvas.images.map((_, canvas.label))
                }

                val inserts = imagesAndLabels.zipWithIndex.map { case ((image, label), idx) =>
                  uploads.insertRemoteFilepart(
                    pendingUpload.getId,
                    owner.username,
                    ContentType.IMAGE_IIIF,
                    image.service,
                    Some(label.value), 
                    Some(idx + 1)) // Remember: seq no. starts at 1 (because it's used in the URI) 
                  }
                
                Future.sequence(inserts).map { result =>
                  if (result.contains(false)) InternalServerError
                  else Ok
                }
                
              // Manifest parse error
              case Failure(e) =>
                Future.successful(BadRequest(e.getMessage))                  
            }
            
          case Failure(e) =>
            Future.successful(BadRequest(e.getMessage))
        }

      case None =>
        // POST without IIIF URL? Not possible through the UI!
        Logger.warn("IIIF POST without URL")
        Future.successful(BadRequest("Something went wrong while registering IIIF image"))
    }
  }


}