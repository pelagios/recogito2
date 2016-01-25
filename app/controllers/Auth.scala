package controllers

import database.DB
import jp.t2v.lab.play2.auth.AuthConfig
import models.Users
import models.generated.tables.records.UsersRecord
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.{ ClassTag, classTag }
import play.api.mvc.{ Result, Results, RequestHeader }


trait Auth extends AuthConfig {
  
  private val NO_PERMISSION = "No permission"

  type Id = String
 
  type User = UsersRecord

  /** TODO Roles **/
  type Authority = String // Role

  val idTag: ClassTag[Id] = classTag[Id]
  
  val sessionTimeoutInSeconds: Int = 3600

  def resolveUser(id: Id)(implicit ctx: ExecutionContext, db: DB): Future[Option[User]] = 
    Users.findByUsername(id)

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Redirect(routes.Application.landingPage))

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Redirect(routes.Application.landingPage))

  /** If the user is not logged in and tries to access a protected resource **/
  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Redirect(routes.Application.landingPage))

  /** If authorization failed (usually incorrect password) **/
  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result] =
    Future.successful(Results.Forbidden(NO_PERMISSION))

  /**
   * TODO A function that determines what `Authority` a user has.
   * You should alter this procedure to suit your application.
   */
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

  /**
   * (Optional)
   * You can custom SessionID Token handler.
   * Default implementation use Cookie.
   *
  override lazy val tokenAccessor = new CookieTokenAccessor(
    /*
     * Whether use the secure option or not use it in the cookie.
     * Following code is default.
     */
    cookieSecureOption = play.api.Play.isProd(play.api.Play.current),
    cookieMaxAge       = Some(sessionTimeoutInSeconds)
  )*/

}
