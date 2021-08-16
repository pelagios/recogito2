package transform.mapkurator

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import java.io.File
import javax.inject.{Inject, Singleton}
import scala.language.postfixOps
import services.ContentType
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.{TaskService, TaskType}
import storage.uploads.Uploads
import sys.process._
import transform.{WorkerActor, WorkerService}

@Singleton
class MapKuratorService @Inject() (
  uploads: Uploads,
  taskService: TaskService, 
  system: ActorSystem
) extends WorkerService(
  system, uploads,
  MapKuratorActor.props(taskService), 4
)      

object MapKuratorService {

  val TASK_TYPE = TaskType("MAPKURATOR")
  
  private[mapkurator] def callMapkurator(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File,
    jobDef: MapKuratorJobDefinition
  ) = {
    // TODO config option!
    val TOOL_PATH = "/home/simonr/Workspaces/mrm/map-kurator"
    
    val filename = part.getFile
    val contentType = ContentType.withName(part.getContentType)

    contentType match {
      case Some(ContentType.IMAGE_IIIF) =>
        play.api.Logger.info("Launching mapKurator - IIIF image")

        val cli = s"docker run -v $TOOL_PATH/data/:/map-kurator/data -v $TOOL_PATH/model:/map-kurator/model --rm --workdir=/map-kurator map-kurator python model/predict_annotations.py iiif --url=$filename --dst=data/test_imgs/sample_output/"
        // play.api.Logger.info(cli)   

        val result =  cli !!

        play.api.Logger.info(result)

      case Some(ContentType.IMAGE_UPLOAD) =>
        val f = new File(dir, filename)

      case Some(ContentType.MAP_WMTS) =>
        play.api.Logger.info("Launching mapKurator - WMTS")

        if (Seq(jobDef.minLon, jobDef.minLat, jobDef.maxLon, jobDef.maxLat).exists(_.isEmpty)) {
          play.api.Logger.error("Cannot process WMTS map without bbox")
        } else {
          val minLon = jobDef.minLon.get
          val minLat = jobDef.minLat.get
          val maxLon = jobDef.maxLon.get
          val maxLat = jobDef.maxLat.get

          val bounds = s"""{"type":"Feature","properties":{},"geometry":{"type":"Polygon","coordinates":[[[$minLon,$minLat],[$maxLon,$minLat],[$maxLon,$maxLat],[$minLon,$maxLat],[$minLon,$minLat]]]}}"""
          val cli = s"docker run -v $TOOL_PATH/data/:/map-kurator/data -v $TOOL_PATH/model:/map-kurator/model --rm  --workdir=/map-kurator map-kurator python model/predict_annotations.py wmts --url=$filename --boundary=$bounds --zoom=16 --dst=data/test_imgs/sample_output/"
          play.api.Logger.info(cli)

          val result = cli !!

          play.api.Logger.info(result)
        }

        throw new Exception(s"Unsupported")


      case _ =>
        throw new Exception(s"Unsupported content type $contentType")
    }

  }
  
}