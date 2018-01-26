package storage

import play.api.Configuration

object TempDir {
  
  /** Returns the path to the temp directory.
    *
    * Temp directory location can be configured via application.conf. If none
    * is set, this method will return the one set via the 'java.io.tmpdir'
    * system property.
    */
  def get()(implicit config: Configuration): String =
    config.getOptional[String]("recogito.tmp.dir") match {
      case Some(preconfigured) => "fpp"
      case None => System.getProperty("java.io.tmpdir")
    }
    
}