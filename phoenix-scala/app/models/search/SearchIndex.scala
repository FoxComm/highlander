package models.search

import com.github.tminglei.slickpg.LTree
import shapeless._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class SearchIndex(id: Int = 0, name: String, scope: LTree) extends FoxModel[SearchIndex] {}

class SearchIndexes(tag: Tag) extends FoxTable[SearchIndex](tag, "search_indexes") {
  def id    = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name  = column[String]("name")
  def scope = column[LTree]("scope")

  def * = (id, name, scope) <> ((SearchIndex.apply _).tupled, SearchIndex.unapply)
}

object SearchIndexes
    extends FoxTableQuery[SearchIndex, SearchIndexes](new SearchIndexes(_))
    with ReturningId[SearchIndex, SearchIndexes] {

  val returningLens: Lens[SearchIndex, Int] = lens[SearchIndex].id

  def findOneByName(name: String): QuerySeq =
    filter(_.name === name)

}
