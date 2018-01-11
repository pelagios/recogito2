package controllers

import java.sql.Timestamp
import java.util.UUID
import services.annotation.Annotation
import services.contribution.ContributionAction._
import services.contribution.ItemType._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source
import play.api.libs.json.Json
import services.generated.tables.records.DocumentRecord

class TestAnnotationValidator extends HasAnnotationValidation

@RunWith(classOf[JUnitRunner])
class HasAnnotationValidationSpec extends Specification {
  
  val validator = new TestAnnotationValidator()
  
  private def loadAnnotation(name: String) =
    Json.fromJson[Annotation](Json.parse(Source.fromFile("test/resources/models/annotation/" + name).getLines().mkString("\n"))).get

  val document = new DocumentRecord(
    "98muze1cl3saib",
    "rainer",
    new Timestamp(System.currentTimeMillis),
    "Sample Document",
    null,
    new Timestamp(System.currentTimeMillis),
    null,
    null,
    null,
    null,
    null,
    null,
    false,
    null)
    
  val annotationBefore = loadAnnotation("text-annotation.json")
  
  "The first test annotation" should {

    "produce one 'comment added' contribution" in {
      val annotationWithAddedComment = loadAnnotation("text-annotation-changed-1.json")
      val contributions = validator.validateUpdate(annotationWithAddedComment, Some(annotationBefore), document)
      contributions.size must equalTo(1)
      contributions.head.action must equalTo(CREATE_BODY)
      contributions.head.affectsItem.itemType must equalTo(COMMENT_BODY)
    }

  }

  "The second test annotation" should {

    "produce one 'place removed' and one 'comment added' contribution" in {
      val annotationWithRemovedPlaceAndAddedComment = loadAnnotation("text-annotation-changed-2.json")
      val contributions = validator.validateUpdate(annotationWithRemovedPlaceAndAddedComment, Some(annotationBefore), document)
      contributions.size must equalTo(2)
      
      val removedPlace = contributions(0)
      removedPlace.action must equalTo(DELETE_BODY)
      removedPlace.affectsItem.itemType must equalTo(PLACE_BODY)
      
      val addedComment = contributions(1)
      addedComment.action must equalTo(CREATE_BODY)
      addedComment.affectsItem.itemType must equalTo(COMMENT_BODY)
    }

  }

  "The third test annotation" should {

    "produce one 'place removed' contribution" in {
      val annotationWithChangedPlace = loadAnnotation("text-annotation-changed-3.json")
      val contributions = validator.validateUpdate(annotationWithChangedPlace, Some(annotationBefore), document)
      contributions.size must equalTo(1)
      contributions.head.action must equalTo(EDIT_BODY)
      contributions.head.affectsItem.itemType must equalTo(PLACE_BODY)
    }

  }
  
  "The fourth test annotation" should {

    "produce four contributions (del comment, confirm place, add place, add comment)" in {
      val annotationWithFourContributions = loadAnnotation("text-annotation-changed-4.json")
      val contributions = validator.validateUpdate(annotationWithFourContributions, Some(annotationBefore), document)
      
      contributions.size must equalTo(4)
      
      val deletedComment = contributions(0)
      deletedComment.action must equalTo(DELETE_BODY)
      deletedComment.affectsItem.itemType must equalTo(COMMENT_BODY)
      
      val confirmedPlace = contributions(1)
      confirmedPlace.action must equalTo(CONFIRM_BODY)
      confirmedPlace.affectsItem.itemType must equalTo(PLACE_BODY)
      
      val addedPlace = contributions(2)
      addedPlace.action must equalTo(CREATE_BODY)
      addedPlace.affectsItem.itemType must equalTo(PLACE_BODY)
      
      val addedComment = contributions(3)
      addedComment.action must equalTo(CREATE_BODY)
      addedComment.affectsItem.itemType must equalTo(COMMENT_BODY)
    }

  }

}
