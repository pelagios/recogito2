package controllers.myrecogito.upload

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{ Annotation, StanfordCoreNLP }
import java.io.File
import java.util.Properties
import scala.collection.JavaConverters._
import scala.io.Source

case class NamedEntity(term: String, category: String, offset: Int)

// TODO needs cleanup
object GeoParser {  
  
  private val props = new Properties()
  props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
  
  private val pipeline = new StanfordCoreNLP(props)
  
  def parse(text: String): Seq[NamedEntity] = {
    val document = new Annotation(text)
    pipeline.annotate(document)

    document.get(classOf[CoreAnnotations.SentencesAnnotation]).asScala.toSeq.map(sentence => {
      val tokens = sentence.get(classOf[CoreAnnotations.TokensAnnotation]).asScala.toSeq
      tokens.foldLeft(Seq.empty[NamedEntity])((result, nextToken) => {        
        val previousNE = if (result.size > 0) Some(result.head) else None		
        val term = nextToken.get(classOf[CoreAnnotations.TextAnnotation])
        val category = nextToken.get(classOf[CoreAnnotations.NamedEntityTagAnnotation])
        val offset = nextToken.beginPosition  

        if (previousNE.isDefined && previousNE.get.category == category)
          NamedEntity(previousNE.get.term + " " + term, category, previousNE.get.offset) +: result.tail
        else
          NamedEntity(term, category, offset) +: result     
      })
    }).flatten    
  }
  
  def parse(file: File): Seq[NamedEntity] =
    parse(Source.fromFile(file).getLines().mkString("\n"))
  
}

