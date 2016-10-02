package models.product

import java.time.Instant
import com.github.tminglei.slickpg.LTree

import models.objects._
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db._
import utils.{JsonFormatters, Validation}

object Product {
  val kind = "product"
}

/**
  * A Product represents something sellable in our system and has a set of
  * skus related to it. This data structure is a pointer to a specific version
  * of a product in the object context referenced. The product may have a different
  * version in a different context. A product is represented in the object form
  * and shadow system where it has attributes controlled by the customer.
  */
case class Product(id: Int = 0,
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
}

class Products(tag: Tag) extends ObjectHeads[Product](tag, "products") {

  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Product.apply _).tupled, Product.unapply)
}

object Products
    extends FoxTableQuery[Product, Products](new Products(_))
    with ReturningId[Product, Products] {

  val returningLens: Lens[Product, Int] = lens[Product].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)
}
