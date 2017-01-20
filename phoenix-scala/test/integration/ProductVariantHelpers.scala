import org.json4s.JsonDSL._
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductVariantPayloads.ProductVariantPayload
import utils.aliases._

trait ProductVariantHelpers {
  def makeSkuPayload(code: String,
                     attrMap: Map[String, Json],
                     albums: Option[Seq[AlbumPayload]] = None) = {
    val codeJson   = ("t"              → "string") ~ ("v" → code)
    val attributes = attrMap + ("code" → codeJson)
    ProductVariantPayload(attributes = attributes, albums = albums)
  }

  def makeVariantPayload(code: String,
                         name: String,
                         albums: Option[Seq[AlbumPayload]]): ProductVariantPayload = {
    val attrMap = Map("title" → (("t" → "string") ~ ("v" → name)),
                      "name" → (("t" → "string") ~ ("v" → name)),
                      "code" → (("t" → "string") ~ ("v" → code)))

    ProductVariantPayload(attributes = attrMap, albums = albums)
  }

  def makeVariantPayload(code: String,
                         attrMap: Map[String, Json],
                         albums: Option[Seq[AlbumPayload]]) = {
    val codeJson   = ("t"               → "string") ~ ("v"      → code)
    val titleJson  = ("t"               → "string") ~ ("v"      → ("title_" + code))
    val attributes = (attrMap + ("code" → codeJson)) + ("title" → titleJson)
    ProductVariantPayload(attributes = attributes, albums = albums)
  }
}
