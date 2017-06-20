package anthill.routes

import anthill.payloads.PurchaseEventPayload
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._

object Train {
  val purchaseEvent: Endpoint[PurchaseEventPayload] =
    post("prod-prod" :: "train" :: jsonBody[PurchaseEventPayload]) { payload: PurchaseEventPayload ⇒
      Ok(payload)
    }
}
