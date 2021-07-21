package controllers.document.downloads.serializers.document.tei

import play.api.mvc.{AnyContent, Request}
import scala.concurrent.{Future, ExecutionContext}
import scala.xml.Elem
import services.annotation.{Annotation, AnnotationService}
import services.entity.EntityType
import services.entity.builtin.EntityService
import services.document.{ExtendedDocumentMetadata, DocumentService}
import storage.es.ES
import storage.uploads.Uploads

trait PlaintextToEpiDoc extends PlaintextToTEI {

  def plaintextToEpiDoc(
    doc: ExtendedDocumentMetadata
  )(implicit documentService: DocumentService,
      annotationService: AnnotationService, 
      entityService: EntityService,
      uploads: Uploads, 
      request: Request[AnyContent], 
      ctx: ExecutionContext
  ) = {
    
    val fTexts = Future.sequence {
      doc.fileparts.map { part =>
        uploads.readTextfile(doc.owner.getUsername, doc.id, part.getFile).map(_.map((_, part)))
      }
    }
    
    val fDivs = fTexts.flatMap { maybeTextsAndParts => 
      val textsAndParts = maybeTextsAndParts.flatten
      val fAnnotations = annotationService.findByDocId(doc.id)
      val fPlaces = entityService.listEntitiesInDocument(doc.id, Some(EntityType.PLACE), 0, ES.MAX_SIZE)

      val f = for {
        annotations <- fAnnotations
        places <- fPlaces
      } yield (
        annotations.map(_._1),
        places.items.map(_._1).map(_.entity)
      )

      f.map { case (annotations, places) =>        
        val annotationsByPart = annotations.groupBy(_.annotates.filepartId)

        val divs = textsAndParts.map { case (text, part) =>
          // Split text to paragraph
          val paragraphs = text.split("\n\n")

          val pTags = paragraphs.foldLeft((Seq.empty[Elem], 0)) { case ((elements, runningOffset), text) => {
            val textLength = text.size

            // Filter annotations for this paragraph
            val annotationsInParagraph = 
              annotationsByPart
                .get(part.getId).getOrElse(Seq.empty[Annotation])
                .filter(annotation => {
                  val offset = getCharOffset(annotation)
                  val quoteLength = getQuote(annotation).size

                  // Keep only annotations contained entirely within this paragraph
                  offset >= runningOffset && (offset + quoteLength) <= (runningOffset + textLength)
                })

            val p = <p>{ paragraphToTEI(text, annotationsInParagraph, runningOffset) }</p>
            (elements :+ p, runningOffset + text.size + 2)
          }}._1

          <div>{pTags}</div>
        }
        
        val listPlaces = placesToList(annotations, places)
        val taxonomy = tagsToTaxonomy(annotations)
        val relations = relationsToList(annotations)
        (divs, listPlaces, taxonomy, relations)
      }
    }
       
    fDivs.map { case (divs, listPlaces, taxonomy, relations) =>
      Seq(<TEI xmlns="http://www.tei-c.org/ns/1.0" xml:space="preserve" xml:lang="en">
        <teiHeader>
          <fileDesc>
            <titleStmt>
              <title>{ doc.title }</title>
              { if (doc.author.isDefined) <author>{ doc.author.get }</author> }
              <editor xml:id={controllers.my.routes.WorkspaceController.workspace(doc.ownerName).absoluteURL}>{ doc.ownerName }</editor>
            </titleStmt>
            <publicationStmt>
              <authority><!-- TODO attribution --></authority>
              <idno type="filename"></idno>
              <availability>
                <licence><!-- TODO license --><ref><!-- TODO licens URL --></ref></licence>
              </availability>
            </publicationStmt>

            <sourceDesc>
              <bibl>{controllers.document.routes.DocumentController.initialDocumentView(doc.id).absoluteURL }</bibl>
              { if (doc.edition.isDefined) <bibl>{ doc.edition.get }</bibl> }
              <msDesc>
                <msIdentifier>
                  <msName>n/a</msName>
                </msIdentifier>
                <msContents>
                  <summary><!-- TODO description --></summary>
                  <textLang><!-- TODO language --></textLang>
                </msContents>
                <history>
                  <origin>
                    <origDate><!-- TODO date --></origDate>
                  </origin>
                </history>
              </msDesc>
            </sourceDesc>
          </fileDesc>
        </teiHeader>
        <text>
          <body>
            <div type="edition" xml:space="preserve"><!-- xml:lang="la"--><!-- ifcontent of Language is a legal language code put it in @xml:lang as well, otherwise omit. -->
              <ab>
                { divs }
              </ab>
            </div>
          </body>
        </text>
      </TEI>)
    }
  }

}