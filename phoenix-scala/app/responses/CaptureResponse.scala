package responses

import java.time.Instant

import models.customer._
import models.location.Region

object CaptureResponse {
  case class Root(ref: String) extends ResponseItem

  def build(ref: String): Root = Root(ref)
}
