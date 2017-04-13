package payloads

import java.time.Instant
import models.auth.Identity.IdentityKind
import models.activity.{Activity, ActivityContext}
import utils.aliases._

case class LoginPayload(email: String, password: String, org: String)

case class UpdateShippingMethod(shippingMethodId: Int)

case class NotificationActivity(id: String,
                                kind: String,
                                data: Json,
                                context: ActivityContext,
                                createdAt: Instant)

case class CreateNotification(sourceDimension: String,
                              sourceObjectId: String,
                              activity: NotificationActivity)
