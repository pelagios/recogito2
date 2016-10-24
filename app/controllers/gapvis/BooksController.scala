package controllers.gapvis

import javax.inject.Inject
import play.api.Configuration
import play.api.mvc.Action
import play.api.mvc.Result
import play.api.libs.json._
import models.generated.tables.records.DocumentRecord
import models.document.DocumentService
import models.user.UserService
import models.gapvis.Book
import models.gapvis.BooksService
import scala.concurrent.{ Future, ExecutionContext }
import controllers.{ BaseController, HasPrettyPrintJSON }

class BooksController @Inject() (
    val config: Configuration,
    val users: UserService,
    val books: BooksService,
    implicit val ctx: ExecutionContext) extends BaseController(config, users) with HasPrettyPrintJSON {
  
  /** writes book **/
  implicit val bookWrites = new Writes[Book] {
    def writes(book: Book) = Json.obj(
      "id" -> book.id,
      "title" -> book.title,
      "uri" -> book.uri,
      "author" -> book.author,
      "printed" -> book.printed)
  }

  /** list books **/
  def list = Action.async { implicit request =>
    val result = books.list.map(book => jsonOk(Json.toJson(book)))
    result
  }
  
  /** a book **/
  def book(id: String) = Action.async { implicit request =>
    val result = books.book(id).map(book => jsonOk(Json.toJson(book)))
    result
  }
}


