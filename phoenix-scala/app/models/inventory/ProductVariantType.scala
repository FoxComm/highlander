package models.inventory

import com.pellucid.sealerate
import utils.ADT

sealed trait ProductVariantType

case object Sellable    extends ProductVariantType
case object NonSellable extends ProductVariantType
case object Preorder    extends ProductVariantType
case object Backorder   extends ProductVariantType

object ProductVariantType extends ADT[ProductVariantType] {
  def types = sealerate.values[ProductVariantType]
}
