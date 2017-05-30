package objectframework.content

import java.time.Instant

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import shapeless._
import core.db.ExPostgresDriver.api._
import core.db._

case class Commit(id: Int = 0,
                  formId: Form#Id,
                  shadowId: Shadow#Id,
                  createdAt: Instant = Instant.now)
    extends FoxModel[Commit]

class Commits(tag: Tag) extends FoxTable[Commit](tag, "object_commits") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def formId    = column[Int]("form_id")
  def shadowId  = column[Int]("shadow_id")
  def createdAt = column[Instant]("created_at")

  def * =
    (id, formId, shadowId, createdAt) <> ((Commit.apply _).tupled, Commit.unapply)
}

object Commits
    extends FoxTableQuery[Commit, Commits](new Commits(_))
    with ReturningId[Commit, Commits] {
  val returningLens: Lens[Commit, Int] = lens[Commit].id
}
