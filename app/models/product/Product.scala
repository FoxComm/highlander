package models.product

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant

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
final case class Product(id: Int = 0, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Product]
  with Validation[Product]

class Products(tag: Tag) extends GenericTable.TableWithId[Product](tag, "products")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def contextId = column[Int]("context_id")
  def shadowId = column[Int]("shadow_id")
  def formId = column[Int]("form_id")
  def commitId = column[Int]("commit_id")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def * = (id, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Product.apply _).tupled, Product.unapply)

  def context = foreignKey(ObjectContexts.tableName, contextId, ObjectContexts)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, shadowId, ObjectShadows)(_.id)
  def form = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
  def commit = foreignKey(ObjectCommits.tableName, commitId, ObjectCommits)(_.id)

}

object Products extends TableQueryWithId[Product, Products](
  idLens = GenLens[Product](_.id))(new Products(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)
}
