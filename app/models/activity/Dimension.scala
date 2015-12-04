package models.activity

import java.time.Instant

import monocle.macros.GenLens
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import org.json4s.JsonAST.JValue
import utils.ExPostgresDriver.api._
import utils.time.JavaTimeSlickMapper._

import Aliases.Json

/**
 * An activity dimension has a set of activity trails. It is used as a logical grouping
 * of trails by some 'kind' of activity. A particular activity can be in multiple dimensions 
 * at a time.
 */
final case class Dimension(
  id: Int = 0, 
  name: String, 
  description: String)
  extends ModelWithIdParameter[Dimension]
  with Validation[Dimension]

class Dimensions(tag: Tag) extends GenericTable.TableWithId[Dimension](tag, "activity_dimensions")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")

  def * = (id, name, description) <> ((Dimension.apply _).tupled, Dimension.unapply)
}

object Dimensions extends TableQueryWithId[Dimension, Dimensions](
  idLens = GenLens[Dimension](_.id))(new Dimensions(_)) {

    private [this] def findByName(name: String) = filter(_.name === name)

  }
