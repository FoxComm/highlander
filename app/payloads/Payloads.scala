package payloads

import models.auth.Identity.IdentityKind
import utils.aliases._

case class LoginPayload(email: String, password: String, kind: IdentityKind)

case class UpdateShippingMethod(shippingMethodId: Int)

case class CreateNotification(
    sourceDimension: String, sourceObjectId: String, activityId: Int, data: Option[Json])
