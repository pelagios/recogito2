package controllers.myrecogito.upload.ner

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{ Annotation, StanfordCoreNLP }
import java.io.File
import java.util.Properties
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import storage.FileAccess

private[ner] case class Phrase(chars: String, entityTag: String, charOffset: Int)

object NERService extends FileAccess {
  
  private lazy val props = new Properties()
  props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
  
  private var pipeline: StanfordCoreNLP = null
    
  private val futurePipeline = Future {
    val coreNLP = new StanfordCoreNLP(props)
    pipeline = coreNLP
    coreNLP
  }
  
  private[ner] def parse(text: String): Future[Seq[Phrase]] = {

    def parseWithPipeline(text: String, pipeline: StanfordCoreNLP): Seq[Phrase] = {
      val document = new Annotation(text)
      pipeline.annotate(document)
    
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
    
      phrases.filter(_.entityTag != "O")    
    }
    
    if (pipeline == null)
      futurePipeline.map(p => parseWithPipeline(text, p))
    else
      Future { parseWithPipeline(text, pipeline) }
  }
    
  /** Spawns a new background parse process. 
    * 
    * The function will throw an exception in case the user data directory
    * for any of the fileparts does not exist. This should, however, never
    * happen. If it does, something is seriously broken with the DB integrity.
    */
  def spawnNERProcess(document: DocumentRecord, parts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem): Unit =
    spawnNERProcess(document, parts, getUserDir(document.getOwner).get)
  
  /** We're splitting this function, so we can inject alternative folders for testing **/
  private[ner] def spawnNERProcess(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], sourceFolder: File, keepalive: Duration = 10 minutes)(implicit system: ActorSystem): Unit = {
    val actor = system.actorOf(Props(classOf[NERSupervisorActor], document, parts, sourceFolder, keepalive), name = "doc_" + document.getId.toString) 
    actor ! NERMessages.Start
  }
  
  /** Queries the progress for a specific process **/ 
  def queryProgress(documentId: Int, timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
    NERSupervisor.getActor(documentId) match {
      case Some(actor) => {
        implicit val t = Timeout(timeout)
        (actor ? NERMessages.QueryProgress).mapTo[NERMessages.DocumentProgress].map(Some(_))
      }
      
      case None =>
        Future.successful(None)
    }
  }
  
}

