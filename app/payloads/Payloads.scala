package payloads

import models.Aliases._
import models.auth.Identity.IdentityKind

case class LoginPayload(email: String, password: String, kind: IdentityKind)

case class UpdateShippingMethod(shippingMethodId: Int)

case class CreateNotification(
    sourceDimension: String, sourceObjectId: String, activityId: Int, data: Option[Json])
