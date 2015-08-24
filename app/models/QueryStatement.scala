package models

import com.pellucid.sealerate
import utils.ADT

final case class QueryStatement(comparison: QueryStatement.Comparison,
  conditions: Seq[Condition] = Seq.empty, statements: Seq[QueryStatement] = Seq.empty)

object QueryStatement {
  sealed trait Comparison

  case object And extends Comparison
  case object Or extends Comparison

  object Comparison extends ADT[Comparison] {
    def types = sealerate.values[Comparison]
  }
}