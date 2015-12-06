package models.activity

import java.time.Instant

import monocle.macros.GenLens
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import org.json4s.JsonAST.JValue
import utils.ExPostgresDriver.api._
import utils.time.JavaTimeSlickMapper._

/**
 * An activity trail belongs in some dimension and points to the tail activity connection.
 * The trail is implemented as a doubly-linked list of activity connnections.
 */
final case class Trail(
  id: Int = 0, 
  dimensionId: Int,
  trailConnectionId: Int = 0,
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Trail]
  with Validation[Trail]

class Trails(tag: Tag) extends GenericTable.TableWithId[Trail](tag, "activity_trails")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def dimensionId = column[Int]("dimension_id")
  def tailConnectionId = column[Int]("tail_connection_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, dimensionId, tailConnectionId, createdAt) <> ((Trail.apply _).tupled, Trail.unapply)
}

object Trails extends TableQueryWithId[Trail, Trails](
  idLens = GenLens[Trail](_.id))(new Trails(_)) {

    def findByDimension(dimensionId: Int) = filter(_.dimensionId === dimensionId)

  }
