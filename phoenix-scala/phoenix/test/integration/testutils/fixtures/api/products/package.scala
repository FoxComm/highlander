package testutils.fixtures.api

import faker.Lorem
import payloads.ImagePayloads.AlbumPayload
import payloads.SkuPayloads.SkuPayload
import testutils.PayloadHelpers._
import utils.aliases.Json
import utils.time.yesterday

package object products {

  def skuAttrs(code: String, price: Int, title: String = Lorem.sentence(1)): Map[String, Json] =
    Map("code"        → tv(code),
        "title"       → tv(title),
        "salePrice"   → usdPrice(price),
        "retailPrice" → usdPrice(price),
        "activeFrom"  → tv(yesterday.toInstant, "datetime"))

  def buildSkuPayload(code: String = Lorem.letterify("#####"),
                      price: Int = 10000,
                      title: String = Lorem.sentence(1),
                      albums: Option[Seq[AlbumPayload]] = None): SkuPayload =
    SkuPayload(attributes = skuAttrs(code, price, title), albums = albums)
}
