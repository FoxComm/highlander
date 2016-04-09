package concepts.discounts

// Adjustment types
sealed trait LineItemType
case object SkuLineItemType extends LineItemType
case object OrderLineItemType extends LineItemType
case object ShippingLineItemType extends LineItemType

// TODO: Include promotion / discount object?
final case class LineItemAdjustment(lineItemType: LineItemType, subtract: Int, lineItemId: Option[Int] = None)
