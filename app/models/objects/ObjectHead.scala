package models.objects

import java.time.Instant

import utils.db.ExPostgresDriver.api._
import utils.db._

trait ObjectHead[M <: ObjectHead[M]] extends FoxModel[M] { self: M ⇒
  def contextId: Int
  def shadowId: Int
  def formId: Int
  def commitId: Int
  def updatedAt: Instant
  def createdAt: Instant

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): M
}

/**
 * Abstract class to help define an object head object which points to the latest
 * version of some object in the context specified.
 */
abstract class ObjectHeads[C <: FoxModel[C]](tag: Tag, table: String) extends FoxTable[C](tag, table)  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def contextId = column[Int]("context_id")
  def shadowId = column[Int]("shadow_id")
  def formId = column[Int]("form_id")
  def commitId = column[Int]("commit_id")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def context = foreignKey(ObjectContexts.tableName, contextId, ObjectContexts)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, shadowId, ObjectShadows)(_.id)
  def form = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
  def commit = foreignKey(ObjectCommits.tableName, commitId, ObjectCommits)(_.id)
}
