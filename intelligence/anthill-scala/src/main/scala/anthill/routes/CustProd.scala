package anthill.routes

import anthill.responses.PingResponse
import io.finch._

object CustProd {

  private val justIds: Endpoint[PingResponse] =
    get(int) { customerId: Int ⇒
      Ok(PingResponse(s"customer: $customerId"))
    }
  private val full: Endpoint[PingResponse] =
    get("full" :: int) { customerId: Int ⇒
      Ok(PingResponse(s"full customer: $customerId"))
    }

  val custProd = "cust-prod" :: (justIds :+: full)
}
