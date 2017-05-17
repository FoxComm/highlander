package models.rules

import com.pellucid.sealerate
import utils.ADT

case class Condition(rootObject: String,
                     field: String,
                     operator: Condition.Operator,
                     valInt: Option[Int] = None,
                     valString: Option[String] = None,
                     valBoolean: Option[Boolean] = None)

object Condition {
  sealed trait Operator

  // General operations
  case object Equals    extends Operator
  case object NotEquals extends Operator

  // Numeric operations
  case object GreaterThan         extends Operator
  case object GreaterThanOrEquals extends Operator
  case object LessThan            extends Operator
  case object LessThanOrEquals    extends Operator

  // String operations
  case object Contains    extends Operator
  case object NotContains extends Operator
  case object StartsWith  extends Operator
  case object InArray     extends Operator
  case object NotInArray  extends Operator

  object Operator extends ADT[Operator] {
    def types = sealerate.values[Operator]
  }

  // TODO (Jeff): Return an actual error, rather than just a boolean.
  def matches(comp: Int, statement: Condition): Boolean = {
    statement.valInt.fold(false) { (v: Int) ⇒
      statement.operator match {
        case Equals              ⇒ comp == v
        case NotEquals           ⇒ comp != v
        case GreaterThan         ⇒ comp > v
        case GreaterThanOrEquals ⇒ comp >= v
        case LessThan            ⇒ comp < v
        case LessThanOrEquals    ⇒ comp <= v
        case _                   ⇒ false
      }
    }
  }

  // TODO (Jeff): Make this more robust and think about things like case-sensitivity.
  def matches(comp: String, statement: Condition): Boolean =
    matches(Some(comp), statement)

  def matches(comp: Option[String], statement: Condition): Boolean = {
    comp.fold(matchAgainstEmptyStringOption(statement)) { (comp: String) ⇒
      statement.valString.fold(false) { (v: String) ⇒
        statement.operator match {
          case Equals      ⇒ comp == v
          case NotEquals   ⇒ comp != v
          case Contains    ⇒ comp.contains(v)
          case NotContains ⇒ !comp.contains(v)
          case StartsWith  ⇒ comp.startsWith(v)
          case InArray     ⇒ v.split(", ").contains(comp)
          case NotInArray  ⇒ !v.split(", ").contains(comp)
          case _           ⇒ false
        }
      }
    }
  }

  def matches(comp: Boolean, condition: Condition): Boolean = {
    condition.valBoolean.fold(false) { (v: Boolean) ⇒
      condition.operator match {
        case Equals    ⇒ comp == v
        case NotEquals ⇒ comp != v
        case _         ⇒ false
      }
    }
  }

  private def matchAgainstEmptyStringOption(statement: Condition): Boolean = {
    statement.operator match {
      case Equals      ⇒ statement.valString.isEmpty
      case NotEquals   ⇒ statement.valString.nonEmpty
      case NotContains ⇒ true
      case _           ⇒ false
    }
  }
}
