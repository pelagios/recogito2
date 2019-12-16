package controllers.document.downloads.serializers.document.tei

import controllers.document.downloads.serializers.BaseSerializer
import java.util.UUID
import services.annotation.{Annotation, AnnotationBody}
import services.annotation.AnnotationStatus._
import services.entity.{Entity, EntityRecord}

trait BaseTEISerializer extends BaseSerializer {
  
  /** Creates an "ID" that conforms to TEI restrictions **/
  def toTeiId(uuid: UUID): String = s"recogito-${uuid}"
  
  def getAttribute(tag: String) = {
    val sepIdx =
      if (tag.count(_ == ':') == 1)
        tag.indexOf(':')
      else 
        tag.indexOf(':', tag.indexOf(':') + 1)
      
    val key = tag.substring(1, sepIdx)
    val value = tag.substring(sepIdx + 1)
    (key, value)
  }
  
  /** Checks if this tag should be treated as XML attribute.
    * 
    * By convention, tags of the form '@key:value' are serialized
    * as xml attribute, e.g. <span key="value">.
    */
  def isAttributeTag(body: AnnotationBody) =
    body.value.map { value =>
      value.startsWith("@") &&
      value.contains(':')
    }.getOrElse(false)
      
  /** By convention, use all tags starting with @ as XML attributes **/
  def getAttributeTags(annotation: Annotation) =
    annotation.bodies.filter { body =>
      body.hasType == AnnotationBody.TAG && isAttributeTag(body)
    }.map { body => 
      getAttribute(body.value.get)
    }.groupBy { _._1 }.mapValues { _.map(_._2) }.toSeq
    
  /** All tags that don't fall into the 'attribute tag' convetion above **/
  def getNonAttributeTags(annotation: Annotation) = 
    annotation.bodies.filter { body =>
      body.hasType == AnnotationBody.TAG && !isAttributeTag(body)
    }.map { _.value.get }

  /** Returns a TEI @cert value, if relevant */
  def getCert(annotation: Annotation): Option[String] = {
    annotation.bodies.flatMap(_.status).headOption.map(_.value).map { _ match {
      case VERIFIED         => "high"
      case UNVERIFIED       => "low"
      case NOT_IDENTIFIABLE => "unknown"
    }}
  }

  def placesToList(annotations: Seq[Annotation], allPlaces: Seq[Entity]) = {
    // List of unique place URIs referred to in annotations
    val placeUris = annotations
      .flatMap(_.bodies)
      .filter(_.hasType == AnnotationBody.PLACE)
      .flatMap(_.uri)
      .toSet

    val referencedRecords = allPlaces
      .flatMap(_.isConflationOf)
      .filter(r => placeUris.contains(r.uri))

    if (referencedRecords.isEmpty) {
      None
    } else {
      // Use titles for local ID, but catch cases where titles aren't unique
      val groupedByTitle: Map[String, Seq[EntityRecord]] = referencedRecords.groupBy(_.title)

      Some(
        <listPlace>
          { referencedRecords.map { r => 
            val recordsWithThisTitle = groupedByTitle.get(r.title).getOrElse(Seq.empty)

            val localId = 
              if (recordsWithThisTitle.length == 1) {
                r.title 
              } else {
                // Should be a rare case
                val idx = recordsWithThisTitle.indexOf(r)
                s"${r.title}_${idx}"
              }

            val localIdClean = 
              localId.replaceAll("(\\s+|\\.|\\/)", "_")
              
            <place xml:id={localIdClean}>
              <placeName>{r.title}</placeName>
              <idno type="URI">{r.uri}</idno>
            </place>
          }}
        </listPlace>
      )
    }
  }

  def tagsToTaxonomy(annotations: Seq[Annotation]): Option[scala.xml.Elem] = {
    val distinctTags = annotations 
      .flatMap(_.bodies)
      .filter(_.hasType == AnnotationBody.TAG)
      .flatMap(_.value)
      .toSet

    if (distinctTags.isEmpty) {
      None
    } else {
      Some(
        <taxonomy>
          { distinctTags.map { t => 
            <category xml:id={t}>
              <catDesc>{t}</catDesc>
            </category>
          }}
        </taxonomy>
      )
    }
  }

  /** Generates a <listRelation> element for relations, if any are contained in the annotations **/
  def relationsToList(annotations: Seq[Annotation]) = 
    annotations.filter(_.relations.size > 0) match {
      case Seq() => None // Empty list
      
      case sourceAnnotations =>
        val relationElements = sourceAnnotations.flatMap { source =>
          source.relations.map { relation =>
            val name = relation.bodies.map(_.value.replaceAll(" ", "_")).mkString
            val active = toTeiId(source.annotationId)
            val passive = toTeiId(relation.relatesTo)
            <relation name={name} active={active} passive={passive} />
          }
        }
        
        Some(<ab>
          <listRelation>
            {relationElements}
          </listRelation>
        </ab>)
    }
  
}