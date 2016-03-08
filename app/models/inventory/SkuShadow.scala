package models.inventory

import java.time.Instant

import models.Aliases.Json
import models.product.ProductContexts
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A SkuShadow is what you get when a context illuminates a Sku.
 * The SkuShadow, when applied to a Sku is what is displayed on the 
 * storefront.
 */
final case class SkuShadow(id: Int = 0, productContextId: Int, skuId: Int, 
  attributes: Json, activeFrom: Option[Instant] = None, activeTo: Option[Instant] = None, 
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[SkuShadow]
  with Validation[SkuShadow]

class SkuShadows(tag: Tag) extends GenericTable.TableWithId[SkuShadow](tag, "sku_shadows")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def productContextId = column[Int]("product_context_id")
  def skuId = column[Int]("sku_id")
  def attributes = column[Json]("attributes")
  def activeFrom = column[Option[Instant]]("active_from")
  def activeTo = column[Option[Instant]]("active_to")
  def createdAt = column[Instant]("created_at")

  def * = (id, productContextId, skuId, attributes, activeFrom, activeTo, createdAt) <> ((SkuShadow.apply _).tupled, SkuShadow.unapply)

  def productContext = foreignKey(ProductContexts.tableName, productContextId, ProductContexts)(_.id)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object SkuShadows extends TableQueryWithId[SkuShadow, SkuShadows](
  idLens = GenLens[SkuShadow](_.id))(new SkuShadows(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(productContextId: Int): QuerySeq = 
    filter(_.productContextId === productContextId)
  def filterBySku(skuId: Int): QuerySeq = 
    filter(_.skuId === skuId)
  def filterByAttributes(key: String, value: String): QuerySeq = 
    filter(_.attributes+>>(key) === value)
  def filterBySkuAndContext(skuId: Int, productContextId: Int): QuerySeq = 
    filter(_.skuId === skuId).filter(_.productContextId === productContextId)
}
