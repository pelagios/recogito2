package controllers

import models.annotation.{Annotation, AnnotationBody}

trait HasTextSnippets {
  
  case class Snippet(text: String, offset: Int)
  
  private val DEFAULT_BUFFER_SIZE = 80
  
  private val CHARS_TO_REMOVE = Set('\n', ' ', ',', ';', ':')
  
  private[controllers] def snippetFromText(text: String, annotation: Annotation, bufferSize: Int = DEFAULT_BUFFER_SIZE) = {
    // It's safe to expect a text anchor, in the form 'char-offset:1269' (otherwise we want to let it fail) 
    val offset = annotation.anchor.substring(12).toInt
    
    // Likewise, safe to expect the annotation has one QUOTE body, with a defined value
    val quoteLength = annotation.bodies.filter(_.hasType == AnnotationBody.QUOTE).head.value.get.size
    
    snip(text, offset, quoteLength, bufferSize)
  }
  
  /** Removes newlines and multiple spaces **/
  protected def normalize(s: String) = 
    s.replace("\n", " ") // replace new lines with space
      .replaceAll("\\s+", " ") // Replace multiple spaces with one
      .trim
  
  protected def snip(text: String, from: Int, len: Int, bufferSize: Int) = {
    val (start, prefix) =
      if (from - bufferSize > 0) {
        // Snippet starts somewhere inside the text - move cursor after first whitespace
        val firstWhitespaceIdx = text.indexOf(' ', from - bufferSize)
        if (firstWhitespaceIdx > -1)
          (firstWhitespaceIdx, "...")
        else
          (from - bufferSize, "...")
      } else {
        // Snippet is at the start of the text
        (0, "")
      }
    
    val (end, suffix) =
      if ((from + len + bufferSize) > text.size) {
        // Snippet is at the end of the text
        (text.size, "")
      } else {
        val lastWhitespaceIdx = text.lastIndexOf(' ', from + len + bufferSize)
        if (lastWhitespaceIdx > -1)
          // Snippet ends somewhere inside the text - move cursor before last whitespace
          (lastWhitespaceIdx, "...")
        else
          (from + len + bufferSize, "...")
      }
    
    val snippet = text.substring(start, end).replace("\n", " ") // Remove newlines in snippets    
    val leadingRemovableChars = snippet.takeWhile(c => CHARS_TO_REMOVE.contains(c)).size
    val trailingRemovableChars = snippet.reverse.takeWhile(c => CHARS_TO_REMOVE.contains(c)).size
    val trimmedSnippet = snippet.dropRight(trailingRemovableChars).substring(leadingRemovableChars)
    val normalizedSuffix = if (trimmedSnippet.endsWith(".") && suffix.size > 0) suffix.take(2) else suffix
    Snippet(normalize(prefix + trimmedSnippet + normalizedSuffix), from - start + leadingRemovableChars)
  }
  
}