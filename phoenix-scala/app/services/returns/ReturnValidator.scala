package services.returns

import failures.{Failure, Failures}
import failures.ReturnFailures.EmptyReturn
import models.returns._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

trait ReturnValidation {
  def validate: DbResultT[ReturnValidatorResponse]
}

case class ReturnValidatorResponse(alerts: Option[Failures] = None,
                                   warnings: Option[Failures] = None)

case class ReturnValidator(rma: Return)(implicit ec: EC) extends ReturnValidation {

  def validate: DbResultT[ReturnValidatorResponse] = {
    val response = ReturnValidatorResponse()

    for {
      state ← * <~ hasItems(response)
      state ← * <~ hasNoPreviouslyRefundedItems(response)
      state ← * <~ hasValidPaymentMethods(response)
    } yield state
  }

  private def hasItems(response: ReturnValidatorResponse): DBIO[ReturnValidatorResponse] = {
    ReturnLineItems.filter(_.returnId === rma.id).length.result.map { numItems ⇒
      if (numItems == 0) warning(response, EmptyReturn(rma.refNum)) else response
    }
  }

  /**
    * TODO: Implement stub methods
    */
  // Query previous completed RMAs, find matches between line items
  private def hasNoPreviouslyRefundedItems(
      response: ReturnValidatorResponse): DBIO[ReturnValidatorResponse] = {
    lift(response)
  }

  // Has at least one payment method
  // Can refund up to the total charged on that order payment method
  // Can refund up to the total charged on that order
  private def hasValidPaymentMethods(
      response: ReturnValidatorResponse): DBIO[ReturnValidatorResponse] = {
    lift(response)
  }

  private def warning(response: ReturnValidatorResponse,
                      failure: Failure): ReturnValidatorResponse =
    response.copy(warnings = response.warnings.fold(Failures(failure))(current ⇒
      Failures(current.toList :+ failure: _*)))
}
