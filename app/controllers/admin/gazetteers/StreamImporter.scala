package controllers.admin.gazetteers

import akka.stream.{ ActorAttributes, ClosedShape, Materializer, Supervision }
import akka.stream.scaladsl._
import akka.util.ByteString
import java.io.InputStream
import models.place.{ GazetteerRecord, PlaceService }
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._
import play.api.libs.json.Json

class StreamImporter(implicit materializer: Materializer) {
  
  private val BATCH_SIZE = 200
  
  private val decider: Supervision.Decider = {    
    case t: Throwable => 
      t.printStackTrace()
      Supervision.Stop    
  }
  
  def importPlaces(is: InputStream, crosswalk: String => Option[GazetteerRecord])(implicit places: PlaceService, ctx: ExecutionContext) = {
    
    val source = StreamConverters.fromInputStream(() => is, 1024)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = Int.MaxValue, allowTruncation = false))
      .map(_.utf8String)
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
      
    val parser = Flow.fromFunction[String, Option[GazetteerRecord]](crosswalk)
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
      .grouped(BATCH_SIZE)
    
    val importer = Sink.foreach[Seq[Option[GazetteerRecord]]] { records =>
      val toImport = records.flatten
      play.api.Logger.info("Importing " + toImport.size + " records")
      Await.result(places.importRecords(toImport), 1.minute)
    }
    
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      
      import GraphDSL.Implicits._
      
      source ~> parser ~> importer
      
      ClosedShape
    })
    
    graph.run()    
  }
  
}