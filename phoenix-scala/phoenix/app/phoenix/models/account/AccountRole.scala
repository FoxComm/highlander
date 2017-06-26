package phoenix.models.account

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class AccountRole(id: Int = 0, accountId: Int, roleId: Int) extends FoxModel[AccountRole]

class AccountRoles(tag: Tag) extends FoxTable[AccountRole](tag, "account_roles") {
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

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)
}
