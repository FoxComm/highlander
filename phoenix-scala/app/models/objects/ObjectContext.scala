package models.objects

import java.time.Instant

import shapeless._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.{JsonFormatters, Validation}

/**
  * A ObjectContext stores information to determine which object shadow to show.
  * Each object shadow is associated with a context where it makes sense to display it.
  *
  * The context will be matched against a user context so that the storefront displays
  * the appropriate object information.
  */
case class ObjectContext(id: Int = 0,
                         parentId: Option[Int] = None,
                         name: String,
                         attributes: Json,
                         createdAt: Instant = Instant.now)
    extends FoxModel[ObjectContext]
    with Validation[ObjectContext]

class ObjectContexts(tag: Tag) extends FoxTable[ObjectContext](tag, "object_contexts") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId   = column[Option[Int]]("parent_id")
  def name       = column[String]("name")
  def attributes = column[Json]("attributes")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, parentId, name, attributes, createdAt) <> ((ObjectContext.apply _).tupled, ObjectContext.unapply)
}

object ObjectContexts
    extends FoxTableQuery[ObjectContext, ObjectContexts](new ObjectContexts(_))
    with ReturningId[ObjectContext, ObjectContexts] {

  val returningLens: Lens[ObjectContext, Int] = lens[ObjectContext].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByName(name: String): QuerySeq =
    filter(_.name === name)

  def filterByContextAttribute(key: String, value: String): QuerySeq =
    filter(_.attributes +>> key === value)

  def filterByLanguage(lang: String): QuerySeq =
    filterByContextAttribute("lang", lang)
}
