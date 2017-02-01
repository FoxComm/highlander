package payloads

import payloads.ImagePayloads.AlbumPayload
import utils.aliases._

object ProductVariantPayloads {
  case class ProductVariantPayload(attributes: Map[String, Json],
                                   scope: Option[String] = None,
                                   schema: Option[String] = None,
                                   albums: Option[Seq[AlbumPayload]] = None)
}
