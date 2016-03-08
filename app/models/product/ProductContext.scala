package models.product

import java.time.Instant

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A ProductContext stores information to determine which product shadow to show.
 * Each product shadow is associated with a context where it makes sense to display it.
 *
 * The context will be matched against a user context so that the storefront displays
 * the appropriate product information.
 */
final case class ProductContext(id: Int = 0, name: String, attributes: Json, 
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ProductContext]
  with Validation[ProductContext]

class ProductContexts(tag: Tag) extends GenericTable.TableWithId[ProductContext](tag, "product_contexts")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def attributes = column[Json]("attributes")
  def createdAt = column[Instant]("created_at")

  def * = (id, name, attributes, createdAt) <> ((ProductContext.apply _).tupled, ProductContext.unapply)

}

object ProductContexts extends TableQueryWithId[ProductContext, ProductContexts](
  idLens = GenLens[ProductContext](_.id))(new ProductContexts(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByName(name: String): QuerySeq = 
    filter(_.name === name)
  def filterByContextAttribute(key: String, value: String): QuerySeq = 
    filter(_.attributes+>>(key) === value)
}
