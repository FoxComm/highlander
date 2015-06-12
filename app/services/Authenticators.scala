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

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication
class AdminUserAuthenticator
object AdminUserAuthenticator {
  def auth(userCredentials: UserCredentials)(implicit ec: ExecutionContext, db: Database): Future[Option[AdminUser]] =
  {
    userCredentials match {
      case p: UserCredentials.Provided =>
        AdminUsers.findByEmail(p.username).map {
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