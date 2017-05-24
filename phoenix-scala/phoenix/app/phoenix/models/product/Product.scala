package phoenix.models.product

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import failures._
import objectframework.models._
import objectframework.services.ObjectManager
import phoenix.failures.ArchiveFailures.ProductIsPresentInCarts
import phoenix.failures.ProductFailures
import phoenix.failures.ProductFailures._
import phoenix.models.cord.lineitems.CartLineItems
import phoenix.models.objects.ProductSkuLinks
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import shapeless._
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

import scala.util.matching.Regex

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
                   slug: String = "",
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

  override def sanitize: Product = super.sanitize.copy(slug = slug.toLowerCase)

}

class Products(tag: Tag) extends ObjectHeads[Product](tag, "products") {

  def slug = column[String]("slug")

  def * =
    (id, slug, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Product.apply _).tupled, Product.unapply)
}

object Products
    extends ObjectHeadsQueries[Product, Products](new Products(_))
    with ReturningIdAndString[Product, Products] {

  val returningLens: Lens[Product, (Int, String)] = {
    val productLens = lens[Product]
    productLens.id ~ productLens.slug
  }

  override val returningQuery = map(p ⇒ (p.id, p.slug))

  implicit val formats = JsonFormatters.phoenixFormats

  override def create(unsaved: Product)(implicit ec: EC): DbResultT[Product] =
    super.create(unsaved).resolveFailures(ErrorResolver.resolveSlugError(unsaved))

  override def update(oldModel: Product, newModel: Product)(implicit ec: EC): DbResultT[Product] =
    super.update(oldModel, newModel).resolveFailures(ErrorResolver.resolveSlugError(newModel))

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
      case ProductId(id) ⇒
        mustFindProductByContextAndFormId404(oc.id, id)
      case ProductSlug(slug) ⇒
        filter(p ⇒ p.contextId === oc.id && p.slug.toLowerCase === slug.toLowerCase())
          .mustFindOneOr(ProductFailures.ProductNotFoundForContext(slug, oc.id))
    }
  }

  def mustFindFullByReference(
      ref: ProductReference)(implicit oc: OC, ec: EC, db: DB): DbResultT[FullObject[Product]] =
    ObjectManager.getFullObject(mustFindByReference(ref: ProductReference))

  private object ErrorResolver {
    val slugDuplicatedRegex: Regex =
      "ERROR: duplicate key value violates unique constraint \"product_slug_idx\".*".r

    def resolveSlugError(product: Product): PartialFunction[Failure, Failure] = {
      case DatabaseFailure(message) if slugDuplicatedRegex.findFirstIn(message).isDefined ⇒
        SlugDuplicates(product.slug)
    }
  }
}
