package controllers.document.downloads.serializers.annotations.csv

import java.io.StringReader

import javax.xml.namespace.NamespaceContext
import javax.xml.xpath.{XPathConstants, XPath, XPathFactory}

import collection.mutable.HashMap
import controllers.HasCSVParsing
import controllers.document.downloads.serializers.BaseSerializer
import java.nio.file.Paths
import java.util
import java.util.UUID

import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import kantan.csv.engine.commons._
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import play.api.{Configuration, Logger}
import play.api.libs.Files.TemporaryFileCreator
import services.ContentType

import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.document.ExtendedDocumentMetadata
import services.entity.{Entity, EntityType}
import services.entity.builtin.EntityService
import storage.uploads.Uploads
import storage.TempDir

trait AnnotationsToCSV extends BaseSerializer with HasCSVParsing { 

  private val EMPTY = ""
  
  private def findPlace(body: AnnotationBody, places: Seq[Entity]): Option[Entity] =
    body.uri.flatMap { uri =>
      places.find(_.uris.contains(uri))
    }

  private def parseAnchor(anchor:String) = {

    def separate(a: String): (String, Int) = {
      var path = a.substring(0, a.indexOf("::"))
        .replaceAll("tei/", "TEI/")
        .replaceAll("teiheader/", "teiHeader/")
        .replaceAll("filedesc/", "fileDesc/")
        .replaceAll("titlestmt/", "titleStmt/")
        .replaceAll("publicationstmt/", "publicationStmt/")
        .replaceAll("sourcedesc/", "sourceDesc/") // patching uppercase/lowercase inconsistencies (sigh)
        .replaceAll("(\\w)(/|$)", "$1[1]$2") // restore positional predicates to prevent ambiguity
        if (!path.startsWith("/TEI.2")) { // if it's P5, do NS stuff
          path = path.replaceAll("@id", "@xml:id") // restore id prefix
            .replaceAll("/(\\w)","/tei:$1") // add NS prefix so XPath works
        }

      val offset = a.substring(a.indexOf("::") + 2).toInt
      (path, offset)
    }

    separate(anchor.substring(5, anchor.indexOf(";")))
  }

  private def getXmlPosition(ann: Annotation, xpath: XPath, docs: Map[UUID, Document]) = {
    val anchor = parseAnchor(ann.anchor)
    xpath.reset()
    val textnodes = xpath.evaluate(anchor._1 + "/preceding::text()", docs(ann.annotates.filepartId), XPathConstants.NODESET).asInstanceOf[NodeList]
    if (textnodes.getLength > 0)
      (for (i <- 0.to(textnodes.getLength - 1))
        yield textnodes.item(i).getNodeValue.length).reduce((a, b) => a + b) + anchor._2
    else 
      anchor._2
  }

  private def parseXML(source: InputSource) = {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setNamespaceAware(true)
    val builder = factory.newDocumentBuilder()
    builder.parse(source)
  }

  private def parseXMLString(xml: String) = {
    parseXML(new InputSource(new StringReader(xml)))
  }

  private def sortByDocumentPosition(annotations: Seq[Annotation], xpath: XPath, docs: Map[UUID, Document]) = {
    var indexedAnnotations = annotations.map((a) => (getXmlPosition(a, xpath, docs), a))
    val groupedByDocument = indexedAnnotations.groupBy(_._2.annotates.filepartId)
    groupedByDocument.values.reduce((a, b) => a ++ b).sortWith {
      (c, d) =>
        c._1 < d._1
    }.map(v => v._2)
  }

  def annotationsToCSV(doc: ExtendedDocumentMetadata)(
    implicit annotationService: AnnotationService,
             entityService: EntityService,
             uploads: Uploads,
             tmpFile: TemporaryFileCreator,
             conf: Configuration,
             ctx: ExecutionContext
  ) = {

    def serializeOne(a: Annotation, filename: String, places: Seq[Entity]): Seq[String] = {
      val firstEntity = getFirstEntityBody(a)
      val maybePlace = firstEntity.flatMap(body => findPlace(body, places))
      
      val quoteOrTranscription = {
        if (a.annotates.contentType.isText)
          getFirstQuote(a)
        else if (a.annotates.contentType.isImage || a.annotates.contentType.isMap)
          getFirstTranscription(a)
        else None
      } map { _.replaceAll("[\\t\\n\\r]+"," ") }
        
      val placeTypes = maybePlace.map(_.subjects.map(_._1).mkString(","))

      val groupId = a.bodies.find(_.hasType == AnnotationBody.GROUPING).flatMap(_.value)

      val groupOrder = a.bodies.find(_.hasType == AnnotationBody.ORDERING).flatMap(_.value)

      // Helper to surface either the tag URI (if available) or the label otherwise
      def tagLabelsOrURIs(): Seq[String] =
        getTagBodies(a).flatMap(body => Seq(body.uri, body.value).flatten.headOption)

      Seq(a.annotationId.toString,
          filename,
          quoteOrTranscription.getOrElse(EMPTY),
          a.anchor,
          firstEntity.map(_.hasType.toString).getOrElse(EMPTY),
          firstEntity.flatMap(_.uri).getOrElse(EMPTY),
          maybePlace.map(_.titles.mkString("|")).getOrElse(EMPTY),
          maybePlace.flatMap(_.temporalBoundsUnion.map(_.toString)).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.y.toString)).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.x.toString)).getOrElse(EMPTY),
          maybePlace.map(_.subjects.map(_._1).mkString(",")).getOrElse(EMPTY),
          firstEntity.flatMap(_.status.map(_.value.toString)).getOrElse(EMPTY),
          tagLabelsOrURIs().mkString("|"),
          getCommentBodies(a).flatMap(_.value).mkString("|"),
          groupId.getOrElse(EMPTY),
          groupOrder.getOrElse(EMPTY))
    }

    val fAnnotationsByPart = Future.sequence {
      doc.fileparts.map { part => 
        annotationService.findByFilepartId(part.getId).map { annotationsWithId => 
          (part, annotationsWithId.map(_._1))
        }
      }
    }

    val fPlaces = entityService.listEntitiesInDocument(doc.id, Some(EntityType.PLACE))

    // For every part that's a TEI file, load the content from the file system
    val fXmlStrings: Future[Seq[(UUID, String)]] = Future.sequence {  
      doc.fileparts.map { _ match {

        case part if part.getContentType == ContentType.TEXT_TEIXML.toString => 
          uploads.readTextfile(doc.owner.getUsername, doc.id, part.getFile)
            .map { _.map(text => (part.getId, text)) }

        case _ => 
          Future.successful(None)
      }}
    } map { _.flatten }

    val f = for {
      annotationByPart <- fAnnotationsByPart
      places <- fPlaces
      xmlStrings <- fXmlStrings
    } yield (annotationByPart, places.items.map(_._1.entity), xmlStrings)

    f.map { case (annotationsByPart, places, xmlStrings) =>
      val (xpath, docs) = 
        if (xmlStrings.size > 0) {
          // Set up XPath resolver
          val xpath = XPathFactory.newInstance().newXPath()
          xpath.setNamespaceContext(new NamespaceContext {
            override def getNamespaceURI(prefix: String): String = {
              prefix match {
                case "tei" => "http://www.tei-c.org/ns/1.0"
                case "xml" => XMLConstants.XML_NS_URI
                case _ => null
              }
            }
            // not needed
            override def getPrefix(namespaceURI: String): String = ???
            // not needed
            override def getPrefixes(namespaceURI: String): util.Iterator[_] = ???
          })

          // Set up a Map of partId -> XML doc
          val docs = xmlStrings.map(t =>  (t._1, parseXMLString(t._2))).toMap
          (Some(xpath), Some(docs)) // Hmmm... not idea
        } else {
          (None, None)
        }

      def sort(annotations: Seq[Annotation]) = {
        val groupedByContentType = annotations.groupBy(_.annotates.contentType)

        groupedByContentType.flatMap { case (cType, a) => cType match {
          case ContentType.TEXT_PLAIN => sortByCharOffset(a)
          case ContentType.IMAGE_UPLOAD | ContentType.IMAGE_IIIF => sortByXY(a)
          case ContentType.DATA_CSV => sortByRow(a)
          case ContentType.TEXT_TEIXML => sortByDocumentPosition(a, xpath.get, docs.get)
          case _ => {
            Logger.warn(s"Can't sort annotations of unsupported content type $cType")
            a
          }
        }}
      }

      scala.concurrent.blocking {
        val header = Seq(
          "UUID",
          "FILE",
          "QUOTE_TRANSCRIPTION",
          "ANCHOR",
          "TYPE",
          "URI",
          "VOCAB_LABEL",
          "VOCAB_TEMPORAL_BOUNDS",
          "LAT",
          "LNG",
          "PLACE_TYPE",
          "VERIFICATION_STATUS",
          "TAGS",
          "COMMENTS",
          "GROUP_ID",
          "GROUP_ORDER")
        
        val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.csv"))
        val underlying = tmp.path.toFile
        val config = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(header))
        val writer = underlying.asCsvWriter[Seq[String]](config)

        val sorted = annotationsByPart.flatMap { case (part, annotations) => 
          sort(annotations).map { (_, part.getTitle) }
        }

        val tupled = sorted.map(t => serializeOne(t._1, t._2, places))
        tupled.foreach(t => writer.write(t))
        writer.close()
        
        underlying
      }
    }
  }
  
}
