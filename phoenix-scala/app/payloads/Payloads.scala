package payloads

import java.time.Instant
import models.auth.Identity.IdentityKind
import models.activity.{Activity, ActivityContext}
import utils.aliases._

case class LoginPayload(email: String, password: String, org: String)

case class UpdateShippingMethod(shippingMethodId: Int)

case class NotificationActivity(id: String,
                                kind: ActivityType,
                                data: Json,
                                context: ActivityContext,
                                createdAt: Instant)

case class CreateNotification(scope: Option[String],
                              sourceDimension: String,
                              sourceObjectId: String,
                              activity: NotificationActivity)
