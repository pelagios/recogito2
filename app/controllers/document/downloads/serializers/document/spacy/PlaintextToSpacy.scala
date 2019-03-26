package controllers.document.downloads.serializers.document.spacy

import java.io.File
import java.nio.file.Paths
import java.util.UUID
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import services.document.ExtendedDocumentMetadata
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.generated.tables.records.DocumentFilepartRecord
import storage.TempDir
import storage.uploads.Uploads

case class Sentence(text: String, charOffset: Int)

case class SpacyEntity(start: Int, end: Int, tag: String)

case class SpacyRecord(sentence: String, entities: Seq[SpacyEntity])

object SpacyEntity {
  
  implicit val spacyEntityWrites = Json.writes[SpacyEntity]

}

object SpacyRecord {

  implicit val spacyRecordWrites = Json.writes[SpacyRecord]

}

trait PlaintextToSpacy {

  // private multiDelimSplit(text: String): 

  /** Loads the text document from storage and builds the token list. 
    * 
    * TODO pretty much identical with tokenization for IOB - refactor
    */
  private def loadLines(
    doc: ExtendedDocumentMetadata,
    part: DocumentFilepartRecord
  )(implicit uploads: Uploads, ctx: ExecutionContext): Future[Seq[Sentence]] = {
    // Load the text
    val fText = 
      uploads.readTextfile(doc.owner.getUsername, doc.id, doc.fileparts.head.getFile)
      
    // Chop up
    fText.map { _ match {
      case Some(text) =>

        // Horrible, but we need to keep the indices in sync for multiple
        // occurrences of the delimiting character
        val lines = scala.collection.mutable.ArrayBuffer.empty[Sentence]

        var runningOffset = 0
        var rest = text

        while (rest.size > 0) {
          val nextDelim = rest.indexOf("\n")
          if (nextDelim > -1) {
            lines.append(Sentence(rest.substring(0, nextDelim), runningOffset))
            runningOffset += nextDelim + 1
            rest = rest.substring(nextDelim + 1)
          } else {
            lines.append(Sentence(rest, runningOffset))
            rest = ""
          }
        }

        lines.toSeq.filter(!_.text.isEmpty)

      case None => 
        // Should never happen
        throw new RuntimeException(s"Plaintext document part without file: ${doc.id}, ${part.getId}")
    }}
  }

  /** Keep only annotations that mark PLACE or PERSON entities 
    * 
    * TODO identical to IOB -> move into common base trait
    */
  private def filterEntityAnnotations(annotations: Seq[Annotation]): Seq[Annotation] = {

    def isEntityBody(body: AnnotationBody) =
      body.hasType == AnnotationBody.PLACE || body.hasType == AnnotationBody.PERSON

    annotations.filter(_.bodies.exists(isEntityBody))
  }

  /** Parses the anchors and extracts numeric offsets
    * 
    * TODO indentical to IOB -> move into common (plaintext) base trait
    */
  private def parseOffsets(annotations: Seq[Annotation]): Seq[(Int, Annotation)] = {

    val ANCHOR_PREFIX_LENGTH = "char-offset:".size

    annotations.map { annotation => 
      val offset = annotation.anchor.substring(ANCHOR_PREFIX_LENGTH).toInt
      (offset, annotation)
    }
  }

  private def getQuote(a: Annotation) = 
    a.bodies
      .find(_.hasType == AnnotationBody.QUOTE)
      .get // Safe to assume that this is a text annotation (or else: fail hard)
      .value
      .get

  private def getTag(annotation: Annotation) = {
    val firstEntityBody = annotation.bodies.find { b => 
      b.hasType == AnnotationBody.PERSON || b.hasType == AnnotationBody.PLACE
    }.get // Safe to assume there is an entity body in this case

    firstEntityBody.hasType match {
      case AnnotationBody.PERSON => "PERSON"
      case AnnotationBody.PLACE => "LOC"
    }
  }

  private def getAnnotationsInSentence(sentence: Sentence, annotations: Seq[(Int, Annotation)]): Seq[(Int, Annotation)] =
    annotations.filter { case (start, annotation) => 
      // Quick hack - just use all annotations that start in this line
      start >= sentence.charOffset && start < (sentence.charOffset + sentence.text.size)
    }

  def plaintextToSpacy(
    doc: ExtendedDocumentMetadata
  )(implicit 
    annotationService: AnnotationService,
    conf: Configuration,
    ctx: ExecutionContext,
    tmpFile: TemporaryFileCreator,
    uploads: Uploads
  ): Future[JsValue] = {

    // For the time being, we assume single-part plaintext files
    val part = doc.fileparts.head

    val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.iob.txt"))
    val underlying = tmp.path.toFile

    val fLines = loadLines(doc, part)

    val fAnnotations = 
      annotationService
        .findByFilepartId(part.getId)
        .map(a => filterEntityAnnotations(a.map(_._1)))

    val f = for {
      lines <- fLines
      annotations <- fAnnotations
    } yield (lines, parseOffsets(annotations))

    // TODO indentical to IOB - up until here. Refactor!
    val fRecords = f.map { case (lines, allAnnotations) =>
      lines.map { line =>
        val annotationsInSentence = getAnnotationsInSentence(line, allAnnotations)

        val entities = annotationsInSentence.map { case (anchorOffset, a) =>
          val start = anchorOffset - line.charOffset
          val end = start + getQuote(a).size
          SpacyEntity(start, end, getTag(a))
        }

        SpacyRecord(line.text, entities)
      }
    }

    fRecords.map { records => 
      Json.toJson(records.map { r => 
        Json.arr(r.sentence, Json.obj(
          "entites" -> r.entities.map(r => Json.arr(r.start, r.end, r.tag))
        ))
      })
    }

  }

}