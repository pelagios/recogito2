package controllers.my.upload.ner

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout
import controllers.my.upload._
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{ Annotation, StanfordCoreNLP }
import java.io.File
import java.util.Properties
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.language.postfixOps
import storage.FileAccess

private[ner] case class Phrase(chars: String, entityTag: String, charOffset: Int)

object NERService extends ProcessingService with FileAccess {
  
  val TASK_NER = TaskType("NER")

  private lazy val props = new Properties()
  props.put("annotators", "tokenize, ssplit, pos, lemma, ner")

  private var runningPipelines = 0

  private[ner] def parse(text: String)(implicit context: ExecutionContext): Future[Seq[Phrase]] = {
    runningPipelines += 1
    
    if (runningPipelines > 5)
      Logger.warn(runningPipelines + " runnning NER pipelines")
    
    Future {
      val document = new Annotation(text)
      val pipeline = new StanfordCoreNLP(props)
      pipeline.annotate(document)
      
      Logger.info("NER annotation completed")
      val phrases = document.get(classOf[CoreAnnotations.SentencesAnnotation]).asScala.toSeq.flatMap(sentence => {
        val tokens = sentence.get(classOf[CoreAnnotations.TokensAnnotation]).asScala.toSeq
        tokens.foldLeft(Seq.empty[Phrase])((result, token) => {
          val entityTag = token.get(classOf[CoreAnnotations.NamedEntityTagAnnotation])
          val chars = token.get(classOf[CoreAnnotations.TextAnnotation])
          val charOffset = token.beginPosition
  
          result.headOption match {
  
            case Some(previousPhrase) if previousPhrase.entityTag == entityTag =>
              // Append to previous phrase if entity tag is the same
              Phrase(previousPhrase.chars + " " + chars, entityTag, previousPhrase.charOffset) +: result.tail
  
            case _ =>
              // Either this is the first token (result.headOption == None), or a new phrase
              Phrase(chars, entityTag, charOffset) +: result
  
          }
        })
      })
  
      runningPipelines -= 1
      
      phrases.filter(_.entityTag != "O")
    }
  }

  /** Spawns a new background parse process.
    *
    * The function will throw an exception in case the user data directory
    * for any of the fileparts does not exist. This should, however, never
    * happen. If it does, something is seriously broken with the DB integrity.
    */
  override def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem): Unit =
    spawnTask(document, parts, getDocumentDir(document.getOwner, document.getId).get)

  /** We're splitting this function, so we can inject alternative folders for testing **/
  private[ner] def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], sourceFolder: File, keepalive: Duration = 10 minutes)(implicit system: ActorSystem): Unit = {
    val actor = system.actorOf(Props(classOf[NERSupervisorActor], TASK_NER, document, parts, sourceFolder, keepalive), name = "ner_doc_" + document.getId)
    actor ! ProcessingTaskMessages.Start
  }

  /** Queries the progress for a specific process **/
  override def queryProgress(documentId: String, timeout: FiniteDuration = 10 seconds)(implicit context: ExecutionContext, system: ActorSystem) = {
    ProcessingTaskSupervisor.getSupervisorActor(TASK_NER, documentId) match {
      case Some(actor) => {
        implicit val t = Timeout(timeout)
        (actor ? ProcessingTaskMessages.QueryProgress).mapTo[ProcessingTaskMessages.DocumentProgress].map(Some(_))
      }

      case None =>
        Future.successful(None)
    }
  }

}
