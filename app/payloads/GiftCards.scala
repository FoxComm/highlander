package payloads

import cats.data._
import cats.implicits._
import models.GiftCard
import services.Failure
import utils.Litterbox._
import utils.Money._
import utils.Validation
import Validation._

final case class GiftCardCreateByCsr(balance: Int, currency: Currency = Currency.USD)
  extends Validation[GiftCardCreateByCsr] {

  def validate: ValidatedNel[Failure, GiftCardCreateByCsr] = {
    validExpr(balance > 0, "Balance must be greater than zero").map { case _ ⇒ this }
  }
}

final case class GiftCardBulkCreateByCsr(quantity: Int, balance: Int, currency: Currency = Currency.USD)
  extends Validation[GiftCardBulkCreateByCsr] {

  val bulkCreateLimit = 20

  def validate: ValidatedNel[Failure, GiftCardBulkCreateByCsr] = {
    (validExpr(balance > 0, "Balance must be greater than zero")
      |@| validExpr(quantity > 0, "Quantity must be greater than zero")
      |@| validExpr(quantity <= bulkCreateLimit, "Bulk creation limit exceeded")
      ).map { case _ ⇒ this }
  }
}

final case class GiftCardUpdateStatusByCsr(status: GiftCard.Status, reason: Option[Int] = None)
  extends Validation[GiftCardUpdateStatusByCsr] {

  def validate: ValidatedNel[Failure, GiftCardUpdateStatusByCsr] = {
    GiftCard.validateStatusReason(status, reason).map { case _ ⇒ this }
  }
}

final case class GiftCardBulkUpdateStatusByCsr(codes: Seq[String], status: GiftCard.Status, reason: Option[Int] = None)
  extends Validation[GiftCardBulkUpdateStatusByCsr] {

  val bulkUpdateLimit = 20

  def validate: ValidatedNel[Failure, GiftCardBulkUpdateStatusByCsr] = {
    (GiftCard.validateStatusReason(status, reason)
      |@| validExpr(codes.nonEmpty, "Please provide at least one code to update")
      |@| validExpr(codes.length <= bulkUpdateLimit, "Bulk update limit exceeded")
      ).map { case _ ⇒ this }
  }
}