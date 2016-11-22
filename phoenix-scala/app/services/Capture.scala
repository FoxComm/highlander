package services

import cats.implicits._
import failures.CouponFailures.CouponWithCodeCannotBeFound
import failures.ShippingMethodFailures.ShippingMethodNotFoundInOrder
import failures.GeneralFailure
import failures.CaptureFailures
import failures.PromotionFailures.PromotionNotFoundForContext
import models.account.{User, Users}
import models.cord._
import models.cord.lineitems._
import models.coupon._
import models.inventory.{Sku, Skus}
import models.objects._
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.product.Mvp
import models.shipping.{ShippingMethod, ShippingMethods}
import payloads.CapturePayloads
import responses.CaptureResponse
import services.orders.OrderQueries
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.aliases._
import utils.apis.Apis
import utils.db._

//
//TODO: Create order state InsufficientFundHold
//TODO: Create order state PaymentErrorHold
//
case class LineItemPrice(referenceNumber: String, sku: String, price: Int, currency: Currency)

object Capture {
  def capture(payload: CapturePayloads.Capture)(implicit ec: EC,
                                                db: DB,
                                                apis: Apis,
                                                ac: AC): DbResultT[CaptureResponse] = {
    Capture(payload).capture
  }
}

case class Capture(payload: CapturePayloads.Capture)(implicit ec: EC, db: DB, apis: Apis, ac: AC) {

  def capture: DbResultT[CaptureResponse] =
    for {
      //get data for capture. We use the findLineItemsByCordRef function in 
      //OrderLineItems to get all the relevant data for the order line item.
      //The function returns a tuple so we will convert it to a case class for
      //convenience.
      order    ← * <~ Orders.mustFindByRefNum(payload.order)
      payState ← * <~ OrderQueries.getPaymentState(order.refNum)
      _        ← * <~ validateOrder(order, payState)

      customer     ← * <~ Users.mustFindByAccountId(order.accountId)
      lineItemData ← * <~ LineItemManager.getOrderLineItems(payload.order)

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
      lineItemAdjustments = adjustments.filter(
          _.adjustmentType == OrderLineItemAdjustment.LineItemAdjustment)
      adjustedPrices     ← * <~ adjust(linePrices, lineItemAdjustments)
      totalLineItemPrice ← * <~ aggregatePrices(adjustedPrices)

      //find the shipping method used for the order, take the minimum between 
      //shipping method and what shipping cost was passed in payload because
      //we don't want to charge more than estimated. Finally adjust shipping cost
      //based on any adjustments.
      shippingMethod ← * <~ ShippingMethods
                        .forCordRef(payload.order)
                        .mustFindOneOr(ShippingMethodNotFoundInOrder(payload.order))
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

      externalCaptureTotal ← * <~ determineExternalCapture(total,
                                                           gcPayments,
                                                           scPayments,
                                                           order.currency)
      internalCaptureTotal = total - externalCaptureTotal
      _ ← * <~ externalCapture(externalCaptureTotal, order)
      _ ← * <~ internalCapture(internalCaptureTotal, order, customer, gcPayments, scPayments)

      resp = CaptureResponse(order = order.refNum,
                             captured = total,
                             external = externalCaptureTotal,
                             internal = internalCaptureTotal,
                             lineItems = totalLineItemPrice,
                             taxes = order.taxesTotal,
                             shipping = adjustedShippingCost,
                             currency = order.currency)

      _ ← * <~ LogActivity.orderCaptured(order, resp)
      //return Capture table tuple id?
    } yield resp

  private def internalCapture(total: Int,
                              order: Order,
                              customer: User,
                              gcPayments: Seq[(OrderPayment, GiftCard)],
                              scPayments: Seq[(OrderPayment, StoreCredit)]): DbResultT[Unit] =
    for {

      scTotal ← * <~ PaymentHelper.paymentTransaction(
                   scPayments,
                   total,
                   StoreCredits.captureOrderPayment,
                   (a: StoreCreditAdjustment) ⇒ a.getAmount.abs
               )

      gcTotal ← * <~ PaymentHelper.paymentTransaction(gcPayments,
                                                      total - scTotal,
                                                      GiftCards.captureOrderPayment,
                                                      (a: GiftCardAdjustment) ⇒ a.getAmount.abs)

      scIds   = scPayments.map { case (_, sc) ⇒ sc.id }.distinct
      gcCodes = gcPayments.map { case (_, gc) ⇒ gc.code }.distinct

      _ ← * <~ doOrMeh(scTotal > 0, LogActivity.scFundsCaptured(customer, order, scIds, scTotal))
      _ ← * <~ doOrMeh(gcTotal > 0, LogActivity.gcFundsCaptured(customer, order, gcCodes, gcTotal))
    } yield {}

  private def externalCapture(total: Int, order: Order): DbResultT[Option[CreditCardCharge]] = {
    require(total >= 0)

    if (total > 0) {
      (for {
        pmt    ← OrderPayments.findAllCreditCardsForOrder(payload.order)
        charge ← CreditCardCharges.filter(_.orderPaymentId === pmt.id)
      } yield charge).one.dbresult.flatMap {
        case Some(charge) ⇒ captureFromStripe(total, charge, order)
        case None ⇒
          DbResultT.failure(CaptureFailures.CreditCardNotFound(order.refNum))
      }
    } else DbResultT.none
  }

  private def captureFromStripe(total: Int,
                                charge: CreditCardCharge,
                                order: Order): DbResultT[Option[CreditCardCharge]] = {

    if (charge.state == CreditCardCharge.Auth) {
      for {
        stripeCharge ← * <~ apis.stripe.captureCharge(charge.chargeId, total)
        updatedCharge = charge.copy(state = CreditCardCharge.FullCapture)
        _ ← * <~ CreditCardCharges.update(charge, updatedCharge)
        _ ← * <~ LogActivity.creditCardCharge(order, updatedCharge)
      } yield updatedCharge.some
    } else
      DbResultT.failure(CaptureFailures.ChargeNotInAuth(charge))
  }

  private def determineExternalCapture(total: Int,
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
  } ensuring (remaining ⇒ remaining >= 0 && remaining <= total)

  private def subtractScPayments(total: Int,
                                 scPayments: Seq[(OrderPayment, StoreCredit)],
                                 currency: Currency): Int = {
    Math.max(0, total - scPayments.foldLeft(0)((a, op) ⇒ a + getPaymentAmount(op._1, currency)))
  } ensuring (remaining ⇒ remaining >= 0 && remaining <= total)

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
      case None ⇒
        Math.min(shippingMethod.price, requestedShippingCost.total)
    }
  } ensuring (_ >= 0)

  private def aggregatePrices(adjustedPrices: Seq[LineItemPrice]): Int = {
    val total = adjustedPrices.foldLeft(0)({ (sum, lineItem) ⇒
      require(lineItem.price >= 0)
      sum + lineItem.price
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
                          adjMap: Map[String, OrderLineItemAdjustment]): LineItemPrice =
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

  private def getPrices(items: Seq[OrderLineItemProductData]): DbResultT[Seq[LineItemPrice]] =
    DbResultT.sequence(items.map { i ⇒
      getPrice(i)
    })

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

  private def validateOrder(order: Order, paymentState: CreditCardCharge.State): DbResultT[Unit] =
    for {
      _ ← * <~ paymentStateMustBeInAuth(order, paymentState)
      //Future validation goes here.
    } yield Unit

  private def paymentStateMustBeInAuth(order: Order,
                                       paymentState: CreditCardCharge.State): DbResultT[Unit] =
    if (paymentState != CreditCardCharge.Auth)
      DbResultT.failure(CaptureFailures.OrderMustBeInAuthState(order.refNum))
    else DbResultT.pure(Unit)

  private def mustHavePositiveShippingCost(
      shippingCost: CapturePayloads.ShippingCost): DbResultT[Unit] =
    if (shippingCost.total < 0)
      DbResultT.failure(CaptureFailures.ShippingCostNegative(shippingCost.total))
    else DbResultT.pure(Unit)

  private def mustHaveCodes(items: Seq[CapturePayloads.CaptureLineItem],
                            codes: Seq[String],
                            orderRef: String): DbResultT[Seq[Unit]] =
    DbResultT.sequence(items.map { i ⇒
      mustHaveCode(i, codes, orderRef)
    })

  private def mustHaveCode(item: CapturePayloads.CaptureLineItem,
                           codes: Seq[String],
                           orderRef: String): DbResultT[Unit] =
    if (codes.contains(item.sku)) DbResultT.pure(Unit)
    else DbResultT.failure(CaptureFailures.SkuNotFoundInOrder(item.sku, orderRef))

  private def mustHaveSameLineItems(lOne: Int, lTwo: Int, orderRef: String): DbResultT[Unit] =
    if (lOne == lTwo) DbResultT.pure(Unit)
    else DbResultT.failure(CaptureFailures.SplitCaptureNotSupported(orderRef))
}
