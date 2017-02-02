package models.objects

import java.time.Instant

import slick.lifted.Tag
import utils.db.ExPostgresDriver.api._
import utils.db._
import com.github.tminglei.slickpg._
import _root_.utils.aliases.OC

trait Head[M <: Head[M]] extends FoxModel[M] { self: M ⇒
  def scope: LTree
  def contextId: Int
  def slug: Option[String]
  def formId: Int
  def shadowId: Int
  def commitId: Int
  def createdAt: Instant
  def updatedAt: Instant
  def archivedAt: Option[Instant]
}

abstract class Heads[M <: Head[M]](tag: Tag, table: String) extends FoxTable[M](tag, table) {

  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope      = column[LTree]("scope")
  def contextId  = column[Int]("context_id")
  def slug       = column[Option[String]]("slug")
  def formId   = column[Int]("form_id")
  def shadowId   = column[Int]("shadow_id")
  def commitId   = column[Int]("commit_id")
  def createdAt  = column[Instant]("created_at")
  def updatedAt  = column[Instant]("updated_at")
  def archivedAt = column[Option[Instant]]("archived_at")

  def context = foreignKey(ObjectContexts.tableName, contextId, ObjectContexts)(_.id)
  def commit  = foreignKey(ObjectCommits.tableName, commitId, ObjectCommits)(_.id)
}

abstract class HeadQueries[M <: Head[M], T <: Heads[M]](construct: Tag ⇒ T)
    extends FoxTableQuery[M, T](construct) {

  def findByReference(reference: ObjectReference)(implicit oc: OC): QuerySeq =
    reference match {
      case ObjectId(id) ⇒ filter(_.formId === id)
      case ObjectSlug(slug) ⇒ filter(_.slug === slug)
    }

}
