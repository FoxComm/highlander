package responses.cord

import models.cord.OrderPayment
import models.location.Region
import models.payment.creditcard.CreditCard
import responses.PromotionResponses.IlluminatedPromotionResponse

package object base {

  type CordResponsePromoDetails =
    Option[(IlluminatedPromotionResponse.Root, CordResponseCouponPair)]

  type CordResponseCcPayment = (OrderPayment, CreditCard, Region)

}
