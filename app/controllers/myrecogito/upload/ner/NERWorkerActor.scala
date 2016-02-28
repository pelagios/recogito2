package controllers.myrecogito.upload.ner

import akka.actor.Actor
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.io.Source
//
import java.util.Properties
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{ Annotation, StanfordCoreNLP }
import scala.collection.JavaConverters._

private[ner] class NERWorkerActor(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) extends Actor {
  
  import NERMessages._
  
  var progress = 0.0
  
  def receive = {
    
    case Start => {
      val origSender = sender
      Future {
        val result = parseFilepart(document, part, dir)
        Logger.info("Done.")
      
        // TODO convert result to annotations
      
        // TODO import to DB
      
        progress = 1.0
      
        origSender ! Completed
        context.stop(self)
      }
    }
    
    case QueryProgress => {
      sender ! WorkerProgress(part.getId, progress)
    }
      
  }

  /** Select appropriate parser for part content type **/
  private def parseFilepart(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) =
    part.getContentType match {
      case t if t == ContentTypes.TEXT_PLAIN.toString =>
        parsePlaintext(document, part, new File(dir, part.getFilename))
          
      case t => {
        Logger.info("Skipping NER for file of unsupported type " + t + ": " + dir.getName + File.separator + part.getFilename) 
        Seq.empty[Phrase]
      }
    }
  
  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    val text = Source.fromFile(file).getLines.mkString("\n")
    NERService.parse(text)
  }
  
  /*
  private[ner] def parse(text: String) = {
    val props = new Properties()
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
  
    val document = new Annotation(text)
    // pipeline.annotate(document)
    new StanfordCoreNLP(props).annotate(document)
    
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
  */
  
}

private[ner] object NERWorkerActor {
  
  val SUPPORTED_CONTENT_TYPES = Set(ContentTypes.TEXT_PLAIN).map(_.toString)
  
}
