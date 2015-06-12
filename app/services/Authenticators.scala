package services

import models._
import akka.http.scaladsl.server.directives._
import slick.lifted.TableQuery
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.language.reflectiveCalls

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication
object Authenticator {
  type Model = { val password: String }
  type EmailFinder[M] = String => Future[Option[M]]

  def authCustomer(credentials: UserCredentials)
                  (implicit ec: ExecutionContext, db: Database): Future[Option[Customer]] = {
    auth[Customer, EmailFinder[Customer]](credentials, Customers.findByEmail)
  }

  def authAdmin(credentials: UserCredentials)
               (implicit ec: ExecutionContext, db: Database): Future[Option[AdminUser]] = {
    auth[AdminUser, EmailFinder[AdminUser]](credentials, AdminUsers.findByEmail)
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
