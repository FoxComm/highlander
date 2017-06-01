package foxcomm

import com.sksamuel.elastic4s.RichSearchHit
import fs2.Chunk
import org.http4s.{EntityEncoder, MediaType}
import org.http4s.headers.`Content-Type`

package object search {
  implicit val searchHitEncoder: EntityEncoder[RichSearchHit] =
    EntityEncoder.simple(`Content-Type`(MediaType.`application/json`))(sh =>
      Chunk.bytes(sh.sourceRef.toBytes))
}
