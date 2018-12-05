package org.pelagios.recogito.plugins.ner.stanford

import java.util.{ArrayList, Properties}
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{CoreDocument, StanfordCoreNLP}
import org.pelagios.recogito.sdk.ner._
import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory

case class StanfordEntity(chars: String, entityTag: String, charOffset: Int)

/** A Recogito plugin providing a wrapper around Stanford NER **/
class StanfordNERWrapperPlugin extends NERPlugin {
  
  private val logger = LoggerFactory.getLogger(this.getClass)

  private def toEntityType(entityTag: String) = entityTag match {
    case "LOCATION" | "CITY" | "COUNTRY" | "STATE_OR_PROVINCE" | "NATIONALITY" => Some(EntityType.LOCATION)
    case "PERSON" => Some(EntityType.PERSON)
    case "DATE" => Some(EntityType.DATE)
    case _ => None
  }
  
  private lazy val props = { 
    val p = new Properties()
    p.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner")
    p
  }
  
  override val getName = "Stanford CoreNLP"
  
  override val getDescription = "Out-of-the box engine with the default English language model"
  
  override val getOrganization = "Stanford NLP Group"
  
  override val getVersion = "3.9.1"
  
  override val getSupportedLanguages = Seq("en").asJava
  
  override def parse(text: String) = {
    logger.info("Initializing NER pipeline")
    val pipeline = new StanfordCoreNLP(props)
    logger.info("Pipeline initialized")
    val document = new CoreDocument(text) 
    pipeline.annotate(document)    
    
    val entities = document.tokens().asScala.foldLeft(Seq.empty[StanfordEntity]) { (result, token) =>
      val entityTag = token.get(classOf[CoreAnnotations.NamedEntityTagAnnotation])
      val chars = token.get(classOf[CoreAnnotations.TextAnnotation])
      val charOffset = token.beginPosition
      
      result.headOption match {
        case Some(previousEntity) if previousEntity.entityTag == entityTag =>
          // Append to previous phrase if entity tag is the same
          StanfordEntity(previousEntity.chars + " " + chars, entityTag, previousEntity.charOffset) +: result.tail
  
        case _ =>
          // Either this is the first token (result.headOption == None), or a new phrase
          StanfordEntity(chars, entityTag, charOffset) +: result
      }
    }

    entities.withFilter(_.entityTag != "O")
      .flatMap(e => toEntityType(e.entityTag).map(etype => new Entity(e.chars, etype, e.charOffset))).asJava
  }
  
}