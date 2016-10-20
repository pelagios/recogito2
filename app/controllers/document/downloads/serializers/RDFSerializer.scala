package controllers.document.downloads.serializers

import java.io.{ File, FileOutputStream }
import java.util.UUID
import models.annotation.{ Annotation, AnnotationService }
import models.document.{ DocumentInfo, DocumentService }
import org.apache.jena.rdf.model.{ Model, ModelFactory }
import org.apache.jena.riot.{ RDFDataMgr, RDFFormat }
import org.apache.jena.vocabulary.{ DCTerms, RDF }
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{ AnyContent, Request }
import scala.concurrent.ExecutionContext
import scala.concurrent.{ ExecutionContext, Future }
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.rdf.model.ResourceFactory
import models.HasDate

trait RDFSerializer extends BaseSerializer with HasDate {
    
  private def createDocumentResource(docInfo: DocumentInfo, baseUri: String, model: Model) = {
    val resource = model.createResource(baseUri)
    resource.addProperty(DCTerms.title, docInfo.title)
    docInfo.author.map(author => resource.addProperty(DCTerms.creator, author))
    
    docInfo.dateFreeform.map(date => resource.addProperty(DCTerms.date, date))
    docInfo.description.map(description => resource.addProperty(DCTerms.description, description))
    docInfo.source.map(source => resource.addProperty(DCTerms.source, source))
    docInfo.language.map(language => resource.addProperty(DCTerms.language, language))
    docInfo.license.map(license => resource.addProperty(DCTerms.license, license))
  }
  
  private def createAnnotationResource(docInfo: DocumentInfo, annotation: Annotation, baseUri: String, model: Model) = {
    val resource = model.createResource(baseUri + "#" + annotation.annotationId)
    resource.addProperty(RDF.`type`, model.createResource(OA.Annotation))
    resource.addProperty(model.createProperty(OA.hasTarget), model.createResource(baseUri))
    
    annotation.bodies.zipWithIndex.foreach { case (body, idx) =>
      val bodyResource = model.createResource(resource.getURI + "/body/" + (idx + 1))
      resource.addProperty(model.createProperty(OA.hasBody), bodyResource)
      
      if (body.value.isDefined) {
        bodyResource.addProperty(RDF.`type`, model.createResource(OA.TextualBody))
        bodyResource.addProperty(model.createProperty(OA.hasBody), body.value.get)
      } else if (body.uri.isDefined) {
        // TODO type semantic tag?
        bodyResource.addProperty(model.createProperty(OA.hasBody), body.uri.get)
      }
      
      body.lastModifiedBy.map(by => bodyResource.addProperty(DCTerms.creator, by))
    }    
  }
  
  def documentToRDF(doc: DocumentInfo, format: RDFFormat)(implicit documentService: DocumentService,
      annotationService: AnnotationService, request: Request[AnyContent], ctx: ExecutionContext) = {
    
    val baseUri = controllers.document.routes.DocumentController.initialDocumentView(doc.id).absoluteURL 
      
    annotationService.findByDocId(doc.id).map { annotations =>
      scala.concurrent.blocking { 
        val model = ModelFactory.createDefaultModel()
        model.setNsPrefix("dcterms", DCTerms.getURI)
        model.setNsPrefix("oa", OA.getURI)

        createDocumentResource(doc, baseUri, model)
        annotations.foreach(t => createAnnotationResource(doc, t._1, baseUri, model))
      
        val tmp = new TemporaryFile(new File(TMP_DIR, UUID.randomUUID + ".ttl"))
        val os = new FileOutputStream(tmp.file)
        RDFDataMgr.write(os, model, format)
        os.close()
      
        tmp.file
      }
    }
    
  }
  
}

object OA {
  
  val getURI = "http://www.w3.org/ns/oa#"
  
  val Annotation = getURI + "Annotation"
  
  val annotatedAt = getURI + "annotatedAt"
  
  val hasBody = getURI + "hasBody"
  
  val TextualBody = getURI + "TextualBody"
  
  val hasTarget = getURI + "hasTarget"
  
}
