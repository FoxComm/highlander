package services

import scala.util.Random

import cats.implicits._
import failures.CouponFailures.CouponWithCodeCannotBeFound
import failures.GeneralFailure
import failures.PromotionFailures.PromotionNotFoundForContext
import models.cord._
import models.cord.lineitems.{OrderLineItemGiftCards, OrderLineItemSkus}
import models.coupon._
import models.customer.{Customer, Customers}
import models.objects._
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.promotion._
import responses.cord.OrderResponse
import services.coupon.CouponUsageService
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.{Apis, OrderReservation, SkuReservation}
import utils.db._

import payloads.CapturePayloads
import responses.CaptureResponse
//TODO: Create order state InsufficientFundHold
//TODO: Create order state PaymentErrorHold

case class Capture(
    payload: CapturePayloads.Capture)(implicit ec: EC, db: DB, apis: Apis, ac: AC, ctx: OC) {

  def capture: DbResultT[CaptureResponse.Root] = DbResultT.good(CaptureResponse.build("dummy"))

  /*
  def capture: DbResultT[CaptureResponse] = { 

    
    //pseudo code for whole capture
    //all in transaction
    for {
      if (lineItems != order.lineItems) fail("split capture not supported")

      adjustedLineItems = adjust(lineItems, order.adjustments)
      adjustedShipping = adjust(order.adjustments, shippingCost)
      totalLineItems = aggregatePrices(adjustedLineItems) 
      totalShipping = adjustedShipping.price
      tax = order.tax

      total = totalLineItems + totalShipping + tax

      errors = captureViaPaymentMethods(order.paymentMethods)
      if(errors)
      {
        abort transaction!
        fail("failed, stop shipment!")
      }

    } yield CaptureResponse(total, errors)
  }



  def capturePaymentMethods(order: Order, paymentMethods: PaymentMethods, total: Int, currency: Currency) = {
      assert(total >= 0) 

      creditCardAmount = determineCreditCartAmount(order, paymentMethods, total, currency)

      if(creditCardAmount > 0) 
        errors = captureCreditCards(order.creditCards, creditCardAmount, currency)

      if(errors) return errors

      remaining = total - creditCardAmount

      remaining = captureGiftCards(paymentMethods.giftCards, remaning, currency)
      if(remaining > 0)
        captureStoreCredit(order.customer, remaning, currency)

      return Seq.empty
  }

  def determineCreditCartAmount(order....)


  def captureCreditCards(order: Order, total: Int, currency: Currency) = 
    for {
      paymentGatewayType = order.paymentGatewayType
      if(paymentGatewayType != "stripe") fail("we are lame")

      gatewayCustomer = Sripe.findCustomer(order.customer)
      authToken = Strip.findAuth(order)
      
      errors = Stripe.capture(authToken, total, currency)
    } yield errors

 */

}
