package controllers.my.directory.create.types

import controllers.my.directory.create.CreateController
import java.util.UUID
import play.api.Logger
import play.api.mvc.AnyContent
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import services.{UnsupportedContentTypeException, UnsupportedTextEncodingException}
import services.generated.tables.records.UploadRecord
import services.upload.QuotaExceededException
import services.user.User

case class UploadSuccess(partId: UUID, contentType: String)

object UploadSuccess {

  implicit val uploadSuccessWrites: Writes[UploadSuccess] = (
    (JsPath \ "uuid").write[UUID] and
    (JsPath \ "content_type").write[String]
  )(unlift(UploadSuccess.unapply))

}

trait FileUpload { self: CreateController =>

  protected def storeFile(pendingUpload: UploadRecord, owner: User, body: AnyContent) = {
    val MSG_ERROR = "Something went wrong while storing your file"

    body.asMultipartFormData.map(tempfile => {
      tempfile.file("file").map { f =>
        uploads.insertUploadFilepart(pendingUpload.getId, owner, f).map(_ match {
          case Right(filepart) =>
            // Upload was properly identified and stored
            Ok(Json.toJson(UploadSuccess(filepart.getId, filepart.getContentType)))

          case Left(e: UnsupportedContentTypeException) =>
            BadRequest("Unknown or unsupported file format")
            
          case Left(e: UnsupportedTextEncodingException) =>
            BadRequest("Unknown or unsupported text encoding")
            
          case Left(e: QuotaExceededException) =>
            BadRequest("Not enough space - you only have " + e.remainingSpaceKb / 1024 + " MB remaining")

          case Left(otherFailure) =>
            // For future use
            BadRequest(MSG_ERROR)
        })
      }.getOrElse({
        // POST without a file? Not possible through the UI!
        Logger.warn("Filepart POST without file attached")
        Future.successful(BadRequest(MSG_ERROR))
      })
    }).getOrElse({
      // POST without form data? Not possible through the UI!
      Logger.warn("Filepart POST without form data")
      Future.successful(BadRequest(MSG_ERROR))
    })
  }

}