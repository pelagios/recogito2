package controllers.document.downloads.serializers.document.iob

import java.io.{File, PrintWriter}
import java.nio.file.Paths
import java.util.UUID
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.document.ExtendedDocumentMetadata
import services.generated.tables.records.DocumentFilepartRecord
import scala.concurrent.{ExecutionContext, Future}
import storage.TempDir
import storage.uploads.Uploads

case class Token(text: String, charOffset: Int)

trait PlaintextToIOB {

  /** Loads the text document from storage and builds the token list */
  private def loadTokens(
    doc: ExtendedDocumentMetadata,
    part: DocumentFilepartRecord
  )(implicit uploads: Uploads, ctx: ExecutionContext): Future[Seq[Token]] = {
    // Load the text
    val fText = 
      uploads.readTextfile(doc.owner.getUsername, doc.id, doc.fileparts.head.getFile)
      
    // Chop up
    fText.map { _ match {
      case Some(text) =>
        text.split("[,\\s\\-:\\?]").foldLeft(Seq.empty[Token]) { (result, next) =>
          val offset = result.lastOption match {
            case Some(last) => last.charOffset + last.text.size + 1
            case None => 0
          }

          if (next.size > 0)
            result :+ Token(next, offset)
          else 
            result
        }

      case None => 
        // Should never happen
        throw new RuntimeException(s"Plaintext document part without file: ${doc.id}, ${part.getId}")
    }}
  }

  /** Keep only annotations that mark PLACE or PERSON entities **/
  private def filterEntityAnnotations(annotations: Seq[Annotation]): Seq[Annotation] = {

    def isEntityBody(body: AnnotationBody) =
      body.hasType == AnnotationBody.PLACE || body.hasType == AnnotationBody.PERSON

    annotations.filter(_.bodies.exists(isEntityBody))
  }

  /** Parses the anchors and extracts numeric offsets **/
  private def parseOffsets(annotations: Seq[Annotation]): Seq[(Int, Annotation)] = {

    val ANCHOR_PREFIX_LENGTH = "char-offset:".size

    annotations.map { annotation => 
      val offset = annotation.anchor.substring(ANCHOR_PREFIX_LENGTH).toInt
      (offset, annotation)
    }
  }

  /** Returns the annotation inside of which the given token is (if any) **/
  private def isInsideOf(token: Token, annotations: Seq[(Int, Annotation)]): Option[Annotation] = {
    val tokenStarts = token.charOffset
    val tokenEnds = tokenStarts + token.text.size

    def getQuote(a: Annotation) = 
      a.bodies
       .find(_.hasType == AnnotationBody.QUOTE)
       .get // Safe to assume that this is a text annotation (or else: fail hard)
       .value
       .get

    annotations.find { case (annotationStarts, annotation) => 
      val annotationEnds = annotationStarts + getQuote(annotation).size
      annotationStarts <= tokenStarts &&
      annotationEnds > tokenStarts
    } map { _._2 }
  }

  private def getTag(annotation: Annotation) = {
    val firstEntityBody = annotation.bodies.find { b => 
      b.hasType == AnnotationBody.PERSON || b.hasType == AnnotationBody.PLACE
    }.get // Safe to assume there is an entity body in this case

    firstEntityBody.hasType match {
      case AnnotationBody.PERSON => "PER"
      case AnnotationBody.PLACE => "LOC"
    }
  }

  def plaintextToIOB(
    doc: ExtendedDocumentMetadata
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

    val fAnnotations = 
      annotationService
        .findByFilepartId(part.getId)
        .map(a => filterEntityAnnotations(a.map(_._1)))

    val f = for {
      tokens <- fTokens
      annotations <- fAnnotations
    } yield (tokens, parseOffsets(annotations))

    f.map { case (tokens, annotations) => 
      tokens.foldLeft(Option.empty[Annotation]) { (previousAnnotation, token) =>
        val annotation = isInsideOf(token, annotations)
        val iob = (previousAnnotation, annotation) match {
          // Still inside the same annotation
          case (Some(before), Some(current)) if before == current => s"I-${getTag(current)}"

          // Begin new token
          case (_, Some(annotation)) => s"B-${getTag(annotation)}"

          // (_, None)
          case _ => "O"
        }

        writer.println(s"${token.text} $iob")
        // Forward the current annotation to the next round for comparison
        annotation 
      }

      writer.close()
      underlying
    }
  }

}