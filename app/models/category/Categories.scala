package models.category

import java.time.Instant

import models.objects.ObjectHeads
import models.objects.ObjectUtils.InsertResult
import shapeless._
import slick.lifted.Tag
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

object Category {
  def build(contextId: Int, insertResult: InsertResult): Category =
    Category(contextId = contextId,
             formId = insertResult.form.id,
             shadowId = insertResult.shadow.id,
             commitId = insertResult.commit.id)

  val kind = "category"
}

case class Category(id: Int = 0,
                    contextId: Int,
                    shadowId: Int,
                    formId: Int,
                    commitId: Int,
                    updatedAt: Instant = Instant.now,
                    createdAt: Instant = Instant.now)
    extends FoxModel[Category]
    with Validation[Category]

class Categories(tag: Tag) extends ObjectHeads[Category](tag, "categories") {
  def * =
    (id, contextId, shadowId, formId, commitId, updatedAt, createdAt) <>
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
