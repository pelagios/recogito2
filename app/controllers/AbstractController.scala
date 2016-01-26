package controllers

import database.DB
import play.api.mvc.Controller

/** Helper trait so we can hand the injected DB down to the AuthConfigImpl trait **/
trait HasDatabase { def db: DB }

/** Currently (mostly) a placeholder for future common Controller functionality **/
abstract class AbstractController extends Controller with HasDatabase {
    
}