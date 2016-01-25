package controllers

import database.DB
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthConfig
import models.Users
import models.Roles._
import models.generated.tables.records.UsersRecord
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.{ ClassTag, classTag }
import play.api.mvc.{ Result, Results, RequestHeader }

/** Helper trait so we can hand the injected DB down to the AuthConfigImpl trait **/
trait HasDB { def db: DB }

trait AuthConfigImpl extends AuthConfig { self: HasDB =>
  
  private val NO_PERMISSION = "No permission"

  type Id = String
 
  type User = UsersRecord

  type Authority = Role

  val idTag: ClassTag[Id] = classTag[Id]
  
  val sessionTimeoutInSeconds: Int = 3600

  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] =
    Users.findByUsername(id)(db)

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Redirect(routes.MyRecogito.index()))

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Redirect(routes.Application.landingPage))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Redirect(routes.Application.landingPage))

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result] =
    Future.successful(Results.Forbidden(NO_PERMISSION))

  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    true
    /*
    (user.role, authority) match {
      case (Administrator, _)       => true
      case (NormalUser, NormalUser) => true
      case _                        => false
    }
    */
  }
  
}

