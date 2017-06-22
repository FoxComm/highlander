package phoenix.models.catalog

import java.time.Instant

import shapeless._
import core.db._
import core.db.ExPostgresDriver.api._

case class CatalogProduct(id: Int,
                          catalogId: Int,
                          productId: Int,
                          createdAt: Instant,
                          archivedAt: Option[Instant])
    extends FoxModel[CatalogProduct]

object CatalogProduct {
  def buildSeq(catalogId: Int, productIds: Seq[Int]): Seq[CatalogProduct] =
    productIds.map(
      productId ⇒
        CatalogProduct(id = 0,
                       catalogId = catalogId,
                       productId = productId,
                       createdAt = Instant.now,
                       archivedAt = None))
}

class CatalogProducts(tag: Tag) extends FoxTable[CatalogProduct](tag, "catalog_products") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def catalogId  = column[Int]("catalog_id")
  def productId  = column[Int]("product_id")
  def createdAt  = column[Instant]("created_at")
  def archivedAt = column[Option[Instant]]("archived_at")

  def * =
    (id, catalogId, productId, createdAt, archivedAt) <> ((CatalogProduct.apply _).tupled, CatalogProduct.unapply)
}

object CatalogProducts
    extends FoxTableQuery[CatalogProduct, CatalogProducts](new CatalogProducts(_))
    with ReturningId[CatalogProduct, CatalogProducts] {
  val returningLens: Lens[CatalogProduct, Int] = lens[CatalogProduct].id

  def filterProduct(catalogId: Int, productId: Int): QuerySeq =
    filter(cp ⇒ cp.catalogId === catalogId && cp.productId === productId)
}
