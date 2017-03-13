package models.objects

import java.time.Instant

import shapeless._
import slick.lifted.Tag
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

import com.github.tminglei.slickpg._

case class GenericObject(id: Int = 0,
                         scope: LTree,
                         contextId: Int,
                         kind: String,
                         shadowId: Int,
                         formId: Int,
                         commitId: Int,
                         updatedAt: Instant = Instant.now,
                         createdAt: Instant = Instant.now,
                         archivedAt: Option[Instant] = None)
    extends FoxModel[GenericObject]
    with Validation[GenericObject]
    with ObjectHead[GenericObject] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): GenericObject =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class GenericObjects(tag: Tag) extends ObjectHeads[GenericObject](tag, "generic_objects") {

  def kind = column[String]("kind")

  def * =
    (id, scope, contextId, kind, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <>
      ((GenericObject.apply _).tupled, GenericObject.unapply)
}

object GenericObjects
    extends FoxTableQuery[GenericObject, GenericObjects](new GenericObjects(_))
    with ReturningId[GenericObject, GenericObjects] {

  val returningLens: Lens[GenericObject, Int] = lens[GenericObject].id

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def withContextAndGenericObject(contextId: Int, genericObjectId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === genericObjectId)

  def withContextAndForm(contextId: Int, formId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === formId)

  def withContextAndKind(contextId: Int, kind: String): QuerySeq =
    filter(_.contextId === contextId).filter(_.kind === kind)
}
