package phoenix.services

import cats.implicits._
import objectframework.FormShadowGet
import phoenix.failures.CaptureFailures.ExternalPaymentNotFound
import phoenix.failures.CaptureFailures
import phoenix.failures.ShippingMethodFailures.ShippingMethodNotFoundInOrder
import phoenix.models.account.{User, Users}
import phoenix.models.cord._
import phoenix.models.cord.lineitems._
import phoenix.models.payment.creditcard._
import phoenix.models.payment.giftcard._
import phoenix.models.payment.storecredit._
import phoenix.models.shipping.{ShippingMethod, ShippingMethods}
import phoenix.payloads.CapturePayloads
import phoenix.responses.CaptureResponse
import phoenix.services.orders.OrderQueries
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import slick.jdbc.PostgresProfile.api._
import core.utils.Money.Currency
import core.db._
import core.utils.Money._
import phoenix.models.payment.{ExternalCharge, ExternalChargeVals}
import phoenix.models.payment.ExternalCharge._
import phoenix.models.payment.applepay.ApplePayCharges

//
//TODO: Create order state InsufficientFundHold
//TODO: Create order state PaymentErrorHold
//
case class LineItemPrice(referenceNumber: String, sku: String, price: Long, currency: Currency)

object Capture {
  def capture(payload: CapturePayloads.Capture)(implicit ec: EC,
                                                db: DB,
                                                apis: Apis,
                                                ac: AC): DbResultT[CaptureResponse] =
    Capture(payload).capture
}

case class Capture(payload: CapturePayloads.Capture)(implicit ec: EC, db: DB, apis: Apis, ac: AC) {
  def capture: DbResultT[CaptureResponse] =
    for {
      //get data for capture. We use the findLineItemsByCordRef function in
      //OrderLineItems to get all the relevant data for the order line item.
      //The function returns a tuple so we will convert it to a case class for
      //convenience.
      order    ← * <~ Orders.mustFindByRefNum(payload.order)
      payState ← * <~ OrderQueries.getCordPaymentState(order.refNum)
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
      adjustments ← * <~ CartLineItemAdjustments.findByCordRef(payload.order).result
      lineItemAdjustments = adjustments.filter(_.adjustmentType == CartLineItemAdjustment.LineItemAdjustment)
      adjustedPrices     ← * <~ adjust(linePrices, lineItemAdjustments)
      totalLineItemPrice ← * <~ aggregatePrices(adjustedPrices)

      orderAdjustmentCost = adjustments
        .filter(_.adjustmentType == CartLineItemAdjustment.OrderAdjustment)
        .map(_.subtract)
        .sum

      //find the shipping method used for the order, take the minimum between
      //shipping method and what shipping cost was passed in payload because
      //we don't want to charge more than estimated. Finally adjust shipping cost
      //based on any adjustments.
      shippingMethod ← * <~ ShippingMethods
                        .forCordRef(payload.order)
                        .mustFindOneOr(ShippingMethodNotFoundInOrder(payload.order))
      shippingAdjustments = adjustments.filter(a ⇒
        a.adjustmentType == CartLineItemAdjustment.ShippingAdjustment)

      adjustedShippingCost ← * <~ adjustShippingCost(shippingMethod, shippingAdjustments, payload.shipping)

      //we compute the total by adding the three price components together. The
      //actual total should be less than or equal to the original grandTotal.
      //It may be different because of various time differences between when
      //taxes and shipping were computed. The computed grand total should never be bigger
      //than the estimated grand total.
      total = computeTotal(totalLineItemPrice,
                           adjustedShippingCost,
                           orderAdjustmentCost,
                           order.taxesTotal,
                           order.grandTotal)

      //Now let's determine how much we will get from the credit card
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByCordRef(payload.order).result
      scPayments ← * <~ OrderPayments.findAllStoreCreditsByCordRef(payload.order).result

      externalCaptureTotal ← * <~ determineExternalCapture(total, gcPayments, scPayments, order.currency)
      internalCaptureTotal = total - externalCaptureTotal
      _ ← * <~ internalCapture(internalCaptureTotal, order, customer, gcPayments, scPayments)
      _ ← * <~ when(externalCaptureTotal > 0, externalCapture(externalCaptureTotal, order))

      resp = CaptureResponse(
        order = order.refNum,
        captured = total,
        external = externalCaptureTotal,
        internal = internalCaptureTotal,
        lineItems = totalLineItemPrice,
        taxes = order.taxesTotal,
        shipping = adjustedShippingCost,
        currency = order.currency
      )

      _ ← * <~ LogActivity().orderCaptured(order, resp)
      //return Capture table tuple id?
    } yield resp

  private def internalCapture(total: Long,
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

      _ ← * <~ when(scTotal > 0, LogActivity().scFundsCaptured(customer, order, scIds, scTotal).void)
      _ ← * <~ when(gcTotal > 0, LogActivity().gcFundsCaptured(customer, order, gcCodes, gcTotal).void)
    } yield {}

  private def externalCapture(total: Long, order: Order): DbResultT[Unit] = {
    def capture(charge: ExternalCharge[_]) = captureFromStripe(total, charge, order)

    for {
      pmt ← * <~ OrderPayments
             .findAllExternalPayments(payload.order)
             .mustFindOneOr(ExternalPaymentNotFound(order.refNum))

      // we must find one the following charges
      apCharge ← * <~ ApplePayCharges.filter(_.orderPaymentId === pmt.id).one
      ccCharge ← * <~ CreditCardCharges.filter(_.orderPaymentId === pmt.id).one
      externalCharges = Set(apCharge, ccCharge)

      _ ← * <~ failIf(externalCharges.forall(_.isEmpty), ExternalPaymentNotFound(order.refNum))

      // capture one of external charges
      // todo here we need to fold Set[Option[DbResultT]]
      _ ← * <~ (apCharge map capture)
      _ ← * <~ (ccCharge map capture)
    } yield ()
  }

  private def captureFromStripe(total: Long, charge: ExternalCharge[_], order: Order): DbResultT[Unit] =
    for {
      _ ← * <~ failIfNot(charge.state == Auth, CaptureFailures.ChargeNotInAuth(charge))
      _ ← * <~ apis.stripe.captureCharge(charge.stripeChargeId, total)
      _ ← * <~ charge.updateModelState(FullCapture)
    } yield ()

  private def determineExternalCapture(total: Long,
                                       gcPayments: Seq[(OrderPayment, GiftCard)],
                                       scPayments: Seq[(OrderPayment, StoreCredit)],
                                       currency: Currency): DbResultT[Long] =
    for {
      remaining ← * <~ subtractGcPayments(total, gcPayments, currency)
      remaining ← * <~ subtractScPayments(remaining, scPayments, currency)
    } yield remaining

  private def subtractGcPayments(total: Long,
                                 gcPayments: Seq[(OrderPayment, GiftCard)],
                                 currency: Currency): Long = {
    (total - gcPayments.map(op ⇒ getPaymentAmount(op._1, currency)).sum).zeroIfNegative
  } ensuring (remaining ⇒ remaining >= 0 && remaining <= total)

  private def subtractScPayments(total: Long,
                                 scPayments: Seq[(OrderPayment, StoreCredit)],
                                 currency: Currency): Long = {
    (total - scPayments.map(op ⇒ getPaymentAmount(op._1, currency)).sum).zeroIfNegative
  } ensuring (remaining ⇒ remaining >= 0 && remaining <= total)

  private def getPaymentAmount(op: OrderPayment, currency: Currency): Long = {
    require(currency == op.currency)
    op.amount.getOrElse(0L)
  } ensuring (_ >= 0)

  private def computeTotal(lineItemTotal: Long,
                           shippingCost: Long,
                           orderAdjustmentCost: Long,
                           taxes: Long,
                           originalGrandTotal: Long): Long = {
    require(lineItemTotal >= 0)
    require(shippingCost >= 0)
    require(taxes >= 0)
    require(originalGrandTotal >= 0)
    require(orderAdjustmentCost >= 0)

    lineItemTotal + shippingCost + taxes - orderAdjustmentCost
  } ensuring (t ⇒ t <= originalGrandTotal && t >= 0)

  private def adjustShippingCost(shippingMethod: ShippingMethod,
                                 adjustments: Seq[CartLineItemAdjustment],
                                 requestedShippingCost: CapturePayloads.ShippingCost): Long = {
    require(adjustments.length <= 1)
    require(requestedShippingCost.total >= 0)
    require(shippingMethod.price >= 0)

    adjustments.headOption match {
      case Some(adjustment) ⇒ {
        require(adjustment.subtract >= 0)
        Math
          .min(shippingMethod.price - adjustment.subtract, requestedShippingCost.total)
          .zeroIfNegative
      }
      case None ⇒
        Math.min(shippingMethod.price, requestedShippingCost.total)
    }
  } ensuring (_ >= 0)

  private def aggregatePrices(adjustedPrices: Seq[LineItemPrice]): Long = {
    adjustedPrices.map { lineItem ⇒
      require(lineItem.price >= 0) // FIXME: woot? @michalrus
      lineItem.price
    }.sum
  } ensuring (_ >= 0)

  private val NO_REF = "no_ref"

  private def adjust(linePrices: Seq[LineItemPrice],
                     adjustments: Seq[CartLineItemAdjustment]): DbResultT[Seq[LineItemPrice]] = {
    val adjMap = adjustments.map(a ⇒ a.lineItemRefNum.getOrElse(NO_REF) → a).toMap
    for {
      adjustedPrices ← * <~ linePrices.map { p ⇒
                        adjustPrice(p, adjMap)
                      }
    } yield adjustedPrices

  }

  private def adjustPrice(line: LineItemPrice, adjMap: Map[String, CartLineItemAdjustment]): LineItemPrice =
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

  private def getPrices(items: Seq[OrderLineItemProductData]): DbResultT[List[LineItemPrice]] =
    DbResultT.seqCollectFailures(items.map { i ⇒
      getPrice(i)
    }.toList)

  private def getPrice(item: OrderLineItemProductData): DbResultT[LineItemPrice] =
    FormShadowGet.price(item.skuForm, item.skuShadow) match {
      case Some((price, currency)) ⇒
        LineItemPrice(item.lineItem.referenceNumber, item.sku.code, price, currency).pure[DbResultT]
      case None ⇒ DbResultT.failure(CaptureFailures.SkuMissingPrice(item.sku.code))
    }

  // FIXME: use MonadError below, no need for DbResultT @michalrus

  private def validatePayload(payload: CapturePayloads.Capture,
                              orderSkus: Seq[OrderLineItemProductData]): DbResultT[Unit] =
    for {
      codes ← * <~ orderSkus.map { _.sku.code }
      _     ← * <~ mustHaveCodes(payload.items, codes, payload.order)
      _     ← * <~ mustHaveSameLineItems(payload.items.length, orderSkus.length, payload.order)
      _     ← * <~ mustHavePositiveShippingCost(payload.shipping)
    } yield Unit

  private def validateOrder(order: Order, paymentState: CordPaymentState.State): DbResultT[Unit] =
    for {
      _ ← * <~ paymentStateMustBeInAuth(order, paymentState)
      //Future validation goes here.
    } yield Unit

  private def paymentStateMustBeInAuth(order: Order, paymentState: CordPaymentState.State): DbResultT[Unit] =
    if (paymentState != CordPaymentState.Auth)
      DbResultT.failure(CaptureFailures.OrderMustBeInAuthState(order.refNum))
    else ().pure[DbResultT]

  private def mustHavePositiveShippingCost(shippingCost: CapturePayloads.ShippingCost): DbResultT[Unit] =
    if (shippingCost.total < 0)
      DbResultT.failure(CaptureFailures.ShippingCostNegative(shippingCost.total))
    else ().pure[DbResultT]

  private def mustHaveCodes(items: Seq[CapturePayloads.CaptureLineItem],
                            codes: Seq[String],
                            orderRef: String): DbResultT[List[Unit]] =
    DbResultT.seqCollectFailures(items.map { i ⇒
      mustHaveCode(i, codes, orderRef)
    }.toList)

  private def mustHaveCode(item: CapturePayloads.CaptureLineItem,
                           codes: Seq[String],
                           orderRef: String): DbResultT[Unit] =
    if (codes.contains(item.sku)) ().pure[DbResultT]
    else DbResultT.failure(CaptureFailures.SkuNotFoundInOrder(item.sku, orderRef))

  private def mustHaveSameLineItems(lOne: Int, lTwo: Int, orderRef: String): DbResultT[Unit] =
    if (lOne == lTwo) ().pure[DbResultT]
    else DbResultT.failure(CaptureFailures.SplitCaptureNotSupported(orderRef))
}
