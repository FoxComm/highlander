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

object ActivityHandler {
  def build(scope: LTree, contextId: Int, insertResult: InsertResult): ActivityHandler =
    ActivityHandler(scope = scope,
             contextId = contextId,
             formId = insertResult.form.id,
             shadowId = insertResult.shadow.id,
             commitId = insertResult.commit.id)

  val kind = "activityHandler"
}

case class ActivityHandler(id: Int = 0,
                    scope: LTree,
                    contextId: Int,
                    shadowId: Int,
                    formId: Int,
                    commitId: Int,
                    updatedAt: Instant = Instant.now,
                    createdAt: Instant = Instant.now,
                    archivedAt: Option[Instant] = None)
    extends FoxModel[ActivityHandler]
    with Validation[ActivityHandler]
    with ObjectHead[ActivityHandler] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): ActivityHandler =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class Categories(tag: Tag) extends ObjectHeads[ActivityHandler](tag, "activity_handlers") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <>
      ((ActivityHandler.apply _).tupled, ActivityHandler.unapply)
}

object Categories
    extends FoxTableQuery[ActivityHandler, Categories](new Categories(_))
    with ReturningId[ActivityHandler, Categories] {

  val returningLens: Lens[ActivityHandler, Int] = lens[ActivityHandler].id

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def withContextAndActivityHandler(contextId: Int, activityHandlerId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === activityHandlerId)

  def withContextAndForm(contextId: Int, formId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === formId)
}
