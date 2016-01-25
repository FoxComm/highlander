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
    OrderLineItem.Status.jsonFormat +
    Shipment.Status.jsonFormat +
    GiftCard.OriginType.jsonFormat +
    GiftCard.Status.jsonFormat +
    GiftCardAdjustment.Status.jsonFormat +
    StoreCredit.Status.jsonFormat +
    StoreCredit.OriginType.jsonFormat +
    StoreCreditAdjustment.Status.jsonFormat +
    Reason.ReasonType.jsonFormat +
    Rma.Status.jsonFormat +
    Rma.RmaType.jsonFormat +
    RmaLineItem.InventoryDisposition.jsonFormat +
    RmaReason.ReasonType.jsonFormat +
    CreditCardCharge.Status.jsonFormat +
    CountryWithRegions.jsonFormat +
    QueryStatement.Comparison.jsonFormat +
    Condition.Operator.jsonFormat +
    PaymentMethod.Type.jsonFormat +
    SharedSearch.Scope.jsonFormat
}

