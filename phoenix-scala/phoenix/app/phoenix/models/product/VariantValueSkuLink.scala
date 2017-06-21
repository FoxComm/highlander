package phoenix.models.product

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import phoenix.models.inventory.Skus
import shapeless._

case class VariantValueSkuLink(id: Int = 0,
                               leftId: Int,
                               rightId: Int,
                               createdAt: Instant = Instant.now,
                               updatedAt: Instant = Instant.now,
                               archivedAt: Option[Instant] = None)
    extends FoxModel[VariantValueSkuLink]

class VariantValueSkuLinks(tag: Tag) extends FoxTable[VariantValueSkuLink](tag, "variant_value_sku_links") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId     = column[Int]("left_id")
  def rightId    = column[Int]("right_id")
  def createdAt  = column[Instant]("created_at")
  def updatedAt  = column[Instant]("updated_at")
  def archivedAt = column[Option[Instant]]("archived_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt, archivedAt) <> ((VariantValueSkuLink.apply _).tupled, VariantValueSkuLink.unapply)

  def left  = foreignKey(VariantValues.tableName, leftId, VariantValues)(_.id)
  def right = foreignKey(Skus.tableName, rightId, Skus)(_.id)
}

object VariantValueSkuLinks
    extends FoxTableQuery[VariantValueSkuLink, VariantValueSkuLinks](new VariantValueSkuLinks(_))
    with ReturningId[VariantValueSkuLink, VariantValueSkuLinks] {

  val returningLens: Lens[VariantValueSkuLink, Int] = lens[VariantValueSkuLink].id

  def filterLeft(leftId: Int): QuerySeq = filter(l ⇒ l.leftId === leftId && l.archivedAt.isEmpty)

  def filterLeft(leftIds: Seq[Int]): QuerySeq = filter(_.leftId.inSet(leftIds))

  def findSkusForVariantValues(
      variantValueHeadIds: Seq[Int]): Query[(Rep[Int], Rep[String]), (Int, String), Seq] =
    for {
      link ← filterLeft(variantValueHeadIds)
      sku  ← Skus if link.rightId === sku.id
    } yield (link.leftId, sku.code)
}
