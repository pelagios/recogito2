package models.document

import java.net.URI

sealed trait License { val name: String; val acronym: String; val uri: Option[URI] }

case object CC0 extends License { 
  val name    = "CC0 1.0 Universal (CC0 1.0)"
  val acronym = "CC0"
  val uri     = Some(new URI("http://creativecommons.org/publicdomain/zero/1.0/")) 
}

case object CC_BY extends License {
  val name    = "CC Attribution 4.0 International (CC BY 4.0)"
  val acronym = "CC BY"
  val uri     = Some(new URI("http://creativecommons.org/licenses/by/4.0/"))
}

case object CC_BY_SA extends License {
  val name    = "CC Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)"
  val acronym = "CC BY-SA"
  val uri     = Some(new URI("http://creativecommons.org/licenses/by-sa/4.0/"))
}

case object CC_BY_NC extends License {
  val name    = "CC Attribution-NonCommerical 4.0 International (CC BY-NC 4.0)"
  val acronym = "CC BY-NC"
  val uri     = Some(new URI("http://creativecommons.org/licenses/by-nc/4.0/"))
}

case object CC_BY_NC_SA extends License {
  val name    = "CC Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)"
  val acronym = "CC BY-NC-SA"
  val uri     = Some(new URI("http://creativecommons.org/licenses/by-nc-sa/4.0/"))
}

case object OUT_OF_COPYRIGHT extends License {
  val name    = "Out of copyright in territory of publication"
  val acronym = "OUT OF COPYRIGHT"
  val uri     = None
}

case object IN_COPYRIGHT extends License {
  val name    = "In copyright in territory of publication"
  val acronym = "IN COPYRIGHT"
  val uri     = None
}

case object OTHER extends License {
  val name    = "Other"
  val acronym = "OTHER"
  val uri     = None
}

case object DONT_KNOW extends License {
  val name    = "I don't know"
  val acronym = "DONT_KNOW"
  val uri     = None
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
    OTHER,
    DONT_KNOW
  ) 
  
  def fromAcronym(acronym: String) = values.find(_.acronym == acronym)
  
}