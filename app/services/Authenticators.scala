package services

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls
import akka.http.scaladsl.server.directives._
import slick.driver.PostgresDriver.api._

import models._

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication
object Authenticator {
  type Model = { val password: String }
  type EmailFinder[M] = String => Future[Option[M]]

  def customer(credentials: UserCredentials)
              (implicit ec: ExecutionContext, db: Database): Future[Option[Customer]] = {
    auth[Customer, EmailFinder[Customer]](credentials, Customers.findByEmail)
  }

  def storeAdmin(credentials: UserCredentials)
                (implicit ec: ExecutionContext, db: Database): Future[Option[StoreAdmin]] = {
    auth[StoreAdmin, EmailFinder[StoreAdmin]](credentials, StoreAdmins.findByEmail)
  }

  private[this] def auth[M <: Model, F <: EmailFinder[M]](credentials: UserCredentials, finder: F)
                                                         (implicit ec: ExecutionContext, db: Database): Future[Option[M]] = credentials match {
    case p: UserCredentials.Provided =>
      finder(p.username).map { optModel =>
        optModel.filter { m => p.verifySecret(m.password) }
      }

    case _ =>
      Future.successful(None)
  }
}
