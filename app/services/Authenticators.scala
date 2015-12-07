package services

import akka.http.scaladsl.server.directives._
import models.{Customer, Customers, StoreAdmin, StoreAdmins}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

import scala.concurrent.{ExecutionContext, Future}

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication
object Authenticator {
  type EmailFinder[M] = String ⇒ DBIO[Option[M]]

  def customer(credentials: UserCredentials)
              (implicit ec: ExecutionContext, db: Database): Future[Option[Customer]] = {
    auth[Customer, EmailFinder[Customer]](credentials, Customers.findByEmail, _.password)
  }

  def storeAdmin(credentials: UserCredentials)
                (implicit ec: ExecutionContext, db: Database): Future[Option[StoreAdmin]] = {
    auth[StoreAdmin, EmailFinder[StoreAdmin]](credentials, StoreAdmins.findByEmail, (m) ⇒ Some(m.password))
  }

  private[this] def auth[M, F <: EmailFinder[M]](credentials: UserCredentials, finder: F, getPassword: M ⇒
    Option[String])
   (implicit ec: ExecutionContext, db: Database): Future[Option[M]] = credentials match {

    case p: UserCredentials.Provided ⇒
      finder(p.username).run().map { optModel ⇒
        optModel.filter { m ⇒ getPassword(m).map(p.verifySecret(_)).getOrElse(false) }
      }
    case _ ⇒
      Future.successful(None)
  }
}
