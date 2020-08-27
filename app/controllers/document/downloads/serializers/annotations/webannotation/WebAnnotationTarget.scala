package controllers.document.downloads.serializers.annotations.webannotation

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{ AnyContent, Request }
import services.ContentType
import services.annotation.Annotation
import services.generated.tables.records.DocumentFilepartRecord

case class WebAnnotationTarget(source: String, hasType: String, label: String, selectors: Seq[WebAnnotationSelector])

object WebAnnotationTarget {

  def fromAnnotation(filepart: DocumentFilepartRecord, annotation: Annotation)(implicit req: Request[AnyContent]) = {

    import services.ContentType._

    // Source URI is either Recogito (for uploaded content) or original source (for IIIF)
    val source = ContentType.withName(filepart.getContentType).get match {
      case IMAGE_IIIF =>
        val fileURL = filepart.getFile
        if (fileURL.endsWith("/info.json"))
          fileURL.dropRight(10)
        else 
          fileURL

      case _ => controllers.document.annotation.routes.AnnotationController
        .resolveFromPart(annotation.annotates.filepartId).absoluteURL
    }

    annotation.annotates.contentType match {
      // Plaintest gets a Text target with two alternative selectors: TextQuote + TextPosition
      case TEXT_PLAIN =>
        WebAnnotationTarget(source, "Text", filepart.getTitle, Seq(
          TextPositionSelector.fromAnnotation(annotation),
          TextQuoteSelector.fromAnnotation(annotation)))

      // TODO TEI gets a Text target with two alternative selectors: TextQuote + XPath Range
      case TEXT_TEIXML =>
        WebAnnotationTarget(source, "Text", filepart.getTitle, Seq(
          TextQuoteSelector.fromAnnotation(annotation),
          XPathRangeSelector.fromAnnotation(annotation)))

      // Images get an Image target with a fragment selector
      case IMAGE_UPLOAD | IMAGE_IIIF =>
        WebAnnotationTarget(source, "Image", filepart.getTitle, Seq(ImageFragmentSelector.fromAnnotation(annotation)))

      // CSV gets a Dataset target with a Data Position selector
      case DATA_CSV  =>
        WebAnnotationTarget(source, "Dataset", filepart.getTitle, Seq(TableFragmentSelector.fromAnnotation(annotation)))

      case c =>
        // Should never happen
        throw new IllegalArgumentException(s"Unable to create Web Annotation target for content type ${c}")
    }

  }

  implicit val webAnnotationTargetWrites: Writes[WebAnnotationTarget] = (
    (JsPath \ "source").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "label").write[String] and
    (JsPath \ "selector").write[Seq[WebAnnotationSelector]]
  )(t => (t.source, t.hasType, t.label, t.selectors))

}
