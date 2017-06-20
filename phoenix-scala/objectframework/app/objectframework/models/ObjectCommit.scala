package objectframework.models

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import shapeless._

/**
  * A ObjectCommit is a tree of commits, each pointing to a form shadow.
  */
case class ObjectCommit(id: Int = 0,
                        formId: Int,
                        shadowId: Int,
                        reasonId: Option[Int] = None,
                        previousId: Option[Int] = None,
                        createdAt: Instant = Instant.now)
    extends FoxModel[ObjectCommit]

class ObjectCommits(tag: Tag) extends FoxTable[ObjectCommit](tag, "object_commits") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def formId     = column[Int]("form_id")
  def shadowId   = column[Int]("shadow_id")
  def reasonId   = column[Option[Int]]("reason_id")
  def previousId = column[Option[Int]]("previous_id")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, formId, shadowId, reasonId, previousId, createdAt) <> ((ObjectCommit.apply _).tupled, ObjectCommit.unapply)

  def shadow = foreignKey(ObjectShadows.tableName, shadowId, ObjectShadows)(_.id)
  def form   = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
}

object ObjectCommits
    extends FoxTableQuery[ObjectCommit, ObjectCommits](new ObjectCommits(_))
    with ReturningId[ObjectCommit, ObjectCommits] {

  val returningLens: Lens[ObjectCommit, Int] = lens[ObjectCommit].id

  def filterByObject(formId: Int): QuerySeq =
    filter(_.formId === formId)
}
