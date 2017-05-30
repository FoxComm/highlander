sealed trait OfferResponse

/*
Since some line items (like gift cards) can be excluded from qualification,
promotion engine must let phoenix map over reference numbers in these responses and
apply appropriate discount
  */
case class PercentOff(percent: Int, lineItemRefs: NonEmptyList[String]) extends OfferResponse
case class AmountOff(amount: Int, lineItemRefs: NonEmptyList[String]) extends OfferResponse

case object FreeShipping extends OfferResponse
