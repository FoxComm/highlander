package models.objects

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.Slick.implicits._
import utils.table.SearchByCode
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.time.JavaTimeSlickMapper._
import java.time.Instant

final case class ObjectForm(id: Int = 0, kind: String, attributes: Json, 
  updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now) 
extends ModelWithIdParameter[ObjectForm]

// This table mostly acts a placeholder in our system.  We may or may not import objects from 'origin' into this.
class ObjectForms(tag: Tag) extends GenericTable.TableWithId[ObjectForm](tag, "object_forms")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def kind = column[String]("kind")
  def attributes = column[Json]("attributes")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def * = (id, kind, attributes, updatedAt, createdAt) <> ((ObjectForm.apply _).tupled, ObjectForm.unapply)

}

object ObjectForms extends TableQueryWithId[ObjectForm, ObjectForms](
  idLens = GenLens[ObjectForm](_.id)
  )(new ObjectForms(_)) {

}
