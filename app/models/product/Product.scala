package models.product

import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant
import monocle.macros.GenLens
import org.json4s.JsonAST.JValue
import models.Aliases.Json

/**
 * A Product is composed of two parts. The Product Form and the Product Shadow.
 * Below describes the product form.
 */
final case class Product(id: Int = 0, attributes: Json, variants: Json, skus: Json, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Product]
  with Validation[Product]

class Products(tag: Tag) extends GenericTable.TableWithId[Product](tag, "products")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def attributes = column[Json]("attributes")
  def variants = column[Json]("variants")
  def skus = column[Json]("skus")
  def createdAt = column[Instant]("created_at")

  def * = (id, attributes, variants, skus, createdAt) <> ((Product.apply _).tupled, Product.unapply)
}

object Products extends TableQueryWithId[Product, Products](
  idLens = GenLens[Product](_.id))(new Products(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByAttributes(key: String, value: String): QuerySeq = 
    filter(_.attributes+>>(key) === value)
  def filterByVariants(key: String, value: String) : QuerySeq =
    filter(_.variants+>>(key) === value)
}
