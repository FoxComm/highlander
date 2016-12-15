package payloads

import payloads.ImagePayloads.AlbumPayload
import utils.aliases._

object ProductVariantPayloads {
  case class ProductVariantPayload(scope: Option[String] = None,
                                   attributes: Map[String, Json],
                                   schema: Option[String] = None,
                                   albums: Option[Seq[AlbumPayload]] = None)
}
