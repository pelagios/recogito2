package controllers.document.downloads.serializers.document.markdown

import controllers.document.downloads.serializers.BaseSerializer
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, AnnotationService}
import services.document.{DocumentService, ExtendedDocumentMetadata}
import services.entity.builtin.EntityService
import services.generated.tables.records.DocumentFilepartRecord
import storage.uploads.Uploads

trait PlaintextToMarkdown extends BaseSerializer {

  private def getEndOffset(a: Annotation) = a.anchor.substring(12).toInt  + getFirstQuote(a).map(_.size).getOrElse(0)

  private def hasComments(a: Annotation) = getCommentBodies(a).length > 0

  private def textToMarkdown(
    text: String, 
    annotations: Seq[Annotation]
  ) = {
    if (annotations.length > 0) {
      val mdBody = annotations.foldLeft(("", 0)) { case((md, cursor), a) =>
        val textBefore = text.substring(cursor, getEndOffset(a))
        if (hasComments(a))
          (s"$textBefore[^${a.annotationId}]", cursor + textBefore.length)
        else
          (s"$md$textBefore", cursor + textBefore.length)
      }._1

      val mdFootnotes = annotations.filter(hasComments).map { a => 
        val comments = getCommentBodies(a).flatMap(_.value).mkString("\n\n")
        s"[^${a.annotationId}]: ${comments}"
      }

      s"${mdBody}\n\n${mdFootnotes.mkString("\n\n")}\n\n"
    } else {
      text
    }
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
      s"""---
         >title: ${doc.title}
         >author: ${doc.author.getOrElse("")}
         >source: ${doc.source.getOrElse("")}
         >publication-date: ${doc.dateFreeform.getOrElse("")}
         >---
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