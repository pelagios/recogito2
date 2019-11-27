package controllers.document.downloads.serializers.document.markdown

import controllers.document.downloads.serializers.BaseSerializer
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.AnnotationService
import services.document.{DocumentService, ExtendedDocumentMetadata}
import services.entity.builtin.EntityService
import storage.uploads.Uploads

trait PlaintextToMarkdown extends BaseSerializer {

  private def textToMarkdown() = ???

  def plaintextToMarkdown(
    doc: ExtendedDocumentMetadata
  )(implicit documentService: DocumentService,
      annotationService: AnnotationService, 
      entityService: EntityService,
      uploads: Uploads,
      ctx: ExecutionContext
  ) = {
    // Fetch plaintexts
    val fTexts = Future.sequence {
      doc.fileparts.map { part =>
        uploads.readTextfile(doc.owner.getUsername, doc.id, part.getFile).map(_.map((_, part)))
      }
    } map { _.flatten }

    val header = """
    |--------------|-----------|
    |              |           |
    |--------------|-----------|
    """

    fTexts.map { _.foldLeft(header) { case (md, (plaintext, part)) => 
      md + plaintext
    }}
  }

}