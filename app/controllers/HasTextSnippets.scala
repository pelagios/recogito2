package controllers

import models.annotation.{ Annotation, AnnotationBody }

trait HasTextSnippets {
  
  case class Snippet(text: String, offset: Int)
  
  private val DEFAULT_BUFFER_SIZE = 80
  
  private val REMOVEABLE_CHARACTERS = Set(' ', ',', ';', ':')
  
  private[controllers] def extractTextSnippet(text: String, annotation: Annotation, bufferSize: Int = DEFAULT_BUFFER_SIZE) = {
    // Save to expect a text anchor, in the form 'char-offset:1269' (otherwise we want to let it fail) 
    val offset = annotation.anchor.substring(12).toInt
    
    // Likewise, safe to expect the annotation has one QUOTE body, with a defined value
    val quoteLength = annotation.bodies.filter(_.hasType == AnnotationBody.QUOTE).head.value.get.size
    
    val (start, prefix) =
      if (offset - bufferSize > 0) {
        // Snippet starts somewhere inside the text - move cursor after first whitespace
        val firstWhitespaceIdx = text.indexOf(' ', offset - bufferSize)
        if (firstWhitespaceIdx > -1)
          (firstWhitespaceIdx, "...")
        else
          (offset - bufferSize, "...")
      } else {
        // Snippet is at the start of the text
        (0, "")
      }
    
    val (end, suffix) =
      if ((offset + quoteLength + bufferSize) > text.size) {
        // Snippet is at the end of the text
        (text.size, "")
      } else {
        val lastWhitespaceIdx = text.lastIndexOf(' ', offset + quoteLength + bufferSize)
        if (lastWhitespaceIdx > -1)
          // Snippet ends somewhere inside the text - move cursor before last whitespace
          (lastWhitespaceIdx, "...")
        else
          (offset + quoteLength + bufferSize, "...")
      }
    
    val snippet = text.substring(start, end).replace("\n", " ") // Remove newlines in snippets    
    val leadingRemovableChars = snippet.takeWhile(c => REMOVEABLE_CHARACTERS.contains(c)).size
    val trailingRemovableChars = snippet.reverse.takeWhile(c => REMOVEABLE_CHARACTERS.contains(c)).size
    val trimmedSnippet = snippet.dropRight(trailingRemovableChars).substring(leadingRemovableChars)
    val normalizedSuffix = if (trimmedSnippet.endsWith(".") && suffix.size > 0) suffix.take(2) else suffix
    Snippet(prefix + trimmedSnippet + normalizedSuffix, offset - start + leadingRemovableChars)
  }
  
}