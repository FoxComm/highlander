package concepts.discounts

import com.pellucid.sealerate
import utils.ADT

// Promotion types
sealed trait PromoType
case object OrderPromo extends PromoType
case object ItemsPromo extends PromoType

object PromoType extends ADT[PromoType] {
  def types = sealerate.values[PromoType]
}
