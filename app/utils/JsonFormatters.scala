package utils

import models.rules.{Condition, QueryStatement}
import models.{SharedSearch, CreditCardCharge, GiftCard, GiftCardAdjustment, Order, OrderLineItem, PaymentMethod,
Rma, RmaLineItem, RmaReason, Shipment, StoreCredit, StoreCreditAdjustment, Reason}
import org.json4s.jackson
import responses.CountryWithRegions

object JsonFormatters {
  val serialization = jackson.Serialization

  val DefaultFormats = org.json4s.DefaultFormats + time.JavaTimeJson4sSerializer.jsonFormat + Money.jsonFormat

  val phoenixFormats = DefaultFormats +
    Order.State.jsonFormat +
    OrderLineItem.State.jsonFormat +
    Shipment.State.jsonFormat +
    GiftCard.OriginType.jsonFormat +
    GiftCard.State.jsonFormat +
    GiftCardAdjustment.State.jsonFormat +
    StoreCredit.State.jsonFormat +
    StoreCredit.OriginType.jsonFormat +
    StoreCreditAdjustment.State.jsonFormat +
    Reason.ReasonType.jsonFormat +
    Rma.State.jsonFormat +
    Rma.RmaType.jsonFormat +
    RmaLineItem.InventoryDisposition.jsonFormat +
    RmaReason.ReasonType.jsonFormat +
    CreditCardCharge.State.jsonFormat +
    CountryWithRegions.jsonFormat +
    QueryStatement.Comparison.jsonFormat +
    Condition.Operator.jsonFormat +
    PaymentMethod.Type.jsonFormat +
    SharedSearch.Scope.jsonFormat
}

