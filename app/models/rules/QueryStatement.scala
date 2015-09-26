package models.rules

import com.pellucid.sealerate
import scala.collection.immutable.Seq
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.{JsonFormatters, ADT}
import utils.ExPostgresDriver.api._

final case class QueryStatement(comparison: QueryStatement.Comparison,
  conditions: Seq[Condition] = Seq.empty, statements: Seq[QueryStatement] = Seq.empty)

object QueryStatement {
  sealed trait Comparison

  case object And extends Comparison
  case object Or extends Comparison

  object Comparison extends ADT[Comparison] {
    def types = sealerate.values[Comparison]
  }

  implicit val QueryStatementColumn: JdbcType[QueryStatement] with BaseTypedType[QueryStatement] = {
    implicit val formats = JsonFormatters.phoenixFormats
    MappedColumnType.base[QueryStatement, JValue](
      q ⇒ Extraction.decompose(q),
      j ⇒ j.extract[QueryStatement]
    )
  }
}