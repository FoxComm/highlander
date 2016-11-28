package models.product

import java.time.Instant

import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.db._
import utils.{JsonFormatters, Validation}
import com.github.tminglei.slickpg.LTree
import failures.ArchiveFailures.ProductIsPresentInCarts
import failures.ProductFailures
import failures.ProductFailures.ProductFormNotFoundForContext
import models.cord.lineitems.CartLineItems
import services.objects.ObjectManager
import utils.aliases._

object Product {
  val kind = "product"
}

object ProductReference {
  def apply(formId: Int): ProductReference  = ProductId(formId)
  def apply(slug: String): ProductReference = ProductSlug(slug)
}

trait ProductReference

case class ProductId(formId: ObjectForm#Id) extends ProductReference
case class ProductSlug(slug: String)        extends ProductReference

/**
  * A Product represents something sellable in our system and has a set of
  * skus related to it. This data structure is a pointer to a specific version
  * of a product in the object context referenced. The product may have a different
  * version in a different context. A product is represented in the object form
  * and shadow system where it has attributes controlled by the customer.
  */
case class Product(id: Int = 0,
                   slug: Option[String] = None,
                   scope: LTree,
                   contextId: Int,
                   shadowId: Int,
                   formId: Int,
                   commitId: Int,
                   updatedAt: Instant = Instant.now,
                   createdAt: Instant = Instant.now,
                   archivedAt: Option[Instant] = None)
    extends FoxModel[Product]
    with Validation[Product]
    with ObjectHead[Product] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Product =
    this.copy(shadowId = shadowId, commitId = commitId)

  def mustNotBePresentInCarts(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      skus        ← * <~ ProductSkuLinks.filter(_.leftId === id).result
      inCartCount ← * <~ CartLineItems.filter(_.skuId.inSetBind(skus.map(_.rightId))).size.result
      _           ← * <~ failIf(inCartCount > 0, ProductIsPresentInCarts(formId))
    } yield {}

  def reference: ProductReference = ProductId(formId)
}

class Products(tag: Tag) extends ObjectHeads[Product](tag, "products") {

  def slug = column[Option[String]]("slug")

  def * =
    (id, slug, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Product.apply _).tupled, Product.unapply)
}

object Products
    extends ObjectHeadsQueries[Product, Products](new Products(_))
    with ReturningId[Product, Products] {

  val returningLens: Lens[Product, Int] = lens[Product].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def mustFindProductByContextAndFormId404(contextId: Int, formId: Int)(
      implicit ec: EC): DbResultT[Product] =
    Products
      .filter(_.contextId === contextId)
      .filter(_.formId === formId)
      .mustFindOneOr(ProductFormNotFoundForContext(formId, contextId))

  def mustFindByReference(reference: ProductReference)(implicit oc: OC,
                                                       ec: EC): DbResultT[Product] = {
    reference match {
      case ProductId(id) ⇒ mustFindProductByContextAndFormId404(oc.id, id)
      case ProductSlug(slug) ⇒
        filter(p ⇒ p.contextId === oc.id && p.slug === slug.toLowerCase())
          .mustFindOneOr(ProductFailures.ProductNotFoundForContext(slug, oc.id))
    }
  }

  def mustFindFullByReference(
      ref: ProductReference)(implicit oc: OC, ec: EC, db: DB): DbResultT[FullObject[Product]] =
    ObjectManager.getFullObject(mustFindByReference(ref: ProductReference))
}
