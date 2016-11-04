package org.pelagios.recogito.plugins.ner.stanford

import java.util.{ ArrayList, Properties }
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{ Annotation, StanfordCoreNLP }
import org.pelagios.recogito.sdk.ner.NERPlugin
import scala.collection.JavaConverters._
import org.pelagios.recogito.sdk.ner.Entity
import org.pelagios.recogito.sdk.ner.EntityType
import scala.util.Try

case class StanfordEntity(chars: String, entityTag: String, charOffset: Int)

/** A Recogito plugin providing a wrapper around Stanford NER **/
class StanfordNERWrapperPlugin extends NERPlugin {
  
  private lazy val props = { 
    val p = new Properties()
    p.put("annotators", "tokenize, ssplit, pos, lemma, ner")
    p
  }
  
  override val getName = "StanfordNLP"
  
  override val getDescription = "A wrapper around the StanfordNLP named entity recognition engine"
  
  override val getOrganization = "Pelagios Commons"
  
  override val getVersion = "0.0.1"
  
  override val getSupportedLanguages = new ArrayList[String]()
  
  override def parse(text: String) = {
    
    def toEntityType(entityTag: String) = Try(EntityType.valueOf(entityTag)).toOption 
    
    val document = new Annotation(text)
    val pipeline = new StanfordCoreNLP(props)
    pipeline.annotate(document)
        
    val entities = document.get(classOf[CoreAnnotations.SentencesAnnotation]).asScala.toSeq.flatMap(sentence => {
      val tokens = sentence.get(classOf[CoreAnnotations.TokensAnnotation]).asScala.toSeq
      tokens.foldLeft(Seq.empty[StanfordEntity])((result, token) => {
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
        
      })
    })
        
    entities.withFilter(_.entityTag != "O")
      .flatMap(e => toEntityType(e.entityTag).map(etype => new Entity(e.chars, etype, e.charOffset))).asJava
  }
  
}