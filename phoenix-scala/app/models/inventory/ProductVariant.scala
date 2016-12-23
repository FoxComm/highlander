package models.inventory

import java.time.Instant

import cats.data.Xor
import failures.ArchiveFailures.{LinkArchivedVariantFailure, VariantIsPresentInCarts}
import failures.Failures
import models.objects._
import shapeless._
import utils.JsonFormatters
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import com.github.tminglei.slickpg._
import models.cord.lineitems.CartLineItems

object ProductVariant {
  val kind         = "variant"
  val skuCodeRegex = """([a-zA-Z0-9-_]*)""".r
}

/**
  * A ProductVariant represents the latest and specific version of Product.
  * For example: Red T-shirt, size XL.
  * This data structure stores a pointer to a commit of a version of a variant in
  * the object context referenced. The same ProductVariant can have a different version
  * in a different context.
  */
case class ProductVariant(id: Int = 0,
                          scope: LTree,
                          code: String, /** sku code */
                          contextId: Int,
                          shadowId: Int,
                          formId: Int,
                          commitId: Int,
                          updatedAt: Instant = Instant.now,
                          createdAt: Instant = Instant.now,
                          archivedAt: Option[Instant] = None)
    extends FoxModel[ProductVariant]
    with ObjectHead[ProductVariant] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): ProductVariant =
    this.copy(shadowId = shadowId, commitId = commitId)

  def mustNotBeArchived[T](target: T, targetId: Any): Failures Xor ProductVariant = {
    if (archivedAt.isEmpty) Xor.right(this)
    else Xor.left(LinkArchivedVariantFailure(target, targetId, code).single)
  }

  def mustNotBePresentInCarts(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      inCartCount ← * <~ CartLineItems.filter(_.variantId === id).size.result
      _           ← * <~ failIf(inCartCount > 0, VariantIsPresentInCarts(code))
    } yield {}

}

class ProductVariants(tag: Tag) extends ObjectHeads[ProductVariant](tag, "product_variants") {

  def code = column[String]("code")

  def * =
    (id, scope, code, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((ProductVariant.apply _).tupled, ProductVariant.unapply)
}

object ProductVariants
    extends FoxTableQuery[ProductVariant, ProductVariants](new ProductVariants(_))
    with ReturningId[ProductVariant, ProductVariants]
    with SearchByCode[ProductVariant, ProductVariants] {

  val returningLens: Lens[ProductVariant, Int] = lens[ProductVariant].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)
  def filterByContextAndCode(contextId: Int, code: String): QuerySeq =
    filter(_.contextId === contextId).filter(_.code.toLowerCase === code.toLowerCase)
  def filterByCode(code: String): QuerySeq =
    filter(_.code.toLowerCase === code.toLowerCase)
  def findOneByCode(code: String): DBIO[Option[ProductVariant]] =
    filter(_.code.toLowerCase === code.toLowerCase).one
}
