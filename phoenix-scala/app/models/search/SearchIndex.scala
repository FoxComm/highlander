package models.search

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import failures.NotFoundFailure404
import shapeless._
import utils.db._
import utils.db.ExPostgresDriver.api._
import utils.aliases._

case class SearchIndex(id: Int = 0,
                       name: String,
                       scope: LTree,
                       createdAt: Instant = Instant.now,
                       updatedAt: Instant = Instant.now,
                       deletedAt: Option[Instant] = None)
    extends FoxModel[SearchIndex] {}

class SearchIndexes(tag: Tag) extends FoxTable[SearchIndex](tag, "search_indexes") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name      = column[String]("name")
  def scope     = column[LTree]("scope")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, name, scope, createdAt, updatedAt, deletedAt) <> ((SearchIndex.apply _).tupled, SearchIndex.unapply)
}

object SearchIndexes
    extends FoxTableQuery[SearchIndex, SearchIndexes](new SearchIndexes(_))
    with ReturningId[SearchIndex, SearchIndexes] {

  val returningLens: Lens[SearchIndex, Int] = lens[SearchIndex].id

}
