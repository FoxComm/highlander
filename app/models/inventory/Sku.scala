package models.inventory

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import utils.table.SearchByCode
import utils.Slick.implicits._

import java.time.Instant

object Sku {
  val kind = "sku"
  val skuCodeRegex = """([a-zA-Z0-9-_]*)""".r
}

/**
 * A Sku represents the latest version of Stock Keeping Unit. 
 * This data structure stores a pointer to a commit of a version of a sku in 
 * the object context referenced. The same Sku can have a different version
 * in a different context.
 */
final case class Sku(id: Int = 0, code: String, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Sku]
  with Validation[Sku]

class Skus(tag: Tag) extends ObjectHeads[Sku](tag, "skus")  {

  def code = column[String]("code")

  def * = (id, code, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Sku.apply _).tupled, Sku.unapply)

}

object Skus extends TableQueryWithId[Sku, Skus](
  idLens = GenLens[Sku](_.id))(new Skus(_))
  with SearchByCode[Sku, Skus] {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)
  def filterByContextAndCode(contextId: Int, code: String): QuerySeq = 
    filter(_.contextId === contextId).filter(_.code.toLowerCase === code.toLowerCase)
  def filterByCode(code: String): QuerySeq = 
    filter(_.code.toLowerCase === code.toLowerCase)
  def findOneByCode(code: String): DBIO[Option[Sku]] = 
    filter(_.code.toLowerCase === code.toLowerCase).one
}
