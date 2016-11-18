package payloads

import payloads.ImagePayloads.AlbumPayload
import utils.aliases._

object SkuPayloads {
  case class SkuPayload(attributes: Map[String, Json],
                        schema: Option[String] = None,
                        albums: Option[Seq[AlbumPayload]])
}
