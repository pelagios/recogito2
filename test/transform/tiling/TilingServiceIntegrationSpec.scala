package transform.tiling

import akka.actor.ActorSystem
import akka.testkit.{ TestKit, ImplicitSender }
import java.io.File
import java.util.UUID
import java.sql.Timestamp
import services.ContentType
import services.task.{ TaskService, TaskStatus }
import services.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.apache.commons.io.FileUtils
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.AfterAll
import org.junit.runner._
import play.api.Logger
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Await
import scala.concurrent.duration._
import services.task.TaskStatus

@RunWith(classOf[JUnitRunner])
class TilingServiceIntegrationSpec extends TestKit(ActorSystem()) with ImplicitSender with SpecificationLike with AfterAll {

  // Force Specs2 to execute tests in sequential order
  sequential

  private val DEST_DIR = new File("test/resources/transform/tiling/Ptolemy_map_15th_century")

  private val TMP_IDX_DIR = "test/resources/services/place/tmp-idx"

  override def afterAll = {
    FileUtils.deleteDirectory(DEST_DIR)
    FileUtils.deleteDirectory(new File(TMP_IDX_DIR))
  }

  val app = new GuiceApplicationBuilder()
    .configure(Map(
      "recogito.index.dir" -> TMP_IDX_DIR,
      "db.default.driver" -> "org.postgresql.Driver",
      "db.default.url" -> "jdbc:postgresql://localhost/recogito2-test",
      "db.default.username" -> "postgres",
      "db.default.password" -> "postgres"
    ))
    .build();

  val tilingService = app.injector.instanceOf(classOf[TilingService])

  val taskService = app.injector.instanceOf(classOf[TaskService])

  "The Tiling service" should {

    FileUtils.deleteDirectory(DEST_DIR)

    val KEEPALIVE = 10.seconds

    val document = new DocumentRecord("hcylkmacy4xgkb", "rainer", new Timestamp(System.currentTimeMillis), "A test image", null, null, null, null, null, null, null, null, false, null)
    val parts = Seq(new DocumentFilepartRecord(UUID.randomUUID, "hcylkmacy4xgkb", "Ptolemy_map_15th_century.jpg", ContentType.IMAGE_UPLOAD.toString, "Ptolemy_map_15th_century.jpg", 0, null))
    val dir = new File("test/resources/transform/tiling")

    val processStartTime = System.currentTimeMillis
    tilingService.spawnTask(document, parts, dir, Map.empty[String, String], KEEPALIVE)

    "start tiling on the test image without blocking" in {
      (System.currentTimeMillis - processStartTime).toInt must be <(1000)
    }

    "report progress until complete" in {
      var ctr = 50
      var isComplete = false

      while (ctr > 0 && !isComplete) {
        ctr -= 1

        val queryStartTime = System.currentTimeMillis

        val result = Await.result(taskService.findByDocument(document.getId), 10 seconds)

        result.isDefined must equalTo(true)
        result.get.taskRecords.size must equalTo(1)

        val progress = result.get
        Logger.info("[TilingServiceIntegrationSpec] Image tiling progress is " + progress.status + " - " + progress.progress)

        if (progress.progress == 100)
          isComplete = true

        Thread.sleep(2000)
      }

      success
    }

    "have created the image tileset" in {
      DEST_DIR.exists must equalTo(true)
      DEST_DIR.list.size must equalTo(2)
      new File(DEST_DIR, "ImageProperties.xml").exists must equalTo(true)

      val tileGroup0 = new File(DEST_DIR, "TileGroup0")
      tileGroup0.exists must equalTo(true)

      tileGroup0.list.size must equalTo(65)
      tileGroup0.list.filter(_.endsWith(".jpg")).size must equalTo(65)
    }

    "accept progress queries after completion" in {
      val queryStartTime = System.currentTimeMillis

      val result = Await.result(taskService.findByDocument(document.getId), 10 seconds)

      (System.currentTimeMillis - queryStartTime).toInt must be <(500)

      result.isDefined must equalTo(true)
      result.get.taskRecords.size must equalTo(1)
      result.get.progress must equalTo(100)
      result.get.status must equalTo(TaskStatus.COMPLETED)
    }

    "reject progress queries after the KEEPALIVE time has expired" in {
      Thread.sleep(KEEPALIVE.toMillis)
      Logger.info("[TilingServiceIntegrationSpec] KEEPALIVE expired")

      val result = Await.result(taskService.findByDocument(document.getId), 10 seconds)

      result.isDefined must equalTo(false)
    }

  }

}
