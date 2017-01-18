package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import models.payment.giftcard.GiftCard
import utils.Money._
import utils.Validation
import utils.Validation._
import utils.aliases.Json

object GiftCardPayloads {

  case class GiftCardCreatedByCustomer(scope: Option[String] = None,
                                       balance: Int,
                                       currency: Currency = Currency.USD,
                                       subTypeId: Option[Int] = None,
                                       senderName: String,
                                       recipientName: String,
                                       recipientEmail: String,
                                       message: String,
                                       cordRef: String)
      extends Validation[GiftCardCreatedByCustomer] {

    def validate: ValidatedNel[Failure, GiftCardCreatedByCustomer] =
      greaterThan(balance, 0, "Balance").map(_ ⇒ this)
  }

  case class GiftCardCreateByCsr(balance: Int,
                                 reasonId: Int,
                                 currency: Currency = Currency.USD,
                                 subTypeId: Option[Int] = None,
                                 scope: Option[String] = None)
      extends Validation[GiftCardCreateByCsr] {

    def validate: ValidatedNel[Failure, GiftCardCreateByCsr] = {
      (greaterThan(balance, 0, "Balance") |@| scope.fold[ValidatedNel[Failure, Unit]](ok)(s ⇒
        notEmpty(s, "scope"))).map {
        case _ ⇒ this
      }
    }
  }

  case class GiftCardBulkCreateByCsr(quantity: Int,
                                     balance: Int,
                                     reasonId: Int,
                                     currency: Currency = Currency.USD,
                                     subTypeId: Option[Int] = None,
                                     scope: Option[String] = None)
      extends Validation[GiftCardBulkCreateByCsr] {

    val bulkCreateLimit = 20

    def validate: ValidatedNel[Failure, GiftCardBulkCreateByCsr] = {
      (greaterThan(balance, 0, "Balance") |@| greaterThan(quantity, 0, "Quantity") |@| lesserThanOrEqual(
        quantity,
        bulkCreateLimit,
        "Quantity") |@| scope.fold(ok)(s ⇒ notEmpty(s, "scope"))).map { case _ ⇒ this }
    }
  }

  case class GiftCardUpdateStateByCsr(state: GiftCard.State, reasonId: Option[Int] = None)
      extends Validation[GiftCardUpdateStateByCsr] {

    def validate: ValidatedNel[Failure, GiftCardUpdateStateByCsr] = {
      GiftCard.validateStateReason(state, reasonId).map(_ ⇒ this)
    }
  }

  case class GiftCardBulkUpdateStateByCsr(codes: Seq[String],
                                          state: GiftCard.State,
                                          reasonId: Option[Int] = None)
      extends Validation[GiftCardBulkUpdateStateByCsr] {

    val bulkUpdateLimit = 20

    def validate: ValidatedNel[Failure, GiftCardBulkUpdateStateByCsr] = {
      (GiftCard.validateStateReason(state, reasonId) |@| validExpr(
        codes.nonEmpty,
        "Please provide at least one code to update") |@| lesserThanOrEqual(codes.length,
                                                                            bulkUpdateLimit,
                                                                            "Quantity")).map {
        case _ ⇒ this
      }
    }
  }
}
