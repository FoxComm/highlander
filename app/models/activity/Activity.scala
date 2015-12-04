
package models.activity

import java.time.Instant

import monocle.macros.GenLens
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import org.json4s.JsonAST.JValue
import utils.ExPostgresDriver.api._
import utils.time.JavaTimeSlickMapper._

object Aliases {
  type ActivityType = String
  type Json = JValue
}

import Aliases.ActivityType
import Aliases.Json

/**
 * An activity keeps information about some interesting change in state. The data an
 * activity contains must be complete enough to render in the UI. The activity also
 * keeps track of what/who created it.
 *
 * An activity can be part of many activity trails in multiple dimensions.
 */
final case class Activity(
  id: Int = 0, 
  activityType: ActivityType, 
  data: Json, 
  triggeredBy: Json,
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Activity]
  with Validation[Activity]

class Activities(tag: Tag) extends GenericTable.TableWithId[Activity](tag, "activities")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def activityType = column[ActivityType]("activity_type")
  def data = column[Json]("data")
  def triggeredBy = column[Json]("triggered_by")
  def createdAt = column[Instant]("created_at")

  def * = (id, activityType, data, triggeredBy, createdAt) <> ((Activity.apply _).tupled, Activity.unapply)
}

object Activities extends TableQueryWithId[Activity, Activities](
  idLens = GenLens[Activity](_.id))(new Activities(_)) {

    private [this] def filterByType(activityType: ActivityType) = filter(_.activityType === activityType)

  }
