package models.customer

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures.UserFailures._
import failures._
import models.location._
import models.payment.creditcard.CreditCards
import payloads.CustomerPayloads.CreateCustomerPayload
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Passwords._
import utils.Validation
import utils.aliases._
import utils.db._

case class CustomerUser(id: Int = 0,
                        userId: Int,
                        accountId: Int,
                        isGuest: Boolean = false,
                        createdAt: Instant = Instant.now,
                        udpatedAt: Instant = Instant.now,
                        deletedAt: Option[Instant] = None)
    extends FoxModel[CustomerUser]
    with Validation[CustomerUser] {

  import Validation._

  def updatePassword(newPassword: String): CustomerUser = {
    this.copy(hashedPassword = hashPassword(newPassword).some)
  }
}

class CustomerUsers(tag: Tag) extends FoxTable[CustomerUser](tag, "customer_users") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId    = column[Int]("user_id")
  def accountId = column[Int]("account_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, userId, accountId, isGuest, createdAt, updatedAt, deletedAt) <> ((CustomerUser.apply _).tupled, CustomerUser.unapply)
}

object CustomerUsers
    extends FoxTableQuery[CustomerUser, CustomerUsers](new CustomerUsers(_))
    with ReturningId[CustomerUser, CustomerUsers] {

  val returningLens: Lens[CustomerUser, Int] = lens[CustomerUser].id

  def findGuests(email: String): DBIO[Option[CustomerUser]] = {
    filter(_.isGuest === true).one
  }

  def mustFindByAccountId(accountId: Int): DbResultT[User] = 
    filter(_.accountId === accountId).one.mustFindOneOr(UserWithAccountNotFound(accountId))
}
