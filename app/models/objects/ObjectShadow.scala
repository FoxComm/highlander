package models.objects

import java.time.Instant

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A ObjectShadow is what you get when a context illuminates a Object.
 * The ObjectShadow, when applied to a Object is what is displayed on the 
 * storefront.
 */
case class ObjectShadow(id: Int = 0, formId: Int = 0, attributes: Json, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ObjectShadow]
  with Validation[ObjectShadow]

class ObjectShadows(tag: Tag) extends GenericTable.TableWithId[ObjectShadow](tag, "object_shadows")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def formId = column[Int]("form_id")
  def attributes = column[Json]("attributes")
  def createdAt = column[Instant]("created_at")

  def * = (id, formId, attributes, createdAt) <> ((ObjectShadow.apply _).tupled, ObjectShadow.unapply)

  def form = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
}

object ObjectShadows extends TableQueryWithId[ObjectShadow, ObjectShadows](
  idLens = GenLens[ObjectShadow](_.id))(new ObjectShadows(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByForm(formId: Int): QuerySeq = 
    filter(_.formId === formId)
  def filterByAttributes(key: String, value: String): QuerySeq = 
    filter(_.attributes+>>(key) === value)
}
