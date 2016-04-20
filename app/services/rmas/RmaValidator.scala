package services.rmas

import failures.{Failure, Failures}
import failures.RmaFailures.EmptyRma
import models.rma._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

trait RmaValidation {
  def validate: DbResult[RmaValidatorResponse]
}

case class RmaValidatorResponse(
  alerts:   Option[Failures] = None,
  warnings: Option[Failures] = None)

case class RmaValidator(rma: Rma)(implicit ec: EC) extends RmaValidation {

  def validate: DbResult[RmaValidatorResponse] = {
    val response = RmaValidatorResponse()

    (for {
      state ← hasItems(response)
      state ← hasNoPreviouslyRefundedItems(response)
      state ← hasValidPaymentMethods(response)
    } yield state).flatMap(DbResult.good)
  }

  private def hasItems(response: RmaValidatorResponse): DBIO[RmaValidatorResponse] = {
    RmaLineItems.filter(_.rmaId === rma.id).length.result.map { numItems ⇒
      if (numItems == 0) warning(response, EmptyRma(rma.refNum)) else response
    }
  }

  /**
   * TODO: Implement stub methods
   */

  // Query previous completed RMAs, find matches between line items
  private def hasNoPreviouslyRefundedItems(response: RmaValidatorResponse): DBIO[RmaValidatorResponse] = {
    lift(response)
  }

  // Has at least one payment method
  // Can refund up to the total charged on that order payment method
  // Can refund up to the total charged on that order
  private def hasValidPaymentMethods(response: RmaValidatorResponse): DBIO[RmaValidatorResponse] = {
    lift(response)
  }

  private def warning(response: RmaValidatorResponse, failure: Failure): RmaValidatorResponse =
    response.copy(warnings = response.warnings.fold(Failures(failure))
      (current ⇒ Failures(current.toList :+ failure: _*)))
}
