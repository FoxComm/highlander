package models.activity

import java.time.Instant

import monocle.macros.GenLens
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import org.json4s.JsonAST.JValue
import utils.ExPostgresDriver.api._
import utils.time.JavaTimeSlickMapper._

import Aliases.Json

/**
 * An activity connection is a node in an activity trail. It connects an activity to a trail
 * in some activity dimension. It is implemented as a doubly linked list.
 *
 * It can also store data about the connection of an activity to a trail in the data field.
 *
 * The tail connection keeps the tail of the list. The tail.next_id points to the first activity and the 
 * previous_id points to the last event in the activity trail.
 */
final case class Connection(
  id: Int = 0, 
  dimensionId: Int, 
  trailId: Int, 
  activityId: Int, 
  previousId: Option[Int], 
  nextId: Option[Int], 
  data: Option[Json], 
  connectedBy: ActivityContext,
  createdAt: Instant = Instant.now) extends ModelWithIdParameter[Connection]
                                    with    Validation[Connection]

class Connections(tag: Tag) extends GenericTable.TableWithId[Connection](tag, "activity_connections")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def dimensionId = column[Int]("dimension_id")
  def trailId = column[Int]("trail_id")
  def activityId = column[Int]("activity_id")
  def previousId = column[Option[Int]]("previous_id")
  def nextId = column[Option[Int]]("next_id")
  def data = column[Option[Json]]("data")
  def connectedBy = column[ActivityContext]("connected_by")
  def createdAt = column[Instant]("created_at")

  def * = (
    id, 
    dimensionId, 
    trailId, 
    activityId, 
    previousId, 
    nextId, 
    data, 
    connectedBy, 
    createdAt) <> ((Connection.apply _).tupled, Connection.unapply)

  def activity = foreignKey(Activities.tableName, activityId, Activities)(_.id)
  def dimension = foreignKey(Dimensions.tableName, dimensionId, Dimensions)(_.id)
  def trail = foreignKey(Trails.tableName, trailId, Trails)(_.id)
}

object Connections extends TableQueryWithId[Connection, Connections](
  idLens = GenLens[Connection](_.id))(new Connections(_)) {

    def filterByTrail(trailId: Int) = filter(_.trailId === trailId)

  }
