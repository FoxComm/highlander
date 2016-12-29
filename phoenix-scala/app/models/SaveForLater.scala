package models

import java.time.Instant

import models.customer.CustomersData
import models.account._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class SaveForLater(id: Int = 0,
                        accountId: Int = 0,
                        productVariantId: Int,
                        createdAt: Instant = Instant.now)
    extends FoxModel[SaveForLater] {}

object SaveForLater {}

class SaveForLaters(tag: Tag) extends FoxTable[SaveForLater](tag, "save_for_later") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId        = column[Int]("account_id")
  def productVariantId = column[Int]("product_variant_id")
  def createdAt        = column[Instant]("created_at")

  def * =
    (id, accountId, productVariantId, createdAt) <> ((SaveForLater.apply _).tupled, SaveForLater.unapply)

  def account = foreignKey(Accounts.tableName, accountId, Accounts)(_.id)
}

object SaveForLaters
    extends FoxTableQuery[SaveForLater, SaveForLaters](new SaveForLaters(_))
    with ReturningId[SaveForLater, SaveForLaters] {

  val returningLens: Lens[SaveForLater, Int] = lens[SaveForLater].id

  def find(accountId: Int, productVariantId: Int): QuerySeq =
    filter(_.accountId === accountId).filter(_.productVariantId === productVariantId)
}
