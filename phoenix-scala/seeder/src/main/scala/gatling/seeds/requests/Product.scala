package gatling.seeds.requests

import java.time.Instant

import gatling.seeds.requests.Auth._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import models.product._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.json4s.JsonDSL._
import payloads.ImagePayloads._
import payloads.ProductPayloads._
import payloads.ProductVariantPayloads._
import utils.aliases._
import utils.Money.Currency
import utils.seeds.Factories

case class StringField(value: String, stringType: String = "string") {
  val illuminated: Json = ("t" → stringType) ~ ("v" → value)
}

case class PriceField(currency: Currency, value: Int) {
  val illuminated: Json =
    ("t" → "price") ~ (("currency" → currency.getCode) ~ ("value" → value))
}

object Product {
  private def buildCreatePayload(sp: SimpleProductData): CreateProductPayload = {
    val activeFrom = if (sp.active) s""""${Instant.now}"""" else "null";
    val ts: String = compact(render(JArray(sp.tags.map(t ⇒ JString(t)).toList)))

    val code        = StringField(sp.code).illuminated
    val title       = StringField(sp.title).illuminated
    val description = StringField(sp.description, "richText").illuminated
    val price       = PriceField(sp.currency, sp.price).illuminated
    val activeDate  = StringField(activeFrom, "date-time").illuminated
    val tags        = ("t" → "tags") ~ ("v" → ts)

    val productAttrs =
      Map("title" → title, "description" → description, "activeFrom" → activeDate, "tags" → tags)
    val variantAttrs = Map("code" → code,
                           "title"       → title,
                           "description" → description,
                           "activeFrom"  → activeDate,
                           "salePrice"   → price,
                           "retailPrice" → price)

    val album =
      AlbumPayload(name = Some("default"), images = Some(Seq(ImagePayload(src = sp.image))))

    val variant = ProductVariantPayload(attributes = variantAttrs, albums = Some(Seq(album)))
    CreateProductPayload(attributes = productAttrs,
                         variants = Seq(variant),
                         options = None,
                         albums = Some(Seq(album)))
  }

  private def createNewProduct =
    http("Create product")
      .post("/v1/products/default")
      .requireAdminAuth
      .body(StringBody(session ⇒ session.get("productPayload").as[String]))

  val createProducts = foreach(Factories.products, "simpleProduct") {
    exec(session ⇒ {
      val simple  = session("simpleProduct").as[SimpleProductData]
      val payload = buildCreatePayload(simple)
      session.set("productPayload", payload)
    }).exec(createNewProduct)
  }

}
