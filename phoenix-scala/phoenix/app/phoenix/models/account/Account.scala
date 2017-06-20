package phoenix.models.account

import java.time.Instant

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class Account(id: Int = 0,
                   ratchet: Int = 0,
                   createdAt: Instant = Instant.now,
                   updatedAt: Instant = Instant.now,
                   deletedAt: Option[Instant] = None)
    extends FoxModel[Account]

object Account {
  type Claims = Map[String, List[String]]
  case class ClaimSet(scope: String, roles: Seq[String], claims: Account.Claims)

}

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

  val returningLens: Lens[Account, Int] = lens[Account].id

  def findByIdAndRatchet(id: Int, ratchet: Int): DBIO[Option[Account]] =
    filter(_.id === id).filter(_.ratchet === ratchet).one
}
