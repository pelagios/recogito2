package controllers

import java.io.File
import kantan.csv.ops._
import models.annotation.Annotation
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source

trait HasCSVParsing {
  
  protected def guessDelimiter(line: String): Char = {
    // This test is pretty trivial but seems to be applied elsewhere (see e.g.
    // http://stackoverflow.com/questions/14693929/ruby-how-can-i-detect-intelligently-guess-the-delimiter-used-in-a-csv-file)
    // Simply count the most-used candidate
    val choices = Seq(',', ';', '\t', '|')
    val ranked = choices
      .map(char => (char, line.count(_ == char)))
      .sortBy(_._2).reverse
      
    ranked.head._1
  }
  
  protected def extractLine(file: File, rowIdx: Int)(implicit ctx: ExecutionContext): Future[Map[String, String]] = Future {
    scala.concurrent.blocking {
      val header = 
        Source.fromFile(file).getLines().take(1).next()
        
      val line = 
        Source.fromFile(file).getLines()
          .drop(rowIdx + 1) // Header
          .take(1)
          .next()
          
      val delimiter = guessDelimiter(header)
      
      val headerFields = header.asCsvReader[Seq[String]](delimiter, header = false).toIterator.next.get
      val lineFields = line.asCsvReader[Seq[String]](delimiter, header = false).toIterator.next.get
      
      headerFields.zip(lineFields).toMap
    }
  }
  
}