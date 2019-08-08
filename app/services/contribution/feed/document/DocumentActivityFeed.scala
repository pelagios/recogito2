package services.contribution.feed.document

import com.sksamuel.elastic4s.searches.RichSearchResponse
import java.util.UUID
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.histogram.InternalDateHistogram
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContent, Request}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import services.contribution.{ContributionAction, ItemType}
import services.document.{DocumentService, ExtendedDocumentMetadata}

/** Activity feed for a specific document **/
case class DocumentActivityFeed(
  url: String, 
  title: String,
  author: Option[String],
  owner: String,
  activities: Seq[DocumentDayActivity])

object DocumentActivityFeed {

  implicit val documentActivityFeedWrites: Writes[DocumentActivityFeed] = (
    (JsPath \ "url").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "author").writeNullable[String] and
    (JsPath \ "owner").write[String] and
    (JsPath \ "timeline").write[Seq[DocumentDayActivity]]
  )(unlift(DocumentActivityFeed.unapply))

  private def parseAggregations(
    documentId: String,
    response: RichSearchResponse,
    doc: ExtendedDocumentMetadata
  )(implicit request: Request[AnyContent]) = {
    val overTime = response.aggregations.getAs[InternalFilter]("over_time")
      .getAggregations.get("per_day").asInstanceOf[InternalDateHistogram]

    val activities = overTime.getBuckets.asScala.map { bucket => 
      val timestamp = new DateTime(bucket.getKey.asInstanceOf[DateTime].getMillis, DateTimeZone.UTC)

      val byUser: Seq[DocumentActivityByUser] = bucket.getAggregations.get("by_user").asInstanceOf[Terms]
        .getBuckets.asScala.map { bucket =>           

          val byPart: Seq[DocumentActivityByPart] = bucket.getAggregations.get("by_part").asInstanceOf[Terms]
            .getBuckets.asScala.map { bucket => 
              // ActivityEntry is a flattened version of the two last nesting levels (action and type)
              val entries = bucket.getAggregations.get("by_action").asInstanceOf[Terms]
                .getBuckets.asScala.flatMap { bucket => 
                  val thisAction = ContributionAction.withName(bucket.getKeyAsString)

                  bucket.getAggregations.get("by_item_type").asInstanceOf[Terms]
                    .getBuckets.asScala.map { bucket => 
                      DocumentActivityFeedEntry(thisAction, ItemType.withName(bucket.getKeyAsString), bucket.getDocCount)
                    }              
                }
              DocumentActivityByPart.build(
                UUID.fromString(bucket.getKeyAsString), bucket.getDocCount, entries, doc)
            } 
          DocumentActivityByUser(bucket.getKeyAsString, bucket.getDocCount, byPart)
        }
      DocumentDayActivity(timestamp, bucket.getDocCount, byUser)
    }

    DocumentActivityFeed(
      controllers.document.routes.DocumentController.initialDocumentView(documentId).absoluteURL, 
      doc.title,
      doc.author,
      doc.ownerName,
      activities)
  }

  def fromSearchResponse(
    documentId: String, 
    response: RichSearchResponse
  )(implicit 
      request: Request[AnyContent],
      documents: DocumentService,
      ctx: ExecutionContext
  ) = documents.getExtendedMeta(documentId).map { _ match {
    case Some((doc, _)) => 
      parseAggregations(documentId, response, doc)

    case None =>
      // Should never happen - let it crash
      throw new Exception("Data integrity error: activity feed for document that is not in the DB")
  }}

}