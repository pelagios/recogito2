package controllers.myrecogito.upload.ner

import akka.actor.{ ActorRef, ActorSystem, Props }
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.pipeline.{ Annotation => NLPAnnotation }
import java.util.Properties
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.collection.JavaConverters._
import org.apache.commons.lang3.RandomStringUtils

private[ner] case class Phrase(chars: String, entityTag: String, charOffset: Int)

object NERService {
  
  private val props = new Properties()
  props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
  
  private val pipeline = new StanfordCoreNLP(props)
  
  // A mutable hashmap for tracking ActorRefs by their ID
  private val actors = scala.collection.mutable.HashMap.empty[String, ActorRef]
  
  private[ner] def parse(text: String) = {
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
    
    phrases.filter(_.entityTag != "O")
  }
  
  private def generateRandomActorId(triesLeft: Int = 100): String = {
    val id = RandomStringUtils.randomAlphanumeric(20)
    if (actors.contains(id)) {
      // Collision! Try again, or throw exception if max number of tries reached
      if (triesLeft > 0)
        generateRandomActorId(triesLeft - 1)      
      else
        throw new RuntimeException("Could not generate unique actor ID")
    } else {
      // Unique ID
      id
    }
  }
  
  /** Spawns a new background parse process **/
  def spawnParseProcess(document: DocumentRecord, fileparts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem) = {
    val actorId = generateRandomActorId()
    val actor = system.actorOf(Props(classOf[NERActor], document, fileparts), name = actorId) 
    actor ! NERActor.StartNER
    actorId
  }
  
  /** Queries the progress for a specific process **/ 
  def queryProgress(processId: String)(implicit system: ActorSystem) = {
    
  }
  
}

