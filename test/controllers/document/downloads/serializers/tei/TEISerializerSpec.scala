package controllers.document.downloads.serializers.tei

import controllers.HasTEISnippets
import org.joox.JOOX._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import org.custommonkey.xmlunit.XMLTestCase
import org.custommonkey.xmlunit.XMLUnit

class TestSerializer extends TEISerializer

@RunWith(classOf[JUnitRunner])
class TEISerializerSpec extends XMLTestCase with SpecificationLike with HasTEISnippets {
  
  private val serializer = new TestSerializer()
  
  private val EXPECTED =
    <TEI xmlns="http://www.tei-c.org/ns/1.0">
      <teiHeader>
        <fileDesc>
          <sourceDesc></sourceDesc>
        </fileDesc>
      </teiHeader>
      <text></text>
    </TEI>
  
  "getOrCreateSourceDesc" should {
    
    "work for TEI without teiHeader" in {  
      val TEI = 
        <TEI xmlns="http://www.tei-c.org/ns/1.0">
          <text></text>
        </TEI>
      
      val doc = parseXMLString(TEI.toString)
      val sourceDesc = serializer.getOrCreateSourceDesc(doc)
 
      XMLUnit.setIgnoreWhitespace(true)
      assertXMLEqual($(doc).toString, EXPECTED.toString)      
      sourceDesc.get.size must equalTo(1)
      sourceDesc.get(0).getTagName must equalTo("sourceDesc")
    }
    
    "work for TEI without fileDesc" in {
      val TEI = 
        <TEI xmlns="http://www.tei-c.org/ns/1.0">
          <teiHeader></teiHeader>
          <text></text>
        </TEI>

      val doc = parseXMLString(TEI.toString)
      val sourceDesc = serializer.getOrCreateSourceDesc(doc)
 
      XMLUnit.setIgnoreWhitespace(true)
      assertXMLEqual($(doc).toString, EXPECTED.toString)      
      sourceDesc.get.size must equalTo(1)
      sourceDesc.get(0).getTagName must equalTo("sourceDesc")
    }

    "work for TEI without sourceDesc" in {  
      val TEI = 
        <TEI xmlns="http://www.tei-c.org/ns/1.0">
          <teiHeader>
            <fileDesc></fileDesc>
          </teiHeader>
          <text></text>
        </TEI>
            
      val doc = parseXMLString(TEI.toString)
      val sourceDesc = serializer.getOrCreateSourceDesc(doc)
 
      XMLUnit.setIgnoreWhitespace(true)
      assertXMLEqual($(doc).toString, EXPECTED.toString)      
      sourceDesc.get.size must equalTo(1)
      sourceDesc.get(0).getTagName must equalTo("sourceDesc")
    }
    
    "work for TEI with existing sourceDesc" in {  
      val TEI = 
        <TEI xmlns="http://www.tei-c.org/ns/1.0">
          <teiHeader>
            <fileDesc>
              <sourceDesc></sourceDesc>
            </fileDesc>
          </teiHeader>
          <text></text>
        </TEI>
            
      val doc = parseXMLString(TEI.toString)
      val sourceDesc = serializer.getOrCreateSourceDesc(doc)
 
      XMLUnit.setIgnoreWhitespace(true)
      assertXMLEqual($(doc).toString, EXPECTED.toString)      
      sourceDesc.get.size must equalTo(1)
      sourceDesc.get(0).getTagName must equalTo("sourceDesc")
    }
    
  }
  
}