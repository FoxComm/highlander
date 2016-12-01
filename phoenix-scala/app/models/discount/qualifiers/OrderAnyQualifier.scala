package models.discount.qualifiers

import models.account.User
import models.discount.DiscountInput
import services.Authenticator.AuthData
import services.Result
import utils.aliases._

case object OrderAnyQualifier extends Qualifier {

  val qualifierType: QualifierType = OrderAny

  def check(
      input: DiscountInput)(implicit db: DB, ec: EC, es: ES, auth: AuthData[User]): Result[Unit] =
    Result.unit
}
