package transform.mapkurator

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import java.io.File
import java.nio.file.{Files, StandardCopyOption} 
import javax.inject.{Inject, Singleton}
import play.api.Configuration
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
  system: ActorSystem,
  config: Configuration
) extends WorkerService(
  system, uploads,
  MapKuratorActor.props(taskService, annotationService, config), 4
)      

object MapKuratorService {

  val TASK_TYPE = TaskType("MAPKURATOR")
  
  private[mapkurator] def callMapkurator(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File,
    jobDef: MapKuratorJobDefinition,
    config: Configuration
  ): File = {

    val TOOL_PATH = config.get[String]("mapkurator.path")

    val SOURCE_FOLDER = s"$TOOL_PATH/data/test_imgs/sample_input"
    val DEST_FOLDER = s"$TOOL_PATH/data/test_imgs/sample_output"

    // The relative path inside the mapKurator Docker container where
    // uploaded images are copied to
    val WORKDIR_PATH = "data/test_imgs/sample_input"
    
    val filename = part.getFile
    val contentType = ContentType.withName(part.getContentType)

    contentType match {
      case Some(ContentType.IMAGE_IIIF) =>
        play.api.Logger.info("Starting mapKurator - IIIF harvest")

        // 1. Start mapKurator processing
        val cli = s"docker run -v $TOOL_PATH/data/:/map-kurator/data -v $TOOL_PATH/model:/map-kurator/model --rm --workdir=/map-kurator map-kurator python model/predict_annotations.py iiif --url=$filename --dst=data/test_imgs/sample_output/ --filename=${part.getId}" 

        val result =  cli !!

        // 2. Cleanup
        val stitched = new File(s"$TOOL_PATH/data/test_imgs/sample_output/${part.getId}_stitched.jpg").toPath().toAbsolutePath()
        val predictions = new File(s"$TOOL_PATH/data/test_imgs/sample_output/${part.getId}_predictions.jpg").toPath().toAbsolutePath() 

        Files.delete(stitched)
        Files.delete(predictions)

        play.api.Logger.info("mapKurator completed")

        // 3. Return result file
        new File(s"$TOOL_PATH/data/test_imgs/sample_output/${part.getId}_annotations.json")

      case Some(ContentType.IMAGE_UPLOAD) =>
        try {
          // 1. Copy file from Recogito upload area to mapKurator staging dir
          val source = new File(dir, filename).toPath().toAbsolutePath() 
          val workingCopy = new File(SOURCE_FOLDER, filename).toPath().toAbsolutePath()
          play.api.Logger.info("Copying from file: " + source.toString() + " to " + workingCopy.toString())
          Files.copy(source, workingCopy)

          // 2. Start mapKurator processing
          play.api.Logger.info("Starting mapKurator - image upload")

          val dockerPath = new File(WORKDIR_PATH, filename)
          val cli = s"docker run -v $TOOL_PATH/data/:/map-kurator/data -v $TOOL_PATH/model:/map-kurator/model --rm --workdir=/map-kurator map-kurator python model/predict_annotations.py file --src=$dockerPath --dst=data/test_imgs/sample_output/ --filename=${part.getId}"

          val result =  cli !!

          // 3. Cleanup
          val predictions = new File(s"$TOOL_PATH/data/test_imgs/sample_output/${part.getId}_predictions.jpg").toPath().toAbsolutePath() 
          Files.delete(predictions)
          Files.delete(workingCopy)

          play.api.Logger.info("mapKurator completed")

          // 4. Return result file
          new File(s"$TOOL_PATH/data/test_imgs/sample_output/${part.getId}_annotations.json")
        } catch { 
          case t: Throwable => 
            t.printStackTrace() 
            throw t
        }

      case Some(ContentType.MAP_WMTS) =>
        play.api.Logger.info("Starting mapKurator - WMTS harvest")

        if (Seq(jobDef.minLon, jobDef.minLat, jobDef.maxLon, jobDef.maxLat).exists(_.isEmpty)) {
          throw new Exception("Cannot process WMTS map without bbox")
        } else {
          // 1. Get geo bounding box
          val minLon = jobDef.minLon.get
          val minLat = jobDef.minLat.get
          val maxLon = jobDef.maxLon.get
          val maxLat = jobDef.maxLat.get

          // 2. Start mapKurator processing
          val bounds = s"""{"type":"Feature","properties":{},"geometry":{"type":"Polygon","coordinates":[[[$minLon,$minLat],[$maxLon,$minLat],[$maxLon,$maxLat],[$minLon,$maxLat],[$minLon,$minLat]]]}}"""
          val cli = s"docker run -v $TOOL_PATH/data/:/map-kurator/data -v $TOOL_PATH/model:/map-kurator/model --rm  --workdir=/map-kurator map-kurator python model/predict_annotations.py wmts --url=$filename --boundary=$bounds --zoom=16 --dst=data/test_imgs/sample_output/ --filename=${part.getId}"

          val result = cli !!

          // 3. Cleanup
          val stitched = new File(s"$TOOL_PATH/data/test_imgs/sample_output/${part.getId}_stitched.jpg").toPath().toAbsolutePath() 
          val predictions = new File(s"$TOOL_PATH/data/test_imgs/sample_output/${part.getId}_predictions.jpg").toPath().toAbsolutePath() 
          
          Files.delete(stitched)
          Files.delete(predictions)

          play.api.Logger.info("mapKurator completed")

          // 4. Return result file
          new File(s"$TOOL_PATH/data/test_imgs/sample_output/${part.getId}_annotations.json")
        }

      case _ =>
        throw new Exception(s"Unsupported content type $contentType")

    }

  }
  
}