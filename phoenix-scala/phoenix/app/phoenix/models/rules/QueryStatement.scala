package phoenix.models.rules

import com.pellucid.sealerate
import core.ADT
import core.db.ExPostgresDriver.api._
import org.json4s.Extraction
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import scala.collection.immutable.Seq

case class QueryStatement(comparison: QueryStatement.Comparison,
                          conditions: Seq[Condition] = Seq.empty,
                          statements: Seq[QueryStatement] = Seq.empty)

object QueryStatement {
  sealed trait Comparison {
    def apply(l: Boolean, r: Boolean): Boolean = false
  }

  case object And extends Comparison {
    override def apply(l: Boolean, r: Boolean): Boolean = l && r
  }

  case object Or extends Comparison {
    override def apply(l: Boolean, r: Boolean): Boolean = l || r
  }

  object Comparison extends ADT[Comparison] {
    def types = sealerate.values[Comparison]
  }

  implicit val QueryStatementColumn: JdbcType[QueryStatement] with BaseTypedType[QueryStatement] = {
    implicit val formats = JsonFormatters.phoenixFormats
    MappedColumnType.base[QueryStatement, Json](
        q ⇒ Extraction.decompose(q),
        j ⇒ j.extract[QueryStatement]
    )
  }

  def evaluate[A](stmt: Option[QueryStatement], data: A, f: (Condition, A) ⇒ Boolean): Boolean = {
    stmt.fold(false) { statement ⇒
      val initial = statement.comparison == QueryStatement.And

      val conditionsResult = statement.conditions.foldLeft(initial) { (result, nextCond) ⇒
        statement.comparison.apply(result, f(nextCond, data))
      }

      statement.statements.foldLeft(conditionsResult) { (result, nextStmt) ⇒
        statement.comparison.apply(result, evaluate(Some(nextStmt), data, f))
      }
    }
  }
}
