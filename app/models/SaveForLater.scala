package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class SaveForLater(id: Int = 0, customerId: Int = 0, 
  skuId: Int, productId: Int, skuShadowId: Int, productShadowId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[SaveForLater] {

}

object SaveForLater {

}

class SaveForLaters(tag: Tag) extends GenericTable.TableWithId[SaveForLater](tag, "save_for_later") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def skuId = column[Int]("sku_id")
  def productId = column[Int]("product_id")
  def skuShadowId = column[Int]("sku_shadow_id")
  def productShadowId = column[Int]("product_shadow_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, customerId, skuId, productId, skuShadowId, productShadowId, createdAt) <>((SaveForLater.apply _).tupled, SaveForLater.unapply)

  def customer = foreignKey(Customers.tableName, customerId, Customers)(_.id)
}

object SaveForLaters extends TableQueryWithId[SaveForLater, SaveForLaters](
  idLens = GenLens[SaveForLater](_.id)
)(new SaveForLaters(_)) {

  def find(customerId: Int, skuId: Int): QuerySeq =
    filter(_.customerId === customerId).filter(_.skuId === skuId)
}
