package controllers.document.downloads.serializers

import java.io.{ File, FileOutputStream }
import java.nio.file.Paths
import java.util.UUID
import models.HasDate
import models.annotation.{ Annotation, AnnotationBody, AnnotationService }
import models.document.{ DocumentInfo, DocumentService }
import org.apache.jena.rdf.model.{ Model, ModelFactory }
import org.apache.jena.riot.{ RDFDataMgr, RDFFormat }
import org.apache.jena.vocabulary.{ DCTerms, RDF }
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.{ AnyContent, Request }
import scala.concurrent.ExecutionContext
import scala.concurrent.{ ExecutionContext, Future }

sealed class BaseVocab(val getURI: String) {
  
  private val model = ModelFactory.createDefaultModel()
  protected def createResource(term: String) = model.createResource(getURI + term)
  protected def createProperty(term: String) = model.createProperty(getURI + term)
  
}

object OA extends BaseVocab("http://www.w3.org/ns/oa#") {
  
  val Annotation = createResource("Annotation")
  val Tag = createResource("Tag")
  
  val hasBody = createProperty("hasBody")
  val hasTarget = createProperty("hasTarget")
  
}

object Content extends BaseVocab("http://www.w3.org/2011/content#") {

  val chars = createProperty("chars")
  
}

object Pelagios extends BaseVocab("http://pelagios.github.io/vocab/terms#") {
  
  val AnnotatedThing = createResource("AnnotatedThing")
  
}


trait RDFSerializer extends BaseSerializer with HasDate {
    
  private def createDocumentResource(docInfo: DocumentInfo, baseUri: String, model: Model) = {
    val resource = model.createResource(baseUri)
    resource.addProperty(RDF.`type`, Pelagios.AnnotatedThing)
    resource.addProperty(DCTerms.title, docInfo.title)
    docInfo.author.map(author => resource.addProperty(DCTerms.creator, author))
    
    docInfo.dateFreeform.map(date => resource.addProperty(DCTerms.date, date))
    docInfo.description.map(description => resource.addProperty(DCTerms.description, description))
    docInfo.source.map(source => resource.addProperty(DCTerms.source, source))
    docInfo.language.map(language => resource.addProperty(DCTerms.language, language))
    docInfo.license.map(license => resource.addProperty(DCTerms.license, license.toString))
  }
  
  private def createAnnotationResource(docInfo: DocumentInfo, annotation: Annotation, baseUri: String, model: Model) = {
    val annotationResource = model.createResource(baseUri + "#" + annotation.annotationId)
    annotationResource.addProperty(RDF.`type`, OA.Annotation)
    annotationResource.addProperty(OA.hasTarget, model.createResource(baseUri))
    
    annotation.bodies.zipWithIndex.foreach { case (body, idx) =>
      
      body.hasType match {
        case AnnotationBody.PLACE | AnnotationBody.PERSON =>
          body.uri.map(uri => annotationResource.addProperty(OA.hasBody, model.createResource(uri)))
          
        case AnnotationBody.TAG | AnnotationBody.COMMENT =>
          val tagResource = model.createResource()
          tagResource.addProperty(RDF.`type`, OA.Tag)
          body.value.map(chars => tagResource.addProperty(Content.chars, chars))
          body.lastModifiedBy.map(by => tagResource.addProperty(DCTerms.creator, by))
          tagResource.addProperty(DCTerms.created, formatDate(body.lastModifiedAt))
          annotationResource.addProperty(OA.hasBody, tagResource)
          
        case _ =>
      }
    }    
  }
  
  def documentToRDF(doc: DocumentInfo, format: RDFFormat)(implicit documentService: DocumentService,
      annotationService: AnnotationService, request: Request[AnyContent], tmpFile: TemporaryFileCreator, ctx: ExecutionContext) = {
    
    val baseUri = controllers.document.routes.DocumentController.initialDocumentView(doc.id).absoluteURL 
      
    annotationService.findByDocId(doc.id).map { annotations =>
      scala.concurrent.blocking { 
        val model = ModelFactory.createDefaultModel()
        model.setNsPrefix("cnt", Content.getURI)
        model.setNsPrefix("dcterms", DCTerms.getURI)
        model.setNsPrefix("oa", OA.getURI)
        model.setNsPrefix("pelagios", Pelagios.getURI)

        createDocumentResource(doc, baseUri, model)
        annotations.foreach(t => createAnnotationResource(doc, t._1, baseUri, model))
      
        val tmp = tmpFile.create(Paths.get(TMP_DIR, s"${UUID.randomUUID}.ttl"))
        val os = java.nio.file.Files.newOutputStream(tmp.path)
        RDFDataMgr.write(os, model, format)
        os.close()
        tmp.path.toFile
      }
    }
    
  }
  
}
