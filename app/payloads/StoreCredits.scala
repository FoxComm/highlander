package payloads

import cats.data._
import cats.implicits._
import models.StoreCredit
import services.Failure
import utils.Litterbox._
import utils.Money._
import utils.Validation
import Validation._

final case class StoreCreditUpdateStatusByCsr(status: StoreCredit.Status, reason: Option[Int] = None)
  extends Validation[StoreCreditUpdateStatusByCsr] {

  def validate: ValidatedNel[Failure, StoreCreditUpdateStatusByCsr] = {
    StoreCredit.validateStatusReason(status, reason).map { case _ ⇒ this }
  }
}

final case class StoreCreditBulkUpdateStatusByCsr(ids: Seq[Int], status: StoreCredit.Status, reason: Option[Int] = None)
  extends Validation[StoreCreditBulkUpdateStatusByCsr] {

  val bulkUpdateLimit = 20

  def validate: ValidatedNel[Failure, StoreCreditBulkUpdateStatusByCsr] = {
    (StoreCredit.validateStatusReason(status, reason)
      |@| validExpr(ids.nonEmpty, "Please provide at least one code to update")
      |@| validExpr(ids.length <= bulkUpdateLimit, "Bulk update limit exceeded")
      ).map { case _ ⇒ this }
  }
}