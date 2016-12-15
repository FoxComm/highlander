package models.inventory

import com.pellucid.sealerate
import utils.ADT

sealed trait VariantType

case object Sellable    extends VariantType
case object NonSellable extends VariantType
case object Preorder    extends VariantType
case object Backorder   extends VariantType

object VariantType extends ADT[VariantType] {
  def types = sealerate.values[VariantType]
}
