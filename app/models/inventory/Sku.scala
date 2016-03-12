package models.inventory

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant

object Sku {
  val kind = "sku"
  val skuCodeRegex = """([a-zA-Z0-9-_]*)""".r
}

/**
 * A Sku is a pointer to a commit of a sku. A ObjectContext is a
 * collection of Skus.
 */
final case class Sku(id: Int = 0, code: String, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Sku]
  with Validation[Sku]

class Skus(tag: Tag) extends GenericTable.TableWithId[Sku](tag, "skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def contextId = column[Int]("context_id")
  def code = column[String]("code")
  def shadowId = column[Int]("shadow_id")
  def formId = column[Int]("form_id")
  def commitId = column[Int]("commit_id")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def * = (id, code, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Sku.apply _).tupled, Sku.unapply)

  def context = foreignKey(ObjectContexts.tableName, contextId, ObjectContexts)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, shadowId, ObjectShadows)(_.id)
  def form = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
  def commit = foreignKey(ObjectCommits.tableName, commitId, ObjectCommits)(_.id)

}

object Skus extends TableQueryWithId[Sku, Skus](
  idLens = GenLens[Sku](_.id))(new Skus(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)
  def filterByContextAndCode(contextId: Int, code: String): QuerySeq = 
    filter(_.contextId === contextId).filter(_.code === code)
  def filterByCode(code: String): QuerySeq = 
    filter(_.code === code)
}
