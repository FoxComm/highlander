package objectframework.content

import java.time.Instant

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import shapeless._
import core.db.ExPostgresDriver.api._
import core.db._

case class Head(id: Int = 0,
                kind: String,
                viewId: View#Id,
                commitId: Commit#Id,
                createdAt: Instant = Instant.now,
                updatedAt: Instant = Instant.now,
                archivedAt: Option[Instant] = None)
    extends FoxModel[Head]

class Heads(tag: Tag) extends FoxTable[Head](tag, "heads") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def kind       = column[String]("kind")
  def viewId     = column[Int]("view_id")
  def commitId   = column[Int]("commit_id")
  def createdAt  = column[Instant]("created_at")
  def updatedAt  = column[Instant]("updated_at")
  def archivedAt = column[Option[Instant]]("archived_at")

  def * =
    (id, kind, viewId, commitId, createdAt, updatedAt, archivedAt) <> ((Head.apply _).tupled, Head.unapply)
}

object Heads extends FoxTableQuery[Head, Heads](new Heads(_)) with ReturningId[Head, Heads] {
  val returningLens: Lens[Head, Int] = lens[Head].id

  def updateCommit(head: Head, commitId: Commit#Id)(implicit ec: EC): DbResultT[Head] =
    update(head, head.copy(commitId = commitId, updatedAt = Instant.now))
}
