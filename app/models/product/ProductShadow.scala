
package models.product

import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant
import monocle.macros.GenLens
import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}
import scala.concurrent.ExecutionContext
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import Aliases.Json

/**
 * A ProductShadow is what you get when a context illuminates a Product.
 * The ProductShadow, when applied to a Product is what is displayed on the 
 * storefront.
 */
final case class ProductShadow(
  id: Int = 0, 
  productContextId: Int, 
  attributes: Json, 
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ProductShadow]
  with Validation[ProductShadow]

class ProductShadows(tag: Tag) extends GenericTable.TableWithId[ProductShadow](tag, "product_shadows")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def productContextId = column[Int]("product_context_id")
  def attributes = column[Json]("attributes")
  def createdAt = column[Instant]("created_at")

  def * = (id, productContextId, attributes, createdAt) <> ((ProductShadow.apply _).tupled, ProductShadow.unapply)

  def productContext = foreignKey(ProductContexts.tableName, productContextId, ProductContexts)(_.id)
}

object ProductShadows extends TableQueryWithId[ProductShadow, ProductShadows](
  idLens = GenLens[ProductShadow](_.id))(new ProductShadows(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByAttributes(key: String, value: String): QuerySeq = 
    filter(_.attributes+>>(key) === value)
}
