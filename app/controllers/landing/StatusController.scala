package controllers.landing

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import scala.concurrent.ExecutionContext
import services.user.UserService
import services.contribution.ContributionService

@Singleton
class StatusController @Inject()(
  val components: ControllerComponents,
  val users: UserService,
  val edits: ContributionService,
  implicit val ctx: ExecutionContext
) extends AbstractController(components) {
  
  def index = Action.async { request =>
    val fDBOnline = users.countUsers()
      .map { _ => true }
      .recover { case t: Throwable => false }
      
    val fIndexOnline = edits.countLast24hrs()
      .map { _ => true }
      .recover { case t: Throwable => false }
    
    val f = for {
      dbOnline <- fDBOnline
      idxOnline <- fIndexOnline
    } yield (dbOnline, idxOnline)
    
    f.map { case (dbOnline, idxOnline) =>
      Ok(views.html.landing.status(dbOnline, idxOnline))
    }    
  }
  
}