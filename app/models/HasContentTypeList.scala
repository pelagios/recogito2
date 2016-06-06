package models

trait HasContentTypeList {
  
  // JSON representation converts ContentType to an array [ MediaType, ContentType ]
  // so we can use it for analytics more easily. E.g. 'TEXT_PLAIN' -> [ 'TEXT', 'TEXT_PLAIN' ] 
  def fromCTypeList(list: Seq[String]): ContentType =
    list.flatMap(ContentType.withName(_)).head
    
  def toCTypeList(ctype: ContentType): Seq[String] =
    Seq(ctype.media, ctype.name)
  
}