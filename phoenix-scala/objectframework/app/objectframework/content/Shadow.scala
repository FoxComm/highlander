package objectframework.content

import java.time.Instant

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import shapeless._
import core.db.ExPostgresDriver.api._
import core.db._

case class Shadow(id: Int = 0,
                  formId: Form#Id,
                  attributes: JValue,
                  relations: Option[JValue],
                  createdAt: Instant = Instant.now)
    extends FoxModel[Shadow]

class Shadows(tag: Tag) extends FoxTable[Shadow](tag, "shadows") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def formId     = column[Int]("form_id")
  def attributes = column[JValue]("attributes")
  def relations  = column[Option[JValue]]("relations")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, formId, attributes, relations, createdAt) <> ((Shadow.apply _).tupled, Shadow.unapply)
}

object Shadows
    extends FoxTableQuery[Shadow, Shadows](new Shadows(_))
    with ReturningId[Shadow, Shadows] {
  val returningLens: Lens[Shadow, Int] = lens[Shadow].id
}
