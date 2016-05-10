package seeds

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Products {

  val createProduct = http("Create product")
    .post("/v1/products/full/${context}")
    .body(ELFileBody("payloads/create_product.json"))

  val createAllProducts = Conf.contexts.map { case (ctx, currency) ⇒
    exec(session ⇒ session
      .set("context", ctx)
      .set("newSkuUUID", UUID.randomUUID)
      .set("newProductCurrency", currency)
    )
    .foreach(ssv(s"data/products/$ctx.ssv").records, "newProductRecord") {
      exec(flattenMapIntoAttributes("${newProductRecord}")).exec(createProduct)
    }
  }

}
