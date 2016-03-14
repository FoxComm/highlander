package models.product

import java.time.Instant

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A ProductShadow is what you get when a context illuminates a Product.
 * The ProductShadow, when applied to a Product is what is displayed on the 
 * storefront.
 */
final case class ProductShadow(id: Int = 0, productContextId: Int, productId: Int, 
  attributes: Json, variants: String, skus: String, activeFrom: Option[Instant] = None,
  activeTo: Option[Instant] = None,
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ProductShadow]
  with Validation[ProductShadow]

class ProductShadows(tag: Tag) extends GenericTable.TableWithId[ProductShadow](tag, "product_shadows")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def productContextId = column[Int]("product_context_id")
  def productId = column[Int]("product_id")
  def attributes = column[Json]("attributes")
  def variants = column[String]("variants")
  def skus = column[String]("skus")
  def activeFrom = column[Option[Instant]]("active_from")
  def activeTo = column[Option[Instant]]("active_to")
  def createdAt = column[Instant]("created_at")

  def * = (id, productContextId, productId, attributes, variants, skus, activeFrom, activeTo, createdAt) <> (
    (ProductShadow.apply _).tupled, ProductShadow.unapply)

  def productContext = foreignKey(ProductContexts.tableName, productContextId, ProductContexts)(_.id)
  def product = foreignKey(Products.tableName, productId, Products)(_.id)
}

object ProductShadows extends TableQueryWithId[ProductShadow, ProductShadows](
  idLens = GenLens[ProductShadow](_.id))(new ProductShadows(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(productContextId: Int): QuerySeq = 
    filter(_.productContextId === productContextId)
  def filterByProduct(productId: Int): QuerySeq = 
    filter(_.productId === productId)
  def filterByAttributes(key: String, value: String): QuerySeq = 
    filter(_.attributes+>>(key) === value)
}
