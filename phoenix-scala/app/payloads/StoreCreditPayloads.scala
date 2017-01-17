package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import models.payment.storecredit.StoreCredit
import utils.Validation
import utils.Validation._

object StoreCreditPayloads {

  case class StoreCreditUpdateStateByCsr(state: StoreCredit.State, reasonId: Option[Int] = None)
      extends Validation[StoreCreditUpdateStateByCsr] {

    def validate: ValidatedNel[Failure, StoreCreditUpdateStateByCsr] = {
      StoreCredit.validateStateReason(state, reasonId).map { case _ ⇒ this }
    }
  }

  case class StoreCreditBulkUpdateStateByCsr(ids: Seq[Int],
                                             state: StoreCredit.State,
                                             reasonId: Option[Int] = None)
      extends Validation[StoreCreditBulkUpdateStateByCsr] {

    val bulkUpdateLimit = 20

    def validate: ValidatedNel[Failure, StoreCreditBulkUpdateStateByCsr] = {
      (StoreCredit.validateStateReason(state, reasonId) |@| validExpr(
        ids.nonEmpty,
        "Please provide at least one code to update") |@| lesserThanOrEqual(ids.length,
                                                                            bulkUpdateLimit,
                                                                            "Quantity")).map {
        case _ ⇒ this
      }
    }
  }
}
