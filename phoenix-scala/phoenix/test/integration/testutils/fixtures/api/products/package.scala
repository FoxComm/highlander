package testutils.fixtures.api

import faker.Lorem
import phoenix.payloads.ImagePayloads.AlbumPayload
import phoenix.payloads.SkuPayloads.SkuPayload
import phoenix.utils.aliases.Json
import phoenix.utils.time.yesterday
import testutils.PayloadHelpers._

package object products {

  def skuAttrs(code: String, price: Long, title: String = Lorem.sentence(1)): Map[String, Json] =
    Map("code"        → tv(code),
        "title"       → tv(title),
        "salePrice"   → usdPrice(price),
        "retailPrice" → usdPrice(price),
        "activeFrom"  → tv(yesterday.toInstant, "datetime"))

  def buildSkuPayload(code: String = Lorem.letterify("#####"),
                      price: Long = 10000,
                      title: String = Lorem.sentence(1),
                      albums: Option[Seq[AlbumPayload]] = None): SkuPayload =
    SkuPayload(attributes = skuAttrs(code, price, title), albums = albums)
}
