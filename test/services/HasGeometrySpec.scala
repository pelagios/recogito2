package services

import com.vividsolutions.jts.geom.Coordinate
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class HasGeometrySpec extends Specification {
  
  val JSON_WITH_COORDINATE = """{ "foo": "bar", "coord": [29.89246, -18.06294] }"""
  
  val JSON_WITHOUT_COORDINATE = """{ "foo": "bar" }"""

  case class ObjectWithNullableCoordinate(foo: String, coord: Option[Coordinate])
  object ObjectWithNullableCoordinate extends HasGeometry {
    
    implicit val testObjectReads: Reads[ObjectWithNullableCoordinate] = (
      (JsPath \ "foo").read[String] and
      (JsPath \ "coord").readNullable[Coordinate]
    )(ObjectWithNullableCoordinate.apply _)
    
  }
  
  /** Shorthand **/
  private def parse(str: String) = 
    Json.fromJson[ObjectWithNullableCoordinate](Json.parse(str))
  
  "The JSON geometry parser" should {
    
    "properly parse the coordinate" in {
      val asJson = parse(JSON_WITH_COORDINATE)
      asJson.isSuccess must equalTo(true)
      asJson.get.foo must equalTo("bar")
      asJson.get.coord must equalTo(Some(new Coordinate(29.89246, -18.06294)))
    }
    
    "properly parse the record without coordinate" in {
      val asJson = parse(JSON_WITHOUT_COORDINATE)
      asJson.isSuccess must equalTo(true)
      asJson.get.foo must equalTo("bar")
      asJson.get.coord must equalTo(None)
    }
    
  }
  
}