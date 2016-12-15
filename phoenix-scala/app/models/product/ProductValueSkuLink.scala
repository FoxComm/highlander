package models.product

import java.time.Instant

import models.inventory.ProductVariants
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ProductValueSkuLink(id: Int = 0,
                               leftId: Int,
                               rightId: Int,
                               createdAt: Instant = Instant.now,
                               updatedAt: Instant = Instant.now)
    extends FoxModel[ProductValueSkuLink]

class ProductValueSkuLinks(tag: Tag)
    extends FoxTable[ProductValueSkuLink](tag, "product_value_sku_links") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId    = column[Int]("left_id")
  def rightId   = column[Int]("right_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductValueSkuLink.apply _).tupled, ProductValueSkuLink.unapply)

  def left  = foreignKey(ProductValues.tableName, leftId, ProductValues)(_.id)
  def right = foreignKey(ProductVariants.tableName, rightId, ProductVariants)(_.id)
}

object ProductValueSkuLinks
    extends FoxTableQuery[ProductValueSkuLink, ProductValueSkuLinks](new ProductValueSkuLinks(_))
    with ReturningId[ProductValueSkuLink, ProductValueSkuLinks] {

  val returningLens: Lens[ProductValueSkuLink, Int] = lens[ProductValueSkuLink].id

  def filterLeft(leftId: Int): QuerySeq = filter(_.leftId === leftId)

  def filterLeft(leftIds: Seq[Int]): QuerySeq = filter(_.leftId.inSet(leftIds))

  def findSkusForVariantValues(
      variantValueHeadIds: Seq[Int]): Query[(Rep[Int], Rep[String]), (Int, String), Seq] = {
    for {
      link ← filterLeft(variantValueHeadIds)
      sku  ← ProductVariants if link.rightId === sku.id
    } yield (link.leftId, sku.code)
  }
}
