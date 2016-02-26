package controllers.myrecogito.upload.ner

import akka.actor.Actor
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.pipeline.{ Annotation => NLPAnnotation }
import java.io.File
import java.util.Properties
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.io.Source
import storage.FileAccess

private[ner] class NERActor(document: DocumentRecord, fileparts: Seq[DocumentFilepartRecord]) extends Actor with FileAccess {
  
  def receive = {
    
    case "SomeMessage" => // TODO implement
      
  }

  private def parseFileparts() = {
    // This will throw an exception if user data dir does not exist - can only
    // happen in case DB integrity is broken!
    val userDir = getUserDir(document.getOwner).get
    
    val files = fileparts.map(part => (part, new File(userDir, part.getFilename)))
    
    // Process files in parallel
    files.par.map(_ match {
      case (part, file) if part.getContentType == ContentTypes.TEXT_PLAIN => 
        parsePlaintext(document, part, file)
        
      case (part, file) =>
        Logger.info("Skipping NER for file of unsupported type " + part.getContentType + ": " + file.getAbsolutePath) 
    })
  }
  
  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    val text = Source.fromFile(file).getLines.mkString("\n")
    
    // TODO implement
    
  }
  
}

private[ner] case class Phrase(chars: String, entityTag: String, charOffset: Int)

object NERService extends FileAccess {
  
  private val props = new Properties()
  props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
  
  private val pipeline = new StanfordCoreNLP(props)
  
  private def parse(text: String) = {
    val document = new NLPAnnotation(text)
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
    
    Logger.info(phrases.toString)
  }
  
}

