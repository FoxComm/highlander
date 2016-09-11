package models.account

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures._
import shapeless._
import utils.Validation
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class Account(id: Int = 0,
                   ratchet: Int = 0,
                   createdAt: Instant = Instant.now,
                   updatedAt: Instant = Instant.now,
                   deletedAt: Option[Instant] = None)
    extends FoxModel[Account]

class Accounts(tag: Tag) extends FoxTable[Account](tag, "accounts") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def ratchet   = column[Int]("ratchet")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, ratchet, createdAt, updatedAt, deletedAt) <> ((Account.apply _).tupled, Account.unapply)
}

object Accounts
    extends FoxTableQuery[Account, Accounts](new Accounts(_))
    with ReturningId[Account, Accounts] {

  type Claims = Map[String, List[String]]

  val returningLens: Lens[Account, Int] = lens[Account].id

  def findByIdAndRatchet(id: Int, ratchet: Int): DBIO[Option[Account]] = {
    filter(_.id === id).filter(_.ratchet === ratchet).one
  }
}
