package payloads

import cats.data._
import cats.implicits._
import models.GiftCard
import services.Failure
import utils.Litterbox._
import utils.Money._
import utils.Validation
import Validation._

final case class GiftCardCreateByCsr(balance: Int, reasonId: Int, currency: Currency = Currency.USD,
  subTypeId: Option[Int] = None)
  extends Validation[GiftCardCreateByCsr] {

  def validate: ValidatedNel[Failure, GiftCardCreateByCsr] = {
    greaterThan(balance, 0, "Balance").map { case _ ⇒ this }
  }
}

final case class GiftCardBulkCreateByCsr(quantity: Int, balance: Int, reasonId: Int, currency: Currency = Currency.USD,
  subTypeId: Option[Int] = None)
  extends Validation[GiftCardBulkCreateByCsr] {

  val bulkCreateLimit = 20

  def validate: ValidatedNel[Failure, GiftCardBulkCreateByCsr] = {
    (greaterThan(balance , 0, "Balance")
      |@| greaterThan(quantity, 0, "Quantity")
      |@| lesserThanOrEqual(quantity, bulkCreateLimit, "Quantity")
      ).map { case _ ⇒ this }
  }
}

final case class GiftCardUpdateStatusByCsr(status: GiftCard.Status, reasonId: Option[Int] = None)
  extends Validation[GiftCardUpdateStatusByCsr] {

  def validate: ValidatedNel[Failure, GiftCardUpdateStatusByCsr] = {
    GiftCard.validateStatusReason(status, reasonId).map { case _ ⇒ this }
  }
}

final case class GiftCardBulkUpdateStatusByCsr(codes: Seq[String], status: GiftCard.Status,
  reasonId: Option[Int] = None)
  extends Validation[GiftCardBulkUpdateStatusByCsr] {

  val bulkUpdateLimit = 20

  def validate: ValidatedNel[Failure, GiftCardBulkUpdateStatusByCsr] = {
    (GiftCard.validateStatusReason(status, reasonId)
      |@| validExpr(codes.nonEmpty, "Please provide at least one code to update")
      |@| lesserThanOrEqual(codes.length, bulkUpdateLimit, "Quantity")
      ).map { case _ ⇒ this }
  }
}