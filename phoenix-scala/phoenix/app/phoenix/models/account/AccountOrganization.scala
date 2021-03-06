package phoenix.models.account

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class AccountOrganization(id: Int = 0, accountId: Int, organizationId: Int)
    extends FoxModel[AccountOrganization]

class AccountOrganizations(tag: Tag) extends FoxTable[AccountOrganization](tag, "account_organizations") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId      = column[Int]("account_id")
  def organizationId = column[Int]("organization_id")
  def * =
    (id, accountId, organizationId) <> ((AccountOrganization.apply _).tupled, AccountOrganization.unapply)

  def account      = foreignKey(Accounts.tableName, accountId, Accounts)(_.id)
  def organization = foreignKey(Organizations.tableName, organizationId, Organizations)(_.id)
}

object AccountOrganizations
    extends FoxTableQuery[AccountOrganization, AccountOrganizations](new AccountOrganizations(_))
    with ReturningId[AccountOrganization, AccountOrganizations] {

  val returningLens: Lens[AccountOrganization, Int] = lens[AccountOrganization].id

  def filterByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)
}
