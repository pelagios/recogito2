package controllers.document.downloads.serializers.document.iob

import java.io.{File, PrintWriter}
import java.nio.file.Paths
import java.util.UUID
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import services.annotation.{Annotation, AnnotationService}
import services.document.DocumentInfo
import services.generated.tables.records.DocumentFilepartRecord
import scala.concurrent.{ExecutionContext, Future}
import storage.TempDir
import storage.uploads.Uploads

case class Token(text: String, charOffset: Int)

trait PlaintextToIOB {

  /** Loads the text document from storage and builds the token list */
  private def loadTokens(
    doc: DocumentInfo,
    part: DocumentFilepartRecord
  )(implicit uploads: Uploads, ctx: ExecutionContext): Future[Seq[Token]] = {
    // Load the text
    val fText = 
      uploads.readTextfile(doc.owner.getUsername, doc.id, doc.fileparts.head.getFile)
      
    // Chop up
    fText.map { _ match {
      case Some(text) =>
        text.split(" ").foldLeft(Seq.empty[Token]) { (result, next) =>
          val offset = result.lastOption match {
            case Some(last) => last.charOffset + last.text.size + 1
            case None => 0
          }

          result :+ Token(next, offset)
        }

      case None => 
        // Should never happen
        throw new RuntimeException(s"Plaintext document part without file: ${doc.id}, ${part.getId}")
    }}
  }

  /** Returns the annotation inside of which the given token is (if any) **/
  private def isInsideOf(token: Token, annotations: Seq[(Int, Annotation)]): Option[Annotation] = ???

  /** Parses the anchors and extracts numeric offsets **/
  private def parseOffsets(annotations: Seq[Annotation]): Seq[(Int, Annotation)] = ???

  def plaintextToIOB(
    doc: DocumentInfo
  )(implicit 
    annotationService: AnnotationService,
    conf: Configuration,
    ctx: ExecutionContext,
    tmpFile: TemporaryFileCreator,
    uploads: Uploads
  ): Future[File] = {

    // For the time being, we assume single-part plaintext files
    val part = doc.fileparts.head

    val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.iob.txt"))
    val underlying = tmp.path.toFile
    val writer = new PrintWriter(underlying)

    val fTokens = loadTokens(doc, part)

    // TODO keep only entity annotations
    val fAnnotations = annotationService.findByFilepartId(part.getId)

    val f = for {
      tokens <- fTokens
      annotations <- fAnnotations
    } yield (tokens, parseOffsets(annotations.map(_._1)))

    f.map { case (tokens, annotations) => 
      tokens.foldLeft(Option.empty[Annotation]) { (previousAnnotation, token) =>
        val annotation = isInsideOf(token, annotations)

        val iob = (previousAnnotation, annotation) match {

          // Begin
          case (None, Some(annotation)) => "B"

          // Still inside the same annotation
          case (Some(before), Some(current)) if before == current => "I"

          // New token
          case (Some(before), Some(current)) => "B"

          // (_, None)
          case _ => "O"
         
        }

        writer.println(s"$token $iob")

        // Hand current annotation to the next iteration
        annotation 
      }
      
      underlying
    }
  }

}