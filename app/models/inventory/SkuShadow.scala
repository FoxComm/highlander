package models.inventory

import models.product.ProductContexts

import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant
import scala.concurrent.ExecutionContext
import monocle.macros.GenLens

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import models.product.Aliases.Json

/**
 * A SkuShadow is what you get when a context illuminates a Sku.
 * The SkuShadow, when applied to a Sku is what is displayed on the 
 * storefront.
 */
final case class SkuShadow(
  id: Int = 0, 
  productContextId: Int, 
  skuId: Int, 
  attributes: Json, 
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[SkuShadow]
  with Validation[SkuShadow]

class SkuShadows(tag: Tag) extends GenericTable.TableWithId[SkuShadow](tag, "sku_shadows")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def productContextId = column[Int]("product_context_id")
  def skuId = column[Int]("sku_id")
  def attributes = column[Json]("attributes")
  def createdAt = column[Instant]("created_at")

  def * = (id, productContextId, skuId, attributes, createdAt) <> ((SkuShadow.apply _).tupled, SkuShadow.unapply)

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
}
