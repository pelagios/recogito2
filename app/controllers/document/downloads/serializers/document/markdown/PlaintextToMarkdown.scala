package controllers.document.downloads.serializers.document.markdown

import controllers.document.downloads.serializers.BaseSerializer
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, AnnotationService}
import services.document.{DocumentService, ExtendedDocumentMetadata}
import services.entity.builtin.EntityService
import services.generated.tables.records.DocumentFilepartRecord
import storage.uploads.Uploads

trait PlaintextToMarkdown extends BaseSerializer {

  private def textToMarkdown(
    text: String, 
    annotations: Seq[Annotation]
  ) = {

    // TODO 'interleave' - place a footnote marker at the end of each annotation
    text

    // TODO append annotation content as footnote

  }

  def plaintextToMarkdown(
    doc: ExtendedDocumentMetadata
  )(implicit documentService: DocumentService,
      annotationService: AnnotationService, 
      entityService: EntityService,
      uploads: Uploads,
      ctx: ExecutionContext
  ) = {
    // Fetch plaintexts
    val fTexts: Future[Seq[(String, DocumentFilepartRecord)]] = Future.sequence {
      doc.fileparts.map { part =>
        uploads.readTextfile(doc.owner.getUsername, doc.id, part.getFile).map(_.map((_, part)))
      }
    } map { _.flatten }

    val fAnnotations = annotationService.findByDocId(doc.id)

    val header =
      """|--------------|-----------|
        >|              |           |
        >|--------------|-----------|
        >
        >""".stripMargin('>')

    val f = for {
      texts <- fTexts
      annotations <- fAnnotations
    } yield (texts, annotations.map(_._1))

    f.map { case(texts, annotations) => {
      texts.foldLeft(header) { case (md, (plaintext, part)) => 
        val annotationsOnPart = sortByCharOffset(
          annotations.filter(_.annotates.filepartId == part.getId)
        )

        md + textToMarkdown(plaintext, annotationsOnPart)
      }
    }}
  }

}