package objectframework.content

import java.time.Instant

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class Form(id: Int = 0,
                kind: String,
                attributes: JValue,
                createdAt: Instant = Instant.now,
                updatedAt: Instant = Instant.now)
    extends FoxModel[Form]

class Forms(tag: Tag) extends FoxTable[Form](tag, "forms") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def kind       = column[String]("kind")
  def attributes = column[JValue]("attributes")
  def createdAt  = column[Instant]("created_at")
  def updatedAt  = column[Instant]("updated_at")

  def * =
    (id, kind, attributes, createdAt, updatedAt) <> ((Form.apply _).tupled, Form.unapply)
}

object Forms extends FoxTableQuery[Form, Forms](new Forms(_)) with ReturningId[Form, Forms] {
  val returningLens: Lens[Form, Int] = lens[Form].id
}
