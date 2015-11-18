package payloads

import cats.data._
import models.Rma
import services.Failure
import utils.Validation
import Validation._

final case class RmaCreatePayload(orderRefNum: String, rmaType: Rma.RmaType)

final case class RmaUpdateStatusPayload(status: Rma.Status)

/* Line item updater payloads */

final case class RmaSkuLineItemsPayload(sku: String, quantity: Int, reasonId: Int)

final case class RmaGiftCardLineItemsPayload(code: String, reasonId: Int)

final case class RmaShippingCostLineItemsPayload(id: Int, reasonId: Int)

/* Payment payloads */

final case class RmaPaymentPayload(amount: Int) extends Validation[RmaPaymentPayload] {

  def validate: ValidatedNel[Failure, RmaPaymentPayload] = {
    greaterThan(amount, 0, "Amount").map { case _ ⇒ this }
  }
}

final case class RmaCcPaymentPayload(creditCardId: Int, amount: Int) extends Validation[RmaCcPaymentPayload] {

  def validate: ValidatedNel[Failure, RmaCcPaymentPayload] = {
    greaterThan(amount, 0, "Amount").map { case _ ⇒ this }
  }
}

/* Assignees */

final case class RmaAssigneesPayload(assignees: Seq[Int])

final case class RmaBulkAssigneesPayload(referenceNumbers: Seq[String], assigneeId: Int)