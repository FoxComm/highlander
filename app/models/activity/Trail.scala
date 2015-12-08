package models.activity

import java.time.Instant

import monocle.macros.GenLens
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import org.json4s.JsonAST.JValue
import org.json4s.JsonAST.JNothing
import utils.ExPostgresDriver.api._
import utils.time.JavaTimeSlickMapper._

import scala.concurrent.ExecutionContext

import Aliases.Json

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
final case class Trail(
  id: Int = 0, 
  dimensionId: Int,
  objectId: Int,
  tailConnectionId: Option[Int] = None,
  data: Option[Json] = None,   
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Trail]
  with Validation[Trail]

class Trails(tag: Tag) extends GenericTable.TableWithId[Trail](tag, "activity_trails")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def dimensionId = column[Int]("dimension_id")
  def objectId = column[Int]("object_id")
  def tailConnectionId = column[Option[Int]]("tail_connection_id")
  def data = column[Option[Json]]("data")
  def createdAt = column[Instant]("created_at")

  def * = (id, dimensionId, objectId, tailConnectionId, data, createdAt) <> ((Trail.apply _).tupled, Trail.unapply)
}

object Trails extends TableQueryWithId[Trail, Trails](
  idLens = GenLens[Trail](_.id))(new Trails(_)) {

    def findByObjectId(dimensionId: Int, objectId: Int) : QuerySeq =
      filter(_.dimensionId === dimensionId).filter(_.objectId === objectId)
  }
