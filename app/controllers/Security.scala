package controllers

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder, Signer}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Authorization, Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.crypto.{JcaSigner, JcaSignerSettings, JcaCrypter, JcaCrypterSettings} 
import com.mohiva.play.silhouette.impl.authenticators.{CookieAuthenticator, CookieAuthenticatorSettings, CookieAuthenticatorService}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.{DelegableAuthInfoDAO, InMemoryAuthInfoDAO}
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import models.user.{User, UserService}
import models.user.Roles.Role
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.mvc.{CookieHeaderEncoding, Request}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class SilhouetteSecurity  extends AbstractModule with ScalaModule {
  
  private def getAppSecret(config: Configuration) =
    config.get[String]("play.crypto.secret")
  
  override def configure(): Unit = {
    bind[Silhouette[Security.Env]].to[SilhouetteProvider[Security.Env]]
    bind[DelegableAuthInfoDAO[PasswordInfo]].toInstance(new InMemoryAuthInfoDAO[PasswordInfo])
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[CookieAuthenticator],
    eventBus: EventBus
  ): Environment[Security.Env] = Environment[Security.Env](
    userService,
    authenticatorService,
    Seq(),
    eventBus
  )
 
  @Provides
  def provideAuthenticatorService(
    @Named("authenticator-signer") signer: Signer,
    @Named("authenticator-crypter") crypter: Crypter,
    cookieHeaderEncoding: CookieHeaderEncoding,
    fingerprintGenerator: FingerprintGenerator,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock
  ): AuthenticatorService[CookieAuthenticator] = {
    val config = CookieAuthenticatorSettings(
      cookieName = "id",
      cookiePath = "/",
      cookieDomain = None,
      secureCookie = false, // Send cookie in HTTP and HTTPS modes
      httpOnlyCookie = true, // Not accessible through JS
      useFingerprinting = true,
      cookieMaxAge = None,
      authenticatorIdleTimeout = None,
      authenticatorExpiry = 1.hour)

    val enc = new CrypterAuthenticatorEncoder(crypter)
    new CookieAuthenticatorService(config, None, signer, cookieHeaderEncoding, enc  , fingerprintGenerator, idGenerator, clock)
  }

  @Provides
  def provideAuthInfoRepository(passwordDAO: DelegableAuthInfoDAO[PasswordInfo]): AuthInfoRepository =
    new DelegableAuthInfoRepository(passwordDAO)

  @Provides
  def providePasswordHasherRegistry(passwordHasher: PasswordHasher): PasswordHasherRegistry =
    PasswordHasherRegistry(passwordHasher)

  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository,
    passwordHasherRegistry: PasswordHasherRegistry
  ): CredentialsProvider =
    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)

  @Provides
  @Named("authenticator-signer")
  def provideAuthenticatorSigner(configuration: Configuration): Signer = {
    val settings = JcaSignerSettings(getAppSecret(configuration))
    new JcaSigner(settings)
  }

  @Provides
  @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val settings = JcaCrypterSettings(getAppSecret(configuration))
    new JcaCrypter(settings)
  }

}


object Security {
  
  val PROVIDER_ID = "recogito.pelagios.org"
  
  trait Env extends com.mohiva.play.silhouette.api.Env {
    
    type I = User
    
    type A = CookieAuthenticator
    
  }
  
  case class WithRole(role: Role) extends Authorization[User, CookieAuthenticator] {

    def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request: Request[B]) = {
      Future.successful(user.hasRole(role))
    }
    
  }
  
}