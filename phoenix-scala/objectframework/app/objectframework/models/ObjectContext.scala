package objectframework.models

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import core.utils.Validation
import org.json4s.JValue
import shapeless._

/**
  * A ObjectContext stores information to determine which object shadow to show.
  * Each object shadow is associated with a context where it makes sense to display it.
  *
  * The context will be matched against a user context so that the storefront displays
  * the appropriate object information.
  */
case class ObjectContext(id: Int = 0, name: String, attributes: JValue, createdAt: Instant = Instant.now)
    extends FoxModel[ObjectContext]
    with Validation[ObjectContext]

class ObjectContexts(tag: Tag) extends FoxTable[ObjectContext](tag, "object_contexts") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name       = column[String]("name")
  def attributes = column[JValue]("attributes")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, name, attributes, createdAt) <> ((ObjectContext.apply _).tupled, ObjectContext.unapply)
}

object ObjectContexts
    extends FoxTableQuery[ObjectContext, ObjectContexts](new ObjectContexts(_))
    with ReturningId[ObjectContext, ObjectContexts] {

  val returningLens: Lens[ObjectContext, Int] = lens[ObjectContext].id

  def filterByName(name: String): QuerySeq =
    filter(_.name === name)

  def filterByContextAttribute(key: String, value: String): QuerySeq =
    filter(_.attributes +>> key === value)

  def filterByLanguage(lang: String): QuerySeq =
    filterByContextAttribute("lang", lang)
}
