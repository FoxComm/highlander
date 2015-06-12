package services

import models._
import akka.http.scaladsl.server.directives._
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}



// TODO: Implement real session-based authentication with JWT
class CustomerAuthenticator
object CustomerAuthenticator {
  def auth(userCredentials: UserCredentials)(implicit ec: ExecutionContext, db: Database): Future[Option[Customer]] =
  {
    userCredentials match {
      case p: UserCredentials.Provided =>
        Customer.findByEmail(p.username).map {
          case Some(u) =>
            // TODO: Actually hash passwords and check against those
            if (p.verifySecret(u.password)) {
              Some(u)
            } else {
              None
            }
          case None => None
        }
      case _ =>
        println("Missing motherfucker")
        Future(None)
    }
  }
}
