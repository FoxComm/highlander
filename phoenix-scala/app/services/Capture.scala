package services

import scala.util.Random

import cats.implicits._
import failures.CouponFailures.CouponWithCodeCannotBeFound
import failures.ShippingMethodFailures.ShippingMethodNotFoundInOrder
import failures.GeneralFailure
import failures.CaptureFailures
import failures.PromotionFailures.PromotionNotFoundForContext

import scala.concurrent.Await
import scala.concurrent.duration._

import models.cord._
import models.cord.lineitems._
import models.coupon._
import models.customer.{Customer, Customers}
import models.inventory.{Sku, Skus}
import models.objects._
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.product.Mvp
import models.promotion._
import models.shipping.{ShippingMethods, ShippingMethod}
import responses.cord.OrderResponse

import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.{Apis, OrderReservation, SkuReservation}
import utils.db._
import utils.Money.Currency

import payloads.CapturePayloads
import responses.CaptureResponse
//TODO: Create order state InsufficientFundHold
//TODO: Create order state PaymentErrorHold

case class LineItemPrice(referenceNumber: String, sku: String, price: Int, currency: Currency)

case class Capture(
    payload: CapturePayloads.Capture)(implicit ec: EC, db: DB, apis: Apis, ac: AC, ctx: OC) {

  def capture: DbResultT[CaptureResponse.Root] =
    for {
      //get data for capture. We use the findLineItemsByCordRef function in 
      //OrderLineItemSkus to get all the relevant data for the order line item.
      //The function returns a tuple so we will convert it to a case class for
      //convenience.
      order ← * <~ Orders.mustFindByRefNum(payload.order)
      items ← * <~ OrderLineItemSkus.findLineItemsByCordRef(payload.order).result

      lineItemData ← * <~ items.map { lineItem ⇒
                      (OrderLineItemProductData.apply _).tupled(lineItem)
                    }

      //validate payload, make sure it's including all line items since we don
      //support split capture yet.
      _ ← * <~ validatePayload(payload, lineItemData)

      //get prices for line items using historical version of sku and adjust
      //the prices based on line item adjustments. Line item adjustments use
      //line item reference number to match the adjustment with the line item.
      //some line items will not have adjustments. Then finally aggregate to
      //get total line item price.
      linePrices  ← * <~ getPrices(lineItemData)
      adjustments ← * <~ OrderLineItemAdjustments.findByCordRef(payload.order).result
      lineItemAdjustments = adjustments.filter(a ⇒
            a.adjustmentType == OrderLineItemAdjustment.LineItemAdjustment)
      adjustedPrices     ← * <~ adjust(linePrices, lineItemAdjustments)
      totalLineItemPrice ← * <~ aggregatePrices(adjustedPrices)

      //find the shipping method used for the order, take the minimum between 
      //shipping method and what shipping cost was passed in payload because
      //we don't want to charge more than estimated. Finally adjust shipping cost
      //based on any adjustments.
      shippingMethod ← * <~ ShippingMethods
                        .forCordRef(payload.order)
                        .one
                        .mustFindOr(ShippingMethodNotFoundInOrder(payload.order))
      shippingAdjustments = adjustments.filter(a ⇒
            a.adjustmentType == OrderLineItemAdjustment.ShippingAdjustment)

      adjustedShippingCost ← * <~ adjustShippingCost(shippingMethod,
                                                     shippingAdjustments,
                                                     payload.shipping)

      //we compute the total by adding the three price components together. The
      //actual total should be less than or equal to the original grandTotal. 
      //It may be different because of various time differences between when 
      //taxes and shipping were computed. The computed grand total should never be bigger
      //than the estimated grand total.
      total = computeTotal(totalLineItemPrice,
                           adjustedShippingCost,
                           order.taxesTotal,
                           order.grandTotal)

      //Now let's determine how much we will get from the credit card
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByCordRef(payload.order).result
      scPayments ← * <~ OrderPayments.findAllStoreCreditsByCordRef(payload.order).result

      internalCaptureTotal ← * <~ determineInternalCapture(total,
                                                           gcPayments,
                                                           scPayments,
                                                           order.currency)
      externalCaptureTotal = total - internalCaptureTotal
      externalCapturedAmount ← * <~ externalCapture(externalCaptureTotal, order)

    } yield CaptureResponse.build("dummy")

  private def externalCapture(total: Int, order: Order): DbResultT[Option[CreditCardCharge]] = {
    require(total >= 0)
    import scala.concurrent.duration._

    if (total > 0) {
      (for {
        pmt    ← OrderPayments.findAllCreditCardsForOrder(payload.order)
        charge ← CreditCardCharges.filter(_.orderPaymentId === pmt.id)
      } yield charge).one.toXor.flatMap {
        case Some(charge) ⇒ captureFromStripe(total, charge, order)
        case None ⇒
          DbResultT.failure(GeneralFailure("Unable to find a credit card for the order."))
      }
    } else DbResultT.none
  }

  private def captureFromStripe(total: Int,
                                charge: CreditCardCharge,
                                order: Order): DbResultT[Option[CreditCardCharge]] = {
    val f = Stripe().captureCharge(charge.chargeId, total)

    for {
      stripeCharge ← * <~ Await.result(f, 5.seconds)
      updatedCharge = charge.copy(state = CreditCardCharge.FullCapture)
      chargeId ← * <~ CreditCardCharges.update(updatedCharge)
      _        ← * <~ LogActivity.creditCardCharge(order, updatedCharge)
    } yield updatedCharge.some
  }

  private def determineInternalCapture(total: Int,
                                       gcPayments: Seq[(OrderPayment, GiftCard)],
                                       scPayments: Seq[(OrderPayment, StoreCredit)],
                                       currency: Currency): DbResultT[Int] =
    for {
      remaining ← * <~ subtractGcPayments(total, gcPayments, currency)
      remaining ← * <~ subtractScPayments(remaining, scPayments, currency)
    } yield remaining

  private def subtractGcPayments(total: Int,
                                 gcPayments: Seq[(OrderPayment, GiftCard)],
                                 currency: Currency): Int = {
    Math.max(0, total - gcPayments.foldLeft(0)((a, op) ⇒ a + getPaymentAmount(op._1, currency)))
  } ensuring (t ⇒ t >= 0 && t <= total)

  private def subtractScPayments(total: Int,
                                 scPayments: Seq[(OrderPayment, StoreCredit)],
                                 currency: Currency): Int = {
    Math.max(0, total - scPayments.foldLeft(0)((a, op) ⇒ a + getPaymentAmount(op._1, currency)))
  } ensuring (t ⇒ t >= 0 && t <= total)

  private def getPaymentAmount(op: OrderPayment, currency: Currency): Int = {
    require(currency == op.currency)
    op.amount.getOrElse(0)
  } ensuring (_ >= 0)

  private def computeTotal(lineItemTotal: Int,
                           shippingCost: Int,
                           taxes: Int,
                           originalGrandTotal: Int): Int = {
    require(lineItemTotal >= 0)
    require(shippingCost >= 0)
    require(taxes >= 0)
    require(originalGrandTotal >= 0)

    lineItemTotal + shippingCost + taxes
  } ensuring (t ⇒ t <= originalGrandTotal && t >= 0)

  private def adjustShippingCost(shippingMethod: ShippingMethod,
                                 adjustments: Seq[OrderLineItemAdjustment],
                                 requestedShippingCost: CapturePayloads.ShippingCost): Int = {
    require(adjustments.length <= 1)
    require(requestedShippingCost.total >= 0)
    require(shippingMethod.price >= 0)

    adjustments.headOption match {
      case Some(adjustment) ⇒ {
        require(adjustment.subtract >= 0)
        Math.max(0,
                 Math.min(shippingMethod.price - adjustment.subtract, requestedShippingCost.total))
      }
      case None ⇒ Math.min(shippingMethod.price, requestedShippingCost.total)
    }
  } ensuring (_ >= 0)

  private def aggregatePrices(adjustedPrices: Seq[LineItemPrice]): Int = {
    val total = adjustedPrices.foldLeft(0)({ (a, p) ⇒
      require(p.price >= 0)
      a + p.price
    })

    total
  } ensuring (_ >= 0)

  private val NO_REF = "no_ref"

  private def adjust(linePrices: Seq[LineItemPrice],
                     adjustments: Seq[OrderLineItemAdjustment]): DbResultT[Seq[LineItemPrice]] = {
    val adjMap = adjustments.map(a ⇒ a.lineItemRefNum.getOrElse(NO_REF) → a).toMap
    for {
      adjustedPrices ← * <~ linePrices.map { p ⇒
                        adjustPrice(p, adjMap)
                      }
    } yield adjustedPrices

  }

  private def adjustPrice(line: LineItemPrice,
                          adjMap: Map[String, OrderLineItemAdjustment]): LineItemPrice = {
    adjMap.get(line.referenceNumber) match {
      case Some(adj) ⇒ {
        require(line.price >= 0)
        require(adj.subtract >= 0)
        require(line.price >= adj.subtract)

        val price = line.price - adj.subtract

        assume(price >= 0)
        line.copy(price = price)
      }
      case None ⇒ line
    }
  }

  private def getPrices(items: Seq[OrderLineItemProductData]): DbResultT[Seq[LineItemPrice]] =
    for {
      prices ← * <~ items.map { i ⇒
                getPrice(i)
              }
    } yield prices

  private def getPrice(item: OrderLineItemProductData): DbResultT[LineItemPrice] =
    Mvp.price(item.skuForm, item.skuShadow) match {
      case Some((price, currency)) ⇒
        DbResultT.pure(
            LineItemPrice(item.lineItem.referenceNumber, item.sku.code, price, currency))
      case None ⇒ DbResultT.failure(CaptureFailures.SkuMissingPrice(item.sku.code))
    }

  private def validatePayload(payload: CapturePayloads.Capture,
                              orderSkus: Seq[OrderLineItemProductData]): DbResultT[Unit] =
    for {
      codes ← * <~ orderSkus.map { _.sku.code }
      _     ← * <~ mustHaveCodes(payload.items, codes, payload.order)
      _     ← * <~ mustHaveSameLineItems(payload.items.length, orderSkus.length, payload.order)
      _     ← * <~ mustHavePositiveShippingCost(payload.shipping)
    } yield Unit

  private def mustHavePositiveShippingCost(
      shippingCost: CapturePayloads.ShippingCost): DbResultT[Unit] =
    if (shippingCost.total < 0)
      DbResultT.failure(CaptureFailures.ShippingCostNegative(shippingCost.total))
    else DbResultT.pure(Unit)

  private def mustHaveCodes(items: Seq[CapturePayloads.CaptureLineItem],
                            codes: Seq[String],
                            orderRef: String): DbResultT[Unit] =
    for {
      _ ← * <~ items.map { i ⇒
           mustHaveCode(i, codes, orderRef)
         }
    } yield Unit

  private def mustHaveCode(item: CapturePayloads.CaptureLineItem,
                           codes: Seq[String],
                           orderRef: String): DbResultT[Unit] =
    if (codes.contains(item.sku)) DbResultT.pure(Unit)
    else DbResultT.failure(CaptureFailures.SkuNotFoundInOrder(item.sku, orderRef))

  private def mustHaveSameLineItems(lOne: Int, lTwo: Int, orderRef: String): DbResultT[Unit] =
    if (lOne == lTwo) DbResultT.pure(Unit)
    else DbResultT.failure(CaptureFailures.SplitCaptureNotSupported(orderRef))

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
