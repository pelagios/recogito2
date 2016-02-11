package controllers.myrecogito.upload

import akka.actor.{ Actor, ActorSystem, Props }
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{ Annotation, StanfordCoreNLP }
import java.io.File
import java.util.Properties
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits._
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.io.Source

case class NamedEntity(term: String, category: String, offset: Int)

class GeoParseActor(text: String) extends Actor {
  
  import GeoParseActor._
  
  private var totalProgress = 0.0
  
  private var currentPhase = "INITIALIZING"
    
  def receive = {
    
    case Start => Future { 
      val results = GeoParser.parse(text)
    }
    
    case QueryProgress => sender() ! Progress(totalProgress, currentPhase)
    
  }
  
}

object GeoParseActor {

  sealed abstract trait Message
  
  case object Start extends Message
  case object QueryProgress extends Message
  case class Progress(value: Double, currentPhase: String) extends Message

}

class GeoParser(implicit system: ActorSystem) {
  
  import GeoParseActor._
  
  def parseAsync(file: File) = {
    val text = Source.fromFile(file).getLines().mkString("\n")
    val props = Props(classOf[GeoParseActor], text)
    val actor = system.actorOf(props) 
    actor ! Start
  }
  
}

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
  
}

