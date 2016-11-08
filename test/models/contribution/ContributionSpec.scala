package models.contribution

import java.util.UUID
import models.ContentType
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class ContributionSpec extends Specification {
  
  private val DATE_TIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  private val madeAt = DateTime.parse("2016-06-03T13:02:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC)

  "The sample Contribution" should {
    
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/models/contribution/contribution.json").getLines().mkString("\n")
      val result = Json.fromJson[Contribution](Json.parse(json))
            
      // Parsed without errors?
      result.isSuccess must equalTo(true) 
      
      val contribution = result.get
      contribution.action must equalTo(ContributionAction.CONFIRM_BODY)
      contribution.madeBy must equalTo("rainer")
      contribution.madeAt must equalTo(madeAt)
      
      val item = contribution.affectsItem
      item.itemType must equalTo(ItemType.PLACE_BODY)
      item.documentId must equalTo("98muze1cl3saib")
      item.documentOwner must equalTo("rainer")
      item.filepartId must equalTo(Some(UUID.fromString("a7126845-16ac-434b-99bd-0f297e227822")))
      item.contentType must equalTo(Some(ContentType.TEXT_PLAIN))
      item.annotationId must equalTo(Some(UUID.fromString("7cfa1504-26de-45ef-a590-8b60ea8a60e8")))
      item.annotationVersionId must equalTo(Some(UUID.fromString("e868423f-5ea9-42ed-bb7d-5e1fac9195a0")))
      
      contribution.affectsUsers must equalTo(Seq("otheruser"))
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal Contribution" in {
      val contribution = Contribution(
        ContributionAction.DELETE_BODY,
        "rainer",
        madeAt,
        Item(
          ItemType.COMMENT_BODY,
          "98muze1cl3saib",
          "rainer",
          Some(UUID.fromString("7ccbf5dd-335b-4d59-bff6-d8d59d977825")),
          Some(ContentType.TEXT_TEIXML),
          Some(UUID.fromString("7cfa1504-26de-45ef-a590-8b60ea8a60e8")),
          Some(UUID.fromString("e868423f-5ea9-42ed-bb7d-5e1fac9195a0")),
          Some("just a comment"),
          None
        ),
        Seq("rainer"),
        None)
        
      // Convert to JSON
      val serialized = Json.prettyPrint(Json.toJson(contribution))
      
      val parseResult = Json.fromJson[Contribution](Json.parse(serialized))
      parseResult.isSuccess must equalTo(true)
      parseResult.get must equalTo(contribution)
    }
    
  }
  
}