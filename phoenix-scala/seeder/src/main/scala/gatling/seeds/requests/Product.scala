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

object Product {
  case class DateTimeField(isActive: Boolean) {
    def illuminated: Json = {
      val activeStr = if (isActive) s""""${Instant.now}"""" else "null"
      ("t" → "date-time") ~ ("v" → activeStr)
    }
  }

  case class StringField(value: String, stringType: String = "string") {
    val illuminated: Json = ("t" → stringType) ~ ("v" → value)
  }

  case class PriceField(currency: Currency, value: Int) {
    val illuminated: Json =
      ("t" → "price") ~ (("currency" → currency.getCode) ~ ("value" → value))
  }

  case class TagsField(tags: Seq[String]) {
    val illuminated: Json = ("t" → "tags") ~ ("v" → tags)
  }

  private def buildCreatePayload(sp: SimpleProductData): CreateProductPayload = {
    val images = Some(Seq(ImagePayload(src = sp.image)))
    val albums = Some(Seq(AlbumPayload(name = Some("default"), images = images)))

    val code        = StringField(sp.code).illuminated
    val title       = StringField(sp.title).illuminated
    val description = StringField(sp.description, "richText").illuminated
    val price       = PriceField(sp.currency, sp.price).illuminated
    val activeFrom  = DateTimeField(sp.active).illuminated
    val tags        = TagsField(sp.tags).illuminated

    val productAttrs =
      Map("title" → title, "description" → description, "activeFrom" → activeFrom, "tags" → tags)

    val variantAttrs = Map("code" → code,
                           "title"       → title,
                           "description" → description,
                           "activeFrom"  → activeFrom,
                           "salePrice"   → price,
                           "retailPrice" → price)

    val variant = ProductVariantPayload(attributes = variantAttrs, albums = albums)

    CreateProductPayload(attributes = productAttrs,
                         variants = Seq(variant),
                         options = None,
                         albums = albums)
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
