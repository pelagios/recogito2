package controllers

import models.annotation.{ Annotation, AnnotationBody }

trait HasTextSnippets {
  
  case class Snippet(text: String, offset: Int)
  
  private val DEFAULT_BUFFER_SIZE = 80
  
  private[controllers] def extractTextSnippet(text: String, annotation: Annotation, bufferSize: Int = DEFAULT_BUFFER_SIZE) = {
    // Save to expect a text anchor, in the form 'char-offset:1269' (otherwise we want to let it fail) 
    val offset = annotation.anchor.substring(12).toInt
    
    // Likewise, safe to expect the annotation has one QUOTE body, with a defined value
    val quoteLength = annotation.bodies.filter(_.hasType == AnnotationBody.QUOTE).head.value.get.size
    
    val start = Math.max(0, offset - bufferSize)
    val end = Math.min(offset + quoteLength + bufferSize, text.size)
    val snippet = text.substring(start, end).replace("\n", " ") // Remove newlines in snippets    
    val leadingSpaces = snippet.takeWhile(_ == ' ').size
    Snippet(snippet.trim, offset - start + leadingSpaces)
  }
  
}