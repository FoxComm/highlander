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
final case class ProductShadow(id: Int = 0, productId: Int, 
  attributes: Json, variants: String, activeFrom: Option[Instant] = None, 
  activeTo: Option[Instant] = None,
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ProductShadow]
  with Validation[ProductShadow]

class ProductShadows(tag: Tag) extends GenericTable.TableWithId[ProductShadow](tag, "product_shadows")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def productId = column[Int]("product_id")
  def attributes = column[Json]("attributes")
  def variants = column[String]("variants")
  def activeFrom = column[Option[Instant]]("active_from")
  def activeTo = column[Option[Instant]]("active_to")
  def createdAt = column[Instant]("created_at")

  def * = (id, productId, attributes, variants, activeFrom, activeTo, createdAt) <> ((ProductShadow.apply _).tupled, ProductShadow.unapply)

  def product = foreignKey(Products.tableName, productId, Products)(_.id)
}

object ProductShadows extends TableQueryWithId[ProductShadow, ProductShadows](
  idLens = GenLens[ProductShadow](_.id))(new ProductShadows(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByProduct(productId: Int): QuerySeq = 
    filter(_.productId === productId)
  def filterByAttributes(key: String, value: String): QuerySeq = 
    filter(_.attributes+>>(key) === value)
}
