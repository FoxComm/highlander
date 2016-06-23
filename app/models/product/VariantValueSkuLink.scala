package models.product

import java.time.Instant

import models.inventory.Skus
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class VariantValueSkuLink(id: Int = 0,
                               leftId: Int,
                               rightId: Int,
                               createdAt: Instant = Instant.now,
                               updatedAt: Instant = Instant.now)
    extends FoxModel[VariantValueSkuLink]

class VariantValueSkuLinks(tag: Tag)
    extends FoxTable[VariantValueSkuLink](tag, "variant_value_sku_links") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def leftId = column[Int]("left_id")

  def rightId = column[Int]("right_id")

  def createdAt = column[Instant]("created_at")

  def updatedAt = column[Instant]("updated_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((VariantValueSkuLink.apply _).tupled, VariantValueSkuLink.unapply)

  def left = foreignKey(Variants.tableName, leftId, Variants)(_.id)

  def right = foreignKey(Skus.tableName, rightId, Skus)(_.id)
}

object VariantValueSkuLinks
    extends FoxTableQuery[VariantValueSkuLink, VariantValueSkuLinks](new VariantValueSkuLinks(_))
    with ReturningId[VariantValueSkuLink, VariantValueSkuLinks] {

  val returningLens: Lens[VariantValueSkuLink, Int] = lens[VariantValueSkuLink].id

  def filterLeft(leftId: Int): QuerySeq = filter(_.leftId === leftId)

  def filterRight(rightId: Int): QuerySeq = filter(_.rightId === rightId)

  def filterLeft(leftIds: Seq[Int]): QuerySeq = filter(_.leftId.inSet(leftIds))

  def filterRight(rightIds: Seq[Int]): QuerySeq = filter(_.rightId.inSet(rightIds))

  def findSkusForVariantValues(
      variantValueHeadIds: Seq[Int]): Query[(Rep[Int], Rep[String]), (Int, String), Seq] = {
    val links = VariantValueSkuLinks.filterLeft(variantValueHeadIds)
    links.join(Skus).on { case (link, sku) ⇒ link.rightId === sku.id }.map {
      case (link, sku)                     ⇒ (link.leftId, sku.code)
    }
  }
}
