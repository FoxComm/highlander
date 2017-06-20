package anthill.routes

import anthill.responses.PingResponse
import io.finch._

object ProdProd {

  private val justIds: Endpoint[PingResponse] =
    get(int) { productId: Int ⇒
      Ok(PingResponse(s"product: $productId"))
    }
  private val full: Endpoint[PingResponse] =
    get("full" :: int) { productId: Int ⇒
      Ok(PingResponse(s"full product: $productId"))
    }

  val prodProd = "prod-prod" :: (justIds :+: full)
}
