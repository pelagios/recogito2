package models

import java.io.File
import play.api.Play

/** Base functionality for services working with user uploads **/
trait AbstractFileService {
  
  private lazy val UPLOAD_BASE = {
    val dir = Play.current.configuration.getString("recogito.upload.dir") match {
      case Some(filename) => new File(filename)   
      case None => new File("uploads") // Default
    }
    
    if (!dir.exists)
      dir.mkdir()
    dir
  }
  
  protected lazy val USER_DATA_DIR = {
    val dir = new File(UPLOAD_BASE, "user_data")
    if (!dir.exists)
      dir.mkdir()
    dir
  }
    
  protected lazy val PENDING_UPLOADS_DIR = {
    val dir = new File(UPLOAD_BASE, "pending")
    if (!dir.exists)
      dir.mkdir()
    dir
  }
  
}