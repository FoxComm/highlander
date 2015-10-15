package services

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.directives._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import models._

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication
object Authenticator {
  type EmailFinder[M] = String ⇒ DBIO[Option[M]]

  def customer(credentials: UserCredentials)
              (implicit ec: ExecutionContext, db: Database): Future[Option[Customer]] = {
    auth[Customer, EmailFinder[Customer]](credentials, Customers.findByEmail, (m) ⇒ m.password.getOrElse(""))
  }

  def storeAdmin(credentials: UserCredentials)
                (implicit ec: ExecutionContext, db: Database): Future[Option[StoreAdmin]] = {
    auth[StoreAdmin, EmailFinder[StoreAdmin]](credentials, StoreAdmins.findByEmail, _.password)
  }

  private[this] def auth[M, F <: EmailFinder[M]](credentials: UserCredentials, finder: F, getPassword: M ⇒ String)
   (implicit ec: ExecutionContext, db: Database): Future[Option[M]] = credentials match {

    case p: UserCredentials.Provided ⇒
      finder(p.username).run().map { optModel ⇒
        optModel.filter { m ⇒ p.verifySecret(getPassword(m)) }
      }

    case _ ⇒
      Future.successful(None)
  }
}
