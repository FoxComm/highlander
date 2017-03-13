package models.chronicle

import java.time.Instant

import models.objects.{ObjectHead, ObjectHeads}
import models.objects.ObjectUtils.InsertResult
import shapeless._
import slick.lifted.Tag
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

import com.github.tminglei.slickpg._

case class ActivityKindHandler(id: Int = 0,
                               scope: LTree,
                               kind: String,
                               activityHandlerHead: Int,
                               createdAt: Instant = Instant.now)
    extends FoxModel[ActivityKindHandler]
    with Validation[ActivityKindHandler]

class ActivityKindHandlers(tag: Tag) extends FoxTable[ActivityKindHandler](tag, "users") {
  def id                  = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope               = column[LTree]("scope")
  def kind                = column[String]("kind")
  def activityHandlerHead = column[Int]("activity_handler_head")
  def createdAt           = column[Instant]("created_at")

  def * =
    (id, scope, kind, activityHandlerHead, createdAt) <> ((ActivityKindHandler.apply _).tupled, ActivityKindHandler.unapply)

  def activityHead =
    foreignKey(ActivityHandlers.tableName, activityHandlerHead, ActivityHandlers)(_.id)
}

object ActivityKindHandlers
    extends FoxTableQuery[ActivityKindHandler, ActivityKindHandlers](new ActivityKindHandlers(_))
    with ReturningId[ActivityKindHandler, ActivityKindHandlers] {

  val returningLens: Lens[ActivityKindHandler, Int] = lens[ActivityKindHandler].id

  def filterByKind(kind: String): QuerySeq =
    filter(_.kind === kind)
}
