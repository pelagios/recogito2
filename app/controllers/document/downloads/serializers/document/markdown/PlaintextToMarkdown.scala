package controllers.document.downloads.serializers.document.markdown

import controllers.document.downloads.serializers.BaseSerializer
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, AnnotationService}
import services.document.{DocumentService, ExtendedDocumentMetadata}
import services.entity.builtin.EntityService
import services.generated.tables.records.DocumentFilepartRecord
import storage.uploads.Uploads

trait PlaintextToMarkdown extends BaseSerializer {

  private def getStartOffset(a: Annotation) = a.anchor.substring(12).toInt

  private def getEndOffset(a: Annotation) = a.anchor.substring(12).toInt  + getFirstQuote(a).map(_.size).getOrElse(0)

  private def hasComments(a: Annotation) = getCommentBodies(a).length > 0

  private def removeOverlappingAnnotations(annotations: Seq[Annotation]) = 
    sort(annotations).foldLeft(Seq.empty[Annotation]) { case (result, next) =>
      result.lastOption match {
        case Some(previous) =>
          // Check if next overlaps previous
          val previousEnd = getEndOffset(previous)          
          val nextStart = getStartOffset(next)
          
          if (nextStart <= previousEnd)
            result :+ next
          else
            result
          
        case None =>
          // First in line
          Seq(next)
      }
    }

  private def textToMarkdown(
    text: String, 
    annotations: Seq[Annotation]
  ) = {
    val nonOverlapping = removeOverlappingAnnotations(annotations)

    if (nonOverlapping.length > 0) {
      val mdBody = nonOverlapping.foldLeft(("", 0)) { case((md, cursor), a) =>
        val textBefore = text.substring(cursor, getEndOffset(a))
        if (hasComments(a))
          (s"$md$textBefore[^${a.annotationId}]", cursor + textBefore.length)
        else
          (s"$md$textBefore", cursor + textBefore.length)
      }

      val mdSuffix = text.substring(mdBody._2)

      val mdFootnotes = nonOverlapping.filter(hasComments).map { a => 
        val comments = getCommentBodies(a).flatMap(_.value).mkString("\n\n")
        s"[^${a.annotationId}]: ${comments}"
      }

      s"${mdBody._1}${mdSuffix}\n\n${mdFootnotes.mkString("\n\n")}\n\n"
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