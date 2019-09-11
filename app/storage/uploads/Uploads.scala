package storage.uploads

import java.io.File
import java.nio.file.Files
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import org.apache.commons.io.FileUtils

@Singleton
class Uploads @Inject() (config: Configuration) {

  private val A_TO_Z =
    (('a' to 'z') ++ ('A' to 'Z')).toSet

  private lazy val UPLOAD_BASE = {
    val dir = config.getOptional[String]("recogito.upload.dir") match {
      case Some(filename) => new File(filename)
      case None => new File("uploads") // Default
    }
    val path = dir.toPath

    Logger.info(s"Using ${path} as upload location")

    if (!Files.exists(path))
      Logger.info(s"Creating ${path}: ${dir.mkdir()}")
    else
      Logger.info(s"${path} exists")

    dir
  }

  lazy val USER_DATA_DIR = {
    val dir = new File(UPLOAD_BASE, "user_data")
    val path = dir.toPath

    if (!Files.exists(path))
      Logger.info(s"Creating ${path}: ${dir.mkdir()}")
    else 
      Logger.info(s"${path} exists")

    dir
  }

  lazy val PENDING_UPLOADS_DIR = {
    val dir = new File(UPLOAD_BASE, "pending")
    val path = dir.toPath

    if (!Files.exists(dir.toPath))
      Logger.info(s"Creating ${path}: ${dir.mkdir()}")
    else 
      Logger.info(s"${path} exists")

    dir
  }

  def getUserDir(username: String, createIfNotExists: Boolean = false): Option[File] = {
    val alphabeticCharsOnly = username.filter(A_TO_Z.contains(_))

    // User folders are contained in common parent folder. The name of that folder is
    // the first two lowercase alphabetic characters of the user name. Example:
    // /user-data/ra/rainer/...
    val parentFolder =
      if (alphabeticCharsOnly.size < 2)
        // Anyone funny enough to insist on a username with fewer than two alphabetic
        // characters is kept in an extra parent folder
        new File(USER_DATA_DIR, "_anomalies")
      else
        new File(USER_DATA_DIR, alphabeticCharsOnly.substring(0, 2).toLowerCase)

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

  /** TODO make async **/
  def getUsedDiskspaceKB(username: String) =
    getUserDir(username).map(dataDir => FileUtils.sizeOfDirectory(dataDir)).getOrElse(0l) / 1024

  def deleteUserDir(username: String)(implicit ctx: ExecutionContext): Future[Unit] = Future {
    scala.concurrent.blocking {
      getUserDir(username).map(userdir => FileUtils.deleteDirectory(userdir))
    }
  }

  def getDocumentDir(owner: String, docId: String, createIfNotExists: Boolean = false): Option[File] =
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
  def readTextfile(owner: String, docId: String, filename: String)(implicit ctx: ExecutionContext): Future[Option[String]] = Future {
    scala.concurrent.blocking {
      getDocumentDir(owner, docId).flatMap(dir => {
        val file = new File(dir, filename)
        if (file.exists)
          Some(Source.fromFile(file).getLines.mkString("\n"))
        else
          None
      })
    }
  }

  /** Helper to get the filesize in bytes.  
    * 
    * Returns -1 if the file doesn't exist.
    */
  def getFilesize(owner: String, docId: String, filename: String)(implicit ctx: ExecutionContext): Future[Long] = Future {
    scala.concurrent.blocking {
      getDocumentDir(owner, docId).map(dir => {
        val file = new File(dir, filename)
        if (file.exists) file.length else -1l
      }).getOrElse(-1l)
    }
  }

  /** Helper **/
  def openThumbnail(owner: String, docId: String, filename: String)(implicit ctx: ExecutionContext): Future[Option[File]] = Future {
    scala.concurrent.blocking {
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
  }

  def getTotalSize()(implicit ctx: ExecutionContext) = Future {
    FileUtils.sizeOfDirectory(UPLOAD_BASE)
  }

}
