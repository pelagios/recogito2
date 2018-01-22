package controllers.admin.gazetteers

import akka.stream.{ActorAttributes, ClosedShape, Materializer, Supervision}
import akka.stream.scaladsl._
import akka.util.ByteString
import java.io.InputStream
import services.entity.EntityRecord
import services.entity.importer.EntityImporter
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class StreamLoader(implicit materializer: Materializer) {
  
  private val BATCH_SIZE = 100
  
  private val decider: Supervision.Decider = {    
    case t: Throwable => 
      t.printStackTrace()
      Supervision.Stop    
  }
  
  def importPlaces(is: InputStream, crosswalk: String => Option[EntityRecord], importer: EntityImporter)(implicit ctx: ExecutionContext) = {
    
    val source = StreamConverters.fromInputStream(() => is, 1024)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = Int.MaxValue, allowTruncation = false))
      .map(_.utf8String)
      
    val parser = Flow.fromFunction[String, Option[EntityRecord]](crosswalk)
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
      .grouped(BATCH_SIZE)
      
    val sink = Sink.foreach[Seq[Option[EntityRecord]]] { records =>
      val toImport = records.flatten
      Await.result(importer.importRecords(toImport), 60.minutes)
    }
    
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      
      import GraphDSL.Implicits._
      
      source ~> parser ~> sink
      
      ClosedShape
    }).withAttributes(ActorAttributes.supervisionStrategy(decider))
        
    graph.run()
  }
  
}