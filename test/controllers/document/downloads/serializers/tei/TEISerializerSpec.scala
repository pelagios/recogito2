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
  
  "getAttribute" should {
    
    "properly split a simple attribute" in {
      val tag = "@id:foo"
      val (key, value) = serializer.getAttribute(tag)
      
      key must equalTo("id")
      value must equalTo("foo")
    }
    
    "properly split prefixed attributes" in {
      val tag1 = "@xml:id:foo"
      val tag2 = "@xml:id:foo:bar"
      
      val (key1, value1) = serializer.getAttribute(tag1)
      val (key2, value2) = serializer.getAttribute(tag2)
      
      key1 must equalTo("xml:id")
      value1 must equalTo("foo")
      
      key2 must equalTo("xml:id")
      value2 must equalTo("foo:bar")
    }
    
  }
  
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