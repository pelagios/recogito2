package controllers.admin.gazetteers

import akka.stream.{ ClosedShape, Materializer }
import akka.stream.scaladsl._
import akka.util.ByteString
import java.io.InputStream
import models.place.{ GazetteerRecord, PlaceService }
import scala.concurrent.ExecutionContext
import play.api.libs.json.Json

class StreamImporter(implicit materializer: Materializer) {
  
  private val BATCH_SIZE = 200
  
  def importPlaces(is: InputStream, crosswalk: String => GazetteerRecord)(implicit places: PlaceService, ctx: ExecutionContext) = {
    
    val source = StreamConverters.fromInputStream(() => is, 5)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = Int.MaxValue, allowTruncation = false))
      .map(_.utf8String)
      
    val parser = Flow.fromFunction[String, GazetteerRecord](crosswalk).grouped(BATCH_SIZE)
    
    val importer = Sink.foreach[Seq[GazetteerRecord]] { records =>
      play.api.Logger.info("Importing batch")
      places.importRecords(records)
    }
    
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      
      import GraphDSL.Implicits._
      
      source ~> parser ~> importer
      
      ClosedShape
    })
    
    graph.run()    
  }
  
}