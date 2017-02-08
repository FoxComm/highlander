package models.objects

import java.time.Instant

import cats.implicits._
import models.objects.ObjectHeadLinks._
import shapeless._
import models.inventory._
import models.product._
import utils.aliases._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class ProductSkuLink(id: Int = 0,
                          leftId: Int,
                          rightId: Int,
                          createdAt: Instant = Instant.now,
                          updatedAt: Instant = Instant.now,
                          archivedAt: Option[Instant] = None)
    extends FoxModel[ProductSkuLink]
    with ObjectHeadLink[ProductSkuLink]

class ProductSkuLinks(tag: Tag) extends ObjectHeadLinks[ProductSkuLink](tag, "product_sku_links") {

  def archivedAt: Rep[Option[Instant]] = column[Option[Instant]]("archived_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt, archivedAt) <> ((ProductSkuLink.apply _).tupled, ProductSkuLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Skus.tableName, rightId, Skus)(_.id)
}

object ProductSkuLinks
    extends ObjectHeadLinkQueries[ProductSkuLink, ProductSkuLinks, Product, Sku](
        new ProductSkuLinks(_),
        Products,
        Skus)
    with ReturningId[ProductSkuLink, ProductSkuLinks] {

  val returningLens: Lens[ProductSkuLink, Int] = lens[ProductSkuLink].id
  import scope._

  override def ensureLinked(left: Product, rights: Seq[Sku])(implicit ec: EC,
                                                             db: DB): DbResultT[Unit] =
    for {
      existingLinks ← * <~ filterLeft(left).filterNotArchived.result
      _             ← * <~ linkAllExceptExisting(left, rights, existingLinks)
    } yield {}

  override def unlinkAllExcept(left: Product, rights: Seq[Sku])(implicit ec: EC,
                                                                db: DB): DbResultT[Unit] = {
    val rightIds = rights.map(_.id)
    for {
      linksToBeDeleted ← * <~ filterLeft(left)
                          .filterNot(_.rightId inSet rightIds)
                          .filterNotArchived
                          .result
      _ ← * <~ linksToBeDeleted.map(link ⇒ update(link, link.copy(archivedAt = Instant.now.some)))
    } yield {}
  }

  def build(left: Product, right: Sku): ProductSkuLink =
    ProductSkuLink(leftId = left.id, rightId = right.id)

  object scope {
    implicit class ProductSkuLinksQuerySeqConversions(q: QuerySeq) {
      def filterNotArchived: QuerySeq = q.filter(_.archivedAt.isEmpty)
    }
  }
}
