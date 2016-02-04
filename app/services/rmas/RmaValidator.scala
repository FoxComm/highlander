package services.rmas

import models.{Rma, RmaLineItems}
import services.RmaFailures._
import services.{Failure, Failures}
import slick.driver.PostgresDriver.api._
import utils.Slick._

import scala.concurrent.ExecutionContext

trait RmaValidation {
  def validate: DbResult[RmaValidatorResponse]
}

final case class RmaValidatorResponse(
  alerts:   Option[Failures] = None,
  warnings: Option[Failures] = None)

final case class RmaValidator(rma: Rma)(implicit db: Database, ec: ExecutionContext) extends RmaValidation {

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
