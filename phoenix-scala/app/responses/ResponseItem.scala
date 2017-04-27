package responses

import io.circe._

trait ResponseItem {
  def json: Json
}
object ResponseItem {
  implicit val encodeResponseItem: Encoder[ResponseItem] = new Encoder[ResponseItem] {
    def apply(a: ResponseItem): Json = a.json
  }
}
