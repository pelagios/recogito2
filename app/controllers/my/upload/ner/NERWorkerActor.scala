package controllers.my.upload.ner

import akka.actor.Actor
import java.io.File
import java.util.UUID
import models.annotation._
import models.content.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.io.Source

private[ner] object NERWorkerActor {

  val SUPPORTED_CONTENT_TYPES = Set(ContentTypes.TEXT_PLAIN).map(_.toString)

}

private[ner] class NERWorkerActor(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) extends Actor {

  import NERMessages._

  var progress = 0.0

  def receive = {

    case Start => {
      val origSender = sender
      parseFilepart(document, part, dir).map { result =>
        val annotations = phrasesToAnnotations(result, document, part)
        AnnotationService.insertAnnotations(annotations)
        progress = 1.0
        origSender ! Completed
      }.recover { case t => {
        t.printStackTrace
        origSender ! Failed(t.getMessage)
      }}
    }

    case QueryProgress =>
      sender ! WorkerProgress(part.getId, progress)

  }

  /** Select appropriate parser for part content type **/
  private def parseFilepart(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) =
    part.getContentType match {
      case t if t == ContentTypes.TEXT_PLAIN.toString =>
        parsePlaintext(document, part, new File(dir, part.getFilename))

      case t => {
        Logger.info("Skipping NER for file of unsupported type " + t + ": " + dir.getName + File.separator + part.getFilename)
        Future { Seq.empty[Phrase] }
      }
    }

  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    val text = Source.fromFile(file).getLines.mkString("\n")
    NERService.parse(text)
  }

  private def phrasesToAnnotations(phrases: Seq[Phrase], document: DocumentRecord, part: DocumentFilepartRecord) =
    phrases
      .filter(p => (p.entityTag == "LOCATION" || p.entityTag == "PERSON"))
      .map(p => {
        val now = DateTime.now
        val annotationType =
          p.entityTag match {
            case "LOCATION" => AnnotationBody.PLACE
            case "PERSON" => AnnotationBody.PERSON
          }

        Annotation(
          UUID.randomUUID,
          UUID.randomUUID,
          AnnotatedObject(document.getId, part.getId),
          None, // no previous versions
          Seq.empty[String], // No contributing users
          "char-offset:" + p.charOffset,
          None, // no last modifying user
          now,
          Seq(
            AnnotationBody(
              AnnotationBody.QUOTE,
              None, // no last modifying user
              now,
              Some(p.chars),
              None), // uri
            AnnotationBody(
              annotationType,
              None,
              now,
              None,
              None)
          ),
          AnnotationStatus(
            AnnotationStatus.UNVERIFIED,
            None,
            now)
        )
      })

}
