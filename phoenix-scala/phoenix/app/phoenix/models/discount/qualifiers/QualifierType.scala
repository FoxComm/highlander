package phoenix.models.discount.qualifiers

import com.pellucid.sealerate
import phoenix.utils.ADT

// Qualifier types
sealed trait QualifierType
case object And                  extends QualifierType
case object CustomerDynamicGroup extends QualifierType
case object OrderAny             extends QualifierType
case object OrderTotalAmount     extends QualifierType
case object OrderNumUnits        extends QualifierType
case object ItemsAny             extends QualifierType
case object ItemsTotalAmount     extends QualifierType
case object ItemsNumUnits        extends QualifierType

object QualifierType extends ADT[QualifierType] {
  def types = sealerate.values[QualifierType]
}
