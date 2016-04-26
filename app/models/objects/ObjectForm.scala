package models.objects

import java.time.Instant

import models.Aliases.Json
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ObjectForm(id: Int = 0, kind: String, attributes: Json,
  updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now) 
extends FoxModel[ObjectForm]

object ObjectForm {
  val product   = "product"
  val sku       = "sku"
  val promotion = "promotion"
  val coupon    = "coupon"
}

// This table mostly acts a placeholder in our system.  We may or may not import objects from 'origin' into this.
class ObjectForms(tag: Tag) extends FoxTable[ObjectForm](tag, "object_forms")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def kind = column[String]("kind")
  def attributes = column[Json]("attributes")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def * = (id, kind, attributes, updatedAt, createdAt) <> ((ObjectForm.apply _).tupled, ObjectForm.unapply)
}

object ObjectForms extends FoxTableQuery[ObjectForm, ObjectForms](
  idLens = lens[ObjectForm].id
  )(new ObjectForms(_)) {

}
