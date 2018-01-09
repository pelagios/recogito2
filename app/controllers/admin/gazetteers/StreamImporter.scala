package controllers.admin.gazetteers

import akka.stream.{ActorAttributes, ClosedShape, Materializer, Supervision}
import akka.stream.scaladsl._
import akka.util.ByteString
import java.io.InputStream
import models.place.{GazetteerRecord, PlaceService}
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class StreamImporter(implicit materializer: Materializer) {
  
  private val BATCH_SIZE = 100
  
  private val decider: Supervision.Decider = {    
    case t: Throwable => 
      t.printStackTrace()
      Supervision.Stop    
  }
  
  def importPlaces(is: InputStream, crosswalk: String => Option[GazetteerRecord])(implicit places: PlaceService, ctx: ExecutionContext) = {
    
    val source = StreamConverters.fromInputStream(() => is, 1024)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = Int.MaxValue, allowTruncation = false))
      .map(_.utf8String)
      
    val parser = Flow.fromFunction[String, Option[GazetteerRecord]](crosswalk)
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
      .grouped(BATCH_SIZE)
      
    val importer = Sink.foreach[Seq[Option[GazetteerRecord]]] { records =>
      val toImport = records.flatten
      Await.result(places.importRecords(toImport), 60.minutes)
    }
    
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      
      import GraphDSL.Implicits._
      
      source ~> parser ~> importer
      
      ClosedShape
    }).withAttributes(ActorAttributes.supervisionStrategy(decider))
        
    graph.run()
  }
  
}