package models.account

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Passwords._
import utils.Validation
import utils.aliases._
import utils.db._

case class AccountAccessMethod(id: Int = 0,
                               accountId: Int,
                               name: String,
                               hashedPassword: String,
                               algorithm: Int = 0, //0 means scrypt, rest are reserved for future
                               createdAt: Instant = Instant.now,
                               updatedAt: Instant = Instant.now,
                               deletedAt: Option[Instant] = None)
    extends FoxModel[AccountAccessMethod] {

  def updatePassword(newPassword: String): AccountAccessMethod = {
    this.copy(hashedPassword = hashPassword(newPassword))
  }
}

object AccountAccessMethod {

  def build(accountId: Int, name: String, password: String): AccountAccessMethod =
    AccountAccessMethod(accountId = accountId,
                        name = name,
                        hashedPassword = hashPassword(password))
}

class AccountAccessMethods(tag: Tag)
    extends FoxTable[AccountAccessMethod](tag, "account_access_methods") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId      = column[Int]("account_id")
  def name           = column[String]("name")
  def hashedPassword = column[String]("hashed_password")
  def algorithm      = column[Int]("algorithm")
  def createdAt      = column[Instant]("created_at")
  def updatedAt      = column[Instant]("updated_at")
  def deletedAt      = column[Option[Instant]]("deleted_at")

  def * =
    (id, accountId, name, hashedPassword, algorithm, createdAt, updatedAt, deletedAt) <> ((AccountAccessMethod.apply _).tupled, AccountAccessMethod.unapply)
}

object AccountAccessMethods
    extends FoxTableQuery[AccountAccessMethod, AccountAccessMethods](new AccountAccessMethods(_))
    with ReturningId[AccountAccessMethod, AccountAccessMethods] {

  val returningLens: Lens[AccountAccessMethod, Int] = lens[AccountAccessMethod].id

  def findByName(name: String): DBIO[Option[AccountAccessMethod]] = {
    filter(_.name === name).one
  }

}
