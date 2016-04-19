package models.product

import java.time.Instant

import models.javaTimeSlickMapper
import models.objects._
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.{JsonFormatters, ModelWithIdParameter, TableQueryWithId, Validation}

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
case class Product(id: Int = 0, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Product]
  with Validation[Product]

class Products(tag: Tag) extends ObjectHeads[Product](tag, "products")  {

  def * = (id, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Product.apply _).tupled, Product.unapply)

}

object Products extends TableQueryWithId[Product, Products](
  idLens = GenLens[Product](_.id))(new Products(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)
}
