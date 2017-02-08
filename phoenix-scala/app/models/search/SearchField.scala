package models.search

import java.time.Instant

import com.pellucid.sealerate
import shapeless.{Lens, lens}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ADT
import utils.db._
import utils.db.ExPostgresDriver.api._

import payloads.SearchPayloads.FieldDefinition

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

  sealed trait FieldType
  case object ProductField extends FieldType
  case object OptionField  extends FieldType

  object FieldType extends ADT[FieldType] {
    def types = sealerate.values[FieldType]
  }

  implicit val fieldColumnType: JdbcType[FieldType] with BaseTypedType[FieldType] =
    FieldType.slickColumn

  def fromPayload(payload: FieldDefinition, indexId: Int): SearchField = {
    SearchField(name = payload.name,
                analyzer = payload.analyzer,
                indexId = indexId,
                `type` = payload.`type`)
  }
}

case class SearchField(id: Int = 0,
                       name: String,
                       `type`: SearchField.FieldType,
                       analyzer: SearchField.Analyzer,
                       indexId: SearchIndex#Id,
                       createdAt: Instant = Instant.now,
                       updatedAt: Instant = Instant.now,
                       deletedAt: Option[Instant] = None)
    extends FoxModel[SearchField]

class SearchFields(tag: Tag) extends FoxTable[SearchField](tag, "search_fields") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name     = column[String]("name")
  def `type`   = column[SearchField.FieldType]("type")
  def analyzer = column[SearchField.Analyzer]("analyzer")
  def indexId  = column[SearchIndex#Id]("index_id")

  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def index = foreignKey(SearchIndexes.tableName, indexId, SearchIndexes)(_.id)

  def * =
    (id, name, `type`, analyzer, indexId, createdAt, updatedAt, deletedAt) <> ((SearchField.apply _).tupled, SearchField.unapply)
}

object SearchFields
    extends FoxTableQuery[SearchField, SearchFields](new SearchFields(_))
    with ReturningId[SearchField, SearchFields] {

  val returningLens: Lens[SearchField, Int] = lens[SearchField].id

  def findOneByName(name: String): QuerySeq =
    filter(_.name === name)

}
