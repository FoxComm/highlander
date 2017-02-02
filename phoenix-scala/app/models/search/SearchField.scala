package models.search

import com.pellucid.sealerate
import models.search.SearchField.Analyzer
import shapeless.{Lens, lens}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ADT
import utils.db._
import utils.db.ExPostgresDriver.api._

object SearchField {

  // TODO: fetch analyzers from ES
  sealed trait Analyzer
  case object NoAnalyzer   extends Analyzer
  case object AutoComplete extends Analyzer
  case object LowerCased   extends Analyzer
  case object UpperCased   extends Analyzer

  object Analyzer extends ADT[Analyzer] {
    def types = sealerate.values[Analyzer]
  }

  implicit val analyzerColumnType: JdbcType[Analyzer] with BaseTypedType[Analyzer] =
    Analyzer.slickColumn
}

case class SearchField(id: Int = 0, name: String, analyzer: Analyzer, indexId: SearchIndex#Id)
    extends FoxModel[SearchField]

class SearchFields(tag: Tag) extends FoxTable[SearchField](tag, "search_fields") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name     = column[String]("name")
  def analyzer = column[Analyzer]("analyzer")
  def indexId  = column[SearchIndex#Id]("index_id")

  def index = foreignKey(SearchIndexes.tableName, indexId, SearchIndexes)(_.id)

  def * = (id, name, analyzer, indexId) <> ((SearchField.apply _).tupled, SearchField.unapply)
}

object SearchFields
    extends FoxTableQuery[SearchField, SearchFields](new SearchFields(_))
    with ReturningId[SearchField, SearchFields] {

  val returningLens: Lens[SearchField, Int] = lens[SearchField].id

  def findOneByName(name: String): QuerySeq =
    filter(_.name === name)

}
