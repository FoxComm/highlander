package models.category

import java.time.Instant

import models.objects.{ObjectHead, ObjectHeads}
import models.objects.ObjectUtils.InsertResult
import shapeless._
import slick.lifted.Tag
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

import com.github.tminglei.slickpg._

object Category {
  def build(scope: LTree, contextId: Int, insertResult: InsertResult): Category =
    Category(scope = scope,
             contextId = contextId,
             formId = insertResult.form.id,
             shadowId = insertResult.shadow.id,
             commitId = insertResult.commit.id)

  val kind = "category"
}

case class Category(id: Int = 0,
                    scope: LTree,
                    contextId: Int,
                    shadowId: Int,
                    formId: Int,
                    commitId: Int,
                    updatedAt: Instant = Instant.now,
                    createdAt: Instant = Instant.now,
                    archivedAt: Option[Instant] = None)
    extends FoxModel[Category]
    with Validation[Category]
    with ObjectHead[Category] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Category =
    this.copy(shadowId = shadowId, commitId = commitId)

  def copyForCreate(contextId: Int, formId: Int, shadowId: Int, commitId: Int): Category =
    this.copy(contextId = contextId, formId = formId, shadowId = shadowId, commitId = commitId)
}

class Categories(tag: Tag) extends ObjectHeads[Category](tag, "categories") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <>
      ((Category.apply _).tupled, Category.unapply)
}

object Categories
    extends FoxTableQuery[Category, Categories](new Categories(_))
    with ReturningId[Category, Categories] {

  val returningLens: Lens[Category, Int] = lens[Category].id

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def withContextAndCategory(contextId: Int, categoryId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === categoryId)

  def withContextAndForm(contextId: Int, formId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === formId)
}
