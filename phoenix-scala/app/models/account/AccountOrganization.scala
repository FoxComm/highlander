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

case class AccountOrganization(id: Int = 0, accountId: Int, organizationId: Int)
    extends FoxModel[AccountOrganization]

class AccountOrganizations(tag: Tag) extends FoxTable[AccountOrganization](tag, "roles") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId      = column[Int]("account_id")
  def organizationId = column[Int]("organization_id")
  def * =
    (id, accountId, organizationId) <> ((AccountOrganization.apply _).tupled, AccountOrganization.unapply)
}

object AccountOrganizations
    extends FoxTableQuery[AccountOrganization, AccountOrganizations](new AccountOrganizations(_))
    with ReturningId[AccountOrganization, AccountOrganizations] {

  val returningLens: Lens[AccountOrganization, Int] = lens[AccountOrganization].id
}
