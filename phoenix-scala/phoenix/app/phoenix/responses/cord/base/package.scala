package phoenix.responses.cord

import phoenix.models.cord.OrderPayment
import phoenix.models.location.Region
import phoenix.models.payment.creditcard.CreditCard
import phoenix.responses.PromotionResponses.PromotionResponse

package object base {

  type CordResponsePromoDetails = (PromotionResponse.Root, Option[CordResponseCouponPair])

  type CordResponseCcPayment = (OrderPayment, CreditCard, Region)

}
