package models.visit

import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class VisitService {
  
  def insertVisit(visit: Visit): Future[Boolean] = ???
  
}