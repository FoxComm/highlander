package models.product

import java.time.Instant

import models.inventory.ProductVariants
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ProductValueVariantLink(id: Int = 0,
                                   leftId: Int,
                                   rightId: Int,
                                   createdAt: Instant = Instant.now,
                                   updatedAt: Instant = Instant.now)
    extends FoxModel[ProductValueVariantLink]

class ProductValueVariantLinks(tag: Tag)
    extends FoxTable[ProductValueVariantLink](tag, "product_value__variant_links") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId    = column[Int]("left_id")
  def rightId   = column[Int]("right_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductValueVariantLink.apply _).tupled, ProductValueVariantLink.unapply)

  def left  = foreignKey(ProductValues.tableName, leftId, ProductValues)(_.id)
  def right = foreignKey(ProductVariants.tableName, rightId, ProductVariants)(_.id)
}

object ProductValueVariantLinks
    extends FoxTableQuery[ProductValueVariantLink, ProductValueVariantLinks](
        new ProductValueVariantLinks(_))
    with ReturningId[ProductValueVariantLink, ProductValueVariantLinks] {

  val returningLens: Lens[ProductValueVariantLink, Int] = lens[ProductValueVariantLink].id

  def filterLeft(leftId: Int): QuerySeq = filter(_.leftId === leftId)

  def filterLeft(leftIds: Seq[Int]): QuerySeq = filter(_.leftId.inSet(leftIds))

  def findVariantsForProductValues(
      productValueHeadIds: Seq[Int]): Query[(Rep[Int], Rep[String]), (Int, String), Seq] = {
    for {
      link    ← filterLeft(productValueHeadIds)
      variant ← ProductVariants if link.rightId === variant.id
    } yield (link.leftId, variant.code)
  }
}
