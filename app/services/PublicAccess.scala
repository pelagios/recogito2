package services

object PublicAccess {
  
  sealed trait Visibility
  case object PRIVATE   extends Visibility
  case object WITH_LINK extends Visibility
  case object PUBLIC    extends Visibility
    
  sealed trait AccessLevel
  case object READ_ALL  extends AccessLevel
  case object READ_DATA extends AccessLevel
  case object WRITE     extends AccessLevel

  // De-serialization
  object Visibility {
    
    lazy val lookup: Map[String, Visibility] = 
      Set(PRIVATE, WITH_LINK, PUBLIC).map(v => (v.toString, v)).toMap
        
    // public_visibility is a NOT NULL column
    def withName(str: String) = lookup.get(str).get
    
  }
  
  object AccessLevel {
    
    lazy val lookup: Map[String, AccessLevel] = 
      Set(READ_ALL, READ_DATA, WRITE).map(v => (v.toString, v)).toMap
      
    // public_access_level might be null due to JOOQ Java API!
    def withName(str: String) = 
      if (str == null) 
        None
      else
        // In order to force exceptions on invalid DB values
        Some(lookup.get(str).get)
    
  }
  
}