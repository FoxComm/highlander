package models.activity

import java.time.Instant

import models.Aliases
import models.Aliases.Json
import shapeless._
import slick.lifted.Tag
import utils.db._
import utils.db.ExPostgresDriver.api._

/**
 * An activity connection is a node in an activity trail. It connects an activity to a trail
 * in some activity dimension. It is implemented as a doubly linked list.
 *
 * It can also store data about the connection of an activity to a trail in the data field.
 *
 * The tail connection keeps the tail of the list. The tail.next_id points to the first activity and the
 * previous_id points to the last event in the activity trail.
 */
case class Connection(id: Int = 0, dimensionId: Int, trailId: Int, activityId: Int, previousId: Option[Int], nextId:
Option[Int], data: Option[Json], connectedBy: ActivityContext, createdAt: Instant = Instant.now)
  extends FoxModel[Connection]

class Connections(tag: Tag) extends FoxTable[Connection](tag, "activity_connections")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def dimensionId = column[Int]("dimension_id")
  def trailId = column[Int]("trail_id")
  def activityId = column[Int]("activity_id")
  def previousId = column[Option[Int]]("previous_id")
  def nextId = column[Option[Int]]("next_id")
  def data = column[Option[Json]]("data")
  def connectedBy = column[ActivityContext]("connected_by")
  def createdAt = column[Instant]("created_at")

  def * = (id, dimensionId, trailId, activityId, previousId, nextId, data, connectedBy, createdAt) <>
    ((Connection.apply _).tupled, Connection.unapply)

  def activity = foreignKey(Activities.tableName, activityId, Activities)(_.id)
  def dimension = foreignKey(Dimensions.tableName, dimensionId, Dimensions)(_.id)
  def trail = foreignKey(Trails.tableName, trailId, Trails)(_.id)
}

object Connections extends FoxTableQuery[Connection, Connections](
  idLens = lens[Connection].id)(new Connections(_)) {

    def filterByTrail(trailId: Int) = filter(_.trailId === trailId)

  }
