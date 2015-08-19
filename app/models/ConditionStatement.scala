package models

import com.pellucid.sealerate
import utils.ADT

final case class ConditionStatement(comparison: ConditionStatement.Comparison,
  conditions: Seq[Condition] = Seq.empty, statements: Seq[ConditionStatement] = Seq.empty)

object ConditionStatement {
  sealed trait Comparison

  case object And extends Comparison
  case object Or extends Comparison

  object Comparison extends ADT[Comparison] {
    def types = sealerate.values[Comparison]
  }
}