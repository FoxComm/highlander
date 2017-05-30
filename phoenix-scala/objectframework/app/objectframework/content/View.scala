package objectframework.content

import java.time.Instant

import org.json4s.JValue
import shapeless._
import core.utils.Validation
import core.db.ExPostgresDriver.api._
import core.db._

case class View(id: Int = 0, name: String, attributes: JValue, createdAt: Instant = Instant.now)
    extends FoxModel[View]
    with Validation[View]

class Views(tag: Tag) extends FoxTable[View](tag, "object_context") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name       = column[String]("name")
  def attributes = column[JValue]("attributes")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, name, attributes, createdAt) <> ((View.apply _).tupled, View.unapply)
}

object Views extends FoxTableQuery[View, Views](new Views(_)) with ReturningId[View, Views] {

  val returningLens: Lens[View, Int] = lens[View].id

  def filterByName(name: String): QuerySeq =
    filter(_.name === name)

  def filterByAttribute(key: String, value: String): QuerySeq =
    filter(_.attributes +>> key === value)

  def filterByLanguage(lang: String): QuerySeq =
    filterByAttribute("lang", lang)
}
