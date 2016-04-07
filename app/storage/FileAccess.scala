package storage

import controllers.HasApplication
import java.io.File
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.{ Logger, Play }
import scala.io.Source

/** Base functionality for using files uploaded by users **/
trait FileAccess {

  private val A_TO_Z =
    (('a' to 'z') ++ ('A' to 'Z')).toSet

  private lazy val UPLOAD_BASE = {
    val dir = Play.current.configuration.getString("recogito.upload.dir") match {
      case Some(filename) => new File(filename)
      case None => new File("uploads") // Default
    }
    
    Logger.info("Using " + dir.getAbsolutePath + " as upload location")

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

  protected def getUserDir(username: String, createIfNotExists: Boolean = false): Option[File] = {
    val alphabeticCharsOnly = username.filter(A_TO_Z.contains(_))

    // User folders are contained in common parent folder. The name of that folder is
    // the first two alphabetic characters of the user name. Example:
    // /user-data/ra/rainer/...
    val parentFolder =
      if (alphabeticCharsOnly.size < 2)
        // Anyone funny enough to insist on a username with fewer than two alphabetic
        // characters is kept in an extra parent folder
        new File(USER_DATA_DIR, "_anomalies")
      else
        new File(USER_DATA_DIR, alphabeticCharsOnly.substring(0, 2))

    val userFolder = new File(parentFolder, username)

    if (createIfNotExists) {
      if (!parentFolder.exists)
        parentFolder.mkdir()

      if (!userFolder.exists)
        userFolder.mkdir()

      Some(userFolder)
    } else if (userFolder.exists) {
      Some(userFolder)
    } else {
      None
    }
  }

  protected def getDocumentDir(owner: String, docId: String, createIfNotExists: Boolean = false): Option[File] =
    getUserDir(owner, createIfNotExists).flatMap(userDir => {
      val documentDir = new File(userDir, docId)
      if (createIfNotExists) {
        if (!documentDir.exists)
          documentDir.mkdir()
        Some(documentDir)
      } else if (documentDir.exists) {
        Some(documentDir)
      } else {
        None
      }
    })
    
  /** Helper to read the contents of a text filepart **/
  protected def readTextfile(owner: String, docId: String, filename: String): Option[String] =
    getDocumentDir(owner, docId).flatMap(dir => {
      val file = new File(dir, filename)
      if (file.exists) {
        Some(Source.fromFile(file).getLines.mkString("\n"))
      } else {
        None
      }
    })
    
  /** Helper **/
  protected def openThumbnail(owner: String, docId: String, filename: String): Option[File] = 
    getDocumentDir(owner, docId).flatMap(dir => {
      val tilesetDir = new File(dir, filename.substring(0, filename.lastIndexOf('.')))
      if (tilesetDir.exists) { 
        val thumbnail = new File(tilesetDir, "TileGroup0/0-0-0.jpg")
        if (thumbnail.exists)
          Some(thumbnail)
        else
          None
      } else {
        None        
      }
    })

}
