package models.objects

import java.time.Instant

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A ObjectContext stores information to determine which object shadow to show.
 * Each object shadow is associated with a context where it makes sense to display it.
 *
 * The context will be matched against a user context so that the storefront displays
 * the appropriate object information.
 */
final case class ObjectContext(id: Int = 0, name: String, attributes: Json, 
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ObjectContext]
  with Validation[ObjectContext]

class ObjectContexts(tag: Tag) extends GenericTable.TableWithId[ObjectContext](tag, "object_contexts")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def attributes = column[Json]("attributes")
  def createdAt = column[Instant]("created_at")

  def * = (id, name, attributes, createdAt) <> ((ObjectContext.apply _).tupled, ObjectContext.unapply)

}

object ObjectContexts extends TableQueryWithId[ObjectContext, ObjectContexts](
  idLens = GenLens[ObjectContext](_.id))(new ObjectContexts(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByName(name: String): QuerySeq = 
    filter(_.name === name)

  def filterByContextAttribute(key: String, value: String): QuerySeq = 
    filter(_.attributes+>>(key) === value)

  def filterByLanguage(lang: String): QuerySeq = 
    filterByContextAttribute("lang", lang)
}
