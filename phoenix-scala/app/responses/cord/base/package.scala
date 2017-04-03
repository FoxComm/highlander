package responses.cord

import models.cord.OrderPayment
import models.location.Region
import models.payment.creditcard.CreditCard
import responses.PromotionResponses.PromotionResponse

package object base {

  type CordResponsePromoDetails = (PromotionResponse.Root, Option[CordResponseCouponPair])

  type CordResponseCcPayment = (OrderPayment, CreditCard, Region)

}
