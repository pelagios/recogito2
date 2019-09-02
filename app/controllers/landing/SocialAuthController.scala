package controllers.landing

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.providers._
import controllers.Security
import javax.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.user.UserService

class SocialAuthController @Inject() (
  val components: ControllerComponents,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val socialProviderRegistry: SocialProviderRegistry,
  implicit val ctx: ExecutionContext
) extends AbstractController(components) {

  private val auth = silhouette.env.authenticatorService

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) => 
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => 
            
            val f = for {
              profile <- p.retrieveProfile(authInfo)
              // TODO if the user doesn't exists in the DB, create profile & run onboarding setup
              authenticator <- auth.create(profile.loginInfo)
              value <- auth.init(authenticator) // What does this do? (And will it fail?)
              result <- auth.embed(value, Redirect(routes.LandingController.index.toString))
            } yield (profile, result) 
          
            f.map { case (profile, result) => 
              // Just for reference - contains user first-, last-, fullname, email and avatar image 
              // In our case, we'd just keep the fullname. Instead of a username, we'd probably need
              // to generate an alias workspace URL e.g. /s/{uuid} for social accounts.
              play.api.Logger.info(profile.toString)
              result
            }
        }

      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        e.printStackTrace()
        play.api.Logger.info(s"Unexpected provider: $provider")
        Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm())
    }
  }

}
