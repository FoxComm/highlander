package models.activity

import java.time.Instant

import models.Aliases.Json
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

/**
  * An activity trail belongs in some dimension and points to the tail activity connection.
  * The trail is implemented as a doubly-linked list of activity connnections.
  *
  * The objectId is an id for the object the trail is linked to. The type of object depends
  * on the dimension the trail lives in.
  *
  * The trail also has arbitrary json data it can store. This can be used to store state
  * specific for a trail, for example, last seen notification, etc.
  */
case class Trail(id: Int = 0,
                 dimensionId: Int,
                 objectId: String,
                 tailConnectionId: Option[Int] = None,
                 data: Option[Json] = None,
                 createdAt: Instant = Instant.now)
    extends FoxModel[Trail]

class Trails(tag: Tag) extends FoxTable[Trail](tag, "activity_trails") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def dimensionId      = column[Int]("dimension_id")
  def objectId         = column[String]("object_id")
  def tailConnectionId = column[Option[Int]]("tail_connection_id")
  def data             = column[Option[Json]]("data")
  def createdAt        = column[Instant]("created_at")

  def * =
    (id, dimensionId, objectId, tailConnectionId, data, createdAt) <> ((Trail.apply _).tupled, Trail.unapply)

  def dimension = foreignKey(Dimensions.tableName, dimensionId, Dimensions)(_.id)
}

object Trails extends FoxTableQuery[Trail, Trails](new Trails(_)) with ReturningId[Trail, Trails] {

  val returningLens: Lens[Trail, Int] = lens[Trail].id

  def findByObjectId(dimensionId: Int, objectId: String): QuerySeq =
    filter(_.dimensionId === dimensionId).filter(_.objectId === objectId)

  def findNotificationByAdminId(adminId: Int): QuerySeq =
    for {
      dimensionId ← Dimensions.findByName(Dimension.notification).map(_.id)
      trail       ← filter(_.dimensionId === dimensionId).filter(_.objectId === adminId.toString)
    } yield trail
}
