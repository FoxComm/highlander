package payloads

import cats.data._
import cats.implicits._
import models.payment.storecredit.StoreCredit
import services.Failure
import utils.Litterbox._
import utils.Money._
import utils.Validation
import Validation._

final case class StoreCreditUpdateStateByCsr(state: StoreCredit.State, reasonId: Option[Int] = None)
  extends Validation[StoreCreditUpdateStateByCsr] {

  def validate: ValidatedNel[Failure, StoreCreditUpdateStateByCsr] = {
    StoreCredit.validateStateReason(state, reasonId).map { case _ ⇒ this }
  }
}

final case class StoreCreditBulkUpdateStateByCsr(ids: Seq[Int], state: StoreCredit.State,
  reasonId: Option[Int] = None)
  extends Validation[StoreCreditBulkUpdateStateByCsr] {

  val bulkUpdateLimit = 20

  def validate: ValidatedNel[Failure, StoreCreditBulkUpdateStateByCsr] = {
    (StoreCredit.validateStateReason(state, reasonId)
      |@| validExpr(ids.nonEmpty, "Please provide at least one code to update")
      |@| lesserThanOrEqual(ids.length, bulkUpdateLimit, "Quantity")
      ).map { case _ ⇒ this }
  }
}