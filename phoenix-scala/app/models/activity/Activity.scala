package models.activity

import java.time.Instant

import com.typesafe.scalalogging.LazyLogging
import faker.Lorem.letterify
import org.json4s.Extraction
import org.json4s.jackson.Serialization.writePretty
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.lifted.Tag
import utils.JsonFormatters
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ActivityContext(userId: Int, userType: String, transactionId: String)

object ActivityContext {

  // Convert context to json and back again
  implicit val ActivityContextColumn: JdbcType[ActivityContext] with BaseTypedType[ActivityContext] = {
    implicit val formats = JsonFormatters.phoenixFormats
    MappedColumnType.base[ActivityContext, Json](
        c ⇒ Extraction.decompose(c),
        j ⇒ j.extract[ActivityContext]
    )
  }

  def build(userId: Int, userType: String): ActivityContext =
    ActivityContext(userId = userId, userType = userType, transactionId = letterify("?" * 5))
}

/**
  * An activity keeps information about some interesting change in state. The data an
  * activity contains must be complete enough to render in the UI. The activity also
  * keeps track of what/who created it.
  *
  * An activity can be part of many activity trails in multiple dimensions.
  */
case class Activity(id: Int = 0,
                    activityType: ActivityType,
                    data: Json,
                    context: ActivityContext,
                    createdAt: Instant = Instant.now)
    extends FoxModel[Activity]

class Activities(tag: Tag) extends FoxTable[Activity](tag, "activities") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def activityType = column[ActivityType]("activity_type")
  def data         = column[Json]("data")
  def context      = column[ActivityContext]("context")
  def createdAt    = column[Instant]("created_at")

  def * =
    (id, activityType, data, context, createdAt) <> ((Activity.apply _).tupled, Activity.unapply)
}

// Any specific activity can have an implicit converion function to the opaque activity
// Opaque here means the scala type system cannot see the activity
case class OpaqueActivity(activityType: ActivityType, data: Json)

object Activities
    extends FoxTableQuery[Activity, Activities](new Activities(_))
    with LazyLogging
    with ReturningId[Activity, Activities] {

  val returningLens: Lens[Activity, Int] = lens[Activity].id

  implicit val formats = JsonFormatters.phoenixFormats

  def log(a: OpaqueActivity)(implicit activityContext: AC, ec: EC): DbResultT[Activity] = {
    val activity =
      Activity(activityType = a.activityType, data = a.data, context = activityContext)

    logger.info(
        s"Activity ${a.activityType} by ${activityContext.userType} ${activityContext.userId}")
    logger.debug(writePretty(activity))

    create(activity)
  }

  def filterByType(activityType: ActivityType): QuerySeq = filter(_.activityType === activityType)

  def filterByData(key: String, value: String): QuerySeq = filter(_.data +>> key === value)

  def filterByData(activityType: ActivityType, key: String, value: String): QuerySeq =
    filter(_.activityType === activityType).filter(_.data +>> key === value)
}
