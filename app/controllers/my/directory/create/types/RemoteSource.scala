package controllers.my.directory.create.types

import controllers.my.directory.create.CreateController
import java.io.PrintWriter
import java.nio.file.Paths
import java.util.UUID
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Result}
import scala.concurrent.Future
import scala.util.{Success, Failure}
import services.ContentType
import services.generated.tables.records.UploadRecord
import services.user.User
import storage.TempDir
import transform.iiif.{IIIF, IIIFParser}

trait RemoteSource { self: CreateController =>

  protected def registerRemoteSource(
    pendingUpload: UploadRecord, 
    owner: User, 
    body: AnyContent
  )(implicit
    tmpFile: TemporaryFileCreator,
    ws: WSClient
  ) = {
    val maybeUrl = body.asMultipartFormData.flatMap(_.dataParts.get("url").flatMap(_.headOption)) 
    val maybeType = body.asMultipartFormData.flatMap(_.dataParts.get("type").flatMap(_.headOption))
    
    (maybeUrl, maybeType)  match {
      case (Some(url), Some(typ)) =>
        typ match {
          case "IIIF" => registerIIIFSource(pendingUpload, owner, url)
          case "CTS" => fetchCTSSource(pendingUpload, owner, url)
          case "TEI_XML" => fetchFileFromURL(pendingUpload, owner, url, "tei-import.tei.xml")
          case "WMTS" => registerWMTSSource(pendingUpload, owner, url)
        }

      case _ =>
        // POST without source URL? Not possible through the UI!
        Logger.warn("Remote source POST needs URL and source type")
        Future.successful(BadRequest("Something went wrong while registering remote source"))
    }
  }

  /** TODO set proper filename from URL (or CTS XML?) **/
  private def fetchCTSSource(
    pendingUpload: UploadRecord, 
    owner: User, 
    url: String
  )(implicit 
      tmpFile: TemporaryFileCreator,
      ws: WSClient
  ): Future[Result] = ws.url(url).withFollowRedirects(true).get.flatMap { response => 
    // Extract TEI to temporary file
    val tei = (response.xml \\ "TEI")

    val p = Paths.get(TempDir.get, s"${UUID.randomUUID}.tei.xml")
    val tmp = tmpFile.create(p)
    val underlying = p.toFile

    new PrintWriter(underlying) { write(tei(0).toString); close }

    // Store TEI filepart 
    uploads.insertUploadFilepart(pendingUpload.getId, owner, tmp, "cts-import.tei.xml").map { _ match {
      case Right(filepart) =>
        Ok(Json.toJson(UploadSuccess(filepart.getId, filepart.getContentType)))
  
      case Left(e) =>
        InternalServerError
    }}
  }

  private def fetchFileFromURL(
    pendingUpload: UploadRecord,
    owner: User,
    url: String,
    filename: String
  )(implicit 
      tmpFile: TemporaryFileCreator,
      ws: WSClient
  ): Future[Result] = ws.url(url).withFollowRedirects(true).get.flatMap { response => 
    val extension = filename.substring(filename.lastIndexOf('.'))
    val p = Paths.get(TempDir.get, s"${UUID.randomUUID}${extension}")
    val tmp = tmpFile.create(p)
    val underlying = p.toFile

    new PrintWriter(underlying) { 
      write(response.body.toString)
      close
    }

    uploads.insertUploadFilepart(pendingUpload.getId, owner, tmp, filename).map { _ match {
      case Right(filepart) => 
        Ok(Json.toJson(UploadSuccess(filepart.getId, filepart.getContentType)))

      case Left(e) =>
        BadRequest
    }}
  }

  private def registerWMTSSource(pendingUpload: UploadRecord, owner: User, url: String): Future[Result] = {
    uploads.insertRemoteFilepart(pendingUpload.getId, owner.username, ContentType.MAP_WMTS, url).map { success => 
      if (success) Ok else InternalServerError
    }
  }

  private def registerIIIFSource(pendingUpload: UploadRecord, owner: User, url: String): Future[Result] = {
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
  }

}