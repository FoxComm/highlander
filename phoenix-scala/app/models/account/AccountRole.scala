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

case class AccountRole(id: Int = 0, accountId: Int, roleId: Int) extends FoxModel[AccountRole]

class AccountRoles(tag: Tag) extends FoxTable[AccountRole](tag, "roles") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId = column[Int]("account_id")
  def roleId    = column[Int]("role_id")
  def * =
    (id, accountId, roleId) <> ((AccountRole.apply _).tupled, AccountRole.unapply)
}

object AccountRoles
    extends FoxTableQuery[AccountRole, AccountRoles](new AccountRoles(_))
    with ReturningId[AccountRole, AccountRoles] {

  val returningLens: Lens[AccountRole, Int] = lens[AccountRole].id
}
