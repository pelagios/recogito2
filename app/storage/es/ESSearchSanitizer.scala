package storage.es

/** http://stackoverflow.com/questions/32107601/is-there-an-implementation-of-a-search-term-sanitizer-for-elasticsearch-in-scala **/
trait ESSearchSanitizer {

  import java.util.regex.Pattern

  /** Sanitizes special characters and set operators in elastic search search-terms. */
  def sanitize(term: String): String = (
    escapeSpecialCharacters _ andThen
    escapeSetOperators andThen
    collapseWhiteSpaces andThen
    escapeOddQuote
  )(term)

  private def escapeSpecialCharacters(term: String): String = {
    val escapedCharacters = Pattern.quote("""\/+-&|!(){}[]^~*?:""")
    term.replaceAll(s"([$escapedCharacters])", "\\\\$1")
  }

  private def escapeSetOperators(term: String): String = {
    val operators = Set("AND", "OR", "NOT")
    operators.foldLeft(term) { case (accTerm, op) =>
      val escapedOp = escapeEachCharacter(op)
      accTerm.replaceAll(s"""\\b($op)\\b""", escapedOp)
    }
  }

  private def escapeEachCharacter(op: String): String =
    op.toCharArray.map(ch => s"""\\\\$ch""").mkString

  private def collapseWhiteSpaces(term: String): String = term.replaceAll("""\s+""", " ")

  private def escapeOddQuote(term: String): String = {
    if (term.count(_ == '"') % 2 == 1) term.replaceAll("""(.*)"(.*)""", """$1\\"$2""") else term
  }

}

