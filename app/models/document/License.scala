package models.document

import java.net.URI

sealed trait License { val name: String; val acronym: String; val uri: URI }

case object CC0 extends License { 
  val name    = "CC0 1.0 Universal (CC0 1.0)"
  val acronym = "CC0"
  val uri     = new URI("http://creativecommons.org/publicdomain/zero/1.0/") 
}

case object CC_BY extends License {
  val name    = "CC Attribution 4.0 International (CC BY 4.0)"
  val acronym = "CC BY"
  val uri     = new URI("http://creativecommons.org/licenses/by/4.0/")
}

case object CC_BY_SA extends License {
  val name    = "CC Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)"
  val acronym = "CC BY-SA"
  val uri     = new URI("http://creativecommons.org/licenses/by-sa/4.0/")
}

case object CC_BY_NC extends License {
  val name    = "CC Attribution-NonCommerical 4.0 International (CC BY-NC 4.0)"
  val acronym = "CC BY-NC"
  val uri     = new URI("http://creativecommons.org/licenses/by-nc/4.0/")
}

case object CC_BY_NC_SA extends License {
  val name    = "CC Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)"
  val acronym = "CC BY-NC-SA"
  val uri     = new URI("http://creativecommons.org/licenses/by-nc-sa/4.0/")
}

object License {
  
  lazy val values: Seq[License] = Seq(CC0, CC_BY, CC_BY_SA, CC_BY_NC, CC_BY_NC_SA) 
  
  def fromAcronym(acronym: String) = values.find(_.acronym == acronym)
  
  def fromURI(uri: URI) = values.find(_.uri == uri)
  
}