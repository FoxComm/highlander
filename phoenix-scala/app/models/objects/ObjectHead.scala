package models.objects

import java.time.Instant

import failures.ObjectFailures.ObjectHeadCannotBeFoundByFormId
import slick.lifted.Tag
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import com.github.tminglei.slickpg._
import failures.GeneralFailure

trait ObjectHead[M <: ObjectHead[M]] extends FoxModel[M] { self: M ⇒
  def scope: LTree
  def contextId: Int
  def shadowId: Int
  def formId: Int
  def commitId: Int
  def updatedAt: Instant
  def createdAt: Instant
  def archivedAt: Option[Instant]

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): M
  def copyForCreate(contextId: Int, formId: Int, shadowId: Int, commitId: Int): M
}

/**
  * Abstract class to help define an object head object which points to the latest
  * version of some object in the context specified.
  */
abstract class ObjectHeads[M <: ObjectHead[M]](tag: Tag, table: String)
    extends FoxTable[M](tag, table) {

  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope      = column[LTree]("scope")
  def contextId  = column[Int]("context_id")
  def shadowId   = column[Int]("shadow_id")
  def formId     = column[Int]("form_id")
  def commitId   = column[Int]("commit_id")
  def updatedAt  = column[Instant]("updated_at")
  def createdAt  = column[Instant]("created_at")
  def archivedAt = column[Option[Instant]]("archived_at")

  def context = foreignKey(ObjectContexts.tableName, contextId, ObjectContexts)(_.id)
  def shadow  = foreignKey(ObjectShadows.tableName, shadowId, ObjectShadows)(_.id)
  def form    = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
  def commit  = foreignKey(ObjectCommits.tableName, commitId, ObjectCommits)(_.id)
}

abstract class ObjectHeadsQueries[M <: ObjectHead[M], T <: ObjectHeads[M]](construct: Tag ⇒ T)
    extends FoxTableQuery[M, T](construct) {

  def mustFindByFormId404(formId: ObjectForm#Id)(implicit oc: OC, ec: EC): DbResultT[M] =
    findOneByFormId(formId).mustFindOneOr(
        ObjectHeadCannotBeFoundByFormId(baseTableRow.tableName, formId, oc.name))

  def findOneByFormId(formId: Int)(implicit oc: OC): QuerySeq =
    filter(_.contextId === oc.id).filter(_.formId === formId)

  def findOneByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === formId)

  def findOneByContextAndShadowId(contextId: Int, shadowId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.shadowId === shadowId)

  def updateHead(fullObject: FullObject[M], commitId: Int)(
      implicit ec: EC): DbResultT[FullObject[M]] =
    update(fullObject.model,
           fullObject.model.withNewShadowAndCommit(fullObject.shadow.id, commitId)).map(updated ⇒
          fullObject.copy(model = updated))

  def findFull(formId: Int, commitId: Option[Int] = None)(implicit ec: EC,
                                                          db: DB,
                                                          oc: OC): DbResultT[FullObject[M]] =
    for {
      head ← * <~ mustFindByFormId404(formId)
      realCommitId = commitId.getOrElse(head.commitId)
      commit ← * <~ ObjectCommits.mustFindById404(realCommitId)
      _ ← * <~ (if (commit.formId == formId) DbResultT.unit
                else DbResultT.failure(GeneralFailure("Form IDs don't match")))
      form   ← * <~ ObjectForms.mustFindById404(formId)
      shadow ← * <~ ObjectShadows.mustFindById404(commit.shadowId)
    } yield FullObject(head, form, shadow)

  def createFull(model: M, kind: String, attributes: Map[String, Json], schema: Option[String])(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[FullObject[M]] = {
    val form   = ObjectForm.fromPayload(kind, attributes)
    val shadow = ObjectShadow.fromPayload(attributes)

    for {
      ins  ← * <~ ObjectUtils.insert(form, shadow, schema)
      head ← * <~ create(model.copyForCreate(oc.id, ins.form.id, ins.shadow.id, ins.commit.id))
    } yield FullObject(head, form, shadow)
  }

  def updateFull(formId: Int, kind: String, attributes: Map[String, Json])(implicit ec: EC,
                                                                           db: DB,
                                                                           oc: OC) = {
    val formAndShadow = FormAndShadow.fromPayload(kind, attributes)

    for {
      old ← * <~ findFull(formId)
      attrs = old.shadow.attributes.merge(formAndShadow.shadow.attributes)

      updated ← * <~ ObjectUtils
                 .update(old.form.id, old.shadow.id, formAndShadow.form.attributes, attrs, true)
      head ← * <~ ObjectUtils.commit(updated).flatMap {
              case Some(commit) ⇒
                update(old.model, old.model.withNewShadowAndCommit(commit.shadowId, commit.id))
              case None ⇒
                DbResultT.pure(old.model)
            }
    } yield FullObject(head, updated.form, updated.shadow)
  }
}
