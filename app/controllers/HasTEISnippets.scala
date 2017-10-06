package controllers

import java.io.File

trait HasTEISnippets {
  
  private val DEFAULT_BUFFER_SIZE = 80
  
  def extractTEISnippet(file: File, startPath: String, endPath: String, bufferSize: Int = DEFAULT_BUFFER_SIZE) = ???
  
}