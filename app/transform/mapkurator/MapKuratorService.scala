package transform.mapkurator

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import java.io.File
import java.nio.file.{Files, StandardCopyOption} 
import javax.inject.{Inject, Singleton}
import scala.language.postfixOps
import services.ContentType
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.annotation.AnnotationService
import services.task.{TaskService, TaskType}
import storage.uploads.Uploads
import sys.process._
import transform.{WorkerActor, WorkerService}

@Singleton
class MapKuratorService @Inject() (
  annotationService: AnnotationService,
  uploads: Uploads,
  taskService: TaskService, 
  system: ActorSystem
) extends WorkerService(
  system, uploads,
  MapKuratorActor.props(taskService, annotationService), 4
)      

object MapKuratorService {

  val TASK_TYPE = TaskType("MAPKURATOR")
  
  private[mapkurator] def callMapkurator(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File,
    jobDef: MapKuratorJobDefinition
  ): File = {
    // Supply this via a config option!
    val TOOL_PATH = "/home/simonr/Workspaces/mrm/map-kurator"

    val SOURCE_FOLDER = s"$TOOL_PATH/data/test_imgs/sample_input"
    val DEST_FOLDER = s"$TOOL_PATH/data/test_imgs/sample_output"

    // The relative path inside the mapKurator Docker container where
    // uploaded images are copied to
    val WORKDIR_PATH = "data/test_imgs/sample_input"
    
    val filename = part.getFile
    val contentType = ContentType.withName(part.getContentType)

    contentType match {
      case Some(ContentType.IMAGE_IIIF) =>
        play.api.Logger.info("Launching mapKurator - IIIF image")

        val cli = s"docker run -v $TOOL_PATH/data/:/map-kurator/data -v $TOOL_PATH/model:/map-kurator/model --rm --workdir=/map-kurator map-kurator python model/predict_annotations.py iiif --url=$filename --dst=data/test_imgs/sample_output/"
        // play.api.Logger.info(cli)   

        val result =  cli !!

        // Just for testing
        val resultFile = new File(s"$TOOL_PATH/data/test_imgs/sample_output/bdfc4fd9-14fe-4e1a-8942-52cfd56a0d02_annotations.json")
        resultFile

      case Some(ContentType.IMAGE_UPLOAD) =>
        // 1. Copy input file to SOURCE_FOLDER
        val source = new File(dir, filename).toPath().toAbsolutePath() 
        val destination = new File(SOURCE_FOLDER, filename).toPath().toAbsolutePath()
        val partId = part.getId

        play.api.Logger.info("part id " + partId)

        val workingCopy = new File(WORKDIR_PATH, filename)

        try {
          play.api.Logger.info("Copying from file: " + source.toString() + " to " + destination.toString())
          Files.copy(source, destination)
          play.api.Logger.info("Starting mapKurator")

          // 2. Run mapKurator
          val cli = s"docker run -v $TOOL_PATH/data/:/map-kurator/data -v $TOOL_PATH/model:/map-kurator/model --rm --workdir=/map-kurator map-kurator python model/predict_annotations.py file --src=$workingCopy --dst=data/test_imgs/sample_output/ --filename=$partId"
          play.api.Logger.info(cli)   

          val result =  cli !!

          // 3. Cleanup
          Files.delete(destination);

          play.api.Logger.info("mapKurator completed: " + result)

          // Just for testing
          val resultFile = new File(s"$TOOL_PATH/data/test_imgs/sample_output/${partId}_annotations.json")
          resultFile
        } catch { 
          case t: Throwable => 
            t.printStackTrace() 
            throw t
        }

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

          null
        }

        throw new Exception(s"Unsupported")


      case _ =>
        throw new Exception(s"Unsupported content type $contentType")
    }

  }
  
}