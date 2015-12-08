
package models.activity

import java.time.Instant

import scala.concurrent.ExecutionContext

import monocle.macros.GenLens

import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.Slick.implicits._

//import com.github.tminglei.slickpg._

import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.{write ⇒ render}

object Aliases {
  type ActivityType = String
  type Json = JValue
}

import Aliases.ActivityType
import Aliases.Json

final case class ActivityContext(
  userId: Int,
  userType: String,
  transactionId: String)

object ActivityContext {

  //Convert context to json and back again
  implicit val ActivityContextColumn: JdbcType[ActivityContext] with BaseTypedType[ActivityContext] = {
    implicit val formats = JsonFormatters.phoenixFormats
    MappedColumnType.base[ActivityContext, JValue](
      c ⇒ Extraction.decompose(c),
      j ⇒ j.extract[ActivityContext]
    )
  }
}

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
  context: ActivityContext,
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Activity]
  with Validation[Activity]

class Activities(tag: Tag) extends GenericTable.TableWithId[Activity](tag, "activities")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def activityType = column[ActivityType]("activity_type")
  def data = column[Json]("data")
  def context = column[ActivityContext]("context")
  def createdAt = column[Instant]("created_at")

  def * = (
    id, 
    activityType, 
    data, 
    context, 
    createdAt) <> ((Activity.apply _).tupled, Activity.unapply)

}

//Any specific activity can have an implicit converion function to the opaque activity
//Opaque here means the scala type system cannot see the activity
final case class OpaqueActivity(activityType: ActivityType, data: Json)

object Activities extends TableQueryWithId[Activity, Activities](
  idLens = GenLens[Activity](_.id))(new Activities(_)) {

  implicit val formats: DefaultFormats.type = DefaultFormats

    def log(a: OpaqueActivity)(implicit context: ActivityContext, ec: ExecutionContext) = {
      create(Activity(
        activityType = a.activityType,
        data = a.data,
        context = context))
    }

    def filterByType(activityType: ActivityType) = filter(_.activityType === activityType)
    def filterByData(key: String, value: String ) = filter(_.data+>>(key) === value)
    def filterByData(activityType: ActivityType, key: String, value: String ) = 
      filter(_.activityType === activityType).filter(_.data+>>(key) === value)
  }
