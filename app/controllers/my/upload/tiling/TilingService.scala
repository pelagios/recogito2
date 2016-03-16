package controllers.my.upload.tiling

import java.io.File
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.language.postfixOps
import sys.process._

object TilingService {
  
  private[tiling] def createZoomify(file: File, destFolder: File): Future[Unit] = {
    Future {
      s"vips dzsave $file $destFolder --layout zoomify" !
    } map { result =>
      if (result == 0)
        Unit
      else
        throw new Exception("Image tiling failed for " + file.getAbsolutePath + " to " + destFolder.getAbsolutePath)
    }
  }
  
}