package controllers

import java.io.File
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{ Header, QuotePolicy }
import kantan.csv.ops._
import kantan.codecs.Result.Success
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
  
  protected def guessDelimiter(file: File): Char = {
    val header = Source.fromFile(file).getLines.next
    guessDelimiter(header)     
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
      val config = CsvConfiguration(delimiter, '"', QuotePolicy.WhenNeeded, Header.None)
      val headerFields = header.asCsvReader[Seq[String]](config).toIterator.next.get
      val lineFields = line.asCsvReader[Seq[String]](config).toIterator.next.get
      
      headerFields.zip(lineFields).toMap
    }
  }
  
  protected def parseCSV[T](file: File, delimiter: Char, header: Boolean, op: (List[String], Int) => T) = {
    val h = if (header) Header.Implicit else Header.None
    val config = CsvConfiguration(delimiter, '"', QuotePolicy.WhenNeeded, h)
    var rowCounter = 0
    
    file.asCsvReader[List[String]](config).map {
      case Success(row) =>
        val t = Some(op(row, rowCounter))
        rowCounter += 1
        t
        
      case _ => 
        rowCounter += 1
        None
    }
    
  }
  
}