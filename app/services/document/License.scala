package services.document

import java.net.URI

sealed trait License { val name: String; val acronym: String; val uri: Option[URI]; val isOpen: Boolean; val isCC: Boolean }

case object CC0 extends License { 
  val name    = "CC0 1.0 Universal (CC0 1.0)"
  val acronym = "CC0 1.0"
  val uri     = Some(new URI("http://creativecommons.org/publicdomain/zero/1.0/"))
  val isOpen  = true
  val isCC    = true
}

case object CC_BY extends License {
  val name    = "CC Attribution 4.0 International (CC BY 4.0)"
  val acronym = "CC BY 4.0"
  val uri     = Some(new URI("http://creativecommons.org/licenses/by/4.0/"))
  val isOpen  = true
  val isCC    = true
}

case object CC_BY_SA extends License {
  val name    = "CC Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)"
  val acronym = "CC BY-SA 4.0"
  val uri     = Some(new URI("http://creativecommons.org/licenses/by-sa/4.0/"))
  val isOpen  = true
  val isCC    = true
}

case object CC_BY_NC extends License {
  val name    = "CC Attribution-NonCommerical 4.0 International (CC BY-NC 4.0)"
  val acronym = "CC BY-NC 4.0"
  val uri     = Some(new URI("http://creativecommons.org/licenses/by-nc/4.0/"))
  val isOpen  = true
  val isCC    = true
}

case object CC_BY_NC_SA extends License {
  val name    = "CC Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)"
  val acronym = "CC BY-NC-SA 4.0"
  val uri     = Some(new URI("http://creativecommons.org/licenses/by-nc-sa/4.0/"))
  val isOpen  = true
  val isCC    = true
}

case object OUT_OF_COPYRIGHT extends License {
  val name    = "Out of copyright in territory of publication"
  val acronym = "OUT OF COPYRIGHT"
  val uri     = None
  val isOpen  = true
  val isCC    = false
}

case object IN_COPYRIGHT extends License {
  val name    = "In copyright in territory of publication"
  val acronym = "IN COPYRIGHT"
  val uri     = None
  val isOpen  = false
  val isCC    = false
}

case object OTHER extends License {
  val name    = "Other"
  val acronym = "OTHER"
  val uri     = None
  val isOpen  = false
  val isCC    = false
}

object License {
  
  lazy val values: Seq[License] = Seq(
    CC0,
    CC_BY,
    CC_BY_SA,
    CC_BY_NC,
    CC_BY_NC_SA,
    OUT_OF_COPYRIGHT,
    IN_COPYRIGHT,
    OTHER
  ) 
  
  def fromAcronym(acronym: String) = values.find(_.acronym == acronym)
  
}