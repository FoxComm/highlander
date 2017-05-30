package phoenix.models.inventory

import com.pellucid.sealerate
import core.ADT

sealed trait SkuType

case object Sellable    extends SkuType
case object NonSellable extends SkuType
case object Preorder    extends SkuType
case object Backorder   extends SkuType

object SkuType extends ADT[SkuType] {
  def types = sealerate.values[SkuType]
}
