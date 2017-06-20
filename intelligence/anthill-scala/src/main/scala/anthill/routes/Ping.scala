package anthill.routes

import anthill.responses.PingResponse
import io.finch._

object Ping {
  val ping: Endpoint[PingResponse] =
    get("ping") {
      Ok(PingResponse("pong"))
    }
}
