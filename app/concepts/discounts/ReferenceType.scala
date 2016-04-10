package concepts.discounts

import com.pellucid.sealerate
import utils.ADT

// Linking mechanism for qualifiers / offers
sealed trait ReferenceType
case object Product extends ReferenceType
case object Sku extends ReferenceType
case object SharedSearch extends ReferenceType

object ReferenceType extends ADT[ReferenceType] {
  def types = sealerate.values[ReferenceType]
}